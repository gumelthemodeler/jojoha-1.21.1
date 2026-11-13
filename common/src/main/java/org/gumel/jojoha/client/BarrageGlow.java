package org.gumel.jojoha.client;

import net.minecraft.util.Mth;
import org.gumel.jojoha.stand.StandEntity;

import java.util.HashMap;
import java.util.Map;

public final class BarrageGlow {
    private static final float RISE_TICKS = 3F;
    private static final float PEAK_TICKS = 8F;
    private static final float SETTLE_TICKS = 30F;
    private static final float SETTLED = 0.65F;
    private static final float GAP_TICKS = 3F;
    private static final float FORGET_TICKS = 40F;
    private static final int KEEP = 32;

    private static final float DARK_FLOOR = 0.45F;

    private static final class Run {
        private final double since;
        private double seen;

        private Run(double since) {
            this.since = since;
            this.seen = since;
        }
    }

    private static final Map<Integer, Run> RUNS = new HashMap<>();

    private BarrageGlow() {
    }

    public static void clear() {
        RUNS.clear();
    }

    public static float fade(StandEntity stand, float partialTick) {
        double now = stand.level().getGameTime() + partialTick;
        Run run = RUNS.get(stand.getId());
        if (run == null || now - run.seen > GAP_TICKS || now < run.since) {
            if (RUNS.size() >= KEEP) {
                RUNS.values().removeIf(old -> now - old.seen > FORGET_TICKS || now < old.since);
            }
            run = new Run(now);
            RUNS.put(stand.getId(), run);
        }
        run.seen = Math.max(run.seen, now);
        float age = (float) (now - run.since);
        float rise = Mth.clamp(age / RISE_TICKS, 0F, 1F);
        float settle = Mth.clamp((age - PEAK_TICKS) / SETTLE_TICKS, 0F, 1F);
        settle = settle * settle * (3F - 2F * settle);
        return rise * Mth.lerp(settle, 1F, SETTLED);
    }

    public static float glow(StandEntity stand, float partialTick) {
        int light = stand.level().getMaxLocalRawBrightness(stand.blockPosition());
        return fade(stand, partialTick) * Mth.lerp(light / 15F, DARK_FLOOR, 1F)
                * JojohaClientConfig.BARRAGE_GLOW.value();
    }

    public static int additive(int rgb, float strength) {
        float scale = Math.max(0F, strength);
        int red = Math.min(255, Math.round(((rgb >> 16) & 0xFF) * scale));
        int green = Math.min(255, Math.round(((rgb >> 8) & 0xFF) * scale));
        int blue = Math.min(255, Math.round((rgb & 0xFF) * scale));
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }
}
