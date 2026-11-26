package com.hanmoyun.dghcompat.mixin;

import com.hanmoyun.dghcompat.TreatmentScene;

import dev.dyslexic.cinematic.client.Scene;

import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "dev.dyslexic.cinematic.client.Hands", remap = false)
abstract class TreatmentHandsMixin {
    @org.spongepowered.asm.mixin.Shadow private static boolean leftWorks;

    @Inject(method = "follow", at = @At("HEAD"))
    private static void chooseHand(
            dev.dyslexic.cinematic.client.ClientStation station,
            Vec3 camera,
            float yaw,
            double dt,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfo callback) {
        if (station instanceof TreatmentScene treatment) leftWorks = treatment.worksLeft();
    }

    @org.spongepowered.asm.mixin.injection.Redirect(
            method = "follow",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Ldev/dyslexic/cinematic/client/ClientStation;lateral(Lnet/minecraft/world/phys/Vec3;)D"))
    private static double keepChosenHand(
            dev.dyslexic.cinematic.client.ClientStation station, Vec3 aim) {
        return station instanceof TreatmentScene treatment
                ? (treatment.worksLeft() ? -1 : 1)
                : station.lateral(aim);
    }

    @Inject(
            method =
                    "drawArm(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;ZILnet/minecraft/world/phys/Vec3;)V",
            at = @At("HEAD"),
            cancellable = true)
    private static void hideUnavailableArm(
            com.mojang.blaze3d.vertex.PoseStack pose,
            net.minecraft.client.renderer.MultiBufferSource buffers,
            Vec3 camera,
            Vec3 hand,
            Vec3 shoulder,
            boolean right,
            int light,
            Vec3 forward,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfo callback) {
        if (Scene.station instanceof TreatmentScene treatment && !treatment.showArm(right))
            callback.cancel();
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private static void hideHands(
            com.mojang.blaze3d.vertex.PoseStack pose,
            net.minecraft.client.renderer.MultiBufferSource buffers,
            Vec3 camera,
            float yaw,
            int light,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfo callback) {
        if (Scene.station instanceof TreatmentScene treatment
                && (Scene.phase != Scene.Phase.ACTIVE
                        || !treatment.showArm(true) && !treatment.showArm(false))) callback.cancel();
    }

    @Inject(method = "shoulder", at = @At("HEAD"), cancellable = true)
    private static void useLyingShoulder(
            boolean right, Vec3 camera, float yaw, CallbackInfoReturnable<Vec3> callback) {
        if (Scene.station instanceof TreatmentScene treatment) {
            callback.setReturnValue(treatment.shoulder(right));
        }
    }
}
