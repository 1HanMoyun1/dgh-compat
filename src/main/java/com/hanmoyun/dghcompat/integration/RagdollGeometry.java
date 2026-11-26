package com.hanmoyun.dghcompat.integration;

import com.raiiiden.ragdollified.RagdollPart;
import com.raiiiden.ragdollified.RagdollTransform;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.joml.Matrix3f;
import org.joml.Quaternionf;

/** World-space enclosing boxes, rotated around each physics part rather than the actor's feet. */
public final class RagdollGeometry {
    public record Shape(Vec3 torso, AABB bounds) {}

    private RagdollGeometry() {}

    public static Shape shape(RagdollTransform[] pose, float scale) {
        if (pose == null) return null;
        Vec3 torso = null;
        AABB bounds = null;
        for (var part : pose) {
            if (part == null) continue;
            var p = part.position;
            var q = part.rotation;
            if (!Float.isFinite(p.x) || !Float.isFinite(p.y) || !Float.isFinite(p.z)) return null;
            float norm = q.x * q.x + q.y * q.y + q.z * q.z + q.w * q.w;
            if (!Float.isFinite(norm) || norm < 1.0e-8f) return null;
            if (part.partId == RagdollPart.TORSO.index) torso = new Vec3(p.x, p.y, p.z);
            var box = partBounds(part, scale);
            bounds = bounds == null ? box : bounds.minmax(box);
        }
        return torso == null || bounds == null ? null : new Shape(torso, bounds.inflate(0.04));
    }

    public static AABB partBounds(RagdollTransform part, float scale) {
        var p = part.position;
        var q = part.rotation;
        var kind = RagdollPart.byIndex(part.partId);
        // Extended animal parts have no public RagdollPart entry. Keep them inside the
        // enclosing box with a small local radius as well.
        var half = kind == null ? new org.joml.Vector3f(0.15f) : kind.getHalfExtents();
        var matrix = new Matrix3f().set(new Quaternionf(q.x, q.y, q.z, q.w).normalize());
        double x =
                scale
                        * (Math.abs(matrix.m00()) * half.x
                                + Math.abs(matrix.m10()) * half.y
                                + Math.abs(matrix.m20()) * half.z);
        double y =
                scale
                        * (Math.abs(matrix.m01()) * half.x
                                + Math.abs(matrix.m11()) * half.y
                                + Math.abs(matrix.m21()) * half.z);
        double z =
                scale
                        * (Math.abs(matrix.m02()) * half.x
                                + Math.abs(matrix.m12()) * half.y
                                + Math.abs(matrix.m22()) * half.z);
        return new AABB(p.x - x, p.y - y, p.z - z, p.x + x, p.y + y, p.z + z);
    }

    /** Transform the world segment into the moving part's local collision box. */
    public static double hitDistance(
            RagdollTransform part, RagdollPart kind, float scale, Vec3 start, Vec3 end) {
        var p = part.position;
        var q = part.rotation;
        var origin = new Vec3(p.x, p.y, p.z);
        var inverse = new Quaternionf(q.x, q.y, q.z, q.w).normalize().conjugate();
        Vec3 a = local(start.subtract(origin), inverse);
        Vec3 b = local(end.subtract(origin), inverse);
        var extents = kind.getHalfExtents();
        double x = extents.x * scale, y = extents.y * scale, z = extents.z * scale;
        var localBox = new AABB(-x, -y, -z, x, y, z);
        if (localBox.contains(a)) return 0;
        return localBox.clip(a, b).map(a::distanceToSqr).orElse(Double.POSITIVE_INFINITY);
    }

    private static Vec3 local(Vec3 offset, Quaternionf inverse) {
        var point =
                inverse.transform(
                        new org.joml.Vector3f(
                                (float) offset.x, (float) offset.y, (float) offset.z));
        return new Vec3(point.x, point.y, point.z);
    }
}
