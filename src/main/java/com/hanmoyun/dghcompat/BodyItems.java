package com.hanmoyun.dghcompat;

import com.hanmoyun.dghcompat.corpse.CorpseEntity;
import com.lastimp.dgh.common.capability.HealthCapability;
import com.lastimp.dgh.common.enums.BodyComponents;
import com.lastimp.dgh.common.utils.Utils;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

public final class BodyItems {
    private static final String KEY = "dghcompat:body_part";
    private static final String IMPLANTS = "implants";

    private BodyItems() {}

    public static void identifyOrgans(HealthCapability health, LivingEntity patient) {
        if (patient.level().isClientSide()) return;
        for (BodyComponents part : BodyComponents.VISIBLE_BODIES) {
            var organ = health.getComponent(part).organ();
            for (int i = 0; i < organ.getSlots(); i++) {
                var stack = organ.getStackInSlot(i);
                if (stack.isEmpty()) continue;
                if (data(stack).hasUUID("owner")) {
                    AnatomicalItems.stamp(stack);
                    continue;
                }
                identify(stack, patient, part);
                organ.setStackInSlot(i, stack);
            }
        }
    }

    private static void identify(ItemStack stack, LivingEntity patient, BodyComponents part) {
        var persistent = patient.getPersistentData();
        var owner =
                persistent.hasUUID("dghcompat:deceased_uuid")
                        ? persistent.getUUID("dghcompat:deceased_uuid")
                        : patient.getUUID();
        if (patient instanceof CorpseEntity corpse && corpse.visual().hasUUID("owner"))
            owner = corpse.visual().getUUID("owner");
        var data = new CompoundTag();
        data.putUUID("owner", owner);
        data.putString("name", patient.getName().getString());
        data.putString("part", part.name());
        save(stack, data);
        AnatomicalItems.stamp(stack);
    }

    private static final String LIMB_IMPLANTS = "dghcompat:limb_implants";

    public static CompoundTag limbImplants(LivingEntity patient) {
        if (patient instanceof CorpseEntity)
            return patient.getPersistentData().getCompound(LIMB_IMPLANTS);
        var record = new CompoundTag();
        if (!CyberApi.available()) return record;
        for (var part : BodyComponents.EXTREMITIES) record.put(part.name(), new ListTag());
        for (var implant : CyberApi.snapshot(patient).implants()) {
            String part = CompatRules.limb(implant.slot());
            if (part != null && !implant.item().startsWith("createcybernetics:bodypart_"))
                record.getList(part, Tag.TAG_STRING)
                        .add(net.minecraft.nbt.StringTag.valueOf(implant.item()));
        }
        return record;
    }

    public static void rememberLimbs(CorpseEntity corpse, CompoundTag record) {
        if (!record.isEmpty()) corpse.getPersistentData().put(LIMB_IMPLANTS, record);
    }

    public static void dropLimb(
            Item item,
            LivingEntity patient,
            com.lastimp.dgh.common.capability.bodyPart.base.AbstractVisibleBody body) {
        var part = body.getBodyType();
        if (!BodyComponents.EXTREMITIES.contains(part) || patient.level().isClientSide()) {
            Utils.drop(item, patient, 1);
            return;
        }
        var stack = new ItemStack(item);
        identify(stack, patient, part);
        var record = limbImplants(patient);
        if (record.contains(part.name())) {
            var origin = data(stack);
            origin.put("installed_at_removal", record.getList(part.name(), Tag.TAG_STRING).copy());
            save(stack, origin);
        }
        Utils.drop(stack, patient);
    }

    static CompoundTag data(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag()
                .getCompound(KEY);
    }

    private static void save(ItemStack stack, CompoundTag data) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.put(KEY, data));
    }

    static ItemStack top(ItemStack stack, HolderLookup.Provider registries) {
        if (!stack.is(com.lastimp.dgh.common.entry.register.ModItems.HUMAN_HAND.get())
                && !stack.is(com.lastimp.dgh.common.entry.register.ModItems.HUMAN_LEG.get()))
            return ItemStack.EMPTY;
        ListTag list = data(stack).getList(IMPLANTS, Tag.TAG_COMPOUND);
        if (list.isEmpty()) return ItemStack.EMPTY;
        return ItemStack.parseOptional(registries, list.getCompound(list.size() - 1));
    }

    static ItemStack afterRemoval(ItemStack stack) {
        var data = data(stack);
        var list = data.getList(IMPLANTS, Tag.TAG_COMPOUND);
        if (list.isEmpty()) return ItemStack.EMPTY;
        list.remove(list.size() - 1);
        if (list.isEmpty()) data.remove(IMPLANTS);
        var result = stack.copyWithCount(1);
        save(result, data);
        return result;
    }

    public static void tooltip(ItemTooltipEvent event) {
        var data = data(event.getItemStack());
        if (!data.contains("owner")) return;
        try {
            var part = BodyComponents.valueOf(data.getString("part"));
            event.getToolTip()
                    .add(
                            Component.translatable(
                                    "dghcompat.body.origin",
                                    data.getString("name"),
                                    PartNames.name(part)));
        } catch (IllegalArgumentException ignored) {
            return;
        }
        var recorded = data.getList("installed_at_removal", Tag.TAG_STRING);
        if (data.contains("installed_at_removal") && recorded.isEmpty())
            event.getToolTip().add(Component.translatable("dghcompat.body.no_implants"));
        for (var id : recorded) {
            var key = net.minecraft.resources.ResourceLocation.tryParse(id.getAsString());
            if (key != null)
                net.minecraft.core.registries.BuiltInRegistries.ITEM
                        .getOptional(key)
                        .ifPresent(
                                item ->
                                        event.getToolTip()
                                                .add(
                                                        Component.translatable(
                                                                "dghcompat.body.implant",
                                                                item.getDescription())));
        }
        var registries = event.getContext().registries();
        if (registries == null) return;
        ListTag list = data.getList(IMPLANTS, Tag.TAG_COMPOUND);
        for (int i = list.size() - 1; i >= 0; i--) {
            var implant = ItemStack.parseOptional(registries, list.getCompound(i));
            if (!implant.isEmpty())
                event.getToolTip()
                        .add(
                                Component.translatable(
                                        "dghcompat.body.implant", implant.getHoverName()));
        }
    }
}
