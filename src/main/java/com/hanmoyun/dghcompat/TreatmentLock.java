package com.hanmoyun.dghcompat;

import com.lastimp.dgh.common.menu.HealthMenu;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

/** The actual server menu owns the lock, so close/logout/death cannot leave stale entries. */
public final class TreatmentLock {
    private TreatmentLock() {}

    public static boolean active(LivingEntity patient) {
        if (!(patient.level() instanceof ServerLevel level)) return false;
        return level.getServer().getPlayerList().getPlayers().stream()
                .anyMatch(
                        player ->
                                player.containerMenu instanceof HealthMenu menu
                                        && menu.targetEntity.equals(patient.getUUID()));
    }
}
