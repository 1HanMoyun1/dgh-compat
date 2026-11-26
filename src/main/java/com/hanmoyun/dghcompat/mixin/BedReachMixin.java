package com.hanmoyun.dghcompat.mixin;

import com.hanmoyun.dghcompat.TreatmentScene;

import dev.dyslexic.cinematic.client.ClientStation;
import dev.dyslexic.cinematic.client.Reach;

import net.minecraft.client.player.LocalPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Pseudo
@Mixin(targets = "dev.dyslexic.cinematic.client.Reach", remap = false)
abstract class BedReachMixin {
    @Inject(method = "planHere", at = @At("HEAD"), cancellable = true)
    private static void fromConfirmedMenu(
            ClientStation station,
            LocalPlayer player,
            CallbackInfoReturnable<Reach.Plan> callback) {
        if (station instanceof TreatmentScene bed && !bed.self()) {
            callback.setReturnValue(Reach.plan(station, player));
        } else if (station instanceof TreatmentScene) {
            callback.setReturnValue(
                    new Reach.Plan(true, List.of(), player.position(), "", null, null));
        }
    }
}
