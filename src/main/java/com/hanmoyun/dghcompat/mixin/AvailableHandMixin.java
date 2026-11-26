package com.hanmoyun.dghcompat.mixin;

import com.hanmoyun.dghcompat.ArmState;

import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
abstract class AvailableHandMixin {
    @Inject(method = "getMainArm", at = @At("RETURN"), cancellable = true)
    private void useSurvivingArm(CallbackInfoReturnable<HumanoidArm> ci) {
        var preferred = ci.getReturnValue();
        var player = (Player) (Object) this;
        if (!ArmState.usable(player, preferred) && ArmState.usable(player, preferred.getOpposite()))
            ci.setReturnValue(preferred.getOpposite());
    }
}
