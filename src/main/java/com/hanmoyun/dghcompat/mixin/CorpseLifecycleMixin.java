package com.hanmoyun.dghcompat.mixin;

import com.hanmoyun.dghcompat.corpse.Corpses;
import com.lastimp.dgh.common.capability.HealthCapability;
import com.lastimp.dgh.common.capability.healthCore.healingSystem.HealingEventHandler;

import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = HealingEventHandler.class, remap = false)
abstract class CorpseLifecycleMixin {
    @Inject(method = "onHealthUpdate", at = @At("HEAD"), cancellable = true)
    private static void preserve(LivingEntity entity, CallbackInfo ci) {
        if (!Corpses.isCorpse(entity)) return;
        // This injection precedes DGH's own side guard. Clients must never enter SYNIfDirty.
        if (entity.level().isClientSide()) {
            ci.cancel();
            return;
        }
        HealthCapability.getAndApply(
                entity,
                h -> {
                    h.update(entity); // Binds owner through the bridge; no physiological update for
                    // corpses.
                    h.SYNIfDirty(entity);
                });
        ci.cancel();
    }
}
