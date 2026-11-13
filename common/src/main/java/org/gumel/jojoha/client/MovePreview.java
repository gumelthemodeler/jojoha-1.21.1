package org.gumel.jojoha.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.client.anim.BedrockAnimation;
import org.gumel.jojoha.client.anim.ClipContact;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.hamon.HamonMoves;
import org.gumel.jojoha.registry.ModRegistries;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.StandShadowForm;
import org.gumel.jojoha.stand.StandTypes;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class MovePreview {
    private record Clip(boolean stand, String name, String body) {
        Clip shownFor(JojohaPlayerData data) {
            return stand && body != null && !data.standOut() ? new Clip(false, body, null) : this;
        }
    }

    private enum Style { STRIKE, BARRAGE, SHOT, TIME, FLAME, HAMON, BLOOD, GUST, AURA }

    private static final Map<String, Clip> CLIPS = Map.ofEntries(
            Map.entry("afterimages", new Clip(true, "afterimage", null)),
            Map.entry("armor_release", new Clip(true, "armor_release", null)),
            Map.entry("atmospheric_rift", new Clip(false, "atmospheric_rift", null)),
            Map.entry("balestra", new Clip(true, "balestra", null)),
            Map.entry("barrage", new Clip(true, "barrage", "barrage")),
            Map.entry("beatdown", new Clip(true, "beatdown", null)),
            Map.entry("blade_flash", new Clip(false, "blinding_flash", null)),
            Map.entry("blade_launch", new Clip(true, "blade_launch", null)),
            Map.entry("blitz", new Clip(false, "blitz", null)),
            Map.entry("blood_boil", new Clip(false, "blood_boil", null)),
            Map.entry("body_possession", new Clip(true, "hg_possession_remake", null)),
            Map.entry("boiling_blood_needles", new Clip(false, "boiling_blood_needles", null)),
            Map.entry("bottle_cap_shot", new Clip(false, "throw", null)),
            Map.entry("brilliant_blades", new Clip(false, "brilliant_blades", null)),
            Map.entry("bubble_cutter", new Clip(false, "bubble_cutter", null)),
            Map.entry("bubble_launcher", new Clip(false, "bubble_launcher", null)),
            Map.entry("burst", new Clip(false, "leap", null)),
            Map.entry("camera_crush", new Clip(false, "throw", null)),
            Map.entry("clacker_volley", new Clip(false, "clacker_spin", null)),
            Map.entry("containment", new Clip(true, "inferno", null)),
            Map.entry("controlled_blaze", new Clip(true, "controlled_blaze", null)),
            Map.entry("crossfire_hurricane", new Clip(true, "cross_fire_hurricane", null)),
            Map.entry("crossfire_hurricane_special", new Clip(true, "cross_fire_hurricane_special", null)),
            Map.entry("crying_vent", new Clip(false, "crying_vent", null)),
            Map.entry("deflection", new Clip(true, "deflection", null)),
            Map.entry("divine_sandstorm", new Clip(false, "divine_sandstorm", null)),
            Map.entry("double_jump", new Clip(false, "doublejump", null)),
            Map.entry("drain", new Clip(false, "drain", null)),
            Map.entry("draw", new Clip(false, "rapier_lunge", null)),
            Map.entry("emerald_splash_barrage", new Clip(true, "barrage", null)),
            Map.entry("emerald_splash_shotgun_blast", new Clip(true, "hg_shotgun_remake", null)),
            Map.entry("erasure", new Clip(true, "timestop_1", null)),
            Map.entry("flesh_bud", new Clip(false, "fleshbud", null)),
            Map.entry("forced_ripple_breathing", new Clip(false, "heal_hold", null)),
            Map.entry("gale_cut", new Clip(false, "gale_cut", null)),
            Map.entry("grab", new Clip(true, "grab", null)),
            Map.entry("ground_breaker", new Clip(true, "ground_breaker", null)),
            Map.entry("ground_slam", new Clip(true, "ground_breaker", null)),
            Map.entry("gut_punch", new Clip(false, "gut_punch", null)),
            Map.entry("hamon_overdrive", new Clip(false, "overdrive", null)),
            Map.entry("heat_body", new Clip(false, "heat_body", null)),
            Map.entry("heaven_step", new Clip(true, "punch2", null)),
            Map.entry("heaven_time_stop", new Clip(true, "timestop_1", null)),
            Map.entry("heavens_uppercut", new Clip(true, "uppercut", null)),
            Map.entry("heavy_punch", new Clip(true, "punch2", null)),
            Map.entry("hermit_grapple", new Clip(false, "thorn_zip_start", null)),
            Map.entry("inferno", new Clip(true, "inferno", null)),
            Map.entry("infiltrate", new Clip(false, "palm", null)),
            Map.entry("infinite_inferno_fire", new Clip(true, "infinite_inferno_fire", null)),
            Map.entry("infinite_servitude", new Clip(true, "grab", null)),
            Map.entry("inhale", new Clip(true, "inhale", null)),
            Map.entry("judgement_fist", new Clip(true, "punch", null)),
            Map.entry("kick_flurry", new Clip(true, "kick_barrage", null)),
            Map.entry("knife_barrage", new Clip(true, "knife_throw", null)),
            Map.entry("knife_halo", new Clip(true, "knife_throw", null)),
            Map.entry("lasso_of_thorns", new Clip(false, "throw", null)),
            Map.entry("life_sense", new Clip(true, "life_sense", null)),
            Map.entry("miniature_trap", new Clip(true, "hg_mine_placement_remake", null)),
            Map.entry("muda_rush", new Clip(true, "punch", null)),
            Map.entry("next_line", new Clip(false, "guard", null)),
            Map.entry("red_bind", new Clip(true, "red_bind", null)),
            Map.entry("restoration", new Clip(true, "star_finger", null)),
            Map.entry("rib_blades", new Clip(false, "rib_blades", null)),
            Map.entry("riposte", new Clip(true, "deflection", null)),
            Map.entry("ripple_barrier", new Clip(false, "ripple_barrier", null)),
            Map.entry("ripple_pulse", new Clip(false, "ripple_pulse", null)),
            Map.entry("ripple_rebuke", new Clip(false, "guard", null)),
            Map.entry("sabre_cycle", new Clip(true, "sabre_cycle", null)),
            Map.entry("saw_grip", new Clip(false, "saw_grip", null)),
            Map.entry("scarlet_overdrive", new Clip(false, "gut_punch", null)),
            Map.entry("shining_slash", new Clip(false, "shining_slash", null)),
            Map.entry("ultimate_strength", new Clip(false, "ultimate_strength", null)),
            Map.entry("skull_crusher", new Clip(true, "grab", null)),
            Map.entry("snake_muffler", new Clip(false, "throw", null)),
            Map.entry("space_ripper_stingy_eyes", new Clip(false, "space_ripper_stingy_eyes", null)),
            Map.entry("stand_leap", new Clip(true, "punch2", "leap")),
            Map.entry("star_finger", new Clip(true, "star_finger", null)),
            Map.entry("subzero", new Clip(false, "subzero", null)),
            Map.entry("sunlight_yellow_overdrive", new Clip(false, "barrage", null)),
            Map.entry("sweep_kick", new Clip(true, "sweep_kick", null)),
            Map.entry("sword_flurry", new Clip(true, "sword_flurry", null)),
            Map.entry("tendril_spike", new Clip(true, "hg_spike_alt", null)),
            Map.entry("thorn_whip", new Clip(false, "throw", null)),
            Map.entry("thorn_zip", new Clip(false, "web_zip_start", null)),
            Map.entry("thunder_cross_split_attack", new Clip(false, "thunder_cross_split_attack", null)),
            Map.entry("time_stop", new Clip(true, "timestop_1", null)),
            Map.entry("time_stop_extended", new Clip(true, "timestop_1", null)),
            Map.entry("time_stop_perfect", new Clip(true, "timestop_1", null)),
            Map.entry("tornado", new Clip(false, "tornado", null)),
            Map.entry("turquoise_blue_overdrive", new Clip(false, "palm", null)),
            Map.entry("twenty_meter_emerald_splash", new Clip(true, "hg_chudsplash", null)),
            Map.entry("twisting_gut_punch", new Clip(true, "punch2", null)),
            Map.entry("unravel_trap", new Clip(true, "hg_mine_placement_remake", null)),
            Map.entry("uppercut", new Clip(true, "uppercut", "uppercut")),
            Map.entry("vampiric_drive", new Clip(false, "vampiric_drive", null)),
            Map.entry("vaporous_freeze", new Clip(false, "gut_punch", null)),
            Map.entry("vessel_grab", new Clip(false, "vessel_grab", null)),
            Map.entry("web_swing", new Clip(false, "thorn_zip_start", null)),
            Map.entry("wind_cloak", new Clip(false, "wind_cloak", null)),
            Map.entry("youthful_breath", new Clip(false, "overdrive", null)),
            Map.entry("zoom_punch", new Clip(false, "zoom_punch", null)));

    private static final Set<String> BARRAGES = Set.of("barrage", "emerald_splash_barrage", "kick_flurry", "sword_flurry",
            "muda_rush", "sunlight_yellow_overdrive", "knife_barrage", "beatdown");
    private static final Set<String> SHOTS = Set.of("star_finger", "emerald_splash_shotgun_blast",
            "twenty_meter_emerald_splash", "bottle_cap_shot", "space_ripper_stingy_eyes", "knife_halo", "blade_launch",
            "zoom_punch", "bubble_launcher", "bubble_cutter", "tendril_spike", "thorn_whip", "lasso_of_thorns",
            "gale_cut", "rib_blades", "boiling_blood_needles", "clacker_volley", "snake_muffler", "camera_crush",
            "hermit_grapple", "web_swing", "thorn_zip", "restoration");
    private static final Set<String> TIMES = Set.of("time_stop", "time_stop_extended", "time_stop_perfect",
            "heaven_time_stop", "erasure", "time_skip");
    private static final Set<String> FLAMES = Set.of("controlled_blaze", "crossfire_hurricane",
            "crossfire_hurricane_special", "red_bind", "inferno", "infinite_inferno_fire", "containment",
            "scarlet_overdrive", "heat_body", "brilliant_blades");
    private static final Set<String> BLOODS = Set.of("blitz", "blood_boil", "drain", "flesh_bud", "vampiric_drive",
            "saw_grip", "vessel_grab", "crying_vent");
    private static final Set<String> GUSTS = Set.of("atmospheric_rift", "divine_sandstorm", "tornado", "wind_cloak",
            "subzero", "vaporous_freeze");
    private static final Set<String> AURAS = Set.of("inhale", "life_sense", "afterimages", "armor_release",
            "body_possession", "deflection", "riposte", "grab", "skull_crusher", "infinite_servitude",
            "next_line", "ripple_rebuke");

    private static final int PANE_W = 44;
    private static final int PANE_H = 56;
    private static final float FILL = 0.42F;
    private static final long BARRAGE_LOOP_MS = 2500L;
    private static final int SHOWN_FROM_TICKS = 40;
    private static final long MIN_LOOP_MS = 1400L;
    private static final long LOOP_REST_MS = 600L;
    private static final float STAND_REACH = 1.3F;
    private static final float PLAYER_REACH = 1.0F;
    private static final float SHOT_RANGE = 3.5F;
    private static final float SHOT_SECONDS = 0.3F;
    private static final float STRIKE_SECONDS = 0.25F;
    private static final float TIME_SECONDS = 1.2F;
    private static final float FLAME_SECONDS = 0.9F;
    private static final float BLOOD_SECONDS = 0.4F;
    private static final float GUST_SECONDS = 0.8F;
    private static final Set<String> ATTACK_CLIPS = Set.of("punch", "punch2", "uppercut", "kick", "grab", "grab2");
    private static final ResourceLocation UNIVERSAL = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "animations/universal.animation.json");
    private static final ResourceLocation REFERENCE = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "animations/star_platinum.animation.json");
    private static final Map<String, Optional<BedrockAnimation>> LOADED = new HashMap<>();
    private static final Quaternionf FACING = new Quaternionf().rotateZ((float) Math.PI)
            .rotateY((float) Math.toRadians(225));

    private static final ResourceLocation STAR = fx("fx/impact_star");
    private static final ResourceLocation STREAK = fx("fx/speed_streak");
    private static final ResourceLocation FLAME = fx("fx/magician_flame");
    private static final ResourceLocation[] RING = frames("fx/circle_medium_", 1, 5);
    private static final ResourceLocation[] HIT = frames("fx/hitmarker", 1, 8);
    private static final ResourceLocation[] SPARK = frames("particle/hamon_electricity_particle", 1, 9);
    private static final ResourceLocation[] BLOOD = frames("particle/blood", 1, 7);
    private static final ResourceLocation[] GUST = frames("particle/pillar_gust_", 0, 3);
    private static final ResourceLocation[] WISP = frames("particle/stand_aura_", 0, 8);
    private static final int HAMON_YELLOW = 0xFFDE33;
    private static final int BLOOD_RED = 0xC0141F;
    private static final int GUST_WHITE = 0xE8E4D8;

    private static StandEntity stand;
    private static RemotePlayer body;
    private static String shown;
    private static long startedMs;
    private static long restingSince;
    private static long lastTick;

    private MovePreview() {
    }

    private static ResourceLocation fx(String path) {
        return ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/" + path + ".png");
    }

    private static ResourceLocation[] frames(String prefix, int first, int count) {
        ResourceLocation[] out = new ResourceLocation[count];
        for (int i = 0; i < count; i++) {
            out[i] = fx(prefix + (first + i));
        }
        return out;
    }

    public static boolean has(JojohaPlayerData data, ResourceLocation move) {
        Clip clip = move == null || data == null ? null : CLIPS.get(move.getPath());
        clip = clip == null ? null : clip.shownFor(data);
        return clip != null && (!clip.stand || data.stand.isPresent()
                && StandTypes.byIdOrDefault(data.stand.standId()).form().hasBody());
    }

    public static JojohaTooltip.Pane pane(JojohaPlayerData data, ResourceLocation move) {
        if (!has(data, move)) {
            return null;
        }
        return new JojohaTooltip.Pane() {
            @Override
            public int width() {
                return PANE_W;
            }

            @Override
            public int height() {
                return PANE_H;
            }

            @Override
            public void draw(GuiGraphics graphics, int x, int y, int w, int h) {
                render(graphics, data, move, x, y, w, h);
            }
        };
    }

    private static void render(GuiGraphics graphics, JojohaPlayerData data, ResourceLocation move,
                               int x, int y, int w, int h) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null) {
            return;
        }
        Clip clip = CLIPS.get(move.getPath()).shownFor(data);
        LivingEntity actor = clip.stand ? standFor(level, data) : bodyFor(level);
        Style style = styleOf(move.getPath(), clip);
        BedrockAnimation anim = clip.stand ? standClip(data, clip.name) : playerClip(clip.name);
        float length = anim == null ? 1F : Math.max(0.2F, anim.lengthSeconds());
        float[] contact = anim == null ? new float[] {length * 0.4F, length * 0.4F} : ClipContact.of(anim);
        double speed = clip.stand ? speedOf(clip.name) : 1.0;

        long now = Util.getMillis();
        String showing = move + (clip.stand ? "#stand" : "#body");
        boolean fresh = !showing.equals(shown);
        if (fresh) {
            shown = showing;
            startedMs = now;
            restingSince = -1L;
            lastTick = now;
        }

        float seconds;
        if (clip.stand) {
            long due = (now - lastTick) / 50L;
            if (due > 0) {
                stand.tickCount += (int) Math.min(due, 4L);
                lastTick += due * 50L;
            }
            long loopMs = style == Style.BARRAGE ? BARRAGE_LOOP_MS
                    : Math.max(MIN_LOOP_MS, Math.round(length / speed * 1000.0) + LOOP_REST_MS);
            if (fresh || now - startedMs >= loopMs) {
                startedMs = now;
                stand.triggerAnim(StandEntity.CONTROLLER, clip.name);
            }
            seconds = (float) ((now - startedMs) / 1000.0 * speed);
        } else {
            float gameNow = (float) level.getGameTime();
            if (fresh) {
                PlayerMoveAnimation.begin(body.getId(), clip.name, gameNow);
            } else if (!PlayerMoveAnimation.playing(body, clip.name)) {
                if (restingSince < 0L) {
                    restingSince = now;
                } else if (now - restingSince >= LOOP_REST_MS) {
                    restingSince = -1L;
                    PlayerMoveAnimation.begin(body.getId(), clip.name, gameNow);
                }
            }
            seconds = PlayerMoveAnimation.playing(body, clip.name)
                    ? PlayerMoveAnimation.progress(body, clip.name) * length : -1F;
        }

        float size = h * FILL / Math.max(1F, actor.getBbHeight());
        Projector view = new Projector(0F, 0F, size, actor.getBbHeight() / 2F);
        float chest = actor.getBbHeight() * 0.6F;
        float reach = clip.stand ? STAND_REACH : PLAYER_REACH;
        float[] near = view.at(0F, chest, 0F);
        float[] far = view.at(0F, chest, reach);
        float shift = Mth.clamp(-(far[0] - near[0]) / 2F, -w * 0.25F, w * 0.25F);
        view = new Projector(x + w / 2F + shift, y + h * 0.6F, size, actor.getBbHeight() / 2F);

        graphics.enableScissor(x, y, x + w, y + h);
        float[] saved = StandPortrait.hold(actor, 0F);
        InventoryScreen.renderEntityInInventory(graphics, view.originX, view.originY, size,
                new Vector3f(0F, view.lift, 0F), FACING, null, actor);
        StandPortrait.release(actor, saved);

        int aura = data.stand.isPresent()
                ? StandShadowForm.auraColourFor(data.stand) & 0xFFFFFF : 0xFFFFFF;
        graphics.pose().pushPose();
        graphics.pose().translate(0F, 0F, 200F);
        RenderSystem.enableBlend();
        effects(graphics, style, seconds, contact, length, now, view, chest, reach, x, y, w, h,
                clip.stand ? aura : HamonMoves.byId(move) != null ? HAMON_YELLOW : 0xFFFFFF, clip.stand);
        graphics.setColor(1F, 1F, 1F, 1F);
        graphics.pose().popPose();
        graphics.disableScissor();
    }

    private static final class Projector {
        final float originX;
        final float originY;
        final float size;
        final float lift;

        Projector(float originX, float originY, float size, float lift) {
            this.originX = originX;
            this.originY = originY;
            this.size = size;
            this.lift = lift;
        }

        float[] at(float px, float py, float pz) {
            Vector3f point = FACING.transform(new Vector3f(px, py, pz));
            return new float[] {originX + size * point.x, originY + size * (lift + point.y)};
        }
    }

    private static double speedOf(String clip) {
        if (ATTACK_CLIPS.contains(clip)) {
            return StandEntity.ATTACK_ANIM_SPEED;
        }
        if (clip.startsWith("hit")) {
            return StandEntity.CHARIOT_HIT_ANIM_SPEED;
        }
        return 1.0;
    }

    private static BedrockAnimation standClip(JojohaPlayerData data, String name) {
        ResourceLocation own = StandTypes.byIdOrDefault(data.stand.standId()).animation();
        for (ResourceLocation file : new ResourceLocation[] {own, UNIVERSAL, REFERENCE}) {
            BedrockAnimation found = LOADED.computeIfAbsent(file + "#" + name,
                    key -> BedrockAnimation.loadGeckoLib(file, name)).orElse(null);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static BedrockAnimation playerClip(String name) {
        ResourceLocation file = PlayerMoveAnimation.fileOf(name);
        return LOADED.computeIfAbsent(file + "#" + name, key -> BedrockAnimation.load(file, name)).orElse(null);
    }

    private static Style styleOf(String id, Clip clip) {
        if (TIMES.contains(id)) {
            return Style.TIME;
        }
        if (FLAMES.contains(id)) {
            return Style.FLAME;
        }
        if (BARRAGES.contains(id) || clip.name.contains("barrage") || clip.name.contains("flurry")) {
            return Style.BARRAGE;
        }
        if (SHOTS.contains(id)) {
            return Style.SHOT;
        }
        if (BLOODS.contains(id)) {
            return Style.BLOOD;
        }
        if (GUSTS.contains(id)) {
            return Style.GUST;
        }
        if (AURAS.contains(id)) {
            return Style.AURA;
        }
        if (!clip.stand && HamonMoves.byId(
                ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, id)) != null) {
            return Style.HAMON;
        }
        return Style.STRIKE;
    }

    private static void effects(GuiGraphics graphics, Style style, float seconds, float[] contact, float length,
                                long now, Projector view, float chest, float reach, int x, int y, int w, int h,
                                int colour, boolean stand) {
        float px = w / (float) PANE_W;
        long tick = now / 50L;
        RandomSource random = RandomSource.create(tick / 2L);
        float[] body = view.at(0F, chest, 0F);
        float[] target = view.at(0F, chest, reach);

        if (stand) {
            for (int i = 0; i < 4; i++) {
                float t = ((tick + i * 5L) % 20L) / 20F;
                float side = (i - 1.5F) * 0.18F;
                float[] at = view.at(side, chest - 0.5F + t * 1.2F, -0.1F);
                sprite(graphics, WISP[Math.min(7, (int) (t * 8))], Math.round(at[0]), Math.round(at[1]),
                        Math.round(6 * px), colour, 0.5F * (1F - t));
            }
        }
        if (seconds < 0F) {
            return;
        }
        float since = seconds - contact[0];
        float held = Math.max(contact[1] - contact[0], 0F);

        switch (style) {
            case STRIKE -> strike(graphics, since, target, px, colour);
            case BARRAGE -> {
                float until = held > 0.1F ? held : BARRAGE_LOOP_MS / 1000F - contact[0] - 0.3F;
                if (since >= 0F && since <= until) {
                    for (int i = 0; i < 3; i++) {
                        float[] at = view.at((random.nextFloat() - 0.5F) * 0.7F,
                                chest + (random.nextFloat() - 0.5F) * 0.8F, reach);
                        int frame = (int) ((tick + i * 3L) % HIT.length);
                        sprite(graphics, HIT[frame], Math.round(at[0]), Math.round(at[1]), Math.round(9 * px), colour, 0.9F);
                    }
                }
            }
            case SHOT -> {
                float flight = SHOT_SECONDS;
                if (since >= 0F && since < flight) {
                    float t = since / flight;
                    float[] from = view.at(0F, chest, 0.5F);
                    float[] to = view.at(0F, chest, SHOT_RANGE);
                    float sx = Mth.lerp(t, from[0], to[0]);
                    float sy = Mth.lerp(t, from[1], to[1]);
                    float angle = (float) Math.toDegrees(Math.atan2(to[1] - from[1], to[0] - from[0])) + 90F;
                    graphics.pose().pushPose();
                    graphics.pose().translate(sx, sy, 0F);
                    graphics.pose().mulPose(Axis.ZP.rotationDegrees(angle));
                    int lw = Math.round(5 * px);
                    int lh = Math.round(16 * px);
                    tint(graphics, colour, 1F);
                    graphics.blit(STREAK, -lw / 2, -lh / 2, lw, lh, Math.min(2, (int) (t * 3)) * 8F, 0F, 8, 32, 24, 32);
                    graphics.pose().popPose();
                }
                strike(graphics, since - flight, view.at(0F, chest, SHOT_RANGE), px * 0.8F, colour);
            }
            case TIME -> {
                if (since >= 0F && since < TIME_SECONDS) {
                    float t = since / TIME_SECONDS;
                    graphics.fill(x, y, x + w, y + h, (Math.round(0x70 * Mth.sin(t * Mth.PI)) << 24) | 0x5A5A78);
                    int frame = Math.min(RING.length - 1, (int) (t * RING.length));
                    sprite(graphics, RING[frame], Math.round(body[0]), Math.round(body[1]),
                            Math.round((14 + t * 40) * px), 0xFFFFFF, 1F - t * 0.6F);
                }
            }
            case FLAME -> {
                if (since >= 0F && since < FLAME_SECONDS) {
                    for (int i = 0; i < 5; i++) {
                        float t = ((since / FLAME_SECONDS) * 2F + i / 5F) % 1F;
                        float[] at = view.at((random.nextFloat() - 0.5F) * 0.3F, chest + t * 0.3F, 0.4F + t * reach * 1.4F);
                        int frame = (int) ((tick + i * 2L) % 8L);
                        int s = Math.round((7 + t * 6) * px);
                        tint(graphics, 0xFFFFFF, 1F - t * 0.4F);
                        graphics.blit(FLAME, Math.round(at[0]) - s / 2, Math.round(at[1]) - s / 2, s, s,
                                frame * 16F, 32F, 16, 16, 128, 80);
                    }
                }
            }
            case HAMON -> {
                float[] hand = view.at(0F, chest, reach * 0.5F);
                for (int i = 0; i < 2; i++) {
                    int frame = (int) ((tick + i * 4L) % SPARK.length);
                    sprite(graphics, SPARK[frame], Math.round(hand[0] + (random.nextFloat() - 0.5F) * 10 * px),
                            Math.round(hand[1] + (random.nextFloat() - 0.5F) * 10 * px), Math.round(8 * px), HAMON_YELLOW, 0.9F);
                }
                strike(graphics, since, target, px, HAMON_YELLOW);
            }
            case BLOOD -> {
                strike(graphics, since, target, px * 0.8F, BLOOD_RED);
                if (since >= 0F && since < BLOOD_SECONDS) {
                    float t = since / BLOOD_SECONDS;
                    for (int i = 0; i < 3; i++) {
                        sprite(graphics, BLOOD[Math.min(6, (int) (t * 7))],
                                Math.round(target[0] + (i - 1) * 5 * px), Math.round(target[1] + (t * 14 + i * 2) * px),
                                Math.round(7 * px), 0xFFFFFF, 1F - t * 0.5F);
                    }
                }
            }
            case GUST -> {
                if (since >= 0F && since < GUST_SECONDS) {
                    for (int i = 0; i < 3; i++) {
                        float t = (since / GUST_SECONDS + i / 3F) % 1F;
                        float[] at = view.at((i - 1) * 0.35F, chest + (i - 1) * 0.3F, 0.3F + t * reach * 2F);
                        sprite(graphics, GUST[(int) ((tick + i) % GUST.length)], Math.round(at[0]), Math.round(at[1]),
                                Math.round(10 * px), GUST_WHITE, 0.9F * (1F - t * 0.5F));
                    }
                }
            }
            case AURA -> {
                float pulse = 0.5F + 0.5F * Mth.sin(seconds * Mth.TWO_PI);
                int frame = (int) ((tick / 2L) % RING.length);
                sprite(graphics, RING[frame], Math.round(body[0]), Math.round(body[1]),
                        Math.round((26 + pulse * 6) * px), colour, 0.35F);
            }
        }
    }

    private static void strike(GuiGraphics graphics, float since, float[] at, float px, int colour) {
        if (since < 0F || since >= STRIKE_SECONDS) {
            return;
        }
        float t = since / STRIKE_SECONDS;
        int cx = Math.round(at[0]);
        int cy = Math.round(at[1]);
        int s = Math.round(18 * px);
        tint(graphics, colour, 1F);
        graphics.blit(STAR, cx - s / 2, cy - s / 2, s, s, Math.min(3, (int) (t * 4)) * 32F, 0F, 32, 32, 128, 32);
        sprite(graphics, RING[Math.min(RING.length - 1, (int) (t * RING.length))], cx, cy,
                Math.round((10 + t * 16) * px), colour, 1F - t);
    }

    private static void sprite(GuiGraphics graphics, ResourceLocation texture, int cx, int cy, int size, int colour,
                               float alpha) {
        if (size <= 0 || alpha <= 0.01F) {
            return;
        }
        tint(graphics, colour, alpha);
        graphics.blit(texture, cx - size / 2, cy - size / 2, size, size, 0F, 0F, 1, 1, 1, 1);
    }

    private static void tint(GuiGraphics graphics, int colour, float alpha) {
        graphics.setColor(((colour >> 16) & 0xFF) / 255F, ((colour >> 8) & 0xFF) / 255F, (colour & 0xFF) / 255F,
                Mth.clamp(alpha, 0F, 1F));
    }

    public static void idle() {
        if (shown != null && body != null) {
            PlayerMoveAnimation.begin(body.getId(), "", 0F);
        }
        shown = null;
    }

    private static StandEntity standFor(ClientLevel level, JojohaPlayerData data) {
        if (stand == null || stand.level() != level) {
            stand = new StandEntity(ModRegistries.STAND.get(), level);
            stand.setInvisible(false);
            stand.setPreviewMode(true);
            stand.tickCount = SHOWN_FROM_TICKS;
        }
        stand.setTrustTier(data.stand.trust());
        stand.setStandType(data.stand.standId());
        stand.setSkin(data.stand.skin());
        return stand;
    }

    private static RemotePlayer bodyFor(ClientLevel level) {
        Minecraft minecraft = Minecraft.getInstance();
        if (body == null || body.level() != level) {
            body = new RemotePlayer(level, minecraft.player.getGameProfile());
        }
        return body;
    }
}
