package com.hanmoyun.dghcompat.mixin;

import com.hanmoyun.dghcompat.integration.PointBlankArms;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.vicmatskiv.pointblank.item.GunItem", remap = false)
abstract class PointBlankGunMixin {
    @ModifyExpressionValue(
            method = "shouldUseRightHandOnlyMainGrip",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isEmpty()Z"))
    private static boolean supportHandEmpty(boolean empty, Player player) {
        return empty && !PointBlankArms.disabled(player, false);
    }

    @Inject(method = "resolveMainHandGunContext", at = @At("HEAD"), cancellable = true)
    private static void mainHand(Player player, CallbackInfoReturnable<?> ci) {
        if (PointBlankArms.disabled(player, true)) ci.setReturnValue(null);
    }

    @Inject(method = "resolveOffhandGunContext", at = @At("HEAD"), cancellable = true)
    private static void offHand(Player player, CallbackInfoReturnable<?> ci) {
        if (PointBlankArms.disabled(player, false)) ci.setReturnValue(null);
    }

    @Inject(method = "resolveServerActionStack", at = @At("HEAD"), cancellable = true)
    private static void checkServerHand(
            ServerPlayer player,
            int slot,
            java.util.UUID id,
            CallbackInfoReturnable<ItemStack> ci) {
        if (PointBlankArms.disabled(player, slot != 40)) ci.setReturnValue(null);
    }

    @Inject(method = "isReloadBlockedByOccupiedOffhand", at = @At("HEAD"), cancellable = true)
    private static void reportOccupied(
            Player player, ItemStack stack, CallbackInfoReturnable<Boolean> ci) {
        if (player != null
                && stack != null
                && !stack.isEmpty()
                && stack == player.getMainHandItem()
                && PointBlankArms.disabled(player, false)
                && PointBlankArms.reloadBlocked(player, stack)) ci.setReturnValue(true);
    }

    @Inject(method = "canReloadInCurrentHands", at = @At("HEAD"), cancellable = true)
    private static void reloadHands(
            Player player, ItemStack stack, CallbackInfoReturnable<Boolean> ci) {
        if (PointBlankArms.reloadBlocked(player, stack)) ci.setReturnValue(false);
    }

    @Inject(
            method = {"canReloadGun", "reloadGun"},
            at = @At("HEAD"),
            cancellable = true)
    private void reloadAmmo(
            ItemStack stack,
            Player player,
            @Coerce Object mode,
            CallbackInfoReturnable<Integer> ci) {
        if (PointBlankArms.reloadBlocked(player, stack)) ci.setReturnValue(0);
    }
}
