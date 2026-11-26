package com.hanmoyun.dghcompat;

import com.lastimp.dgh.common.capability.HealthCapability;
import com.lastimp.dgh.common.entity.StretcherEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;

import tschipp.carryon.Constants;
import tschipp.carryon.common.carry.CarryOnData;
import tschipp.carryon.common.carry.CarryOnDataManager;
import tschipp.carryon.common.carry.PickupHandler;
import tschipp.carryon.common.carry.PlacementHandler;
import tschipp.carryon.events.EntityPickupEvent;
import tschipp.carryon.networking.clientbound.ClientboundStartRidingOtherPlayerPacket;
import tschipp.carryon.platform.Services;

/**
 * Only linked when Carry On is installed. Changes use Carry On's own storage and synchronization.
 */
public final class CarryOnIntegration {
    private static final ThreadLocal<Boolean> FROM_BED = ThreadLocal.withInitial(() -> false);

    private CarryOnIntegration() {}

    public static boolean fromBed() {
        return FROM_BED.get();
    }

    public static boolean carrying(Player player) {
        return CarryOnDataManager.getCarryData(player).isCarrying();
    }

    public static boolean ready(ServerPlayer player) {
        return CarrySupport.bothHands(player)
                && player.getMainHandItem().isEmpty()
                && player.getOffhandItem().isEmpty()
                && !carrying(player)
                && player.containerMenu == player.inventoryMenu
                && player.gameMode.getGameModeForPlayer()
                        != net.minecraft.world.level.GameType.ADVENTURE;
    }

    public static boolean protectedTarget(Entity target) {
        return target instanceof StretcherEntity
                || target instanceof LivingEntity living
                        && (TreatmentLock.active(living)
                                || (!fromBed() && living.getVehicle() instanceof StretcherEntity));
    }

    public static boolean pickUpFromBed(ServerPlayer player, LivingEntity patient) {
        if (!ready(player) || !StasisPatients.wrapped(patient) || TreatmentLock.active(patient)) {
            StasisPatients.notify(player, "cannot_carry");
            return false;
        }
        FROM_BED.set(true);
        try {
            boolean success =
                    PickupHandler.tryPickupEntity(
                            player,
                            com.hanmoyun.dghcompat.corpse.NativePatients.actual(patient),
                            entity ->
                                    !NeoForge.EVENT_BUS
                                            .post(new EntityPickupEvent(player, entity))
                                            .isCanceled());
            if (!success) StasisPatients.notify(player, "cannot_carry");
            return success;
        } finally {
            FROM_BED.remove();
        }
    }

    /**
     * Called for a wrapped or downed patient after the common checks and permission callback pass.
     */
    public static boolean pickupPatient(ServerPlayer player, LivingEntity patient) {
        if (!ready(player) || !StasisPatients.wrapped(patient) || !patient.isAlive()) return false;
        if (patient instanceof com.hanmoyun.dghcompat.corpse.CorpseEntity corpse)
            com.hanmoyun.dghcompat.corpse.CorpseLoot.stow(corpse);
        if (patient.isVehicle()) return false;
        var carry = CarryOnDataManager.getCarryData(player);
        if (patient instanceof ServerPlayer other) {
            if (!other.startRiding(player, true)) return false;
            carry.setCarryingPlayer();
            Services.PLATFORM.sendPacketToAllPlayers(
                    Constants.PACKET_ID_START_RIDING_OTHER,
                    new ClientboundStartRidingOtherPlayerPacket(
                            player.getId(), other.getId(), true),
                    player.serverLevel());
        } else {
            // Save before dismounting: Entity.save refuses passengers. saveWithoutId preserves all
            // attachments.
            net.minecraft.nbt.CompoundTag data = new net.minecraft.nbt.CompoundTag();
            var actual = com.hanmoyun.dghcompat.corpse.NativePatients.actual(patient);
            actual.saveWithoutId(data);
            data.putString(
                    "id",
                    net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
                            .getKey(actual.getType())
                            .toString());
            net.minecraft.nbt.CompoundTag carryTag = carry.getNbt();
            carryTag.putString("type", "ENTITY");
            carryTag.put("entity", data);
            var replacement = new CarryOnData(carryTag);
            patient.stopRiding();
            patient.remove(Entity.RemovalReason.UNLOADED_WITH_PLAYER);
            carry = replacement;
        }
        CarryOnDataManager.setCarryData(player, carry);
        return true;
    }

    public static boolean toBed(ServerPlayer player, BlockPos head) {
        var carry = CarryOnDataManager.getCarryData(player);
        if (!carry.isCarrying()) return false;
        if (player.containerMenu != player.inventoryMenu) return true;
        if (!CarrySupport.bothHands(player) || !PatientTransport.canDock(player, head)) {
            StasisPatients.notify(player, "cannot_place");
            return true;
        }
        boolean serialized = carry.isCarrying(CarryOnData.CarryType.ENTITY);
        Entity target =
                serialized
                        ? carry.getEntity(player.level())
                        : carry.isCarrying(CarryOnData.CarryType.PLAYER)
                                ? player.getFirstPassenger()
                                : null;
        var patient = com.hanmoyun.dghcompat.corpse.NativePatients.resolve(target);
        if (patient == null
                || !HealthCapability.has(patient)
                || !patient.isAlive()
                || TreatmentLock.active(patient)) {
            StasisPatients.notify(player, "cannot_place");
            return true;
        }
        if (serialized) {
            // Carry On reconstructs the serialized entity at its former position. Set its
            // destination first so an attempted docking never exposes it elsewhere.
            var state = player.level().getBlockState(head);
            var facing = state.getValue(com.lastimp.dgh.common.block.OperatingBedBlock.FACING);
            var feet = BedPose.feet(head, facing);
            com.hanmoyun.dghcompat.corpse.NativePatients.actual(patient)
                    .moveTo(feet.x, feet.y + 0.20, feet.z, facing.toYRot(), 0);
            if (!player.serverLevel()
                    .addFreshEntity(com.hanmoyun.dghcompat.corpse.NativePatients.actual(patient)))
                return true;
        }
        if (!PatientTransport.dock(player, patient, head)) {
            if (serialized) patient.discard();
            return true;
        }
        if (!serialized)
            Services.PLATFORM.sendPacketToAllPlayers(
                    Constants.PACKET_ID_START_RIDING_OTHER,
                    new ClientboundStartRidingOtherPlayerPacket(
                            player.getId(), patient.getId(), false),
                    player.serverLevel());
        carry.clear();
        CarryOnDataManager.setCarryData(player, carry);
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        StasisPatients.sync(patient);
        if (com.hanmoyun.dghcompat.corpse.Corpses.blocksTreatment(patient))
            StasisPatients.notify(player, "remove_bag");
        // Placement consumes this click. Treatment starts only on a later interaction.
        return true;
    }

    public static void tick(ServerPlayer player) {
        if (carrying(player) && !CarrySupport.bothHands(player)) {
            PlacementHandler.placeCarried(player);
        }
    }
}
