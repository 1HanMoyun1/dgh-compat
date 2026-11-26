package com.hanmoyun.dghcompat.mixin;

import com.hanmoyun.dghcompat.GroundTreatment;
import com.hanmoyun.dghcompat.CaseBook;
import com.hanmoyun.dghcompat.CaseBookChinese;
import com.lastimp.dgh.common.capability.bodyPart.base.AbstractVisibleBody;
import com.lastimp.dgh.common.menu.HealthMenu;
import com.lastimp.dgh.common.utils.Utils;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = HealthMenu.class, remap = false)
abstract class GroundHealthMenuMixin extends AbstractContainerMenu {
    @Unique private AbstractVisibleBody dghcompat$openBody;

    protected GroundHealthMenuMixin(MenuType<?> type, int id) {
        super(type, id);
    }

    @Inject(method = "setOrganActive", at = @At("RETURN"))
    private void trackOrganPage(boolean active, AbstractVisibleBody body, CallbackInfo ci) {
        dghcompat$openBody = active ? body : null;
    }

    @Override
    public void clicked(int slot, int button, ClickType type, Player player) {
        if (!(player instanceof ServerPlayer doctor) || dghcompat$openBody == null
                || slot < 47 || slot >= 83) {
            super.clicked(slot, button, type, player);
            return;
        }
        var before = getSlot(slot).getItem().copy();
        super.clicked(slot, button, type, player);
        var after = getSlot(slot).getItem();
        if (ItemStack.matches(before, after)) return;
        var menu = (HealthMenu) (Object) this;
        var patient = Utils.getLivingWithHealth(doctor.serverLevel(), menu.targetEntity);
        if (patient == null) return;
        String action = before.isEmpty() ? "植入" + CaseBookChinese.item(after)
                : after.isEmpty() ? "取出" + CaseBookChinese.item(before)
                : "将" + CaseBookChinese.item(before) + "换为" + CaseBookChinese.item(after);
        CaseBook.record(doctor, patient, dghcompat$openBody.getBodyType(), action);
    }

    @Inject(method = "stillValid", at = @At("HEAD"), cancellable = true)
    private void groundReach(Player player, CallbackInfoReturnable<Boolean> ci) {
        if (player instanceof ServerPlayer doctor
                && !GroundTreatment.valid(doctor, (HealthMenu) (Object) this))
            ci.setReturnValue(false);
    }
}
