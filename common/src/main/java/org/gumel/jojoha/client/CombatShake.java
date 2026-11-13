package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

public final class CombatShake {
    private static final int SLOTS = 8;

    private static final float RING_TICKS = 7F;

    private static final float DEGREES = 3.4F;

    private static final float CEILING_DEGREES = 6.0F;

    private static final double FULL_RANGE = 6.0;
    private static final double FEEL_RANGE = 24.0;

    private static final float[] STRENGTH = new float[SLOTS];
    private static final float[] STARTED = new float[SLOTS];
    private static int next;

    private CombatShake() {
    }

    public static void add(double x, double y, double z, float strength) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || strength <= 0F) {
            return;
        }

        double distance = Math.sqrt(minecraft.player.distanceToSqr(x, y, z));
        if (distance > FEEL_RANGE) {
            return;
        }

        double near = 1.0 - Math.max(0.0, distance - FULL_RANGE) / (FEEL_RANGE - FULL_RANGE);
        float felt = strength * (float) Mth.clamp(near, 0.0, 1.0);
        if (felt <= 0F) {
            return;
        }

        STRENGTH[next] = felt;
        STARTED[next] = clientTicks();
        next = (next + 1) % SLOTS;
    }

    public static void itch(float strength) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            add(minecraft.player.getX(), minecraft.player.getY(), minecraft.player.getZ(), strength);
        }
    }

    public static float[] cameraShake() {
        float now = clientTicks();

        float intensity = 0F;
        for (int slot = 0; slot < SLOTS; slot++) {
            if (STRENGTH[slot] <= 0F) {
                continue;
            }

            float elapsed = now - STARTED[slot];
            if (elapsed < 0F || elapsed > RING_TICKS) {
                STRENGTH[slot] = 0F;
                continue;
            }

            float falloff = 1F - elapsed / RING_TICKS;
            intensity += falloff * falloff * DEGREES * STRENGTH[slot];
        }

        if (intensity <= 0F) {
            return null;
        }

        float yaw = (Mth.sin(now * 3.9F) * 0.7F + Mth.sin(now * 9.1F) * 0.3F) * intensity;
        float pitch = (Mth.cos(now * 5.3F) * 0.7F + Mth.cos(now * 12.7F) * 0.3F) * intensity;
        return new float[] {squash(yaw), squash(pitch)};
    }

    private static float squash(float degrees) {
        return CEILING_DEGREES * (float) Math.tanh(degrees / CEILING_DEGREES);
    }

    private static float clientTicks() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level == null
                ? 0F
                : (float) minecraft.level.getGameTime() + minecraft.getTimer().getGameTimeDeltaPartialTick(false);
    }
}
