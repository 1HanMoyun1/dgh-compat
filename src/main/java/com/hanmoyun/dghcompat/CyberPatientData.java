package com.hanmoyun.dghcompat;

import com.lastimp.dgh.common.capability.HealthCapability;
import com.lastimp.dgh.common.enums.BodyComponents;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.lang.reflect.Array;
import java.util.Map;
import java.util.UUID;

public final class CyberPatientData {
    private static final String ROOT = "com.perigrine3.createcybernetics.";
    private static final String PROFILE = "dghcompat:engrams";
    private static final long BIOCHIP_TICKS = 28L * 24000L;

    private record Engram(int integrity, String name, UUID uuid) {}

    private CyberPatientData() {}

    static Object cyberware(net.minecraft.world.entity.LivingEntity player)
            throws ReflectiveOperationException {
        return Class.forName(ROOT + "common.capabilities.CyberwareAccess")
                .getMethod("getData", net.minecraft.world.entity.LivingEntity.class)
                .invoke(null, player);
    }

    /** Render-only copy. The independent patient never borrows the respawned player's attachments. */
    public static void captureCorpseSkin(Player player, CompoundTag visual) {
        if (!net.neoforged.fml.ModList.get().isLoaded("createcybernetics")) return;
        try {
            Object data = cyberware(player);
            if (data == null) return;
            var copy = (CompoundTag) data.getClass().getMethod("serializeNBT",
                    net.minecraft.core.HolderLookup.Provider.class).invoke(data, player.registryAccess());
            visual.put("cc_holo_snapshot_cyberware", copy);
            visual.put("cc_cybereye_cfg", player.getPersistentData().getCompound("cc_cybereye_cfg").copy());
            var skinItems = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("createcybernetics", "skin_items"));
            for (var group : groups(data).entrySet()) {
                if (!((Enum<?>) group.getKey()).name().equals("SKIN")) continue;
                Object slots = group.getValue();
                boolean skin = false;
                for (int i = 0; i < Array.getLength(slots); i++) skin |= item(Array.get(slots, i)).is(skinItems);
                visual.putBoolean("missing_skin", !skin);
                break;
            }
        } catch (ReflectiveOperationException error) {
            com.mojang.logging.LogUtils.getLogger().warn("Could not copy Cybernetics corpse appearance", error);
        }
    }

    public static void applyCorpseSkin(Player visualPlayer, CompoundTag visual) {
        if (!net.neoforged.fml.ModList.get().isLoaded("createcybernetics")) return;
        if (visual.contains("cc_holo_snapshot_cyberware")) {
            visualPlayer.getPersistentData().putBoolean("cc_holo_snapshot", true);
            for (String key : java.util.List.of("cc_holo_snapshot_cyberware", "cc_cybereye_cfg"))
                visualPlayer.getPersistentData().put(key, visual.getCompound(key).copy());
        } else {
            // Older corpses have no recoverable implant history. Use natural organs on the visual model.
            try {
                Object data = cyberware(visualPlayer);
                if (data != null) data.getClass().getMethod("resetToDefaultOrgans").invoke(data);
            } catch (ReflectiveOperationException error) {
                com.mojang.logging.LogUtils.getLogger().warn("Could not initialise legacy corpse appearance", error);
            }
        }
    }

    static Map<?, ?> groups(Object data) throws ReflectiveOperationException {
        return (Map<?, ?>) data.getClass().getMethod("getAll").invoke(data);
    }

    static ItemStack item(Object entry) throws ReflectiveOperationException {
        return entry == null
                ? ItemStack.EMPTY
                : (ItemStack) entry.getClass().getMethod("getItem").invoke(entry);
    }

    private static Engram engram(ItemStack stack) {
        if (stack.isEmpty()
                || !stack.getItem()
                        .getClass()
                        .getName()
                        .equals(ROOT + "item.cyberware.brain.CerebralProcessingUnitItem"))
            return null;
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains("cc_biochip_done") && !tag.contains("cc_biochip_progress")) return null;
        String name = tag.getString("cc_biochip_owner_name");
        UUID owner = null;
        try {
            owner = UUID.fromString(tag.getString("cc_biochip_owner_uuid"));
        } catch (IllegalArgumentException ignored) {
            // An incomplete engram has no verified persona.
        }
        boolean complete = tag.getBoolean("cc_biochip_done") && owner != null && !name.isBlank();
        int integrity =
                complete
                        ? 100
                        : (int)
                                Math.clamp(
                                        tag.getLong("cc_biochip_progress")
                                                * (100.0 / BIOCHIP_TICKS),
                                        0,
                                        99);
        return new Engram(integrity, name, complete ? owner : null);
    }

    public static boolean hasCompleteEngram(ItemStack stack) {
        var engram = engram(stack);
        return engram != null && engram.uuid() != null;
    }

    public static int capacity(HealthCapability health, String group, int maximum) {
        return capacity(health, group, maximum, null, -1);
    }

    private static int capacity(
            HealthCapability health,
            String group,
            int maximum,
            com.lastimp.dgh.common.container.DynamicValidItemHandler omitted,
            int slot) {
        if (health == null) return maximum;
        String limb = CompatRules.limb(group);
        BodyComponents part =
                limb != null
                        ? BodyComponents.valueOf(limb)
                        : switch (group) {
                            case "BRAIN", "EYES" -> BodyComponents.HEAD;
                            case "HEART", "LUNGS", "ORGANS" -> BodyComponents.TORSO;
                            default -> null;
                        };
        if (part == null) return maximum;
        int neural = OrganCapacity.neuralBonus(health, health.getComponent(part), omitted, slot);
        // The upstream menu and persistence have fixed bounds. Open its last two positions with
        // neural support, and never index outside the native array.
        return Math.min(maximum, Math.max(0, maximum - 2) + neural);
    }

    static boolean canLoseNeural(
            HealthCapability health,
            com.lastimp.dgh.common.container.DynamicValidItemHandler handler,
            int slot) {
        var player = ((HealthOwner) health).dghcompat$owner();
        if (!CyberApi.available()
                || player == null
                || player.level().isClientSide()
                || !(player instanceof Player) && !hasMobData(player)) return true;
        try {
            var data = cyberware(player);
            if (data == null) return true;
            for (var entry : groups(data).entrySet()) {
                if (!(entry.getKey() instanceof Enum<?> group) || entry.getValue() == null)
                    continue;
                int length = Array.getLength(entry.getValue());
                int remaining = capacity(health, group.name(), length, handler, slot);
                if (remaining >= capacity(health, group.name(), length)) continue;
                for (int i = remaining; i < length; i++)
                    if (!item(Array.get(entry.getValue(), i)).isEmpty()) return false;
            }
            return true;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return false;
        }
    }

    public static CompoundTag read(net.minecraft.world.entity.LivingEntity patient) {
        var display = new CompoundTag();
        if (!CyberApi.available()) return display;
        try {
            var data = cyberware(patient);
            if (data == null) return display;
            int humanity;
            try {
                humanity =
                        (int)
                                data.getClass()
                                        .getMethod("getHumanity", Player.class)
                                        .invoke(data, patient);
            } catch (NoSuchMethodException ignored) {
                humanity = (int) data.getClass().getMethod("getHumanity").invoke(data);
            }
            display.putBoolean("managed", true);
            if (patient instanceof Player) display.putInt("humanity", humanity);
            for (var entry : groups(data).entrySet()) {
                if (!(entry.getKey() instanceof Enum<?> group)
                        || !group.name().equals("BRAIN")
                        || entry.getValue() == null) continue;
                Object slots = entry.getValue();
                for (int i = 0; i < Array.getLength(slots); i++) {
                    var personality = engram(item(Array.get(slots, i)));
                    if (personality == null) continue;
                    display.putInt("integrity", personality.integrity());
                    if (personality.uuid() != null) {
                        display.putUUID("persona_id", personality.uuid());
                        display.putString("persona", personality.name());
                    }
                    return display;
                }
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return new CompoundTag();
        }
        return display;
    }

    public static boolean hasMobData(net.minecraft.world.entity.LivingEntity mob) {
        if (!CyberApi.available()) return false;
        try {
            return mob.hasData(mobAttachment());
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return false;
        }
    }

    private static net.neoforged.neoforge.attachment.AttachmentType<?> mobAttachment()
            throws ReflectiveOperationException {
        return (net.neoforged.neoforge.attachment.AttachmentType<?>)
                Class.forName(ROOT + "common.capabilities.ModMobAttachments")
                        .getField("CYBERENTITY_CYBERWARE")
                        .get(null);
    }

    public static void refresh(ServerPlayer player) {
        var state = read(player);
        var tag = new CompoundTag();
        if (state.hasUUID("persona_id") && !player.getUUID().equals(state.getUUID("persona_id"))) {
            tag.putUUID("id", state.getUUID("persona_id"));
            tag.putString("name", state.getString("persona"));
        }
        if (tag.equals(player.getPersistentData().getCompound(PROFILE))) return;
        if (tag.isEmpty()) player.getPersistentData().remove(PROFILE);
        else player.getPersistentData().put(PROFILE, tag);
        player.refreshDisplayName();
        player.refreshTabListName();
    }

    private static Component alias(Player player) {
        if (!CyberApi.available()) return null;
        String name = player.getPersistentData().getCompound(PROFILE).getString("name");
        return name.isBlank() ? null : Component.literal(name);
    }

    public static void name(
            net.neoforged.neoforge.event.entity.player.PlayerEvent.NameFormat event) {
        var alias = alias(event.getEntity());
        if (alias != null) event.setDisplayname(alias);
    }

    public static void tab(
            net.neoforged.neoforge.event.entity.player.PlayerEvent.TabListNameFormat event) {
        var alias = alias(event.getEntity());
        if (alias != null) event.setDisplayName(alias);
    }

    public static void login(
            net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) refresh(player);
    }
}
