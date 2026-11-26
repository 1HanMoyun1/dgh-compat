package com.hanmoyun.dghcompat.mixin;

import com.hanmoyun.dghcompat.StasisPatients;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.world.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Overlay the silent query without overwriting the entity's saved Silent flag. */
@Mixin(Entity.class)
abstract class StasisSoundMixin {
    @Unique private boolean dghcompat$readingSavedSilence;

    @Inject(method = "isSilent", at = @At("HEAD"), cancellable = true)
    private void wrappedSilent(CallbackInfoReturnable<Boolean> ci) {
        if (!dghcompat$readingSavedSilence && StasisPatients.wrapped((Entity) (Object) this))
            ci.setReturnValue(true);
    }

    /** Both queries in vanilla saving must see the original silence, including Carry On saves. */
    @WrapOperation(
            method = "saveWithoutId",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;isSilent()Z"),
            require = 2,
            allow = 2)
    private boolean dghcompat$saveOriginalSilence(Entity entity, Operation<Boolean> original) {
        boolean previous = dghcompat$readingSavedSilence;
        dghcompat$readingSavedSilence = true;
        try {
            // Preserve subclass behavior and other mods; suppress only this bridge's overlay.
            return original.call(entity);
        } finally {
            dghcompat$readingSavedSilence = previous;
        }
    }
}
