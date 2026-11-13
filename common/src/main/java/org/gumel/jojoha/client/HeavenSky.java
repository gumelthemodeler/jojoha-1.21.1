package org.gumel.jojoha.client;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class HeavenSky {
    private static final float[] TIER_STRENGTH = {0.12F, 0.18F, 0.26F, 0.42F};
    private static final Vec3 WARM = new Vec3(1.0, 0.82, 0.42);
    private static final Vec3 DEEP = new Vec3(0.58, 0.42, 0.2);
    private static final float EASE = 0.05F;
    private static final float FOG_SHARE = 0.6F;

    private static int ticksLeft;
    private static int tier;
    private static float strength;
    private static float strengthO;
    private static float deep;
    private static float deepO;

    private HeavenSky() {
    }

    public static void cue(int seenTier, int ticks) {
        tier = Mth.clamp(seenTier, 0, TIER_STRENGTH.length - 1);
        ticksLeft = Math.max(ticksLeft, ticks);
    }

    public static void clear() {
        ticksLeft = 0;
        strength = 0F;
        strengthO = 0F;
        deep = 0F;
        deepO = 0F;
    }

    public static void tick() {
        strengthO = strength;
        deepO = deep;
        float target = ticksLeft > 0 ? TIER_STRENGTH[tier] : 0F;
        float deepTarget = ticksLeft > 0 && tier >= 3 ? 1F : 0F;
        strength += (target - strength) * EASE;
        deep += (deepTarget - deep) * EASE;
        if (ticksLeft > 0) {
            ticksLeft--;
        }
    }

    private static float strength(float partialTick) {
        return Mth.lerp(partialTick, strengthO, strength);
    }

    private static Vec3 tint(float partialTick) {
        float d = Mth.lerp(partialTick, deepO, deep);
        return WARM.lerp(DEEP, d);
    }

    public static Vec3 sky(Vec3 colour, float partialTick) {
        float s = strength(partialTick);
        return s <= 0.001F ? colour : colour.lerp(tint(partialTick), s);
    }

    public static float[] fog(float red, float green, float blue, float partialTick) {
        float s = strength(partialTick) * FOG_SHARE;
        if (s <= 0.001F) {
            return null;
        }
        Vec3 to = tint(partialTick);
        return new float[]{Mth.lerp(s, red, (float) to.x), Mth.lerp(s, green, (float) to.y),
                Mth.lerp(s, blue, (float) to.z)};
    }
}
