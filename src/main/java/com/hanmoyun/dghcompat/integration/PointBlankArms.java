package com.hanmoyun.dghcompat.integration;

import com.hanmoyun.dghcompat.CompatMod;

import net.minecraft.world.entity.player.Player;

public final class PointBlankArms {
    private PointBlankArms() {}

    /** Use the gun stack's native dual-wield capability, including its attachments. */
    public static boolean reloadBlocked(Player player, net.minecraft.world.item.ItemStack stack) {
        if (player == null
                || stack == null
                || !(stack.getItem()
                        instanceof com.hanmoyun.dghcompat.mixin.PointBlankGripAccess gun))
            return false;
        boolean main = stack != player.getOffhandItem();
        return disabled(player, main)
                || (main && disabled(player, false) && !gun.dghcompat$allowsOneHand(stack));
    }

    public static boolean disabled(Player player, boolean mainHand) {
        if (player == null) return false;
        return com.hanmoyun.dghcompat.ArmState.blocked(
                player,
                mainHand
                        ? net.minecraft.world.InteractionHand.MAIN_HAND
                        : net.minecraft.world.InteractionHand.OFF_HAND);
    }
}
