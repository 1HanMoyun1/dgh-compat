package com.hanmoyun.dghcompat;

import dev.dyslexic.cinematic.client.Scene;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;

/** Client-only path, reached only while explicitly drawing the patient's own body. */
final class SelfBedRender {
    static BlockPos bed(Player player) {
        return player == Minecraft.getInstance().player
                        && Scene.station instanceof TreatmentScene scene
                        && scene.renderingSelf()
                ? scene.renderBed()
                : null;
    }
}
