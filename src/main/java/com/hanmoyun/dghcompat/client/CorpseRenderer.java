package com.hanmoyun.dghcompat.client;

import com.hanmoyun.dghcompat.CompatMod;
import com.hanmoyun.dghcompat.corpse.CorpseEntity;
import com.hanmoyun.dghcompat.corpse.Corpses;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** The surgical carrier uses the same adult six-part layout as the bridge's treatment UI. */
@EventBusSubscriber(modid = CompatMod.ID, value = Dist.CLIENT)
public final class CorpseRenderer
        extends HumanoidMobRenderer<CorpseEntity, PlayerModel<CorpseEntity>> {
    public CorpseRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
    }

    @Override
    public void render(
            CorpseEntity corpse,
            float yaw,
            float tick,
            com.mojang.blaze3d.vertex.PoseStack pose,
            net.minecraft.client.renderer.MultiBufferSource buffers,
            int light) {
        if (RagdollAdapter.rendersCorpse(corpse)
                && !com.hanmoyun.dghcompat.StasisPatients.wrapped(corpse)
                && !corpse.isPassenger()) return;
        var visual = CorpseVisuals.get(corpse);
        if (com.hanmoyun.dghcompat.StasisPatients.wrapped(corpse) || visual == corpse) {
            super.render(corpse, yaw, tick, pose, buffers, light);
        } else {
            pose.pushPose();
            if (!corpse.isPassenger()) {
                pose.translate(0, 0.25, 0);
                pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-yaw));
                pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90));
                pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(yaw));
            }
            net.minecraft.client.Minecraft.getInstance()
                    .getEntityRenderDispatcher()
                    .getRenderer(visual)
                    .render(visual, yaw, tick, pose, buffers, light);
            pose.popPose();
        }
    }

    @Override
    public ResourceLocation getTextureLocation(CorpseEntity entity) {
        return ResourceLocation.withDefaultNamespace("textures/entity/player/wide/steve.png");
    }

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(
                Corpses.LOOT.get(), net.minecraft.client.renderer.entity.NoopRenderer::new);
        event.registerEntityRenderer(Corpses.TYPE.get(), CorpseRenderer::new);
    }
}
