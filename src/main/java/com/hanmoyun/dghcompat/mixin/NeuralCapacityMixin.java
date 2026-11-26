package com.hanmoyun.dghcompat.mixin;

import com.hanmoyun.dghcompat.OrganCapacity;
import com.lastimp.dgh.common.capability.bodyPart.base.AbstractBody;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AbstractBody.class, remap = false)
abstract class NeuralCapacityMixin {
    @Inject(method = "organ1Level()I", at = @At("HEAD"), cancellable = true)
    private void actualNeuralSlots(CallbackInfoReturnable<Integer> ci) {
        Integer slots = OrganCapacity.firstFor((AbstractBody) (Object) this);
        if (slots != null) ci.setReturnValue(slots);
    }
}
