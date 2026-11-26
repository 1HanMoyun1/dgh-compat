package com.hanmoyun.dghcompat.mixin;

import com.hanmoyun.dghcompat.SuspendedLoadout;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Pseudo
@Mixin(
        targets = {
            "com.perigrine3.createcybernetics.common.capabilities.PlayerCyberwareData",
            "com.perigrine3.createcybernetics.common.capabilities.EntityCyberwareData"
        },
        remap = false)
abstract class SuspendedLoadoutMixin implements SuspendedLoadout {
    @Unique private CompoundTag dghcompat$inactive = new CompoundTag();

    public CompoundTag dghcompat$suspended() {
        return dghcompat$inactive;
    }

    @Inject(
            method =
                    "serializeNBT(Lnet/minecraft/core/HolderLookup$Provider;)Lnet/minecraft/nbt/CompoundTag;",
            at = @At("RETURN"))
    private void saveSuspended(
            HolderLookup.Provider provider, CallbackInfoReturnable<CompoundTag> ci) {
        if (!dghcompat$inactive.isEmpty())
            ci.getReturnValue().put("dghcompat:suspended", dghcompat$inactive.copy());
    }

    @Inject(
            method =
                    "deserializeNBT(Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/core/HolderLookup$Provider;)V",
            at = @At("TAIL"))
    private void loadSuspended(CompoundTag tag, HolderLookup.Provider provider, CallbackInfo ci) {
        dghcompat$inactive = tag.getCompound("dghcompat:suspended").copy();
    }
}
