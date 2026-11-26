package com.hanmoyun.dghcompat.mixin;

import com.hanmoyun.dghcompat.client.CorpseVisuals;
import com.hanmoyun.dghcompat.corpse.CorpseEntity;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Native capture needs the original species renderer, not the medical carrier's model. */
@Pseudo
@Mixin(targets = "com.raiiiden.ragdollified.network.RagdollSpawnPacket", remap = false)
abstract class NativeRagdollSpawnMixin {
    @Redirect(
            method = "handleClient",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/multiplayer/ClientLevel;getEntity(I)Lnet/minecraft/world/entity/Entity;"))
    private static Entity originalVisual(ClientLevel level, int id) {
        var entity = level.getEntity(id);
        return entity instanceof CorpseEntity corpse ? CorpseVisuals.get(corpse) : entity;
    }
}
