package com.hanmoyun.dghcompat;

import com.lastimp.dgh.common.capability.HealthCapability;
import com.lastimp.dgh.common.capability.bodyPart.base.AbstractBody;
import com.lastimp.dgh.common.enums.BodyComponents;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.Set;

public final class CompatBridge {
    private CompatBridge() {}

    public static void bind(HealthCapability health, LivingEntity entity) {
        if (!CyberApi.available()) return;
        if (!entity.level().isClientSide()
                && (entity instanceof Player || CyberPatientData.hasMobData(entity)))
            SuspendedImplants.update(entity, health);
        CyberSnapshot snapshot =
                entity instanceof Player || CyberPatientData.hasMobData(entity)
                        ? CyberApi.snapshot(entity)
                        : CyberSnapshot.EMPTY;
        if (entity instanceof ServerPlayer player)
            snapshot = LimbBridge.update(health, player, snapshot);
        for (BodyComponents part : BodyComponents.values()) {
            ((BodyLink) health.getComponent(part)).dghcyber$bind(snapshot);
        }
    }

    public static CyberSnapshot snapshot(AbstractBody body) {
        return ((BodyLink) body).dghcyber$snapshot();
    }

    /** Run native DGH organ behavior for a missing organ without inserting an extractable item. */
    public static boolean onCyberSurgeryBed(LivingEntity entity) {
        if (!CompatMod.CONFIG.isLoaded()
                || !CompatMod.SAFE_TABLE.get()
                || !(entity instanceof Player player)
                || !player.isSleeping()) return false;
        return player.getSleepingPos()
                .map(
                        pos -> {
                            ResourceLocation id =
                                    BuiltInRegistries.BLOCK.getKey(
                                            player.level().getBlockState(pos).getBlock());
                            return id.getNamespace().equals("createcybernetics")
                                    && Set.of(
                                                    "surgery_table",
                                                    "surgery_chamber",
                                                    "surgery_chamber_top")
                                            .contains(id.getPath());
                        })
                .orElse(false);
    }

    public static void afterSurgery(ServerPlayer player, Set<String> parts) {
        if (parts.isEmpty()) return;
        HealthCapability.getAndApply(
                player,
                health -> {
                    health.update(player);
                    health.updateIfDirty(Boolean.TRUE, Boolean.FALSE);
                    health.SYNIfDirty(player);
                });
    }
}
