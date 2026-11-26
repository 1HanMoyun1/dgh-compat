package com.hanmoyun.dghcompat.mixin;

import com.lastimp.dgh.common.entry.register.ModCreativeModTabs;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Put DGH after vanilla's last content tab, instead of between vanilla tabs. */
@Mixin(value = ModCreativeModTabs.class, remap = false)
abstract class CreativeTabOrderMixin {
    @Redirect(
            method = "lambda$static$2",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/item/CreativeModeTab$Builder;withTabsBefore([Lnet/minecraft/resources/ResourceKey;)Lnet/minecraft/world/item/CreativeModeTab$Builder;"))
    private static CreativeModeTab.Builder order(
            CreativeModeTab.Builder builder, ResourceKey<CreativeModeTab>[] ignored) {
        return builder.withTabsBefore(CreativeModeTabs.SPAWN_EGGS);
    }
}
