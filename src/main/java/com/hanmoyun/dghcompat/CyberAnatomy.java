package com.hanmoyun.dghcompat;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

public final class CyberAnatomy {
    private static final String ROOT = "com.perigrine3.createcybernetics.";
    private static final ThreadLocal<Player> PATIENT = new ThreadLocal<>();

    private CyberAnatomy() {}

    public static Player patient(AbstractContainerMenu menu, Player operator) {
        try {
            return (Player)
                    menu.getClass()
                            .getMethod("getTargetPatient", Player.class)
                            .invoke(menu, operator);
        } catch (ReflectiveOperationException failure) {
            return null;
        }
    }

    public static Player exchangePatient(Player next) {
        Player previous = PATIENT.get();
        if (next == null) PATIENT.remove();
        else PATIENT.set(next);
        return previous;
    }

    private static Object slotAt(int index) throws ReflectiveOperationException {
        return Class.forName(ROOT + "common.surgery.RobosurgeonSlotMap")
                .getMethod("getSlotForIndex", int.class)
                .invoke(null, index);
    }

    public static boolean mayEdit(int inventoryIndex, ItemStack stack) {
        Player patient = PATIENT.get();
        if (patient == null) return true;
        try {
            var slot = slotAt(inventoryIndex);
            return !(slot instanceof Enum<?> named)
                    || AnatomicalItems.available(patient, named.name(), stack);
        } catch (ReflectiveOperationException failure) {
            return true;
        }
    }

    private static boolean withinCapacity(Player patient, Object slot, int index)
            throws ReflectiveOperationException {
        if (!(slot instanceof Enum<?> group)) return true;
        var mapping =
                (java.util.Map<?, ?>)
                        Class.forName(ROOT + "common.surgery.RobosurgeonSlotMap")
                                .getMethod("getMapping")
                                .invoke(null);
        if (!(mapping.get(slot) instanceof int[] indexes)) return true;
        var health =
                com.lastimp.dgh.common.capability.HealthCapability.getAndApply(
                        patient, h -> h, null);
        int available = CyberPatientData.capacity(health, group.name(), indexes.length);
        for (int i = 0; i < indexes.length; i++) if (indexes[i] == index) return i < available;
        return false;
    }

    public static boolean mayInstall(int index, ItemStack stack) {
        var patient = PATIENT.get();
        if (patient == null) return true;
        try {
            return mayEdit(index, stack) && withinCapacity(patient, slotAt(index), index);
        } catch (ReflectiveOperationException failure) {
            return true;
        }
    }

    public static String organFor(ItemStack stack) {
        String direct = AnatomicalItems.kind(stack);
        if (!direct.isEmpty()) return direct;
        if (stack.isEmpty()) return "";
        try {
            var slotType = Class.forName(ROOT + "api.CyberwareSlot");
            Object slot = slotType.getField("ORGANS").get(null);
            Object tag =
                    stack.getItem()
                            .getClass()
                            .getMethod("getReplacedOrganItemTag", ItemStack.class, slotType)
                            .invoke(stack.getItem(), stack, slot);
            if (tag instanceof net.minecraft.tags.TagKey<?> key) {
                var mapped = CompatRules.organ("ORGANS", key.location().toString());
                return mapped == null ? "" : mapped;
            }
        } catch (ReflectiveOperationException ignored) {
            // Some accessories use the shared torso slot without replacing a specific organ.
        }
        return "";
    }

    public static boolean canCommit(
            Player patient, Object surgeon, boolean[] staged, boolean[] removal) {
        if (patient.level().isClientSide()) return true;
        try {
            var inventory = (IItemHandler) surgeon.getClass().getField("inventory").get(surgeon);
            for (int i = 0; i < inventory.getSlots(); i++) {
                if (!flag(staged, i) && !flag(removal, i)) continue;
                var stack = inventory.getStackInSlot(i);
                if (slotAt(i) instanceof Enum<?> slot
                        && (!AnatomicalItems.available(patient, slot.name(), stack)
                                || flag(staged, i) && !withinCapacity(patient, slot, i))) {
                    patient.displayClientMessage(
                            Component.translatable("dghcompat.cyber.missing"), true);
                    return false;
                }
            }
            for (int i = 0; i < inventory.getSlots(); i++)
                if (flag(staged, i)) AnatomicalItems.stamp(inventory.getStackInSlot(i));
            return true;
        } catch (ReflectiveOperationException failure) {
            return true;
        }
    }

    private static boolean flag(boolean[] flags, int i) {
        return flags != null && i < flags.length && flags[i];
    }
}
