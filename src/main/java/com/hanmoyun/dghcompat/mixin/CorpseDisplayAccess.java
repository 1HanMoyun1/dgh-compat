package com.hanmoyun.dghcompat.mixin;

import com.lastimp.dgh.common.capability.HealthCapability;
import com.lastimp.dgh.common.capability.bodyPart.base.AbstractBody;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = AbstractBody.class, remap = false)
public interface CorpseDisplayAccess {
    @Invoker("updateDisplayValue")
    void dghcompat$display(HealthCapability health);
}
