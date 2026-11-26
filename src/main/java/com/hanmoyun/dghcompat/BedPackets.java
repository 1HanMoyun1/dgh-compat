package com.hanmoyun.dghcompat;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

final class BedPackets {
    private BedPackets() {}

    record Request(BlockPos pos) implements CustomPacketPayload {
        static final Type<Request> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(CompatMod.ID, "treat_bed"));
        static final StreamCodec<RegistryFriendlyByteBuf, Request> CODEC =
                StreamCodec.composite(BlockPos.STREAM_CODEC, Request::pos, Request::new);

        @Override
        public Type<Request> type() {
            return TYPE;
        }
    }

    record Position(int entityId, BlockPos bed, boolean lying) implements CustomPacketPayload {
        static final Type<Position> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(CompatMod.ID, "bed_pose"));
        static final StreamCodec<RegistryFriendlyByteBuf, Position> CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.VAR_INT,
                        Position::entityId,
                        BlockPos.STREAM_CODEC,
                        Position::bed,
                        ByteBufCodecs.BOOL,
                        Position::lying,
                        Position::new);

        @Override
        public Type<Position> type() {
            return TYPE;
        }
    }

    record Treat(int menuId, int part) implements CustomPacketPayload {
        static final Type<Treat> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(CompatMod.ID, "treat_carried"));
        static final StreamCodec<RegistryFriendlyByteBuf, Treat> CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.VAR_INT,
                        Treat::menuId,
                        ByteBufCodecs.VAR_INT,
                        Treat::part,
                        Treat::new);

        @Override
        public Type<Treat> type() {
            return TYPE;
        }
    }

    record OrganMode(int menuId, int part, boolean active) implements CustomPacketPayload {
        static final Type<OrganMode> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(CompatMod.ID, "organ_mode"));
        static final StreamCodec<RegistryFriendlyByteBuf, OrganMode> CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.VAR_INT,
                        OrganMode::menuId,
                        ByteBufCodecs.VAR_INT,
                        OrganMode::part,
                        ByteBufCodecs.BOOL,
                        OrganMode::active,
                        OrganMode::new);

        @Override
        public Type<OrganMode> type() {
            return TYPE;
        }
    }

    record Open(BlockPos pos, int menuId) implements CustomPacketPayload {
        static final Type<Open> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(CompatMod.ID, "treat_scene"));
        static final StreamCodec<RegistryFriendlyByteBuf, Open> CODEC =
                StreamCodec.composite(
                        BlockPos.STREAM_CODEC,
                        Open::pos,
                        ByteBufCodecs.VAR_INT,
                        Open::menuId,
                        Open::new);

        @Override
        public Type<Open> type() {
            return TYPE;
        }
    }
}
