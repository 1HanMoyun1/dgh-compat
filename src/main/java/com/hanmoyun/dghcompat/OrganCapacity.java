package com.hanmoyun.dghcompat;

import com.lastimp.dgh.common.capability.HealthCapability;
import com.lastimp.dgh.common.capability.bodyPart.base.AbstractBody;
import com.lastimp.dgh.common.container.DynamicValidItemHandler;
import com.lastimp.dgh.common.enums.BodyComponents;
import com.lastimp.dgh.common.tags.ModTags;

import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public final class OrganCapacity {

    private static final Map<DynamicValidItemHandler, java.lang.ref.WeakReference<HealthCapability>>
            OWNERS = Collections.synchronizedMap(new WeakHashMap<>());

    private OrganCapacity() {}

    public static void bind(HealthCapability health, AbstractBody body) {
        var existing = OWNERS.get(body.organ());
        if (existing == null || existing.get() != health)
            OWNERS.put(body.organ(), new java.lang.ref.WeakReference<>(health));
    }

    private static HealthCapability owner(DynamicValidItemHandler handler) {
        var ref = OWNERS.get(handler);
        return ref == null ? null : ref.get();
    }

    public static int capacity(HealthCapability health, AbstractBody body, int group) {
        return switch (group) {
            case 0 -> first(health, body, null, -1);
            case 1 -> bounded(body.organ2Level());
            case 2 -> bounded(body.organ3Level());
            default -> 0;
        };
    }

    private static int first(
            HealthCapability health,
            AbstractBody body,
            DynamicValidItemHandler skipped,
            int index) {
        return bounded(
                ((com.hanmoyun.dghcompat.mixin.OrganBaseAccess) body).dghcompat$baseSlots()
                        + neuralBonus(health, body, skipped, index));
    }

    static int neuralBonus(
            HealthCapability health,
            AbstractBody body,
            DynamicValidItemHandler skipped,
            int index) {
        var head = health.getComponent(BodyComponents.HEAD);
        int spine = count(head.organ(), stack -> stack.is(ModTags.SPINAL_CORD), skipped, index);
        int bonus = 0;
        if (body.getBodyType() == BodyComponents.HEAD || body.getBodyType() == BodyComponents.TORSO)
            bonus = 2 * spine;
        if (spine > 0)
            bonus +=
                    2
                            * count(
                                    body.organ(),
                                    stack ->
                                            stack.getItem()
                                                    instanceof
                                                    com.lastimp.dgh.common.item.organs.Neuro,
                                    skipped,
                                    index);
        return bonus;
    }

    private static int count(
            DynamicValidItemHandler handler,
            java.util.function.Predicate<ItemStack> filter,
            DynamicValidItemHandler skipped,
            int index) {
        int total = 0;
        for (int slot = 0; slot < handler.getSlots(); slot++)
            if (!(handler == skipped && slot == index) && filter.test(handler.getStackInSlot(slot)))
                total++;
        return total;
    }

    private static int bounded(int capacity) {
        return Math.clamp(capacity, 0, 12);
    }

    /** Returns false only when losing a capacity provider would strand another organ. */
    public static boolean neural(ItemStack stack) {
        return stack.is(ModTags.SPINAL_CORD)
                || stack.getItem() instanceof com.lastimp.dgh.common.item.organs.Neuro;
    }

    public static boolean canRemove(DynamicValidItemHandler handler, int index) {
        var stack = handler.getStackInSlot(index);
        if (!stack.is(ModTags.SPINAL_CORD)
                && !(stack.getItem() instanceof com.lastimp.dgh.common.item.organs.Neuro))
            return true;
        var health = owner(handler);
        if (health == null) return true;
        for (var part : BodyComponents.VISIBLE_BODIES) {
            var body = health.getComponent(part);
            int reduced = first(health, body, handler, index);
            int current = capacity(health, body, 0);
            if (reduced >= current) continue;
            int occupied = 0;
            for (int slot = 0; slot < 12; slot++)
                if (!body.organ().getStackInSlot(slot).isEmpty()) occupied++;
            if (occupied > reduced) return false;
            for (int slot = reduced; slot < 12; slot++) {
                if (body.organ() == handler && slot == index) continue;
                if (!body.organ().getStackInSlot(slot).isEmpty()) return false;
            }
        }
        return CyberPatientData.canLoseNeural(health, handler, index);
    }

    public static Integer firstFor(AbstractBody body) {
        var health = owner(body.organ());
        return health == null ? null : first(health, body, null, -1);
    }
}
