package com.hanmoyun.dghcompat.mixin;

import com.lastimp.dgh.common.capability.HealthCapability;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = HealthCapability.class, remap = false, priority = 1100)
abstract class StasisSurgeryMixin {
    @Inject(method = "safeSurgery", at = @At("RETURN"), cancellable = true)
    private void frozen(CallbackInfoReturnable<Boolean> ci) {
        var owner = ((com.hanmoyun.dghcompat.HealthOwner) this).dghcompat$owner();
        if (com.hanmoyun.dghcompat.corpse.Corpses.isCorpse(owner)) {
            ci.setReturnValue(com.hanmoyun.dghcompat.corpse.Corpses.onSurgeryBed(owner));
        } else if (((HealthCapability) (Object) this).isFrozen()) ci.setReturnValue(false);
    }
}
