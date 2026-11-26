package com.hanmoyun.dghcompat.mixin;

import com.hanmoyun.dghcompat.TreatmentScene;

import dev.dyslexic.cinematic.client.CameraRig;
import dev.dyslexic.cinematic.client.ClientStation;
import dev.dyslexic.cinematic.client.Scene;

import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "dev.dyslexic.cinematic.client.CameraRig", remap = false)
abstract class TreatmentCameraMixin {
    @Shadow private static CameraRig.Pose last;

    @Inject(method = "tether", at = @At("HEAD"), cancellable = true)
    private static void useBedEyeDuringApproach(
            CameraRig.Pose pose,
            Vec3 eye,
            ClientStation station,
            CallbackInfoReturnable<CameraRig.Pose> callback) {
        if (station instanceof TreatmentScene treatment) {
            callback.setReturnValue(treatment.lean());
        }
    }

    @Inject(method = "override", at = @At("RETURN"), cancellable = true)
    private static void keepBedEye(
            Vec3 position,
            float yaw,
            float pitch,
            CallbackInfoReturnable<CameraRig.Pose> callback) {
        if (Scene.phase == Scene.Phase.ACTIVE
                && Scene.station instanceof TreatmentScene treatment) {
            last = treatment.lean();
            callback.setReturnValue(last);
        }
    }
}
