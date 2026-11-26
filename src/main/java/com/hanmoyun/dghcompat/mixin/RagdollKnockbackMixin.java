package com.hanmoyun.dghcompat.mixin;

import com.hanmoyun.dghcompat.integration.RagdollBodies;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
abstract class RagdollKnockbackMixin {
    @Inject(method = "knockback", at = @At("HEAD"), cancellable = true)
    private void physicsKnockback(double strength, double x, double z, CallbackInfo ci) {
        var entity = (LivingEntity) (Object) this;
        if (!RagdollBodies.active(entity)) return;
        var direction = new Vec3(-x, 0, -z).normalize();
        RagdollBodies.impulse(entity, direction.scale(strength).add(0, strength * 0.25, 0));
        ci.cancel();
    }
}
