package com.hanmoyun.dghcompat.mixin;

import com.hanmoyun.dghcompat.corpse.Corpses;
import com.lastimp.dgh.common.client.gui.screen.HealthScreen;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A corpse keeps its medical record while the monitor shows no pulse. */
@Mixin(value = HealthScreen.class, remap = false)
abstract class CorpseMonitorMixin {
    @Shadow @Final protected static ResourceLocation HUD_HEART_BEAT_STOP;

    @ModifyArg(
            method = "renderHeartBeat",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIFFIIII)V"),
            index = 0)
    private ResourceLocation flatline(ResourceLocation waveform) {
        return dghcompat$corpseTarget() ? HUD_HEART_BEAT_STOP : waveform;
    }

    @Inject(method = "playSound", at = @At("HEAD"), cancellable = true)
    private void silentMonitor(CallbackInfo ci) {
        if (dghcompat$corpseTarget()) ci.cancel();
    }

    @Unique private Boolean dghcompat$confirmedCorpse;

    @Unique
    private boolean dghcompat$corpseTarget() {
        if (dghcompat$confirmedCorpse != null) return dghcompat$confirmedCorpse;
        var level = Minecraft.getInstance().level;
        if (level == null) return false;
        var target = ((HealthScreen<?>) (Object) this).getMenu().targetEntity;
        for (var entity : level.entitiesForRendering()) {
            if (entity.getUUID().equals(target)) {
                dghcompat$confirmedCorpse = Corpses.isCorpse(entity);
                return dghcompat$confirmedCorpse;
            }
        }
        return false;
    }
}

