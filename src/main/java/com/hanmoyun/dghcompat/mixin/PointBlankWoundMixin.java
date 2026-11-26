package com.hanmoyun.dghcompat.mixin;

import com.hanmoyun.dghcompat.integration.PointBlankHit;
import com.lastimp.dgh.common.capability.healthCore.damageSystem.InjuryDataSet;
import com.lastimp.dgh.common.capability.healthCore.damageSystem.InjuryEventHandler;
import com.lastimp.dgh.common.capability.healthCore.damageSystem.subHandler.PassThroughHandler;
import com.lastimp.dgh.common.enums.InjuryPart;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = InjuryEventHandler.class, remap = false)
abstract class PointBlankWoundMixin {
    @Inject(method = "getInjuryData", at = @At("HEAD"), cancellable = true)
    private static void gunshot(
            LivingEntity patient, DamageSource source, CallbackInfoReturnable<InjuryDataSet> ci) {
        var hit = PointBlankHit.CURRENT.get();
        if (hit == null || hit.patient() != patient || hit.source() != source) return;
        var region =
                switch (hit.part()) {
                    case HEAD -> InjuryPart.HEAD;
                    case LEFT_LEG, RIGHT_LEG -> InjuryPart.FEET;
                    default -> InjuryPart.BODY;
                };
        ci.setReturnValue(
                InjuryDataSet.create(
                        region,
                        InjuryDataSet.createData(
                                hit.part(), 1, PassThroughHandler::handleEntityAttack)));
    }
}
