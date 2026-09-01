package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.registry.ModEffects;
import org.gumel.jojoha.registry.ModRegistries;
import org.gumel.jojoha.registry.ModSounds;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.StandSummonHandler;
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.StandSkill;

/**
 * The one honest punch Hermit Purple has.
 *
 * <p>Two beats, and the order is the whole move: the vine takes hold and hauls them in, and the fist
 * is waiting where they arrive. Reversed - punch first, drag second - it is two unrelated things
 * happening near each other. Done this way the pull is what makes the hit land, and a player who
 * misses the grab does not get the damage either.
 *
 * <h2>Why the punch waits</h2>
 *
 * <p>Because the pull has to be seen. Applying both on the same tick reads as a hit at range with
 * some particles on it - the victim is already next to you by the time the first frame is drawn, so
 * nothing appears to have travelled. Half a second of them actually crossing the gap is what makes
 * the punch feel like the end of something.
 *
 * <p>The wait is also the counterplay. It is the window in which the target can be blocked for,
 * healed, or simply walked away from by a third party, and a move with no window is a move with no
 * answer.
 */
public final class TwistingGutPunchSkill implements StandSkill {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "twisting_gut_punch");

    public static final TwistingGutPunchSkill INSTANCE = new TwistingGutPunchSkill();

    private static final int COOLDOWN_TICKS = 70;
    private static final float ENERGY_COST = EnergyWeight.HEAVY.cost();

    /** How far the vine reaches for this, and how forgiving the aim is. */
    private static final double REACH = 11.0;
    private static final double FORGIVENESS = 2.0;

    /** How long they spend being dragged, and how close is close enough to stop. */
    private static final int PULL_TICKS = 10;
    private static final double ARRIVE = 2.1;
    private static final double PULL_SPEED = 0.9;

    /** The punch itself. Heavy, because it is the only one this Stand throws. */
    private static final float DAMAGE = 11.0F;
    private static final double PUNCH_AWAY = 0.85;
    private static final double PUNCH_UP = 0.28;
    private static final int WINDED_TICKS = 45;

    private TwistingGutPunchSkill() {
    }

    /**
     * Nothing to start any more - the dragging lives in VineHaul, which has its own clock.
     *
     * <p>Kept as a no-op rather than deleted so the call in Jojoha.init stays put, and so anything
     * this move later needs at start-up has an obvious home.
     */
    public static void init() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "skill.jojoha.twisting_gut_punch";
    }

    @Override
    public int cooldownTicks() {
        return COOLDOWN_TICKS;
    }

    @Override
    public float energyCost() {
        return ENERGY_COST;
    }

    @Override
    public boolean activate(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        LivingEntity caught = look(player);
        if (caught == null) {
            return false;
        }

        ServerLevel level = player.serverLevel();

        // In, then the fist. The same haul the whip uses - see VineHaul.
        VineHaul.begin(player, caught, PULL_TICKS, TwistingGutPunchSkill::punch);

        // The rope stays up for the whole haul, because here it is doing the hauling - the whip's
        // is over in a snap, this one is the thing dragging them across the gap.
        org.gumel.jojoha.network.NetworkHandler.sendThornLash(level, player, caught, PULL_TICKS + 2);

        vine(level, player.getEyePosition(),
                caught.position().add(0, caught.getBbHeight() * 0.5, 0));

        if (stand != null) {
            stand.triggerGrabAt(caught);
        }
        return true;
    }

    /** The vine going out, drawn as motes along the line it takes. */
    private static void vine(ServerLevel level, Vec3 from, Vec3 to) {
        // Sparse, because the rope is really drawn now - see ThornLashFx. What is left is thorns
        // shedding off it rather than a dotted line pretending to be it.
        int steps = 10;
        Vec3 step = to.subtract(from).scale(1.0 / steps);

        for (int i = 2; i <= steps; i += 3) {
            Vec3 at = from.add(step.scale(i));
            level.sendParticles(ModRegistries.STAND_AURA.get(), at.x, at.y, at.z, 1,
                    0.08, 0.08, 0.08, 0.0);
        }

        level.playSound(null, BlockPos.containing(from), SoundEvents.WEEPING_VINES_PLACE,
                SoundSource.PLAYERS, 0.85F, 1.25F);
    }

    /**
     * The fist, once the vine has brought them to it.
     *
     * <p>Static, and takes both parties, because that is the shape VineHaul calls back with - the
     * dragging is not this class's business any more and neither is remembering who was being
     * dragged.
     */
    private static void punch(ServerPlayer holder, LivingEntity target) {
        ServerLevel level = holder.serverLevel();
        JojohaPlayerData data = org.gumel.jojoha.data.PlayerDataAccess.get(holder);
        Vec3 gut = target.position().add(0, target.getBbHeight() * 0.45, 0);

        float damage = org.gumel.jojoha.stand.passive.StandPassives.scaleOutgoing(
                holder, data, target, DAMAGE);
        target.hurt(level.damageSources().playerAttack(holder), damage);

        // Backward off the fist, and barely upward. A gut punch folds somebody over it - sending
        // them into the air would read as an uppercut, which is a different move entirely.
        Vec3 away = target.position().subtract(holder.position());
        Vec3 flat = new Vec3(away.x, 0, away.z);
        Vec3 push = flat.lengthSqr() < 1.0E-4 ? holder.getLookAngle() : flat.normalize();

        target.setDeltaMovement(push.x * PUNCH_AWAY, PUNCH_UP, push.z * PUNCH_AWAY);
        target.hurtMarked = true;

        // Winded rather than stunned outright: they keep their feet, they just cannot do anything
        // with them for a moment.
        target.addEffect(new MobEffectInstance(ModEffects.stun(), WINDED_TICKS, 0,
                false, false, true));

        // Rings square on the point of contact, and the air going out of them.
        for (int ring = 0; ring < 4; ring++) {
            level.sendParticles(ModRegistries.IMPACT_RING.get(), gut.x, gut.y, gut.z,
                    0, 1.0 + ring * 0.8, 1.0, 0.0, 1.0);
        }
        level.sendParticles(ParticleTypes.CRIT, gut.x, gut.y, gut.z, 24, 0.3, 0.25, 0.3, 0.45);
        level.sendParticles(ParticleTypes.CLOUD, gut.x, gut.y, gut.z, 10, 0.25, 0.2, 0.25, 0.14);
        level.sendParticles(ModRegistries.STAND_AURA.get(), gut.x, gut.y, gut.z, 12,
                0.3, 0.3, 0.3, 0.04);

        level.playSound(null, BlockPos.containing(gut), ModSounds.STAND_HIT.get(),
                SoundSource.PLAYERS, 1.2F, 0.85F);
        level.playSound(null, BlockPos.containing(gut), SoundEvents.PLAYER_ATTACK_STRONG,
                SoundSource.PLAYERS, 0.9F, 0.8F);

        StandEntity stand = StandSummonHandler.findStand(holder, data);
        if (stand != null) {
            stand.triggerPunchAt(target, true);
        }
    }

    /** Aim is VineHaul's, so this and the whip feel identical to point. */
    private static LivingEntity look(ServerPlayer player) {
        return VineHaul.look(player, REACH, FORGIVENESS);
    }
}
