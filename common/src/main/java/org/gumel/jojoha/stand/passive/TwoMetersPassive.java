package org.gumel.jojoha.stand.passive;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.StandTuning;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.StandSummonHandler;

/**
 * Star Platinum hits hardest with its target inside arm's reach.
 *
 * <p>The Stand's precision is famously a matter of distance - it is devastating within about two
 * metres of itself and ordinary beyond that. So this is not a flat damage passive with a range
 * check bolted on; the distance <em>is</em> the passive, and the falloff is what a player is meant
 * to be playing around.
 *
 * <h2>Measured from the user, not the Stand</h2>
 *
 * <p>Because a Stand has an effective range, and it is measured from the person it belongs to. It
 * is at its most precise in the space its user occupies and thins out toward the edge of its reach,
 * so the distance that decides the bonus is the user's to the target - not the Stand's.
 *
 * <p>This was the other way round, which quietly made the passive worse the better it worked: a
 * Stand sent out to strike something across the yard collected the full bonus while its user stood
 * safely behind, so the reward for the archetype that is supposed to fight up close was largest at
 * exactly the moment it was not fighting up close. Measuring from the user inverts that. Walking
 * into the exchange is the price of the damage, which is what gives BRAWLER a niche rather than a
 * bonus it gets for free.
 *
 * <p>A Stand that never took form does not strike at all and gets nothing - see
 * {@code TrustTier.manifestsEntity}.
 */
public final class TwoMetersPassive implements StandPassive {
    public static final TwoMetersPassive INSTANCE = new TwoMetersPassive();

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "two_meters");

    /**
     * Inside this, the whole bonus; past the second, none of it.
     *
     * <p>The near figure is the name and is not a coincidence: two blocks is one block of clearance
     * around a body, which is as close as two entities can ordinarily stand - so the full bonus asks
     * the user to be in the fight, not near it.
     *
     * <p>The far figure is {@code PURSUIT_ABANDON_RANGE}, the distance at which a Stand gives up
     * chasing something for its user. That is this mod's own answer to how far a Stand reaches, so
     * the bonus reaching nothing exactly where the Stand stops working is one number rather than
     * two that have to be remembered together.
     */
    private static final double POINT_BLANK = 2.0;
    private static final double FALLOFF = 9.0;

    /** What a point blank hit is worth. Tunable, because "dramatically" is an opinion. */
    private static final float MAX_MULTIPLIER = 1.6F;

    private TwoMetersPassive() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "passive.jojoha.two_meters";
    }

    @Override
    public float onOutgoingDamage(ServerPlayer player, JojohaPlayerData data, LivingEntity target,
                                  float amount) {
        // Still requires a Stand - this is the Stand's precision, and without one there is no
        // strike to sharpen. Only its position has stopped mattering.
        StandEntity stand = StandSummonHandler.findStand(player, data);
        if (stand == null) {
            return amount;
        }

        return amount * multiplierAt(player.distanceTo(target));
    }

    /**
     * How much a hit at this range is worth, from the full bonus down to nothing.
     *
     * <p>The distance is the user's to the target.
     *
     * <p>Smoothstepped rather than linear so there is no single step where the damage visibly
     * changes - a hard edge at a given distance turns the passive into a threshold to stand exactly
     * on, where a curve makes closing the gap continuously worth doing.
     */
    public static float multiplierAt(double distance) {
        float max = StandTuning.value("two_meters_max_multiplier", MAX_MULTIPLIER);
        if (distance <= POINT_BLANK) {
            return max;
        }
        if (distance >= FALLOFF) {
            return 1F;
        }

        float t = (float) ((distance - POINT_BLANK) / (FALLOFF - POINT_BLANK));
        float eased = t * t * (3F - 2F * t);
        return Mth.lerp(eased, max, 1F);
    }
}
