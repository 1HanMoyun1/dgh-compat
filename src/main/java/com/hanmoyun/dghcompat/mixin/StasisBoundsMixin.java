package com.hanmoyun.dghcompat.mixin;

import com.hanmoyun.dghcompat.StasisGeometry;
import com.hanmoyun.dghcompat.StasisPatients;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Horizontal adult shell bounds, shared by server collision and client ray picking. */
@Mixin(Entity.class)
abstract class StasisBoundsMixin {
    @Inject(method = "makeBoundingBox", at = @At("HEAD"), cancellable = true)
    private void bagBounds(CallbackInfoReturnable<AABB> ci) {
        Entity entity = (Entity) (Object) this;
        if (entity.getPersistentData().getBoolean(StasisPatients.WRAPPED)) {
            ci.setReturnValue(StasisGeometry.bounds(entity));
        } else {
            var body = com.hanmoyun.dghcompat.integration.RagdollBodies.bounds(entity);
            if (body != null) ci.setReturnValue(body);
        }
    }
}
