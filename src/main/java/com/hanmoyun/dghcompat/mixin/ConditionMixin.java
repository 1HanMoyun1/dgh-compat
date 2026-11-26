package com.hanmoyun.dghcompat.mixin;

import com.hanmoyun.dghcompat.LimbBridge;
import com.lastimp.dgh.common.capability.bodyPart.ConditionAccessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ConditionAccessor.class, remap = false)
abstract class ConditionMixin {
    @Inject(method = "init", at = @At("HEAD"))
    private static void registerCondition(CallbackInfo callback) {
        LimbBridge.registerCondition();
    }
}
