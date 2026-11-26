package com.hanmoyun.dghcompat;


import com.lastimp.dgh.common.capability.HealthCapability;
import com.lastimp.dgh.common.entry.register.ModItems;
import com.lastimp.dgh.common.menu.HealthMenu;
import com.lastimp.dgh.common.utils.Utils;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.function.Consumer;

final class BedToolPackets {
    static final int OXYGEN = 0, AUTOPULSE = 1, BOOK = 2, HEALTH = 3, BLOOD = 4;
    private static final int NATIVE_OXYGEN = 45, NATIVE_AUTOPULSE = 46;
    static Consumer<State> display = state -> {};

    private static final java.util.Map<HealthMenu, java.util.Set<Integer>> SCANNERS =
            new java.util.WeakHashMap<>();

    static net.minecraft.world.item.Item scanner(int tool) {
        return tool == HEALTH ? ModItems.HEALTH_SCANNER.get() : ModItems.BLOOD_SCANNER.get();
    }

    static boolean enabled(ServerPlayer doctor, int tool) {
        if (!(doctor.containerMenu instanceof HealthMenu menu)) return false;
        var enabled = SCANNERS.get(menu);
        if (enabled == null || !enabled.contains(tool)) return false;
        if (doctor.getInventory().contains(stack -> stack.is(scanner(tool)))) return true;
        enabled.remove(tool);
        return false;
    }

    private BedToolPackets() {}

    record Action(int menu, int tool, boolean remove) implements CustomPacketPayload {
        static final Type<Action> TYPE = new Type<>(id("bed_tool"));
        static final StreamCodec<RegistryFriendlyByteBuf, Action> CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.VAR_INT,
                        Action::menu,
                        ByteBufCodecs.VAR_INT,
                        Action::tool,
                        ByteBufCodecs.BOOL,
                        Action::remove,
                        Action::new);

        @Override
        public Type<Action> type() {
            return TYPE;
        }
    }

    record State(int menu, CompoundTag data) implements CustomPacketPayload {
        static final Type<State> TYPE = new Type<>(id("bed_tool_state"));
        static final StreamCodec<RegistryFriendlyByteBuf, State> CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.VAR_INT,
                        State::menu,
                        ByteBufCodecs.COMPOUND_TAG,
                        State::data,
                        State::new);

        @Override
        public Type<State> type() {
            return TYPE;
        }
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(CompatMod.ID, path);
    }

    static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1")
                .optional()
                .playToServer(
                        Action.TYPE,
                        Action.CODEC,
                        (action, context) ->
                                context.enqueueWork(
                                        () -> {
                                            if (context.player() instanceof ServerPlayer doctor)
                                                handle(doctor, action);
                                        }))
                .playToClient(
                        State.TYPE,
                        State.CODEC,
                        (state, context) -> context.enqueueWork(() -> display.accept(state)));
    }

    private static void handle(ServerPlayer doctor, Action action) {
        if (!(doctor.containerMenu instanceof HealthMenu menu)
                || menu.containerId != action.menu()
                || !doctor.isAlive()
                || doctor.isSpectator()) return;
        LivingEntity patient = Utils.getLivingWithHealth(doctor.serverLevel(), menu.targetEntity);
        if (patient == null || !patient.isAlive() || doctor.distanceToSqr(patient) > 36) return;
        var bed = CaseBook.bed(patient);
        if (bed == null
                || !(doctor.serverLevel().getBlockState(bed).getBlock()
                        instanceof com.lastimp.dgh.common.block.OperatingBedBlock)
                || !doctor.level().mayInteract(doctor, bed)) return;
        boolean cursor = !menu.getCarried().isEmpty();
        ItemStack held = cursor ? menu.getCarried() : doctor.getMainHandItem();
        if (action.tool() == HEALTH || action.tool() == BLOOD) {
            if (!action.remove() && !cursor) {
                int tool = action.tool();
                var enabled = SCANNERS.computeIfAbsent(menu, ignored -> new java.util.HashSet<>());
                if (!enabled.remove(tool)) {
                    if (doctor.getInventory().contains(stack -> stack.is(scanner(tool)))) {
                        enabled.add(tool);
                        if (tool == HEALTH) CaseBook.scan(doctor, patient);
                    } else hint(doctor, "missing_scanner");
                }
            }
        } else if (action.tool() == BOOK && HealthCapability.canApplyHealing(doctor)) {
            if (action.remove()) {
                if (!cursor) CaseBook.take(doctor, bed);
            } else if (held.is(Items.WRITABLE_BOOK)) CaseBook.place(doctor, bed, patient);
            else hint(doctor, "book_input");
        } else if (action.tool() >= OXYGEN && action.tool() <= AUTOPULSE) {
            if (!HealthCapability.canApplyHealing(doctor)
                    || !(TreatmentArms.available(doctor, true)
                            || TreatmentArms.available(doctor, false))
                    || StasisPatients.wrapped(patient)
                    || com.hanmoyun.dghcompat.corpse.Corpses.isCorpse(patient)) {
                hint(doctor, "patient");
            } else {
                equipment(doctor, menu, patient, held, cursor, action);
            }
            menu.broadcastChanges();
        }
        sendState(doctor, menu, patient, bed);
    }

    private static void equipment(
            ServerPlayer doctor,
            HealthMenu menu,
            LivingEntity patient,
            ItemStack held,
            boolean cursor,
            Action action) {
        boolean oxygen = action.tool() == OXYGEN;
        var slot = menu.getSlot(oxygen ? NATIVE_OXYGEN : NATIVE_AUTOPULSE);
        if (action.remove()) {
            if (cursor || !slot.mayPickup(doctor)) return;
            var removed = slot.remove(1);
            if (removed.isEmpty()) return;
            var name = CaseBookChinese.item(removed);
            if (!doctor.getInventory().add(removed)) doctor.drop(removed, false);
            CaseBook.record(doctor, patient, null, "取下 " + name);
            slot.setChanged();
        } else {
            if (!held.is(oxygen ? ModItems.OXYGEN_MASK.get() : ModItems.AUTOPULSE.get())) {
                hint(doctor, oxygen ? "oxygen_input" : "pulse_input");
                return;
            }
            if (slot.hasItem()) {
                hint(doctor, "occupied");
                return;
            }
            if (!slot.mayPlace(held)) return;
            slot.setByPlayer(held.copyWithCount(1));
            CaseBook.record(doctor, patient, null, "装上 " + CaseBookChinese.item(held));
            held.shrink(1);
            if (cursor) menu.setCarried(held.isEmpty() ? ItemStack.EMPTY : held);
        }
    }

    private static void hint(ServerPlayer doctor, String key) {
        doctor.displayClientMessage(Component.translatable("dghcompat.tools." + key), true);
    }

    private static void sendState(
            ServerPlayer doctor,
            HealthMenu menu,
            LivingEntity patient,
            net.minecraft.core.BlockPos bed) {
        if (!doctor.connection.hasChannel(State.TYPE)) return;
        var tag = new CompoundTag();
        tag.putBoolean("health", enabled(doctor, HEALTH));
        tag.putBoolean("blood", enabled(doctor, BLOOD));
        tag.putBoolean("wrapped", StasisPatients.wrapped(patient));
        save(tag, "oxygen", menu.getSlot(NATIVE_OXYGEN).getItem(), patient);
        save(tag, "pulse", menu.getSlot(NATIVE_AUTOPULSE).getItem(), patient);
        var book = CaseBook.placedBook(patient.level(), bed);
        if (!book.isEmpty()) tag.putBoolean("book", true);
        PacketDistributor.sendToPlayer(doctor, new State(menu.containerId, tag));
    }

    private static void save(CompoundTag tag, String key, ItemStack stack, LivingEntity patient) {
        if (!stack.isEmpty()) tag.put(key, stack.save(patient.registryAccess()));
    }
}
