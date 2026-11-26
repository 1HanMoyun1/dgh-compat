package com.hanmoyun.dghcompat;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;

/** Keep optional Carry On classes out of the no-Carry-On execution path. */
public final class CarrySupport {
    private CarrySupport() {}

    public static boolean loaded() {
        return ModList.get().isLoaded("carryon");
    }

    public static boolean bothHands(Player player) {
        return player.isAlive()
                && !player.isSpectator()
                && !com.lastimp.dgh.common.capability.HealthCapability.isDown(player)
                && (!CompatMod.enabled(CompatMod.CARRY_ARMS) || TreatmentArms.available(player, true)
                && TreatmentArms.available(player, false));
    }

    public static boolean carrying(Player player) {
        return loaded() && CarryOnIntegration.carrying(player);
    }

    public static boolean pickUp(ServerPlayer player, LivingEntity patient) {
        return loaded() && CarryOnIntegration.pickUpFromBed(player, patient);
    }

    public static boolean toBed(ServerPlayer player, BlockPos head) {
        return loaded() && CarryOnIntegration.toBed(player, head);
    }
}
