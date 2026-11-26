package com.hanmoyun.dghcompat.mixin;

import com.hanmoyun.dghcompat.CompatMod;
import com.hanmoyun.dghcompat.corpse.Corpses;
import com.hanmoyun.dghcompat.integration.NativeRagdollBodies;
import com.raiiiden.ragdollified.network.RagdollSpawnPacket;

import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.raiiiden.ragdollified.server.ServerRagdollSyncManager", remap = false)
abstract class MedicalDeathPhysicsMixin {
    @Inject(method = "registerDeath", at = @At("HEAD"), cancellable = true)
    private static void handOffMedicalDeath(
            LivingEntity source, RagdollSpawnPacket packet, CallbackInfo ci) {
        if (Corpses.needsCorpse(source)) {
            NativeRagdollBodies.rememberDeath(source, packet);
            ci.cancel();
        }
    }
}
