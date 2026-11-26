package com.hanmoyun.dghcompat;

import com.lastimp.dgh.common.block.OperatingBedBlock;
import com.lastimp.dgh.common.client.gui.screen.HealthScreen;

import dev.dyslexic.cinematic.api.client.AddonScenes;
import dev.dyslexic.cinematic.client.Scene;
import dev.dyslexic.cinematic.station.StationKind;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = CompatMod.ID, value = Dist.CLIENT)
public final class BedSceneClient {
    private BedSceneClient() {}

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        if (ModList.get().isLoaded("cinematic")) {
            event.enqueueWork(
                    () -> {
                        AddonScenes.register(
                                BedInteraction.BED,
                                (pos, state, player, side) -> new TreatmentScene(pos, side));
                        BedInteraction.openScene = BedSceneClient::open;
                        BedToolPackets.display =
                                state -> {
                                    var scene = Scene.station(TreatmentScene.class);
                                    if (scene != null) scene.receiveTools(state);
                                };
                        CyberPatientPackets.display =
                                state -> {
                                    var scene = Scene.station(TreatmentScene.class);
                                    if (scene != null) scene.receiveCyberData(state);
                                };
                        BedInteraction.clientPose =
                                packet -> {
                                    var level = Minecraft.getInstance().level;
                                    if (level != null
                                            && level.getEntity(packet.entityId())
                                                    instanceof
                                                    net.minecraft.world.entity.player.Player
                                                                    player) {
                                        BedPose.set(player, packet.lying() ? packet.bed() : null);
                                    }
                                };
                    });
        }
    }

    static boolean canRequest() {
        var client = Minecraft.getInstance();
        return ModList.get().isLoaded("cinematic")
                && client.getConnection() != null
                && client.getConnection().hasChannel(BedPackets.Request.TYPE);
    }

    static void request(BlockPos pos) {
        PacketDistributor.sendToServer(new BedPackets.Request(pos));
    }

    private static void open(BlockPos pos, int menuId) {
        var client = Minecraft.getInstance();
        if (client.player == null
                || client.level == null
                || !(client.level.getBlockState(pos).getBlock() instanceof OperatingBedBlock)
                || !(client.screen instanceof HealthScreen<?> screen)
                || client.player.containerMenu != screen.getMenu()
                || screen.getMenu().containerId != menuId) return;
        if (Scene.beginClientOnly(pos, StationKind.ADDON)) {
            // The native screen registered this callback in init; setScreen(null) does not call
            // onClose.
            com.lastimp.dgh.api.client.DGH_ClientAPI.endCallingDataFromServer(
                    screen.getMenu().targetEntity, java.util.List.of("update_screen"));
            client.setScreen(null);
        }
    }
}
