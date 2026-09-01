package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.StandTuning;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.StandSkill;

/**
 * A rising blow that puts the target in the air.
 *
 * <p>One of the two moves that work with or without a Stand. Whoever throws it is decided at the
 * moment of use rather than being two separate moves, so it stays in the same slot and behaves the
 * same way whether or not the Stand is currently out - it simply hits harder when it is.
 *
 * <p>Launch is applied as a replacement for vertical motion rather than an addition, so a target
 * that was already falling still gets picked up instead of having the blow cancelled out by its own
 * descent. Horizontal motion is left alone, which keeps the target roughly where it was hit.
 */
public final class UppercutSkill implements StandSkill {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "uppercut");
    public static final UppercutSkill INSTANCE = new UppercutSkill();

    /**
     * How long the Stand's fist takes to arrive, in ticks.
     *
     * <p>Short enough that the move still feels like a press, long enough that the punch animation
     * has visibly started swinging before anything is hurt by it.
     */
    private static final int WINDUP_TICKS = 5;

    private static final int COOLDOWN_TICKS = 50;
    private static final float ENERGY_COST = EnergyWeight.LIGHT.cost();

    private static final double UNARMED_REACH = 4.0;
    // Has to stay clearly above a punch: it costs energy, sits on a cooldown and only lands on one
    // target, so a launcher worth six against a punch worth four is not worth the slot.
    private static final float STAND_DAMAGE = 7.0F;
    private static final float BODY_DAMAGE = 3.0F;
    private static final double STAND_LAUNCH = 1.05;
    private static final double BODY_LAUNCH = 0.72;

    private UppercutSkill() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "skill.jojoha.uppercut";
    }

    @Override
    public int cooldownTicks() {
        return COOLDOWN_TICKS;
    }

    @Override
    public float energyCost() {
        return ENERGY_COST;
    }

    /** Usable bare-handed, so it must not require a manifested Stand. */
    @Override
    public boolean requiresStand() {
        return false;
    }

    @Override
    public boolean activate(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        boolean withStand = stand != null;
        double reach = withStand ? SkillTargeting.REACH : UNARMED_REACH;

        LivingEntity target = SkillTargeting.lookTarget(player, reach);
        if (target == null) {
            return false;
        }

        ServerLevel level = player.serverLevel();
        // Only the Stand's version is offered to the passives. The other one is the player's own
        // fist, and a Stand passive has no business scaling a punch its Stand did not throw.
        float damage = withStand
                ? org.gumel.jojoha.stand.passive.StandPassives.scaleOutgoing(player, data, target,
                        StandTuning.damage("uppercut_stand", STAND_DAMAGE) * data.stand.powerScale())
                : StandTuning.damage("uppercut_body", BODY_DAMAGE)
                        * org.gumel.jojoha.data.StatEffects.bodyDamageScale(data.strength);
        if (withStand) {
            // The swing first, and the blow when it arrives. Borrowing the punch animation: there is
            // no uppercut of its own yet, and a rising blow reads far better as a mistimed punch
            // than as Star Finger's precision thrust.
            stand.triggerPunchAt(target, false);
            org.gumel.jojoha.stand.skill.StandBeat.after(WINDUP_TICKS, player, target,
                    (attacker, hit) -> land(attacker, hit, damage, STAND_LAUNCH));
        } else {
            // The player's own fist has no animation to wait for, so it lands on the tick it is
            // thrown. Delaying it would be a pause with nothing in it.
            land(player, target, damage, BODY_LAUNCH);
        }
        return true;
    }

    /**
     * The blow itself: damage, the launch, and the noise of it.
     *
     * <p>Split out because the Stand's version arrives a few ticks after the key and the player's
     * own arrives at once - see StandBeat. Everything either of them does is here, so the two can
     * never drift into doing different things.
     */
    private static void land(ServerPlayer player, LivingEntity target, float damage, double launch) {
        ServerLevel level = player.serverLevel();

        target.hurt(level.damageSources().playerAttack(player), damage);

        Vec3 motion = target.getDeltaMovement();
        target.setDeltaMovement(motion.x, launch, motion.z);
        target.hurtMarked = true;

        Vec3 at = target.position().add(0, target.getBbHeight() * 0.4, 0);
        level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 12, 0.25, 0.35, 0.25, 0.1);
        level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG,
                SoundSource.PLAYERS, 1.0F, launch == BODY_LAUNCH ? 1.1F : 0.9F);
    }
}
