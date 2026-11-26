package com.hanmoyun.dghcompat.mixin;

import com.hanmoyun.dghcompat.CyberAnatomy;

import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(
        targets =
                "com.perigrine3.createcybernetics.screen.custom.surgery.robosurgeon.RobosurgeonSlotItemHandler",
        remap = false)
abstract class CyberSlotAnatomyMixin {
    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true, require = 0)
    private void checkPart(ItemStack stack, CallbackInfoReturnable<Boolean> ci) {
        if (!CyberAnatomy.mayInstall(((Slot) (Object) this).getSlotIndex(), stack))
            ci.setReturnValue(false);
    }
}
