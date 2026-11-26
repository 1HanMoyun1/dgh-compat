package com.hanmoyun.dghcompat;

import com.lastimp.dgh.common.capability.HealthCapability;
import com.lastimp.dgh.common.capability.bodyPart.base.AbstractExtremities;
import com.lastimp.dgh.common.enums.BodyComponents;

import net.minecraft.world.entity.player.Player;

public final class TreatmentArms {
    private TreatmentArms() {}

    public static boolean available(Player player, boolean right) {
        return HealthCapability.getAndApply(
                player,
                health ->
                        AbstractExtremities.available(
                                health, right ? BodyComponents.RIGHT_ARM : BodyComponents.LEFT_ARM),
                false);
    }

    public static boolean canOperate(Player player, boolean self, BodyComponents part) {
        return (part != BodyComponents.RIGHT_ARM || !self) && available(player, true)
                || (part != BodyComponents.LEFT_ARM || !self) && available(player, false);
    }
}
