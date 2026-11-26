package com.hanmoyun.dghcompat.mixin;

import com.hanmoyun.dghcompat.corpse.Corpses;
import com.lastimp.dgh.common.capability.healthCore.dyingSystem.DyingHandler;

import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = DyingHandler.class, remap = false)
abstract class CorpseDeathMixin {
    @Inject(method = "setLivingDead", at = @At("HEAD"), cancellable = true)
    private static void preventSecondDeath(LivingEntity entity, CallbackInfo ci) {
        if (Corpses.isCorpse(entity)) ci.cancel();
    }

    @Redirect(
            method = "lambda$setLivingDead$2",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lcom/lastimp/dgh/common/utils/Utils;drop(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;)V"))
    private static void keepEquipment(
            net.minecraft.world.item.ItemStack stack, LivingEntity patient) {
        if (stack.is(com.lastimp.dgh.common.entry.register.ModItems.STASIS_BAG.get())) {
            com.lastimp.dgh.common.capability.HealthCapability.getAndApply(
                    patient,
                    h -> {
                        h.getAutoPulse().setStackInSlot(0, stack);
                    });
        } else com.lastimp.dgh.common.utils.Utils.drop(stack, patient);
    }

    @Inject(method = "onLivingDeath", at = @At("HEAD"))
    private static void snapshotBeforeOrganReset(LivingEntity entity, CallbackInfo ci) {
        Corpses.capture(entity);
    }
}
