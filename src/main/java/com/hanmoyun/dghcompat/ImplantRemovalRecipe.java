package com.hanmoyun.dghcompat;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ImplantRemovalRecipe extends CustomRecipe {
    static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(
                    net.minecraft.core.registries.Registries.RECIPE_SERIALIZER, CompatMod.ID);
    private static final java.util.function.Supplier<RecipeSerializer<?>> TYPE =
            SERIALIZERS.register(
                    "remove_implant",
                    () -> new SimpleCraftingRecipeSerializer<>(ImplantRemovalRecipe::new));

    public ImplantRemovalRecipe(CraftingBookCategory category) {
        super(category);
    }

    private static int limbSlot(CraftingInput input) {
        if (input.ingredientCount() != 1) return -1;
        for (int i = 0; i < input.size(); i++)
            if (!input.getItem(i).isEmpty()) return input.getItem(i).getCount() == 1 ? i : -1;
        return -1;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        int slot = limbSlot(input);
        return slot >= 0 && !BodyItems.top(input.getItem(slot), level.registryAccess()).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        int slot = limbSlot(input);
        return slot < 0 ? ItemStack.EMPTY : BodyItems.top(input.getItem(slot), registries);
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        var remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        int slot = limbSlot(input);
        if (slot >= 0) remaining.set(slot, BodyItems.afterRemoval(input.getItem(slot)));
        return remaining;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width == 3 && height == 3;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return TYPE.get();
    }
}
