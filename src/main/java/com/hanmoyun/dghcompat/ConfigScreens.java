package com.hanmoyun.dghcompat;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.lastimp.dgh.neoforge.config.ConfigNF;

import me.shedaniel.clothconfig2.api.ConfigBuilder;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.Optional;

@EventBusSubscriber(modid = CompatMod.ID, value = Dist.CLIENT)
public final class ConfigScreens {
    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ModList.get().getModContainerById("dgh").orElseThrow()
                .registerExtensionPoint(IConfigScreenFactory.class, (container, parent) -> screen(parent)));
    }

    private static Screen screen(Screen parent) {
        var spec = ConfigNF.SPEC;
        var builder =
                ConfigBuilder.create()
                        .setParentScreen(parent)
                        .setTitle(Component.translatable("dghcyber.config.dgh"));
        var category =
                builder.getOrCreateCategory(Component.translatable("dghcyber.config.general"));
        if (spec.isLoaded()) add(builder, category, spec.getValues());
        if (spec.isLoaded()) builder.setSavingRunnable(spec::save);
        return builder.build();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void add(
            ConfigBuilder builder,
            me.shedaniel.clothconfig2.api.ConfigCategory category,
            UnmodifiableConfig config) {
        for (var entry : config.valueMap().entrySet()) {
            if (entry.getValue() instanceof UnmodifiableConfig nested) {
                add(builder, category, nested);
                continue;
            }
            if (!(entry.getValue() instanceof ModConfigSpec.ConfigValue value)) continue;
            Object current = value.get(), def = value.getDefault();
            var label = Component.translatable("dghcyber.option." + entry.getKey());
            if (current instanceof Boolean b)
                category.addEntry(
                        builder.entryBuilder()
                                .startBooleanToggle(label, b)
                                .setDefaultValue((Boolean) def)
                                .setSaveConsumer(value::set)
                                .build());
            else if (current instanceof Integer n)
                category.addEntry(
                        builder.entryBuilder()
                                .startIntField(label, n)
                                .setDefaultValue((Integer) def)
                                .setErrorSupplier(
                                        v ->
                                                value.getSpec().test(v)
                                                        ? Optional.empty()
                                                        : Optional.of(
                                                                Component.translatable(
                                                                        "dghcyber.config.invalid")))
                                .setSaveConsumer(value::set)
                                .build());
            else if (current instanceof Double n)
                category.addEntry(
                        builder.entryBuilder()
                                .startDoubleField(label, n)
                                .setDefaultValue((Double) def)
                                .setErrorSupplier(
                                        v ->
                                                Double.isFinite(v) && value.getSpec().test(v)
                                                        ? Optional.empty()
                                                        : Optional.of(
                                                                Component.translatable(
                                                                        "dghcyber.config.invalid")))
                                .setSaveConsumer(value::set)
                                .build());
        }
    }
}
