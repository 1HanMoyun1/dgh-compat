package com.hanmoyun.dghcompat.mixin;

import me.shedaniel.clothconfig2.gui.widget.SearchFieldEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(value = SearchFieldEntry.class, remap = false)
abstract class ConfigSearchMixin {
    @ModifyConstant(method = "render", constant = @Constant(stringValue = "Search..."), require = 0)
    private String localizeSearch(String original) {
        var screen = Minecraft.getInstance().screen;
        if (screen != null && screen.getTitle().getContents() instanceof TranslatableContents title
                && title.getKey().equals("dghcyber.config.dgh"))
            return Component.translatable("dghcyber.config.search").getString();
        return original;
    }
}
