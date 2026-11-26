package com.hanmoyun.dghcompat.integration;

import com.hanmoyun.dghcompat.corpse.CorpseEntity;
import com.raiiiden.ragdollified.MobModelHelper;
import com.raiiiden.ragdollified.api.RagdollifiedServerApi;
import com.raiiiden.ragdollified.network.RagdollImpulsePacket;
import com.raiiiden.ragdollified.network.RagdollSpawnPacket;
import com.raiiiden.ragdollified.server.ServerRagdollSyncManager;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.VillagerDataHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.WeakHashMap;

/** Optional Ragdollified 1.2.2 adapter. Only loaded after both physics mods are present. */
public final class NativeRagdollBodies {
    private record Session(float scale) {}

    private static final Map<Entity, Session> SESSIONS = new WeakHashMap<>();

    private static final Map<LivingEntity, RagdollSpawnPacket> DEATHS = new WeakHashMap<>();

    public static void rememberDeath(LivingEntity source, RagdollSpawnPacket packet) {
        DEATHS.put(source, packet.copy());
    }

    private NativeRagdollBodies() {}

    public static void tick(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel level)) return;
        if (!RagdollBodies.desired(entity)) {
            stop(entity);
            return;
        }
        Session session = SESSIONS.get(entity);
        if (session == null) {
            float scale = RagdollBodies.scale(entity);
            RagdollSpawnPacket packet = null;
            if (entity instanceof CorpseEntity corpse
                    && corpse.visual().contains("source_entity")
                    && level.getGameTime() - corpse.visual().getLong("fall_time") < 10) {
                var source = level.getEntity(corpse.visual().getInt("source_entity"));
                if (source instanceof LivingEntity living
                        && living.getUUID().equals(corpse.visual().getUUID("owner"))) {
                    packet = DEATHS.remove(living);
                    if (packet == null && corpse.tickCount < 4) return;
                    if (packet != null)
                        ((com.hanmoyun.dghcompat.mixin.DeathPacketAccess) (Object) packet)
                                .dghcompat$setEntityId(entity.getId());
                }
            }
            if (packet == null) packet = spawnPacket(entity, scale);
            if (packet == null) return;
            // Native code elects one simulator and checks its stream identity/serial/pose.
            // An indefinitely downed patient must not expire like a decorative death body.
            ServerRagdollSyncManager.registerLive(entity, packet, Integer.MAX_VALUE);
            session = new Session(scale);
            SESSIONS.put(entity, session);
        }
        var body = ServerRagdollSyncManager.retainedBody(entity.getId(), level.dimension());
        if (body == null || !body.live()) {
            SESSIONS.remove(entity);
            RagdollBodies.release(entity);
            return;
        }
        var shape = RagdollGeometry.shape(body.pose(), session.scale());
        // Do not freeze or hide an actor until the elected simulator supplies a real pose.
        if (shape != null) RagdollBodies.follow(entity, shape.torso(), shape.bounds());
    }

    private static RagdollSpawnPacket spawnPacket(LivingEntity entity, float scale) {
        var corpse = entity instanceof CorpseEntity c ? c.visual() : null;
        String type =
                corpse == null
                        ? BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString()
                        : corpse.getString("type");
        boolean player = entity instanceof Player || type.equals("minecraft:player");
        var model =
                player
                        ? MobModelHelper.ModelType.HUMANOID_STANDARD
                        : corpse == null
                                ? MobModelHelper.getModelTypeFromEntity(entity)
                                : MobModelHelper.getModelTypeFromMobType(type);
        if (!MobModelHelper.isSupportedModelType(model)) return null;
        var velocity = entity.getDeltaMovement();
        if (velocity.horizontalDistanceSqr() < 0.001 && corpse == null) {
            double yaw = Math.toRadians(entity.yBodyRot);
            velocity = velocity.add(-Math.sin(yaw) * 0.12, 0, Math.cos(yaw) * 0.12);
        }
        String villagerType = "minecraft:plains", profession = "minecraft:none";
        byte villagerLevel = 1;
        if (entity instanceof VillagerDataHolder villager) {
            var data = villager.getVillagerData();
            villagerType = BuiltInRegistries.VILLAGER_TYPE.getKey(data.getType()).toString();
            profession =
                    BuiltInRegistries.VILLAGER_PROFESSION.getKey(data.getProfession()).toString();
            villagerLevel = (byte) data.getLevel();
        }
        // DGH forces a swimming pose when downed. That is not the pose from which a land
        // patient fell: passing it here makes the new physics body instantly horizontal.
        boolean horizontal = corpse != null || (entity.isInWater() && entity.isSwimming());
        var packet =
                new RagdollSpawnPacket(
                        entity.getId(),
                        player,
                        type,
                        model,
                        scale,
                        corpse == null
                                ? entity.getStringUUID()
                                : corpse.getUUID("owner").toString(),
                        entity.getName().getString(),
                        entity.getX(),
                        entity.getY(),
                        entity.getZ(),
                        entity.yBodyRot,
                        0,
                        velocity.x,
                        velocity.y,
                        velocity.z,
                        horizontal,
                        corpse == null ? entity.isBaby() : corpse.getBoolean("baby"),
                        entity.getItemBySlot(EquipmentSlot.HEAD).copy(),
                        entity.getItemBySlot(EquipmentSlot.CHEST).copy(),
                        entity.getItemBySlot(EquipmentSlot.LEGS).copy(),
                        entity.getItemBySlot(EquipmentSlot.FEET).copy(),
                        animalState(entity),
                        (byte) -1,
                        0,
                        0,
                        0,
                        (byte) 0,
                        villagerType,
                        profession,
                        villagerLevel);
        packet.severedMask(RagdollifiedServerApi.resolveSeveredPartMask(entity));
        return packet.tropicalFishVariant(MobModelHelper.tropicalFishVariant(entity));
    }

    private static byte animalState(LivingEntity entity) {
        if (entity instanceof net.minecraft.world.entity.animal.Sheep sheep)
            return RagdollSpawnPacket.packSheepState(sheep.isSheared(), sheep.getColor().getId());
        return 0;
    }

    public static boolean endSession(Entity entity) {
        if (SESSIONS.remove(entity) == null) return false;
        // Death may already have installed a new native body under the same runtime ID.
        if (ServerRagdollSyncManager.isLive(entity.getId()))
            ServerRagdollSyncManager.remove(entity.getId());
        return true;
    }

    public static void stop(Entity entity) {
        if (!endSession(entity)) return;
        RagdollBodies.release(entity);
        if (entity instanceof ServerPlayer player && player.isAlive() && !player.isRemoved())
            player.connection.teleport(
                    entity.getX(),
                    entity.getY(),
                    entity.getZ(),
                    entity.getYRot(),
                    entity.getXRot());
    }

    public static void impulse(Entity entity, Vec3 impulse) {
        if (!RagdollBodies.active(entity)
                || !(entity.level() instanceof ServerLevel level)
                || impulse.lengthSqr() < 1.0e-8) return;
        double length = impulse.length();
        Vec3 limited = length > 2 ? impulse.scale(2 / length) : impulse;
        RagdollImpulsePacket.broadcastOrdered(
                level, null, entity.getId(), 0, limited, entity.getBoundingBox().getCenter(), 0);
    }

    public static com.lastimp.dgh.common.enums.BodyComponents hitPart(
            LivingEntity target, net.minecraft.world.damagesource.DamageSource source) {
        Vec3 start, end;
        if (source.getDirectEntity()
                        instanceof net.minecraft.world.entity.projectile.Projectile shot
                && shot.getDeltaMovement().lengthSqr() > 1.0e-8) {
            start = shot.position().subtract(shot.getDeltaMovement());
            end = shot.position().add(shot.getDeltaMovement());
        } else if (source.getEntity() instanceof Player attacker) {
            start = attacker.getEyePosition();
            end = start.add(attacker.getLookAngle().scale(attacker.entityInteractionRange()));
        } else {
            return null;
        }
        var blocked =
                target.level()
                        .clip(
                                new net.minecraft.world.level.ClipContext(
                                        start,
                                        end,
                                        net.minecraft.world.level.ClipContext.Block.COLLIDER,
                                        net.minecraft.world.level.ClipContext.Fluid.NONE,
                                        target));
        if (blocked.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK)
            end = blocked.getLocation();
        return hitPart(target, start, end);
    }

    public static com.lastimp.dgh.common.enums.BodyComponents hitPart(
            LivingEntity target, Vec3 start, Vec3 end) {
        var body =
                ServerRagdollSyncManager.retainedBody(target.getId(), target.level().dimension());
        if (body == null || !body.live() || body.pose() == null) return null;
        double nearest = Double.POSITIVE_INFINITY;
        com.raiiiden.ragdollified.RagdollPart hit = null;
        float scale = RagdollBodies.scale(target);
        for (var transform : body.pose()) {
            if (transform == null) continue;
            var part = com.raiiiden.ragdollified.RagdollPart.byIndex(transform.partId);
            if (part == null) continue;
            double distance = RagdollGeometry.hitDistance(transform, part, scale, start, end);
            if (distance < nearest) {
                nearest = distance;
                hit = part;
            }
        }
        return hit == null ? null : com.lastimp.dgh.common.enums.BodyComponents.valueOf(hit.name());
    }
}
