package com.hanmoyun.dghcompat.integration;

import com.hanmoyun.dghcompat.CompatMod;
import com.hanmoyun.dghcompat.mixin.NeoSyncChamberAccess;
import com.lastimp.dgh.common.entry.register.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class NeoSyncBags {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CompatMod.ID);
    private static final DeferredItem<Item> PACKED = ITEMS.registerSimpleItem(
            "packed_clone", new Item.Properties().stacksTo(1));
    private static final String SHELL = "shell";

    private NeoSyncBags() {}

    public static boolean complete(CompoundTag shell) {
        return shell.hasUUID("uuid") && shell.hasUUID("ownerUuid") && shell.getFloat("progress") >= 1.0F;
    }

    public static void use(PlayerInteractEvent.RightClickBlock event) {
        var held = event.getItemStack();
        boolean packing = held.is(ModItems.STASIS_BAG.get());
        if ((!packing && !held.is(PACKED)) || event.getUseBlock() == TriState.FALSE
                || event.getUseItem() == TriState.FALSE) return;
        var level = event.getLevel();
        var pos = event.getPos();
        var state = level.getBlockState(pos);
        var block = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (!block.getNamespace().equals("neosync")
                || !(block.getPath().equals("shell_constructor") || block.getPath().equals("shell_storage"))
                || !state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) return;
        if (state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER) pos = pos.below();
        var chamber = level.getBlockEntity(pos);
        if (!(chamber instanceof NeoSyncChamberAccess access) || !level.getBlockState(pos).is(state.getBlock())) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide()));
        if (level.isClientSide()) return;
        var player = event.getEntity();
        if (player.isSpectator() || !level.mayInteract(player, pos)
                || !player.mayUseItemAt(pos, event.getFace(), held)) return;

        // NeoSync's own serializer supplies registry context for inventory and extension components.
        var stateData = chamber.saveCustomOnly(level.registryAccess());
        CompoundTag shell;
        ItemStack result;
        if (packing) {
            shell = stateData.getCompound(SHELL);
            if (!complete(shell)) {
                player.displayClientMessage(Component.translatable("dghcompat.clone.incomplete"), true);
                return;
            }
            result = PACKED.toStack();
            var contents = new CompoundTag();
            contents.put(SHELL, shell.copy());
            result.set(DataComponents.CUSTOM_DATA, CustomData.of(contents));
            result.set(DataComponents.CUSTOM_NAME, Component.translatable("dghcompat.clone.named", shell.getString("ownerName")));
            stateData.remove(SHELL);
        } else {
            if (stateData.contains(SHELL)) {
                player.displayClientMessage(Component.translatable("dghcompat.clone.occupied"), true);
                return;
            }
            shell = held.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound(SHELL);
            if (!complete(shell)) return;
            shell.putString("world", level.dimension().location().toString());
            stateData.put(SHELL, shell);
            result = new ItemStack(ModItems.STASIS_BAG.get());
        }
        chamber.loadCustomOnly(stateData, level.registryAccess());
        access.dghcompat$syncShell(level, pos, chamber.getBlockState());
        chamber.setChanged();
        if (held.getCount() == 1) player.setItemInHand(event.getHand(), result);
        else {
            held.shrink(1);
            player.getInventory().placeItemBackInInventory(result);
        }
    }
}
