package com.hanmoyun.dghcompat.client;

import com.hanmoyun.dghcompat.corpse.CorpseEntity;
import com.hanmoyun.dghcompat.corpse.NativePatients;
import com.hanmoyun.dghcompat.integration.RagdollBodies;
import com.hanmoyun.dghcompat.integration.RagdollGeometry;
import com.raiiiden.ragdollified.RagdollPart;
import com.raiiiden.ragdollified.api.CameraOptions;
import com.raiiiden.ragdollified.api.RagdollifiedApi;
import com.raiiiden.ragdollified.client.ClientRagdollManager;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.WeakHashMap;

/** Never spawns an independent local simulation for a medical actor. */
final class NativeRagdollClient {
    private record Body(float scale, int started) {}

    private static final java.util.Set<CorpseEntity> SEEDED =
            java.util.Collections.newSetFromMap(new WeakHashMap<>());
    private static final Map<LivingEntity, Body> TRACKED = new WeakHashMap<>();

    private static final java.util.Set<CorpseEntity> PARKED =
            java.util.Collections.newSetFromMap(new WeakHashMap<>());
    private static LivingEntity cameraOwner;

    // Use the native settled pose. A nearby observer or a drag wakes the body again.
    private static void updateRest(CorpseEntity corpse, com.raiiiden.ragdollified.RagdollTransform[] pose) {
        int id = corpse.getId();
        boolean ours = PARKED.contains(corpse);
        boolean nearby = corpse.level().getNearestPlayer(corpse, ours ? 20 : 28) != null;
        if (nearby || ClientRagdollManager.isDragging(id) || ClientRagdollManager.isHeld(id)) {
            if (PARKED.remove(corpse)) RagdollifiedApi.unpark(id);
        } else if (!RagdollifiedApi.isParked(id) && corpse.tickCount % 10 == 0
                && RagdollifiedApi.getState(id).map(s -> s.settled() || s.frozen()).orElse(false)
                && RagdollifiedApi.park(id, pose)) {
            PARKED.add(corpse);
        }
    }

    static void replacePlayer(Player previous) {
        releaseCamera(previous);
        TRACKED.remove(previous);
        int id = previous.getId();
        ClientRagdollManager.setPersistent(id, false);
        RagdollifiedApi.remove(id);
        RagdollifiedApi.setEntityHidden(id, false);
        RagdollBodies.forget(previous);
    }

    private static void releaseCamera(Entity entity) {
        if (cameraOwner == null || cameraOwner != entity) return;
        RagdollifiedApi.detachCamera(entity.getId());
        cameraOwner = null;
    }

    private static void updateCamera(LivingEntity entity, boolean desired) {
        var client = Minecraft.getInstance();
        if (entity != client.player
                || !desired
                || client.getCameraEntity() != entity
                || !client.options.getCameraType().isFirstPerson()) {
            releaseCamera(entity);
            return;
        }
        if (cameraOwner != entity
                && RagdollifiedApi.attachCamera(
                        entity.getId(), RagdollPart.HEAD, CameraOptions.DEFAULT))
            cameraOwner = entity;
    }

    private NativeRagdollClient() {}

    static boolean supported(LivingEntity entity) {
        Body body = TRACKED.get(entity);
        if (body != null && entity.tickCount - body.started() > 40 && !RagdollBodies.active(entity))
            return false;
        return entity instanceof Player
                || com.raiiiden.ragdollified.MobModelHelper.isSupportedModelType(
                        com.raiiiden.ragdollified.client.ClientMobModelHelper.getActualModelType(
                                entity));
    }

    static Entity pick(Vec3 start, Vec3 end) {
        var hit = RagdollifiedApi.raycast(start, end);
        var level = Minecraft.getInstance().level;
        if (hit.isEmpty() || level == null) return null;
        int id = hit.get().entityId();
        return level.getEntity(id);
    }

    static void update(LivingEntity entity) {
        if (NativePatients.actual(entity) != entity) return;
        boolean desired = RagdollBodies.desired(entity);
        updateCamera(entity, desired);
        if (!desired) {
            if (entity instanceof CorpseEntity corpse && PARKED.remove(corpse)) RagdollifiedApi.unpark(corpse.getId());
            if (TRACKED.remove(entity) != null) {
                boolean dying = !entity.isAlive() || entity.isDeadOrDying();
                RagdollifiedApi.setEntityHidden(
                        entity.getId(), dying && RagdollifiedApi.hasRagdoll(entity.getId()));
                ClientRagdollManager.setPersistent(entity.getId(), false);
                // Preserve a native death body that may just have taken over the same ID.
                if (entity.isAlive() && !entity.isDeadOrDying()) {
                    RagdollifiedApi.remove(entity.getId());
                }
                RagdollBodies.release(entity);
            }
            return;
        }
        Body body =
                TRACKED.computeIfAbsent(entity, e -> new Body(RagdollBodies.scale(e), e.tickCount));
        ClientRagdollManager.setPersistent(entity.getId(), true);
        if (entity instanceof CorpseEntity dead
                && dead.visual().contains("death_pose")
                && entity.level().getGameTime() - dead.visual().getLong("pose_time") < 60
                && !SEEDED.contains(dead)
                && RagdollifiedApi.hasRagdoll(entity.getId())) {
            var saved =
                    com.hanmoyun.dghcompat.integration.DeathPose.read(
                            dead.visual(), dead.position());
            if (saved != null)
                ClientRagdollManager.enqueueAuthoritativeState(dead.getId(), saved, 0, false);
            SEEDED.add(dead);
        }
        var pose = RagdollifiedApi.worldPose(entity.getId());
        var shape = pose.isEmpty() ? null : RagdollGeometry.shape(pose.get(), body.scale());
        if (shape == null) {
            if (entity instanceof CorpseEntity corpse && renders(corpse)) {
                RagdollifiedApi.setEntityHidden(entity.getId(), true);
                return;
            }
            RagdollifiedApi.setEntityHidden(entity.getId(), false);
            RagdollBodies.release(entity);
            return;
        }
        if (entity instanceof CorpseEntity corpse
                && corpse.visual().contains("source_entity")
                && entity.level().getGameTime() - corpse.visual().getLong("fall_time") < 60) {
            int oldId = corpse.visual().getInt("source_entity");
            var original = entity.level().getEntity(oldId);
            if (oldId != entity.getId()
                    && (original == null
                            || (!original.isAlive()
                                    && corpse.visual()
                                            .getUUID("owner")
                                            .equals(original.getUUID())))) {
                RagdollifiedApi.remove(oldId);
            }
        }
        if (entity instanceof CorpseEntity corpse) updateRest(corpse, pose.orElseThrow());
        RagdollBodies.follow(entity, shape.torso(), shape.bounds());
        RagdollifiedApi.setEntityHidden(entity.getId(), true);
    }

    static boolean renders(CorpseEntity corpse) {
        var nativeBody = ClientRagdollManager.get(corpse.getId());
        return nativeBody != null && !nativeBody.isDestroyed() && nativeBody.getSnapshot() != null;
    }

    static void clear() {
        releaseCamera(cameraOwner);
        for (var entity : TRACKED.keySet()) {
            RagdollifiedApi.setEntityHidden(entity.getId(), false);
            ClientRagdollManager.setPersistent(entity.getId(), false);
            RagdollifiedApi.remove(entity.getId());
            RagdollBodies.forget(entity);
        }
        TRACKED.clear();
        PARKED.clear();
        SEEDED.clear();
    }

    static void forget(Entity entity) {
        releaseCamera(entity);
        if (entity instanceof CorpseEntity corpse && PARKED.remove(corpse)) RagdollifiedApi.unpark(entity.getId());
        if (!(entity instanceof LivingEntity living) || TRACKED.remove(living) == null) return;
        ClientRagdollManager.setPersistent(entity.getId(), false);
        RagdollifiedApi.setEntityHidden(entity.getId(), false);
        if (living instanceof CorpseEntity || (living.isAlive() && !living.isDeadOrDying()))
            RagdollifiedApi.remove(entity.getId());
        RagdollBodies.forget(entity);
    }
}
