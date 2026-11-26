package com.hanmoyun.dghcompat.mixin;

import com.hanmoyun.dghcompat.BedPose;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "dev.dyslexic.cinematic.client.compat.FirstPersonBodyCompat", remap = false)
abstract class TreatmentBodyVisibilityMixin {
    @Inject(method = "hideLocalBody", at = @At("HEAD"), cancellable = true)
    private static void allowPatientBody(CallbackInfoReturnable<Boolean> callback) {
        if (BedPose.drawingSelf) callback.setReturnValue(false);
    }
}
