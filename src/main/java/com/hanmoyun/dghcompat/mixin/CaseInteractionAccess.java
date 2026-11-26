package com.hanmoyun.dghcompat.mixin;

import net.minecraft.world.entity.Interaction;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Interaction.class)
public interface CaseInteractionAccess {
    @Invoker("setWidth")
    void caseSetWidth(float width);

    @Invoker("setHeight")
    void caseSetHeight(float height);
}
