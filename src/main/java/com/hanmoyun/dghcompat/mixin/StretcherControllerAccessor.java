package com.hanmoyun.dghcompat.mixin;

import com.lastimp.dgh.common.entity.StretcherEntity;

import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = StretcherEntity.class, remap = false)
public interface StretcherControllerAccessor {
    @Accessor("controller")
    LivingEntity dghcompat$controller();
}
