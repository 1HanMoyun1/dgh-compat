package com.hanmoyun.dghcompat.mixin;

import com.hanmoyun.dghcompat.CyberAnatomy;

import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.perigrine3.createcybernetics.common.surgery.SurgeryController", remap = false)
abstract class CyberCommitAnatomyMixin {
    @Inject(
            method = {
                "performSurgery(Lnet/minecraft/world/entity/player/Player;Lcom/perigrine3/createcybernetics/block/entity/RobosurgeonBlockEntity;[Z[Z)F",
                "performSurgery(Lnet/minecraft/world/entity/player/Player;Lcom/perigrine3/createcybernetics/block/entity/SurgeryTableBlockEntity;[Z[Z)F"
            },
            at = @At("HEAD"),
            cancellable = true,
            require = 0)
    private static void checkPatient(
            Player patient,
            @Coerce Object surgeon,
            boolean[] staged,
            boolean[] removal,
            CallbackInfoReturnable<Float> ci) {
        if (!CyberAnatomy.canCommit(patient, surgeon, staged, removal)) ci.setReturnValue(0F);
    }
}
