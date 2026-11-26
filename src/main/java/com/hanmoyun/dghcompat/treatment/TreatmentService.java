package com.hanmoyun.dghcompat.treatment;

import com.hanmoyun.dghcompat.TreatmentArms;
import com.lastimp.dgh.api.DGH_API;
import com.lastimp.dgh.common.capability.HealthCapability;
import com.lastimp.dgh.common.enums.BodyComponents;
import com.lastimp.dgh.common.menu.HealthMenu;
import com.lastimp.dgh.common.tags.ModTags;
import com.lastimp.dgh.common.utils.Utils;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Validates and executes a scene treatment on the server thread using the existing DGH API. */
public final class TreatmentService {
    private static final double MAX_DISTANCE_SQUARED = 36;

    private TreatmentService() {}

    public static void organMode(ServerPlayer doctor, int menuId, int index, boolean active) {
        if (!(doctor.containerMenu instanceof HealthMenu menu) || menu.containerId != menuId)
            return;
        var parts = BodyComponents.values();
        if (index < 0
                || index >= parts.length
                || !BodyComponents.VISIBLE_BODIES.contains(parts[index])) return;
        var patient = Utils.getLivingWithHealth(doctor.serverLevel(), menu.targetEntity);
        if (patient == null
                || !patient.isAlive()
                || doctor.distanceToSqr(patient) > MAX_DISTANCE_SQUARED
                || !HealthCapability.canApplyHealing(doctor)
                || com.hanmoyun.dghcompat.corpse.Corpses.blocksTreatment(patient)) return;
        boolean onBed =
                com.hanmoyun.dghcompat.corpse.Corpses.onSurgeryBed(patient)
                        || patient == doctor && com.hanmoyun.dghcompat.BedPose.bed(doctor) != null;
        if (!onBed) return;
        HealthCapability.getAndApply(
                patient,
                health -> {
                    var body =
                            (com.lastimp.dgh.common.capability.bodyPart.base.AbstractVisibleBody)
                                    health.getComponent(parts[index]);
                    com.hanmoyun.dghcompat.BodyItems.identifyOrgans(health, patient);
                    menu.setOrganActive(
                            active
                                    && body.abnormal(
                                            com.lastimp
                                                    .dgh
                                                    .common
                                                    .capability
                                                    .bodyPart
                                                    .base
                                                    .BodyCondition
                                                    .RETRACTED_SKIN)
                                    && TreatmentArms.canOperate(
                                            doctor, patient == doctor, parts[index]),
                            body);
                    menu.broadcastFullState();
                });
    }

    public static void treat(ServerPlayer doctor, int menuId, int partIndex) {
        if (!doctor.isAlive()
                || doctor.isSpectator()
                || !(doctor.containerMenu instanceof HealthMenu menu)
                || menu.containerId != menuId
                || !com.hanmoyun.dghcompat.GroundTreatment.valid(doctor, menu)) return;

        BodyComponents[] parts = BodyComponents.values();
        if (partIndex < 0
                || partIndex >= parts.length
                || !BodyComponents.VISIBLE_BODIES.contains(parts[partIndex])) return;
        BodyComponents part = parts[partIndex];
        var patient = Utils.getLivingWithHealth(doctor.serverLevel(), menu.targetEntity);
        if (patient == null
                || !patient.isAlive()
                || com.hanmoyun.dghcompat.corpse.Corpses.blocksTreatment(patient)
                || doctor.distanceToSqr(patient) > MAX_DISTANCE_SQUARED
                || !HealthCapability.canApplyHealing(doctor)) return;
        if (!TreatmentArms.canOperate(doctor, patient == doctor, part)) {
            doctor.displayClientMessage(
                    Component.translatable("dghcompat.treatment.no_hand"), true);
            return;
        }

        boolean fromCursor = !menu.getCarried().isEmpty();
        var stack = fromCursor ? menu.getCarried() : doctor.getMainHandItem();
        if (stack.isEmpty()) return;
        if (stack.is(ModTags.MEDICAL_USAGE_BAGS)) {
            DGH_API.handleBagHealing(stack, doctor, patient, part);
        } else {
            DGH_API.useItemOn(stack, doctor, patient, part);
        }
        if (fromCursor) menu.setCarried(stack);
        menu.broadcastChanges();
    }
}
