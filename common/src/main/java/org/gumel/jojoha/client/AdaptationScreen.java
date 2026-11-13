package org.gumel.jojoha.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import org.gumel.jojoha.data.ClientPlayerDataCache;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.ultimate.Adaptation;
import org.gumel.jojoha.ultimate.AdaptationLibrary;
import org.gumel.jojoha.ultimate.UltimateStrengthMove;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class AdaptationScreen extends Screen {
    private static final int PANEL_W = 196;
    private static final int PANEL_H = 176;
    private static final int CELL = 20;
    private static final int COLUMNS = 9;
    private static final int GRID_X = 8;
    private static final int GRID_Y = 76;
    private static final int SLOT_Y = 36;
    private static final int ACTIVE_X = 8;
    private static final int PASSIVE_X = 104;

    private static final int TEXT = 0xFFFFFF;
    private static final int MUTED = 0xA8A8A8;
    private static final int SLOT_EDGE = 0xFF373737;
    private static final int SLOT_FILL = 0xFF8B8B8B;
    private static final int SLOT_LOCKED = 0xFF4A4A4A;
    private static final int UNKNOWN_TINT = 0xC0101010;
    private static final int ACTIVE_MARK = 0xFFE0E0E0;
    private static final int PASSIVE_MARK = 0xFF7FA7D8;
    private static final int PIP_ON = 0xFFFFFFFF;
    private static final int PIP_OFF = 0xFF555555;
    private static final int BAR_BACK = 0xFF2A2A2A;
    private static final int BAR_FILL = 0xFFD8D8D8;

    private int left;
    private int top;
    private Component notice;
    private long noticeAt;

    public AdaptationScreen() {
        super(Component.translatable("screen.jojoha.adaptations"));
    }

    @Override
    protected void init() {
        left = (width - PANEL_W) / 2;
        top = (height - PANEL_H) / 2;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static JojohaPlayerData data() {
        return ClientPlayerDataCache.data;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        JojohaPlayerData data = data();
        if (data == null) {
            return;
        }
        JojohaTooltip.nineSlice(graphics, JojohaTooltip.TOOLTIP, left, top, PANEL_W, PANEL_H, JojohaTooltip.BORDER);
        JojohaTooltip.corners(graphics, left, top, PANEL_W, PANEL_H);

        int rank = AdaptationLibrary.rank(data);
        int score = AdaptationLibrary.score(data);
        graphics.drawString(font, title, left + 8, top + 7, TEXT, true);
        Component rankText = Component.translatable("screen.jojoha.adaptations.rank", rank, AdaptationLibrary.TOP_RANK);
        graphics.drawString(font, rankText, left + PANEL_W - 8 - font.width(rankText), top + 7, TEXT, true);
        bar(graphics, data, rank, score);

        graphics.drawString(font, Component.translatable("screen.jojoha.adaptations.actives"), left + ACTIVE_X,
                top + SLOT_Y - 10, MUTED, false);
        graphics.drawString(font, Component.translatable("screen.jojoha.adaptations.passives"), left + PASSIVE_X,
                top + SLOT_Y - 10, MUTED, false);

        List<Component> tip = null;
        for (int slot = 0; slot < AdaptationLibrary.MAX_SLOTS; slot++) {
            List<Component> hovered = slot(graphics, data, true, slot, mouseX, mouseY);
            tip = hovered != null ? hovered : tip;
            hovered = slot(graphics, data, false, slot, mouseX, mouseY);
            tip = hovered != null ? hovered : tip;
        }

        graphics.drawString(font, Component.translatable("screen.jojoha.adaptations.library",
                known(data), Adaptation.values().length), left + GRID_X, top + GRID_Y - 10, MUTED, false);
        Adaptation[] all = Adaptation.values();
        for (int i = 0; i < all.length; i++) {
            int x = left + GRID_X + (i % COLUMNS) * CELL;
            int y = top + GRID_Y + (i / COLUMNS) * CELL;
            List<Component> hovered = cell(graphics, data, all[i], x, y, mouseX, mouseY);
            tip = hovered != null ? hovered : tip;
        }

        graphics.drawString(font, perks(data), left + 8, top + PANEL_H - 14, MUTED, false);

        if (notice != null && System.currentTimeMillis() - noticeAt < 2500L) {
            graphics.drawCenteredString(font, notice, left + PANEL_W / 2, top + PANEL_H + 6, TEXT);
        }
        if (tip != null) {
            JojohaTooltip.drawCompact(graphics, font, tip, mouseX, mouseY, width, height, 1, 1F);
        }
    }

    private static int known(JojohaPlayerData data) {
        int known = 0;
        for (Adaptation adaptation : Adaptation.values()) {
            if (AdaptationLibrary.knows(data, adaptation)) {
                known++;
            }
        }
        return known;
    }

    private void bar(GuiGraphics graphics, JojohaPlayerData data, int rank, int score) {
        int x = left + 8;
        int y = top + 18;
        int w = PANEL_W - 16;
        graphics.fill(x, y, x + w, y + 3, BAR_BACK);
        float filled = 1F;
        if (rank < AdaptationLibrary.TOP_RANK) {
            int from = AdaptationLibrary.rankAt(rank);
            int to = AdaptationLibrary.rankAt(rank + 1);
            filled = (score - from) / (float) Math.max(1, to - from);
        }
        graphics.fill(x, y, x + Math.round(w * filled), y + 3, BAR_FILL);
    }

    private List<Component> slot(GuiGraphics graphics, JojohaPlayerData data, boolean active, int slot,
                                 int mouseX, int mouseY) {
        int x = left + (active ? ACTIVE_X : PASSIVE_X) + slot * CELL;
        int y = top + SLOT_Y;
        int open = active ? AdaptationLibrary.activeSlots(data) : AdaptationLibrary.passiveSlots(data);
        boolean locked = slot >= open;
        graphics.fill(x, y, x + 18, y + 18, SLOT_EDGE);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, locked ? SLOT_LOCKED : SLOT_FILL);
        List<String> equipped = active ? data.adaptActives : data.adaptPassives;
        Adaptation adaptation = !locked && slot < equipped.size() ? Adaptation.named(equipped.get(slot)) : null;
        if (adaptation != null) {
            graphics.renderItem(icon(data, adaptation), x + 1, y + 1);
            graphics.fill(x + 1, y + 16, x + 17, y + 17, active ? ACTIVE_MARK : PASSIVE_MARK);
        } else if (locked) {
            graphics.drawCenteredString(font, "-", x + 9, y + 5, MUTED);
        }
        if (!over(mouseX, mouseY, x, y)) {
            return null;
        }
        graphics.fill(x + 1, y + 1, x + 17, y + 17, 0x40FFFFFF);
        List<Component> tip = new ArrayList<>();
        if (locked) {
            int needed = active ? AdaptationLibrary.activeSlotRank(slot) : AdaptationLibrary.passiveSlotRank(slot);
            tip.add(Component.translatable(active ? "screen.jojoha.adaptations.active_slot"
                    : "screen.jojoha.adaptations.passive_slot"));
            tip.add(Component.translatable("screen.jojoha.adaptations.unlocks", needed).withStyle(ChatFormatting.GRAY));
        } else if (adaptation == null) {
            tip.add(Component.translatable(active ? "screen.jojoha.adaptations.active_slot"
                    : "screen.jojoha.adaptations.passive_slot"));
            tip.add(Component.translatable(active ? "screen.jojoha.adaptations.active_empty"
                    : "screen.jojoha.adaptations.passive_empty").withStyle(ChatFormatting.GRAY));
        } else {
            describe(tip, data, adaptation);
            tip.add(Component.translatable("screen.jojoha.adaptations.unequip").withStyle(ChatFormatting.GRAY));
        }
        return tip;
    }

    private List<Component> cell(GuiGraphics graphics, JojohaPlayerData data, Adaptation adaptation, int x, int y,
                                 int mouseX, int mouseY) {
        boolean known = AdaptationLibrary.knows(data, adaptation);
        graphics.fill(x, y, x + 18, y + 18, SLOT_EDGE);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, SLOT_FILL);
        graphics.renderItem(icon(data, adaptation), x + 1, y + 1);
        if (!known) {
            graphics.pose().pushPose();
            graphics.pose().translate(0F, 0F, 200F);
            graphics.fill(x + 1, y + 1, x + 17, y + 17, UNKNOWN_TINT);
            graphics.pose().popPose();
        } else {
            graphics.pose().pushPose();
            graphics.pose().translate(0F, 0F, 200F);
            int mastery = AdaptationLibrary.mastery(data, adaptation);
            for (int pip = 0; pip < AdaptationLibrary.MASTERY_MAX; pip++) {
                int px = x + 2 + pip * 3;
                graphics.fill(px, y + 15, px + 2, y + 17, pip < mastery ? PIP_ON : PIP_OFF);
            }
            if (AdaptationLibrary.equippedActive(data, adaptation)) {
                graphics.fill(x + 13, y + 1, x + 17, y + 3, ACTIVE_MARK);
            }
            if (AdaptationLibrary.equippedPassive(data, adaptation)) {
                graphics.fill(x + 13, y + 4, x + 17, y + 6, PASSIVE_MARK);
            }
            graphics.pose().popPose();
        }
        if (!over(mouseX, mouseY, x, y)) {
            return null;
        }
        graphics.pose().pushPose();
        graphics.pose().translate(0F, 0F, 200F);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, 0x40FFFFFF);
        graphics.pose().popPose();
        List<Component> tip = new ArrayList<>();
        if (!known) {
            tip.add(Component.translatable("screen.jojoha.adaptations.unknown"));
            tip.add(Component.translatable("screen.jojoha.adaptations.unknown_hint").withStyle(ChatFormatting.GRAY));
            return tip;
        }
        describe(tip, data, adaptation);
        tip.add(Component.translatable("screen.jojoha.adaptations.equip_hint").withStyle(ChatFormatting.GRAY));
        return tip;
    }

    private static void describe(List<Component> tip, JojohaPlayerData data, Adaptation adaptation) {
        tip.add(Component.translatable(adaptation.translationKey()));
        EntityType<?> face = AdaptationLibrary.source(data, adaptation);
        if (face != null) {
            tip.add(Component.translatable("screen.jojoha.adaptations.from", face.getDescription())
                    .withStyle(ChatFormatting.GRAY));
        }
        tip.add(Component.translatable("screen.jojoha.adaptations.mastery",
                AdaptationLibrary.mastery(data, adaptation), AdaptationLibrary.MASTERY_MAX).withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable("screen.jojoha.adaptations.cooldown",
                String.format(Locale.ROOT, "%.1f", AdaptationLibrary.cooldownTicks(data, adaptation) / 20F))
                .withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable("screen.jojoha.adaptations.passive_line",
                Component.translatable(adaptation.passiveKey())).withStyle(ChatFormatting.GRAY));
    }

    private static ItemStack icon(JojohaPlayerData data, Adaptation adaptation) {
        EntityType<?> face = AdaptationLibrary.source(data, adaptation);
        SpawnEggItem egg = face == null ? null : SpawnEggItem.byId(face);
        return new ItemStack(egg == null ? Items.EGG : egg);
    }

    private static boolean over(int mouseX, int mouseY, int x, int y) {
        return mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        JojohaPlayerData data = data();
        if (data == null || (button != 0 && button != 1)) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        int mx = (int) mouseX;
        int my = (int) mouseY;
        for (int slot = 0; slot < AdaptationLibrary.MAX_SLOTS; slot++) {
            for (boolean active : new boolean[]{true, false}) {
                int x = left + (active ? ACTIVE_X : PASSIVE_X) + slot * CELL;
                List<String> equipped = active ? data.adaptActives : data.adaptPassives;
                if (over(mx, my, x, top + SLOT_Y) && slot < equipped.size()) {
                    NetworkHandler.sendEquipAdaptation(equipped.get(slot), active, -1);
                    return true;
                }
            }
        }
        Adaptation[] all = Adaptation.values();
        for (int i = 0; i < all.length; i++) {
            int x = left + GRID_X + (i % COLUMNS) * CELL;
            int y = top + GRID_Y + (i / COLUMNS) * CELL;
            if (!over(mx, my, x, y)) {
                continue;
            }
            Adaptation adaptation = all[i];
            if (!AdaptationLibrary.knows(data, adaptation)) {
                return true;
            }
            boolean active = button == 0;
            boolean worn = active ? AdaptationLibrary.equippedActive(data, adaptation)
                    : AdaptationLibrary.equippedPassive(data, adaptation);
            if (worn) {
                NetworkHandler.sendEquipAdaptation(adaptation.key(), active, -1);
                return true;
            }
            int open = active ? AdaptationLibrary.activeSlots(data) : AdaptationLibrary.passiveSlots(data);
            int used = Math.min(open, (active ? data.adaptActives : data.adaptPassives).size());
            if (used >= open) {
                notice = Component.translatable("screen.jojoha.adaptations.full");
                noticeAt = System.currentTimeMillis();
                return true;
            }
            NetworkHandler.sendEquipAdaptation(adaptation.key(), active, used);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    public static Component perks(JojohaPlayerData data) {
        int rank = AdaptationLibrary.rank(data);
        return Component.translatable("screen.jojoha.adaptations.perks", rank * 2, rank,
                String.format(Locale.ROOT, "%.1f", rank * 0.5F), roman(UltimateStrengthMove.levelFor(data)));
    }

    private static String roman(int level) {
        return switch (level) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> String.valueOf(level);
        };
    }
}
