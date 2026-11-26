package com.hanmoyun.dghcompat.mixin;

import com.hanmoyun.dghcompat.corpse.Corpses;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/** Let PC finish older pending deaths before it decides whether to capture this inventory. */
@Pseudo
@Mixin(targets = "com.raiiiden.ragdollifiedpc.server.CorpseManager", remap = false)
abstract class PlayerCorpseConflictMixin {
    @ModifyExpressionValue(
            method = "onDeath",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lcom/raiiiden/ragdollifiedpc/config/CorpseConfig;isEnabled()Z"))
    private static boolean skipSecondBody(
            boolean enabled,
            LivingEntity source,
            net.minecraft.world.damagesource.DamageSource damage) {
        return enabled && !Corpses.needsCorpse(source);
    }
}
