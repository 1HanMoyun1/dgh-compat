package com.hanmoyun.dghcompat.corpse;

import net.minecraft.nbt.CompoundTag;

public interface PendingHealth {
    CompoundTag dghcompat$health();

    void dghcompat$health(CompoundTag tag);
}
