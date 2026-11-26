package com.hanmoyun.dghcompat.mixin;

import com.hanmoyun.dghcompat.ImplantRemovalRecipe;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;

/** The trimmed recipe input is identical for a workbench and a pocket crafting grid. */
@Mixin(CraftingMenu.class)
abstract class ImplantWorkbenchMixin {
    @ModifyExpressionValue(
            method = "slotChangedCraftingGrid",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/item/crafting/RecipeManager;getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/crafting/RecipeHolder;)Ljava/util/Optional;"))
    private static Optional<RecipeHolder<CraftingRecipe>> workbenchOnly(
            Optional<RecipeHolder<CraftingRecipe>> match, AbstractContainerMenu menu) {
        if (menu instanceof CraftingMenu) return match;
        return match.filter(recipe -> !(recipe.value() instanceof ImplantRemovalRecipe));
    }
}
