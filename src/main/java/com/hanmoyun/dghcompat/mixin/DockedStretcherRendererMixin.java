package com.hanmoyun.dghcompat.mixin;

import com.lastimp.dgh.common.client.renderer.StretcherRenderer;
import com.lastimp.dgh.common.entity.StretcherEntity;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = StretcherRenderer.class, remap = false)
abstract class DockedStretcherRendererMixin {
    @Inject(
            method =
                    "render(Lcom/lastimp/dgh/common/entity/StretcherEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"),
            cancellable = true)
    private void hideDocked(
            StretcherEntity entity,
            float yaw,
            float tick,
            PoseStack pose,
            MultiBufferSource buffers,
            int light,
            CallbackInfo ci) {
        if (entity.isInvisible()) ci.cancel();
    }
}
