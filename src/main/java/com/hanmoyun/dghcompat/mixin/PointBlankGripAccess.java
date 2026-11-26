package com.hanmoyun.dghcompat.mixin;

import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Invoker;

@Pseudo
@Mixin(targets = "com.vicmatskiv.pointblank.item.GunItem", remap = false)
public interface PointBlankGripAccess {
    @Invoker("isDualWieldingEnabled")
    boolean dghcompat$allowsOneHand(ItemStack stack);
}
