package com.hanmoyun.dghcompat;

import com.hanmoyun.dghcompat.mixin.StretcherControllerAccessor;
import com.lastimp.dgh.common.block.OperatingBedBlock;
import com.lastimp.dgh.common.capability.HealthCapability;
import com.lastimp.dgh.common.entity.StretcherEntity;
import com.lastimp.dgh.common.entry.register.ModEntities;
import com.lastimp.dgh.common.entry.register.ModItems;
import com.lastimp.dgh.common.item.tool.StretcherItem;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** Server-authoritative loading, pulling, docking and unloading of DGH stretchers. */
@EventBusSubscriber(modid = CompatMod.ID)
public final class PatientTransport {
    private static final String BED = "dghcompat:transport_bed";
    private static final String RETURNED = "dghcompat:stretcher_returned";

    private PatientTransport() {}

    public static BlockPos bed(StretcherEntity stretcher) {
        return stretcher.getPersistentData().contains(BED)
                ? BlockPos.of(stretcher.getPersistentData().getLong(BED))
                : null;
    }

    private static LivingEntity controller(StretcherEntity stretcher) {
        return ((StretcherControllerAccessor) stretcher).dghcompat$controller();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void specific(PlayerInteractEvent.EntityInteractSpecific event) {
        interact(event, event.getTarget());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void generic(PlayerInteractEvent.EntityInteract event) {
        interact(event, event.getTarget());
    }

    private static void interact(PlayerInteractEvent event, Entity target) {
        Player player = event.getEntity();
        if (player.isSpectator()) return;
        if (StasisPatients.interact(event, target)) {
            cancelEntity(event);
            return;
        }
        if (target instanceof LivingEntity injured
                && event.getHand() == InteractionHand.MAIN_HAND
                && StasisPatients.freeInjured(injured, player)) {
            if (!player.isShiftKeyDown()
                    && !(player.getMainHandItem().getItem() instanceof StretcherItem)) {
                if (player instanceof ServerPlayer doctor) GroundTreatment.open(doctor, injured);
                cancelEntity(event);
                return;
            }
        }
        StretcherEntity stretcher =
                target instanceof StretcherEntity s
                        ? s
                        : target.getVehicle() instanceof StretcherEntity s ? s : null;
        boolean loading =
                stretcher == null
                        && com.hanmoyun.dghcompat.corpse.NativePatients.resolve(target) != null
                        && player.getMainHandItem().getItem() instanceof StretcherItem;
        boolean sleeping =
                target instanceof LivingEntity living
                        && living.isSleeping()
                        && living.getSleepingPos()
                                .filter(
                                        pos ->
                                                player.level().getBlockState(pos).getBlock()
                                                        instanceof OperatingBedBlock)
                                .isPresent();
        boolean selfBed =
                target instanceof Player patientPlayer && BedPose.bed(patientPlayer) != null;
        if (stretcher == null && !loading && !((sleeping || selfBed) && player.isShiftKeyDown()))
            return;
        if (event instanceof PlayerInteractEvent.EntityInteractSpecific specific) {
            specific.setCanceled(true);
            specific.setCancellationResult(InteractionResult.SUCCESS);
        } else if (event instanceof PlayerInteractEvent.EntityInteract generic) {
            generic.setCanceled(true);
            generic.setCancellationResult(InteractionResult.SUCCESS);
        }
        if (event.getHand() != InteractionHand.MAIN_HAND || player.level().isClientSide()) return;
        if (!player.canInteractWithEntity(target, 0)
                || !target.isAlive()
                || HealthCapability.isDown(player)) return;
        if (stretcher != null) {
            if (player.isShiftKeyDown()) release(stretcher, player);
            else if (bed(stretcher) != null) {
                // Upgrade an occupied bed created by 2.4.13 without losing its stretcher.
                if (!stretcher.getPersistentData().getBoolean(RETURNED)) {
                    stretcher.getPersistentData().putBoolean(RETURNED, true);
                    refund(player);
                }
                openTreatment((ServerPlayer) player, bed(stretcher));
            } else {
                if (stretcher.getPatient() == null) release(stretcher, player);
                else if (controller(stretcher) != null && controller(stretcher) != player)
                    message(player, "busy");
                else {
                    // A player can pull only one stretcher; another right click releases the
                    // handle.
                    var oldController = controller(stretcher);
                    for (var other :
                            player.level()
                                    .getEntitiesOfClass(
                                            StretcherEntity.class,
                                            player.getBoundingBox().inflate(16))) {
                        if (controller(other) == player) other.setController(null);
                    }
                    stretcher.setController(oldController == player ? null : player);
                }
            }
            return;
        }
        LivingEntity patient = com.hanmoyun.dghcompat.corpse.NativePatients.resolve(target);
        if (selfBed && player.isShiftKeyDown() && patient instanceof ServerPlayer serverPatient) {
            if (TreatmentLock.active(patient)) {
                StasisPatients.notify(player, "locked");
                return;
            }
            serverPatient.closeContainer();
            return;
        }
        if (sleeping && player.isShiftKeyDown()) {
            if (TreatmentLock.active(patient)) {
                StasisPatients.notify(player, "locked");
                return;
            }
            if (patient instanceof ServerPlayer server) server.stopSleepInBed(true, true);
            else patient.stopSleeping();
            return;
        }
        if (!(HealthCapability.isDown(patient)
                        || com.hanmoyun.dghcompat.corpse.Corpses.isCorpse(patient))
                || patient.isPassenger()
                || patient.isSleeping()) {
            message(player, "not_down");
            return;
        }
        var created = new StretcherEntity(ModEntities.STRETCHER.get(), player.level());
        created.moveTo(patient.getX(), patient.getY() + 0.1, patient.getZ(), player.getYRot(), 0);
        if (!player.level().addFreshEntity(created)) return;
        if (!patient.startRiding(created, true)) {
            created.discard();
            return;
        }
        player.getMainHandItem().shrink(1);
    }

    private static void cancelEntity(PlayerInteractEvent event) {
        if (event instanceof PlayerInteractEvent.EntityInteractSpecific e) {
            e.setCanceled(true);
            e.setCancellationResult(InteractionResult.SUCCESS);
        } else if (event instanceof PlayerInteractEvent.EntityInteract e) {
            e.setCanceled(true);
            e.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    public static boolean canDock(ServerPlayer player, BlockPos head) {
        if (!player.serverLevel().isLoaded(head)
                || !player.canInteractWithBlock(head, 0)
                || !player.level().mayInteract(player, head)
                || !(player.level().getBlockState(head).getBlock() instanceof OperatingBedBlock))
            return false;
        if (!player.level()
                .getEntitiesOfClass(
                        StretcherEntity.class, new AABB(head).inflate(3), c -> head.equals(bed(c)))
                .isEmpty()) return false;
        return player.level()
                .getEntitiesOfClass(
                        LivingEntity.class,
                        new AABB(head).inflate(2),
                        entity ->
                                entity.getSleepingPos().filter(head::equals).isPresent()
                                        || entity instanceof Player p
                                                && head.equals(BedPose.bed(p)))
                .isEmpty();
    }

    public static boolean dock(ServerPlayer player, LivingEntity patient, BlockPos head) {
        if (!canDock(player, head) || TreatmentLock.active(patient)) return false;
        var anchor = new StretcherEntity(ModEntities.STRETCHER.get(), player.level());
        anchor.getPersistentData().putLong(BED, head.asLong());
        anchor.getPersistentData().putBoolean(RETURNED, true);
        hold(anchor, head);
        if (!player.level().addFreshEntity(anchor)) return false;
        if (!patient.startRiding(anchor, true)) {
            anchor.discard();
            return false;
        }
        hold(anchor, head);
        return true;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void block(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity().isSpectator()
                || !(event.getLevel().getBlockState(event.getPos()).getBlock()
                        instanceof OperatingBedBlock)) return;
        if (event.getHand() == InteractionHand.MAIN_HAND
                && event.getEntity() instanceof ServerPlayer player
                && handleBed(player, event.getPos())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    /** Also called before the cinematic bed-open packet can start self-treatment. */
    public static boolean handleBed(ServerPlayer player, BlockPos clicked) {
        var level = player.serverLevel();
        if (player.isSpectator()
                || !level.isLoaded(clicked)
                || !level.mayInteract(player, clicked)
                || !player.canInteractWithBlock(clicked, 0)) return false;
        var state = level.getBlockState(clicked);
        if (!(state.getBlock() instanceof OperatingBedBlock)) return false;
        BlockPos head =
                state.getValue(OperatingBedBlock.PART) == BedPart.HEAD
                        ? clicked
                        : clicked.relative(state.getValue(OperatingBedBlock.FACING));
        if (!level.isLoaded(head)
                || !level.mayInteract(player, head)
                || !(level.getBlockState(head).getBlock() instanceof OperatingBedBlock))
            return true;
        if (CaseBook.place(player, head)) return true;
        if (CarrySupport.toBed(player, head)) return true;
        var carriers = level.getEntitiesOfClass(StretcherEntity.class, new AABB(head).inflate(3));
        if (player.isShiftKeyDown()) {
            for (var carrier : carriers)
                if (head.equals(bed(carrier))) {
                    release(carrier, player);
                    return true;
                }
            for (var patient :
                    level.getEntitiesOfClass(LivingEntity.class, new AABB(head).inflate(2))) {
                if (patient instanceof ServerPlayer server && head.equals(BedPose.bed(server))) {
                    StasisPatients.notify(player, "locked");
                    return true;
                }
                if (patient.getSleepingPos().filter(head::equals).isPresent()) {
                    if (TreatmentLock.active(patient)) {
                        StasisPatients.notify(player, "locked");
                        return true;
                    }
                    if (patient instanceof ServerPlayer server) server.stopSleepInBed(true, true);
                    else patient.stopSleeping();
                    return true;
                }
            }
            return true; // Shift-right-click on an empty bed must not start self-treatment.
        }
        var dragged =
                level.getEntitiesOfClass(
                        StretcherEntity.class,
                        player.getBoundingBox().inflate(8),
                        carrier -> controller(carrier) == player && bed(carrier) == null);
        if (dragged.isEmpty()) return false;
        var carrier = dragged.getFirst();
        var patient = carrier.getPatient();
        if (patient == null || !patient.isAlive() || HealthCapability.isDown(player)) return true;
        boolean occupied = carriers.stream().anyMatch(other -> head.equals(bed(other)));
        occupied |=
                !level.getEntitiesOfClass(
                                LivingEntity.class,
                                new AABB(head).inflate(2),
                                entity ->
                                        entity != patient
                                                && (entity.getSleepingPos()
                                                                .filter(head::equals)
                                                                .isPresent()
                                                        || entity instanceof Player p
                                                                && head.equals(BedPose.bed(p))))
                        .isEmpty();
        if (occupied) {
            message(player, "occupied");
            return true;
        }
        // Replace the physical stretcher with a bed anchor; refund only after the transfer
        // succeeds.
        var anchor = new StretcherEntity(ModEntities.STRETCHER.get(), level);
        anchor.getPersistentData().putLong(BED, head.asLong());
        anchor.getPersistentData().putBoolean(RETURNED, true);
        anchor.setNoGravity(true);
        anchor.setInvisible(true);
        hold(anchor, head);
        if (!level.addFreshEntity(anchor)) return true;
        if (!patient.startRiding(anchor, true)) {
            anchor.discard();
            return true;
        }
        carrier.setController(null);
        carrier.discard();
        refund(player);
        hold(anchor, head);
        return true;
    }

    private static void hold(StretcherEntity carrier, BlockPos head) {
        var state = carrier.level().getBlockState(head);
        if (!(state.getBlock() instanceof OperatingBedBlock)) return;
        var direction = state.getValue(OperatingBedBlock.FACING);
        Vec3 position = BedPose.feet(head, direction).add(0, 0.20, 0);
        carrier.setNoGravity(true);
        carrier.setInvisible(true);
        carrier.setPos(position.x, position.y, position.z);
        carrier.setYRot(direction.toYRot());
        carrier.setDeltaMovement(Vec3.ZERO);
        if (carrier.getPatient() != null) {
            carrier.getPatient().setYRot(direction.toYRot());
            carrier.getPatient().setYBodyRot(direction.toYRot());
            HealthCapability.getAndApply(carrier.getPatient(), health -> health.setNearBedTick(40));
        }
    }

    @SubscribeEvent
    public static void beforeTick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof StretcherEntity carrier)
                || carrier.level().isClientSide()) return;
        var puller = controller(carrier);
        if (puller != null
                && (puller.isRemoved()
                        || !puller.isAlive()
                        || puller.level() != carrier.level()
                        || HealthCapability.isDown(puller))) carrier.setController(null);
    }

    @SubscribeEvent
    public static void tick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof StretcherEntity carrier)
                || carrier.level().isClientSide()) return;
        var puller = controller(carrier);
        if (puller != null
                && (puller.isRemoved()
                        || !puller.isAlive()
                        || puller.level() != carrier.level()
                        || HealthCapability.isDown(puller))) carrier.setController(null);
        var bed = bed(carrier);
        if (bed == null) return;
        if (!(carrier.level().getBlockState(bed).getBlock() instanceof OperatingBedBlock)) {
            carrier.getPersistentData().remove(BED);
            carrier.setNoGravity(false);
            carrier.setInvisible(false);
            if (carrier.getPersistentData().getBoolean(RETURNED)) {
                carrier.ejectPassengers();
                carrier.discard();
            }
        } else if (carrier.getPatient() == null) {
            if (!carrier.getPersistentData().getBoolean(RETURNED))
                carrier.spawnAtLocation(ModItems.STRETCHER.get());
            carrier.discard();
        } else hold(carrier, bed);
    }

    private static void release(StretcherEntity carrier, Player player) {
        if (carrier.isRemoved()) return;
        if (controller(carrier) != null && controller(carrier) != player) {
            message(player, "busy");
            return;
        }
        var patient = carrier.getPatient();
        if (patient != null && TreatmentLock.active(patient)) {
            StasisPatients.notify(player, "locked");
            return;
        }
        if (patient != null && StasisPatients.wrapped(patient) && CarrySupport.loaded()) {
            CarrySupport.pickUp((ServerPlayer) player, patient);
            return;
        }
        if (patient != null) {
            Vec3 exit = null;
            for (var direction : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                Vec3 candidate =
                        carrier.position()
                                .add(direction.getStepX() * 1.5, 0.1, direction.getStepZ() * 1.5);
                if (carrier.level()
                        .noCollision(
                                patient,
                                patient.getDimensions(patient.getPose())
                                        .makeBoundingBox(candidate))) {
                    exit = candidate;
                    break;
                }
            }
            if (exit == null) {
                message(player, "no_space");
                return;
            }
            patient.stopRiding();
            patient.teleportTo(exit.x, exit.y, exit.z);
        }
        carrier.setController(null);
        if (!carrier.getPersistentData().getBoolean(RETURNED)) refund(player);
        carrier.discard();
    }

    private static void refund(Player player) {
        var item = new ItemStack(ModItems.STRETCHER.get());
        if (!player.getInventory().add(item)) player.drop(item, false);
    }

    public static void openTreatment(ServerPlayer player, BlockPos bed) {
        for (var carrier :
                player.level()
                        .getEntitiesOfClass(StretcherEntity.class, new AABB(bed).inflate(3))) {
            if (bed.equals(bed(carrier)) && carrier.getPatient() != null) {
                if (com.hanmoyun.dghcompat.corpse.Corpses.blocksTreatment(carrier.getPatient())) {
                    StasisPatients.notify(player, "remove_bag");
                    return;
                }
                if (TreatmentLock.active(carrier.getPatient())) {
                    StasisPatients.notify(player, "locked");
                    return;
                }
            }
        }
        if (net.neoforged.fml.ModList.get().isLoaded("cinematic")) BedInteraction.open(player, bed);
        else {
            for (var carrier :
                    player.level()
                            .getEntitiesOfClass(StretcherEntity.class, new AABB(bed).inflate(3))) {
                if (bed.equals(bed(carrier)) && carrier.getPatient() != null) {
                    com.lastimp.dgh.common.menu.MenuOpenWrapper.openHealthMenu(
                            player, carrier.getPatient().getUUID(), false);
                    break;
                }
            }
        }
    }

    private static void message(Player player, String key) {
        player.displayClientMessage(Component.translatable("dghcompat.transport." + key), true);
    }
}
