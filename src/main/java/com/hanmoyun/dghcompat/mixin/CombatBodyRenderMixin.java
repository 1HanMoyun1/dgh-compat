package com.hanmoyun.dghcompat.mixin;

import com.hanmoyun.dghcompat.ArmState;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Combat animations may change part visibility after setupAnim. Mask geometry for this draw only. */
@Mixin(LivingEntityRenderer.class)
abstract class CombatBodyRenderMixin {
    @WrapOperation(
            method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"))
    private void maskMissingArms(EntityModel<?> model, PoseStack pose, VertexConsumer vertices,
            int light, int overlay, int color, Operation<Void> draw, @Local(argsOnly = true) LivingEntity entity) {
        if (!(entity instanceof Player player) || !(model instanceof PlayerModel<?> body)) {
            draw.call(model, pose, vertices, light, overlay, color);
            return;
        }
        boolean left = body.leftArm.skipDraw, right = body.rightArm.skipDraw;
        boolean leftSleeve = body.leftSleeve.skipDraw, rightSleeve = body.rightSleeve.skipDraw;
        try {
            if (!ArmState.visible(player, HumanoidArm.LEFT)) {
                body.leftArm.skipDraw = true;
                body.leftSleeve.skipDraw = true;
            }
            if (!ArmState.visible(player, HumanoidArm.RIGHT)) {
                body.rightArm.skipDraw = true;
                body.rightSleeve.skipDraw = true;
            }
            draw.call(model, pose, vertices, light, overlay, color);
        } finally {
            body.leftArm.skipDraw = left;
            body.rightArm.skipDraw = right;
            body.leftSleeve.skipDraw = leftSleeve;
            body.rightSleeve.skipDraw = rightSleeve;
        }
    }
}
