package com.hanmoyun.dghcompat.mixin;

import com.hanmoyun.dghcompat.*;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Function;

@Pseudo
@Mixin(targets = "tschipp.carryon.common.carry.PickupHandler", remap = false)
abstract class CarryPickupMixin {
    @Inject(method = "canCarryGeneral", at = @At("HEAD"), cancellable = true)
    private static void hands(ServerPlayer player, CallbackInfoReturnable<Boolean> ci) {
        if (!CarrySupport.bothHands(player)) ci.setReturnValue(false);
        else if (CarryOnIntegration.fromBed()) ci.setReturnValue(CarryOnIntegration.ready(player));
    }

    @Inject(method = "tryPickupEntity", at = @At("HEAD"), cancellable = true)
    private static void patient(
            ServerPlayer player,
            Entity target,
            Function<Entity, Boolean> permission,
            CallbackInfoReturnable<Boolean> ci) {
        if (CarryOnIntegration.protectedTarget(target)) {
            ci.setReturnValue(false);
            return;
        }
        var living = com.hanmoyun.dghcompat.corpse.NativePatients.resolve(target);
        if (living == null) return;
        if (!StasisPatients.wrapped(living)) {
            ci.setReturnValue(false);
            return;
        }
        if (living instanceof net.minecraft.world.entity.TamableAnimal pet
                && pet.getOwnerUUID() != null
                && !player.getUUID().equals(pet.getOwnerUUID())) {
            ci.setReturnValue(false);
            return;
        }
        // Medical patients intentionally bypass original species/size/player allow-lists, not
        // permissions.
        if (!tschipp.carryon.common.carry.PickupHandler.canCarryGeneral(player)
                || !player.canInteractWithEntity(target, 0)
                || !target.isAlive()
                || (permission != null && !permission.apply(target))) {
            ci.setReturnValue(false);
            return;
        }
        ci.setReturnValue(CarryOnIntegration.pickupPatient(player, living));
    }
}
