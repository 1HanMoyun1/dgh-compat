package com.hanmoyun.dghcompat;

import com.lastimp.dgh.common.enums.BodyComponents;
import com.lastimp.dgh.common.menu.HealthMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class CaseBookPackets {
    public record Action(BlockPos bed, int menu, int part) implements CustomPacketPayload {
        public static final Type<Action> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(CompatMod.ID, "case_book"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Action> CODEC =
                StreamCodec.composite(
                        BlockPos.STREAM_CODEC,
                        Action::bed,
                        ByteBufCodecs.VAR_INT,
                        Action::menu,
                        ByteBufCodecs.VAR_INT,
                        Action::part,
                        Action::new);

        public Type<Action> type() {
            return TYPE;
        }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1")
                .optional()
                .playToServer(
                        Action.TYPE,
                        Action.CODEC,
                        (packet, ctx) ->
                                ctx.enqueueWork(
                                        () -> {
                                            if (!(ctx.player() instanceof ServerPlayer player)
                                                    || player.isSpectator()
                                                    || !player.isAlive()
                                                    || !(player.containerMenu
                                                            instanceof HealthMenu menu)
                                                    || menu.containerId != packet.menu()) return;
                                            var patient =
                                                    com.lastimp.dgh.common.utils.Utils
                                                            .getLivingWithHealth(
                                                                    player.serverLevel(),
                                                                    menu.targetEntity);
                                            if (patient == null
                                                    || !patient.isAlive()
                                                    || player.distanceToSqr(patient) > 36
                                                    || !packet.bed().equals(CaseBook.bed(patient)))
                                                return;
                                            if (packet.part() >= 0
                                                    && packet.part()
                                                            < BodyComponents.values().length) {
                                                var part = BodyComponents.values()[packet.part()];
                                                if (!BodyComponents.VISIBLE_BODIES.contains(part))
                                                    return;
                                                long now = player.level().getGameTime();
                                                if (player.getPersistentData()
                                                                .getLong(
                                                                        "dghcompat:last_book_check")
                                                        > now - 4) return;
                                                player.getPersistentData()
                                                        .putLong("dghcompat:last_book_check", now);
                                                CaseBook.record(player, patient, part, "检查");
                                            }
                                        }));
    }
}
