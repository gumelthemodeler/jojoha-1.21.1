package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.StandTuning;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.StandSkill;

/**
 * The flurry every Stand has - ORA ORA ORA, and its equivalents.
 *
 * <p>Generic rather than Star Platinum's own: a barrage is the one move essentially every close
 * combat Stand in the series performs, so it belongs to the range class, not to a character.
 *
 * <p>With a Stand out it rides the engagement machinery that already exists (see
 * {@code StandEntity.pursueAndPunch}) instead of hand-rolling a burst of damage, so a commanded
 * barrage behaves exactly like one the Stand starts on its own - same approach, same rhythm, same
 * animation - and there is only one place where "how a Stand fights something" is written down.
 *
 * <p>Without one the user throws it themselves: a shorter, weaker flurry landed over the following
 * seconds. That path cannot borrow the engagement code, because there is no second body to send
 * anywhere - the blows have to land from where the user is standing.
 */
public final class BarrageSkill implements StandSkill {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "barrage");
    public static final BarrageSkill INSTANCE = new BarrageSkill();

    /**
     * Long enough to outlast the flurry itself, with a breather after it.
     *
     * <p>The barrage runs three seconds of hits plus up to half a second waiting for the Stand to
     * get into position, so anything under about 70 ticks would come off cooldown while the
     * previous one was still swinging. The Speed stat can cut this by up to 40%, which is why the
     * headroom is generous rather than exact - at 120 even a fully-invested Stand lands at 72,
     * still clear of the barrage it has to outlast.
     */
    private static final int COOLDOWN_TICKS = 120;
    private static final float ENERGY_COST = EnergyWeight.STANDARD.cost();

    /** The bare-handed version: fewer hits, less reach, and it hurts less. */
    private static final double BODY_REACH = 3.5;
    private static final int BODY_HITS = 5;
    private static final int BODY_HIT_INTERVAL = 3;
    private static final float BODY_HIT_DAMAGE = 1.0F;

    private BarrageSkill() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "skill.jojoha.barrage";
    }

    @Override
    public int cooldownTicks() {
        return COOLDOWN_TICKS;
    }

    @Override
    public float energyCost() {
        return ENERGY_COST;
    }

    /** Throwable bare-handed - see the class note on the two paths. */
    @Override
    public boolean requiresStand() {
        return false;
    }

    /**
     * Always fires. There is nothing to aim at - a barrage is thrown at the space in front of the
     * user and connects with whatever is standing in it.
     */
    /**
     * Held rather than tapped, when there is a Stand to throw it.
     *
     * <p>A flurry is the one move where how long it goes on is the move. Three fixed seconds made it
     * a thing you set off and watched; holding it makes it something you lean into, and the Stand
     * presses further forward the longer it runs (see StandMovementProfile.barragePush). There is
     * still a ceiling, so it is a held move rather than a toggle.
     *
     * <p>The bare-handed version is untouched and stays a tap. It is a short burst thrown from the
     * user's own body with no second party to sustain anything, and there is nothing there to hold.
     */
    @Override
    public boolean isSustained() {
        return true;
    }

    /**
     * Running exactly when the Stand says it is.
     *
     * <p>Read off the entity rather than tracked here, because this is asked on the client too - it
     * is what decides whether releasing the key should send a stop - and the entity is the only
     * thing about a flurry that both sides can see.
     */
    @Override
    public boolean isSustainActive(net.minecraft.world.entity.player.Player player) {
        StandEntity stand = StandEntity.findFor(player);
        return stand != null && stand.isHeldBarrageRunning();
    }

    @Override
    public boolean activate(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        if (stand != null) {
            // The same packet arrives to start and to stop - see StandSkillInput - so which one this
            // is depends on whether one is already running.
            if (stand.isHeldBarrageRunning()) {
                stand.endHeldBarrage();
            } else {
                stand.beginHeldBarrage();
            }
            return true;
        }

        float damage = StandTuning.damage("barrage_body_hit", BODY_HIT_DAMAGE)
                * org.gumel.jojoha.data.StatEffects.bodyDamageScale(data.strength);
        PlayerFlurry.begin(player, BODY_HITS, BODY_HIT_INTERVAL, damage, BODY_REACH);
        return true;
    }
}
