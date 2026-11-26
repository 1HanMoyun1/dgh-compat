package com.hanmoyun.dghcompat.client;

import com.hanmoyun.dghcompat.*;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;

@EventBusSubscriber(modid = CompatMod.ID, value = Dist.CLIENT)
public final class ArmRendering {
    private ArmRendering() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void arm(RenderArmEvent event) {
        if (!ArmState.visible(event.getPlayer(), event.getArm())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void hand(RenderHandEvent event) {
        var player = Minecraft.getInstance().player;
        if (player != null && ArmState.blocked(player, event.getHand())) event.setCanceled(true);
    }
}
