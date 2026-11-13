package org.gumel.jojoha.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.data.ClientPlayerDataCache;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.WorkshopLook;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.stand.StandType;
import org.gumel.jojoha.stand.workshop.BarrageShout;
import org.gumel.jojoha.stand.workshop.StancePreset;
import org.gumel.jojoha.stand.workshop.SummonStyle;
import org.gumel.jojoha.stand.workshop.Unlocks;
import org.gumel.jojoha.stand.workshop.Workshop;
import java.util.ArrayList;
import java.util.List;

final class MenuStyle {
    private final PlayerMenuScreen screen;

    MenuStyle(PlayerMenuScreen screen) {
        this.screen = screen;
    }

    static void style(JojohaPlayerData data, int summon, int shout, int rest) {
        data.summonStyle = summon;
        data.barrageShout = shout;
        data.restPose = rest;
        NetworkHandler.sendWorkshopStyle(summon, shout, rest);
    }

    void renderStyle(GuiGraphics guiGraphics, JojohaPlayerData data) {
        if (PlayerMenuScreen.poseSlot >= 0) {
            screen.renderIdles(guiGraphics);
            return;
        }
        screen.smallRaw(guiGraphics, Component.translatable("menu.jojoha.workshop.summon").getString(),
                STYLE_LEFT_X, STYLE_TITLE_Y, PlayerMenuScreen.COLOR_ACCENT);
        screen.smallRaw(guiGraphics, Component.translatable("menu.jojoha.workshop.shout").getString(),
                STYLE_RIGHT_X, STYLE_TITLE_Y, PlayerMenuScreen.COLOR_ACCENT);
        screen.smallRaw(guiGraphics, Component.translatable("menu.jojoha.custom.idle").getString(),
                PlayerMenuScreen.IDLE_LIST_X, STYLE_TITLE_Y, PlayerMenuScreen.COLOR_ACCENT);
        for (SummonStyle style : SummonStyle.values()) {
            boolean open = Unlocks.style(data, style);
            String key = "menu.jojoha.workshop.summon." + style.key();
            Component locked = Component.translatable("menu.jojoha.workshop.summon.locked");
            styleRow(guiGraphics, STYLE_LEFT_X, style.ordinal(), STYLE_W, key, data.summonStyle == style.ordinal(), open,
                    locked);
        }
        for (BarrageShout shout : BarrageShout.values()) {
            boolean open = Unlocks.shout(data, shout);
            String key = "menu.jojoha.workshop.shout." + shout.key();
            styleRow(guiGraphics, STYLE_RIGHT_X, shout.ordinal(), STYLE_W, key,
                    data.barrageShout == shout.ordinal(), open, shoutLocked(shout));
        }
        screen.leftWrapped(guiGraphics, Component.translatable("menu.jojoha.workshop.style.note").getString(),
                STYLE_LEFT_X, STYLE_ROW_Y + SummonStyle.values().length * STYLE_ROW_H + 6, STYLE_W, PlayerMenuScreen.COLOR_DIM);
        screen.renderIdles(guiGraphics);
    }

    static Component shoutSource(BarrageShout shout) {
        if (shout == BarrageShout.OVER_HEAVEN) {
            return Component.translatable(Workshop.OVER_HEAVEN);
        }
        List<ResourceLocation> voices = shout.voices();
        return voices.size() > 1
                ? Component.translatable("menu.jojoha.workshop.pair", standName(voices.get(0)), standName(voices.get(1)))
                : standName(voices.get(0));
    }

    private static Component shoutLocked(BarrageShout shout) {
        if (shout.voices().isEmpty()) {
            return Component.empty();
        }
        return shout == BarrageShout.OVER_HEAVEN
                ? Component.translatable("menu.jojoha.workshop.shout.over_heaven.locked")
                : Component.translatable("menu.jojoha.workshop.shout.locked", shoutSource(shout));
    }

    private void styleRow(GuiGraphics guiGraphics, int x, int index, int w, String key, boolean chosen, boolean open,
                          Component locked) {
        int y = STYLE_ROW_Y + index * STYLE_ROW_H;
        boolean over = screen.within(x, y, w, STYLE_ROW_H);
        if (chosen || over && open) {
            guiGraphics.fill(x, y, x + w, y + STYLE_ROW_H, chosen ? PlayerMenuScreen.COLOR_ROW_PICKED : PlayerMenuScreen.COLOR_ROW_OVER);
        }
        screen.smallLeft(guiGraphics, Component.translatable(key).getString(), x + 4, y + (STYLE_ROW_H - screen.lineStep()) / 2 + 1,
                w - 8, !open ? COLOR_STYLE_LOCKED : chosen ? PlayerMenuScreen.COLOR_ACCENT
                        : over ? PlayerMenuScreen.COLOR_HOVER : PlayerMenuScreen.COLOR_TEXT);
        if (over) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable(key).withStyle(style -> style.withColor(PlayerMenuScreen.COLOR_ACCENT)));
            lines.add(Component.translatable(key + ".hint").withStyle(style -> style.withColor(PlayerMenuScreen.COLOR_DIM)));
            if (!open) {
                lines.add(locked.copy().withStyle(style -> style.withColor(PlayerMenuScreen.COLOR_DIM)));
            }
            screen.tooltip = List.copyOf(lines);
        }
    }

    boolean clickedStyle(int button) {
        if (PlayerMenuScreen.poseSlot >= 0) {
            return screen.clickedIdles(button);
        }
        if (button != 0) {
            return false;
        }
        JojohaPlayerData data = ClientPlayerDataCache.data;
        for (SummonStyle style : SummonStyle.values()) {
            if (screen.within(STYLE_LEFT_X, STYLE_ROW_Y + style.ordinal() * STYLE_ROW_H, STYLE_W, STYLE_ROW_H)) {
                if (Unlocks.style(data, style) && data.summonStyle != style.ordinal()) {
                    style(data, style.ordinal(), data.barrageShout, data.restPose);
                    PlayerMenuScreen.click();
                }
                return true;
            }
        }
        for (BarrageShout shout : BarrageShout.values()) {
            if (screen.within(STYLE_RIGHT_X, STYLE_ROW_Y + shout.ordinal() * STYLE_ROW_H, STYLE_W, STYLE_ROW_H)) {
                if (Unlocks.shout(data, shout) && data.barrageShout != shout.ordinal()) {
                    style(data, data.summonStyle, shout.ordinal(), data.restPose);
                    PlayerMenuScreen.click();
                }
                return true;
            }
        }
        return screen.clickedIdles(button);
    }

    static Component standName(ResourceLocation id) {
        return Component.translatable("stand." + id.getNamespace() + "." + id.getPath());
    }

    void renderLooks(GuiGraphics guiGraphics, JojohaPlayerData data) {
        StandType type = PlayerMenuScreen.standType(data);
        for (int slot = 0; slot < data.looks.length; slot++) {
            WorkshopLook look = data.looks[slot];
            int x = LOOK_X + slot * LOOK_PITCH;
            screen.smallRaw(guiGraphics, String.valueOf(slot + 1), x, LOOK_Y + (PlayerMenuScreen.REST_H - screen.lineStep()) / 2 + 1,
                    look.saved() ? PlayerMenuScreen.COLOR_ACCENT : PlayerMenuScreen.COLOR_DIM);
            screen.button(guiGraphics, x + LOOK_LABEL_W, LOOK_Y, LOOK_BUTTON_W, PlayerMenuScreen.REST_H,
                    Component.translatable("menu.jojoha.workshop.looks.save"), type != null);
            screen.button(guiGraphics, x + LOOK_LABEL_W + LOOK_BUTTON_W + 1, LOOK_Y, LOOK_BUTTON_W, PlayerMenuScreen.REST_H,
                    Component.translatable("menu.jojoha.workshop.looks.wear"), look.saved() && type != null);
            boolean same = look.saved() && type != null && look.stand().get().equals(type.id());
            if (screen.within(x, LOOK_Y, LOOK_LABEL_W, PlayerMenuScreen.REST_H)) {
                screen.tooltip = lookTooltip(look, type, same, slot);
            } else if (screen.within(x + LOOK_LABEL_W, LOOK_Y, LOOK_BUTTON_W, PlayerMenuScreen.REST_H)) {
                screen.tooltip = List.of(Component.translatable("menu.jojoha.workshop.looks.save.hint")
                        .withStyle(style -> style.withColor(PlayerMenuScreen.COLOR_DIM)));
            } else if (screen.within(x + LOOK_LABEL_W + LOOK_BUTTON_W + 1, LOOK_Y, LOOK_BUTTON_W, PlayerMenuScreen.REST_H)) {
                screen.tooltip = look.saved() ? lookTooltip(look, type, same, slot)
                        : List.of(Component.translatable("menu.jojoha.workshop.looks.empty")
                                .withStyle(style -> style.withColor(PlayerMenuScreen.COLOR_DIM)));
            }
        }
    }

    private static List<Component> lookTooltip(WorkshopLook look, StandType type, boolean same, int slot) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("menu.jojoha.workshop.looks.slot", slot + 1)
                .withStyle(style -> style.withColor(PlayerMenuScreen.COLOR_ACCENT)));
        if (!look.saved()) {
            lines.add(Component.translatable("menu.jojoha.workshop.looks.empty")
                    .withStyle(style -> style.withColor(PlayerMenuScreen.COLOR_DIM)));
            return List.copyOf(lines);
        }
        lines.add(same ? type.skinName(look.skin())
                : Component.translatable("menu.jojoha.workshop.looks.other", standName(look.stand().get())));
        StancePreset preset = null;
        for (StancePreset candidate : PlayerMenuScreen.PRESETS) {
            if (candidate.placement().equals(look.placement()) && candidate.rest().ordinal() == look.rest()) {
                preset = candidate;
            }
        }
        lines.add(Component.translatable(preset == null ? "menu.jojoha.workshop.stance.custom"
                : "menu.jojoha.workshop.stance." + preset.key()).withStyle(style -> style.withColor(PlayerMenuScreen.COLOR_DIM)));
        lines.add(Component.translatable("menu.jojoha.workshop.summon." + SummonStyle.of(look.summon()).key())
                .withStyle(style -> style.withColor(PlayerMenuScreen.COLOR_DIM)));
        lines.add(Component.translatable("menu.jojoha.workshop.shout." + BarrageShout.of(look.shout()).key())
                .withStyle(style -> style.withColor(PlayerMenuScreen.COLOR_DIM)));
        lines.add(Component.literal(PlayerMenuScreen.idleLabel(look.idle()))
                .withStyle(style -> style.withColor(PlayerMenuScreen.COLOR_DIM)));
        if (!same) {
            lines.add(Component.translatable("menu.jojoha.workshop.looks.wear.other")
                    .withStyle(style -> style.withColor(PlayerMenuScreen.COLOR_DIM)));
        }
        return List.copyOf(lines);
    }

    boolean clickedLooks(int button) {
        JojohaPlayerData data = ClientPlayerDataCache.data;
        if (button != 0 || PlayerMenuScreen.standType(data) == null) {
            return false;
        }
        for (int slot = 0; slot < data.looks.length; slot++) {
            int x = LOOK_X + slot * LOOK_PITCH;
            if (screen.within(x + LOOK_LABEL_W, LOOK_Y, LOOK_BUTTON_W, PlayerMenuScreen.REST_H)) {
                NetworkHandler.sendWorkshopLook(slot, true, MenuSkins.skinToSave(data, PlayerMenuScreen.standType(data)));
                PlayerMenuScreen.click();
                return true;
            }
            if (data.looks[slot].saved() && screen.within(x + LOOK_LABEL_W + LOOK_BUTTON_W + 1, LOOK_Y, LOOK_BUTTON_W, PlayerMenuScreen.REST_H)) {
                NetworkHandler.sendWorkshopLook(slot, false, 0);
                if (data.looks[slot].stand().get().equals(data.stand.standId())) {
                    MenuSkins.showSkin(data.looks[slot].skin());
                }
                if (!data.looks[slot].idle().equals(StandIdles.chosen())) {
                    StandIdles.choose(data.looks[slot].idle());
                }
                PlayerMenuScreen.click();
                return true;
            }
        }
        return false;
    }

    private static final int COLOR_STYLE_LOCKED = 0xFF606068;

    private static final int STYLE_LEFT_X = PlayerMenuScreen.SKIN_BG_X + 8, STYLE_RIGHT_X = PlayerMenuScreen.SKIN_BG_X + 88, STYLE_W = 76;

    private static final int STYLE_TITLE_Y = PlayerMenuScreen.SKIN_BG_Y + 7, STYLE_ROW_Y = PlayerMenuScreen.SKIN_BG_Y + 18, STYLE_ROW_H = 12;

    private static final int LOOK_X = PlayerMenuScreen.CARD_X, LOOK_Y = PlayerMenuScreen.CARD_Y + PlayerMenuScreen.CARD_ROWS * PlayerMenuScreen.CARD_PITCH_Y + 11;

    private static final int LOOK_PITCH = 67, LOOK_LABEL_W = 8, LOOK_BUTTON_W = 27;
}
