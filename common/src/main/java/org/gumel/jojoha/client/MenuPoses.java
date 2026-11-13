package org.gumel.jojoha.client;

import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.gumel.jojoha.data.ClientPlayerDataCache;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.StandPose;
import org.gumel.jojoha.network.NetworkHandler;
import org.joml.Quaternionf;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class MenuPoses {
    private final PlayerMenuScreen screen;

    MenuPoses(PlayerMenuScreen screen) {
        this.screen = screen;
    }

    private static final int IDLE_BUTTON_Y = PlayerMenuScreen.IDLE_LIST_Y + PlayerMenuScreen.IDLE_ROWS * PlayerMenuScreen.IDLE_ROW_H + 3;

    private static final int IDLE_OPEN_X = PlayerMenuScreen.IDLE_LIST_X, IDLE_OPEN_W = 52;

    private static final int IDLE_RELOAD_X = IDLE_OPEN_X + IDLE_OPEN_W + 2, IDLE_RELOAD_W = 36;

    private static final int IDLE_EDIT_X = IDLE_RELOAD_X + IDLE_RELOAD_W + 2, IDLE_EDIT_W = 57;

    private static final int IDLE_TEXT_Y = IDLE_BUTTON_Y + PlayerMenuScreen.REST_H + 3;

    private static final int POSE_BONE_X = PlayerMenuScreen.SKIN_BG_X + 8, POSE_BONE_Y = PlayerMenuScreen.SKIN_BG_Y + 8, POSE_BONE_W = 86;

    private static final int POSE_BONE_H = 9;

    private static final int POSE_MIRROR_Y = POSE_BONE_Y + 12 * POSE_BONE_H + 3, POSE_MIRROR_W = 42;

    private static final String[] POSE_MIRRORS = {"mirror", "flip"};

    private static final int POSE_VIEW_X = PlayerMenuScreen.SKIN_BG_X + 98, POSE_VIEW_Y = PlayerMenuScreen.SKIN_BG_Y + 6;

    private static final int POSE_VIEW_W = 120, POSE_VIEW_H = 132;

    private static final float POSE_VIEW_SCALE = 38F;

    private static final int POSE_SLIDER_X = PlayerMenuScreen.SKIN_BG_X + 224, POSE_SLIDER_Y = PlayerMenuScreen.SKIN_BG_Y + 8;

    private static final int POSE_SLIDER_W = 98, POSE_SLIDER_PITCH = 18, POSE_BAR_H = 5;

    private static final int POSE_BUTTON_Y = PlayerMenuScreen.SKIN_BG_Y + 120, POSE_BUTTON_W = 31, POSE_BUTTON_GAP = 2;

    private static final float POSE_MOVE_STEP = 0.25F, POSE_TURN_STEP = 1F, POSE_TURN_SCROLL = 5F;

    private static final String[] POSE_CHANNELS = {"move_x", "move_y", "move_z", "turn_x", "turn_y", "turn_z"};

    private static final String[] POSE_BUTTONS = {"save", "reset", "back"};

    private static final int COLOR_BAR = 0xFF2A2A30, COLOR_BAR_MID = 0xFF505058, COLOR_KNOB = 0xFFFFFFFF;

    private static int poseBone;

    private static final float[][] poseDraft = new float[StandPose.BONES.size()][StandPose.CHANNELS];

    private static void openPose(int slot) {
        PlayerMenuScreen.poseSlot = slot;
        poseBone = 0;
        PlayerMenuScreen.poseSlider = -1;
        StandPose saved = ClientPlayerDataCache.data.poses[slot];
        for (int i = 0; i < poseDraft.length; i++) {
            poseDraft[i] = saved.values(StandPose.BONES.get(i));
        }
        StandIdles.editing(draftPose());
    }

    static void closePose() {
        if (PlayerMenuScreen.poseSlot >= 0) {
            PlayerMenuScreen.poseSlot = -1;
            PlayerMenuScreen.poseSlider = -1;
            PlayerMenuScreen.poseTurning = false;
            StandIdles.editing(null);
        }
    }

    private static StandPose draftPose() {
        List<StandPose.Bone> bones = new ArrayList<>();
        for (int i = 0; i < poseDraft.length; i++) {
            bones.add(StandPose.Bone.of(StandPose.BONES.get(i), poseDraft[i]));
        }
        return new StandPose(bones).tidy();
    }

    static void setPose(int channel, float value) {
        float limit = StandPose.limit(channel);
        float step = channel < 3 ? POSE_MOVE_STEP : POSE_TURN_STEP;
        poseDraft[poseBone][channel] = Mth.clamp(Math.round(value / step) * step, -limit, limit);
        StandIdles.editing(draftPose());
    }

    private static int poseBarY(int channel) {
        return POSE_SLIDER_Y + channel * POSE_SLIDER_PITCH + POSE_SLIDER_PITCH - POSE_BAR_H - 3;
    }

    void slidePose() {
        float t = Mth.clamp((float) (screen.localMouseX - POSE_SLIDER_X) / POSE_SLIDER_W, 0F, 1F);
        float limit = StandPose.limit(PlayerMenuScreen.poseSlider);
        setPose(PlayerMenuScreen.poseSlider, (t * 2F - 1F) * limit);
    }

    private void renderPoseEditor(GuiGraphics guiGraphics, JojohaPlayerData data) {
        for (int i = 0; i < StandPose.BONES.size(); i++) {
            int y = POSE_BONE_Y + i * POSE_BONE_H;
            boolean selected = i == poseBone;
            boolean over = screen.within(POSE_BONE_X, y, POSE_BONE_W, POSE_BONE_H);
            boolean moved = false;
            for (float value : poseDraft[i]) {
                moved |= value != 0F;
            }
            if (selected || over) {
                guiGraphics.fill(POSE_BONE_X, y, POSE_BONE_X + POSE_BONE_W, y + POSE_BONE_H,
                        selected ? PlayerMenuScreen.COLOR_ROW_PICKED : PlayerMenuScreen.COLOR_ROW_OVER);
            }
            String label = Component.translatable("menu.jojoha.workshop.pose.bone."
                    + StandPose.BONES.get(i).toLowerCase(Locale.ROOT)).getString();
            screen.smallLeft(guiGraphics, (moved ? "* " : "") + label, POSE_BONE_X + 3, y + (POSE_BONE_H - screen.lineStep()) / 2 + 1,
                    POSE_BONE_W - 6, selected ? PlayerMenuScreen.COLOR_ACCENT : over ? PlayerMenuScreen.COLOR_HOVER : PlayerMenuScreen.COLOR_TEXT);
        }

        int across = StandPose.opposite(poseBone);
        for (int i = 0; i < POSE_MIRRORS.length; i++) {
            int x = POSE_BONE_X + i * (POSE_MIRROR_W + POSE_BUTTON_GAP);
            boolean usable = i == 1 || across >= 0;
            String key = "menu.jojoha.workshop.pose." + POSE_MIRRORS[i];
            screen.button(guiGraphics, x, POSE_MIRROR_Y, POSE_MIRROR_W, PlayerMenuScreen.REST_H, Component.translatable(key), usable);
            if (screen.within(x, POSE_MIRROR_Y, POSE_MIRROR_W, PlayerMenuScreen.REST_H)) {
                screen.tooltip = List.of(Component.translatable(key).withStyle(style -> style.withColor(PlayerMenuScreen.COLOR_ACCENT)),
                        Component.translatable(usable ? key + ".hint" : key + ".none")
                                .withStyle(style -> style.withColor(PlayerMenuScreen.COLOR_DIM)));
            }
        }

        guiGraphics.fill(POSE_VIEW_X, POSE_VIEW_Y, POSE_VIEW_X + POSE_VIEW_W, POSE_VIEW_Y + POSE_VIEW_H, PlayerMenuScreen.COLOR_MAP);
        Quaternionf view = new Quaternionf(StandPortrait.ROTATION).rotateY(PlayerMenuScreen.poseYaw * Mth.DEG_TO_RAD);
        screen.clipped(guiGraphics, POSE_VIEW_X, POSE_VIEW_Y, POSE_VIEW_W, POSE_VIEW_H,
                () -> StandPortrait.render(guiGraphics, data, POSE_VIEW_X, POSE_VIEW_Y, POSE_VIEW_W, POSE_VIEW_H,
                        POSE_VIEW_SCALE, PlayerMenuScreen.SKIN_ANCHOR, view));
        if (screen.within(POSE_VIEW_X, POSE_VIEW_Y, POSE_VIEW_W, POSE_VIEW_H) && !PlayerMenuScreen.poseTurning) {
            screen.tooltip = List.of(Component.translatable("menu.jojoha.workshop.pose.preview")
                            .withStyle(style -> style.withColor(PlayerMenuScreen.COLOR_ACCENT)),
                    Component.translatable("menu.jojoha.workshop.pose.preview.hint")
                            .withStyle(style -> style.withColor(PlayerMenuScreen.COLOR_DIM)));
        }

        for (int channel = 0; channel < StandPose.CHANNELS; channel++) {
            int labelY = POSE_SLIDER_Y + channel * POSE_SLIDER_PITCH;
            int barY = poseBarY(channel);
            float value = poseDraft[poseBone][channel];
            float limit = StandPose.limit(channel);
            screen.smallRaw(guiGraphics, Component.translatable("menu.jojoha.workshop.pose." + POSE_CHANNELS[channel]).getString(),
                    POSE_SLIDER_X, labelY, PlayerMenuScreen.COLOR_TEXT);
            String shown = channel < 3 ? PlayerMenuScreen.signed(value, 2) : PlayerMenuScreen.signed(value, 0) + "°";
            int width = Math.round(screen.font().width(shown) * screen.textScale);
            screen.smallRaw(guiGraphics, shown, POSE_SLIDER_X + POSE_SLIDER_W - width, labelY,
                    value == 0F ? PlayerMenuScreen.COLOR_DIM : PlayerMenuScreen.COLOR_ACCENT);
            guiGraphics.fill(POSE_SLIDER_X, barY, POSE_SLIDER_X + POSE_SLIDER_W, barY + POSE_BAR_H, COLOR_BAR);
            int mid = POSE_SLIDER_X + POSE_SLIDER_W / 2;
            guiGraphics.fill(mid, barY, mid + 1, barY + POSE_BAR_H, COLOR_BAR_MID);
            int knob = POSE_SLIDER_X + Math.round((value / limit + 1F) / 2F * (POSE_SLIDER_W - 1));
            guiGraphics.fill(knob - 1, barY - 1, knob + 2, barY + POSE_BAR_H + 1,
                    channel == PlayerMenuScreen.poseSlider ? PlayerMenuScreen.COLOR_HOVER : COLOR_KNOB);
            if (screen.within(POSE_SLIDER_X, labelY, POSE_SLIDER_W, POSE_SLIDER_PITCH) && PlayerMenuScreen.poseSlider < 0) {
                screen.tooltip = List.of(Component.translatable("menu.jojoha.workshop.pose." + POSE_CHANNELS[channel])
                                .withStyle(style -> style.withColor(PlayerMenuScreen.COLOR_ACCENT)),
                        Component.translatable("menu.jojoha.workshop.pose.slider.hint")
                                .withStyle(style -> style.withColor(PlayerMenuScreen.COLOR_DIM)));
            }
        }

        for (int i = 0; i < POSE_BUTTONS.length; i++) {
            int x = POSE_SLIDER_X + i * (POSE_BUTTON_W + POSE_BUTTON_GAP);
            screen.button(guiGraphics, x, POSE_BUTTON_Y, POSE_BUTTON_W, PlayerMenuScreen.REST_H,
                    Component.translatable("menu.jojoha.workshop.pose." + POSE_BUTTONS[i]), true);
            if (screen.within(x, POSE_BUTTON_Y, POSE_BUTTON_W, PlayerMenuScreen.REST_H)) {
                screen.tooltip = List.of(Component.translatable("menu.jojoha.workshop.pose." + POSE_BUTTONS[i] + ".hint")
                        .withStyle(style -> style.withColor(PlayerMenuScreen.COLOR_DIM)));
            }
        }
        screen.smallLeft(guiGraphics, Component.translatable("menu.jojoha.custom.idle.pose", PlayerMenuScreen.poseSlot + 1).getString(),
                POSE_SLIDER_X, POSE_BUTTON_Y + PlayerMenuScreen.REST_H + 4, POSE_SLIDER_W, PlayerMenuScreen.COLOR_DIM);
    }

    private boolean clickedPoseEditor(int button) {
        for (int i = 0; i < StandPose.BONES.size(); i++) {
            if (button == 0 && screen.within(POSE_BONE_X, POSE_BONE_Y + i * POSE_BONE_H, POSE_BONE_W, POSE_BONE_H)) {
                if (poseBone != i) {
                    poseBone = i;
                    PlayerMenuScreen.click();
                }
                return true;
            }
        }
        if (screen.within(POSE_VIEW_X, POSE_VIEW_Y, POSE_VIEW_W, POSE_VIEW_H)) {
            if (button == 0) {
                PlayerMenuScreen.poseTurning = true;
            } else if (button == 1) {
                PlayerMenuScreen.poseYaw = 0F;
                PlayerMenuScreen.click();
            }
            return true;
        }
        for (int channel = 0; channel < StandPose.CHANNELS; channel++) {
            if (screen.within(POSE_SLIDER_X, poseBarY(channel) - 3, POSE_SLIDER_W, POSE_BAR_H + 6)) {
                if (button == 1) {
                    setPose(channel, 0F);
                    PlayerMenuScreen.click();
                } else if (button == 0) {
                    PlayerMenuScreen.poseSlider = channel;
                    slidePose();
                }
                return true;
            }
        }
        if (button != 0) {
            return false;
        }
        for (int i = 0; i < POSE_MIRRORS.length; i++) {
            if (!screen.within(POSE_BONE_X + i * (POSE_MIRROR_W + POSE_BUTTON_GAP), POSE_MIRROR_Y, POSE_MIRROR_W, PlayerMenuScreen.REST_H)) {
                continue;
            }
            int across = StandPose.opposite(poseBone);
            if (i == 1) {
                StandPose.flip(poseDraft);
            } else if (across >= 0) {
                poseDraft[across] = StandPose.mirrored(poseDraft[poseBone]);
            } else {
                return true;
            }
            StandIdles.editing(draftPose());
            PlayerMenuScreen.click();
            return true;
        }
        for (int i = 0; i < POSE_BUTTONS.length; i++) {
            if (!screen.within(POSE_SLIDER_X + i * (POSE_BUTTON_W + POSE_BUTTON_GAP), POSE_BUTTON_Y, POSE_BUTTON_W, PlayerMenuScreen.REST_H)) {
                continue;
            }
            switch (i) {
                case 0 -> {
                    StandPose pose = draftPose();
                    ClientPlayerDataCache.data.poses[PlayerMenuScreen.poseSlot] = pose;
                    NetworkHandler.sendWorkshopPose(PlayerMenuScreen.poseSlot, pose);
                    if (StandIdles.chosenPose() == PlayerMenuScreen.poseSlot) {
                        StandIdles.resend();
                    }
                }
                case 1 -> {
                    poseDraft[poseBone] = new float[StandPose.CHANNELS];
                    StandIdles.editing(draftPose());
                }
                default -> closePose();
            }
            PlayerMenuScreen.click();
            return true;
        }
        return false;
    }

    boolean scrolledPoseEditor(double scrollY) {
        for (int channel = 0; channel < StandPose.CHANNELS; channel++) {
            if (screen.within(POSE_SLIDER_X, POSE_SLIDER_Y + channel * POSE_SLIDER_PITCH, POSE_SLIDER_W, POSE_SLIDER_PITCH)) {
                float step = channel < 3 ? POSE_MOVE_STEP : POSE_TURN_SCROLL;
                setPose(channel, poseDraft[poseBone][channel] + (float) Math.signum(scrollY) * step);
                return true;
            }
        }
        return false;
    }

    void renderIdles(GuiGraphics guiGraphics) {
        if (PlayerMenuScreen.poseSlot >= 0) {
            renderPoseEditor(guiGraphics, ClientPlayerDataCache.data);
            return;
        }
        if (!PlayerMenuScreen.idleScanned) {
            StandIdles.scan();
            PlayerMenuScreen.idleScanned = true;
        }
        List<StandIdles.Entry> entries = idleChoices();
        String chosen = StandIdles.chosen();
        int rows = entries.size() + 1;
        PlayerMenuScreen.idleScroll = Mth.clamp(PlayerMenuScreen.idleScroll, 0, Math.max(0, rows - PlayerMenuScreen.IDLE_ROWS));
        guiGraphics.fill(PlayerMenuScreen.IDLE_LIST_X, PlayerMenuScreen.IDLE_LIST_Y, PlayerMenuScreen.IDLE_LIST_X + PlayerMenuScreen.IDLE_LIST_W, PlayerMenuScreen.IDLE_LIST_Y + PlayerMenuScreen.IDLE_ROWS * PlayerMenuScreen.IDLE_ROW_H,
                PlayerMenuScreen.COLOR_MAP);
        for (int i = 0; i < PlayerMenuScreen.IDLE_ROWS && PlayerMenuScreen.idleScroll + i < rows; i++) {
            int index = PlayerMenuScreen.idleScroll + i;
            int y = PlayerMenuScreen.IDLE_LIST_Y + i * PlayerMenuScreen.IDLE_ROW_H;
            StandIdles.Entry entry = index == 0 ? null : entries.get(index - 1);
            boolean picked = entry == null ? chosen.isEmpty() : entry.key().equals(chosen);
            boolean over = screen.within(PlayerMenuScreen.IDLE_LIST_X, y, PlayerMenuScreen.IDLE_LIST_W, PlayerMenuScreen.IDLE_ROW_H);
            if (picked || over) {
                guiGraphics.fill(PlayerMenuScreen.IDLE_LIST_X, y, PlayerMenuScreen.IDLE_LIST_X + PlayerMenuScreen.IDLE_LIST_W, y + PlayerMenuScreen.IDLE_ROW_H,
                        picked ? PlayerMenuScreen.COLOR_ROW_PICKED : PlayerMenuScreen.COLOR_ROW_OVER);
            }
            String label = entry == null ? Component.translatable("menu.jojoha.custom.idle.default").getString()
                    : entry.label();
            screen.smallLeft(guiGraphics, label, PlayerMenuScreen.IDLE_LIST_X + 4, y + (PlayerMenuScreen.IDLE_ROW_H - screen.lineStep()) / 2 + 1, PlayerMenuScreen.IDLE_LIST_W - 8,
                    picked ? PlayerMenuScreen.COLOR_ACCENT : over ? PlayerMenuScreen.COLOR_HOVER : PlayerMenuScreen.COLOR_TEXT);
            if (over && entry != null) {
                screen.tooltip = entry.pose() >= 0
                        ? List.of(Component.literal(entry.label()).withStyle(style -> style.withColor(PlayerMenuScreen.COLOR_ACCENT)),
                                Component.translatable("menu.jojoha.custom.idle.pose.hint")
                                        .withStyle(style -> style.withColor(PlayerMenuScreen.COLOR_DIM)))
                        : entry.builtIn()
                        ? List.of(Component.literal(entry.label()).withStyle(style -> style.withColor(PlayerMenuScreen.COLOR_ACCENT)),
                                Component.translatable("menu.jojoha.custom.idle.builtin.hint")
                                        .withStyle(style -> style.withColor(PlayerMenuScreen.COLOR_DIM)))
                        : List.of(Component.literal(entry.animation()).withStyle(style -> style.withColor(PlayerMenuScreen.COLOR_ACCENT)),
                                Component.literal(entry.file()).withStyle(style -> style.withColor(PlayerMenuScreen.COLOR_DIM)));
            }
        }
        if (rows > PlayerMenuScreen.IDLE_ROWS) {
            int track = PlayerMenuScreen.IDLE_ROWS * PlayerMenuScreen.IDLE_ROW_H;
            int thumb = Math.max(8, track * PlayerMenuScreen.IDLE_ROWS / rows);
            int at = PlayerMenuScreen.IDLE_LIST_Y + (track - thumb) * PlayerMenuScreen.idleScroll / Math.max(1, rows - PlayerMenuScreen.IDLE_ROWS);
            guiGraphics.fill(PlayerMenuScreen.IDLE_LIST_X + PlayerMenuScreen.IDLE_LIST_W - 2, at, PlayerMenuScreen.IDLE_LIST_X + PlayerMenuScreen.IDLE_LIST_W, at + thumb, 0xAAFFFFFF);
        }

        screen.button(guiGraphics, IDLE_OPEN_X, IDLE_BUTTON_Y, IDLE_OPEN_W, PlayerMenuScreen.REST_H,
                Component.translatable("menu.jojoha.custom.idle.open"), true);
        screen.button(guiGraphics, IDLE_RELOAD_X, IDLE_BUTTON_Y, IDLE_RELOAD_W, PlayerMenuScreen.REST_H,
                Component.translatable("menu.jojoha.custom.idle.reload"), true);
        int editable = StandIdles.chosenPose();
        screen.button(guiGraphics, IDLE_EDIT_X, IDLE_BUTTON_Y, IDLE_EDIT_W, PlayerMenuScreen.REST_H, editable < 0
                ? Component.translatable("menu.jojoha.workshop.pose.edit_none")
                : Component.translatable("menu.jojoha.workshop.pose.edit", editable + 1), editable >= 0);
        if (screen.within(IDLE_EDIT_X, IDLE_BUTTON_Y, IDLE_EDIT_W, PlayerMenuScreen.REST_H)) {
            screen.tooltip = List.of(Component.translatable("menu.jojoha.workshop.pose.edit.hint")
                    .withStyle(style -> style.withColor(PlayerMenuScreen.COLOR_DIM)));
        }

        String note = switch (StandIdles.status()) {
            case TOO_LARGE -> "menu.jojoha.custom.idle.too_large";
            case UNREADABLE -> "menu.jojoha.custom.idle.unreadable";
            case MISSING -> "menu.jojoha.custom.idle.missing";
            default -> StandIdles.found().isEmpty() ? "menu.jojoha.custom.idle.how" : "menu.jojoha.custom.idle.bones";
        };
        screen.leftWrapped(guiGraphics, Component.translatable(note).getString(), PlayerMenuScreen.IDLE_LIST_X, IDLE_TEXT_Y, PlayerMenuScreen.IDLE_LIST_W,
                StandIdles.status() == StandIdles.Status.NONE || StandIdles.status() == StandIdles.Status.SENT
                        ? PlayerMenuScreen.COLOR_DIM : PlayerMenuScreen.COLOR_ACCENT);
    }

    static String idleLabel(String key) {
        if (key.isEmpty()) {
            return Component.translatable("menu.jojoha.custom.idle.default").getString();
        }
        for (StandIdles.Entry entry : idleChoices()) {
            if (entry.key().equals(key)) {
                return entry.label();
            }
        }
        return key.substring(key.lastIndexOf('#') + 1);
    }

    private static List<StandIdles.Entry> idleChoices() {
        List<StandIdles.Entry> choices = new ArrayList<>(StandIdles.BUILT_IN);
        choices.addAll(StandIdles.POSES);
        choices.addAll(StandIdles.found());
        return choices;
    }

    boolean clickedIdles(int button) {
        if (PlayerMenuScreen.poseSlot >= 0) {
            return clickedPoseEditor(button);
        }
        if (button != 0) {
            return false;
        }
        if (screen.within(IDLE_EDIT_X, IDLE_BUTTON_Y, IDLE_EDIT_W, PlayerMenuScreen.REST_H)) {
            if (StandIdles.chosenPose() >= 0) {
                openPose(StandIdles.chosenPose());
                PlayerMenuScreen.click();
            }
            return true;
        }
        if (screen.within(IDLE_OPEN_X, IDLE_BUTTON_Y, IDLE_OPEN_W, PlayerMenuScreen.REST_H)) {
            Util.getPlatform().openPath(StandIdles.folder());
            PlayerMenuScreen.click();
            return true;
        }
        if (screen.within(IDLE_RELOAD_X, IDLE_BUTTON_Y, IDLE_RELOAD_W, PlayerMenuScreen.REST_H)) {
            StandIdles.scan();
            PlayerMenuScreen.idleScanned = true;
            PlayerMenuScreen.click();
            return true;
        }
        List<StandIdles.Entry> entries = idleChoices();
        for (int i = 0; i < PlayerMenuScreen.IDLE_ROWS && PlayerMenuScreen.idleScroll + i <= entries.size(); i++) {
            if (screen.within(PlayerMenuScreen.IDLE_LIST_X, PlayerMenuScreen.IDLE_LIST_Y + i * PlayerMenuScreen.IDLE_ROW_H, PlayerMenuScreen.IDLE_LIST_W, PlayerMenuScreen.IDLE_ROW_H)) {
                int index = PlayerMenuScreen.idleScroll + i;
                StandIdles.choose(index == 0 ? null : entries.get(index - 1));
                PlayerMenuScreen.click();
                return true;
            }
        }
        return false;
    }
}
