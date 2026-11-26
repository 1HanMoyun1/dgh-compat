package com.hanmoyun.dghcompat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class BedPose {
    private static final java.util.Map<Player, BlockPos> BEDS = new java.util.WeakHashMap<>();
    public static boolean drawingSelf;

    private BedPose() {}

    public static synchronized BlockPos bed(Player player) {
        if (drawingSelf && net.neoforged.fml.ModList.get().isLoaded("cinematic")) {
            return SelfBedRender.bed(player);
        }
        return BEDS.get(player);
    }

    static synchronized void set(Player player, BlockPos bed) {
        if (bed == null) BEDS.remove(player);
        else BEDS.put(player, bed);
    }

    public static Vec3 feet(BlockPos head, Direction facing) {
        return Vec3.atBottomCenterOf(head)
                .subtract(Vec3.atLowerCornerOf(facing.getNormal()).scale(1.35))
                .add(0, 0.63, 0);
    }

    static void hold(Player player, Vec3 feet) {
        player.setPos(feet.x, feet.y, feet.z);
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0;
        player.setForcedPose(Pose.SWIMMING);
        player.setPose(Pose.SWIMMING);
        player.setSwimming(false);
    }
}
