package com.hanmoyun.dghcompat;

import com.lastimp.dgh.common.block.OperatingBedBlock;
import com.lastimp.dgh.common.entity.StretcherEntity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = CompatMod.ID)
public final class TreatmentProtection {
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void breakBed(BlockEvent.BreakEvent event) {
        if (!(event.getState().getBlock() instanceof OperatingBedBlock)) return;
        var head = BedInteraction.head(event.getPos(), event.getState());
        for (var patient :
                event.getPlayer()
                        .level()
                        .getEntitiesOfClass(LivingEntity.class, new AABB(head).inflate(3))) {
            if (TreatmentLock.active(patient)
                    && (patient.getVehicle() instanceof StretcherEntity s
                                    && head.equals(PatientTransport.bed(s))
                            || patient instanceof net.minecraft.world.entity.player.Player p
                                    && head.equals(BedPose.bed(p)))) {
                event.setCanceled(true);
                StasisPatients.notify(event.getPlayer(), "locked");
                return;
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void attackAnchor(AttackEntityEvent event) {
        if (event.getTarget() instanceof StretcherEntity s
                && s.getPatient() != null
                && TreatmentLock.active(s.getPatient())) {
            event.setCanceled(true);
            StasisPatients.notify(event.getEntity(), "locked");
        }
    }
}
