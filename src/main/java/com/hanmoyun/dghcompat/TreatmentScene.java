package com.hanmoyun.dghcompat;

import com.lastimp.dgh.common.block.OperatingBedBlock;
import com.lastimp.dgh.common.enums.BodyComponents;
import com.lastimp.dgh.common.item.bases.AbstractHealingItem;
import com.lastimp.dgh.common.menu.HealthMenu;
import com.lastimp.dgh.common.tags.ModTags;
import com.mojang.blaze3d.vertex.PoseStack;

import dev.dyslexic.cinematic.api.client.AddonMenuStation;
import dev.dyslexic.cinematic.client.CameraRig;
import dev.dyslexic.cinematic.client.Picking;
import dev.dyslexic.cinematic.client.Scene;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public final class TreatmentScene extends AddonMenuStation<HealthMenu> {
    private long toolRequestTick = Long.MIN_VALUE;
    private net.minecraft.nbt.CompoundTag tools = new net.minecraft.nbt.CompoundTag();

    void receiveTools(BedToolPackets.State state) {
        if (menu() != null && menu().containerId == state.menu()) tools = state.data();
    }

    void useBedTool(int index, boolean remove) {
        var connection = Minecraft.getInstance().getConnection();
        if (connection == null
                || !connection.hasChannel(BedToolPackets.Action.TYPE)
                || menu() == null) return;
        PacketDistributor.sendToServer(
                new BedToolPackets.Action(menu().containerId, index, remove));
    }

    boolean scannerActive(int tool) {
        return tools.getBoolean(tool == BedToolPackets.HEALTH ? "health" : "blood");
    }

    boolean pulseLocked() {
        return tools.getBoolean("wrapped");
    }

    int bloodPage;

    boolean bookPlaced() {
        return tools.getBoolean("book");
    }

    net.minecraft.world.item.ItemStack toolStack(int index) {
        var level = Minecraft.getInstance().level;
        if (level == null) return net.minecraft.world.item.ItemStack.EMPTY;
        if (index == BedToolPackets.HEALTH || index == BedToolPackets.BLOOD)
            return new net.minecraft.world.item.ItemStack(BedToolPackets.scanner(index));
        if (index == BedToolPackets.BOOK)
            return new net.minecraft.world.item.ItemStack(
                    net.minecraft.world.item.Items.WRITABLE_BOOK);
        return net.minecraft.world.item.ItemStack.parseOptional(
                level.registryAccess(),
                tools.getCompound(index == BedToolPackets.OXYGEN ? "oxygen" : "pulse"));
    }

    private boolean organMode;
    private long cyberRequestTick = Long.MIN_VALUE;
    private net.minecraft.nbt.CompoundTag cyberData = new net.minecraft.nbt.CompoundTag();

    void receiveCyberData(CyberPatientPackets.State state) {
        if (menu() == null || state.menuId() != menu().containerId) return;
        cyberData = state.data();
    }

    boolean cyberManaged() {
        return cyberData.getBoolean("managed");
    }

    net.minecraft.nbt.CompoundTag cyberData() {
        return cyberData;
    }

    private boolean organsActive;
    private BodyComponents organPart;
    private BodyComponents hovered;

    boolean organMode() {
        return organMode;
    }

    boolean organsActive() {
        return organsActive;
    }

    private void bindOrganMode() {
        var menu = menu();
        if (menu == null || patientHealth == null || selected == null) return;
        var body =
                (com.lastimp.dgh.common.capability.bodyPart.base.AbstractVisibleBody)
                        patientHealth.getComponent(selected);
        OrganCapacity.bind(patientHealth, body);
        boolean active =
                organMode
                        && body.abnormal(
                                com.lastimp
                                        .dgh
                                        .common
                                        .capability
                                        .bodyPart
                                        .base
                                        .BodyCondition
                                        .RETRACTED_SKIN)
                        && (showArm(true) || showArm(false));
        if (organPart == selected && organsActive == active && organHealth == patientHealth) return;
        if (organPart != selected || organsActive != active)
            PacketDistributor.sendToServer(
                    new BedPackets.OrganMode(menu.containerId, selected.ordinal(), active));
        menu.setOrganActive(active, body);
        organPart = selected;
        organHealth = patientHealth;
        organsActive = active;
    }

    private com.lastimp.dgh.common.capability.HealthCapability organHealth;
    private BodyComponents selected = BodyComponents.HEAD;
    private boolean explicitlySelected;
    private CameraRig.Pose cameraPose;
    private long lastFrame;
    private long selectedAt;
    private long usedAt;
    private long scanTick = -1;
    private boolean ended;
    private final boolean selfTreatment;
    private Vec3 doctorEye;
    private Vec3 doctorForward;
    private java.util.UUID subscribedPatient;
    private java.util.UUID subscribedDoctor;
    private com.lastimp.dgh.common.capability.HealthCapability doctorHealth;
    private final String healthCallback = CompatMod.ID + ":scene:" + java.util.UUID.randomUUID();
    private com.lastimp.dgh.common.capability.HealthCapability patientHealth;

    public BlockPos renderBed() {
        return pos;
    }

    public boolean renderingSelf() {
        return self();
    }

    public com.lastimp.dgh.common.capability.HealthCapability patientHealth() {
        return patientHealth;
    }

    private void subscribeHealth() {
        var patient = menu().targetEntity;
        if (patient.equals(subscribedPatient)) return;
        releaseHealth();
        subscribedPatient = patient;
        com.lastimp.dgh.api.client.DGH_ClientAPI.beginCallingDataFromServer(
                patient,
                List.of(
                        new oshi.util.tuples.Pair<>(
                                healthCallback,
                                health -> {
                                    if (patient.equals(subscribedPatient)) patientHealth = health;
                                })));
        com.lastimp.dgh.api.client.DGH_ClientAPI.callForHealthDataFromServer(patient);
        if (!self()) {
            subscribedDoctor = Minecraft.getInstance().player.getUUID();
            var doctor = subscribedDoctor;
            com.lastimp.dgh.api.client.DGH_ClientAPI.beginCallingDataFromServer(
                    doctor,
                    List.of(
                            new oshi.util.tuples.Pair<>(
                                    healthCallback + ":doctor",
                                    health -> {
                                        if (doctor.equals(subscribedDoctor)) doctorHealth = health;
                                    })));
            com.lastimp.dgh.api.client.DGH_ClientAPI.callForHealthDataFromServer(doctor);
        }
    }

    private void releaseHealth() {
        if (subscribedPatient != null) {
            com.lastimp.dgh.api.client.DGH_ClientAPI.endCallingDataFromServer(
                    subscribedPatient, List.of(healthCallback));
        }
        if (subscribedDoctor != null) {
            com.lastimp.dgh.api.client.DGH_ClientAPI.endCallingDataFromServer(
                    subscribedDoctor, List.of(healthCallback + ":doctor"));
        }
        subscribedDoctor = null;
        if (!ended) doctorHealth = null;
        subscribedPatient = null;
        if (!ended) patientHealth = null;
        scanTick = -1;
    }

    @Override
    public void onEnd() {
        ended = true;
        try {
            super.onEnd();
        } finally {
            releaseHealth();
        }
    }

    private Vec3 along() {
        return Vec3.atLowerCornerOf(heading().getNormal());
    }

    private Vec3 across() {
        return Vec3.atLowerCornerOf(heading().getCounterClockWise().getNormal());
    }

    public Vec3 shoulder(boolean rightHand) {
        if (self())
            return SelfSurgeryMotion.shoulder(
                    BedPose.feet(pos, heading()), along(), across(), rightHand);
        prepareDoctorPose();
        Vec3 lateral = new Vec3(-doctorForward.z, 0, doctorForward.x);
        return doctorEye
                .add(doctorForward.scale(-0.10))
                .add(lateral.scale(rightHand ? 0.24 : -0.24))
                .add(0, -0.43, 0);
    }

    private void prepareDoctorPose() {

        var player = Minecraft.getInstance().player;
        doctorEye = player.getEyePosition();
        Vec3 towardBed = center().subtract(doctorEye).multiply(1, 0, 1);
        doctorForward = towardBed.lengthSqr() < 1e-6 ? side().scale(-1) : towardBed.normalize();
    }

    private boolean cameraSettled() {
        if (cameraPose == null) return false;
        var target = destination();
        return cameraPose.pos().distanceToSqr(target.pos()) < 0.0009
                && Math.abs(net.minecraft.util.Mth.wrapDegrees(target.yaw() - cameraPose.yaw()))
                        < 1.5
                && Math.abs(target.pitch() - cameraPose.pitch()) < 1.5;
    }

    public BodyComponents selectedPart() {
        return selected;
    }

    /** 是否仍有这条手臂；存在与能否操作分开判断，失能不等于截肢。 */
    public boolean armPresent(boolean right) {
        var health = self() ? patientHealth() : doctorHealth;
        return health != null && armPresent(health, right);
    }

    private boolean armPresent(
            com.lastimp.dgh.common.capability.HealthCapability health, boolean right) {
        var body = health.getComponent(right ? BodyComponents.RIGHT_ARM : BodyComponents.LEFT_ARM);
        return !body.abnormal(
                        com.lastimp
                                .dgh
                                .common
                                .capability
                                .bodyPart
                                .base
                                .BodyCondition
                                .SURGICAL_AMPUTATION)
                && !body.abnormal(
                        com.lastimp
                                .dgh
                                .common
                                .capability
                                .bodyPart
                                .base
                                .BodyCondition
                                .TRAUMATIC_AMPUTATION);
    }

    private boolean armUsable(boolean right) {
        if (!armPresent(right)) return false;
        var health = self() ? patientHealth() : doctorHealth;
        return health != null
                && com.lastimp.dgh.common.capability.bodyPart.base.AbstractExtremities.available(
                        health, right ? BodyComponents.RIGHT_ARM : BodyComponents.LEFT_ARM);
    }

    /** 电影化操作手：自疗时隐藏被检查侧，只让另一只可用手参与操作。 */
    public boolean showArm(boolean right) {
        if (!armUsable(right)) return false;
        return !self()
                || !explicitlySelected
                || selected != (right ? BodyComponents.RIGHT_ARM : BodyComponents.LEFT_ARM);
    }

    /** 原版身体手臂：保留被检查侧或失能侧，避免与电影化操作手重复显示。 */
    public boolean showBodyArm(boolean right) {
        if (!armPresent(right) || !explicitlySelected) return false;
        return !showArm(right);
    }

    public boolean hasExplicitSelection() {
        return explicitlySelected;
    }

    /** 床坐标下的身体分区；拾取和轮廓共用，尺寸保持已验证版本的数值。 */
    private net.minecraft.world.phys.AABB region(BodyComponents part) {
        double lo, hi, left, right;
        switch (part) {
            case HEAD -> {
                lo = 1.50;
                hi = 2.0;
                left = -0.25;
                right = 0.25;
            }
            case TORSO -> {
                lo = 0.75;
                hi = 1.50;
                left = -0.25;
                right = 0.25;
            }
            case LEFT_ARM -> {
                lo = 0.75;
                hi = 1.50;
                left = -0.50;
                right = -0.25;
            }
            case RIGHT_ARM -> {
                lo = 0.75;
                hi = 1.50;
                left = 0.25;
                right = 0.50;
            }
            case LEFT_LEG -> {
                lo = 0;
                hi = 0.75;
                left = -0.25;
                right = 0;
            }
            case RIGHT_LEG -> {
                lo = 0;
                hi = 0.75;
                left = 0;
                right = 0.25;
            }
            default -> {
                return null;
            }
        }
        var patient = targetPatient();
        boolean bag = patient != null && StasisPatients.wrapped(patient);
        float size = patient == null || bag ? 1 : patient.getScale();
        // DGH transports use a feet anchor. Fit the boxes to the model and keep the
        // same bounds for cursor picking, outline drawing, and camera aim.
        lo *= size;
        hi *= size;
        left *= size;
        right *= size;
        double thickness = (bag ? 0.19 : 0.175) * size;
        Vec3 feet = patientFeet();
        return new net.minecraft.world.phys.AABB(
                feet.add(along().scale(lo)).add(across().scale(left)).add(0, -thickness, 0),
                feet.add(along().scale(hi)).add(across().scale(right)).add(0, thickness, 0));
    }

    public boolean worksLeft() {
        var player = Minecraft.getInstance().player;
        if (!showArm(true)) return true;
        if (!showArm(false)) return false;
        return player != null && player.getMainArm() == net.minecraft.world.entity.HumanoidArm.LEFT;
    }

    private net.minecraft.world.entity.LivingEntity targetPatient() {
        var client = Minecraft.getInstance();
        if (client.level == null) return null;
        if (menu() == null) return null;
        for (var entity : client.level.entitiesForRendering()) {
            if (com.hanmoyun.dghcompat.corpse.NativePatients.resolve(entity)
                            instanceof net.minecraft.world.entity.LivingEntity living
                    && entity.getUUID().equals(menu().targetEntity)) return living;
        }
        return null;
    }

    private Vec3 bodyPoint(BodyComponents part) {
        var box = region(part);
        return box == null ? center() : box.getCenter();
    }

    @Override
    public void beforeFrame() {
        var client = Minecraft.getInstance();
        if (ended || client.level == null || menu() == null) return;
        subscribeHealth();
        bindOrganMode();
        long tick = client.level.getGameTime();
        if (toolRequestTick == Long.MIN_VALUE || tick - toolRequestTick >= 20) {
            toolRequestTick = tick;
            useBedTool(-1, false);
        }
        if (net.neoforged.fml.ModList.get().isLoaded("createcybernetics")
                && client.getConnection() != null
                && client.getConnection().hasChannel(CyberPatientPackets.Request.TYPE)
                && (cyberRequestTick == Long.MIN_VALUE || tick - cyberRequestTick >= 20)) {
            cyberRequestTick = tick;
            PacketDistributor.sendToServer(new CyberPatientPackets.Request(menu().containerId));
        }
        if (tick != scanTick && tick % 5 == 0) {
            scanTick = tick;
            com.lastimp.dgh.api.client.DGH_ClientAPI.callForHealthDataFromServer(subscribedPatient);
            if (subscribedDoctor != null)
                com.lastimp.dgh.api.client.DGH_ClientAPI.callForHealthDataFromServer(
                        subscribedDoctor);
        }
        CameraRig.Pose destination = destination();
        long now = System.nanoTime();
        double dt = lastFrame == 0 ? 0 : Math.min(0.1, (now - lastFrame) / 1e9);
        lastFrame = now;
        if (cameraPose == null) cameraPose = destination;
        float k = (float) (1 - Math.exp(-7 * dt));
        cameraPose =
                new CameraRig.Pose(
                        cameraPose.pos().lerp(destination.pos(), k),
                        cameraPose.yaw()
                                + net.minecraft.util.Mth.wrapDegrees(
                                                destination.yaw() - cameraPose.yaw())
                                        * k,
                        net.minecraft.util.Mth.lerp(k, cameraPose.pitch(), destination.pitch()),
                        0);
    }

    TreatmentScene(BlockPos pos, Direction side) {
        super(pos, side, HealthMenu.class, 0);
        var player = Minecraft.getInstance().player;
        selfTreatment =
                player != null
                        && player.containerMenu instanceof HealthMenu healthMenu
                        && healthMenu.targetEntity.equals(player.getUUID());
    }

    int organCapacity() {
        if (patientHealth == null || selected == null) return 0;
        var body = patientHealth.getComponent(selected);
        return OrganCapacity.capacity(patientHealth, body, 0)
                + OrganCapacity.capacity(patientHealth, body, 1)
                + OrganCapacity.capacity(patientHealth, body, 2);
    }

    int organUsed() {
        if (patientHealth == null || selected == null) return 0;
        int used = 0;
        for (var stack : patientHealth.getComponent(selected).organ()) if (!stack.isEmpty()) used++;
        return used;
    }

    boolean organSlotVisible(int index) {
        if (!organsActive || patientHealth == null || index < 0 || index >= 36) return false;
        var body = patientHealth.getComponent(selected);
        return index % 12 < OrganCapacity.capacity(patientHealth, body, index / 12);
    }

    net.minecraft.world.item.ItemStack organStack(int index) {
        return organSlotVisible(index) && menu() != null
                ? menu().getSlot(47 + index).getItem()
                : net.minecraft.world.item.ItemStack.EMPTY;
    }

    public boolean self() {
        return selfTreatment;
    }

    private Direction heading() {
        var state = Minecraft.getInstance().level.getBlockState(pos);
        return state.getOptionalValue(OperatingBedBlock.FACING).orElse(Direction.SOUTH);
    }

    private Vec3 patientFeet() {
        if (!self()) {
            var patient = targetPatient();
            if (patient != null) {
                var actual = com.hanmoyun.dghcompat.corpse.NativePatients.actual(patient);
                if (actual.getVehicle()
                                instanceof com.lastimp.dgh.common.entity.StretcherEntity carrier
                        && carrier.isInvisible()) return carrier.position();
                return actual.position();
            }
        }
        return BedPose.feet(pos, heading());
    }

    private Vec3 head() {
        return patientFeet()
                .add(Vec3.atLowerCornerOf(heading().getNormal()).scale(1.58))
                .add(0, 0.10, 0);
    }

    @Override
    public Vec3 center() {
        return head().subtract(Vec3.atLowerCornerOf(heading().getNormal()).scale(0.7));
    }

    @Override
    public CameraRig.Pose lean() {
        return cameraPose == null ? destination() : cameraPose;
    }

    private CameraRig.Pose destination() {
        if (selected != null && selected != BodyComponents.HEAD && self()) {
            Vec3 eye =
                    SelfSurgeryMotion.destination(
                            BedPose.feet(pos, heading()),
                            Vec3.atLowerCornerOf(heading().getNormal()),
                            Vec3.atLowerCornerOf(heading().getCounterClockWise().getNormal()),
                            selected.name());
            return lookingAt(eye, bodyPoint(selected));
        }
        if (self()) {
            var along = Vec3.atLowerCornerOf(heading().getNormal());
            Vec3 eye = SelfSurgeryMotion.home(BedPose.feet(pos, heading()), along);
            return new CameraRig.Pose(eye, heading().getOpposite().toYRot(), 16, 0);
        }
        prepareDoctorPose();
        Vec3 eye = doctorEye;
        if (explicitlySelected) eye = eye.add(doctorForward.scale(0.08)).add(0, -0.06, 0);
        return lookingAt(eye, explicitlySelected ? bodyPoint(selected) : center());
    }

    private CameraRig.Pose lookingAt(Vec3 eye, Vec3 target) {
        Vec3 look = target.subtract(eye);
        float yaw = (float) Math.toDegrees(Math.atan2(-look.x, look.z));
        float pitch = (float) Math.toDegrees(Math.atan2(-look.y, Math.hypot(look.x, look.z)));
        return new CameraRig.Pose(eye, yaw, pitch, 0);
    }

    @Override
    public boolean inFront(Vec3 feet) {
        // Either long side of a bed is a valid operating position.
        return Math.abs(feet.subtract(center()).dot(across())) >= 0.65;
    }

    @Override
    public List<Vec3> approach(LocalPlayer player) {
        if (self()) return List.of();
        Vec3 middle = center();
        Vec3 lateral = across();
        if (player.position().subtract(middle).dot(lateral) < 0) lateral = lateral.scale(-1);
        Vec3 feet = middle.add(lateral.scale(0.95));
        return List.of(new Vec3(feet.x, player.getY(), feet.z));
    }

    @Override
    public Vec3 handTarget(Vec3 target, Vec3 shoulder, boolean carrying, double tap) {
        double elapsed = (System.nanoTime() - usedAt) / 280_000_000.0;
        double stroke = elapsed >= 0 && elapsed < 1 ? Math.sin(Math.PI * elapsed) * 0.045 : 0;
        Vec3 approach = shoulder.subtract(target).multiply(1, 0, 1).normalize().scale(0.055);
        return target.add(approach).add(0, 0.07 - stroke, 0);
    }

    @Override
    public Vec3 heldItemPoint(Vec3 hand, Vec3 shoulder) {
        return hand;
    }

    @Override
    public Vec3 rest(boolean rightHand) {
        if (self()) {
            if (explicitlySelected && rightHand != worksLeft() && showArm(rightHand)) {
                Vec3 target = bodyPoint(selected);
                Vec3 from = shoulder(rightHand).subtract(target).multiply(1, 0, 1).normalize();
                return target.add(from.scale(0.12)).add(0, 0.16, 0);
            }
            return SelfSurgeryMotion.rest(
                    BedPose.feet(pos, heading()), along(), across(), rightHand);
        }
        prepareDoctorPose();
        Vec3 lateral = new Vec3(-doctorForward.z, 0, doctorForward.x);
        Vec3 target = explicitlySelected ? bodyPoint(selected) : center();
        return target.subtract(doctorForward.scale(0.22))
                .add(lateral.scale(rightHand ? 0.22 : -0.22))
                .add(0, 0.20, 0);
    }

    @Override
    public double maxReach() {
        return self() ? 1.15 : 1.35;
    }

    @Override
    protected Vec3 restPoint(int slot) {
        return center();
    }

    @Override
    public void clearCursor() {
        super.clearCursor();
        hovered = null;
    }

    @Override
    public void updateCursor(boolean overPanel) {
        clearCursor();
        if (overPanel || TreatmentPanel.containsPointer()) return;
        Vec3[] ray = Picking.cursorRay();
        if (ray == null) return;
        Vec3 end = ray[0].add(ray[1].scale(6));
        double nearest = Double.MAX_VALUE;
        Vec3 hitPoint = null;
        for (BodyComponents part : BodyComponents.VISIBLE_BODIES) {
            var box = region(part);
            if (box == null || box.contains(ray[0]) || (self() && part == BodyComponents.HEAD))
                continue;
            var hit = box.clip(ray[0], end);
            if (hit.isPresent() && hit.get().distanceToSqr(ray[0]) < nearest) {
                nearest = hit.get().distanceToSqr(ray[0]);
                hovered = part;
                hitPoint = hit.get();
            }
        }
        if (cameraSettled() && hovered == selected && (showArm(true) || showArm(false)))
            aim = hitPoint;
    }

    @Override
    protected void act(int button, boolean shift) {
        if (TreatmentPanel.containsPointer()) {
            int tool = TreatmentPanel.toolIndex();
            if (tool == BedToolPackets.AUTOPULSE && pulseLocked()) return;
            if (tool == BedToolPackets.BLOOD) bloodPage = 0;
            if (tool >= 0 && (button == 0 || button == 1)) useBedTool(tool, button == 1);
            else if (button == 0 && TreatmentPanel.overExit()) Scene.leave();
            else if (button == 0 && TreatmentPanel.overMode() >= 0) {
                organMode = TreatmentPanel.overMode() == 1;
                bindOrganMode();
            } else if (scannerActive(BedToolPackets.BLOOD)) {
                if (button == 0) bloodPage++;
            } else if (organSlotVisible(TreatmentPanel.organIndex())) {
                int slot = 47 + TreatmentPanel.organIndex();
                if (menu() != null && menu().getSlot(slot).isActive())
                    slotClick(slot, button, shift);
            }
            return;
        }
        if (Scene.overPanel) return;
        updateCursor(false);
        var bookMenu = menu();
        if (button == 0) {
            explicitlySelected = hovered != null;
            selected = hovered == null ? BodyComponents.HEAD : hovered;
            selectedAt = System.nanoTime();
            bindOrganMode();
            if (hovered != null && bookMenu != null)
                PacketDistributor.sendToServer(
                        new CaseBookPackets.Action(pos, bookMenu.containerId, selected.ordinal()));
            return;
        }
        var player = Minecraft.getInstance().player;
        var activeMenu = menu();
        if (!cameraSettled() || System.nanoTime() - selectedAt < 300_000_000L) return;
        if (button != 1
                || selected == null
                || hovered != selected
                || player == null
                || activeMenu == null) return;
        boolean carrying = !activeMenu.getCarried().isEmpty();
        if (!TreatmentArms.canOperate(player, self(), selected)) {
            player.displayClientMessage(
                    Component.translatable("dghcompat.treatment.no_hand"), true);
            return;
        }
        var held = carrying ? activeMenu.getCarried() : player.getMainHandItem();
        if (held.getItem() instanceof AbstractHealingItem || held.is(ModTags.MEDICAL_USAGE_BAGS)) {
            usedAt = System.nanoTime();
            PacketDistributor.sendToServer(
                    new BedPackets.Treat(activeMenu.containerId, selected.ordinal()));
        }
    }

    @Override
    public void renderBody(PoseStack pose, MultiBufferSource buffers, Vec3 camera, int light) {
        var client = Minecraft.getInstance();
        var player = client.player;
        if (player == null || !self() || Scene.phase != Scene.Phase.ACTIVE) return;
        float partialTick = client.getTimer().getGameTimeDeltaPartialTick(false);
        Vec3 offset = BedPose.feet(pos, heading()).subtract(camera);
        BedPose.drawingSelf = true;
        try {
            client.getEntityRenderDispatcher()
                    .render(
                            player,
                            offset.x,
                            offset.y,
                            offset.z,
                            heading().toYRot(),
                            partialTick,
                            pose,
                            buffers,
                            light);
        } finally {
            BedPose.drawingSelf = false;
        }
    }

    @Override
    public Component hint() {
        if (selected != null) {
            return Component.translatable("dghcompat.treatment.selected", PartNames.name(selected));
        }
        return hovered == null
                ? Component.translatable("dghcompat.treatment.hint")
                : Component.translatable("dghcompat.treatment.hovered", PartNames.name(hovered));
    }

    @Override
    public void renderScene(PoseStack pose, MultiBufferSource buffers, Camera camera, int light) {}

    @Override
    public void renderScreen(
            net.minecraft.client.gui.GuiGraphics graphics,
            net.minecraft.client.gui.Font font,
            float alpha) {
        TreatmentPanel.render(graphics, font, this, self());
    }

    @Override
    public void renderWorld(PoseStack pose, MultiBufferSource buffers, Vec3 camera, int light) {
        BodyComponents part = hovered == null ? selected : hovered;
        if (part == null
                || (!explicitlySelected && hovered == null)
                || (self() && part == BodyComponents.HEAD)) return;
        var box = region(part);
        if (box == null) return;
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        net.minecraft.client.renderer.LevelRenderer.renderLineBox(
                pose,
                buffers.getBuffer(net.minecraft.client.renderer.RenderType.lines()),
                box,
                0.25f,
                0.87f,
                0.82f,
                0.9f);
        pose.popPose();
    }
}
