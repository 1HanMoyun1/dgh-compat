package com.hanmoyun.dghcompat.mixin;

import com.hanmoyun.dghcompat.BedPose;
import com.lastimp.dgh.common.block.OperatingBedBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
abstract class TreatmentPlayerRendererMixin {
    @Inject(
            method =
                    "setupRotations(Lnet/minecraft/client/player/AbstractClientPlayer;Lcom/mojang/blaze3d/vertex/PoseStack;FFFF)V",
            at = @At("HEAD"),
            cancellable = true)
    private void rotateBody(
            AbstractClientPlayer player,
            PoseStack pose,
            float bob,
            float yaw,
            float partialTick,
            float scale,
            CallbackInfo callback) {
        var bed = BedPose.bed(player);
        if (bed == null) return;
        var state = player.level().getBlockState(bed);
        if (!(state.getBlock() instanceof OperatingBedBlock)) return;
        pose.mulPose(Axis.YP.rotationDegrees(-state.getValue(OperatingBedBlock.FACING).toYRot()));
        pose.mulPose(Axis.XP.rotationDegrees(90));
        callback.cancel();
    }
}
