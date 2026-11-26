package com.hanmoyun.dghcompat;

import com.hanmoyun.dghcompat.mixin.CaseDisplayAccess;
import com.hanmoyun.dghcompat.mixin.CaseInteractionAccess;
import com.hanmoyun.dghcompat.mixin.CaseItemDisplayAccess;
import com.lastimp.dgh.common.block.OperatingBedBlock;
import com.lastimp.dgh.common.capability.HealthCapability;
import com.lastimp.dgh.common.capability.bodyPart.ConditionAccessor;
import com.lastimp.dgh.common.entity.StretcherEntity;
import com.lastimp.dgh.common.enums.BodyComponents;
import com.lastimp.dgh.common.item.tool.HealthScanner;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Display.ItemDisplay;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Persist the actual writable book in a vanilla display entity, never in a transient map. */
@EventBusSubscriber(modid = CompatMod.ID)
public final class CaseBook {
    private static final String BED = "dghcompat:case_bed";
    private static final String LINK = "dghcompat:case_display";
    private static final String PATIENT = "dghcompat:case_patient";

    private CaseBook() {}

    public static Vec3 position(BlockPos head, net.minecraft.core.Direction facing) {
        // Patient looks toward the foot of the bed. Keep the book in front of that eye,
        // on the patient's left, so it remains in the self-treatment camera's field of view.
        var left = facing.getClockWise();
        return Vec3.atBottomCenterOf(head)
                .add(
                        left.getStepX() * .50 - facing.getStepX() * .55,
                        .84,
                        left.getStepZ() * .50 - facing.getStepZ() * .55);
    }

    private static List<ItemDisplay> books(Level level, BlockPos head) {
        return level.getEntitiesOfClass(
                ItemDisplay.class,
                new AABB(head).inflate(2),
                e ->
                        e.getPersistentData().contains(BED)
                                && e.getPersistentData().getLong(BED) == head.asLong());
    }

    private static ItemStack held(ServerPlayer player) {
        return player.containerMenu.getCarried().isEmpty()
                ? player.getMainHandItem()
                : player.containerMenu.getCarried();
    }

    public static ItemStack placedBook(Level level, BlockPos head) {
        for (var display : books(level, head))
            return ((CaseItemDisplayAccess) display).caseGetItem().copy();
        return ItemStack.EMPTY;
    }

    public static boolean place(ServerPlayer player, BlockPos head) {
        return place(player, head, null);
    }

    public static boolean place(ServerPlayer player, BlockPos head, LivingEntity patient) {
        var stack = held(player);
        if (!stack.is(Items.WRITABLE_BOOK)) return false;
        if (!player.canInteractWithBlock(head, 1)
                || !player.level().mayInteract(player, head)
                || !books(player.level(), head).isEmpty()) return true;
        var state = player.level().getBlockState(head);
        if (!(state.getBlock() instanceof OperatingBedBlock)) return true;
        var p = position(head, state.getValue(OperatingBedBlock.FACING));
        ItemDisplay display = new ItemDisplay(EntityType.ITEM_DISPLAY, player.level());
        display.setPos(p);
        var copy = stack.copyWithCount(1);
        if (patient != null) {
            var origin =
                    copy.getOrDefault(
                                    DataComponents.CUSTOM_DATA,
                                    net.minecraft.world.item.component.CustomData.EMPTY)
                            .copyTag();
            if (origin.hasUUID(PATIENT) && !origin.getUUID(PATIENT).equals(patient.getUUID())) {
                player.displayClientMessage(
                        Component.translatable("dghcompat.tools.book_wrong"), true);
                return true;
            }
            boolean precise =
                    BedToolPackets.enabled(player, BedToolPackets.HEALTH)
                            || player.getOffhandItem().getItem() instanceof HealthScanner
                            || player.getMainHandItem().getItem() instanceof HealthScanner;
            if (!append(copy, examination(patient, precise))) {
                player.displayClientMessage(
                        Component.translatable("dghcompat.tools.book_full"), true);
                return true;
            }
            origin.putUUID(PATIENT, patient.getUUID());
            copy.set(
                    DataComponents.CUSTOM_DATA,
                    net.minecraft.world.item.component.CustomData.of(origin));
        }
        ((CaseItemDisplayAccess) display).caseSetItem(copy);
        ((CaseItemDisplayAccess) display).caseSetContext(ItemDisplayContext.FIXED);
        ((CaseDisplayAccess) display)
                .caseSetTransform(
                        new com.mojang.math.Transformation(
                                new org.joml.Vector3f(),
                                new org.joml.Quaternionf().rotationX((float) -Math.PI / 2),
                                new org.joml.Vector3f(.45f),
                                new org.joml.Quaternionf()));
        display.getPersistentData().putLong(BED, head.asLong());
        var saved =
                copy.getOrDefault(
                                DataComponents.CUSTOM_DATA,
                                net.minecraft.world.item.component.CustomData.EMPTY)
                        .copyTag();
        if (saved.hasUUID(PATIENT))
            display.getPersistentData().putUUID(PATIENT, saved.getUUID(PATIENT));
        Interaction click = new Interaction(EntityType.INTERACTION, player.level());
        click.setPos(p.x, p.y - .08, p.z);
        ((CaseInteractionAccess) click).caseSetWidth(.48f);
        ((CaseInteractionAccess) click).caseSetHeight(.20f);
        click.getPersistentData().putUUID(LINK, display.getUUID());
        if (!player.serverLevel().addFreshEntity(display)) return true;
        if (!player.serverLevel().addFreshEntity(click)) {
            display.discard();
            return true;
        }
        stack.shrink(1);
        player.containerMenu.broadcastChanges();
        return true;
    }

    public static void take(ServerPlayer player, BlockPos head) {
        if (!player.canInteractWithBlock(head, 1) || !player.level().mayInteract(player, head))
            return;
        for (var display : books(player.level(), head)) {
            ItemStack stack = ((CaseItemDisplayAccess) display).caseGetItem().copy();
            display.discard();
            for (var click :
                    player.level().getEntitiesOfClass(Interaction.class, new AABB(head).inflate(2)))
                if (click.getPersistentData().hasUUID(LINK)
                        && click.getPersistentData().getUUID(LINK).equals(display.getUUID()))
                    click.discard();
            if (!player.getInventory().add(stack)) player.drop(stack, false);
        }
    }

    public static BlockPos bed(LivingEntity patient) {
        if (patient.getVehicle() instanceof StretcherEntity carrier)
            return PatientTransport.bed(carrier);
        if (patient instanceof Player p && BedPose.bed(p) != null) return BedPose.bed(p);
        return patient.getSleepingPos().orElse(null);
    }

    private static String stamp(Level level) {
        return CaseBookText.time(level.getDayTime());
    }

    private static boolean append(ItemStack book, String text) {
        var old =
                book.getOrDefault(DataComponents.WRITABLE_BOOK_CONTENT, WritableBookContent.EMPTY);
        var pages = new ArrayList<>(old.pages());
        // Keep existing writing intact; split into short readable pages without deleting old
        // entries.
        var extra = BookPages.addition(pages.size(), text);
        if (extra == null) return false;
        for (String page : extra) pages.add(Filterable.passThrough(page));
        book.set(DataComponents.WRITABLE_BOOK_CONTENT, new WritableBookContent(pages));
        return true;
    }

    private static String findings(LivingEntity patient, BodyComponents part, boolean precise) {
        return HealthCapability.getAndApply(
                patient,
                health -> {
                    var result = new StringBuilder();
                    var body = health.getComponent(part);
                    var keys =
                            precise
                                    ? HealthScanner.healthScannerConditions()
                                    : HealthScanner.eyesightConditions();
                    for (var key : keys) {
                        if (!body.getBodyConditions().contains(key)) continue;
                        float value = body.getCondition(key).getDisplayValue();
                        if (!ConditionAccessor.get(key).abnormal(value)) continue;
                        result.append(
                                CaseBookChinese.label(
                                        key.toString(),
                                        Component.translatable(key.toString()).getString()));
                        if (precise) result.append(String.format(Locale.ROOT, " %.2f", value));
                        result.append("；");
                    }
                    return result.isEmpty() ? "未发现可识别异常" : result.toString();
                },
                "无健康数据");
    }

    public static void record(
            ServerPlayer doctor, LivingEntity patient, BodyComponents part, String action) {
        if (!CompatMod.enabled(CompatMod.CHART)) return;
        BlockPos head = bed(patient);
        if (head == null) return;
        for (var display : books(patient.level(), head)) {
            var data = display.getPersistentData();
            if (data.hasUUID(PATIENT) && !data.getUUID(PATIENT).equals(patient.getUUID())) continue;
            ItemStack book = ((CaseItemDisplayAccess) display).caseGetItem().copy();
            String text =
                    stamp(patient.level())
                            + patientName(patient)
                            + "\n"
                            + doctor.getName().getString()
                            + "："
                            + action
                            + "\n"
                            + (part == null ? "全身" : CaseBookText.part(part.name()));
            if (action.startsWith("检查") && part != null) {
                boolean precise =
                        BedToolPackets.enabled(doctor, BedToolPackets.HEALTH)
                                || doctor.getOffhandItem().getItem() instanceof HealthScanner
                                || held(doctor).getItem() instanceof HealthScanner;
                text += precise ? "（分析仪）\n" : "（粗略）\n";
                text += findings(patient, part, precise);
            }
            if (action.equals("精准全身检查")) text += "\n" + examination(patient, true);
            if (append(book, text)) {
                data.putUUID(PATIENT, patient.getUUID());
                var saved =
                        book.getOrDefault(
                                        DataComponents.CUSTOM_DATA,
                                        net.minecraft.world.item.component.CustomData.EMPTY)
                                .copyTag();
                saved.putUUID(PATIENT, patient.getUUID());
                book.set(
                        DataComponents.CUSTOM_DATA,
                        net.minecraft.world.item.component.CustomData.of(saved));
                ((CaseItemDisplayAccess) display).caseSetItem(book);
            }
        }
    }

    static void scan(ServerPlayer doctor, LivingEntity patient) {
        record(doctor, patient, null, "精准全身检查");
    }

    private static String patientName(LivingEntity patient) {
        if (patient instanceof net.minecraft.world.entity.npc.Villager && !patient.hasCustomName())
            return "村民";
        return patient.getName().getString();
    }

    private static String examination(LivingEntity patient, boolean precise) {
        var text =
                new StringBuilder(stamp(patient.level()))
                        .append(patientName(patient))
                        .append(precise ? "\n全身检查（分析仪）\n" : "\n全身检查（粗略）\n");
        for (var part : BodyComponents.VISIBLE_BODIES)
            text.append(CaseBookText.part(part.name()))
                    .append("：")
                    .append(findings(patient, part, precise))
                    .append('\n');
        return text.toString();
    }

    /** Reading a wrapped patient is deliberately separate from the frozen healing guard. */
    public static boolean snapshot(PlayerInteractEvent event, LivingEntity patient) {
        var player = event.getEntity();
        if (!StasisPatients.wrapped(patient) || !player.getMainHandItem().is(Items.WRITABLE_BOOK))
            return false;
        if (event.getHand() != InteractionHand.MAIN_HAND
                || !(player instanceof ServerPlayer server)) return true;
        if (player.isSpectator() || !player.canInteractWithEntity(patient, 0) || !patient.isAlive())
            return true;
        boolean precise = player.getOffhandItem().getItem() instanceof HealthScanner;
        ItemStack book = player.getMainHandItem().copyWithCount(1);
        if (!append(book, examination(patient, precise))) return true;
        player.setItemInHand(InteractionHand.MAIN_HAND, book);
        server.containerMenu.broadcastChanges();
        return true;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void click(PlayerInteractEvent.EntityInteract event) {
        clickBook(event, event.getTarget());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void clickSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        clickBook(event, event.getTarget());
    }

    private static void clickBook(PlayerInteractEvent event, Entity target) {
        if (!(target instanceof Interaction) || !target.getPersistentData().hasUUID(LINK)) return;
        if (event instanceof PlayerInteractEvent.EntityInteract e) {
            e.setCanceled(true);
            e.setCancellationResult(InteractionResult.SUCCESS);
        }
        if (event instanceof PlayerInteractEvent.EntityInteractSpecific e) {
            e.setCanceled(true);
            e.setCancellationResult(InteractionResult.SUCCESS);
        }
        if (event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getEntity() instanceof ServerPlayer p)
                || p.isSpectator()) return;
        var entity = p.serverLevel().getEntity(target.getPersistentData().getUUID(LINK));
        if (entity instanceof ItemDisplay d && d.getPersistentData().contains(BED))
            take(p, BlockPos.of(d.getPersistentData().getLong(BED)));
    }

    @SubscribeEvent
    public static void cleanup(EntityTickEvent.Post event) {
        var entity = event.getEntity();
        if (entity.level().isClientSide() || entity.tickCount % 20 != 0) return;
        if (entity instanceof ItemDisplay display && entity.getPersistentData().contains(BED)) {
            var head = BlockPos.of(entity.getPersistentData().getLong(BED));
            if (entity.level().isLoaded(head)
                    && !(entity.level().getBlockState(head).getBlock()
                            instanceof OperatingBedBlock)) {
                display.spawnAtLocation(((CaseItemDisplayAccess) display).caseGetItem().copy());
                display.discard();
            }
        }
        if (entity instanceof Interaction
                && entity.getPersistentData().hasUUID(LINK)
                && ((net.minecraft.server.level.ServerLevel) entity.level())
                                .getEntity(entity.getPersistentData().getUUID(LINK))
                        == null) entity.discard();
    }
}
