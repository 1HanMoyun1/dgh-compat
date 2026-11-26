package com.hanmoyun.dghcompat;

import com.lastimp.dgh.common.menu.HealthMenu;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.function.Consumer;

final class CyberPatientPackets {
    static Consumer<State> display = state -> {};

    private CyberPatientPackets() {}

    record Request(int menuId) implements CustomPacketPayload {
        static final Type<Request> TYPE = new Type<>(id("cyber_patient_request"));
        static final StreamCodec<RegistryFriendlyByteBuf, Request> CODEC =
                StreamCodec.composite(ByteBufCodecs.VAR_INT, Request::menuId, Request::new);

        @Override
        public Type<Request> type() {
            return TYPE;
        }
    }

    record State(int menuId, CompoundTag data) implements CustomPacketPayload {
        static final Type<State> TYPE = new Type<>(id("cyber_patient_state"));
        static final StreamCodec<RegistryFriendlyByteBuf, State> CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.VAR_INT,
                        State::menuId,
                        ByteBufCodecs.COMPOUND_TAG,
                        State::data,
                        State::new);

        @Override
        public Type<State> type() {
            return TYPE;
        }
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(CompatMod.ID, path);
    }

    static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1")
                .optional()
                .playToServer(
                        Request.TYPE,
                        Request.CODEC,
                        (request, ctx) ->
                                ctx.enqueueWork(
                                        () -> {
                                            if (ctx.player() instanceof ServerPlayer doctor
                                                    && doctor.containerMenu
                                                            instanceof HealthMenu menu
                                                    && menu.containerId == request.menuId()
                                                    && doctor.connection.hasChannel(State.TYPE)
                                                    && doctor.serverLevel()
                                                                    .getEntity(menu.targetEntity)
                                                            instanceof
                                                            net.minecraft.world.entity.LivingEntity
                                                                            patient
                                                    && doctor.distanceToSqr(patient) <= 36
                                                    && !com.hanmoyun.dghcompat.corpse.Corpses
                                                            .isCorpse(patient)) {
                                                PacketDistributor.sendToPlayer(
                                                        doctor,
                                                        new State(
                                                                menu.containerId,
                                                                CyberPatientData.read(patient)));
                                            }
                                        }))
                .playToClient(
                        State.TYPE,
                        State.CODEC,
                        (state, ctx) -> ctx.enqueueWork(() -> display.accept(state)));
    }
}
