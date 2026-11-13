package org.gumel.jojoha.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.Mth;
import org.gumel.jojoha.data.ClientPlayerDataCache;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.stand.StandType;
import org.gumel.jojoha.stand.workshop.RestPose;
import org.gumel.jojoha.stand.workshop.StancePreset;
import org.gumel.jojoha.stand.workshop.Unlocks;
import org.gumel.jojoha.supporter.SupporterAuras;
import java.util.ArrayList;
import java.util.List;

final class MenuSkins {
    private final PlayerMenuScreen screen;

    MenuSkins(PlayerMenuScreen screen) {
        this.screen = screen;
    }

    private static int skinCursor;

    void renderSkinsPage(GuiGraphics guiGraphics, JojohaPlayerData data) {
        if (PlayerMenuScreen.section != PlayerMenuScreen.Section.STYLE) {
            PlayerMenuScreen.closePose();
        }
        screen.sectionTabTooltip();
        switch (PlayerMenuScreen.section) {
            case STANCE -> screen.renderPlacement(guiGraphics, data);
            case STYLE -> screen.renderStyle(guiGraphics, data);
            default -> renderSkinGrid(guiGraphics, data);
        }
    }

    private void renderSkinGrid(GuiGraphics guiGraphics, JojohaPlayerData data) {
        StandType type = PlayerMenuScreen.standType(data);
        if (type == null) {
            screen.centredWrapped(guiGraphics, Component.translatable("menu.jojoha.skins.no_stand").getString(),
                    PlayerMenuScreen.SKIN_BG_X + PlayerMenuScreen.SKIN_BG_W / 2, PlayerMenuScreen.SKIN_BG_Y + PlayerMenuScreen.SKIN_BG_H / 2 - 4, PlayerMenuScreen.SKIN_BG_W - 40, PlayerMenuScreen.COLOR_DIM);
            return;
        }
        int count = type.skinCount();
        int cursor = skinCursor(data, count);
        int rows = (count + PlayerMenuScreen.CARD_COLS - 1) / PlayerMenuScreen.CARD_COLS;
        PlayerMenuScreen.skinScroll = Mth.clamp(PlayerMenuScreen.skinScroll, 0, Math.max(0, rows - PlayerMenuScreen.CARD_ROWS));
        int owned = 0;
        for (int skin = 0; skin < count; skin++) {
            owned += wearable(data, type, skin) ? 1 : 0;
        }
        for (int i = 0; i < PlayerMenuScreen.CARD_COLS * PlayerMenuScreen.CARD_ROWS; i++) {
            int skin = PlayerMenuScreen.skinScroll * PlayerMenuScreen.CARD_COLS + i;
            if (skin >= count) {
                break;
            }
            drawSkinCard(guiGraphics, data, type, skin, cardX(i), cardY(i), skin == cursor);
        }
        if (rows > PlayerMenuScreen.CARD_ROWS) {
            int track = PlayerMenuScreen.CARD_ROWS * PlayerMenuScreen.CARD_PITCH_Y - 4;
            int thumb = Math.max(8, track * PlayerMenuScreen.CARD_ROWS / rows);
            int at = PlayerMenuScreen.CARD_Y + (track - thumb) * PlayerMenuScreen.skinScroll / Math.max(1, rows - PlayerMenuScreen.CARD_ROWS);
            int x = PlayerMenuScreen.CARD_X + PlayerMenuScreen.CARD_COLS * PlayerMenuScreen.CARD_PITCH_X;
            guiGraphics.fill(x, at, x + 2, at + thumb, 0xAAFFFFFF);
        }
        screen.smallRaw(guiGraphics, Component.translatable("menu.jojoha.workshop.skins.tally", owned, count).getString(),
                PlayerMenuScreen.CARD_X, PlayerMenuScreen.CARD_Y + PlayerMenuScreen.CARD_ROWS * PlayerMenuScreen.CARD_PITCH_Y + 2, PlayerMenuScreen.COLOR_DIM);
        screen.renderLooks(guiGraphics, data);

        if (PlayerMenuScreen.previewsPlacement(type)) {
            screen.clipped(guiGraphics, PlayerMenuScreen.LOOK_VIEW_X, PlayerMenuScreen.LOOK_VIEW_Y, PlayerMenuScreen.LOOK_VIEW_W, PlayerMenuScreen.LOOK_VIEW_H,
                    () -> PlacementPreview.render(guiGraphics, data, type, data.standPlacement, cursor, PlayerMenuScreen.LOOK_VIEW_X,
                            PlayerMenuScreen.LOOK_VIEW_Y, PlayerMenuScreen.LOOK_VIEW_W, PlayerMenuScreen.LOOK_VIEW_H, LOOK_VIEW_PIXELS));
            if (screen.within(PlayerMenuScreen.LOOK_VIEW_X, PlayerMenuScreen.LOOK_VIEW_Y, PlayerMenuScreen.LOOK_VIEW_W, PlayerMenuScreen.LOOK_VIEW_H) && !PlacementPreview.turning()) {
                screen.tooltip = List.of(
                        Component.translatable("menu.jojoha.workshop.skins.preview")
                                .withStyle(style -> style.withColor(PlayerMenuScreen.COLOR_ACCENT)),
                        Component.translatable("menu.jojoha.custom.place.preview.hint")
                                .withStyle(style -> style.withColor(PlayerMenuScreen.COLOR_DIM)));
            }
        } else {
            guiGraphics.fill(PlayerMenuScreen.LOOK_VIEW_X, PlayerMenuScreen.LOOK_VIEW_Y, PlayerMenuScreen.LOOK_VIEW_X + PlayerMenuScreen.LOOK_VIEW_W, PlayerMenuScreen.LOOK_VIEW_Y + PlayerMenuScreen.LOOK_VIEW_H, PlayerMenuScreen.COLOR_MAP);
            screen.clipped(guiGraphics, PlayerMenuScreen.LOOK_VIEW_X, PlayerMenuScreen.LOOK_VIEW_Y, PlayerMenuScreen.LOOK_VIEW_W, PlayerMenuScreen.LOOK_VIEW_H,
                    () -> StandPortrait.render(guiGraphics, data, cursor, PlayerMenuScreen.LOOK_VIEW_X, PlayerMenuScreen.LOOK_VIEW_Y, PlayerMenuScreen.LOOK_VIEW_W,
                            PlayerMenuScreen.LOOK_VIEW_H, CARD_SCALE * 2F, PlayerMenuScreen.SKIN_ANCHOR, StandPortrait.ROTATION, Float.NaN, Float.NaN));
        }
        int tint = PlayerMenuScreen.skinTint(data, type, cursor);
        String name = type.skinName(cursor).getString();
        int width = Math.round(screen.font().width(name) * screen.textScale);
        screen.smallRaw(guiGraphics, name, PlayerMenuScreen.LOOK_VIEW_X + (PlayerMenuScreen.LOOK_VIEW_W - width) / 2, PlayerMenuScreen.LOOK_VIEW_Y + PlayerMenuScreen.LOOK_VIEW_H + 3, tint);

        boolean worn = cursor == data.stand.skin();
        boolean wearable = wearable(data, type, cursor);
        screen.button(guiGraphics, WEAR_X, WEAR_Y, PlayerMenuScreen.BUTTON_W, PlayerMenuScreen.BUTTON_H, Component.translatable(worn
                ? "menu.jojoha.workshop.skins.worn" : wearable ? "menu.jojoha.workshop.skins.wear"
                : "menu.jojoha.workshop.skins.locked"), wearable && !worn);
        if (screen.within(WEAR_X, WEAR_Y, PlayerMenuScreen.BUTTON_W, PlayerMenuScreen.BUTTON_H)) {
            screen.tooltip = worn
                    ? List.of(Component.translatable("menu.jojoha.skins.equipped")
                            .withStyle(style -> style.withColor(PlayerMenuScreen.COLOR_ACCENT)))
                    : wearable
                    ? List.of(Component.translatable("menu.jojoha.skins.equip")
                            .withStyle(style -> style.withColor(PlayerMenuScreen.COLOR_ACCENT)))
                    : lockedTooltip(type, cursor);
        }
    }

    private static int cardX(int index) {
        return PlayerMenuScreen.CARD_X + (index % PlayerMenuScreen.CARD_COLS) * PlayerMenuScreen.CARD_PITCH_X;
    }

    private static int cardY(int index) {
        return PlayerMenuScreen.CARD_Y + (index / PlayerMenuScreen.CARD_COLS) * PlayerMenuScreen.CARD_PITCH_Y;
    }

    private static boolean wearable(JojohaPlayerData data, StandType type, int skin) {
        return type.supporterSkin(skin)
                ? SupporterAuras.ownSkins() || Unlocks.all(data)
                : data.canWearSkin(data.stand.standId(), skin);
    }

    private List<Component> lockedTooltip(StandType type, int skin) {
        if (type.supporterSkin(skin)) {
            return List.of(
                    Component.translatable("menu.jojoha.skins.supporter").withStyle(style -> style.withColor(PlayerMenuScreen.COLOR_ACCENT)),
                    Component.translatable("menu.jojoha.skins.supporter_hint").withStyle(style -> style.withColor(PlayerMenuScreen.COLOR_DIM)));
        }
        String earned = type.skinUnlock(skin);
        if (earned != null) {
            return List.of(
                    Component.translatable("menu.jojoha.skins.unlock.earned").withStyle(style -> style.withColor(PlayerMenuScreen.COLOR_ACCENT)),
                    Component.translatable(earned).withStyle(style -> style.withColor(PlayerMenuScreen.COLOR_DIM)));
        }
        return notCommittedTooltip();
    }

    private static int skinCursor(JojohaPlayerData data, int count) {
        if (!data.stand.standId().equals(PlayerMenuScreen.skinCursorStand)) {
            PlayerMenuScreen.skinCursorStand = data.stand.standId();
            skinCursor = data.stand.skin();
            PlayerMenuScreen.skinScroll = Math.max(0, skinCursor / PlayerMenuScreen.CARD_COLS - PlayerMenuScreen.CARD_ROWS + 1);
        }
        skinCursor = Math.floorMod(skinCursor, Math.max(1, count));
        return skinCursor;
    }

    static int skinToSave(JojohaPlayerData data, StandType type) {
        int cursor = skinCursor(data, type.skinCount());
        return wearable(data, type, cursor) ? cursor : data.stand.skin();
    }

    static void showSkin(int skin) {
        int row = skin / PlayerMenuScreen.CARD_COLS;
        skinCursor = skin;
        PlayerMenuScreen.skinScroll = Mth.clamp(PlayerMenuScreen.skinScroll, Math.max(0, row - PlayerMenuScreen.CARD_ROWS + 1), row);
    }

    private void drawSkinCard(GuiGraphics guiGraphics, JojohaPlayerData data, StandType type, int skin,
                              int x, int y, boolean selected) {
        guiGraphics.fill(x, y, x + PlayerMenuScreen.CARD_W, y + PlayerMenuScreen.CARD_H, PlayerMenuScreen.COLOR_CARD);
        int ix = x + 1, iy = y + 1, iw = PlayerMenuScreen.CARD_W - 2, ih = PlayerMenuScreen.CARD_H - 2;
        screen.clipped(guiGraphics, ix, iy, iw, ih, () -> StandPortrait.render(guiGraphics, data, skin, ix, iy, iw, ih,
                CARD_SCALE, PlayerMenuScreen.SKIN_ANCHOR, StandPortrait.ROTATION, Float.NaN, Float.NaN));

        boolean owned = wearable(data, type, skin);
        boolean worn = skin == data.stand.skin();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0F, 0F, 200F);
        if (!owned) {
            guiGraphics.fill(ix, iy, ix + iw, iy + ih, PlayerMenuScreen.COLOR_CARD_LOCKED);
            screen.centred(guiGraphics, "?", x + PlayerMenuScreen.CARD_W / 2, y + PlayerMenuScreen.CARD_H / 2 - 4, PlayerMenuScreen.COLOR_DIM);
        }
        if (type.supporterSkin(skin)) {
            guiGraphics.fill(x + PlayerMenuScreen.CARD_W - 7, y + 2, x + PlayerMenuScreen.CARD_W - 2, y + 7, 0xFF000000);
            guiGraphics.fill(x + PlayerMenuScreen.CARD_W - 6, y + 3, x + PlayerMenuScreen.CARD_W - 3, y + 6, COLOR_SUPPORTER_PIP);
        }
        if (worn) {
            guiGraphics.renderOutline(x, y, PlayerMenuScreen.CARD_W, PlayerMenuScreen.CARD_H, PlayerMenuScreen.COLOR_HOVER);
        } else if (selected) {
            guiGraphics.renderOutline(x, y, PlayerMenuScreen.CARD_W, PlayerMenuScreen.CARD_H, 0xFFFFFFFF);
        }
        guiGraphics.pose().popPose();

        if (!screen.within(x, y, PlayerMenuScreen.CARD_W, PlayerMenuScreen.CARD_H)) {
            return;
        }
        int tint = PlayerMenuScreen.skinTint(data, type, skin);
        List<Component> lines = new ArrayList<>();
        lines.add(type.skinName(skin).withStyle(style -> style.withColor(TextColor.fromRgb(tint))));
        if (worn) {
            lines.add(Component.translatable(owned ? "menu.jojoha.skins.equipped"
                    : "menu.jojoha.skins.worn_uncommitted").withStyle(style -> style.withColor(PlayerMenuScreen.COLOR_DIM)));
        } else if (!owned) {
            lines.addAll(lockedTooltip(type, skin));
        }
        screen.tooltip = List.copyOf(lines);
    }

    private List<Component> notCommittedTooltip() {
        return List.of(
                Component.translatable("menu.jojoha.skins.unlock.arrow_title").withStyle(style -> style.withColor(PlayerMenuScreen.COLOR_ACCENT)),
                Component.translatable("menu.jojoha.skins.unlock.arrow").withStyle(style -> style.withColor(PlayerMenuScreen.COLOR_DIM)),
                Component.translatable("menu.jojoha.skins.commit_hint").withStyle(style -> style.withColor(PlayerMenuScreen.COLOR_DIM)));
    }

    private boolean clickedSkinGrid(int button) {
        JojohaPlayerData data = ClientPlayerDataCache.data;
        StandType type = PlayerMenuScreen.standType(data);
        if (type == null) {
            return false;
        }
        if (PlayerMenuScreen.previewsPlacement(type) && screen.within(PlayerMenuScreen.LOOK_VIEW_X, PlayerMenuScreen.LOOK_VIEW_Y, PlayerMenuScreen.LOOK_VIEW_W, PlayerMenuScreen.LOOK_VIEW_H)) {
            if (button == 0) {
                PlacementPreview.grab(false);
                return true;
            }
            if (button == 1) {
                PlacementPreview.reset();
                PlayerMenuScreen.click();
                return true;
            }
        }
        if (button != 0) {
            return false;
        }
        if (screen.clickedLooks(button)) {
            return true;
        }
        int count = type.skinCount();
        int cursor = skinCursor(data, count);
        for (int i = 0; i < PlayerMenuScreen.CARD_COLS * PlayerMenuScreen.CARD_ROWS; i++) {
            int skin = PlayerMenuScreen.skinScroll * PlayerMenuScreen.CARD_COLS + i;
            if (skin < count && screen.within(cardX(i), cardY(i), PlayerMenuScreen.CARD_W, PlayerMenuScreen.CARD_H)) {
                skinCursor = skin;
                PlayerMenuScreen.click();
                return true;
            }
        }
        if (screen.within(WEAR_X, WEAR_Y, PlayerMenuScreen.BUTTON_W, PlayerMenuScreen.BUTTON_H)) {
            if (cursor != data.stand.skin() && wearable(data, type, cursor)) {
                NetworkHandler.sendEquipStandSkin(cursor);
                PlayerMenuScreen.click();
            }
            return true;
        }
        return false;
    }

    private static StancePreset presetOf(JojohaPlayerData data) {
        for (StancePreset preset : PlayerMenuScreen.PRESETS) {
            if (preset.placement().equals(data.standPlacement) && preset.rest().ordinal() == data.restPose) {
                return preset;
            }
        }
        return null;
    }

    void drawPresets(GuiGraphics guiGraphics, JojohaPlayerData data, StandType type) {
        screen.smallRaw(guiGraphics, Component.translatable("menu.jojoha.workshop.stance.presets").getString(),
                STANCE_X, STANCE_Y - screen.lineStep() - 2, PlayerMenuScreen.COLOR_DIM);
        StancePreset current = presetOf(data);
        for (int i = 0; i < PlayerMenuScreen.PRESETS.length; i++) {
            StancePreset preset = PlayerMenuScreen.PRESETS[i];
            int y = STANCE_Y + i * STANCE_ROW_H;
            boolean over = screen.within(STANCE_X, y, STANCE_W, STANCE_ROW_H);
            if (preset == current || over) {
                guiGraphics.fill(STANCE_X, y, STANCE_X + STANCE_W, y + STANCE_ROW_H,
                        preset == current ? PlayerMenuScreen.COLOR_ROW_PICKED : PlayerMenuScreen.COLOR_ROW_OVER);
            }
            String key = "menu.jojoha.workshop.stance." + preset.key();
            screen.smallLeft(guiGraphics, Component.translatable(key).getString(), STANCE_X + 3,
                    y + (STANCE_ROW_H - screen.lineStep()) / 2 + 1, STANCE_W - 6,
                    preset == current ? PlayerMenuScreen.COLOR_ACCENT : over ? PlayerMenuScreen.COLOR_HOVER : PlayerMenuScreen.COLOR_TEXT);
            if (over) {
                screen.tooltip = List.of(Component.translatable(key).withStyle(style -> style.withColor(PlayerMenuScreen.COLOR_ACCENT)),
                        Component.translatable(key + ".hint").withStyle(style -> style.withColor(PlayerMenuScreen.COLOR_DIM)));
            }
        }
        screen.smallRaw(guiGraphics, Component.translatable("menu.jojoha.workshop.rest").getString(),
                STANCE_X, PlayerMenuScreen.REST_Y - screen.lineStep() - 1, PlayerMenuScreen.COLOR_DIM);
        for (RestPose rest : RestPose.values()) {
            int x = STANCE_X + rest.ordinal() * (PlayerMenuScreen.REST_W + 2);
            boolean usable = rest.fits(type);
            boolean chosen = data.restPose == rest.ordinal();
            screen.button(guiGraphics, x, PlayerMenuScreen.REST_Y, PlayerMenuScreen.REST_W, PlayerMenuScreen.REST_H,
                    Component.translatable("menu.jojoha.workshop.rest." + rest.key()), usable && !chosen);
            if (chosen) {
                guiGraphics.renderOutline(x, PlayerMenuScreen.REST_Y, PlayerMenuScreen.REST_W, PlayerMenuScreen.REST_H, PlayerMenuScreen.COLOR_HOVER);
            }
            if (screen.within(x, PlayerMenuScreen.REST_Y, PlayerMenuScreen.REST_W, PlayerMenuScreen.REST_H)) {
                String key = "menu.jojoha.workshop.rest." + rest.key();
                screen.tooltip = List.of(Component.translatable(key).withStyle(style -> style.withColor(PlayerMenuScreen.COLOR_ACCENT)),
                        Component.translatable(usable ? key + ".hint" : "menu.jojoha.workshop.rest.no_ready")
                                .withStyle(style -> style.withColor(PlayerMenuScreen.COLOR_DIM)));
            }
        }
    }

    boolean clickedPresets(JojohaPlayerData data, StandType type) {
        for (int i = 0; i < PlayerMenuScreen.PRESETS.length; i++) {
            if (screen.within(STANCE_X, STANCE_Y + i * STANCE_ROW_H, STANCE_W, STANCE_ROW_H)) {
                StancePreset preset = PlayerMenuScreen.PRESETS[i];
                int rest = preset.rest().fits(type) ? preset.rest().ordinal() : RestPose.CALM.ordinal();
                PlayerMenuScreen.place(data, preset.placement());
                PlayerMenuScreen.style(data, data.summonStyle, data.barrageShout, rest);
                return true;
            }
        }
        for (RestPose rest : RestPose.values()) {
            int x = STANCE_X + rest.ordinal() * (PlayerMenuScreen.REST_W + 2);
            if (screen.within(x, PlayerMenuScreen.REST_Y, PlayerMenuScreen.REST_W, PlayerMenuScreen.REST_H)) {
                if (rest.fits(type) && data.restPose != rest.ordinal()) {
                    PlayerMenuScreen.style(data, data.summonStyle, data.barrageShout, rest.ordinal());
                    PlayerMenuScreen.click();
                }
                return true;
            }
        }
        return false;
    }

    private static final float CARD_SCALE = 17F;

    private static final float LOOK_VIEW_PIXELS = 26F;

    private static final int WEAR_X = PlayerMenuScreen.LOOK_VIEW_X + (PlayerMenuScreen.LOOK_VIEW_W - PlayerMenuScreen.BUTTON_W) / 2, WEAR_Y = PlayerMenuScreen.LOOK_VIEW_Y + PlayerMenuScreen.LOOK_VIEW_H + 14;

    private static final int COLOR_SUPPORTER_PIP = 0xFFFFD23F;

    private static final int STANCE_X = PlayerMenuScreen.SKIN_BG_X + 8, STANCE_Y = PlayerMenuScreen.SKIN_BG_Y + 20, STANCE_W = 86, STANCE_ROW_H = 14;

    boolean clickedSkins(int button) {
        if (button == 0 && screen.clickedSectionTab()) {
            return true;
        }
        return switch (PlayerMenuScreen.section) {
            case STANCE -> screen.clickedPlacement(button);
            case STYLE -> screen.clickedStyle(button);
            default -> clickedSkinGrid(button);
        };
    }
}
