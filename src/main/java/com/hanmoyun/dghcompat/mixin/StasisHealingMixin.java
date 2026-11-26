package com.hanmoyun.dghcompat.mixin;

import com.lastimp.dgh.common.capability.healthCore.healingSystem.HealingHandler;
import com.lastimp.dgh.common.capability.healthCore.healingSystem.HealingResult;
import com.lastimp.dgh.common.enums.BodyComponents;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = HealingHandler.class, remap = false)
abstract class StasisHealingMixin {
    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private static void frozen(
            ItemStack stack,
            ServerPlayer doctor,
            LivingEntity patient,
            BodyComponents part,
            CallbackInfoReturnable<HealingResult> ci) {
        if (com.hanmoyun.dghcompat.corpse.Corpses.isCorpse(patient)) {
            if (!com.hanmoyun.dghcompat.corpse.Corpses.permitsSurgery(patient, stack))
                ci.setReturnValue(HealingResult.ITEM_NOT_AVAILABLE);
        } else if (patient != null
                && com.hanmoyun.dghcompat.corpse.Corpses.blocksTreatment(patient))
            ci.setReturnValue(HealingResult.ITEM_NOT_AVAILABLE);
    }
}
