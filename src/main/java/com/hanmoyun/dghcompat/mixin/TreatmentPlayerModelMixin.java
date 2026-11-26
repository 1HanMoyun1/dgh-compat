package com.hanmoyun.dghcompat.mixin;

import com.hanmoyun.dghcompat.ArmState;
import com.hanmoyun.dghcompat.BedPose;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
public class TreatmentPlayerModelMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void neutralHead(
            LivingEntity player,
            float limbSwing,
            float amount,
            float age,
            float yaw,
            float pitch,
            CallbackInfo ci) {
        if (!(player instanceof Player patient)) return;
        var model = (PlayerModel<?>) (Object) this;
        if (BedPose.bed(patient) == null) {
            model.leftArm.visible &= ArmState.visible(patient, HumanoidArm.LEFT);
            model.rightArm.visible &= ArmState.visible(patient, HumanoidArm.RIGHT);
            model.leftSleeve.visible &= model.leftArm.visible;
            model.rightSleeve.visible &= model.rightArm.visible;
            model.leftLeg.visible &= ArmState.visibleLeg(patient, HumanoidArm.LEFT);
            model.rightLeg.visible &= ArmState.visibleLeg(patient, HumanoidArm.RIGHT);
            model.leftPants.visible &= model.leftLeg.visible;
            model.rightPants.visible &= model.rightLeg.visible;
            return;
        }
        model.body.setRotation(0, 0, 0);
        model.body.y = 0;
        model.body.z = 0;
        model.head.y = 0;
        model.rightLeg.y = 12;
        model.leftLeg.y = 12;
        model.rightLeg.z = 0;
        model.leftLeg.z = 0;
        model.rightArm.y = 2;
        model.leftArm.y = 2;
        model.rightArm.z = 0;
        model.leftArm.z = 0;
        model.jacket.copyFrom(model.body);
        model.head.setRotation(0, 0, 0);
        model.hat.copyFrom(model.head);
        model.rightLeg.setRotation(0, 0, 0);
        model.leftLeg.setRotation(0, 0, 0);
        model.rightArm.setRotation(0, 0, 0.06f);
        model.leftArm.setRotation(0, 0, -0.06f);
        model.rightPants.copyFrom(model.rightLeg);
        model.leftPants.copyFrom(model.leftLeg);
        model.rightSleeve.copyFrom(model.rightArm);
        model.leftSleeve.copyFrom(model.leftArm);
        model.rightArm.visible = false;
        model.rightSleeve.visible = false;
        model.leftArm.visible = false;
        model.leftSleeve.visible = false;
        if (BedPose.drawingSelf
                && dev.dyslexic.cinematic.client.Scene.station
                        instanceof com.hanmoyun.dghcompat.TreatmentScene scene) {
            var health = scene.patientHealth();
            model.rightLeg.visible =
                    health != null
                            && !health.getComponent(
                                            com.lastimp.dgh.common.enums.BodyComponents.RIGHT_LEG)
                                    .abnormal(
                                            com.lastimp
                                                    .dgh
                                                    .common
                                                    .capability
                                                    .bodyPart
                                                    .base
                                                    .BodyCondition
                                                    .SURGICAL_AMPUTATION)
                            && !health.getComponent(
                                            com.lastimp.dgh.common.enums.BodyComponents.RIGHT_LEG)
                                    .abnormal(
                                            com.lastimp
                                                    .dgh
                                                    .common
                                                    .capability
                                                    .bodyPart
                                                    .base
                                                    .BodyCondition
                                                    .TRAUMATIC_AMPUTATION);
            model.leftLeg.visible =
                    health != null
                            && !health.getComponent(
                                            com.lastimp.dgh.common.enums.BodyComponents.LEFT_LEG)
                                    .abnormal(
                                            com.lastimp
                                                    .dgh
                                                    .common
                                                    .capability
                                                    .bodyPart
                                                    .base
                                                    .BodyCondition
                                                    .SURGICAL_AMPUTATION)
                            && !health.getComponent(
                                            com.lastimp.dgh.common.enums.BodyComponents.LEFT_LEG)
                                    .abnormal(
                                            com.lastimp
                                                    .dgh
                                                    .common
                                                    .capability
                                                    .bodyPart
                                                    .base
                                                    .BodyCondition
                                                    .TRAUMATIC_AMPUTATION);
            model.rightPants.visible = model.rightLeg.visible;
            model.leftPants.visible = model.leftLeg.visible;
            model.rightArm.visible = scene.showBodyArm(true);
            model.leftArm.visible = scene.showBodyArm(false);
            model.rightSleeve.visible = model.rightArm.visible;
            model.leftSleeve.visible = model.leftArm.visible;
        } else {
            com.lastimp.dgh.common.capability.HealthCapability.getAndApply(
                    patient,
                    health -> {
                        var right =
                                health.getComponent(
                                        com.lastimp.dgh.common.enums.BodyComponents.RIGHT_ARM);
                        var left =
                                health.getComponent(
                                        com.lastimp.dgh.common.enums.BodyComponents.LEFT_ARM);
                        model.rightArm.visible =
                                !right.abnormal(
                                                com.lastimp
                                                        .dgh
                                                        .common
                                                        .capability
                                                        .bodyPart
                                                        .base
                                                        .BodyCondition
                                                        .SURGICAL_AMPUTATION)
                                        && !right.abnormal(
                                                com.lastimp
                                                        .dgh
                                                        .common
                                                        .capability
                                                        .bodyPart
                                                        .base
                                                        .BodyCondition
                                                        .TRAUMATIC_AMPUTATION);
                        model.leftArm.visible =
                                !left.abnormal(
                                                com.lastimp
                                                        .dgh
                                                        .common
                                                        .capability
                                                        .bodyPart
                                                        .base
                                                        .BodyCondition
                                                        .SURGICAL_AMPUTATION)
                                        && !left.abnormal(
                                                com.lastimp
                                                        .dgh
                                                        .common
                                                        .capability
                                                        .bodyPart
                                                        .base
                                                        .BodyCondition
                                                        .TRAUMATIC_AMPUTATION);
                        model.rightSleeve.visible = model.rightArm.visible;
                        model.leftSleeve.visible = model.leftArm.visible;
                    });
        }
        if (BedPose.drawingSelf) {
            model.head.visible = false;
            model.hat.visible = false;
        }
    }
}
