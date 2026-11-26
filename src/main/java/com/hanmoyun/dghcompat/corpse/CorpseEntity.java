package com.hanmoyun.dghcompat.corpse;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;

/** Independent, inert surgical target. Vanilla health is only the carrier, never a resurrection. */
public class CorpseEntity extends PathfinderMob {
    private final net.minecraft.world.SimpleContainer belongings =
            new net.minecraft.world.SimpleContainer(54);

    public net.minecraft.world.SimpleContainer belongings() {
        return belongings;
    }

    private static final net.minecraft.network.syncher.EntityDataAccessor<CompoundTag> VISUAL =
            net.minecraft.network.syncher.SynchedEntityData.defineId(
                    CorpseEntity.class,
                    net.minecraft.network.syncher.EntityDataSerializers.COMPOUND_TAG);

    @Override
    protected void defineSynchedData(
            net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(VISUAL, new CompoundTag());
    }

    public boolean hasMedicalData() {
        // Entity asks DGH for its air limit before it finishes building synced data.
        return entityData != null && !visual().getBoolean("loot_only");
    }

    public CompoundTag visual() {
        return entityData.get(VISUAL);
    }

    public void visual(CompoundTag data) {
        entityData.set(VISUAL, data.copy());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag data) {
        super.addAdditionalSaveData(data);
        CompoundTag saved = visual().copy();
        saved.remove("source_entity"); // Runtime entity IDs must never survive a world reload.
        saved.remove("death_pose");
        saved.remove("pose_time");
        saved.remove("saved_rest_pose");
        if (com.hanmoyun.dghcompat.integration.RagdollBodies.active(this)) {
            com.hanmoyun.dghcompat.integration.DeathPose.capture(this, saved);
            if (saved.contains("death_pose")) saved.putBoolean("saved_rest_pose", true);
        } else if (visual().getBoolean("saved_rest_pose") && !isPassenger()) {
            saved.putByteArray("death_pose", visual().getByteArray("death_pose"));
            saved.putBoolean("saved_rest_pose", true);
        }
        data.put("dghcompat:corpse_visual", saved);
        data.put("dghcompat:belongings", belongings.createTag(registryAccess()));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag data) {
        super.readAdditionalSaveData(data);
        var saved = data.getCompound("dghcompat:corpse_visual");
        if (saved.getBoolean("saved_rest_pose")) saved.putLong("pose_time", level().getGameTime());
        visual(saved);
        belongings.fromTag(data.getList("dghcompat:belongings", 10), registryAccess());
        setNoAi(true);
        setInvulnerable(true);
    }

    public CorpseEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setNoAi(true);
        setInvulnerable(true);
        setPersistenceRequired();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide()) {
            DeathBelongings.refill(this, belongings);
            if (!belongings.isEmpty()) {
                for (int slot = 0; slot < belongings.getContainerSize(); slot++) {
                    var stack = belongings.getItem(slot);
                    if (!stack.isEmpty() && spawnAtLocation(stack.copy()) != null)
                        belongings.removeItemNoUpdate(slot);
                }
            }
            int xp = getPersistentData().getInt("dghcompat:stored_xp");
            if (xp > 0 && level() instanceof net.minecraft.server.level.ServerLevel level) {
                net.minecraft.world.entity.ExperienceOrb.award(level, position(), xp);
                getPersistentData().remove("dghcompat:stored_xp");
            }
        }
        setAirSupply(getMaxAirSupply());
        clearFire();
    }
}
