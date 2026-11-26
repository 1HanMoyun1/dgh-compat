package com.hanmoyun.dghcompat;

import com.lastimp.dgh.common.capability.HealthCapability;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Array;
import java.util.Set;

/** Keep implants with the patient's loadout while their DGH body part is absent. */
final class SuspendedImplants {
    private static final Set<String> GROUPS =
            Set.of("LARM", "RARM", "LLEG", "RLEG", "BRAIN", "EYES", "HEART", "LUNGS", "ORGANS");

    private SuspendedImplants() {}

    static void update(LivingEntity patient, HealthCapability health) {
        try {
            Object data = CyberPatientData.cyberware(patient);
            if (!(data instanceof SuspendedLoadout loadout)) return;
            CompoundTag inactive = loadout.dghcompat$suspended();
            for (var group : CyberPatientData.groups(data).entrySet()) {
                if (!(group.getKey() instanceof Enum<?> slot) || !GROUPS.contains(slot.name()))
                    continue;
                Object entries = group.getValue();
                if (entries == null) continue;
                for (int i = 0; i < Array.getLength(entries); i++) {
                    Object entry = Array.get(entries, i);
                    String key = slot.name() + "_" + i;
                    if (entry == null) {
                        if (inactive.contains(key))
                            restore(
                                    patient,
                                    health,
                                    data,
                                    slot,
                                    i,
                                    entries.getClass().getComponentType(),
                                    inactive,
                                    key);
                        continue;
                    }

                    ItemStack stack = CyberPatientData.item(entry);
                    var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
                    if (stack.isEmpty()
                            || (id.getNamespace().equals("createcybernetics")
                                    && id.getPath().startsWith("bodypart_"))
                            || AnatomicalItems.available(health, slot.name(), stack)) continue;
                    suspend(patient, data, slot, i, entry, stack, inactive, key);
                }
            }
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(
                    "Unable to reconcile DGH anatomy with the installed Cybernetics loadout",
                    error);
        }
    }

    private static void suspend(
            LivingEntity patient,
            Object data,
            Enum<?> slot,
            int index,
            Object entry,
            ItemStack stack,
            CompoundTag inactive,
            String key)
            throws ReflectiveOperationException {
        Class<?> group = slot.getDeclaringClass();
        var saved =
                (CompoundTag)
                        entry.getClass()
                                .getMethod("save", HolderLookup.Provider.class)
                                .invoke(entry, patient.registryAccess());
        saved.putBoolean(
                "bridge_enabled",
                (boolean)
                        data.getClass()
                                .getMethod("isEnabled", group, int.class)
                                .invoke(data, slot, index));
        var callback = lifecycle(stack, "onRemoved", patient, false);
        data.getClass().getMethod("remove", group, int.class).invoke(data, slot, index);
        if (inactive.contains(key)) saved.put("bridge_previous", inactive.getCompound(key));
        inactive.put(key, saved);
        if (callback != null) callback.invoke(stack.getItem(), patient);
    }

    private static void restore(
            LivingEntity patient,
            HealthCapability health,
            Object data,
            Enum<?> slot,
            int index,
            Class<?> type,
            CompoundTag inactive,
            String key)
            throws ReflectiveOperationException {
        if (!slot.name().equals("ORGANS")
                && !AnatomicalItems.available(health, slot.name(), ItemStack.EMPTY)) return;
        var saved = inactive.getCompound(key);
        // Preserve item components used by Cybernetics when resolving a shared abdominal slot.
        var stack = ItemStack.parseOptional(patient.registryAccess(), saved.getCompound("Item"));
        if (stack.isEmpty() || !AnatomicalItems.available(health, slot.name(), stack)) return;
        var entry =
                type.getMethod("load", CompoundTag.class, HolderLookup.Provider.class)
                        .invoke(null, saved, patient.registryAccess());
        var installedStack = CyberPatientData.item(entry);
        if (installedStack.isEmpty()) return;
        var callback = lifecycle(installedStack, "onInstalled", patient, true);
        Class<?> group = slot.getDeclaringClass();
        var enabled = data.getClass().getMethod("setEnabled", group, int.class, boolean.class);
        data.getClass().getMethod("set", group, int.class, type).invoke(data, slot, index, entry);
        enabled.invoke(data, slot, index, saved.getBoolean("bridge_enabled"));
        if (saved.contains("bridge_previous"))
            inactive.put(key, saved.getCompound("bridge_previous"));
        else inactive.remove(key);
        if (callback != null) callback.invoke(installedStack.getItem(), patient, installedStack);
    }

    private static java.lang.reflect.Method lifecycle(
            ItemStack stack, String name, LivingEntity patient, boolean withStack)
            throws NoSuchMethodException {
        if (!CyberApi.isCyberware(stack)) return null;
        Class<?> owner = patient instanceof Player ? Player.class : LivingEntity.class;
        return withStack
                ? stack.getItem().getClass().getMethod(name, owner, ItemStack.class)
                : stack.getItem().getClass().getMethod(name, owner);
    }
}
