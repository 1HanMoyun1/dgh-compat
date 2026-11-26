package com.hanmoyun.dghcompat.client;

import com.hanmoyun.dghcompat.CompatMod;
import com.hanmoyun.dghcompat.StasisPatients;
import com.hanmoyun.dghcompat.TransportOrientation;
import com.lastimp.dgh.common.entity.StretcherEntity;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;

/** Reuses a dedicated bag model until model layers are rebuilt by a resource reload. */
@EventBusSubscriber(modid = CompatMod.ID, value = Dist.CLIENT)
public final class StasisBagRenderer {
    private static ResourceLocation texture;
    private static ResourceLocation deadTexture;
    private static PlayerModel<LivingEntity> model;

    private StasisBagRenderer() {}

    @SubscribeEvent
    public static void reloadModels(EntityRenderersEvent.AddLayers event) {
        model = new PlayerModel<>(event.getEntityModels().bakeLayer(ModelLayers.PLAYER), false);
        model.young = false;
        model.hat.visible = false;
        model.jacket.visible = false;
        model.leftSleeve.visible = model.rightSleeve.visible = false;
        model.leftPants.visible = model.rightPants.visible = false;
    }

    private static ResourceLocation texture(boolean dead) {
        if (dead ? deadTexture != null : texture != null) return dead ? deadTexture : texture;
        // A deterministic Steve UV skin: sealed hood, opaque visor and teal straps.
        NativeImage image = new NativeImage(64, 64, false);
        for (int y = 0; y < 64; y++)
            for (int x = 0; x < 64; x++) {
                int color = 0xffc47d28; // NativeImage uses ABGR: blue fabric.
                // Limb UVs start at (0,16), (40,16), (16,48), (32,48).
                // Draw the same seam and straps relative to each limb, including back faces.
                int limbX = -1, limbY = -1;
                if (y >= 20 && y < 32 && x < 16) {
                    limbX = x;
                    limbY = y - 20;
                }
                if (y >= 20 && y < 32 && x >= 40 && x < 56) {
                    limbX = x - 40;
                    limbY = y - 20;
                }
                if (y >= 52 && y < 64 && x >= 16 && x < 48) {
                    limbX = (x - 16) % 16;
                    limbY = y - 52;
                }
                if (limbX >= 0 && limbX % 4 == 0) color = 0xffa15b20;
                if (limbY == 3 || limbY == 9) color = 0xffeed7ac;
                if (x >= 20 && x < 28 && y >= 20 && y < 32 && x == 24) color = 0xffa15b20;
                if (x >= 8 && x < 16 && y >= 10 && y < 14) color = 0xff332b22;
                // Front torso UV: chest mark, a green plus for living patients and red X for
                // corpses.
                if (x >= 21 && x <= 26 && y >= 21 && y <= 26) {
                    int dx = x - 21, dy = y - 21;
                    boolean mark =
                            dead
                                    ? dx == dy || dx + dy == 5
                                    : dx == 2 || dx == 3 || dy == 2 || dy == 3;
                    if (mark) color = dead ? 0xff3030ef : 0xff40df40;
                }
                image.setPixelRGBA(x, y, color);
            }
        ResourceLocation generated =
                Minecraft.getInstance()
                        .getTextureManager()
                        .register(
                                dead ? "dgh_corpse_bag" : "dgh_stasis_bag",
                                new DynamicTexture(image));
        if (dead) deadTexture = generated;
        else texture = generated;
        return generated;
    }

    @SubscribeEvent
    public static void render(RenderLivingEvent.Pre<?, ?> event) {
        var patient = event.getEntity();
        if (patient.isDeadOrDying()
                && patient.getPersistentData().getBoolean("dghcompat:down_visual")
                && !StasisPatients.wrapped(patient)
                && com.hanmoyun.dghcompat.integration.RagdollBodies.available()) {
            event.setCanceled(true);
            return;
        }
        if (renderBag(
                event.getEntity(),
                event.getPoseStack(),
                event.getMultiBufferSource(),
                event.getPackedLight())) event.setCanceled(true);
    }

    public static boolean renderBag(
            LivingEntity patient,
            com.mojang.blaze3d.vertex.PoseStack pose,
            net.minecraft.client.renderer.MultiBufferSource buffers,
            int light) {
        if (model == null || !StasisPatients.wrapped(patient)) return false;

        pose.pushPose();
        try {
            boolean carried = patient.getPersistentData().getBoolean("dghcompat:carry_render");
            if (carried) {
                pose.mulPose(Axis.YP.rotationDegrees(90));
                pose.translate(0, 0, -0.9);
            } else if (patient.getVehicle() instanceof StretcherEntity carrier) {
                pose.mulPose(
                        Axis.YP.rotationDegrees(
                                TransportOrientation.modelYaw(
                                        carrier.getYRot(), carrier.isInvisible())));
            } else {
                pose.translate(0, 0.25, 0);
                pose.mulPose(Axis.YP.rotationDegrees(180 - patient.getYRot()));
                pose.translate(0, 0, -1.0);
            }
            pose.mulPose(Axis.XP.rotationDegrees(90));
            pose.scale(-1, -1, 1);
            pose.translate(0, -1.501, 0);
            model.renderToBuffer(
                    pose,
                    buffers.getBuffer(
                            RenderType.entityCutoutNoCull(
                                    texture(
                                            com.hanmoyun.dghcompat.corpse.Corpses.isCorpse(patient)
                                                    || patient.isDeadOrDying()))),
                    light,
                    OverlayTexture.NO_OVERLAY,
                    -1);
        } finally {
            pose.popPose();
        }
        return true;
    }
}
