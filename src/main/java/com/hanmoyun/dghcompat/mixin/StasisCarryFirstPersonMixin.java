package com.hanmoyun.dghcompat.mixin;

import com.hanmoyun.dghcompat.StasisPatients;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import tschipp.carryon.client.render.CarryRenderHelper;

@Pseudo
@Mixin(targets = "tschipp.carryon.client.render.CarriedObjectRender", remap = false)
abstract class StasisCarryFirstPersonMixin {
    @Inject(method = "drawFirstPersonEntity", at = @At("HEAD"), cancellable = true)
    private static void bag(
            Player player,
            MultiBufferSource buffer,
            PoseStack pose,
            int light,
            float tick,
            CallbackInfo ci) {
        var entity = CarryRenderHelper.getRenderEntity(player);
        if (entity == null || !StasisPatients.wrapped(entity)) return;
        pose.pushPose();
        try {
            pose.translate(0, -0.5, -0.85);
            entity.getPersistentData().putBoolean("dghcompat:carry_render", true);
            Minecraft.getInstance()
                    .getEntityRenderDispatcher()
                    .render(entity, 0, 0, 0, 0, 0, pose, buffer, light);
        } finally {
            pose.popPose();
        }
        ci.cancel();
    }
}
