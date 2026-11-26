package com.hanmoyun.dghcompat;

import com.lastimp.dgh.common.capability.HealthCapability;
import com.lastimp.dgh.common.capability.bodyPart.base.AbstractExtremities;
import com.lastimp.dgh.common.enums.BodyComponents;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = CompatMod.ID)
public final class ArmState {
    private ArmState() {}

    static final String TRACKED = "dghcompat:arm_flags";

    /*
     * Synced flag layout. Bits 0-3 are the historical arm flags; bits 4-5 are the
     * leg flags and are inverted on purpose: a set bit means "this leg is hidden".
     * A value written by an older build only ever contains bits 0-3, so it still
     * reads as two intact legs on a new client, while older clients ignore bits
     * 4-5 instead of misreading them. The packet keeps its single integer layout,
     * so no protocol bump is needed in either direction.
     */
    static final int ARM_USABLE_LEFT = 1;
    static final int ARM_USABLE_RIGHT = 2;
    static final int ARM_VISIBLE_LEFT = 4;
    static final int ARM_VISIBLE_RIGHT = 8;
    static final int LEG_HIDDEN_LEFT = 16;
    static final int LEG_HIDDEN_RIGHT = 32;
    /** Both arms usable and visible; both legs intact (the pre-leg default). */
    static final int INTACT =
            ARM_USABLE_LEFT | ARM_USABLE_RIGHT | ARM_VISIBLE_LEFT | ARM_VISIBLE_RIGHT;

    static int flags(net.minecraft.world.entity.LivingEntity entity) {
        if (!(entity instanceof Player player)) return INTACT;
        return com.lastimp.dgh.common.PlatformService.CAPABILITY_HELPER
                .getHealth(player)
                .map(
                        h ->
                                (medicallyUsable(h, BodyComponents.LEFT_ARM) ? ARM_USABLE_LEFT : 0)
                                        | (medicallyUsable(h, BodyComponents.RIGHT_ARM)
                                                ? ARM_USABLE_RIGHT
                                                : 0)
                                        | (AbstractExtremities.visible(h, BodyComponents.LEFT_ARM)
                                                ? ARM_VISIBLE_LEFT
                                                : 0)
                                        | (AbstractExtremities.visible(h, BodyComponents.RIGHT_ARM)
                                                ? ARM_VISIBLE_RIGHT
                                                : 0)
                                        | legFlags(h, BodyComponents.LEFT_LEG, LEG_HIDDEN_LEFT)
                                        | legFlags(
                                                h, BodyComponents.RIGHT_LEG, LEG_HIDDEN_RIGHT))
                .orElse(INTACT);
    }

    /** Legs are healthy by default; only DGH's own visibility predicate can hide one. */
    private static int legFlags(HealthCapability health, BodyComponents part, int hiddenBit) {
        return AbstractExtremities.visible(health, part) ? 0 : hiddenBit;
    }

    private static boolean medicallyUsable(HealthCapability health, BodyComponents part) {
        var arm = (AbstractExtremities) health.getComponent(part);
        if (!arm.visible()) return false;
        // Only DGH injuries can trigger the bridge's hand restriction. Cybernetics handles its own
        // power state.
        if (!arm.abnormal(com.lastimp.dgh.common.capability.bodyPart.base.BodyCondition.DISLOCATION)
                && !arm.abnormalWithHidden(
                        com.lastimp.dgh.common.capability.bodyPart.base.BodyCondition.FRACTURE)
                && !arm.abnormalWithHidden(
                        com.lastimp.dgh.common.capability.bodyPart.base.BodyCondition.SAWED_BONES))
            return true;
        return arm.available(health);
    }

    private static int current(Player player) {
        if (player.level().isClientSide() && player.getPersistentData().contains(TRACKED))
            return player.getPersistentData().getInt(TRACKED);
        return flags(player);
    }

    public static boolean usable(Player player, HumanoidArm arm) {
        return (current(player) & (arm == HumanoidArm.LEFT ? ARM_USABLE_LEFT : ARM_USABLE_RIGHT))
                != 0;
    }

    public static boolean visible(Player player, HumanoidArm arm) {
        return (current(player) & (arm == HumanoidArm.LEFT ? ARM_VISIBLE_LEFT : ARM_VISIBLE_RIGHT))
                != 0;
    }

    /** True unless the synced flags mark this leg (and its pants/armor layer) as hidden. */
    public static boolean visibleLeg(Player player, HumanoidArm arm) {
        return (current(player) & (arm == HumanoidArm.LEFT ? LEG_HIDDEN_LEFT : LEG_HIDDEN_RIGHT))
                == 0;
    }

    public static boolean blocked(Player player, InteractionHand hand) {
        HumanoidArm main = player.getMainArm();
        return !usable(player, hand == InteractionHand.MAIN_HAND ? main : main.getOpposite());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void blockRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (blocked(event.getEntity(), event.getHand())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void blockRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (blocked(event.getEntity(), event.getHand())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void blockEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (blocked(event.getEntity(), event.getHand())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void blockEntityInteractSpecific(
            PlayerInteractEvent.EntityInteractSpecific event) {
        if (blocked(event.getEntity(), event.getHand())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void blockLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (blocked(event.getEntity(), event.getHand())) event.setCanceled(true);
    }
}
