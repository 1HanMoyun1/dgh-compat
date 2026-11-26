package com.hanmoyun.dghcompat.mixin;

import net.minecraft.world.entity.Display.ItemDisplay;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ItemDisplay.class)
public interface CaseItemDisplayAccess {
    @Invoker("setItemStack")
    void caseSetItem(ItemStack stack);

    @Invoker("getItemStack")
    ItemStack caseGetItem();

    @Invoker("setItemTransform")
    void caseSetContext(ItemDisplayContext context);
}
