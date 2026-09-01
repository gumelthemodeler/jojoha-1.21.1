package org.gumel.jojoha.client;

import net.minecraft.Util;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/**
 * How entities caught in a time stop are drawn: shaking in place, and rendered as their own
 * negative.
 *
 * <p>The shake is the part that says they are being <em>held</em> rather than merely paused. A
 * genuinely motionless model is indistinguishable from a screenshot; a model straining against
 * something invisible reads as a body that still wants to move and cannot.
 */
public final class FrozenEntityFx {
    /** How far a held body can tremble, in blocks. Small - this is a vibration, not a stagger. */
    private static final float SHAKE = 0.022F;

    /**
     * How quickly the inversion comes and goes.
     *
     * <p>Faded rather than switched, so entities turn over as the stop lands instead of flipping
     * between one frame and the next.
     */
    private static final float FADE_PER_SECOND = 5F;

    private static float amount;

    private FrozenEntityFx() {
    }

    /** Call once per client tick. */
    public static void tick(boolean frozen) {
        amount = Mth.approach(amount, frozen ? 1F : 0F, FADE_PER_SECOND / 20F);
    }

    public static void clear() {
        amount = 0F;
    }

    /** Whether anything is frozen at all, for the renderer to decide if it needs to do work. */
    public static boolean active() {
        return amount > 0.001F;
    }

    /**
     * The tremble offset for one entity.
     *
     * <p>Off the wall clock rather than the entity's own tick count, which is exactly what a stopped
     * entity no longer has - the freeze is implemented by taking its ticks away, so anything driven
     * from them would sit perfectly still. Three frequencies that do not divide into one another, and
     * a per-entity offset, so no two bodies shake in sympathy.
     */
    public static float[] shake(Entity entity) {
        if (amount <= 0.001F) {
            return null;
        }

        float seconds = (Util.getMillis() % 1000000L) / 1000F;
        float seed = entity.getId() * 1.618F;
        float scale = SHAKE * amount;

        return new float[] {
                Mth.sin(seconds * 41.3F + seed) * scale,
                Mth.sin(seconds * 57.7F + seed * 2.1F) * scale * 0.6F,
                Mth.sin(seconds * 47.1F + seed * 3.7F) * scale,
        };
    }
}
