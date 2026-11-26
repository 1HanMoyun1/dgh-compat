package com.hanmoyun.dghcompat.mixin;

import com.hanmoyun.dghcompat.integration.RagdollBodies;

import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Ordinary player movement cannot override the elected physics stream while downed. */
@Mixin(ServerGamePacketListenerImpl.class)
abstract class RagdollPlayerMovementMixin {
    @Shadow public ServerPlayer player;

    @Inject(method = "handleMovePlayer", at = @At("HEAD"), cancellable = true)
    private void physicsPosition(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        if (RagdollBodies.active(player)) ci.cancel();
    }
}
