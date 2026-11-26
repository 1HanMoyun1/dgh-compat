package com.hanmoyun.dghcompat;

import com.lastimp.dgh.common.capability.HealthCapability;
import com.lastimp.dgh.common.entity.StretcherEntity;
import com.lastimp.dgh.common.entry.register.ModItems;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = CompatMod.ID)
public final class StasisPatients {
    public static final String WRAPPED = "dghcompat:stasis_wrapped";

    private StasisPatients() {}

    public static boolean wrapped(Entity entity) {
        if (!(entity instanceof LivingEntity living)) return false;
        if (entity.level().isClientSide()) return entity.getPersistentData().getBoolean(WRAPPED);
        return HealthCapability.getAndApply(living, HealthCapability::isFrozen, false);
    }

    public static void setVisual(Entity entity, boolean value) {
        boolean changed = entity.getPersistentData().getBoolean(WRAPPED) != value;
        entity.getPersistentData().putBoolean(WRAPPED, value);
        if (changed) entity.refreshDimensions();
    }

    public static void sync(LivingEntity patient) {
        boolean value = wrapped(patient);
        setVisual(patient, value);
        if (!patient.level().isClientSide()) {
            var level = (net.minecraft.server.level.ServerLevel) patient.level();
            var state =
                    new StasisPackets.State(
                            patient.getId(),
                            patient.getUUID(),
                            value,
                            down(patient),
                            ArmState.flags(patient));
            for (var viewer : level.getChunkSource().chunkMap.getPlayersWatching(patient))
                sendState(viewer, state);
            if (patient instanceof ServerPlayer player) sendState(player, state);
        }
    }

    private static boolean down(LivingEntity patient) {
        return com.hanmoyun.dghcompat.corpse.Corpses.isCorpse(patient)
                || !patient.isAlive()
                || HealthCapability.isDown(patient);
    }

    private static void sendState(ServerPlayer viewer, StasisPackets.State state) {
        if (viewer.connection.hasChannel(StasisPackets.State.TYPE))
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(viewer, state);
    }

    @SubscribeEvent
    public static void dimensions(EntityEvent.Size event) {
        if (event.getEntity().getPersistentData().getBoolean(WRAPPED))
            event.setNewSize(EntityDimensions.fixed(1.0f, 0.5f));
    }

    @SubscribeEvent
    public static void track(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer viewer
                && com.hanmoyun.dghcompat.corpse.NativePatients.resolve(event.getTarget())
                        instanceof LivingEntity patient
                && viewer.connection.hasChannel(StasisPackets.State.TYPE))
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
                    viewer,
                    new StasisPackets.State(
                            patient.getId(),
                            patient.getUUID(),
                            wrapped(patient),
                            down(patient),
                            ArmState.flags(patient)));
    }

    // A player does not track itself. Seed its local visual state after a new client
    // world/identity.
    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sync(player);
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sync(player);
    }

    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST)
    public static void clonePlayer(PlayerEvent.Clone event) {
        if (!event.isWasDeath() || !(event.getEntity() instanceof ServerPlayer player)) return;
        var old = event.getOriginal().getPersistentData();
        if (old.getBoolean("cc_forced_main_arm")) {
            player.setMainArm(old.getBoolean("cc_original_main_arm")
                    ? net.minecraft.world.entity.HumanoidArm.LEFT : net.minecraft.world.entity.HumanoidArm.RIGHT);
            player.getPersistentData().remove("cc_forced_main_arm");
            player.getPersistentData().remove("cc_original_main_arm");
        }
    }

    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST)
    public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sync(player);
    }

    @SubscribeEvent
    public static void leave(net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide())
            com.hanmoyun.dghcompat.integration.RagdollBodies.forget(event.getEntity());
    }

    @SubscribeEvent
    public static void tick(EntityTickEvent.Post event) {
        // Rotation alone need not move an entity; refresh the oriented enclosing box too.
        var entity = event.getEntity();
        if (entity.getPersistentData().getBoolean(WRAPPED)) StasisGeometry.refreshBounds(entity);
        var resolved = com.hanmoyun.dghcompat.corpse.NativePatients.resolve(event.getEntity());
        if (!(resolved instanceof LivingEntity patient) || patient.level().isClientSide()) return;
        if (com.hanmoyun.dghcompat.corpse.NativePatients.actual(patient) != patient)
            HealthCapability.getAndApply(
                    patient,
                    h -> {
                        h.update(patient);
                        h.SYNIfDirty(patient);
                    });
        boolean down = down(patient);
        if (!down
                && patient.getPersistentData().getBoolean("dghcompat:down_visual")
                && !patient.isPassenger()
                && !patient.isSleeping()
                && !wrapped(patient)) {
            StasisGeometry.placeOnGround(patient);
            patient.refreshDimensions();
        }
        int arms = ArmState.flags(patient);
        boolean armsChanged =
                patient instanceof Player
                        && arms != patient.getPersistentData().getInt(ArmState.TRACKED);
        if (armsChanged) patient.getPersistentData().putInt(ArmState.TRACKED, arms);
        if (armsChanged
                || wrapped(patient) != patient.getPersistentData().getBoolean(WRAPPED)
                || down != patient.getPersistentData().getBoolean("dghcompat:down_visual")) {
            patient.getPersistentData().putBoolean("dghcompat:down_visual", down);
            sync(patient);
        }
        if (patient instanceof ServerPlayer player && CarrySupport.loaded())
            CarryOnIntegration.tick(player);
        // Seed the medical visual state before native packets capture a fall's source pose.
        com.hanmoyun.dghcompat.integration.RagdollBodies.serverTick(patient);
    }

    public static boolean freeInjured(LivingEntity patient, Player doctor) {
        return patient != doctor
                && patient.isAlive()
                && !patient.isSleeping()
                && !com.hanmoyun.dghcompat.corpse.Corpses.isCorpse(patient)
                && !patient.isPassenger()
                && !wrapped(patient)
                && (patient.level().isClientSide()
                        ? patient.getPersistentData().getBoolean("dghcompat:down_visual")
                                || HealthCapability.isDown(patient)
                        : HealthCapability.isDown(patient));
    }

    public static void notify(Player player, String key) {
        // Stasis interactions intentionally have no action-bar text.
    }

    /** Runs before the transport handler; only successful server checks mutate inventory/state. */
    public static boolean interact(PlayerInteractEvent event, Entity target) {
        Player player = event.getEntity();
        LivingEntity patient =
                com.hanmoyun.dghcompat.corpse.NativePatients.resolve(target)
                                instanceof LivingEntity living
                        ? living
                        : target instanceof StretcherEntity carrier ? carrier.getPatient() : null;
        if (patient == null) return false;
        if (CaseBook.snapshot(event, patient)) return true;
        boolean wrapRequest =
                !wrapped(patient)
                        && player.isShiftKeyDown()
                        && player.getMainHandItem().is(ModItems.STASIS_BAG.get());
        boolean unwrapRequest =
                wrapped(patient) && !player.isShiftKeyDown() && player.getMainHandItem().isEmpty();
        if (!wrapRequest && !unwrapRequest && !TreatmentLock.active(patient)) return false;
        if (event.getHand() != InteractionHand.MAIN_HAND || patient.level().isClientSide())
            return true;
        if (!player.canInteractWithEntity(patient, 0) || player.isSpectator() || !patient.isAlive())
            return true;
        if (TreatmentLock.active(patient)) {
            notify(player, "locked");
            return true;
        }
        if (wrapRequest) {
            if (!(HealthCapability.isDown(patient)
                            || com.hanmoyun.dghcompat.corpse.Corpses.isCorpse(patient))
                    || wrapped(patient)
                    || CarrySupport.carrying(player)) {
                notify(player, "cannot_wrap");
                return true;
            }
            boolean success =
                    HealthCapability.getAndApply(
                            patient,
                            health -> {
                                if (!health.getAutoPulse().getStackInSlot(0).isEmpty())
                                    return false;
                                health.getAutoPulse()
                                        .setStackInSlot(
                                                0, player.getMainHandItem().copyWithCount(1));
                                if (com.hanmoyun.dghcompat.corpse.Corpses.isCorpse(patient))
                                    com.hanmoyun.dghcompat.corpse.SealedBodyBags.bind(health);
                                player.getMainHandItem().shrink(1);
                                return true;
                            },
                            false);
            if (!success) notify(player, "equipment_busy");
            else {
                if (!patient.isPassenger()) StasisGeometry.placeOnGround(patient);
                com.hanmoyun.dghcompat.integration.RagdollBodies.stop(patient);
                sync(patient);
            }
            return true;
        }
        if (unwrapRequest) {
            if (com.hanmoyun.dghcompat.corpse.Corpses.isCorpse(patient)) {
                // A sealed corpse is never unwrapped. A later click on the bedded bag opens
                // surgery.
                if (patient.getVehicle() instanceof StretcherEntity carrier
                        && PatientTransport.bed(carrier) != null
                        && player instanceof ServerPlayer doctor)
                    PatientTransport.openTreatment(doctor, PatientTransport.bed(carrier));
                return true;
            }
            // Manual removal is deliberately distinct from Shift-right-click pickup.
            if (!(patient.getVehicle() instanceof StretcherEntity carrier)
                    || PatientTransport.bed(carrier) == null) {
                notify(player, "unwrap_on_bed");
                return true;
            }
            ItemStack bag =
                    HealthCapability.getAndApply(
                            patient,
                            health -> health.getAutoPulse().extractItem(0, 1, false),
                            ItemStack.EMPTY);
            if (!bag.isEmpty() && !player.getInventory().add(bag)) player.drop(bag, false);
            sync(patient);
            return true;
        }
        return true;
    }
}
