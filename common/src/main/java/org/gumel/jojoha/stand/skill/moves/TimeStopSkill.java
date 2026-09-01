package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.VampireStage;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.TrustTier;
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.StandSkill;
import org.gumel.jojoha.stand.skill.TimeStopCast;

/**
 * The stopped world. Everything nearby holds still; the user does not.
 *
 * <p>Length follows the design doc on two counts. It scales with the Stand's Endurance, the stat
 * defined as how long the Stand can sustain itself, and it is extended for Vampires, which the doc
 * calls out by name: "If the stand has a timestop ability, the maximum timestop length is
 * increased." Gated to BONDED and priced accordingly - it is the most powerful thing in the kit.
 *
 * <p>Scope worth knowing: this freezes mobs, not the world. Blocks, weather, other players and
 * projectiles already in flight all keep running. Those need a far wider reach into the tick loop
 * than a move should take on its own.
 */
public final class TimeStopSkill implements StandSkill {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "time_stop");
    public static final TimeStopSkill INSTANCE = new TimeStopSkill();

    private static final int COOLDOWN_TICKS = 400;

    /**
     * How many stops one fight is worth, and what the last one costs.
     *
     * <p>The cooldown alone caps how fast time can be stopped, but nothing capped how many times a
     * single fight could be stopped - so a long enough fight was a fight fought largely in stopped
     * time, which is the one thing the move should never become. Three is enough for a stop to open
     * a fight, save one, and finish it, and few enough that the third is a decision.
     *
     * <p>The lockout is three times the ordinary cooldown and is not shortened by the Stand's Speed,
     * because it is not the move recovering - it is the user having spent something they do not have
     * any more of. Counted against the same combat timer everything else uses, so it clears when the
     * fight does rather than on a clock of its own.
     */
    private static final int USES_PER_FIGHT = 3;
    private static final int EXHAUSTION_LOCKOUT_TICKS = 1200;
    private static final float ENERGY_COST = EnergyWeight.ULTIMATE.cost();

    /**
     * The ceiling on a stop, before and after the vampire clause.
     *
     * <p>Ten seconds is the human limit however far the Stand's Endurance is pushed. The doc's
     * vampire progression doubles it, which is the one place the ceiling moves rather than the
     * multiplier simply running into it and being wasted.
     */
    private static final int MAX_DURATION_TICKS = 200;
    private static final int MAX_VAMPIRE_DURATION_TICKS = 400;

    /**
     * What practice is worth.
     *
     * <p>Five seconds, earned by using the move rather than by any stat - the doc treats time stop
     * as a thing you get better at holding, not a thing you buy.
     *
     * <p>Added to the duration <em>and</em> to the ceiling, which is the only way it is worth
     * anything. Added to the ceiling alone it does nothing for anybody below the cap, which is
     * almost everybody; added to the duration alone it is swallowed by the cap for anybody at it,
     * which is exactly the players who have earned it. Both, and it is five seconds either way.
     */
    private static final int TRAINED_BONUS_TICKS = 100;
    private static final int CASTS_TO_TRAIN = 24;

    /**
     * How long the key may be held, and what a bare tap is worth.
     *
     * <p>Three and a half seconds to charge fully, up from two. Two was quick enough that the bar
     * was full about as soon as you noticed it had appeared, which makes a deliberate choice into a
     * reflex - and the thing being chosen is now up to twenty seconds of stopped time, which is
     * worth spending a moment on. It is also long enough for the wind-up to be interrupted, which is
     * the counterplay the move is supposed to have.
     *
     * <p>A tap still stops time - being unable to get anything at all out of the move without a long
     * press would make it feel unresponsive rather than deliberate - but it stops it for about a
     * third of the time, and the length, the cost and the cooldown all rise together from there.
     * Paying most of the price for a stop you cut short is what stops the tap from simply being the
     * correct way to use the move.
     */
    private static final int CHARGE_MAX_TICKS = 70;
    private static final float TAP_DURATION_SHARE = 0.34F;
    private static final float TAP_COST_SHARE = 0.40F;
    private static final float TAP_COOLDOWN_SHARE = 0.45F;
    /**
     * What each point of the Stand's Endurance adds to the value of a <em>tap</em>.
     *
     * <p>Endurance used to add flat ticks to the duration, which stopped meaning anything the moment
     * the duration became a share of the ceiling - the ceiling is a designed cap and a stat has no
     * business pushing through it. Moved to the floor instead: a tougher Stand gets more out of a
     * short press, and everybody's full hold is still their own maximum. The stat now buys
     * responsiveness rather than length, which is the more interesting thing for it to buy.
     */
    private static final float TAP_SHARE_PER_ENDURANCE = 0.012F;

    /** However tough the Stand, a tap is never most of a full hold. */
    private static final float MAX_TAP_SHARE = 0.7F;

    private TimeStopSkill() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "skill.jojoha.time_stop";
    }

    @Override
    public int cooldownTicks() {
        return COOLDOWN_TICKS;
    }

    @Override
    public int cooldownTicks(int chargeTicks) {
        return Math.round(COOLDOWN_TICKS * scale(chargeTicks, TAP_COOLDOWN_SHARE));
    }

    @Override
    public float energyCost() {
        return ENERGY_COST;
    }

    @Override
    public float energyCost(int chargeTicks) {
        return ENERGY_COST * scale(chargeTicks, TAP_COST_SHARE);
    }

    /**
     * Already stopped, or in the act of stopping.
     *
     * <p>Both halves matter. The wind-up is the obvious one - holding the key again while the cast
     * is landing would be winding up a second stop on top of the first. The held stop is the one
     * that was actually visible: time being stopped is exactly when a player is most likely to
     * press things, and the charge meter reappearing over a world that is already frozen suggested
     * a stop could be stacked on a stop.
     */
    @Override
    public boolean isRunning(JojohaPlayerData data) {
        return data.timeStopCastTicks > 0 || data.timeStopHeldTicks > 0;
    }

    /**
     * The four numbers that decide what a stop is worth, each answerable to a datapack.
     *
     * <p>Length above all. A stop is not a source of damage, it is a window in which none of the
     * usual rules apply, so how long that window is outweighs every damage figure in the kit when
     * deciding how a fight against one plays out - which is exactly why a balance pass that could
     * only reach damage would never have been a balance pass. See {@code TuningPreset}.
     *
     * <p>Read per call rather than cached into a field: a datapack reload has to take effect on the
     * next stop, not on the next restart.
     */
    private static int usesPerFight() {
        return org.gumel.jojoha.data.StandTuning.ticks("time_stop_uses_per_fight", USES_PER_FIGHT);
    }

    private static int lockoutTicks() {
        return org.gumel.jojoha.data.StandTuning.ticks("time_stop_lockout_ticks",
                EXHAUSTION_LOCKOUT_TICKS);
    }

    private static float mortalCeiling() {
        return org.gumel.jojoha.data.StandTuning.value("time_stop_max_ticks", MAX_DURATION_TICKS);
    }

    private static float undeadCeiling() {
        return org.gumel.jojoha.data.StandTuning.value("time_stop_vampire_max_ticks",
                MAX_VAMPIRE_DURATION_TICKS);
    }

    /** Spent for this fight once the last stop has been thrown - see usesPerFight(). */
    @Override
    public int lockoutTicks(JojohaPlayerData data) {
        return data.timeStopUsesThisFight >= usesPerFight() ? lockoutTicks() : 0;
    }

    /** How many stops are left in this fight, for anything that wants to say so. */
    public static int usesLeftThisFight(JojohaPlayerData data) {
        return Math.max(0, usesPerFight() - data.timeStopUsesThisFight);
    }

    /** Whether the count should be wiped - the fight is over, or the lockout has been served. */
    public static boolean shouldClearFightUses(JojohaPlayerData data, long gameTime) {
        if (data.timeStopUsesThisFight <= 0) {
            return false;
        }

        // Out of combat, the fight the count belonged to is over. Still in it, the count only
        // clears once the lockout it earned has actually elapsed - otherwise serving the penalty
        // would leave the player one cast away from earning it again.
        return data.combatTicks <= 0
                || (data.timeStopUsesThisFight >= usesPerFight()
                        && !data.isMoveOnCooldown(ID, gameTime));
    }

    @Override
    public int chargeMaxTicks() {
        return CHARGE_MAX_TICKS;
    }

    /** A tap is worth {@code tapShare} of the full value, a full hold all of it. */
    private static float scale(int chargeTicks, float tapShare) {
        float held = Mth.clamp(chargeTicks / (float) CHARGE_MAX_TICKS, 0F, 1F);
        return tapShare + (1F - tapShare) * held;
    }

    /** What a bare tap is worth for this player - see TAP_SHARE_PER_ENDURANCE. */
    private static float tapShare(JojohaPlayerData data) {
        return Math.min(MAX_TAP_SHARE,
                TAP_DURATION_SHARE + data.stand.endurance() * TAP_SHARE_PER_ENDURANCE);
    }

    @Override
    public TrustTier minimumTrust() {
        return TrustTier.BONDED;
    }

    /**
     * How many separate time stops must be survived before the user can perform one.
     *
     * <p>The ability is not taught, it is recognised: standing inside stopped time repeatedly is
     * what lets someone eventually perceive it, and then reproduce it.
     */
    public static final int REQUIRED_EXPOSURES = 3;

    /** Named apart from the interface method so the two do not collide on one signature. */
    public static boolean hasLearned(JojohaPlayerData data) {
        return data.timeStopExposures >= REQUIRED_EXPOSURES;
    }

    /**
     * Both gates, not either.
     *
     * <p>The node has to be taken like any other move, and the exposures still have to have
     * happened - buying Time Stop off a tree without ever having been caught in one would throw
     * away the one piece of progression this move had before the tree existed.
     */
    @Override
    public boolean isUnlocked(JojohaPlayerData data) {
        return hasLearned(data)
                && org.gumel.jojoha.skilltree.SkillTrees.skillUnlocked(data, id());
    }

    @Override
    public boolean activate(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        return activate(player, data, stand, CHARGE_MAX_TICKS);
    }

    @Override
    public boolean activate(ServerPlayer player, JojohaPlayerData data, StandEntity stand,
                            int chargeTicks) {
        // The freeze does not happen here. This starts the wind-up; TimeStopCast lands it, or
        // somebody knocks the caster out of it first.
        // Counted here, where the cast is committed and paid for. Whether it lands is TimeStopCast's
        // business; being knocked out of one is still practice at starting them.
        data.timeStopCasts++;
        data.timeStopUsesThisFight++;

        TimeStopCast.begin(player, durationTicks(data, chargeTicks));
        return true;
    }

    /**
     * How long a stop this hold buys, as a share of the longest one this player may hold.
     *
     * <p>A full charge is now the ceiling exactly, which is the whole point: the bar on screen is a
     * share of the charge, so the charge has to be a share of the same thing the bar claims to be
     * measuring. It was not. The duration grew from a base of 170 plus a little Endurance and was
     * then clipped against a ceiling worked out somewhere else entirely, so a full bar bought 183 of
     * a possible 200 ticks for an ordinary player - and for a vampire, whose ceiling is twice as
     * high, 228 of 400. Filling the bar and getting a bit over half of what you were owed is not a
     * charge meter, it is a decoration.
     *
     * <p>Everything that used to move the duration now moves the ceiling instead, which is where a
     * limit belongs, and the charge is the one thing that decides how much of it you get.
     */
    public static int durationTicks(JojohaPlayerData data, int chargeTicks) {
        return Math.max(1, Math.round(ceiling(data) * scale(chargeTicks, tapShare(data))));
    }

    /**
     * The longest stop this player can currently hold, in ticks.
     *
     * <p>Ten seconds for anybody, twenty once the vampire progression is in play, and the practice
     * bonus on top of whichever applies.
     */
    public static int ceiling(JojohaPlayerData data) {
        // The doc's two numbers are the ends of this, and the stages in between are spaced along it
        // - Pillar Men and the Ultimate Lifeform are described throughout as further along the same
        // progression, not as separate cases.
        float base = switch (data.vampireStage) {
            case NONE -> mortalCeiling();
            case VAMPIRE -> Mth.lerp(0.45F, mortalCeiling(), undeadCeiling());
            case PILLAR_MAN -> Mth.lerp(0.75F, mortalCeiling(), undeadCeiling());
            case ULTIMATE_LIFEFORM -> undeadCeiling();
        };

        return Math.round(base) + trainedTicks(data);
    }

    /** The practice bonus in ticks, up to the full five seconds. */
    public static int trainedTicks(JojohaPlayerData data) {
        return Math.round(TRAINED_BONUS_TICKS * trainedShare(data));
    }

    /** 0 to 1 through the practice needed for the full extra five seconds. */
    public static float trainedShare(JojohaPlayerData data) {
        return Mth.clamp(data.timeStopCasts / (float) CASTS_TO_TRAIN, 0F, 1F);
    }
}
