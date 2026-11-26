package com.hanmoyun.dghcompat.mixin;

import com.hanmoyun.dghcompat.client.CorpseVisuals;
import com.hanmoyun.dghcompat.corpse.CorpseEntity;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.raiiiden.ragdollified.client.ClientRagdoll;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "com.raiiiden.ragdollified.client.ClientRagdollRenderer", remap = false)
abstract class NativeCorpseSkinMixin {
    @ModifyExpressionValue(method = "renderPlayerRagdoll", at = @At(value = "INVOKE",
            target = "Lcom/raiiiden/ragdollified/client/ClientRagdollRenderer;findPlayerEntity(Ljava/util/UUID;)Lnet/minecraft/client/player/AbstractClientPlayer;"))
    private static AbstractClientPlayer corpsePlayer(AbstractClientPlayer original, @Local(argsOnly = true) ClientRagdoll body) {
        var level = Minecraft.getInstance().level;
        if (level != null && level.getEntity(body.getOriginalEntityId()) instanceof CorpseEntity corpse
                && CorpseVisuals.get(corpse) instanceof AbstractClientPlayer visual) return visual;
        return original;
    }

    @ModifyExpressionValue(method = "renderPlayerRagdoll", at = @At(value = "INVOKE",
            target = "Lcom/raiiiden/ragdollified/client/ClientPlayerSkinCache;drawable(Lnet/minecraft/resources/ResourceLocation;Ljava/util/UUID;)Lnet/minecraft/resources/ResourceLocation;"))
    private static ResourceLocation corpseSkin(ResourceLocation original, @Local(argsOnly = true) ClientRagdoll body) {
        var level = Minecraft.getInstance().level;
        if (level != null && level.getEntity(body.getOriginalEntityId()) instanceof CorpseEntity corpse
                && corpse.visual().getBoolean("missing_skin") && net.neoforged.fml.ModList.get().isLoaded("createcybernetics"))
            return ResourceLocation.fromNamespaceAndPath("createcybernetics", "textures/entity/playermuscles_wide.png");
        return original;
    }
}
