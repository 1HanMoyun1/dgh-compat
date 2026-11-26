package com.hanmoyun.dghcompat.mixin;

import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "com.vicmatskiv.pointblank.client.ClientEventHandler", remap = false)
abstract class PointBlankClientMixin {
    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(
            method = "onClientTick",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lcom/vicmatskiv/pointblank/client/GunClientState;tryDraw(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean drawHeldGun(
            com.vicmatskiv.pointblank.client.GunClientState state,
            net.minecraft.world.entity.LivingEntity holder,
            net.minecraft.world.item.ItemStack stack,
            com.llamalad7.mixinextras.injector.wrapoperation.Operation<Boolean> original) {
        if (!(stack.getItem() instanceof com.vicmatskiv.pointblank.item.GunItem)
                && holder instanceof Player player
                && state == com.vicmatskiv.pointblank.client.GunClientState.getOffhandState(player))
            stack = player.getOffhandItem();
        return stack.getItem() instanceof com.vicmatskiv.pointblank.item.GunItem
                && original.call(state, holder, stack);
    }
}
