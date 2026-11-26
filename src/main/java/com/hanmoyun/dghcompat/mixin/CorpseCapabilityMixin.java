package com.hanmoyun.dghcompat.mixin;

import com.hanmoyun.dghcompat.corpse.CorpseEntity;
import com.hanmoyun.dghcompat.corpse.NativePatients;
import com.lastimp.dgh.neoforge.capability.CapabilityHelperNF;

import net.minecraft.world.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The bridge-owned carrier must have health even when absent from DGH's species allow-list. */
@Mixin(value = CapabilityHelperNF.class, remap = false)
abstract class CorpseCapabilityMixin {
    @Inject(method = "hasHealth", at = @At("HEAD"), cancellable = true)
    private void corpse(Entity entity, CallbackInfoReturnable<Boolean> ci) {
        if (entity instanceof CorpseEntity corpse) ci.setReturnValue(corpse.hasMedicalData());
    }
}
