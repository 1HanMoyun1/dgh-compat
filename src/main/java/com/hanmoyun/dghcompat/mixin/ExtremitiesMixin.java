package com.hanmoyun.dghcompat.mixin;

import com.hanmoyun.dghcompat.CompatBridge;
import com.lastimp.dgh.common.capability.HealthCapability;
import com.lastimp.dgh.common.capability.bodyPart.base.AbstractBody;
import com.lastimp.dgh.common.capability.bodyPart.base.AbstractExtremities;
import com.lastimp.dgh.common.capability.bodyPart.base.BodyCondition;

import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AbstractExtremities.class, remap = false)
public abstract class ExtremitiesMixin {
    @Inject(
            method = "available(Lcom/lastimp/dgh/common/capability/HealthCapability;)Z",
            at = @At("HEAD"),
            cancellable = true)
    private void dghcyber$available(HealthCapability health, CallbackInfoReturnable<Boolean> cir) {
        AbstractBody body = (AbstractBody) (Object) this;
        if (missing(body)) return;
        var snapshot = CompatBridge.snapshot(body);
        if (snapshot.hasLimb(body.getBodyType().name()))
            cir.setReturnValue(snapshot.workingLimb(body.getBodyType().name()));
    }

    @Inject(method = "visible()Z", at = @At("HEAD"), cancellable = true)
    private void dghcyber$visible(CallbackInfoReturnable<Boolean> cir) {
        AbstractBody body = (AbstractBody) (Object) this;
        if (!missing(body) && CompatBridge.snapshot(body).hasLimb(body.getBodyType().name()))
            cir.setReturnValue(true);
    }

    @Inject(method = "updateVitalityLost", at = @At("RETURN"), cancellable = true)
    private void dghcyber$amputationLoss(
            HealthCapability health, LivingEntity entity, CallbackInfoReturnable<Float> cir) {
        AbstractBody body = (AbstractBody) (Object) this;
        if (missing(body) || !CompatBridge.snapshot(body).workingLimb(body.getBodyType().name()))
            return;
        // Remove only the amputation floor; preserve all of AbstractVisibleBody's wound losses.
        float wounds =
                body.getCondition(BodyCondition.BURN).getTotalValue()
                        + body.getCondition(BodyCondition.OPEN_WOUND).getTotalValue()
                        + body.getCondition(BodyCondition.PASS_THROUGH).getTotalValue()
                        + body.getCondition(BodyCondition.INTERNAL_INJURY).getTotalValue();
        cir.setReturnValue(wounds);
    }

    @org.spongepowered.asm.mixin.Unique
    private static boolean missing(AbstractBody body) {
        return body.abnormal(BodyCondition.SURGICAL_AMPUTATION)
                || body.abnormal(BodyCondition.TRAUMATIC_AMPUTATION);
    }
}
