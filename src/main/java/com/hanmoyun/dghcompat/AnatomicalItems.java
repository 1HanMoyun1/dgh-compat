package com.hanmoyun.dghcompat;

import com.lastimp.dgh.common.capability.HealthCapability;
import com.lastimp.dgh.common.capability.bodyPart.base.BodyCondition;
import com.lastimp.dgh.common.enums.BodyComponents;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class AnatomicalItems {
    private static final String KEY = "dghcompat:anatomy";

    private AnatomicalItems() {}

    public static String kind(ItemStack stack) {
        var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String path = id.getPath();
        if (id.getNamespace().equals("dgh")) {
            return switch (path) {
                case "human_hand", "human_leg" ->
                        BodyItems.data(stack).getString("part").toLowerCase(java.util.Locale.ROOT);
                case "brain",
                                "spinal_cord",
                                "eye",
                                "heart",
                                "kidney",
                                "liver",
                                "lungs",
                                "stomach",
                                "muscle",
                                "neuro",
                                "skin" ->
                        path;
                default -> "";
            };
        }
        if (!id.getNamespace().equals("createcybernetics")) return "";
        return switch (path) {
            case "bodypart_leftarm" -> "left_arm";
            case "bodypart_rightarm" -> "right_arm";
            case "bodypart_leftleg" -> "left_leg";
            case "bodypart_rightleg" -> "right_leg";
            case "bodypart_eyeballs" -> "eye";
            case "bodypart_intestines" -> "stomach";
            case "bodypart_brain",
                            "bodypart_heart",
                            "bodypart_lungs",
                            "bodypart_liver",
                            "bodypart_muscle",
                            "bodypart_skin",
                            "bodypart_kidney" ->
                    path.substring(9);
            default -> "";
        };
    }

    public static void stamp(ItemStack stack) {
        String kind = kind(stack);
        if (kind.isEmpty()) return;
        var data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        if (!data.contains(KEY))
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putString(KEY, kind));

        var origin = BodyItems.data(stack);
        var existingName = stack.get(DataComponents.ITEM_NAME);
        boolean bridgeName =
                existingName != null
                        && existingName.getContents()
                                instanceof
                                net.minecraft.network.chat.contents.TranslatableContents translated
                        && translated.getKey().equals("dghcompat.body.name");
        if (origin.hasUUID("owner") && (existingName == null || bridgeName)) {
            Component name;
            try {
                var part = BodyComponents.valueOf(origin.getString("part"));
                name =
                        BodyComponents.EXTREMITIES.contains(part)
                                        && (kind.endsWith("_arm") || kind.endsWith("_leg"))
                                ? PartNames.name(part)
                                : Component.translatable(stack.getItem().getDescriptionId());
            } catch (IllegalArgumentException ignored) {
                return;
            }
            var localized =
                    Component.translatable("dghcompat.body.name", origin.getString("name"), name);
            if (!localized.equals(existingName)) stack.set(DataComponents.ITEM_NAME, localized);
        }
    }

    public static void inventoryTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)
                || player.tickCount % 20 != 0) return;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++)
            stamp(player.getInventory().getItem(i));
        stamp(player.containerMenu.getCarried());
    }

    public static boolean available(
            net.minecraft.world.entity.LivingEntity patient, String slot, ItemStack candidate) {
        return com.lastimp.dgh.common.PlatformService.CAPABILITY_HELPER
                .getHealth(patient)
                .map(h -> available(h, slot, candidate))
                .orElse(true);
    }

    static boolean available(HealthCapability health, String slot, ItemStack candidate) {
        String limb = CompatRules.limb(slot);
        if (limb != null) {
            var body = health.getComponent(BodyComponents.valueOf(limb));
            return !body.abnormal(BodyCondition.TRAUMATIC_AMPUTATION)
                    && !body.abnormal(BodyCondition.SURGICAL_AMPUTATION);
        }
        String organ =
                switch (slot) {
                    case "BRAIN" -> "brain";
                    case "EYES" -> "eye";
                    case "HEART" -> "heart";
                    case "LUNGS" -> "lungs";
                    case "ORGANS" -> CyberAnatomy.organFor(candidate);
                    default -> "";
                };
        if (slot.equals("ORGANS") && organ.isEmpty())
            return present(health, BodyComponents.TORSO, "liver")
                    || present(health, BodyComponents.TORSO, "stomach")
                    || present(health, BodyComponents.TORSO, "kidney");
        if (organ.isEmpty()) return true;
        return present(
                health,
                slot.equals("BRAIN") || slot.equals("EYES")
                        ? BodyComponents.HEAD
                        : BodyComponents.TORSO,
                organ);
    }

    private static boolean present(HealthCapability health, BodyComponents part, String organ) {
        for (var stack : health.getComponent(part).organ()) {
            if (!stack.isEmpty() && organ.equals(kind(stack))) return true;
        }
        return false;
    }
}
