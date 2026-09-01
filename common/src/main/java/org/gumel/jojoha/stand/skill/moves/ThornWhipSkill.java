package org.gumel.jojoha.stand.skill.moves;

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
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.StandSkill;

/**
 * The lasso, used the way you would use a whip.
 *
 * <p>Same vine and the same throw; the difference is what happens once it has hold of something. The
 * lasso keeps the mob - it is for leading things about - and this one lets go immediately and puts
 * everything into the release, which is for getting something away from you.
 *
 * <p>Which is why it replaces the lasso rather than sitting beside it. Both are one vine round one
 * target, and a bar carrying the pair would be two keys that look identical and differ only in
 * whether the mob is still attached afterwards. See StandSkill.replaces.
 *
 * <h2>In, then out</h2>
 *
 * <p>The vine takes hold and hauls them to you before it throws, which is the crack of a whip rather
 * than a shove: the distance they cover coming in is what makes the distance going out read as
 * force. Throwing on contact moved them the same number of blocks and looked like a push.
 *
 * <p>It also means the two halves can disagree in direction. They are pulled toward you and then
 * flung along wherever you are looking by then - so a player who turns during the haul redirects the
 * throw, and the move rewards paying attention rather than pressing a button.
 *
 * <h2>Thrown outward, not upward</h2>
 *
 * <p>The obvious spelling of "throw them back" is a big vertical launch, and it is the wrong one for
 * a whip: something flung straight up lands more or less where it started, which undoes the point.
 * The lift here is a fraction of the push, enough to take them off their feet so the ground stops
 * slowing them down, and the rest is distance.
 */
public final class ThornWhipSkill implements StandSkill {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "thorn_whip");

    public static final ThornWhipSkill INSTANCE = new ThornWhipSkill();

    private static final int COOLDOWN_TICKS = 45;
    private static final float ENERGY_COST = EnergyWeight.STANDARD.cost();

    /** How far the whip reaches, and how wide a miss it forgives. */
    private static final double REACH = 13.0;
    private static final double FORGIVENESS = 2.2;

    /** What it does on contact: a graze from the thorns, then the throw. */
    private static final float DAMAGE = 5.0F;

    /**
     * How hard they go out, now that they are coming in first.
     *
     * <p>Raised from 2.4. The haul spends the gap it used to throw them across, so the same number
     * would have landed them roughly where they started - the throw has to beat the pull before it
     * moves anybody anywhere.
     */
    private static final double THROW_AWAY = 3.6;
    private static final double THROW_UP = 0.72;

    /** How long the haul is given before the throw fires anyway. */
    private static final int HAUL_TICKS = 8;

    /** Long enough to land badly, short enough not to be a stun-lock. */
    private static final int STUN_TICKS = 25;

    /** How long the vine stays on screen after the snap, in ticks. */
    private static final int LASH_TICKS = 7;

    /** Vine segments drawn along the snap, and debris where it bites. */
    private static final int LASH_STEPS = 10;
    private static final int RING_COUNT = 3;

    private ThornWhipSkill() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "skill.jojoha.thorn_whip";
    }

    /** The lasso grown up. One or the other on the bar, never both. */
    @Override
    public ResourceLocation replaces() {
        return LassoOfThornsSkill.ID;
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
        ServerLevel level = player.serverLevel();

        LivingEntity caught = look(player);
        if (caught == null) {
            return false;
        }

        Vec3 from = player.getEyePosition();
        Vec3 at = caught.position().add(0, caught.getBbHeight() * 0.5, 0);

        // The rope stays up for the haul, because the haul is what it is doing.
        org.gumel.jojoha.network.NetworkHandler.sendThornLash(level, player, caught,
                HAUL_TICKS + 2);
        lash(level, from, at);

        // In first, and the throw when they get here - see the class note.
        VineHaul.begin(player, caught, HAUL_TICKS, ThornWhipSkill::fling);

        if (stand != null) {
            stand.triggerGrabAt(caught);
        }
        return true;
    }

    /** The crack: they arrive, and they leave considerably faster. */
    private static void fling(ServerPlayer player, LivingEntity caught) {
        ServerLevel level = player.serverLevel();
        JojohaPlayerData data = org.gumel.jojoha.data.PlayerDataAccess.get(player);

        float damage = org.gumel.jojoha.stand.passive.StandPassives.scaleOutgoing(
                player, data, caught, DAMAGE);
        caught.hurt(level.damageSources().playerAttack(player), damage);

        // Along wherever the thrower is looking now, not along the line the vine took on the way
        // out. The haul is a beat long and a player who spends it turning has earned the redirect.
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        Vec3 push = flat.lengthSqr() < 1.0E-4 ? player.getLookAngle() : flat.normalize();

        caught.setDeltaMovement(push.x * THROW_AWAY, THROW_UP, push.z * THROW_AWAY);
        caught.hurtMarked = true;

        caught.addEffect(new MobEffectInstance(ModEffects.stun(), STUN_TICKS, 0,
                false, false, true));

        impact(level, caught.position().add(0, caught.getBbHeight() * 0.5, 0));
    }

    /**
     * The vine, drawn as it goes out.
     *
     * <p>Particles rather than the rope renderer, because there is no entity here to hang a rope on -
     * the whip exists for a single tick and the throw is already over by the time anything could be
     * drawn from it. A line of motes along the same path reads as the same vine at a fraction of the
     * machinery.
     */
    private static void lash(ServerLevel level, Vec3 from, Vec3 to) {
        // Thinned right down now that the rope itself is drawn. These are the thorns coming off it,
        // not a stand-in for it - a full line of motes over a real vine reads as two ropes.
        Vec3 step = to.subtract(from).scale(1.0 / LASH_STEPS);

        for (int i = 2; i <= LASH_STEPS; i += 3) {
            Vec3 at = from.add(step.scale(i));
            level.sendParticles(ModRegistries.STAND_AURA.get(), at.x, at.y, at.z, 1,
                    0.08, 0.08, 0.08, 0.0);
        }

        level.playSound(null, net.minecraft.core.BlockPos.containing(from),
                SoundEvents.WEEPING_VINES_PLACE, SoundSource.PLAYERS, 0.9F, 1.4F);
    }

    /** And where it bites: rings out, thorns off, and the crack of it. */
    private static void impact(ServerLevel level, Vec3 at) {
        for (int ring = 0; ring < RING_COUNT; ring++) {
            level.sendParticles(ModRegistries.IMPACT_RING.get(), at.x, at.y, at.z,
                    0, 1.1 + ring * 0.7, 1.0, 0.0, 1.0);
        }

        level.sendParticles(ModRegistries.STAND_AURA.get(), at.x, at.y, at.z, 16,
                0.35, 0.35, 0.35, 0.05);
        level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 14, 0.3, 0.3, 0.3, 0.3);

        level.playSound(null, net.minecraft.core.BlockPos.containing(at),
                ModSounds.STAND_HIT.get(), SoundSource.PLAYERS, 1.0F, 1.15F);
    }

    /** Aim is VineHaul's, because the gut punch aims the same way and they should feel alike. */
    private static LivingEntity look(ServerPlayer player) {
        return VineHaul.look(player, REACH, FORGIVENESS);
    }
}
