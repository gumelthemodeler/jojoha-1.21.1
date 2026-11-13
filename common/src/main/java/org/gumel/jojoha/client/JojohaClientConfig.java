package org.gumel.jojoha.client;

import org.gumel.jojoha.config.ChoiceSetting;
import org.gumel.jojoha.config.ConfigFile;
import org.gumel.jojoha.config.NumberSetting;
import org.gumel.jojoha.config.ToggleSetting;

public final class JojohaClientConfig {
    public static final ConfigFile FILE = new ConfigFile("jojoha-client.json", false);

    public static final ToggleSetting CENTRAL_BAR_MODE = FILE.toggle("combat_bar", "centralBarMode",
            "central_bar_mode", false);
    public static final ChoiceSetting<BarCorner> BAR_CORNER = FILE.choice("combat_bar", "barCorner",
            "bar_corner", BarCorner.class, BarCorner.TOP_LEFT);
    public static final NumberSetting BAR_OFFSET_X = FILE.number("combat_bar", "barOffsetX",
            "bar_offset_x", 0F, -200F, 200F, 1F, NumberSetting.Unit.PIXELS);
    public static final NumberSetting BAR_OFFSET_Y = FILE.number("combat_bar", "barOffsetY",
            "bar_offset_y", 0F, -200F, 200F, 1F, NumberSetting.Unit.PIXELS);

    public static final ToggleSetting IMPACT_FRAMES = FILE.toggle("effects", "impactFrames",
            "impact_frames", true);
    public static final ToggleSetting CAMERA_MOTION = FILE.toggle("effects", "cameraMotion",
            "camera_motion", true);
    public static final ToggleSetting IMPACT_WORDS = FILE.toggle("effects", "impactWords",
            "impact_words", true);
    public static final ToggleSetting PALETTE_SWAPS = FILE.toggle("effects", "paletteSwaps",
            "palette_swaps", true);
    public static final NumberSetting PALETTE_STRENGTH = FILE.number("effects", "paletteStrength",
            "palette_strength", 0.8F, 0.1F, 1F, 0.1F, NumberSetting.Unit.TIMES);
    public static final ChoiceSetting<GoreLevel> GORE = FILE.choice("effects", "gore",
            "gore", GoreLevel.class, GoreLevel.FULL);
    public static final ToggleSetting SUMMON_FLAIR = FILE.toggle("effects", "summonFlair",
            "summon_flair", true);

    public static final NumberSetting SAND_SHADE = FILE.number("terrain", "sandShade",
            "sand_shade", 1F, 0F, 2F, 0.1F, NumberSetting.Unit.TIMES);

    public static final NumberSetting BARRAGE_TRAILS = FILE.number("barrage", "barrageTrails",
            "barrage_trails", StandModel.TRAIL_COPIES, 0F, StandModel.TRAIL_COPIES_MAX, 1F, NumberSetting.Unit.WHOLE);
    public static final NumberSetting BARRAGE_TRAIL_OPACITY = FILE.number("barrage", "barrageTrailOpacity",
            "barrage_trail_opacity", 1F, 0F, 2F, 0.1F, NumberSetting.Unit.TIMES);
    public static final ToggleSetting BARRAGE_AIR = FILE.toggle("barrage", "barrageAir",
            "barrage_air", true);
    public static final NumberSetting BARRAGE_RECOIL = FILE.number("barrage", "barrageRecoil",
            "barrage_recoil", 1F, 0F, 2F, 0.1F, NumberSetting.Unit.TIMES);
    public static final NumberSetting BARRAGE_ECHO = FILE.number("barrage", "barrageEcho",
            "barrage_echo", 5F, 0F, 10F, 1F, NumberSetting.Unit.WHOLE);
    public static final ToggleSetting BARRAGE_PHANTOMS = FILE.toggle("barrage", "barragePhantoms",
            "barrage_phantoms", true);
    public static final NumberSetting BARRAGE_GLOW = FILE.number("barrage", "barrageGlow",
            "barrage_glow", 1F, 0F, 2F, 0.1F, NumberSetting.Unit.TIMES);
    public static final NumberSetting BARRAGE_SMEARS = FILE.number("barrage", "barrageSmears",
            "barrage_smears", 1F, 0F, 2F, 0.1F, NumberSetting.Unit.TIMES);
    public static final ToggleSetting BARRAGE_WORDS = FILE.toggle("barrage", "barrageWords",
            "barrage_words", true);
    public static final ToggleSetting BARRAGE_HIT_SOUNDS = FILE.toggle("barrage", "barrageHitSounds",
            "barrage_hit_sounds", true);

    static {
        BAR_CORNER.onlyWhen(() -> !CENTRAL_BAR_MODE.on());
        BAR_OFFSET_X.onlyWhen(() -> !CENTRAL_BAR_MODE.on());
        BAR_OFFSET_Y.onlyWhen(() -> !CENTRAL_BAR_MODE.on());
        PALETTE_STRENGTH.onlyWhen(PALETTE_SWAPS::on);
    }

    private JojohaClientConfig() {
    }

    public static boolean centralBarMode() {
        return CENTRAL_BAR_MODE.on();
    }

    public static void setCentralBarMode(boolean value) {
        CENTRAL_BAR_MODE.set(value);
        FILE.save();
    }

    public static BarCorner barCorner() {
        return BAR_CORNER.get();
    }

    public static int barOffsetX() {
        return BAR_OFFSET_X.whole();
    }

    public static int barOffsetY() {
        return BAR_OFFSET_Y.whole();
    }

    public static boolean impactFrames() {
        return IMPACT_FRAMES.on();
    }

    public static void setImpactFrames(boolean value) {
        IMPACT_FRAMES.set(value);
        FILE.save();
    }

    public static boolean cameraMotion() {
        return CAMERA_MOTION.on();
    }

    public static void setCameraMotion(boolean value) {
        CAMERA_MOTION.set(value);
        FILE.save();
    }
}
