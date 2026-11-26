package com.hanmoyun.dghcompat;

import net.minecraft.nbt.CompoundTag;

/** Stored with the corresponding Cybernetics attachment, including when it is cloned. */
public interface SuspendedLoadout {
    CompoundTag dghcompat$suspended();
}
