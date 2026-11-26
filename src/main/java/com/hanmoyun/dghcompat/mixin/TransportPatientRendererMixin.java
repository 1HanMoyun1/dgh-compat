package com.hanmoyun.dghcompat.mixin;

import com.hanmoyun.dghcompat.TransportOrientation;
import com.lastimp.dgh.common.entity.StretcherEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Transport replaces the rotation call, so DGH's ground-collapse callback cannot cancel it first.
 */
@Mixin(LivingEntityRenderer.class)
abstract class TransportPatientRendererMixin {
    @Shadow
    protected abstract void setupRotations(
            LivingEntity patient, PoseStack pose, float bob, float yaw, float tick, float scale);

    @Redirect(
            method =
                    "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;setupRotations(Lnet/minecraft/world/entity/LivingEntity;Lcom/mojang/blaze3d/vertex/PoseStack;FFFF)V"))
    private void patientPose(
            LivingEntityRenderer<?, ?> renderer,
            LivingEntity patient,
            PoseStack pose,
            float bob,
            float yaw,
            float tick,
            float scale) {
        if (patient.getVehicle() instanceof StretcherEntity carrier) {
            pose.mulPose(
                    Axis.YP.rotationDegrees(
                            TransportOrientation.modelYaw(
                                    carrier.getYRot(), carrier.isInvisible())));
            pose.mulPose(Axis.XP.rotationDegrees(90));
        } else if (com.hanmoyun.dghcompat.integration.RagdollBodies.desired(patient)
                && com.hanmoyun.dghcompat.client.RagdollAdapter.supported(patient)) {
            // Keep the fall's source pose upright. DGH's ground callback rotates it by 90
            // degrees immediately, including during native physics pose capture.
            pose.mulPose(Axis.YP.rotationDegrees(180 - yaw));
        } else {
            setupRotations(patient, pose, bob, yaw, tick, scale);
        }
    }
}
