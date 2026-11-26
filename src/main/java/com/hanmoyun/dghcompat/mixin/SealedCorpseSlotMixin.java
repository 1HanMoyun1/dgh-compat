package com.hanmoyun.dghcompat.mixin;

import com.hanmoyun.dghcompat.corpse.SealedBodyBags;
import com.lastimp.dgh.common.container.DynamicSlot;
import com.lastimp.dgh.common.container.IItemHandler;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(value = DynamicSlot.class, remap = false)
abstract class SealedCorpseSlotMixin {
    @Shadow private IItemHandler handler;

    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void pickup(Player player, CallbackInfoReturnable<Boolean> ci) {
        if (SealedBodyBags.locked(handler)) ci.setReturnValue(false);
    }

    @Inject(method = "remove", at = @At("HEAD"), cancellable = true)
    private void remove(int amount, CallbackInfoReturnable<ItemStack> ci) {
        if (SealedBodyBags.locked(handler)) ci.setReturnValue(ItemStack.EMPTY);
    }

    @Inject(method = "set", at = @At("HEAD"), cancellable = true)
    private void replace(ItemStack stack, CallbackInfo ci) {
        if (SealedBodyBags.locked(handler)
                && (!ItemStack.isSameItemSameComponents(stack, handler.getStackInSlot(0))
                        || stack.getCount() != handler.getStackInSlot(0).getCount())) ci.cancel();
    }
}
