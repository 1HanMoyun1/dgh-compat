package com.hanmoyun.dghcompat.mixin;

import com.hanmoyun.dghcompat.CaseBook;
import com.lastimp.dgh.common.capability.healthCore.healingSystem.HealingHandler;
import com.lastimp.dgh.common.capability.healthCore.healingSystem.HealingResult;
import com.lastimp.dgh.common.enums.BodyComponents;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = HealingHandler.class, remap = false)
abstract class CaseBookHealingMixin {
    @WrapMethod(method = "useItemOn")
    private static HealingResult record(
            ItemStack stack,
            ServerPlayer doctor,
            LivingEntity patient,
            BodyComponents part,
            Operation<HealingResult> original) {
        String tool = com.hanmoyun.dghcompat.CaseBookChinese.item(stack);
        HealingResult result = original.call(stack, doctor, patient, part);
        if (result == HealingResult.SUCCESS && patient != null)
            CaseBook.record(doctor, patient, part, "使用 " + tool + " 治疗成功");
        return result;
    }
}
