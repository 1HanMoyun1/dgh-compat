package com.hanmoyun.dghcompat.mixin;

import com.hanmoyun.dghcompat.integration.RagdollPolicy;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Optional Reactions 0.1.3 entry point. Other players keep the addon's normal behavior. */
@Pseudo
@Mixin(targets = "com.raiiiden.ragdollifiedreactions.server.ServerRagdolls", remap = false)
abstract class RagdollReactionsMixin {
    @Inject(method = "requestStart", at = @At("HEAD"), cancellable = true)
    private static void medicalPose(
            ServerPlayer player, @Coerce Object trigger, Vec3 impulse, CallbackInfo ci) {
        if (RagdollPolicy.medicalPose(player)) ci.cancel();
    }
}
