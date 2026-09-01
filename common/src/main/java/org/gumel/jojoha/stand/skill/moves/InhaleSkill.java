package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.registry.ModRegistries;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.StandSkill;

import java.util.List;

/**
 * Star Platinum takes an enormous breath - and everything nearby comes with it, or gets sent away.
 *
 * <p>One slot, both directions. Inhaling drags entities toward the user; sneaking turns the move
 * around and blows them out instead. Pairing them is not just slot economy - drawing something in
 * and then flinging it is a combination, and splitting the two across separate cooldowns would stop
 * that reading as one motion.
 *
 * <p>Pull strength falls off with distance so something at the edge drifts in rather than being
 * snapped to the user's feet, and the draw is capped, ramped and stopped at arm's length so what
 * arrives stands there to be hit instead of being fired through the user - see {@link InhaleChannel}.
 */
public final class InhaleSkill implements StandSkill {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "inhale");
    public static final InhaleSkill INSTANCE = new InhaleSkill();

    private static final int COOLDOWN_TICKS = 140;
    private static final float ENERGY_COST = EnergyWeight.HEAVY.cost();

    /**
     * How long the breath is held, and how far it reaches.
     *
     * <p>Long and wide on purpose - the move is a lungful of air, not a shove. Three seconds is
     * enough time for something heavy at the far edge to actually make the journey, which is the
     * difference between a pull that works on a zombie and one that works on a boss.
     */
    private static final int CHANNEL_TICKS = 60;
    private static final double RADIUS = 18.0;

    /** Applied per tick for the length of the channel, so these are small numbers on purpose. */
    private static final double PULL_STRENGTH = 0.22;
    private static final double BLOW_STRENGTH = 0.20;

    /**
     * How long the user cannot be touched after taking the breath.
     *
     * <p>The move ends with everything nearby standing on top of the user, which is the opening the
     * barrage and the uppercut are for - and also the moment they are most likely to be hit from
     * three directions at once. A second and a quarter is enough to turn and commit to a follow-up
     * and not much else; it is a combo window, not a shield.
     *
     * <p>Granted on the exhale too. It is one move on one cooldown, and a defensive property that
     * appeared or vanished depending on whether the user happened to be crouching would be harder
     * to learn than the small amount of balance it buys back.
     */
    private static final int IFRAME_TICKS = 25;

    /** How long the Stand keeps standing there after the pull stops, in ticks. */
    private static final int HOLD_OVERRUN_TICKS = 12;

    // The visible stream. Links are spaced evenly along the breath so they form a row rather than a
    // cloud, and each is nudged off the axis a little so a straight line of identical sprites
    // doesn't read as one stretched decal.
    //
    // Counts were roughly halved. The stream was busy rather than strong - a dense crowd of small
    // sprites crossing the view is what makes an effect hard to see past, and it was competing with
    // the thing the move exists to show you, which is what is being dragged in. Fewer and larger
    // covers the same air with a fraction of the edges in it. See InhaleWindParticle for the other
    // half of this: they are also bigger, softer and no longer fullbright.
    private static final int WIND_LINKS = 4;
    private static final double WIND_ROW_LENGTH = 7.0;
    private static final double WIND_JITTER = 0.55;
    private static final double WIND_SPEED = 0.55;

    private static final int SMOKE_PUFFS = 6;
    /** Puffs start where the air is coming from, then converge - see InhaleSmokeParticle. */
    private static final double SMOKE_SPREAD = 2.2;

    private InhaleSkill() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "skill.jojoha.inhale";
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
        boolean blowing = player.isShiftKeyDown();

        // The Stand plants itself and does the breathing. Held a little past the channel so it is
        // still standing there as the last of what it caught arrives, rather than turning for home
        // on the same tick.
        stand.beginInhale(player, CHANNEL_TICKS + HOLD_OVERRUN_TICKS);

        Vec3 axis = player.getLookAngle();

        // The pull itself runs for the length of the channel - see InhaleChannel for why a single
        // impulse cannot move anything that steers itself, and why it gathers on the Stand.
        InhaleChannel.begin(player, stand, axis, CHANNEL_TICKS, RADIUS,
                blowing ? BLOW_STRENGTH : PULL_STRENGTH, blowing);

        data.inhaleIFrameTicks = IFRAME_TICKS;

        // First frame here so the air appears on the same tick as the sound; the channel keeps it
        // going from the next one. Drawn from the Stand's own chest, which is where the breath is.
        Vec3 origin = stand.position().add(0, stand.getBbHeight() * 0.5, 0);
        spawnWindRow(level, origin, axis, blowing);
        spawnSmoke(level, origin, axis, blowing);
        // Both spelled out rather than picking the SoundEvent in a ternary: vanilla stores some of
        // these as a bare SoundEvent and others as a Holder, so the two branches have no common
        // type that playSound accepts.
        if (blowing) {
            level.playSound(null, player.blockPosition(), SoundEvents.BREEZE_WIND_CHARGE_BURST.value(),
                    SoundSource.PLAYERS, 0.8F, 1.3F);
        } else {
            level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE,
                    SoundSource.PLAYERS, 0.8F, 1.6F);
        }
        return true;
    }
    /**
     * Lays the wind out as a row along the breath.
     *
     * <p>Every link is spawned on the same tick at a different point down the axis, which is what
     * makes the stream read as one continuous current with a direction rather than a puff expanding
     * from a point. Inhaling runs each link back toward the user; exhaling drives them out.
     */
    static void spawnWindRow(ServerLevel level, Vec3 origin, Vec3 look, boolean blowing) {
        RandomSource random = level.getRandom();

        // A stable pair of axes across the breath, used to scatter links off the centre line.
        Vec3 side = look.cross(new Vec3(0, 1, 0));
        if (side.lengthSqr() < 1.0E-4) {
            side = new Vec3(1, 0, 0);
        }
        side = side.normalize();
        Vec3 up = side.cross(look).normalize();

        Vec3 travel = look.scale(blowing ? WIND_SPEED : -WIND_SPEED);

        for (int i = 0; i < WIND_LINKS; i++) {
            double along = (i + 1) / (double) WIND_LINKS * WIND_ROW_LENGTH;
            Vec3 at = origin.add(look.scale(along))
                    .add(side.scale((random.nextDouble() - 0.5) * WIND_JITTER))
                    .add(up.scale((random.nextDouble() - 0.5) * WIND_JITTER));

            // count=0 makes the velocity slots mean velocity rather than a spread radius, which is
            // what lets every link be aimed along the same heading.
            level.sendParticles(ModRegistries.INHALE_WIND.get(), at.x, at.y, at.z, 0,
                    travel.x, travel.y, travel.z, 1.0);
        }
    }

    /**
     * Seeds smoke into the airstream and lets it find its own way to the user.
     *
     * <p>Only the direction of the breath is sent; each puff works out its own acceleration
     * client-side, so the converging motion stays smooth instead of arriving in tick-sized steps.
     */
    static void spawnSmoke(ServerLevel level, Vec3 origin, Vec3 look, boolean blowing) {
        Vec3 centre = origin.add(look.scale(blowing ? 1.2 : WIND_ROW_LENGTH * 0.6));
        RandomSource random = level.getRandom();

        for (int i = 0; i < SMOKE_PUFFS; i++) {
            double x = centre.x + (random.nextDouble() - 0.5) * SMOKE_SPREAD;
            double y = centre.y + (random.nextDouble() - 0.5) * SMOKE_SPREAD;
            double z = centre.z + (random.nextDouble() - 0.5) * SMOKE_SPREAD;

            // The middle slot is the direction flag, not a speed - see the particle's provider.
            level.sendParticles(ModRegistries.INHALE_SMOKE.get(), x, y, z, 0,
                    0.0, blowing ? -1.0 : 1.0, 0.0, 1.0);
        }
    }
}
