package com.hanmoyun.dghcompat;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@Mod(CompatMod.ID)
public final class CompatMod {
    public static final String ID = "dgh_cybernetics_compat";
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec.BooleanValue SAFE_TABLE =
            BUILDER.define("cyberSurgeryBedIsSafe", true);
    public static final ModConfigSpec.BooleanValue CARRY_ARMS =
            BUILDER.define("carryNeedsArms", true);
    public static final ModConfigSpec.BooleanValue CHART = BUILDER.define("recordTreatment", true);


    public static boolean enabled(ModConfigSpec.BooleanValue option) {
        return !CONFIG.isLoaded() || option.get();
    }

    public static final ModConfigSpec CONFIG = BUILDER.build();

    public CompatMod(IEventBus bus, ModContainer container) {
        com.hanmoyun.dghcompat.corpse.Corpses.register(bus);
        com.hanmoyun.dghcompat.integration.NeoSyncBags.ITEMS.register(bus);
        if (ModList.get().isLoaded("neosync"))
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                    net.neoforged.bus.api.EventPriority.LOWEST,
                    com.hanmoyun.dghcompat.integration.NeoSyncBags::use);
        ImplantRemovalRecipe.SERIALIZERS.register(bus);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(BodyItems::tooltip);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                AnatomicalItems::inventoryTick);
        bus.addListener(CyberPatientPackets::register);
        bus.addListener(BedToolPackets::register);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(CyberPatientData::name);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(CyberPatientData::tab);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(CyberPatientData::login);
        bus.addListener(StasisPackets::register);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(GroundTreatment::closed);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(GroundTreatment::logout);
        bus.addListener(CaseBookPackets::register);
        container.registerConfig(ModConfig.Type.COMMON, CONFIG);
        if (ModList.get().isLoaded("cinematic")) {
            bus.addListener(BedInteraction::register);
            bus.addListener(
                    (FMLCommonSetupEvent event) ->
                            event.enqueueWork(BedInteraction::registerStation));
        }
        bus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(CyberApi::initialize));
    }
}
