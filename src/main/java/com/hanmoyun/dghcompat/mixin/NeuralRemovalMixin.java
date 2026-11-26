package com.hanmoyun.dghcompat.mixin;

import com.hanmoyun.dghcompat.OrganCapacity;
import com.lastimp.dgh.common.container.DynamicValidItemHandler;

import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = DynamicValidItemHandler.class, remap = false)
abstract class NeuralRemovalMixin {
    @Inject(method = "extractItem", at = @At("HEAD"), cancellable = true)
    private void keepCapacity(
            int index, int amount, boolean simulate, CallbackInfoReturnable<ItemStack> ci) {
        if (!OrganCapacity.canRemove((DynamicValidItemHandler) (Object) this, index))
            ci.setReturnValue(ItemStack.EMPTY);
    }
}
