package com.hanmoyun.dghcompat;

import com.lastimp.dgh.common.enums.BodyComponents;

import net.minecraft.network.chat.Component;

import java.util.Locale;

public final class PartNames {
    private PartNames() {}

    public static Component name(BodyComponents part) {
        return Component.translatable("dghcompat.part." + part.name().toLowerCase(Locale.ROOT));
    }
}
