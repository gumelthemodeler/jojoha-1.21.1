package org.gumel.jojoha.client;

import net.minecraft.util.Mth;
import org.gumel.jojoha.item.StandArrowRitual;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks who is currently driving a Stand Arrow into themselves, so the item can be drawn glowing.
 *
 * <p>Keyed by player rather than held as a single client-side flag, because the ritual is a
 * spectacle: an onlooker should see the arrow burning in someone else's hand, not just the person
 * holding it. The window is derived from the ritual's own length, so the glow ends on the same tick
 * the arrow is spent and vanishes - the light goes out because the arrow is gone, not because a
 * separate timer happened to agree.
 *
 * <p>Parked along with the rest of the arrow glow - see {@code StandArrowColors} for how to switch
 * the whole thing back on.
 */
public final class StandArrowGlow {
    /** How long one full trip around the colour wheel takes, in ticks. */
    private static final float HUE_CYCLE_TICKS = 24F;

    /** See {@link #rainbowTint} - held below full so the arrow's own texture still reads. */
    private static final float TINT_SATURATION = 0.75F;

    /**
     * Plain opaque white: the tint is a multiply, so this is the "leave it alone" value.
     *
     * <p>The alpha byte is not decoration. Item tints are read as ARGB and the alpha is applied to
     * the quad, so an RGB-only white would come through as fully transparent and the arrow would
     * vanish whenever it wasn't glowing.
     */
    public static final int NO_TINT = 0xFFFFFFFF;

    /** Opaque alpha, forced onto colours from {@link Mth#hsvToRgb} - see {@link #rainbowTint}. */
    private static final int OPAQUE = 0xFF000000;

    /** Player UUID -> the client tick their arrow stops glowing. */
    private static final Map<UUID, Float> ACTIVE = new HashMap<>();

    private StandArrowGlow() {
    }

    public static void begin(UUID playerId, float clientTimeTicks) {
        ACTIVE.put(playerId, clientTimeTicks + StandArrowRitual.RITUAL_DURATION_TICKS);
    }

    public static boolean isActive(UUID playerId) {
        return ACTIVE.containsKey(playerId);
    }

    /**
     * Whether any arrow anywhere should be burning right now.
     *
     * <p>Needed because the colour handler that recolours the model is only ever handed the
     * {@link net.minecraft.world.item.ItemStack} - vanilla's item tinting has no idea who is
     * holding the thing, so it cannot ask about a specific player. In practice the distinction
     * barely exists: an arrow is consumed by the ritual it starts, so during those few seconds the
     * one being driven in is almost always the only one on screen.
     */
    public static boolean isAnyActive() {
        return !ACTIVE.isEmpty();
    }

    public static void tick(float clientTimeTicks) {
        ACTIVE.values().removeIf(end -> clientTimeTicks > end);
    }

    public static void clear() {
        ACTIVE.clear();
    }

    /**
     * The current point on the colour wheel, as packed RGB.
     *
     * <p>Full saturation and value: this is added as light rather than mixed as paint, so anything
     * dimmer just reads as a wash rather than as a colour.
     */
    public static int rainbow(float clientTimeTicks) {
        float hue = (clientTimeTicks % HUE_CYCLE_TICKS) / HUE_CYCLE_TICKS;
        return Mth.hsvToRgb(hue, 1F, 1F);
    }

    /**
     * The same colour, softened for use as a multiplied tint rather than as added light.
     *
     * <p>A tint multiplies the arrow's own texture, so a fully saturated hue drives two of the
     * three channels to zero and the arrow comes out nearly black in the other two. Backing the
     * saturation off leaves enough white in the colour for the texture underneath to survive, which
     * is the difference between an arrow washed in colour and an arrow turned into a silhouette.
     */
    public static int rainbowTint(float clientTimeTicks) {
        float hue = (clientTimeTicks % HUE_CYCLE_TICKS) / HUE_CYCLE_TICKS;
        // hsvToRgb builds its result with an alpha of zero, and the item tint path honours that
        // alpha - so handing it over unmodified would render the arrow completely transparent
        // rather than coloured.
        return Mth.hsvToRgb(hue, TINT_SATURATION, 1F) | OPAQUE;
    }
}
