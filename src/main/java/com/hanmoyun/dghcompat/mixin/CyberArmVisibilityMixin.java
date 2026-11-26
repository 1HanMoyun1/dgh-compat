package com.hanmoyun.dghcompat.mixin;

import com.hanmoyun.dghcompat.ArmState;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.HumanoidArm;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "com.perigrine3.createcybernetics.client.render.CyberwareLimbHider", remap = false)
abstract class CyberArmVisibilityMixin {
    @ModifyReturnValue(method = "shouldRenderLeftArm", at = @At("RETURN"), require = 0)
    private static boolean left(boolean shown, AbstractClientPlayer player) {
        return shown && (player == null || ArmState.visible(player, HumanoidArm.LEFT));
    }

    @ModifyReturnValue(method = "shouldRenderRightArm", at = @At("RETURN"), require = 0)
    private static boolean right(boolean shown, AbstractClientPlayer player) {
        return shown && (player == null || ArmState.visible(player, HumanoidArm.RIGHT));
    }
}
