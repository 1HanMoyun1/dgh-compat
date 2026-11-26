package com.hanmoyun.dghcompat.mixin;

import com.hanmoyun.dghcompat.CompatBridge;
import com.lastimp.dgh.common.capability.HealthCapability;

import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = HealthCapability.class, remap = false)
public abstract class HealthMixin implements com.hanmoyun.dghcompat.HealthOwner {
    @Unique private LivingEntity dghcyber$owner;

    @Override
    public LivingEntity dghcompat$owner() {
        return dghcyber$owner;
    }

    @Inject(method = "update", at = @At("HEAD"), cancellable = true)
    private void dghcyber$bind(LivingEntity entity, CallbackInfoReturnable<HealthCapability> cir) {
        dghcyber$owner = entity;
        var capability = (HealthCapability) (Object) this;
        for (var part : com.lastimp.dgh.common.enums.BodyComponents.VISIBLE_BODIES)
            com.hanmoyun.dghcompat.OrganCapacity.bind(capability, capability.getComponent(part));
        com.hanmoyun.dghcompat.BodyItems.identifyOrgans((HealthCapability) (Object) this, entity);
        if (com.hanmoyun.dghcompat.corpse.Corpses.isCorpse(entity)) {
            var health = (HealthCapability) (Object) this;
            com.hanmoyun.dghcompat.corpse.SealedBodyBags.bind(health);
            for (var part : com.lastimp.dgh.common.enums.BodyComponents.values())
                ((CorpseDisplayAccess) health.getComponent(part)).dghcompat$display(health);
            cir.setReturnValue((HealthCapability) (Object) this);
            return;
        }
        CompatBridge.bind((HealthCapability) (Object) this, entity);
    }

    @Inject(method = "safeSurgery", at = @At("HEAD"), cancellable = true)
    private void dghcyber$safeTable(CallbackInfoReturnable<Boolean> cir) {
        if (dghcyber$owner != null && CompatBridge.onCyberSurgeryBed(dghcyber$owner))
            cir.setReturnValue(true);
    }
}
