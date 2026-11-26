package com.hanmoyun.dghcompat.mixin;

import com.hanmoyun.dghcompat.BodyItems;
import com.lastimp.dgh.common.capability.bodyPart.base.AbstractVisibleBody;
import com.lastimp.dgh.common.capability.healthCore.damageSystem.FollowInjuryHandler;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = FollowInjuryHandler.class, remap = false)
abstract class TraumaticLimbDropMixin {
    @Redirect(
            method = "traumaticAmputationHandler",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lcom/lastimp/dgh/common/utils/Utils;drop(Lnet/minecraft/world/item/Item;Lnet/minecraft/world/entity/LivingEntity;I)V"),
            require = 0)
    private static void droppedPart(
            Item item,
            LivingEntity source,
            int count,
            LivingEntity patient,
            AbstractVisibleBody body,
            com.lastimp.dgh.common.capability.HealthCapability health,
            float damage,
            float threshold,
            float factor,
            float min,
            float max) {
        BodyItems.dropLimb(item, patient, body);
    }
}
