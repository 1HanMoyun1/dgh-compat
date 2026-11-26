package com.hanmoyun.dghcompat.integration;

import com.hanmoyun.dghcompat.BedPose;
import com.hanmoyun.dghcompat.StasisPatients;
import com.hanmoyun.dghcompat.TreatmentLock;

import net.minecraft.world.entity.player.Player;

/** A fixed medical pose must not be replaced by a new server-controlled reaction ragdoll. */
public final class RagdollPolicy {
    private RagdollPolicy() {}

    public static boolean medicalPose(Player player) {
        return !player.level().isClientSide()
                && com.lastimp.dgh.common.capability.HealthCapability.has(player)
                && (com.lastimp.dgh.common.capability.HealthCapability.isDown(player)
                        || StasisPatients.wrapped(player)
                        || TreatmentLock.active(player)
                        || BedPose.bed(player) != null
                        || player.isPassenger()
                        || player.isSleeping());
    }
}
