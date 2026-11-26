package com.hanmoyun.dghcompat;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/** Resolves the supplied Cybernetics API once, without bundling any upstream code. */
public final class CyberApi {
    private static final String ROOT = "com.perigrine3.createcybernetics.";
    private static CyberApi instance;
    private final Class<?> itemType;
    private final Method getAll, getItem, getSlot, getIndex, isPowered, isEnabled;
    private final Method replacesOrgan, replacedOrgans, replacedTag, needsEnergy, functional;

    private CyberApi() throws ReflectiveOperationException {
        Class<?> access = Class.forName(ROOT + "common.capabilities.CyberwareAccess");
        Class<?> installed = Class.forName(ROOT + "api.InstalledCyberware");
        Class<?> slot = Class.forName(ROOT + "api.CyberwareSlot");
        itemType = Class.forName(ROOT + "api.ICyberwareItem");
        getAll = access.getMethod("getAll", LivingEntity.class);
        isEnabled = access.getMethod("isEnabled", LivingEntity.class, slot, int.class);
        getItem = installed.getMethod("getItem");
        getSlot = installed.getMethod("getSlot");
        getIndex = installed.getMethod("getIndex");
        isPowered = installed.getMethod("isPowered");
        replacesOrgan = itemType.getMethod("replacesOrgan");
        replacedOrgans = itemType.getMethod("getReplacedOrgans");
        replacedTag = itemType.getMethod("getReplacedOrganItemTag", ItemStack.class, slot);
        needsEnergy =
                itemType.getMethod(
                        "requiresEnergyToFunction", LivingEntity.class, ItemStack.class, slot);
        functional =
                Class.forName(ROOT + "common.durability.CyberwareDurabilityManager")
                        .getMethod("isFunctional", LivingEntity.class, installed);
    }

    static boolean isCyberware(ItemStack stack) {
        return instance != null && instance.itemType.isInstance(stack.getItem());
    }

    public static boolean available() {
        return instance != null;
    }

    public static void initialize() {
        instance = null;
        if (!net.neoforged.fml.ModList.get().isLoaded("createcybernetics")) {
            org.slf4j.LoggerFactory.getLogger(CompatMod.ID)
                    .info("Cybernetics absent: skipping its optional adapter");
            return;
        }
        try {
            CyberApi candidate = new CyberApi();
            candidate.registerSurgeryEvent();
            instance = candidate;
        } catch (ReflectiveOperationException | LinkageError error) {
            instance = null;
            org.slf4j.LoggerFactory.getLogger(CompatMod.ID)
                    .warn(
                            "Cybernetics adapter unavailable; body health, surgery and other"
                                    + " adapters remain enabled",
                            error);
        }
    }

    public static CyberSnapshot snapshot(LivingEntity entity) {
        if (instance == null) return CyberSnapshot.EMPTY;
        try {
            return instance.read(entity);
        } catch (ReflectiveOperationException e) {
            // A failed read must never be mistaken for a successful uninstall.
            throw new IllegalStateException("Failed to read installed Cybernetics replacements", e);
        }
    }

    private CyberSnapshot read(LivingEntity entity) throws ReflectiveOperationException {
        Map<?, ?> all = (Map<?, ?>) getAll.invoke(null, entity);
        List<CyberSnapshot.Implant> result = new ArrayList<>();
        for (Object array : all.values()) {
            if (array == null) continue;
            for (int i = 0; i < Array.getLength(array); i++) {
                Object installed = Array.get(array, i);
                if (installed == null) continue;
                ItemStack stack = (ItemStack) getItem.invoke(installed);
                if (stack == null || stack.isEmpty() || !itemType.isInstance(stack.getItem()))
                    continue;
                Object slot = getSlot.invoke(installed);
                if (!(slot instanceof Enum<?> namedSlot)) continue;
                int index = (int) getIndex.invoke(installed);
                boolean replacement =
                        (boolean) replacesOrgan.invoke(stack.getItem())
                                && ((Set<?>) replacedOrgans.invoke(stack.getItem())).contains(slot);
                boolean working =
                        (boolean) functional.invoke(null, entity, installed)
                                && (boolean) isEnabled.invoke(null, entity, slot, index)
                                && (!(boolean)
                                                needsEnergy.invoke(
                                                        stack.getItem(), entity, stack, slot)
                                        || (boolean) isPowered.invoke(installed));
                Object tag = replacedTag.invoke(stack.getItem(), stack, slot);
                result.add(
                        new CyberSnapshot.Implant(
                                namedSlot.name(),
                                index,
                                BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),
                                replacement,
                                working,
                                tag instanceof TagKey<?> key ? key.location().toString() : ""));
            }
        }
        return new CyberSnapshot(result);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void registerSurgeryEvent() throws ReflectiveOperationException {
        Class<? extends Event> type =
                (Class<? extends Event>) Class.forName(ROOT + "event.custom.CyberwareSurgeryEvent");
        Method player = type.getMethod("getPlayer");
        Method installed = type.getMethod("getInstalled");
        Method removed = type.getMethod("getRemoved");
        Class<?> change = Class.forName(ROOT + "event.custom.CyberwareSurgeryEvent$Change");
        Method slot = change.getMethod("slot");
        Consumer<Event> listener =
                event -> {
                    try {
                        Set<String> parts = new java.util.HashSet<>();
                        List<Object> changes = new ArrayList<>((List<?>) installed.invoke(event));
                        changes.addAll((List<?>) removed.invoke(event));
                        for (Object entry : changes) {
                            Object value = slot.invoke(entry);
                            if (value instanceof Enum<?> named)
                                parts.addAll(CompatRules.parts(named.name()));
                        }
                        CompatBridge.afterSurgery(
                                (net.minecraft.server.level.ServerPlayer) player.invoke(event),
                                parts);
                        CyberPatientData.refresh(
                                (net.minecraft.server.level.ServerPlayer) player.invoke(event));
                    } catch (ReflectiveOperationException e) {
                        throw new IllegalStateException(
                                "Failed to synchronize a Cybernetics surgery", e);
                    }
                };
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, (Class) type, listener);
    }
}
