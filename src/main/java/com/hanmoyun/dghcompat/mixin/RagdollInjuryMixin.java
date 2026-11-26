package com.hanmoyun.dghcompat.mixin;

import com.hanmoyun.dghcompat.integration.NativeRagdollBodies;
import com.hanmoyun.dghcompat.integration.RagdollBodies;
import com.lastimp.dgh.common.capability.bodyPart.base.AbstractVisibleBody;
import com.lastimp.dgh.common.capability.healthCore.damageSystem.InjuryDataSet;
import com.lastimp.dgh.common.capability.healthCore.damageSystem.InjuryEventHandler;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = InjuryEventHandler.class, remap = false)
abstract class RagdollInjuryMixin {
    // DGH also has a shorter overload that delegates to this one.
    @Inject(
            method =
                    "handleSimpleHit(Lnet/minecraft/world/damagesource/DamageSource;"
                        + "Lnet/minecraft/world/entity/LivingEntity;F"
                        + "Lcom/lastimp/dgh/common/capability/healthCore/damageSystem/InjuryDataSet$InjuryHandler;)"
                        + "Lcom/lastimp/dgh/common/capability/healthCore/damageSystem/InjuryDataSet;",
            at = @At("HEAD"),
            cancellable = true,
            require = 0)
    private static void movingPart(
            DamageSource source,
            LivingEntity patient,
            float factor,
            InjuryDataSet.InjuryHandler<AbstractVisibleBody> handler,
            CallbackInfoReturnable<InjuryDataSet> ci) {
        if (patient.level().isClientSide()
                || !RagdollBodies.active(patient)
                || !RagdollBodies.available()) return;
        var hit = NativeRagdollBodies.hitPart(patient, source);
        if (hit == null) return;

        var injuryPart = InjuryEventHandler.getAttackPart(source.getEntity(), patient);
        ci.setReturnValue(
                InjuryDataSet.create(injuryPart, InjuryDataSet.createData(hit, factor, handler)));
    }
}
