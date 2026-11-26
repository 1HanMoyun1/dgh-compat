package com.hanmoyun.dghcompat.mixin;

import com.hanmoyun.dghcompat.StasisPatients;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "tschipp.carryon.client.render.CarryRenderHelper", remap = false)
abstract class StasisCarryPoseMixin {
    @Inject(method = "applyEntityTransformations", at = @At("HEAD"), cancellable = true)
    private static void bag(
            Player player, float tick, PoseStack pose, Entity entity, CallbackInfo ci) {
        if (!StasisPatients.wrapped(entity)) return;
        // Match Carry On's two pushes; its caller owns the corresponding pops.
        pose.pushPose();
        var camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        var pos = player.getPosition(tick).subtract(camera);
        pose.translate(pos.x, pos.y, pos.z);
        pose.mulPose(
                Axis.YP.rotationDegrees(-Mth.rotLerp(tick, player.yBodyRotO, player.yBodyRot)));
        pose.pushPose();
        pose.translate(0, player.isCrouching() ? 0.85 : 1.15, 0.65);
        entity.getPersistentData().putBoolean("dghcompat:carry_render", true);
        ci.cancel();
    }
}
