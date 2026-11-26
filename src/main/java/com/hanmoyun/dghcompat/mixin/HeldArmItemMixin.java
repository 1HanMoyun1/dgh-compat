package com.hanmoyun.dghcompat.mixin;

import com.hanmoyun.dghcompat.ArmState;

import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandLayer.class)
abstract class HeldArmItemMixin {
    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    private void hideDisabledItem(
            LivingEntity holder,
            ItemStack stack,
            ItemDisplayContext context,
            HumanoidArm arm,
            com.mojang.blaze3d.vertex.PoseStack poses,
            net.minecraft.client.renderer.MultiBufferSource buffers,
            int light,
            CallbackInfo ci) {
        if (holder instanceof Player player && !ArmState.usable(player, arm)) ci.cancel();
    }
}
