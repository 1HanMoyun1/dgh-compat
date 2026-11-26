package com.hanmoyun.dghcompat.corpse;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class NativePatients {
    private NativePatients() {}

    public static Entity actual(Entity entity) {
        return entity;
    }

    public static LivingEntity resolve(Entity entity) {
        return entity instanceof LivingEntity living ? living : null;
    }
}
