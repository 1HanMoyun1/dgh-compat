package com.hanmoyun.dghcompat.mixin;

import com.hanmoyun.dghcompat.CompatMod;
import com.hanmoyun.dghcompat.integration.PointBlankHit;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.vicmatskiv.pointblank.util.HitScan;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "com.vicmatskiv.pointblank.item.HurtingItem", remap = false)
abstract class PointBlankHitMixin {
    @WrapOperation(
            method = "hurtEntity",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean trackImpact(
            Entity target,
            DamageSource source,
            float damage,
            Operation<Boolean> original,
            LivingEntity shooter,
            EntityHitResult hit) {
        if (!(target instanceof LivingEntity patient)
                || patient.level().isClientSide())
            return original.call(target, source, damage);
        var previous = PointBlankHit.CURRENT.get();
        var point = hit.getLocation();
        var part =
                PointBlankHit.locate(
                        patient,
                        shooter.getEyePosition(),
                        point,
                        HitScan.isHeadshot(patient, point));
        PointBlankHit.CURRENT.set(new PointBlankHit(patient, source, part));
        try {
            return original.call(target, source, damage);
        } finally {
            if (previous == null) PointBlankHit.CURRENT.remove();
            else PointBlankHit.CURRENT.set(previous);
        }
    }
}
