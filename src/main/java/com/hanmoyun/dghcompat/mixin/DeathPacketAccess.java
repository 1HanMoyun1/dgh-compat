package com.hanmoyun.dghcompat.mixin;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@Mixin(targets = "com.raiiiden.ragdollified.network.RagdollSpawnPacket", remap = false)
public interface DeathPacketAccess {
    @Mutable
    @Accessor("originalEntityId")
    void dghcompat$setEntityId(int id);
}
