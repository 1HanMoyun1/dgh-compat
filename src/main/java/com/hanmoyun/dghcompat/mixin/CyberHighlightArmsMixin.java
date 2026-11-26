package com.hanmoyun.dghcompat.mixin;

import com.hanmoyun.dghcompat.ArmState;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.HumanoidArm;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.perigrine3.createcybernetics.client.skin.SkinHighlightLayer", remap = false)
abstract class CyberHighlightArmsMixin {
    @Inject(method = "applyHighlightVisibility", at = @At("RETURN"), require = 0)
    private static void hide(AbstractClientPlayer player, PlayerModel<?> model, CallbackInfo ci) {
        model.leftArm.visible &= ArmState.visible(player, HumanoidArm.LEFT);
        model.leftSleeve.visible &= model.leftArm.visible;
        model.rightArm.visible &= ArmState.visible(player, HumanoidArm.RIGHT);
        model.rightSleeve.visible &= model.rightArm.visible;
    }
}
