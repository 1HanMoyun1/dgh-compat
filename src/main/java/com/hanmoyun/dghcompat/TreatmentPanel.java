package com.hanmoyun.dghcompat;

import com.lastimp.dgh.common.capability.bodyPart.ConditionAccessor;
import com.lastimp.dgh.common.item.tool.HealthScanner;

import dev.dyslexic.cinematic.client.Picking;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.Locale;

final class TreatmentPanel {
    private static final float SCALE = 0.65f;
    private static final int WIDTH = 166;
    private static final int HEIGHT = 222;
    private static final int TOOL_TOP = 164;
    private static final int TOOL_WIDTH = 28;
    private static final int TOOL_STEP = 30;
    private static final int EXIT_TOP = 198;

    private TreatmentPanel() {}

    private static int x() {
        return (int)
                Math.max(
                        4,
                        Minecraft.getInstance().getWindow().getGuiScaledWidth() / SCALE
                                - WIDTH
                                - 6);
    }

    private static int y() {
        return 50;
    }

    static boolean containsPointer() {
        double[] mouse = Picking.mouseGui();
        mouse[0] /= SCALE;
        mouse[1] /= SCALE;
        return mouse[0] >= x()
                && mouse[0] < x() + WIDTH
                && mouse[1] >= y()
                && mouse[1] < y() + HEIGHT;
    }

    private static final int GRID_X = 11;
    private static final int GRID_Y = 62;
    private static final int CELL = 12;

    static int overMode() {
        if (!containsPointer()) return -1;
        var mouse = Picking.mouseGui();
        double dy = mouse[1] / SCALE - y();
        if (dy < 27 || dy >= 45) return -1;
        return mouse[0] / SCALE < x() + WIDTH / 2 ? 0 : 1;
    }

    static int organIndex() {
        var mouse = Picking.mouseGui();
        double dx = mouse[0] / SCALE - x() - GRID_X;
        double dy = mouse[1] / SCALE - y() - GRID_Y;
        if (dx < 0 || dy < 0 || dx >= 12 * CELL || dy >= 3 * CELL) return -1;
        return (int) (dy / CELL) * 12 + (int) (dx / CELL);
    }

    static int toolIndex() {
        var mouse = Picking.mouseGui();
        var dx = mouse[0] / SCALE - x() - 8;
        var dy = mouse[1] / SCALE - y() - TOOL_TOP;
        if (dx < 0
                || dx >= 5 * TOOL_STEP
                || dy < 0
                || dy >= 28
                || (int) dx % TOOL_STEP >= TOOL_WIDTH) return -1;
        return (int) dx / TOOL_STEP;
    }

    static boolean overExit() {
        return containsPointer() && Picking.mouseGui()[1] / SCALE >= y() + EXIT_TOP;
    }

    static void render(GuiGraphics g, Font font, TreatmentScene scene, boolean self) {
        int x = x(), y = y();
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        var tool =
                player.containerMenu.getCarried().isEmpty()
                        ? player.getMainHandItem()
                        : player.containerMenu.getCarried();
        g.pose().pushPose();
        g.pose().scale(SCALE, SCALE, 1);
        boolean precise =
                scene.scannerActive(BedToolPackets.HEALTH)
                        || tool.getItem() instanceof HealthScanner
                        || player.getOffhandItem().getItem() instanceof HealthScanner;
        g.fill(x, y, x + WIDTH, y + 145, 0xDC101A23);
        g.fill(x, y, x + 2, y + 145, 0xFF45D7D0);
        g.drawString(
                font,
                Component.translatable(
                        precise
                                ? "dghcompat.panel.precise"
                                : self ? "dghcompat.panel.self" : "dghcompat.panel.other"),
                x + 8,
                y + 8,
                0x69E8DC,
                false);
        for (int i = 0; i < 2; i++) {
            int tabX = x + 8 + i * 76;
            boolean active = scene.organMode() == (i == 1);
            g.fill(tabX, y + 27, tabX + 73, y + 45, active ? 0xFF356970 : 0xFF233641);
            g.drawString(
                    font,
                    Component.translatable(i == 0 ? "dghcompat.mode.limb" : "dghcompat.mode.organ"),
                    tabX + 5,
                    y + 32,
                    0xE1FFFA,
                    false);
        }
        var stats = scene.cyberData();
        if (scene.cyberManaged() && (stats.contains("integrity") || stats.contains("humanity"))) {
            var metric =
                    stats.contains("integrity")
                            ? Component.translatable(
                                    "dghcompat.cyber.integrity", stats.getInt("integrity"))
                            : Component.translatable(
                                    "dghcompat.cyber.humanity", stats.getInt("humanity"));
            g.drawString(font, metric, x + 8, y + 17, 0xC9B3FA, false);
        }
        var part = scene.selectedPart();
        var health = scene.patientHealth();
        boolean blood = scene.scannerActive(BedToolPackets.BLOOD);
        if (blood && health != null) {
            var sample = health.getComponent(com.lastimp.dgh.common.enums.BodyComponents.BLOOD);
            var lines = new java.util.ArrayList<Component>();
            for (var key : ConditionAccessor.bloodConditions()) {
                if (sample.abnormal(key)) lines.add(Component.translatable(key.toString()).append(
                        String.format(Locale.ROOT, " %.2f", sample.getConditionValue(key))));
            }
            if (lines.isEmpty()) lines.add(Component.translatable("dghcompat.tools.blood_normal"));
            int pages = (lines.size() + 5) / 6;
            int page = Math.floorMod(scene.bloodPage, pages);
            g.drawString(font, Component.translatable("dghcompat.tools.blood_title", page + 1, pages),
                    x + 8, y + 49, 0x9DDEE1, false);
            for (int i = 0; i < 6 && page * 6 + i < lines.size(); i++)
                g.drawString(font, lines.get(page * 6 + i), x + 8, y + 65 + 12*i, 0xD2DEE5, false);
        } else if (part == null || health == null) {
            g.drawWordWrap(
                    font,
                    Component.translatable(
                            part == null ? "dghcompat.treatment.hint" : "dghcompat.panel.wait"),
                    x + 8,
                    y + 52,
                    WIDTH - 16,
                    0xD2DEE5);
        } else if (scene.organMode()) {
            g.drawString(font, PartNames.name(part), x + 8, y + 49, 0xFFFFFF, false);
            g.drawString(
                    font,
                    Component.translatable(
                            "dghcompat.mode.capacity", scene.organUsed(), scene.organCapacity()),
                    x + 8,
                    y + 108,
                    0x9DDEE1,
                    false);
            if (!scene.organsActive()) {
                g.drawWordWrap(
                        font,
                        Component.translatable("dghcompat.mode.incise"),
                        x + 8,
                        y + GRID_Y,
                        WIDTH - 16,
                        0xD2DEE5);
            } else {
                for (int i = 0; i < 36; i++) {
                    if (!scene.organSlotVisible(i)) continue;
                    int sx = x + GRID_X + i % 12 * CELL;
                    int sy = y + GRID_Y + i / 12 * CELL;
                    g.fill(
                            sx,
                            sy,
                            sx + CELL - 1,
                            sy + CELL - 1,
                            organIndex() == i ? 0xFF356970 : 0xFF233641);
                    var item = scene.organStack(i);
                    if (!item.isEmpty()) {
                        g.pose().pushPose();
                        g.pose().translate(sx, sy, 0);
                        g.pose().scale(0.7f, 0.7f, 1);
                        g.renderItem(item, 0, 0);
                        g.renderItemDecorations(font, item, 0, 0);
                        g.pose().popPose();
                    }
                }
            }
        } else {
            g.drawString(font, PartNames.name(part), x + 8, y + 49, 0xFFFFFF, false);
            var body = health.getComponent(part);
            var conditions =
                    precise
                            ? HealthScanner.healthScannerConditions()
                            : HealthScanner.eyesightConditions();
            int lineY = y + 64;
            for (var key : conditions) {
                if (!body.getBodyConditions().contains(key)) continue;
                float value = body.getCondition(key).getDisplayValue();
                if (!ConditionAccessor.get(key).abnormal(value)) continue;
                Component label = Component.translatable(key.toString());
                if (precise)
                    label = label.copy().append(String.format(Locale.ROOT, " %.2f", value));
                for (var line : font.split(label, WIDTH - 16)) {
                    if (lineY > y + 134) break;
                    g.drawString(font, line, x + 8, lineY, 0xD2DEE5, false);
                    lineY += 11;
                }
                if (lineY > y + 134) break;
            }
            if (lineY == y + 64)
                g.drawString(
                        font,
                        Component.translatable("dghcompat.panel.normal"),
                        x + 8,
                        lineY,
                        0xD2DEE5,
                        false);
        }
        renderTools(g, font, scene, x, y);
        g.fill(x, y + EXIT_TOP, x + WIDTH, y + HEIGHT, overExit() ? 0xEF356970 : 0xE0254B51);
        g.drawString(
                font,
                Component.translatable(self ? "dghcompat.panel.rise" : "dghcompat.panel.finish"),
                x + 8,
                y + EXIT_TOP + 8,
                0xE1FFFA,
                false);
        int hovered = !blood && scene.organMode() && scene.organsActive() ? organIndex() : -1;
        var hoveredStack =
                hovered < 0 ? net.minecraft.world.item.ItemStack.EMPTY : scene.organStack(hovered);
        g.pose().popPose();
        int hoveredTool = toolIndex();
        if (hoveredTool >= 0) {
            var mouse = Picking.mouseGui();
            g.renderTooltip(
                    font,
                    Component.translatable("dghcompat.tools.full_" + hoveredTool),
                    (int) mouse[0],
                    (int) mouse[1]);
        }
        if (hoveredTool < 0 && !hoveredStack.isEmpty()) {
            var mouse = Picking.mouseGui();
            g.renderTooltip(font, hoveredStack, (int) mouse[0], (int) mouse[1]);
        }
        g.drawString(
                font,
                tool.getHoverName(),
                12,
                Minecraft.getInstance().getWindow().getGuiScaledHeight() - 42,
                0xD0E8E5,
                false);
    }

    private static void renderTools(GuiGraphics g, Font font, TreatmentScene scene, int x, int y) {
        g.fill(x, y + 148, x + WIDTH, y + 194, 0xDC101A23);
        g.drawString(
                font,
                Component.translatable("dghcompat.tools.title"),
                x + 8,
                y + 152,
                0x9DDEE1,
                false);
        int hover = toolIndex();
        for (int i = 0; i < 5; i++) {
            int sx = x + 8 + i * TOOL_STEP, sy = y + TOOL_TOP;
            var item = scene.toolStack(i);
            boolean active =
                    i == BedToolPackets.BOOK
                            ? scene.bookPlaced()
                            : i >= BedToolPackets.HEALTH ? scene.scannerActive(i) : !item.isEmpty();
            g.fill(
                    sx,
                    sy,
                    sx + TOOL_WIDTH,
                    sy + 28,
                    i == BedToolPackets.AUTOPULSE && scene.pulseLocked() ? 0xFF171B1E : hover == i ? 0xFF356970 : active ? 0xFF285451 : 0xFF233641);
            if (item.isEmpty())
                item =
                        new net.minecraft.world.item.ItemStack(
                                i == BedToolPackets.OXYGEN
                                        ? com.lastimp.dgh.common.entry.register.ModItems.OXYGEN_MASK
                                                .get()
                                        : com.lastimp.dgh.common.entry.register.ModItems.AUTOPULSE
                                                .get());
            g.renderItem(item, sx + (TOOL_WIDTH - 16) / 2, sy);
            g.drawCenteredString(
                    font,
                    Component.translatable("dghcompat.tools.short_" + i),
                    sx + TOOL_WIDTH / 2,
                    sy + 17,
                    0xE1FFFA);
        }
    }
}
