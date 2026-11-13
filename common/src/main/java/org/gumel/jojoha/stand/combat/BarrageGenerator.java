package org.gumel.jojoha.stand.combat;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

public final class BarrageGenerator {
    public static final double SPREAD_SIDE = 0.35;
    public static final double SPREAD_UP = 0.4;

    public static final double TRACKING_SPEED = 0.12;

    public static final double FOCUS_AT = 0.5;

    public static final int BASE_EXTRAS = 1;
    public static final double EXTRA_AT = 0.4;
    public static final double SECOND_EXTRA_AT = 0.75;
    public static final int MAX_EXTRAS = 4;

    public static final int EXTRAS_PER_TICK = 2;

    public static final float EXTRA_DAMAGE_SHARE = 0.08F;

    public static final double MOMENTUM_TIGHTEN = 0.3;
    public static final double MOMENTUM_LEAD = 0.3;

    public static final double HIT = 0.07;
    public static final double EXTRA_HIT = 0.03;
    public static final double MISS = -0.06;
    public static final double KNOCKED_BACK = -0.12;
    public static final double GUARDED = -0.65;

    public static final int NEW_BARRAGE_GAP = 15;

    public static final int RUSH_BEATS = 4;

    public static final float FINISHER_SCALE = 3.0F;
    public static final float FINISHER_CEILING = 10.0F;

    public static final float FINISHER_SHARE = 0.25F;

    public static final int FINISHER_MIN_BEATS = 8;

    public static final int WINDUP_BEATS = 3;
    public static final int WINDUP_PACE = 2;
    public static final int WINDUP_TICKS = 18;

    public static final double PEAK_AT = 0.85;

    public static final float THUD_RAMP = 1.0F;
    public static final float THUD_WIDE_UNTIL = 0.4F;
    public static final float THUD_MID_UNTIL = 0.75F;
    public static final float THUD_HEAVY_AT = 0.8F;
    public static final float THUD_LEAST_HEFT = 0.5F;
    public static final int THUD_SPACING_BEATS = 16;

    private static final double[] FOCUS_HEIGHTS = {0.85, 0.62, 0.42};

    private BarrageGenerator() {
    }

    public static BarragePattern pattern(int bodies, double targetSpeed, double momentum) {
        if (bodies <= 0) {
            return BarragePattern.STANDARD;
        }
        if (bodies >= 2) {
            return BarragePattern.WIDE;
        }
        if (targetSpeed > TRACKING_SPEED) {
            return BarragePattern.TRACKING;
        }
        return momentum >= FOCUS_AT ? BarragePattern.CONCENTRATED : BarragePattern.STANDARD;
    }

    public static int extras(double momentum, boolean rushing) {
        int extras = BASE_EXTRAS + (momentum >= EXTRA_AT ? 1 : 0) + (momentum >= SECOND_EXTRA_AT ? 1 : 0)
                + (rushing ? 1 : 0);
        return Math.min(MAX_EXTRAS, extras);
    }

    public static int extras(double momentum, boolean rushing, StandCombatProfile profile) {
        return Math.min(MAX_EXTRAS, extras(momentum, rushing) + profile.extraAfterimages());
    }

    public static double spread(BarragePattern pattern, double momentum, StandCombatProfile profile) {
        return spread(pattern, momentum) * profile.spread();
    }

    public static double lead(BarragePattern pattern, double momentum, StandCombatProfile profile) {
        return Math.min(1.0, lead(pattern, momentum) + profile.lead());
    }

    public static double change(double change, StandCombatProfile profile) {
        return change > 0.0 ? change * profile.momentumGain() : change;
    }

    public static float finisherBlow(float blow, StandCombatProfile profile) {
        return blow * profile.finisherScale();
    }

    public static float finisherShare(StandCombatProfile profile) {
        return FINISHER_SHARE * profile.finisherScale() / FINISHER_SCALE;
    }

    public static double spread(BarragePattern pattern, double momentum) {
        return pattern.spread() * (1.0 - MOMENTUM_TIGHTEN * clampMomentum(momentum));
    }

    public static double lead(BarragePattern pattern, double momentum) {
        return Math.min(1.0, pattern.lead() + MOMENTUM_LEAD * clampMomentum(momentum));
    }

    public static double momentum(double momentum, double change) {
        return clampMomentum(momentum + change);
    }

    public static double focusHeight(RandomSource random) {
        return FOCUS_HEIGHTS[random.nextInt(FOCUS_HEIGHTS.length)];
    }

    public static Vec3 aim(PunchType type, Vec3 feet, double width, double height, Vec3 motion, boolean onGround,
                           Vec3 right, boolean fromRight, double spread, double lead, double focus, RandomSource random) {
        double side = fromRight ? 1.0 : -1.0;
        double across = Mth.clamp(width / 0.6, 0.5, 2.0);
        double tall = Mth.clamp(height / 1.8, 0.5, 2.0);
        double at = Double.isNaN(focus) ? type.height() : focus;
        Vec3 led = new Vec3(motion.x, onGround ? 0.0 : motion.y, motion.z).scale(StandFist.OUT_TICKS * lead);
        return feet.add(0, height * at, 0)
                .add(led)
                .add(right.scale(side * type.flank() * width))
                .add(right.scale(signed(random) * SPREAD_SIDE * across * spread))
                .add(0, signed(random) * SPREAD_UP * tall * spread, 0);
    }

    public static boolean thud(int beat, int beats) {
        if (beat < 0 || beat >= beats) {
            return false;
        }
        int at = 0;
        while (at < beat) {
            at += thudGap(progress(at, beats), beats);
        }
        return at == beat;
    }

    public static float thudWeight(int beat, int beats) {
        if (!thud(beat, beats)) {
            return 0F;
        }
        float sum = 0F;
        for (int at = 0; at < beats; at += thudGap(progress(at, beats), beats)) {
            sum += rawWeight(progress(at, beats));
        }
        return rawWeight(progress(beat, beats)) * beats / Math.max(1.0E-3F, sum);
    }

    public static float heavyHeft(int beats) {
        float sum = 0F;
        for (int at = 0; at < beats; at += thudGap(progress(at, beats), beats)) {
            sum += rawWeight(progress(at, beats));
        }
        return rawWeight(THUD_HEAVY_AT) * beats / Math.max(1.0E-3F, sum);
    }

    public static float thudStrength(float heft, int beats) {
        float most = rawWeight(1F) / rawWeight(THUD_HEAVY_AT) * heavyHeft(beats);
        return Mth.clamp(heft / Math.max(1.0E-3F, most), 0F, 1F);
    }

    private static float progress(int beat, int beats) {
        return beats <= 1 ? 1F : beat / (float) (beats - 1);
    }

    private static int thudGap(float progress, int beats) {
        int spacing = Math.max(1, Math.round(beats / (float) THUD_SPACING_BEATS));
        return (progress < THUD_WIDE_UNTIL ? 3 : progress < THUD_MID_UNTIL ? 2 : 1) * spacing;
    }

    private static float rawWeight(float progress) {
        return 1F + THUD_RAMP * progress;
    }

    public static float finisherBlow(float blow) {
        return blow * FINISHER_SCALE;
    }

    private static double clampMomentum(double momentum) {
        return Mth.clamp(momentum, 0.0, 1.0);
    }

    private static double signed(RandomSource random) {
        return random.nextDouble() * 2.0 - 1.0;
    }
}
