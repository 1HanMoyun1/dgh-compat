package com.hanmoyun.dghcompat.mixin;

import com.hanmoyun.dghcompat.corpse.Corpses;
import com.hanmoyun.dghcompat.treatment.TreatmentService;
import com.lastimp.dgh.common.capability.HealthCapability;
import com.lastimp.dgh.common.menu.HealthMenu;
import com.lastimp.dgh.common.network.message.MyKeyPressedData;
import com.lastimp.dgh.common.utils.Utils;
import com.lastimp.dgh.neoforge.network.handler.ServerPayloadHandler;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Consumer;

/** DGH's regular screen must bind the organ slots to the corpse rather than to its doctor. */
@Mixin(value = ServerPayloadHandler.class, remap = false)
abstract class CorpseOrganMenuMixin {
    @WrapOperation(
            method =
                    "lambda$handleClientPress$4(Lnet/neoforged/neoforge/network/handling/IPayloadContext;Lcom/lastimp/dgh/common/network/message/MyKeyPressedData;)V",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lcom/lastimp/dgh/common/capability/HealthCapability;getAndApply(Lnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Consumer;)V"),
            require = 0)
    private static void corpseOrgans(
            LivingEntity source,
            Consumer<HealthCapability> apply,
            Operation<Void> original,
            IPayloadContext context,
            MyKeyPressedData data) {
        if (source instanceof ServerPlayer doctor
                && doctor.containerMenu instanceof HealthMenu menu
                && Corpses.isCorpse(
                        Utils.getLivingWithHealth(doctor.serverLevel(), menu.targetEntity))) {
            int index = data.index();
            int part = index == 0 ? 0 : Math.abs(index) - 1;
            TreatmentService.organMode(doctor, menu.containerId, part, index > 0);
        } else {
            original.call(source, apply);
        }
    }
}
