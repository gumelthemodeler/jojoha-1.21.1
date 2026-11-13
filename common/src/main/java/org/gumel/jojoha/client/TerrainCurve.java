package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import org.gumel.jojoha.stand.skill.TimeStopSystem;

public final class TerrainCurve {
    private static final float SPHERE_RADIUS = (float) TimeStopSystem.RADIUS;
    private static final float SPHERE_SECONDS = 0.55F;

    private static final float EDGE_WIDTH = 3F;

    private static final double ORIGIN_WRAP = 4096.0;

    private static final float DRAIN_RISE_PER_SECOND = 3F;
    private static final float DRAIN_RELEASE_PER_SECOND = 0.85F;

    private static float drain;
    private static float line;
    private static float radius;
    private static float elapsed;
    private static float stopProgress;
    private static float originX;
    private static float originZ;

    private TerrainCurve() {
    }

    public static float drain() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return drain;
        }

        return Math.max(drain, VampireColour.grey((float) minecraft.level.getGameTime()
                + minecraft.getTimer().getGameTimeDeltaPartialTick(false)));
    }

    public static float line() {
        return line;
    }

    public static float radius() {
        return radius;
    }

    public static float edgeWidth() {
        return EDGE_WIDTH;
    }

    public static float originX() {
        return originX;
    }

    public static float originZ() {
        return originZ;
    }

    public static void setStopProgress(float progress) {
        stopProgress = Mth.clamp(progress, 0F, 1F);
    }

    public static void setOrigin(double cameraX, double cameraZ) {
        originX = (float) Mth.positiveModulo(cameraX, ORIGIN_WRAP);
        originZ = (float) Mth.positiveModulo(cameraZ, ORIGIN_WRAP);
    }

    public static boolean isActive() {
        return drain != 0F || line != 0F;
    }

    public static void tick(float foldStrength, float drainStrength, float deltaSeconds) {
        float held = Mth.clamp(foldStrength, 0F, 1F);
        float draining = Mth.clamp(drainStrength, 0F, 1F);

        line = Mth.approach(line, held,
                (held > line ? DRAIN_RISE_PER_SECOND : DRAIN_RELEASE_PER_SECOND) * deltaSeconds);

        drain = Mth.approach(drain, draining,
                (draining > drain ? DRAIN_RISE_PER_SECOND : DRAIN_RELEASE_PER_SECOND) * deltaSeconds);

        if (held <= 0F) {
            elapsed = 0F;
            stopProgress = 0F;

            if (drain <= 0.001F) {
                drain = 0F;
                line = 0F;
                radius = Float.MAX_VALUE;
            }
            return;
        }

        elapsed += deltaSeconds;

        radius = SPHERE_RADIUS * smoothstep(0F, SPHERE_SECONDS, elapsed);
    }

    private static float smoothstep(float from, float to, float value) {
        float u = Mth.clamp((value - from) / Math.max(to - from, 0.0001F), 0F, 1F);
        return u * u * (3F - 2F * u);
    }

    public static void clear() {
        drain = 0F;
        line = 0F;
        radius = Float.MAX_VALUE;
        elapsed = 0F;
        stopProgress = 0F;
    }
}
