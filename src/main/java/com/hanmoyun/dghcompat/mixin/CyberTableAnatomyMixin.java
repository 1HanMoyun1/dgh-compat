package com.hanmoyun.dghcompat.mixin;

import com.hanmoyun.dghcompat.CyberAnatomy;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(
        targets =
                "com.perigrine3.createcybernetics.screen.custom.surgery.surgery_table.SurgeryTableMenu",
        remap = false)
abstract class CyberTableAnatomyMixin {
    @WrapMethod(method = "clicked", require = 0)
    private void guardClicks(
            int slotId, int button, ClickType type, Player operator, Operation<Void> original) {
        var menu = (AbstractContainerMenu) (Object) this;
        var previous = CyberAnatomy.exchangePatient(CyberAnatomy.patient(menu, operator));
        try {
            if (slotId >= 0 && slotId < menu.slots.size()) {
                var slot = menu.getSlot(slotId);
                if (slot.getClass().getName().endsWith(".RobosurgeonSlotItemHandler")) {
                    int index = slot.getSlotIndex();
                    if (!CyberAnatomy.mayEdit(index, slot.getItem())
                            || !menu.getCarried().isEmpty()
                                    && !CyberAnatomy.mayInstall(index, menu.getCarried())) return;
                }
            }
            original.call(slotId, button, type, operator);
        } finally {
            CyberAnatomy.exchangePatient(previous);
        }
    }

    @WrapMethod(method = "quickMoveStack", require = 0)
    private ItemStack guardTransfers(Player operator, int slotId, Operation<ItemStack> original) {
        var menu = (AbstractContainerMenu) (Object) this;
        var previous = CyberAnatomy.exchangePatient(CyberAnatomy.patient(menu, operator));
        try {
            return original.call(operator, slotId);
        } finally {
            CyberAnatomy.exchangePatient(previous);
        }
    }
}
