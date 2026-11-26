package com.hanmoyun.dghcompat.interaction;

import com.hanmoyun.dghcompat.CarryOnIntegration;
import com.hanmoyun.dghcompat.CarrySupport;
import com.hanmoyun.dghcompat.TreatmentArms;
import com.hanmoyun.dghcompat.corpse.CorpseEntity;
import com.hanmoyun.dghcompat.corpse.Corpses;
import com.hanmoyun.dghcompat.corpse.NativePatients;
import com.lastimp.dgh.common.capability.HealthCapability;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.ChestMenu;

/** Opens belongings on a key press; live patients are never affected. */
public final class MedicalActions {
    private MedicalActions() {}

    public static boolean validReach(ServerPlayer doctor, Entity target) {
        return target != null
                && !target.isRemoved()
                && doctor.isAlive()
                && !doctor.isSpectator()
                && doctor.containerMenu == doctor.inventoryMenu
                && !HealthCapability.isDown(doctor)
                && (TreatmentArms.available(doctor, true) || TreatmentArms.available(doctor, false))
                && inReach(doctor, target);
    }

    public static boolean inReach(ServerPlayer doctor, Entity target) {
        return target.getBoundingBox().distanceToSqr(doctor.getEyePosition()) <= 16
                && seesBody(doctor, target);
    }

    private static boolean seesBody(ServerPlayer doctor, Entity target) {
        if (!com.hanmoyun.dghcompat.integration.RagdollBodies.active(target))
            return doctor.hasLineOfSight(target);
        var hit =
                doctor.level()
                        .clip(
                                new net.minecraft.world.level.ClipContext(
                                        doctor.getEyePosition(),
                                        target.getBoundingBox().getCenter(),
                                        net.minecraft.world.level.ClipContext.Block.COLLIDER,
                                        net.minecraft.world.level.ClipContext.Fluid.NONE,
                                        doctor));
        return hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS;
    }

}
