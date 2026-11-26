package com.hanmoyun.dghcompat;

import com.hanmoyun.dghcompat.treatment.TreatmentService;
import com.lastimp.dgh.common.block.OperatingBedBlock;
import com.lastimp.dgh.common.capability.HealthCapability;
import com.lastimp.dgh.common.menu.HealthMenu;
import com.lastimp.dgh.common.menu.MenuOpenWrapper;

import dev.dyslexic.cinematic.api.AddonStations;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@EventBusSubscriber(modid = CompatMod.ID)
public final class BedInteraction {
    public static final ResourceLocation BED =
            ResourceLocation.fromNamespaceAndPath("dgh", "operating_bed");

    private record SelfTreatment(
            BlockPos bed, HealthMenu menu, Pose previousPose, boolean noGravity) {}

    private static final Map<ServerPlayer, SelfTreatment> SELF_TREATMENT = new WeakHashMap<>();
    static Consumer<BedPackets.Position> clientPose = packet -> {};
    static BiConsumer<BlockPos, Integer> openScene = (pos, menuId) -> {};

    private BedInteraction() {}

    static void registerStation() {
        AddonStations.register(
                new AddonStations.Definition(
                        BED,
                        BED,
                        HealthMenu.class,
                        state -> {
                            var facing = state.getValue(OperatingBedBlock.FACING);
                            return List.of(facing.getClockWise(), facing.getCounterClockWise());
                        }));
    }

    static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("5")
                .optional()
                .playToServer(
                        BedPackets.Request.TYPE,
                        BedPackets.Request.CODEC,
                        (packet, context) ->
                                context.enqueueWork(
                                        () -> {
                                            if (context.player() instanceof ServerPlayer player)
                                                open(player, packet.pos());
                                        }))
                .playToServer(
                        BedPackets.OrganMode.TYPE,
                        BedPackets.OrganMode.CODEC,
                        (packet, context) ->
                                context.enqueueWork(
                                        () -> {
                                            if (context.player() instanceof ServerPlayer doctor)
                                                TreatmentService.organMode(
                                                        doctor,
                                                        packet.menuId(),
                                                        packet.part(),
                                                        packet.active());
                                        }))
                .playToServer(
                        BedPackets.Treat.TYPE,
                        BedPackets.Treat.CODEC,
                        (packet, context) ->
                                context.enqueueWork(
                                        () -> {
                                            if (context.player() instanceof ServerPlayer player)
                                                TreatmentService.treat(
                                                        player, packet.menuId(), packet.part());
                                        }))
                .playToClient(
                        BedPackets.Position.TYPE,
                        BedPackets.Position.CODEC,
                        (packet, context) -> context.enqueueWork(() -> clientPose.accept(packet)))
                .playToClient(
                        BedPackets.Open.TYPE,
                        BedPackets.Open.CODEC,
                        (packet, context) ->
                                context.enqueueWork(
                                        () -> openScene.accept(packet.pos(), packet.menuId())));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void use(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()
                && event.getHand() == InteractionHand.MAIN_HAND
                && event.getUseBlock() != TriState.FALSE
                && !event.getEntity().isSpectator()
                && event.getLevel().getBlockState(event.getPos()).getBlock()
                        instanceof OperatingBedBlock
                && BedSceneClient.canRequest()) {
            BedSceneClient.request(event.getPos());
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    public static BlockPos head(BlockPos pos, BlockState state) {
        return state.getValue(OperatingBedBlock.PART) == BedPart.HEAD
                ? pos
                : pos.relative(state.getValue(OperatingBedBlock.FACING));
    }

    static void open(ServerPlayer doctor, BlockPos clicked) {
        var level = doctor.serverLevel();
        if (!doctor.connection.hasChannel(BedPackets.Open.TYPE)
                || !doctor.isAlive()
                || doctor.isSpectator()
                || !level.isLoaded(clicked)
                || !level.mayInteract(doctor, clicked)
                || !doctor.canInteractWithBlock(clicked, 0)) return;
        var clickedState = level.getBlockState(clicked);
        if (!(clickedState.getBlock() instanceof OperatingBedBlock)) return;
        BlockPos head = head(clicked, clickedState);
        if (PatientTransport.handleBed(doctor, clicked)) return;
        if (!level.isLoaded(head)
                || !level.mayInteract(doctor, head)
                || !(level.getBlockState(head).getBlock() instanceof OperatingBedBlock)) return;

        var occupants =
                level.getEntitiesOfClass(
                        LivingEntity.class,
                        new AABB(head).inflate(2),
                        entity ->
                                entity.isAlive()
                                        && (entity.getSleepingPos().filter(head::equals).isPresent()
                                                || entity.getVehicle()
                                                                instanceof
                                                                com.lastimp.dgh.common.entity
                                                                                        .StretcherEntity
                                                                                carrier
                                                        && head.equals(
                                                                PatientTransport.bed(carrier))
                                                || entity instanceof ServerPlayer player
                                                        && SELF_TREATMENT.containsKey(player)
                                                        && head.equals(
                                                                SELF_TREATMENT.get(player).bed())));
        for (var carrier :
                level.getEntitiesOfClass(
                        com.lastimp.dgh.common.entity.StretcherEntity.class,
                        new AABB(head).inflate(3)))
            if (head.equals(PatientTransport.bed(carrier))
                    && carrier.getPatient() != null
                    && !occupants.contains(carrier.getPatient()))
                occupants.add(carrier.getPatient());
        LivingEntity patient = occupants.isEmpty() ? doctor : occupants.getFirst();
        if (!HealthCapability.has(patient)) return;
        if (com.hanmoyun.dghcompat.corpse.Corpses.blocksTreatment(patient)) {
            StasisPatients.notify(doctor, "remove_bag");
            return;
        }
        if (TreatmentLock.active(patient)) {
            StasisPatients.notify(doctor, "locked");
            return;
        }
        SelfTreatment active = SELF_TREATMENT.get(doctor);
        if (active != null) return;
        if (doctor.containerMenu != doctor.inventoryMenu) doctor.closeContainer();

        HealthCapability.getAndApply(patient, health -> health.setNearBedTick(40));
        MenuOpenWrapper.openHealthMenu(doctor, patient.getUUID(), false);
        if (doctor.containerMenu instanceof HealthMenu menu) {
            if (patient == doctor) {
                if (doctor.isSleeping()) doctor.stopSleepInBed(true, true);
                doctor.stopRiding();
                var previous =
                        new SelfTreatment(head, menu, doctor.getForcedPose(), doctor.isNoGravity());
                SELF_TREATMENT.put(doctor, previous);
                BedPose.set(doctor, head);
                var feet =
                        BedPose.feet(
                                head, level.getBlockState(head).getValue(OperatingBedBlock.FACING));
                doctor.teleportTo(feet.x, feet.y, feet.z);
                doctor.setNoGravity(true);
                BedPose.hold(doctor, feet);
                syncPose(doctor, head, true);
            }
            PacketDistributor.sendToPlayer(doctor, new BedPackets.Open(head, menu.containerId));
        }
    }

    @SubscribeEvent
    public static void hold(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var treatment = SELF_TREATMENT.get(player);
        if (treatment == null) return;
        var state = player.level().getBlockState(treatment.bed());
        if (!player.isAlive()
                || player.containerMenu != treatment.menu()
                || !(state.getBlock() instanceof OperatingBedBlock)) {
            wake(player);
            if (player.containerMenu == treatment.menu()) player.closeContainer();
            return;
        }
        BedPose.hold(
                player, BedPose.feet(treatment.bed(), state.getValue(OperatingBedBlock.FACING)));
        HealthCapability.getAndApply(player, health -> health.setNearBedTick(40));
    }

    @SubscribeEvent
    public static void track(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer viewer
                && event.getTarget() instanceof ServerPlayer patient) {
            var treatment = SELF_TREATMENT.get(patient);
            if (treatment != null && viewer.connection.hasChannel(BedPackets.Position.TYPE)) {
                PacketDistributor.sendToPlayer(
                        viewer, new BedPackets.Position(patient.getId(), treatment.bed(), true));
            }
        }
    }

    private static void syncPose(ServerPlayer patient, BlockPos bed, boolean lying) {
        var packet = new BedPackets.Position(patient.getId(), bed, lying);
        for (var viewer : patient.serverLevel().players()) {
            if (viewer.connection.hasChannel(BedPackets.Position.TYPE)) {
                PacketDistributor.sendToPlayer(viewer, packet);
            }
        }
    }

    @SubscribeEvent
    public static void closed(PlayerContainerEvent.Close event) {
        if (event.getEntity() instanceof ServerPlayer player
                && event.getContainer() instanceof HealthMenu) wake(player);
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) wake(player);
    }

    private static void wake(ServerPlayer player) {
        var treatment = SELF_TREATMENT.remove(player);
        if (treatment == null) return;
        BedPose.set(player, null);
        player.setNoGravity(treatment.noGravity());
        player.setForcedPose(treatment.previousPose());
        player.setPose(treatment.previousPose() == null ? Pose.STANDING : treatment.previousPose());
        syncPose(player, treatment.bed(), false);
        if (!player.isAlive()) return;
        for (var direction : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            var exit =
                    net.minecraft.world.phys.Vec3.atBottomCenterOf(
                            treatment.bed().relative(direction));
            if (player.level()
                    .noCollision(
                            player, player.getDimensions(player.getPose()).makeBoundingBox(exit))) {
                player.teleportTo(exit.x, exit.y, exit.z);
                break;
            }
        }
    }
}
