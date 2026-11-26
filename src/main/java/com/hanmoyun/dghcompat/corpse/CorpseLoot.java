package com.hanmoyun.dghcompat.corpse;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;

/** Converts saved 2.4.45 loot entries to ordinary drops on load. */
public final class CorpseLoot extends ItemEntity {
    public CorpseLoot(EntityType<? extends ItemEntity> type, Level level) {
        super(type, level);
        setNeverPickUp();
        setInvulnerable(true);
        setNoGravity(true);
    }

    @Override
    public void tick() {
        if (level().isClientSide()) return;
        // A dismounted entry, or an old world opened without Better Looting, becomes a normal drop.
        if (getItem().isEmpty() || spawnAtLocation(getItem().copy()) != null) discard();
    }

    public static void stow(CorpseEntity body) {
        for (var passenger : java.util.List.copyOf(body.getPassengers())) {
            if (passenger instanceof CorpseLoot entry && !entry.isRemoved()) {
                DeathBelongings.store(body, body.belongings(), entry.getItem().copy());
                entry.discard();
            }
        }
    }
}
