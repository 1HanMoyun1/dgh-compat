package com.hanmoyun.dghcompat.mixin;

import com.hanmoyun.dghcompat.integration.PointBlankArms;
import com.vicmatskiv.pointblank.item.GunItem;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.vicmatskiv.pointblank.client.GunClientState", remap = false)
abstract class PointBlankReloadMixin {
    @Inject(method = "tryReload", at = @At("HEAD"), cancellable = true)
    private void checkBeforeAnimation(
            LivingEntity holder, ItemStack stack, CallbackInfoReturnable<Boolean> ci) {
        if (!(holder instanceof Player player) || !(stack.getItem() instanceof GunItem)) return;
        if (PointBlankArms.reloadBlocked(player, stack)) ci.setReturnValue(false);
    }
}
