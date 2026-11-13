package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class TimeStopShader {
    private static final float SETTLE_SECONDS = 0.9F;

    private static final float SETTLE_SHARE = 0.3F;

    private static final float BREAK_OUT_SECONDS = 1.6F;

    private static final float SHORE_DEPTH = 0.1F;

    private static final float WAKE_BLOCKS = 8F;
    private static final float WAKE_SHARE = 0.6F;
    private static final float GLOBAL_WAKE_BLOCKS = 40F;

    private static final float WARP_PUSH = 1.8F;
    private static final float WARP_WIDTH = 5F;
    private static final float GLOBAL_WARP_WIDTH = 8F;

    private static final float SKY_SHARE = 0.55F;

    private static final float RING_WIDTH = 1.1F;
    private static final float RING_STRENGTH = 0.55F;

    private static final float DESATURATION = 0.80F;
    private static final float DARKEN = 0.88F;

    private static final float HUE_ROLL = 0.07F;
    private static final float HUE_RADIANS_PER_SECOND = 9.0F;

    private static final float SQUIGGLE = 0.008F;
    private static final float WAVE_SCALE = 0.55F;
    private static final float WAVE_RADIANS_PER_SECOND = 6.0F;

    private static final float MAX_FRAME_SECONDS = 0.1F;

    private static boolean stoppedNow;

    private static boolean live;

    private static float stoppedSeconds;
    private static float releasedSeconds;
    private static long lastFrameNanos;

    private static float centreX;
    private static float centreY;
    private static float centreZ;

    private TimeStopShader() {
    }

    public static void setPhase(boolean stopped) {
        stoppedNow = stopped;
    }

    public static void clear() {
        stoppedNow = false;
        live = false;
        stoppedSeconds = 0F;
        releasedSeconds = 0F;
        lastFrameNanos = 0L;
        TerrainCurve.clear();
    }

    public static void render(float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            clear();
            return;
        }

        long now = System.nanoTime();
        float deltaSeconds = lastFrameNanos == 0L ? 0F : (now - lastFrameNanos) / 1.0E9F;
        lastFrameNanos = now;
        float step = minecraft.isPaused() ? 0F : Math.min(deltaSeconds, MAX_FRAME_SECONDS);

        if (stoppedNow) {
            if (!live) {
                live = true;
                stoppedSeconds = 0F;
            }
            stoppedSeconds += step;
            releasedSeconds = 0F;
        } else if (live) {
            releasedSeconds += step;
            if (releasedSeconds >= BREAK_OUT_SECONDS) {
                live = false;
                stoppedSeconds = 0F;
                releasedSeconds = 0F;
            }
        }

        TerrainCurve.tick(0F, 0F, step);

        Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
        Vec3 centre = TimeStopView.centre();
        centreX = (float) (centre.x - camera.x);
        centreY = (float) (centre.y - camera.y);
        centreZ = (float) (centre.z - camera.z);
    }

    private static float smooth(float t) {
        float u = Mth.clamp(t, 0F, 1F);
        return u * u * (3F - 2F * u);
    }

    private static float easeOut(float t) {
        float u = 1F - Mth.clamp(t, 0F, 1F);
        return 1F - u * u;
    }

    private static float settleSeconds() {
        return Math.min(SETTLE_SECONDS, TimeStopView.waveTicks() / 20F * SETTLE_SHARE);
    }

    private static float waveSeconds() {
        return TimeStopView.waveTicks() / 20F - settleSeconds();
    }

    public static float strength() {
        return live ? 1F : 0F;
    }

    public static float frontRadius() {
        if (!live || !stoppedNow) {
            return 0F;
        }
        float t = stoppedSeconds / waveSeconds();
        return TimeStopView.fieldRadius() * (TimeStopView.global() ? t * t * smooth(t) : easeOut(t));
    }

    public static float frontAlong(Vec3 outward) {
        return frontRadius() * (1F - SHORE_DEPTH * waveStrength() * shoreline(outward, wavePhase()));
    }

    private static float shoreline(Vec3 outward, float time) {
        float angle = (float) Mth.atan2(outward.z, outward.x);
        float lift = (float) outward.y;
        float swell = Mth.sin(angle * 3F + time * 0.09F + 1.3F) * 0.5F
                + Mth.sin(angle * 5F - time * 0.13F + 4.1F) * 0.3F
                + Mth.sin(angle * 2F + lift * 3F + time * 0.06F) * 0.45F
                + Mth.sin(angle * 7F + lift * 2F - time * 0.2F + 2.2F) * 0.15F
                + Mth.sin(lift * 4F - time * 0.1F) * 0.25F;
        return Mth.clamp(swell / 1.65F * 0.5F + 0.5F, 0F, 1F);
    }

    public static float waveStrength() {
        if (!live || !stoppedNow) {
            return 0F;
        }
        float since = stoppedSeconds - waveSeconds();
        return since <= 0F ? 1F : 1F - smooth(since / settleSeconds());
    }

    public static float greyRadius() {
        if (!live) {
            return 0F;
        }
        if (stoppedNow) {
            return frontRadius();
        }
        float back = releasedSeconds / BREAK_OUT_SECONDS;
        float u = Mth.clamp(back, 0F, 1F);
        return TimeStopView.fieldRadius() * (1F - u * u);
    }

    public static float greyStrength() {
        return live ? 1F : 0F;
    }

    public static float releaseStrength() {
        if (!live || stoppedNow) {
            return 0F;
        }
        return 1F - Mth.clamp(releasedSeconds / BREAK_OUT_SECONDS, 0F, 1F);
    }

    public static float wakeBlocks() {
        return (TimeStopView.global() ? GLOBAL_WAKE_BLOCKS : WAKE_BLOCKS) + frontRadius() * WAKE_SHARE;
    }

    public static float warpPush() {
        return WARP_PUSH;
    }

    public static float warpWidth() {
        return TimeStopView.global() ? GLOBAL_WARP_WIDTH : WARP_WIDTH;
    }

    public static float skyShare() {
        return SKY_SHARE;
    }

    public static float ringWidth() {
        return RING_WIDTH;
    }

    public static float ringStrength() {
        return RING_STRENGTH;
    }

    public static float hueRoll() {
        return HUE_ROLL;
    }

    public static float huePhase() {
        return stoppedSeconds * HUE_RADIANS_PER_SECOND;
    }

    public static float desaturation() {
        return DESATURATION;
    }

    public static float darken() {
        return DARKEN;
    }

    public static float squiggle() {
        return SQUIGGLE;
    }

    public static float waveScale() {
        return WAVE_SCALE;
    }

    public static float wavePhase() {
        return (stoppedSeconds + releasedSeconds) * WAVE_RADIANS_PER_SECOND;
    }

    public static float globalDrain() {
        return TerrainCurve.drain();
    }

    public static float centreX() {
        return centreX;
    }

    public static float centreY() {
        return centreY;
    }

    public static float centreZ() {
        return centreZ;
    }
}
