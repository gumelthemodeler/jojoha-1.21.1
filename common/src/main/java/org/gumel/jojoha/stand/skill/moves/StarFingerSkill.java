package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.StandTuning;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.TrustTier;
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.StandSkill;

/**
 * Star Platinum's extending finger - a single precise stab well past its normal reach.
 *
 * <p>Signature rather than generic, and gated to BONDED: the doc reserves the full moveset for full
 * trust, and this is the move that breaks Star Platinum's defining limitation. A close-range Stand
 * suddenly threatening something three blocks further out should feel like it was earned.
 *
 * <p>Its identity is precision, not weight, so it is written as one high-damage point strike with a
 * longer reach and no knockback - the opposite profile to the barrage sharing its slot bar.
 */
public final class StarFingerSkill implements StandSkill {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "star_finger");
    public static final StarFingerSkill INSTANCE = new StarFingerSkill();

    private static final int COOLDOWN_TICKS = 45;
    private static final float ENERGY_COST = EnergyWeight.STANDARD.cost();
    /** Reaches past the Stand's usual 7 blocks - that extra distance is the whole point of the move. */
    private static final double REACH = 10.0;
    private static final float DAMAGE = 7.0F;

    private StarFingerSkill() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "skill.jojoha.star_finger";
    }

    @Override
    public int cooldownTicks() {
        return COOLDOWN_TICKS;
    }

    @Override
    public float energyCost() {
        return ENERGY_COST;
    }

    /**
     * How long the finger takes to reach full extension, in ticks.
     *
     * <p>Read off the animation rather than chosen: the finger bone's scale peaks at 0.7917 seconds
     * into star_finger, which is just under sixteen ticks.
     */
    private static final int EXTEND_TICKS = 16;

    @Override
    public TrustTier minimumTrust() {
        return TrustTier.BONDED;
    }

    @Override
    public boolean activate(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        LivingEntity target = SkillTargeting.lookTarget(player, REACH);
        if (target == null) {
            return false;
        }

        // Scaled by Power, the doc's "general strength of your stand".
        float damage = org.gumel.jojoha.stand.passive.StandPassives.scaleOutgoing(player, data, target,
                StandTuning.damage("star_finger", DAMAGE) * data.stand.powerScale());

        stand.triggerStarFingerPose(target);
        org.gumel.jojoha.stand.skill.StandBeat.after(EXTEND_TICKS, player, target,
                (attacker, hit) -> land(attacker, hit, damage));
        return true;
    }

    /**
     * The hit, once the finger has actually got there.
     *
     * <p>Everything about this move is the reach, and resolving it on the key press threw that
     * away: the target took the damage on the tick the button went down and the finger extended
     * afterwards, over an outcome that had already happened. Testers read it exactly as it played -
     * "he punches them" - because a blow that lands before the animation is a punch no matter what
     * the model does next.
     *
     * <p>The extension peaks at 0.79 seconds into the clip, where the finger bone scales to fifteen
     * times its length. That moment is what EXTEND_TICKS names.
     *
     * <h2>Re-checked on arrival, unlike the uppercut</h2>
     *
     * <p>Sixteen ticks is long enough to walk out of a fight. The uppercut can skip this because its
     * wind-up is five ticks and its target is already against the Stand's fist; here the target was
     * a good distance away to begin with, and hitting somebody who has since stepped behind cover
     * would be the reach going through a wall.
     */
    private static void land(ServerPlayer player, LivingEntity target, float damage) {
        ServerLevel level = player.serverLevel();

        if (player.distanceTo(target) > REACH) {
            return;
        }

        target.hurt(level.damageSources().playerAttack(player), damage);

        // No drawn trail: the animation extends the finger itself, so the reach is already visible
        // in the model and a line of particles on top of it only doubles up on what it shows.
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT,
                SoundSource.PLAYERS, 0.9F, 1.5F);
    }
}
