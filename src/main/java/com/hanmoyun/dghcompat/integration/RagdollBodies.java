package com.hanmoyun.dghcompat.integration;

import com.hanmoyun.dghcompat.StasisPatients;
import com.hanmoyun.dghcompat.corpse.NativePatients;
import com.lastimp.dgh.common.capability.HealthCapability;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/** The original medical entity follows the physics body; there is no second world entity. */
public final class RagdollBodies {
    private record Body(AABB bounds) {}

    // Client and integrated server share this class, but never the same entity objects.
    private static final Map<Entity, Body> BODIES =
            Collections.synchronizedMap(new WeakHashMap<>());

    private RagdollBodies() {}

    public static boolean available() {
        return ModList.get().isLoaded("ragdollified") && ModList.get().isLoaded("zarith");
    }

    public static boolean desired(LivingEntity entity) {
        return NativePatients.actual(entity) == entity
                && entity.isAlive()
                && !entity.isDeadOrDying()
                && !entity.isRemoved()
                && !entity.isPassenger()
                && !entity.isSleeping()
                && !StasisPatients.wrapped(entity)
                && (entity.level().isClientSide()
                        ? entity instanceof com.hanmoyun.dghcompat.corpse.CorpseEntity
                                || entity.getPersistentData().getBoolean("dghcompat:down_visual")
                                || (!entity.getPersistentData().contains("dghcompat:down_visual")
                                        && HealthCapability.isDown(entity))
                        : com.hanmoyun.dghcompat.corpse.Corpses.isCorpse(entity)
                                || HealthCapability.isDown(entity));
    }

    public static boolean active(Entity entity) {
        return BODIES.containsKey(entity);
    }

    public static float scale(LivingEntity entity) {
        if (entity instanceof com.hanmoyun.dghcompat.corpse.CorpseEntity corpse
                && corpse.visual().contains("physics_scale"))
            return Math.clamp(corpse.visual().getFloat("physics_scale"), 0.05f, 16f);
        return entity instanceof net.minecraft.world.entity.player.Player
                ? entity.getScale()
                : entity.getDimensions(net.minecraft.world.entity.Pose.STANDING).height() / 1.8f;
    }

    public static AABB bounds(Entity entity) {
        Body body = BODIES.get(entity);
        return body == null ? null : body.bounds();
    }

    public static void follow(Entity entity, Vec3 torso, AABB bounds) {
        BODIES.put(entity, new Body(bounds));
        // Vanilla coordinates are feet based. X/Z belong at the torso, Y at the body bottom.
        // makeBoundingBox is overridden while following, so a pose change cannot restore a
        // standing hitbox between physics updates.
        entity.setPos(torso.x, bounds.minY, torso.z);
        entity.setBoundingBox(bounds);
        entity.setDeltaMovement(Vec3.ZERO);
        entity.fallDistance = 0;
    }

    public static void release(Entity entity) {
        if (BODIES.remove(entity) == null) return;
        if (entity.isAlive() && !entity.isRemoved() && !entity.isPassenger()) {
            com.hanmoyun.dghcompat.StasisGeometry.placeOnGround(entity);
        }
        entity.refreshDimensions();
        if (entity.getPersistentData().getBoolean(StasisPatients.WRAPPED))
            com.hanmoyun.dghcompat.StasisGeometry.refreshBounds(entity);
        else
            entity.setBoundingBox(
                    entity.getDimensions(entity.getPose()).makeBoundingBox(entity.position()));
    }

    public static void serverTick(LivingEntity entity) {
        if (available()) NativeRagdollBodies.tick(entity);
    }

    /** Tracking can end while Minecraft is iterating the entity section. */
    public static void forget(Entity entity) {
        BODIES.remove(entity);
        if (available() && !entity.level().isClientSide()) NativeRagdollBodies.endSession(entity);
    }

    public static void stop(Entity entity) {
        if (available() && !entity.level().isClientSide()) NativeRagdollBodies.stop(entity);
        release(entity);
    }

    public static void impulse(Entity entity, Vec3 impulse) {
        if (available() && !entity.level().isClientSide())
            NativeRagdollBodies.impulse(entity, impulse);
    }
}
