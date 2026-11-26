package com.hanmoyun.dghcompat.mixin;

import com.hanmoyun.dghcompat.CyberPatientData;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(
        targets =
                "com.perigrine3.createcybernetics.item.cyberware.brain.CerebralProcessingUnitItem",
        remap = false)
abstract class CyberbrainEngramMixin {
    @Inject(method = "isBoundToPlayerName", at = @At("HEAD"), cancellable = true, require = 0)
    private static void permitCompleteEngram(
            ItemStack brain, ServerPlayer wearer, CallbackInfoReturnable<Boolean> ci) {
        if (wearer != null && CyberPatientData.hasCompleteEngram(brain)) ci.setReturnValue(true);
    }
}
