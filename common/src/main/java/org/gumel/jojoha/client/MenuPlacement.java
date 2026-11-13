package org.gumel.jojoha.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.gumel.jojoha.data.ClientPlayerDataCache;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.StandPlacement;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.stand.StandMovementProfile;
import org.gumel.jojoha.stand.StandType;
import java.util.List;
import java.util.Locale;

final class MenuPlacement {
    private final PlayerMenuScreen screen;

    MenuPlacement(PlayerMenuScreen screen) {
        this.screen = screen;
    }

    private static final int SKIN_ARROW = 16;

    private static final int PLACE_ROW_X = PlayerMenuScreen.SKIN_BG_X + 220, PLACE_ROW_Y = PlayerMenuScreen.SKIN_BG_Y + 20, PLACE_ROW_H = 22;

    private static final int PLACE_LABEL_W = 36, PLACE_VALUE_W = 30;

    private static final int PLACE_MINUS_X = PLACE_ROW_X + PLACE_LABEL_W;

    private static final int PLACE_PLUS_X = PLACE_MINUS_X + SKIN_ARROW + PLACE_VALUE_W;

    private static final int PLACE_RESET_X = PLACE_MINUS_X + SKIN_ARROW + PLACE_VALUE_W / 2 - PlayerMenuScreen.BUTTON_W / 2;

    private static final int PLACE_RESET_Y = PLACE_ROW_Y + 4 * PLACE_ROW_H + 4;

    private static final int PLACE_MAP_X = PlayerMenuScreen.SKIN_BG_X + 100, PLACE_MAP_Y = PlayerMenuScreen.SKIN_BG_Y + 16, PLACE_MAP_SIZE = 112;

    private static final float PLACE_MAP_PIXELS = 16F;

    private static final float PLACE_STEP = 0.1F, PLACE_BIG_STEP = 0.5F;

    private static final float TURN_STEP = 15F, TURN_BIG_STEP = 45F;

    private static final int PLACE_DOT = 6;

    private static final float PLACE_VIEW_PIXELS = 30F;

    private static final String[] PLACE_ROWS = {"side", "height", "ahead", "turn"};

    void renderPlacement(GuiGraphics guiGraphics, JojohaPlayerData data) {
        StandType type = PlayerMenuScreen.standType(data);
        if (type == null) {
            screen.centredWrapped(guiGraphics, Component.translatable("menu.jojoha.custom.place.no_stand").getString(),
                    PlayerMenuScreen.SKIN_BG_X + PlayerMenuScreen.SKIN_BG_W / 2, PlayerMenuScreen.SKIN_BG_Y + PlayerMenuScreen.SKIN_BG_H / 2 - 4, PlayerMenuScreen.SKIN_BG_W - 40, PlayerMenuScreen.COLOR_DIM);
            return;
        }
        screen.drawPresets(guiGraphics, data, type);
        StandPlacement placed = data.standPlacement;
        if (previewsPlacement(type)) {
            screen.clipped(guiGraphics, PlayerMenuScreen.PLACE_VIEW_X, PlayerMenuScreen.PLACE_VIEW_Y, PlayerMenuScreen.PLACE_VIEW_W, PlayerMenuScreen.PLACE_VIEW_H,
                    () -> PlacementPreview.render(guiGraphics, data, type, placed, PlayerMenuScreen.PLACE_VIEW_X, PlayerMenuScreen.PLACE_VIEW_Y,
                            PlayerMenuScreen.PLACE_VIEW_W, PlayerMenuScreen.PLACE_VIEW_H, PLACE_VIEW_PIXELS));
            if (screen.within(PlayerMenuScreen.PLACE_VIEW_X, PlayerMenuScreen.PLACE_VIEW_Y, PlayerMenuScreen.PLACE_VIEW_W, PlayerMenuScreen.PLACE_VIEW_H) && !PlacementPreview.turning()) {
                screen.tooltip = List.of(
                        Component.translatable("menu.jojoha.custom.place.preview")
                                .withStyle(style -> style.withColor(PlayerMenuScreen.COLOR_ACCENT)),
                        Component.translatable("menu.jojoha.custom.place.preview.spin")
                                .withStyle(style -> style.withColor(PlayerMenuScreen.COLOR_DIM)));
            }
        } else {
            drawPlacementMap(guiGraphics, data, type, placed);
        }

        float[] values = {placed.side(), placed.height(), placed.ahead(), placed.turn()};
        for (int row = 0; row < PLACE_ROWS.length; row++) {
            int y = PLACE_ROW_Y + row * PLACE_ROW_H;
            screen.smallRaw(guiGraphics, Component.translatable("menu.jojoha.custom.place." + PLACE_ROWS[row]).getString(),
                    PLACE_ROW_X, y + (SKIN_ARROW - screen.lineStep()) / 2 + 1, PlayerMenuScreen.COLOR_TEXT);
            boolean overMinus = screen.within(PLACE_MINUS_X, y, SKIN_ARROW, SKIN_ARROW);
            boolean overPlus = screen.within(PLACE_PLUS_X, y, SKIN_ARROW, SKIN_ARROW);
            PlayerMenuScreen.blit(guiGraphics, overMinus ? PlayerMenuScreen.SKIN_ARROW_LEFT_HOVER : PlayerMenuScreen.SKIN_ARROW_LEFT, PLACE_MINUS_X, y, SKIN_ARROW, SKIN_ARROW);
            PlayerMenuScreen.blit(guiGraphics, overPlus ? PlayerMenuScreen.SKIN_ARROW_RIGHT_HOVER : PlayerMenuScreen.SKIN_ARROW_RIGHT, PLACE_PLUS_X, y, SKIN_ARROW, SKIN_ARROW);
            String value = row == 3 ? signed(values[row], 0) + "°" : signed(values[row], 1);
            int width = Math.round(screen.font().width(value) * screen.textScale);
            screen.smallRaw(guiGraphics, value, PLACE_MINUS_X + SKIN_ARROW + (PLACE_VALUE_W - width) / 2,
                    y + (SKIN_ARROW - screen.lineStep()) / 2 + 1, values[row] == 0F ? PlayerMenuScreen.COLOR_DIM : PlayerMenuScreen.COLOR_ACCENT);
            if (overMinus || overPlus || screen.within(PLACE_ROW_X, y, PLACE_LABEL_W, SKIN_ARROW)) {
                screen.tooltip = List.of(
                        Component.translatable("menu.jojoha.custom.place." + PLACE_ROWS[row])
                                .withStyle(style -> style.withColor(PlayerMenuScreen.COLOR_ACCENT)),
                        Component.translatable("menu.jojoha.custom.place." + PLACE_ROWS[row] + ".hint")
                                .withStyle(style -> style.withColor(PlayerMenuScreen.COLOR_DIM)),
                        Component.translatable("menu.jojoha.custom.place.shift")
                                .withStyle(style -> style.withColor(PlayerMenuScreen.COLOR_DIM)));
            }
        }
        screen.button(guiGraphics, PLACE_RESET_X, PLACE_RESET_Y, PlayerMenuScreen.BUTTON_W, PlayerMenuScreen.BUTTON_H,
                Component.translatable("menu.jojoha.custom.place.reset"), !placed.isNone());
    }

    static String signed(float value, int decimals) {
        String text = String.format(Locale.ROOT, "%." + decimals + "f", value);
        return value > 0F ? "+" + text : text.equals("-0.0") || text.equals("-0") ? text.substring(1) : text;
    }

    private void drawPlacementMap(GuiGraphics guiGraphics, JojohaPlayerData data, StandType type,
                                  StandPlacement placed) {
        int x0 = PLACE_MAP_X, y0 = PLACE_MAP_Y, size = PLACE_MAP_SIZE;
        guiGraphics.fill(x0, y0, x0 + size, y0 + size, PlayerMenuScreen.COLOR_MAP);
        int cx = x0 + size / 2, cy = y0 + size / 2;
        int cells = Math.round(size / 2F / PLACE_MAP_PIXELS);
        for (int i = -cells; i <= cells; i++) {
            int gx = cx + Math.round(i * PLACE_MAP_PIXELS);
            int gy = cy + Math.round(i * PLACE_MAP_PIXELS);
            if (gx > x0 && gx < x0 + size) {
                guiGraphics.fill(gx, y0, gx + 1, y0 + size, PlayerMenuScreen.COLOR_MAP_LINE);
            }
            if (gy > y0 && gy < y0 + size) {
                guiGraphics.fill(x0, gy, x0 + size, gy + 1, PlayerMenuScreen.COLOR_MAP_LINE);
            }
        }
        dot(guiGraphics, cx, cy, 0xFFFFFFFF);
        mark(guiGraphics, cx, cy, 0F, 0xFFFFFFFF);

        StandMovementProfile profile = StandMovementProfile.forStand(type);
        float across = (float) profile.sideOffset() + placed.side();
        float forward = placed.ahead() - (float) profile.followDistance();
        int sx = Mth.clamp(cx + Math.round(across * PLACE_MAP_PIXELS), x0 + PLACE_DOT, x0 + size - PLACE_DOT);
        int sy = Mth.clamp(cy - Math.round(forward * PLACE_MAP_PIXELS), y0 + PLACE_DOT, y0 + size - PLACE_DOT);
        int colour = 0xFF000000 | PlayerMenuScreen.skinTint(data, type, data.stand.skin());
        dot(guiGraphics, sx, sy, colour);
        mark(guiGraphics, sx, sy, placed.turn(), colour);

        screen.smallRaw(guiGraphics, Component.translatable("menu.jojoha.custom.place.you").getString(),
                cx + PLACE_DOT, cy + PLACE_DOT, PlayerMenuScreen.COLOR_DIM);
        String height = Component.translatable("menu.jojoha.custom.place.raised",
                signed((float) profile.verticalOffset() + placed.height(), 1)).getString();
        screen.smallRaw(guiGraphics, height, x0 + 3, y0 + size - screen.lineStep() - 2, PlayerMenuScreen.COLOR_DIM);
    }

    static void dot(GuiGraphics guiGraphics, int x, int y, int colour) {
        int half = PLACE_DOT / 2;
        guiGraphics.fill(x - half - 1, y - half - 1, x + half + 1, y + half + 1, 0xFF000000);
        guiGraphics.fill(x - half, y - half, x + half, y + half, colour);
    }

    static void mark(GuiGraphics guiGraphics, int x, int y, float turn, int colour) {
        double radians = Math.toRadians(turn);
        for (int step = PLACE_DOT / 2 + 2; step <= PLACE_DOT / 2 + 6; step++) {
            int px = x + (int) Math.round(Math.sin(radians) * step);
            int py = y - (int) Math.round(Math.cos(radians) * step);
            guiGraphics.fill(px - 1, py - 1, px + 1, py + 1, colour);
        }
    }

    static boolean previewsPlacement(StandType type) {
        return type != null && type.form().hasBody();
    }

    boolean clickedPlacement(int button) {
        JojohaPlayerData data = ClientPlayerDataCache.data;
        if (previewsPlacement(PlayerMenuScreen.standType(data)) && screen.within(PlayerMenuScreen.PLACE_VIEW_X, PlayerMenuScreen.PLACE_VIEW_Y, PlayerMenuScreen.PLACE_VIEW_W, PlayerMenuScreen.PLACE_VIEW_H)) {
            if (button == 0) {
                PlacementPreview.grab(screen.hasShiftDown());
                return true;
            }
            if (button == 1) {
                PlacementPreview.reset();
                PlayerMenuScreen.click();
                return true;
            }
        }
        if (button != 0 || PlayerMenuScreen.standType(data) == null) {
            return false;
        }
        if (screen.clickedPresets(data, PlayerMenuScreen.standType(data))) {
            return true;
        }
        StandPlacement placed = data.standPlacement;
        if (!placed.isNone() && screen.within(PLACE_RESET_X, PLACE_RESET_Y, PlayerMenuScreen.BUTTON_W, PlayerMenuScreen.BUTTON_H)) {
            place(data, StandPlacement.NONE);
            return true;
        }
        boolean big = screen.hasShiftDown();
        for (int row = 0; row < PLACE_ROWS.length; row++) {
            int y = PLACE_ROW_Y + row * PLACE_ROW_H;
            int sign = screen.within(PLACE_MINUS_X, y, SKIN_ARROW, SKIN_ARROW) ? -1
                    : screen.within(PLACE_PLUS_X, y, SKIN_ARROW, SKIN_ARROW) ? 1 : 0;
            if (sign == 0) {
                continue;
            }
            float step = sign * (row == 3 ? (big ? TURN_BIG_STEP : TURN_STEP) : (big ? PLACE_BIG_STEP : PLACE_STEP));
            float side = placed.side(), height = placed.height(), ahead = placed.ahead(), turn = placed.turn();
            switch (row) {
                case 0 -> side = snap(side + step);
                case 1 -> height = snap(height + step);
                case 2 -> ahead = snap(ahead + step);
                default -> turn = Mth.wrapDegrees(turn + step);
            }
            place(data, new StandPlacement(side, height, ahead, turn));
            return true;
        }
        return false;
    }

    static float snap(float value) {
        return Math.round(value * 10F) / 10F;
    }

    static void place(JojohaPlayerData data, StandPlacement placement) {
        data.standPlacement = placement;
        NetworkHandler.sendStandPlacement(placement);
        PlayerMenuScreen.click();
    }
}
