package com.hanmoyun.dghcompat;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.UUID;
import java.util.function.Consumer;

public final class StasisPackets {
    public static Consumer<State> clientState = state -> {};

    private StasisPackets() {}

    public record State(int entityId, UUID uuid, boolean wrapped, boolean down, int arms)
            implements CustomPacketPayload {
        public static final Type<State> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(CompatMod.ID, "stasis_state_v3"));
        public static final StreamCodec<RegistryFriendlyByteBuf, State> CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.VAR_INT,
                        State::entityId,
                        UUIDUtil.STREAM_CODEC,
                        State::uuid,
                        ByteBufCodecs.BOOL,
                        State::wrapped,
                        ByteBufCodecs.BOOL,
                        State::down,
                        ByteBufCodecs.VAR_INT,
                        State::arms,
                        State::new);

        public Type<State> type() {
            return TYPE;
        }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("2")
                .optional()
                .playToClient(
                        State.TYPE,
                        State.CODEC,
                        (packet, context) -> context.enqueueWork(() -> clientState.accept(packet)));
    }
}
