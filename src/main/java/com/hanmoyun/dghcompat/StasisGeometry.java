package com.hanmoyun.dghcompat;

import com.lastimp.dgh.common.entity.StretcherEntity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

/** One calculation for initial dimensions and rotation-only updates on both logical sides. */
public final class StasisGeometry {
    private StasisGeometry() {}

    public static AABB bounds(Entity entity) {
        boolean mounted = entity.getVehicle() instanceof StretcherEntity;
        float yaw = 180 - entity.getYRot();
        if (entity.getVehicle() instanceof StretcherEntity carrier)
            yaw = TransportOrientation.modelYaw(carrier.getYRot(), carrier.isInvisible());
        return bounds(entity.getX(), entity.getY(), entity.getZ(), yaw, mounted);
    }

    public static AABB bounds(
            double originX, double originY, double originZ, float yaw, boolean mounted) {
        double angle = Math.toRadians(yaw);
        double sin = Math.sin(angle);
        double cos = Math.cos(angle);
        double x = originX + (mounted ? sin : 0);
        double z = originZ + (mounted ? cos : 0);
        double radiusX = Math.abs(sin) + 0.5 * Math.abs(cos);
        double radiusZ = Math.abs(cos) + 0.5 * Math.abs(sin);
        double y = originY - (mounted ? 0.25 : 0);
        return new AABB(x - radiusX, y, z - radiusZ, x + radiusX, y + 0.5, z + radiusZ);
    }

    /** The physics envelope includes padding below the body and is not a floor height. */
    public static void placeOnGround(Entity entity) {
        var bottom = entity.position();
        var hit =
                entity.level()
                        .clip(
                                new net.minecraft.world.level.ClipContext(
                                        bottom.add(0, 0.6, 0),
                                        bottom.add(0, -0.35, 0),
                                        net.minecraft.world.level.ClipContext.Block.COLLIDER,
                                        net.minecraft.world.level.ClipContext.Fluid.NONE,
                                        entity));
        if (hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK)
            entity.setPos(bottom.x, Math.max(bottom.y, hit.getLocation().y), bottom.z);
    }

    public static void refreshBounds(Entity entity) {
        AABB bounds = bounds(entity);
        if (!bounds.equals(entity.getBoundingBox())) entity.setBoundingBox(bounds);
    }
}
