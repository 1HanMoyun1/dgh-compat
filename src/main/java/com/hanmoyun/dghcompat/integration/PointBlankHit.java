package com.hanmoyun.dghcompat.integration;

import com.hanmoyun.dghcompat.CompatMod;
import com.lastimp.dgh.common.enums.BodyComponents;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public record PointBlankHit(LivingEntity patient, DamageSource source, BodyComponents part) {
    public static final ThreadLocal<PointBlankHit> CURRENT = new ThreadLocal<>();

    public static BodyComponents locate(
            LivingEntity patient, Vec3 start, Vec3 impact, boolean headshot) {
        if (RagdollBodies.available()
                && RagdollBodies.active(patient)) {
            var end =
                    impact.add(
                            impact.subtract(start)
                                    .normalize()
                                    .scale(patient.getBoundingBox().getSize() * 3));
            var part = NativeRagdollBodies.hitPart(patient, start, end);
            if (part != null) return part;
        }
        if (headshot) return BodyComponents.HEAD;
        var box = patient.getBoundingBox();
        var offset = impact.subtract(box.getCenter());
        double yaw = Math.toRadians(patient.yBodyRot);
        // At yaw zero the patient faces +Z. Its anatomical right points toward -X.
        double left = offset.x * Math.cos(yaw) + offset.z * Math.sin(yaw);
        if (impact.y < box.minY + box.getYsize() * 0.46)
            return left >= 0 ? BodyComponents.LEFT_LEG : BodyComponents.RIGHT_LEG;
        if (Math.abs(left) > patient.getBbWidth() * 0.30)
            return left > 0 ? BodyComponents.LEFT_ARM : BodyComponents.RIGHT_ARM;
        return BodyComponents.TORSO;
    }
}
