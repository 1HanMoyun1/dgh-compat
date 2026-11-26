package com.hanmoyun.dghcompat.mixin;

import com.hanmoyun.dghcompat.corpse.Corpses;
import com.lastimp.dgh.common.item.bases.AbstractHealingItem;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AbstractHealingItem.class, remap = false)
abstract class CorpseItemMixin {
    @Inject(method = "available", at = @At("HEAD"), cancellable = true)
    private void dissect(LivingEntity target, ItemStack stack, CallbackInfoReturnable<Boolean> ci) {
        if (Corpses.isCorpse(target)) ci.setReturnValue(Corpses.permitsSurgery(target, stack));
    }
}
