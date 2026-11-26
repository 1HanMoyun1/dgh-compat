package com.hanmoyun.dghcompat.mixin;

import com.mojang.math.Transformation;

import net.minecraft.world.entity.Display;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Display.class)
public interface CaseDisplayAccess {
    @Invoker("setTransformation")
    void caseSetTransform(Transformation transformation);
}
