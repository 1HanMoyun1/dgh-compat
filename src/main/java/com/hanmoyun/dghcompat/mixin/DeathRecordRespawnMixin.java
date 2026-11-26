package com.hanmoyun.dghcompat.mixin;

import com.lastimp.dgh.common.capability.HealthCapability;
import com.lastimp.dgh.common.event.eventHandler.PlayerEventHandler;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = PlayerEventHandler.class, remap = false)
abstract class DeathRecordRespawnMixin {
    @WrapOperation(
            method = "lambda$onPlayerRespawn$0",
            at = @At(value = "INVOKE", target = "Lcom/lastimp/dgh/common/capability/HealthCapability;deserialize(Lnet/minecraft/core/HolderLookup$Provider;Lnet/minecraft/nbt/CompoundTag;)V"))
    private static void restoreRecord(HealthCapability health, HolderLookup.Provider registries,
            CompoundTag data, Operation<Void> original, @Local(argsOnly = true) String key) {
        if (HealthCapability.HEALTH_RECORD.equals(key)) {
            health.respawnDeserializeNBT(registries, data);
        } else {
            original.call(health, registries, data);
        }
    }
}
