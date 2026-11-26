package com.hanmoyun.dghcompat.mixin;

import com.hanmoyun.dghcompat.ArmState;

import net.minecraft.client.Minecraft;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.vicmatskiv.pointblank.client.render.GunItemRenderer", remap = false)
abstract class PointBlankArmModelMixin {
    @Inject(method = "renderLeftArm", at = @At("HEAD"), cancellable = true)
    private void left(CallbackInfo ci) {
        var p = Minecraft.getInstance().player;
        if (p != null && !ArmState.visible(p, p.getMainArm().getOpposite())) ci.cancel();
    }

    @Inject(method = "renderRightArm", at = @At("HEAD"), cancellable = true)
    private void right(CallbackInfo ci) {
        var p = Minecraft.getInstance().player;
        if (p != null && !ArmState.visible(p, p.getMainArm())) ci.cancel();
    }
}
