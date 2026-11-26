package com.hanmoyun.dghcompat;

import com.hanmoyun.dghcompat.interaction.MedicalActions;
import com.lastimp.dgh.common.capability.HealthCapability;
import com.lastimp.dgh.common.menu.HealthMenu;
import com.lastimp.dgh.common.menu.MenuOpenWrapper;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.WeakHashMap;

/** A moving patient can be treated where they fell, while the doctor stays in reach. */
public final class GroundTreatment {
    private record Session(HealthMenu menu, LivingEntity patient) {}

    private static final Map<ServerPlayer, Session> SESSIONS = new WeakHashMap<>();

    private GroundTreatment() {}

    public static void open(ServerPlayer doctor, LivingEntity patient) {
        if (!StasisPatients.freeInjured(patient, doctor)
                || TreatmentLock.active(patient)
                || !MedicalActions.validReach(doctor, patient)
                || CarrySupport.carrying(doctor)
                || !HealthCapability.canApplyHealing(doctor)) return;
        MenuOpenWrapper.openHealthMenu(doctor, patient.getUUID(), false);
        if (doctor.containerMenu instanceof HealthMenu menu) {
            SESSIONS.put(doctor, new Session(menu, patient));
        }
    }

    public static boolean valid(ServerPlayer doctor, HealthMenu menu) {
        var session = SESSIONS.get(doctor);
        if (session == null || session.menu() != menu) return true;
        var patient = session.patient();
        return patient.level() == doctor.level()
                && !patient.isRemoved()
                && StasisPatients.freeInjured(patient, doctor)
                && MedicalActions.inReach(doctor, patient);
    }

    public static void closed(
            net.neoforged.neoforge.event.entity.player.PlayerContainerEvent.Close event) {
        if (event.getEntity() instanceof ServerPlayer doctor) SESSIONS.remove(doctor);
    }

    public static void logout(
            net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer doctor) SESSIONS.remove(doctor);
    }
}
