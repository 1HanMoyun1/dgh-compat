package com.hanmoyun.dghcompat.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Invoker;

@Pseudo
@Mixin(targets = "com.breakinblocks.neosync.common.block.entity.AbstractShellContainerBlockEntity", remap = false)
public interface NeoSyncChamberAccess {
    @Invoker("checkShellState")
    void dghcompat$syncShell(Level level, BlockPos pos, BlockState state);
}
