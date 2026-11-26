package com.hanmoyun.dghcompat.corpse;

import com.hanmoyun.dghcompat.CompatMod;

import net.minecraft.world.SimpleContainer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

/** Takes ownership only of normal death drops actually accepted by the fallback inventory. */
@EventBusSubscriber(modid = CompatMod.ID)
public final class DeathBelongings {
    private DeathBelongings() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void drops(LivingDropsEvent event) {
        var entity = event.getEntity();
        if (Corpses.isCorpse(entity)) return;
        // Vanilla cleared equipment slots that already contributed an item to this event.
        if (entity instanceof net.minecraft.world.entity.Mob mob) {
            for (var slot : net.minecraft.world.entity.EquipmentSlot.values()) {
                var stack = mob.getItemBySlot(slot);
                if (stack.isEmpty()) continue;
                event.getDrops()
                        .add(
                                new net.minecraft.world.entity.item.ItemEntity(
                                        mob.level(),
                                        mob.getX(),
                                        mob.getY(),
                                        mob.getZ(),
                                        stack.copy()));
                mob.setItemSlot(slot, net.minecraft.world.item.ItemStack.EMPTY);
            }
        }
    }

    public static final String EXTRA = "dghcompat:extra_drops";

    public static void store(
            net.minecraft.world.entity.Entity target,
            SimpleContainer inventory,
            net.minecraft.world.item.ItemStack stack) {
        var rest = inventory.addItem(stack);
        if (!rest.isEmpty()) {
            var list = target.getPersistentData().getList(EXTRA, 10);
            list.add(rest.save(target.registryAccess()));
            target.getPersistentData().put(EXTRA, list);
        }
    }

    public static void refill(net.minecraft.world.entity.Entity target, SimpleContainer inventory) {
        if (target.level().isClientSide()
                || !target.getPersistentData().contains(EXTRA)
                || inventory.getItems().stream()
                        .noneMatch(net.minecraft.world.item.ItemStack::isEmpty)) return;
        var list = target.getPersistentData().getList(EXTRA, 10);
        while (!list.isEmpty()) {
            var stack =
                    net.minecraft.world.item.ItemStack.parseOptional(
                            target.registryAccess(), list.getCompound(0));
            var rest = inventory.addItem(stack);
            if (!rest.isEmpty()) {
                list.set(0, rest.save(target.registryAccess()));
                break;
            }
            list.remove(0);
        }
        if (list.isEmpty()) target.getPersistentData().remove(EXTRA);
    }
}
