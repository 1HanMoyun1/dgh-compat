package com.hanmoyun.dghcompat.mixin;

import com.hanmoyun.dghcompat.integration.RagdollBodies;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Physics moves the original actor. Vanilla pushes cannot move an invisible second body. */
@Mixin(Entity.class)
abstract class RagdollMotionMixin {
    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    private void physicsMovement(MoverType type, Vec3 motion, CallbackInfo ci) {
        if (RagdollBodies.active((Entity) (Object) this)) ci.cancel();
    }

    @Inject(method = "push(DDD)V", at = @At("HEAD"), cancellable = true)
    private void physicsPush(double x, double y, double z, CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;
        if (!RagdollBodies.active(entity)) return;
        RagdollBodies.impulse(entity, new Vec3(x, y, z));
        ci.cancel();
    }
}
