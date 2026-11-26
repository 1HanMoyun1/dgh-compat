package com.hanmoyun.dghcompat;

import com.hanmoyun.dghcompat.client.PendingEntityStates;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

import java.util.UUID;

/** Client state delivery only; rendering is owned by client.StasisBagRenderer. */
@EventBusSubscriber(modid = CompatMod.ID, value = Dist.CLIENT)
public final class StasisClient {
    private static final int MAX_PENDING_ENTITIES = 4096;
    private static final int PACKET_LIFETIME_TICKS = 200;
    private static final PendingEntityStates<UUID, StasisPackets.State> PENDING =
            new PendingEntityStates<>(MAX_PENDING_ENTITIES, PACKET_LIFETIME_TICKS);

    private StasisClient() {}

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(
                () -> StasisPackets.clientState = state -> PENDING.put(state.uuid(), state));
    }

    @SubscribeEvent
    public static void respawn(ClientPlayerNetworkEvent.Clone event) {
        var player = event.getNewPlayer();
        PENDING.remove(player.getUUID());
        player.getPersistentData().remove(ArmState.TRACKED);
        player.getPersistentData().remove("dghcompat:down_visual");
        StasisPatients.setVisual(player, false);
        var options = Minecraft.getInstance().options;
        player.setMainArm(options.mainHand().get());
        // The server can inherit a hand forced by Cybernetics. Re-send the actual user preference.
        player.connection.send(new net.minecraft.network.protocol.common.ServerboundClientInformationPacket(
                options.buildPlayerInformation()));
        com.hanmoyun.dghcompat.client.RagdollAdapter.replacePlayer(event.getOldPlayer());
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        PENDING.clear();
        com.hanmoyun.dghcompat.client.RagdollAdapter.clear();
    }

    @SubscribeEvent
    public static void unload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            PENDING.clear();
            com.hanmoyun.dghcompat.client.RagdollAdapter.clear();
        }
    }

    @SubscribeEvent
    public static void entityLeft(net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide())
            com.hanmoyun.dghcompat.client.RagdollAdapter.forget(event.getEntity());
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        var level = Minecraft.getInstance().level;
        if (level != null)
            for (var entity : level.entitiesForRendering())
                if (entity instanceof net.minecraft.world.entity.LivingEntity living)
                    com.hanmoyun.dghcompat.client.RagdollAdapter.update(living);
        PENDING.tick(
                state -> {
                    if (level == null) return false;
                    var entity = level.getEntity(state.entityId());
                    if (entity == null || !entity.getUUID().equals(state.uuid())) return false;
                    entity.getPersistentData().putInt(ArmState.TRACKED, state.arms());
                    StasisPatients.setVisual(entity, state.wrapped());
                    entity.getPersistentData().putBoolean("dghcompat:down_visual", state.down());
                    return true;
                });
    }
}
