package com.hanmoyun.dghcompat.mixin;

import com.hanmoyun.dghcompat.ArmState;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.ClientHooks;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = ClientHooks.class, remap = false)
abstract class ArmorArmMixin {
    @ModifyReturnValue(
            method = "getArmorModel(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/EquipmentSlot;Lnet/minecraft/client/model/HumanoidModel;)Lnet/minecraft/client/model/Model;",
            at = @At("RETURN"))
    private static Model hideMissingLimbs(Model model, LivingEntity wearer) {
        if (wearer instanceof Player player && model instanceof HumanoidModel<?> armor) {
            armor.leftArm.visible &= ArmState.visible(player, HumanoidArm.LEFT);
            armor.rightArm.visible &= ArmState.visible(player, HumanoidArm.RIGHT);
            armor.leftLeg.visible &= ArmState.visibleLeg(player, HumanoidArm.LEFT);
            armor.rightLeg.visible &= ArmState.visibleLeg(player, HumanoidArm.RIGHT);
        }
        return model;
    }
}
