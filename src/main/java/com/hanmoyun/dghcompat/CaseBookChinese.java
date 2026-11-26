package com.hanmoyun.dghcompat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lastimp.dgh.common.capability.HealthCapability;

import net.minecraft.world.item.ItemStack;

import org.slf4j.LoggerFactory;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class CaseBookChinese {
    private CaseBookChinese() {}

    private static final class Text {
        private static final JsonObject ZH = load();

        private static JsonObject load() {
            try (var input =
                    HealthCapability.class.getResourceAsStream("/assets/dgh/lang/zh_cn.json")) {
                if (input != null) {
                    try (var reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                        return JsonParser.parseReader(reader).getAsJsonObject();
                    }
                }
            } catch (java.io.IOException | RuntimeException failure) {
                LoggerFactory.getLogger(CompatMod.ID)
                        .warn("Cannot read DGH Chinese case notes", failure);
            }
            return new JsonObject();
        }
    }

    public static String label(String key, String fallback) {
        var text = Text.ZH.get(key);
        return text != null && text.isJsonPrimitive() ? text.getAsString() : fallback;
    }

    public static String item(ItemStack stack) {
        return label(stack.getItem().getDescriptionId(), stack.getHoverName().getString());
    }
}
