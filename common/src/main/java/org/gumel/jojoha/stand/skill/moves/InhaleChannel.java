package org.gumel.jojoha.stand.skill.moves;

import dev.architectury.event.events.common.TickEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.stand.StandEntity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The breath, held open for a few seconds rather than resolved in one frame.
 *
 * <p>It gathers on the <em>Stand</em>, not on its user. The Stand plants itself a few blocks ahead,
 * stops following, and everything caught by the breath is drawn to where it is standing - so the
 * move leaves a pile of mobs at a fixed spot on the ground rather than in the player's own hitbox.
 * That is what makes it something to follow up on: the user is free to walk around it, line an
 * uppercut up on what is arriving, or simply stand beside it and swing. Anchoring the pull on the
 * player instead meant the destination moved whenever they did, and the only place anything could
 * ever arrive was on top of them.
 *
 * <p>A single impulse cannot move anything that steers itself. A mob writes its own velocity every
 * tick from its AI, so one shove is overwritten before it has travelled anywhere - which is exactly
 * why the earlier version could not budge a boss no matter how large the number was. Pulling every
 * tick means the mob and the breath are fighting over the same value each frame, and the breath
 * wins as long as it is still going.
 *
 * <p>Pull strength is eased off at the edge, so distant things start drifting rather than snapping
 * on at some boundary. Nothing is teleported: whatever comes in does so by actually crossing the
 * ground between, which is what makes it look like suction rather than a summon.
 *
 * <p>What stops it being a yank is the speed cap. Force is <em>added</em> to whatever the entity was
 * already doing, every tick - so with nothing to hold it back the velocity compounds against drag
 * alone and settles somewhere near forty blocks a second, which is not a suction, it is a hook.
 * Force is only applied while the entity is still closing slower than {@link #DRAW_SPEED}, so
 * runaway acceleration becomes a steady draw at a speed the move picks - and because that speed is
 * itself ramped from {@link #DRAW_SPEED_START} to {@link #DRAW_SPEED_FULL} across the length of the
 * breath, the draw builds rather than arriving. The hold radius does the rest: things park at arm's
 * length instead of firing through the user and out the far side.
 */
final class InhaleChannel {
    /** How often the air is redrawn while the breath is held. */
    private static final int PARTICLE_INTERVAL_TICKS = 5;

    /**
     * How sharply the breath builds, as an exponent on how far through the channel it is.
     *
     * <p>The ramp used to be a short lead-in - full strength by the first second, flat for the
     * remaining two. That is a breath with a soft attack, not one that builds, and what it looked
     * like was the pull switching on. Spreading it across the whole channel and bending it means
     * there is no point at which the strength is simply the strength: it is always still rising.
     *
     * <p>Squared rather than linear because the interesting half is the end. A linear build spends
     * its first half already hauling; this one barely stirs anything for the opening second, which
     * is what makes the last one read as the lungs really going.
     */
    private static final double RAMP_EXPONENT = 2.0;

    /**
     * The draw speed at the very start of the breath and at the end of it, in blocks per tick.
     *
     * <p>Ramped between the two rather than held at the ceiling, which is what actually makes the
     * pull gradual. Capping the force alone is not enough: a low force applied every tick still
     * accelerates to whatever drag allows, so a weak opening would have looked identical to a
     * strong one within a second. Moving the cap is what makes early weak and late strong.
     *
     * <p>The floor is a drift - about a block and a half a second, enough to see something has
     * started. The ceiling is twelve a second, which crosses the radius comfortably in what is left
     * of the channel. See the class note for why a cap is the thing that makes this a suction.
     */
    private static final double DRAW_SPEED_START = 0.08;
    private static final double DRAW_SPEED_FULL = 0.6;

    /**
     * How close is close enough, in blocks.
     *
     * <p>Inside this the pull stops entirely rather than continuing to haul toward a point the
     * entity is already standing on - that used to fire things straight through the middle, at
     * which point the pull caught them from the far side and hauled them back, and the target spent
     * the channel juddering instead of standing there to be hit.
     *
     * <p>Tightened from 2.5 when the breath moved onto the Stand, because the distance is now
     * measured from a different thing. Two and a half blocks from the player was arm's length; two
     * and a half from a Stand planted nearly three blocks ahead of them put the pile over five
     * blocks away, which is past anything the user can swing at. At 1.6 - the Stand's own engage
     * range, where its punches land - what arrives clusters tight against the Stand and stays
     * inside both the uppercut's seven-block reach and a short walk of the user.
     */
    private static final double HOLD_DISTANCE = 1.6;

    private static final List<Channel> ACTIVE = new ArrayList<>();

    private InhaleChannel() {
    }

    static void init() {
        TickEvent.SERVER_POST.register(server -> tick());
    }

    static void begin(ServerPlayer player, StandEntity stand, Vec3 axis, int durationTicks,
                      double radius, double strength, boolean blowing) {
        // Re-casting replaces rather than stacks, so holding the key cannot multiply the force.
        ACTIVE.removeIf(channel -> channel.player == player);
        ACTIVE.add(new Channel(player, stand, axis, durationTicks, radius, strength, blowing));
    }

    private static void tick() {
        if (ACTIVE.isEmpty()) {
            return;
        }

        Iterator<Channel> iterator = ACTIVE.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().step()) {
                iterator.remove();
            }
        }
    }

    private static final class Channel {
        private final ServerPlayer player;
        private final StandEntity stand;
        /**
         * Which way the breath is aimed, taken once at the cast.
         *
         * <p>Held rather than re-read from the user's crosshair each tick, so the stream keeps
         * pointing where the move was aimed while its user turns to deal with what arrives.
         */
        private final Vec3 axis;
        private final double radius;
        private final double strength;
        private final boolean blowing;
        private final int duration;

        private int ticksLeft;

        private Channel(ServerPlayer player, StandEntity stand, Vec3 axis, int ticksLeft,
                        double radius, double strength, boolean blowing) {
            this.player = player;
            this.stand = stand;
            this.axis = axis;
            this.ticksLeft = ticksLeft;
            this.duration = ticksLeft;
            this.radius = radius;
            this.strength = strength;
            this.blowing = blowing;
        }

        /**
         * The point everything is drawn to, at the Stand's chest.
         *
         * <p>Falls back to the user if the Stand is gone - dismissed mid-breath, or killed. The
         * breath should finish somewhere rather than stop dead, and the user is the only other
         * place it could sensibly finish.
         */
        private Vec3 focus() {
            if (stand == null || !stand.isAlive()) {
                return player.position().add(0, player.getBbHeight() * 0.5, 0);
            }
            return stand.position().add(0, stand.getBbHeight() * 0.5, 0);
        }

        /** @return true when the breath is spent and the channel should be dropped. */
        private boolean step() {
            if (--ticksLeft <= 0 || !player.isAlive()) {
                return true;
            }

            ServerLevel level = player.serverLevel();
            Vec3 focus = focus();

            // Kept running for the whole channel rather than only at the cast. A pull that lasts
            // three seconds behind a one-frame puff of air reads as broken.
            if (ticksLeft % PARTICLE_INTERVAL_TICKS == 0) {
                InhaleSkill.spawnWindRow(level, focus, axis, blowing);
                InhaleSkill.spawnSmoke(level, focus, axis, blowing);
            }

            // How much of the breath is in yet, across the whole of it rather than a lead-in.
            double ramp = Math.pow(Math.min(1.0, (duration - ticksLeft) / (double) duration),
                    RAMP_EXPONENT);
            double drawSpeed = DRAW_SPEED_START + (DRAW_SPEED_FULL - DRAW_SPEED_START) * ramp;

            // Centred on the breath rather than on the player, so the reach is measured from the
            // thing doing the inhaling.
            AABB area = new AABB(focus, focus).inflate(radius);

            for (Entity entity : level.getEntities(player, area,
                    candidate -> candidate.isAlive() && !(candidate instanceof StandEntity))) {

                Vec3 toUser = focus.subtract(entity.position());
                double distance = toUser.length();
                if (distance > radius || distance < 1.0E-4) {
                    continue;
                }

                if (!blowing && distance < HOLD_DISTANCE) {
                    // Arrived. Left alone from here - see HOLD_DISTANCE.
                    continue;
                }

                Vec3 heading = toUser.scale((blowing ? -1.0 : 1.0) / distance);

                // Only pushed while it is still coming in slower than the draw allows, which is
                // what turns a compounding impulse into a steady speed - and, since the allowance
                // itself rises across the breath, into a speed that builds.
                if (entity.getDeltaMovement().dot(heading) >= drawSpeed) {
                    continue;
                }

                // Strongest close in. The floor keeps something at the very edge still moving, so a
                // long pull starts as a drift and builds rather than snapping on at some boundary.
                double falloff = Math.max(0.25, 1.0 - distance / radius);
                Vec3 pull = heading.scale(strength * falloff * ramp);

                // Added to existing motion rather than replacing it, so a mob fighting the pull is
                // slowed and turned instead of being frozen and dragged like a dead weight.
                Vec3 motion = entity.getDeltaMovement().add(pull.x, pull.y * 0.5 + 0.02, pull.z);
                entity.setDeltaMovement(motion);
                // Without this the client keeps its own prediction and the entity rubber-bands.
                entity.hurtMarked = true;
                entity.fallDistance = 0F;
            }

            return false;
        }
    }
}
