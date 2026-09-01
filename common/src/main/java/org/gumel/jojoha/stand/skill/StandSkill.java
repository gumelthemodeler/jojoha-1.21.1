package org.gumel.jojoha.stand.skill;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.TrustTier;

/**
 * One move a Stand can perform, triggered by its user from a skill slot.
 *
 * <p>Deliberately shaped like {@code HamonMove}, since the two are the same idea for different
 * power sources and there is no reason for a reader to learn two vocabularies. The differences are
 * the ones that actually matter for Stands: a move needs the manifested {@link StandEntity} to
 * perform it, and it can be gated behind a Trust Tier, because the design doc reserves the full
 * moveset for a Bonded Stand rather than handing everything over the moment one is obtained.
 */
public interface StandSkill {
    ResourceLocation id();

    /** Translation key for the HUD slot label. */
    String translationKey();

    /**
     * The label to actually show, which for most moves is just {@link #translationKey()}.
     *
     * <p>Exists for moves that change identity with progression - Stand Dash becoming Time Shift -
     * so the slot renames itself rather than continuing to advertise something the move no longer
     * does.
     */
    default String translationKeyFor(JojohaPlayerData data) {
        return translationKey();
    }

    /**
     * Whether a manifested Stand is required.
     *
     * <p>True for almost everything, since a Stand move without a Stand is a contradiction. The
     * exceptions are the moves the user can throw with their own body - Barrage and Uppercut -
     * which get a null Stand handed to {@link #activate} and are expected to cope.
     */
    default boolean requiresStand() {
        return true;
    }

    /**
     * Whether the user has learned this at all.
     *
     * <p>Separate from {@link #minimumTrust()} because the two gate different things. Trust is the
     * Stand's willingness and rises on its own; this is a move that has to be earned by doing
     * something specific, and the only one so far - Time Stop - is earned by surviving other
     * people's.
     */
    /**
     * Whether this player has this move yet.
     *
     * <p>Answered by the skill tree: a move sits behind a node, and having the node is having the
     * move. A move that no tree mentions is ungated and always available, which is what stops a
     * move added before its node from disappearing out of the game in the meantime.
     */
    default boolean isUnlocked(JojohaPlayerData data) {
        return org.gumel.jojoha.skilltree.SkillTrees.skillUnlocked(data, id());
    }

    /**
     * The move this one is a better version of, or null if it stands alone.
     *
     * <p>Not an upgrade that happens to you - an alternative you choose. Both moves stay in the game
     * and both can be learned; what they cannot do is share a bar, because they answer the same
     * question and putting both on it would spend two slots on one ability.
     *
     * <p>Declared on the newer move and read in both directions, so a move never has to know what
     * might replace it later.
     */
    default ResourceLocation replaces() {
        return null;
    }

    int cooldownTicks();

    /**
     * How long this move may be held before it stops charging, in ticks. Zero for a tap.
     *
     * <p>Read by the client to decide whether to fire on the press or on the release, and by the
     * server to clamp whatever the client claims it held for.
     */
    default int chargeMaxTicks() {
        return 0;
    }

    /**
     * Whether this move runs for as long as the key is held.
     *
     * <p>A third shape alongside the press and the charge, and it needs to be one. A press is over
     * the instant it happens; a charge is a wind-up that fires on the release. This is neither - it
     * starts on the press, stays running while the key is down, and ends when it comes up. A grapple
     * is the obvious case: you are on the rope until you decide not to be, and how long that is is
     * the move.
     *
     * <p>Implemented as two uses rather than a start and a stop. The key going down sends the move,
     * and the key coming up sends it again - so a sustained move only has to make its own activate
     * a toggle, and the input layer needs to know nothing about what is being sustained.
     */
    default boolean isSustained() {
        return false;
    }

    /**
     * Whether this move is running for that player right now.
     *
     * <p>Asked by the input layer, and it is the difference between a hold that works and one that
     * inverts itself. The obvious way to track a sustained move on the client is a flag set when the
     * start packet goes out - but the server can refuse that packet, for any of half a dozen
     * reasons, and the flag has no way of hearing about it. One refused start and the client
     * believes the move is running when it is not: the next press is swallowed as "already going",
     * the release fires the start instead, and from then on the move works inside out.
     *
     * <p>Answering from the world rather than from a flag makes that impossible. The move is running
     * if the thing it created exists, which both sides can see, and a start that never happened
     * leaves nothing behind to lie about.
     */
    default boolean isSustainActive(net.minecraft.world.entity.player.Player player) {
        return false;
    }

    /** Cooldown for a given charge. Tap moves ignore it. */
    default int cooldownTicks(int chargeTicks) {
        return cooldownTicks();
    }

    /**
     * A flat lockout to apply instead of the usual cooldown after this cast, or 0 for none.
     *
     * <p>For limits that are about how often a move may be used across a fight rather than how soon
     * it may be repeated. Deliberately separate from {@link #cooldownTicks(int)} and deliberately
     * not run through the Speed reduction: a cooldown is the move recovering, and a stat that makes
     * a Stand quicker should shorten it. A lockout is a penalty for having used something too much,
     * and a penalty a stat can shorten is not much of one.
     *
     * <p>Asked after {@code activate} has run, so a move that counts its own uses can have already
     * counted this one.
     */
    default int lockoutTicks(JojohaPlayerData data) {
        return 0;
    }

    /**
     * Whether this move is already under way and should not be started again.
     *
     * <p>Distinct from a cooldown, which is a move that has finished and is recovering. This is a
     * move that has not finished - time being stopped right now is the case it exists for. The
     * client reads it to decide whether a held key should wind anything up at all, so that a move
     * you cannot start does not put a charge meter on screen.
     */
    default boolean isRunning(JojohaPlayerData data) {
        return false;
    }

    /** Stand energy consumed. Checked and deducted by {@link StandSkills} only if {@link #activate} succeeds. */
    float energyCost();

    /** Cost for a given charge. Tap moves ignore it. */
    default float energyCost(int chargeTicks) {
        return energyCost();
    }

    /**
     * Lowest Trust Tier that may use this at all.
     *
     * <p>Defaults to EMERGING - the first tier with a whole Stand to act with. A signature move
     * should generally raise this to BONDED, which is the tier the doc describes as granting "all
     * base moves".
     */
    default TrustTier minimumTrust() {
        return TrustTier.EMERGING;
    }

    /**
     * Performs the move.
     *
     * @param stand the manifested Stand, or null when {@link #requiresStand()} is false and none
     *              is currently out
     * @return false if the move declined to fire - no target in reach, nowhere to leap to. The
     *         caller charges neither energy nor cooldown in that case, so a move that could not
     *         happen never costs the player anything.
     */
    boolean activate(ServerPlayer player, JojohaPlayerData data, StandEntity stand);

    /**
     * Runs the move with the charge it was held for.
     *
     * <p>Defaulted to the un-charged form so every existing move is untouched - a move that does not
     * declare a charge maximum is handed zero and never asked about it again.
     */
    default boolean activate(ServerPlayer player, JojohaPlayerData data, StandEntity stand,
                             int chargeTicks) {
        return activate(player, data, stand);
    }
}
