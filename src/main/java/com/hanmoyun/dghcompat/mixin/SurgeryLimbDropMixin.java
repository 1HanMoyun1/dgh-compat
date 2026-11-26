package com.hanmoyun.dghcompat.mixin;

import com.hanmoyun.dghcompat.LimbBridge;
import com.lastimp.dgh.common.capability.bodyPart.base.AbstractExtremities;
import com.lastimp.dgh.common.item.tool.SurgerySaw;
import com.lastimp.dgh.common.utils.Utils;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(value = SurgerySaw.class, remap = false)
public class SurgeryLimbDropMixin {
    @org.spongepowered.asm.mixin.Unique
    private static final ThreadLocal<LivingEntity> dghcompat$patient = new ThreadLocal<>();

    @com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod(
            method =
                    "healOn(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/LivingEntity;Lcom/lastimp/dgh/common/enums/BodyComponents;)Z")
    private boolean harvestContext(
            LivingEntity surgeon,
            LivingEntity patient,
            com.lastimp.dgh.common.enums.BodyComponents part,
            com.llamalad7.mixinextras.injector.wrapoperation.Operation<Boolean> original) {
        var previous = dghcompat$patient.get();
        dghcompat$patient.set(patient);
        try {
            return original.call(surgeon, patient, part);
        } finally {
            if (previous == null) dghcompat$patient.remove();
            else dghcompat$patient.set(previous);
        }
    }

    @Redirect(
            method = "cut",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lcom/lastimp/dgh/common/utils/Utils;drop(Lnet/minecraft/world/item/Item;Lnet/minecraft/world/entity/LivingEntity;I)V"))
    private void recipient(
            Item item,
            LivingEntity source,
            int amount,
            LivingEntity surgeon,
            AbstractExtremities body) {

        var patient = dghcompat$patient.get();
        if (patient != null) {
            com.hanmoyun.dghcompat.BodyItems.dropLimb(item, patient, body);
            return;
        }
        Utils.drop(item, source, amount);
    }

    @Redirect(
            method = "saw",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lcom/lastimp/dgh/common/utils/Utils;drop(Lnet/minecraft/world/item/Item;Lnet/minecraft/world/entity/LivingEntity;I)V"))
    private void noOrganicBone(
            Item item,
            LivingEntity source,
            int amount,
            LivingEntity surgeon,
            com.lastimp.dgh.common.capability.bodyPart.base.AbstractVisibleBody body) {
        if (body instanceof AbstractExtremities && LimbBridge.isCybernetic(body)) return;
        Utils.drop(item, source, amount);
    }
}
