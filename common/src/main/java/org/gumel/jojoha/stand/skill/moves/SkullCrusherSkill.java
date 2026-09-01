package org.gumel.jojoha.stand.skill.moves;

import dev.architectury.event.events.common.TickEvent;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.network.packet.SkullFlashPacket;
import org.gumel.jojoha.registry.ModEffects;
import org.gumel.jojoha.registry.ModRegistries;
import org.gumel.jojoha.registry.ModSounds;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.StandSummonHandler;
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.StandSkill;

import java.util.ArrayList;
import java.util.List;

/**
 * A grab that becomes a skull-splitting punch.
 *
 * <p>Three beats, and the spacing between them is the whole character of the move: caught, crushed,
 * and only then thrown. A hit you can see coming and cannot escape reads as being caught; the same
 * damage delivered instantly reads as a heavier jab.
 *
 * <p>The throw waits for the crush to finish. It used to happen on the same tick, which meant the
 * skull came apart somewhere behind a body already sailing away - the one thing the move exists to
 * show, happening where nobody was looking. Now the victim stays pinned in front of you until there
 * is nothing left of it, and leaves afterwards.
 *
 * <p>The hold uses the same Stun the punch leaves behind, so the target is pinned by an ordinary
 * effect rather than by anything special-cased, and a grab whose owner dies simply expires.
 */
public final class SkullCrusherSkill implements StandSkill {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "skull_crusher");
    public static final SkullCrusherSkill INSTANCE = new SkullCrusherSkill();

    private static final int COOLDOWN_TICKS = 140;
    private static final float ENERGY_COST = EnergyWeight.HEAVY.cost();

    private static final double REACH = 4.5;

    /**
     * The three beats, in ticks.
     *
     * <p>Weighted, not evenly slow. The hold is the long part because anticipation is the only beat
     * that benefits from time - you are waiting for something. The crush is short because impact is
     * the opposite: a hit reads as hard in proportion to how little of it you see.
     *
     * <p>Both were tried longer. Twenty-four and eighteen gave a hold that worked and a crush that
     * played out like a slow demolition, with the punch somewhere in the middle of it doing nothing
     * in particular. Letting the impact frame carry the contact is what turns it back into a punch.
     *
     * <p>These numbers have been wrong in both directions. Twenty and eighteen was a slow
     * demolition; twelve and ten went past before you could see what had happened to whom. The
     * honest reading is that the beat has to be long enough to find the victim on screen and short
     * enough that you are not waiting on it, and that is closer to four fifths of a second each
     * than to either extreme.
     *
     * <p>The crush is the longer half now, which is a reversal. It used to be the shorter one on
     * the reasoning that it was over the moment the skull went, so anything past that was just the
     * launch being late. That was true when the body faded gradually and the skull was visible for
     * most of the move; it stopped being true when the reveal became a snap. The skull is only on
     * screen during the crush, and only for the part of it before the implosion finishes, so the
     * crush is the one beat where length buys anything at all.
     */
    private static final int GRAB_TICKS = 20;
    private static final int CRUSH_TICKS = 15;

    /**
     * How long the blood keeps coming after they are thrown, in ticks.
     *
     * <p>A third beat, added because the burst was a one-off at the moment of the crush and the
     * victim then flew out of it - so what you saw was a red cloud hanging in the air where someone
     * used to be, which reads as the blood having been a decoration on the punch rather than having
     * come out of them. Emitted from wherever they are each tick instead, so it trails.
     */
    private static final int TRAIL_TICKS = 12;

    /**
     * Where the victim is held, relative to whoever is holding them.
     *
     * <p>An arm length out in front and lifted until their feet leave the floor. The lift is what
     * makes it read as a grab rather than as two entities standing near each other - a mob with its
     * boots on the ground is standing there, and a mob with daylight under it is being held up by
     * the skull.
     *
     * <p>Capped, because a mob taller than the holder cannot be raised to the holder eye line
     * without being lifted through a ceiling, and the cap is cheaper than the collision check.
     */
    private static final double GRIP_DISTANCE = 1.55;
    private static final double GRIP_MAX_LIFT = 0.85;

    /**
     * Where the Stand plants itself for the grab, relative to its user.
     *
     * <p>Off to one side rather than straight ahead, which is the whole of this. Directly in front
     * put it squarely between the camera and the victim, and both of them are see-through during
     * the move - the Stand always is, and the victim is being faded on purpose so the skeleton
     * shows. Two translucent surfaces stacked on the same sight line do not add up to a clearer
     * picture, they add up to a muddle, and the thing that gets lost is the one the move is about.
     *
     * <p>Standing it at the shoulder and letting it reach across also happens to be the better
     * shot. The arm crosses the frame instead of pointing away down it, so you can see that
     * something is doing the holding rather than just that the victim is hanging there.
     */
    private static final double STAND_FORWARD = 0.55;
    private static final double STAND_SIDE = 0.9;
    private static final double STAND_LIFT = 0.4;

    private static final float DAMAGE = 12.0F;
    private static final double LAUNCH_AWAY = 1.35;
    private static final double LAUNCH_UP = 0.55;

    /** How long the victim stays stunned after being launched, in ticks. */
    private static final int STUN_TICKS = 60;

    /** Rings thrown out by the impact, each wider than the last. */
    private static final int IMPACT_RINGS = 5;

    /**
     * The row of rings the victim is driven back down.
     *
     * <p>Large band, evenly spaced, laid along the way they are about to travel. A single burst at
     * the point of impact says something happened there; a row says something is still happening to
     * them, and it is the row that reads as the force carrying on after the fist has stopped.
     *
     * <p>Laid at the moment of the launch rather than trailed behind the flight, because a ring
     * lives seven ticks and the whole corridor has to be on screen at once to be a corridor.
     */
    private static final int LAUNCH_RINGS = 6;
    private static final double LAUNCH_RING_GAP = 0.9;
    private static final double LAUNCH_RING_SCALE = 2.6;

    /** The large band of the ring sheet - see impact_ring.json. */
    private static final double RING_BAND_LARGE = 2.0;

    /**
     * When the head goes, as a share of the crush, and how long the going takes in ticks.
     *
     * <p>Public and living here rather than with the rendering because both sides need the same
     * answer. The client draws the skull imploding and the server throws the particles that are
     * meant to be the same event, and when those two were kept as separate numbers they were not
     * the same event at all - the burst fired on the first tick of the crush and the skull collapsed
     * eleven ticks later, so the head sprayed and then shrank quietly afterwards.
     *
     * <p>Placed so the pop happens well inside the see-through window rather than on the edge of
     * it. At 0.86 the skull finished collapsing on the same frame the body snapped opaque, so the
     * one thing the translucency exists to show was over by the time you could see it had happened -
     * the head was solid again before the pop registered.
     *
     * <p>It now lands at three fifths of a crush that is itself shorter. The order that falls out is
     * the one the move wants: rattle, pop, a beat of an empty head spraying, then solid, then
     * thrown. Every step gets its own frames and none of them collide.
     */
    public static final float POP_AT = 0.6F;
    public static final float POP_TICKS = 3F;

    /** How far up the victim their head is, as a share of their height. */
    private static final double HEAD_SHARE = 0.85;

    /** And where the head used to join on, which is where the trail comes from. */
    private static final double NECK_SHARE = 0.72;

    /**
     * The burst that replaces the head, in particles.
     *
     * <p>Weighted toward the strands. Blood alone is a red cloud and reads as a hit landing on
     * something soft; the pale tumbling shreds are what make it read as a skull, and the blood is
     * the colour under them rather than the substance of it.
     */
    private static final int BURST_WEB = 40;
    private static final int BURST_BLOOD = 26;

    /**
     * How hard the spray leaves the neck, out and up.
     *
     * <p>Up is the bigger of the two on purpose. Blood under pressure goes up first and outward
     * second, and a spray thrown flat reads as a splash on the floor rather than as something
     * having burst.
     */
    private static final double SPURT_OUT = 0.24;
    private static final double SPURT_UP = 0.38;

    /**
     * Cobweb, as a break particle.
     *
     * <p>The block particle takes its sprite from whatever state it is handed, so this is simply the
     * cobweb texture torn into short strands that tumble and fall. Built once - the option is
     * immutable and a new one per particle per use would be a lot of garbage for a constant.
     */
    private static final net.minecraft.core.particles.BlockParticleOption WEB_SHRED =
            new net.minecraft.core.particles.BlockParticleOption(
                    net.minecraft.core.particles.ParticleTypes.BLOCK,
                    net.minecraft.world.level.block.Blocks.COBWEB.defaultBlockState());

    /**
     * The two things a breaking skull throws off.
     *
     * <p>Dust for the powder and item shards for the pieces. Both are built once: a dust option
     * carries a colour and a size and allocating one per particle per tick would be a lot of
     * garbage for something that never changes.
     */
    private static final net.minecraft.core.particles.DustParticleOptions BONE_DUST =
            new net.minecraft.core.particles.DustParticleOptions(
                    new org.joml.Vector3f(0.93F, 0.91F, 0.84F), 1.1F);

    private static final net.minecraft.core.particles.ItemParticleOption BONE_SHARD =
            new net.minecraft.core.particles.ItemParticleOption(
                    ParticleTypes.ITEM, new net.minecraft.world.item.ItemStack(
                            net.minecraft.world.item.Items.BONE));

    private static final List<Grab> HELD = new ArrayList<>();

    private SkullCrusherSkill() {
    }

    public static void init() {
        TickEvent.SERVER_POST.register(server -> tick());
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "skill.jojoha.skull_crusher";
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
        LivingEntity target = SkillTargeting.lookTarget(player, REACH);
        if (target == null) {
            return false;
        }

        // Nobody is grabbed twice at once - a second press on the same victim would land two punches
        // on one grab and read as the move stuttering.
        HELD.removeIf(grab -> grab.target == target);
        HELD.add(new Grab(player, target));

        hold(target, GRAB_TICKS + 2);

        ServerLevel level = player.serverLevel();

        // The skull lights up inside a head that is still there, while the fist draws back.
        NetworkHandler.sendSkullFlash(level, target, player, SkullFlashPacket.WINDUP, GRAB_TICKS);

        Vec3 head = target.position().add(0, target.getBbHeight() * 0.85, 0);
        level.sendParticles(ParticleTypes.CRIT, head.x, head.y, head.z, 8, 0.2, 0.2, 0.2, 0.02);
        // Light and quick - the hand closing, not the blow. The whole move speaks with the Stand's
        // voice now; the vanilla player-attack sounds that used to sit on the seize and the throw
        // were the wrong character entirely, since it is not the player swinging.
        level.playSound(null, target.blockPosition(), ModSounds.STAND_HIT.get(),
                SoundSource.PLAYERS, 0.6F, 1.35F);

        if (stand != null) {
            // Grab, not punch. The punch is the second beat and it has its own trigger; playing the
            // strike here left the Stand swinging at something it had not taken hold of yet.
            stand.triggerGrabAt(target);
        }
        return true;
    }

    /**
     * Whether this entity is in somebody's hand right now.
     *
     * <p>Asked by the stun effect, which spawns its ring of marks for every stunned thing in the
     * world and has no other way to tell the hold apart from the aftermath - {@code applyEffectTick}
     * is handed an amplifier and nothing else, and a single {@code MobEffect} instance is shared by
     * every entity carrying it, so it cannot keep the distinction itself.
     *
     * <p>The obvious alternative was to give the two applications different amplifiers and read
     * that. It was not worth it: the amplifier scales the attribute modifiers, and this effect
     * carries a -1.0 multiplier on movement speed, so raising it would have taken the total to -2.0
     * and left speed to be clamped out of a negative. A question about presentation should not be
     * routed through a number that means something else.
     */
    public static boolean isGripped(LivingEntity entity) {
        for (int i = 0; i < HELD.size(); i++) {
            Grab grab = HELD.get(i);

            // Held means held. A grab that has reached its trailing beat has already thrown them,
            // and it only stays in the list so the blood can keep coming - so it must not go on
            // suppressing the marks, which are supposed to start the moment they are airborne.
            if (grab.target == entity && !grab.trailing) {
                return true;
            }
        }
        return false;
    }

    /** Pins something in place using the same effect the punch leaves behind. */
    private static void hold(LivingEntity target, int ticks) {
        target.addEffect(new MobEffectInstance(ModEffects.stun(), ticks, 0, false, false, true));
        target.setDeltaMovement(Vec3.ZERO);
        target.hurtMarked = true;
    }

    private static void tick() {
        HELD.removeIf(Grab::advance);
    }

    /** One grab in progress: who has whom, which beat it is on, and how long that beat has left. */
    private static final class Grab {
        private final ServerPlayer holder;
        private final LivingEntity target;
        private final JojohaPlayerData data;

        /**
         * Resolved once, not every tick.
         *
         * <p>The lookup itself is cheap - a UUID against the level map - but it was being done
         * three times a tick alongside a player-data fetch, for a thing that cannot change during a
         * grab that is already cancelled if the holder dies. Once, at the start, and every use
         * checks it is still alive.
         */
        private final StandEntity stand;

        private boolean crushing;
        private boolean trailing;
        private boolean burst;
        private int remaining;

        private Grab(ServerPlayer holder, LivingEntity target) {
            this.holder = holder;
            this.target = target;
            this.data = org.gumel.jojoha.data.PlayerDataAccess.get(holder);
            this.stand = StandSummonHandler.findStand(holder, this.data);
            this.remaining = GRAB_TICKS;
        }

        /** The Stand, if it is still there to be given orders. */
        private StandEntity stand() {
            return stand != null && stand.isAlive() ? stand : null;
        }

        /** True once this grab is finished with, one way or another. */
        private boolean advance() {
            // The trail runs after they have been thrown, so it is deliberately outside everything
            // below: no pinning, no gripping, and no distance check - the whole point of the beat is
            // that they are leaving.
            if (trailing) {
                if (!target.isAlive() || holder.level() != target.level()) {
                    return true;
                }
                bleeding(holder.serverLevel());
                return --remaining <= 0;
            }

            if (!holder.isAlive() || !target.isAlive()
                    || holder.level() != target.level()
                    || holder.distanceTo(target) > REACH * 2.5) {
                // Every way out of this move goes through here or through launch, and both have to
                // put the Stand back on its spring - one left anchored to the spot where a grab was
                // interrupted stays standing there for good.
                StandEntity held = stand();
                if (held != null) {
                    held.clearWork();
                }
                return true;
            }

            // Pinned through both beats. Re-applied rather than trusted to outlast the grab, so that
            // anything clearing the effect cannot quietly free the target early - and so the victim
            // is still in front of you while their skull comes apart.
            hold(target, remaining + 2);
            grip();

            // Every tick, not once at the start. A single burst is an event; a trickle that keeps
            // coming is a process, and this is meant to look like something being slowly given way
            // to and then broken.
            if (crushing) {
                cracking(holder.serverLevel());

                // The head itself, thrown on the tick the skull is coming apart on screen rather
                // than on the tick the fist landed - see POP_AT. One tick past the start of the
                // collapse, which is the frame after the swell, so the particles arrive as the
                // shape gives rather than as it is still gathering itself.
                float elapsed = CRUSH_TICKS - remaining;
                if (!burst && elapsed >= POP_AT * CRUSH_TICKS - POP_TICKS + 1F) {
                    burst = true;
                    burst(holder.serverLevel());
                }
            } else {
                straining(holder.serverLevel());
            }

            if (--remaining > 0) {
                return false;
            }

            if (!crushing) {
                crush();
                crushing = true;
                remaining = CRUSH_TICKS;
                return false;
            }

            launch();
            trailing = true;
            remaining = TRAIL_TICKS;
            return false;
        }

        /**
         * Blood coming off them as they go, from wherever they now are.
         *
         * <p>Thinning as it runs. A trail at a constant rate reads as a leak; one that starts heavy
         * and gives out reads as the spray from something that has just happened and is stopping.
         *
         * <p>Taken from the neck rather than the head height used everywhere else in this move -
         * there is no head left by this point, and blood appearing where one used to be looks like
         * it is coming off a ghost.
         */
        private void bleeding(ServerLevel level) {
            float left = remaining / (float) TRAIL_TICKS;
            int drops = 1 + Math.round(left * 3);

            Vec3 at = target.position().add(0, target.getBbHeight() * NECK_SHARE, 0);
            level.sendParticles(ModRegistries.BLOOD_MOTE.get(), at.x, at.y, at.z, drops,
                    0.1, 0.1, 0.1, 0.06);

            if (remaining % 3 == 0) {
                level.sendParticles(WEB_SHRED, at.x, at.y, at.z, 1, 0.08, 0.08, 0.08, 0.05);
            }
        }

        /**
         * Holds the pair of them in the shape of a grab, every tick, on the server.
         *
         * <p>Two halves. The Stand is parked on a fixed point beside its user for the whole move,
         * which is what locked means here - it is normally on a follow spring that trails the
         * player and drifts with them, and a Stand drifting while its fist is buried in somebody
         * head reads as the grab having no grip at all. The work anchor already exists for exactly
         * this, and stops dead on arrival instead of settling, so it is the one used.
         *
         * <p>The other half is the victim, who is moved rather than pushed. Knockback and pathing
         * both keep working on a mob that is only being asked nicely, and the result is a victim
         * who squirms out of frame halfway through. Writing the position outright and flagging it
         * means the client is told where they are rather than left to guess.
         *
         * <p>Re-taken every tick from where the holder is now, so walking or turning during the
         * grab carries the victim round with you. That is the whole difference between an attack
         * and a cutscene: you never stop being in control of where this is happening.
         */
        private void grip() {
            Vec3 look = holder.getLookAngle();
            Vec3 forward = new Vec3(look.x, 0, look.z);
            forward = forward.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : forward.normalize();

            Vec3 grip = holder.position().add(forward.scale(GRIP_DISTANCE));

            // Raised until their head is at the holder eye line, or as far as the cap allows,
            // whichever is the lesser - see GRIP_MAX_LIFT.
            double wanted = holder.getEyeY() - target.getBbHeight() * HEAD_SHARE;
            double lift = Math.min(Math.max(wanted, holder.getY()), holder.getY() + GRIP_MAX_LIFT);

            target.setPos(grip.x, lift, grip.z);
            target.setDeltaMovement(Vec3.ZERO);
            target.fallDistance = 0F;
            target.hurtMarked = true;

            // Turned to face whoever has hold of them. A victim held by the head and looking off
            // over your shoulder is a victim who has not noticed.
            float facing = holder.getYRot() + 180F;
            target.setYRot(facing);
            target.setYBodyRot(facing);
            target.setYHeadRot(facing);

            StandEntity held = stand();
            if (held != null) {
                // The user's right. Yaw zero looks down positive Z, and the right hand from there
                // is negative X, so turning the heading a quarter turn that way is (-z, 0, x).
                Vec3 side = new Vec3(-forward.z, 0, forward.x);

                // At the shoulder and off the sight line - see STAND_SIDE. Re-sent every tick
                // because the anchor is a fixed world point and the holder is free to walk about
                // while holding it; the Stand turns to face the victim on its own.
                held.sendToWork(holder.position()
                        .add(forward.scale(STAND_FORWARD))
                        .add(side.scale(STAND_SIDE))
                        .add(0, STAND_LIFT, 0));
            }
        }

        /** The punch: it lands, the skull goes, and the victim stays exactly where they are. */
        private void crush() {
            ServerLevel level = holder.serverLevel();

            float damage = org.gumel.jojoha.stand.passive.StandPassives.scaleOutgoing(
                    holder, data, target, DAMAGE);
            target.hurt(level.damageSources().playerAttack(holder), damage);

            // Held, not thrown. Being hurt imparts motion of its own, and letting that stand would
            // drift the victim out of the shot the break is happening in.
            target.setDeltaMovement(Vec3.ZERO);
            target.hurtMarked = true;

            impact(level);

            // The skull is a picture, not a thing in the world, so it is told to the clients that
            // can see it rather than spawned. See SkullFlashPacket.
            NetworkHandler.sendSkullFlash(level, target, holder,
                    SkullFlashPacket.SHATTER, CRUSH_TICKS);

            StandEntity held = stand();
            if (held != null) {
                held.triggerPunchAt(target, true);
            }
        }

        /** And afterwards, once there is nothing left to look at, they go. */
        private void launch() {
            ServerLevel level = holder.serverLevel();

            Vec3 away = target.position().subtract(holder.position());
            Vec3 heading = away.lengthSqr() < 1.0E-4 ? holder.getLookAngle() : away.normalize();
            target.setDeltaMovement(heading.x * LAUNCH_AWAY, LAUNCH_UP, heading.z * LAUNCH_AWAY);
            target.hurtMarked = true;

            // Invisible, like the hold before it. The fifth flag is vanilla's own swirl particles,
            // and the effect colour is a near-white - so a stunned mob was being covered in pale
            // motes from head to foot, which is the whitewash. The ring of marks over the head is
            // this effect's particle now, and it is spawned by the effect itself. See ModEffects.
            target.addEffect(new MobEffectInstance(ModEffects.stun(), STUN_TICKS, 0,
                    false, false, true));

            // Handed back to the follow spring. Left on the anchor, it would stand in the road where
            // the grab happened until something else moved it.
            StandEntity held = stand();
            if (held != null) {
                held.clearWork();
            }

            launchRow(level, heading);

            Vec3 at = target.position().add(0, target.getBbHeight() * 0.5, 0);
            level.sendParticles(ParticleTypes.CLOUD, at.x, at.y, at.z, 14, 0.25, 0.25, 0.25, 0.22);
            // And heavy and low for the throw, the same voice at the other end of its range.
            level.playSound(null, target.blockPosition(), ModSounds.STAND_HIT.get(),
                    SoundSource.PLAYERS, 1.0F, 0.62F);
        }

        /**
         * The corridor of rings the victim is sent down.
         *
         * <p>Started a little ahead of them rather than on top of them, so the first ring is
         * something they are being driven into rather than one more thing at the point of impact -
         * which already has five of its own.
         *
         * <p>Held at chest height along the whole row instead of following the arc they will
         * actually take. The arc is a ballistic curve and the row is a straight line, and lining the
         * two up exactly turns out to read as worse: the eye takes the row as the direction of the
         * blow, and a blow that curves downward looks like it lost its nerve.
         */
        private void launchRow(ServerLevel level, Vec3 heading) {
            Vec3 from = target.position().add(0, target.getBbHeight() * 0.55, 0);

            for (int step = 0; step < LAUNCH_RINGS; step++) {
                Vec3 at = from.add(heading.scale((step + 1) * LAUNCH_RING_GAP));

                // Tapering off down the row, so it reads as the force spending itself rather than
                // as a fence of identical circles.
                double scale = LAUNCH_RING_SCALE * (1.0 - step * 0.09);

                level.sendParticles(ModRegistries.IMPACT_RING.get(), at.x, at.y, at.z,
                        0, scale, RING_BAND_LARGE, 0.0, 1.0);
            }
        }

        /** Where the head is, which everything here is thrown from. */
        private Vec3 head() {
            return target.position().add(0, target.getBbHeight() * HEAD_SHARE, 0);
        }

        /**
         * The hold: something building up that has not given yet.
         *
         * <p>Almost nothing, now. This used to throw a ring of end rod specks drifting inward at the
         * head every third tick, on the theory that particles converging read as charging where
         * particles leaving read as a hit that had already landed. The theory holds; the particle
         * was simply wrong for it. End rod is a pale glowing mote with a long soft trail, and a
         * handful of them around somebody's head reads as enchanting, not as pressure.
         *
         * <p>Nothing has been put in its place. The seize already has plenty carrying it - the mob
         * is off the floor, held at arm's length, turned to face you, with a Stand planted at your
         * shoulder - and the payoff is a body snapping transparent a moment later. A wind-up whose
         * job is to make you wait does not need decorating.
         */
        private void straining(ServerLevel level) {
            // A tighter, quicker tick as it gets close to going, and only that.
            if (remaining % 3 == 0 && remaining < GRAB_TICKS / 3) {
                Vec3 at = head();
                level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 2, 0.18, 0.18, 0.18, 0.01);
            }
        }

        /**
         * The crush: bone giving way, a bit more of it each tick.
         *
         * <p>Three things at once, because one particle type on its own always reads as a particle
         * type. White dust is the powder, bone shards are the pieces, and the sparks are what sells
         * it as a hard impact rather than a crumble.
         */
        private void cracking(ServerLevel level) {
            Vec3 at = head();

            // Heaviest at the moment of breaking and easing off, so the burst has a shape rather
            // than being a constant emitter for most of a second.
            float weight = remaining / (float) CRUSH_TICKS;

            level.sendParticles(BONE_DUST, at.x, at.y, at.z, 2 + Math.round(weight * 3),
                    0.22, 0.2, 0.22, 0.02);

            // Shards on alternate ticks only. An item particle carries a whole item model and is
            // the dearest thing being thrown here by a wide margin; dust is near enough free, so the
            // trickle is made of dust and the shards are the accent on top of it.
            if (remaining % 2 == 0) {
                level.sendParticles(BONE_SHARD, at.x, at.y, at.z, 1 + Math.round(weight * 2),
                        0.16, 0.16, 0.16, 0.14);
                level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 2, 0.2, 0.2, 0.2, 0.12);

                // Blood from here on as well, not only in the one burst. head() is read fresh every
                // tick, so it keeps up with the victim rather than staying where they started.
                level.sendParticles(ModRegistries.BLOOD_MOTE.get(), at.x, at.y, at.z, 2,
                        0.12, 0.1, 0.12, 0.08);
            }
        }

        /**
         * The head going, thrown from the middle of it.
         *
         * <p>Two things, chosen for what they look like rather than for what they are. Cobweb break
         * particles are short pale strands that tumble and fall, which is as close as the game has
         * to torn tissue and splintered bone without a custom sheet; and the blood mote is the one
         * already in the mod for exactly this. Between them the head reads as having burst rather
         * than as having faded out.
         *
         * <p>Thrown from a single point with real speed on them rather than scattered across a box.
         * A box of particles is a cloud that happens to be near the head; a point with speed is an
         * explosion that came out of it, and the difference is entirely in where they start.
         *
         * <p>Cobweb goes through the block particle, which needs a block state - it takes its
         * texture straight off whatever it is handed, so the sprite is the cobweb sprite.
         */
        private void burst(ServerLevel level) {
            Vec3 at = head();

            level.sendParticles(WEB_SHRED, at.x, at.y, at.z, BURST_WEB, 0.0, 0.0, 0.0, 0.55);

            // Aimed, one at a time, rather than a count with a speed on it.
            //
            // A count tells the client to scatter that many with gaussian velocities, which is
            // symmetrical in every direction - fine for a puff, wrong for a spray, because a spray
            // has somewhere it is going. Sending them singly with a count of nought means the three
            // speed arguments are the velocity itself, so each drop can be pointed.
            //
            // Up and out, around a circle, with the upward share the larger of the two. Gravity
            // does the rest: they leave fast, arc over and come down, which is the shape of blood
            // leaving something under pressure.
            for (int i = 0; i < BURST_BLOOD; i++) {
                double angle = i * (Math.PI * 2 / BURST_BLOOD) + level.random.nextDouble() * 0.5;
                double out = SPURT_OUT * (0.55 + level.random.nextDouble() * 0.9);
                double up = SPURT_UP * (0.6 + level.random.nextDouble() * 0.8);

                level.sendParticles(ModRegistries.BLOOD_MOTE.get(), at.x, at.y, at.z, 0,
                        Math.cos(angle) * out, up, Math.sin(angle) * out, 1.0);
            }

            // And a slower handful left hanging where the head was, once the spray has gone past.
            level.sendParticles(ModRegistries.BLOOD_MOTE.get(), at.x, at.y, at.z,
                    BURST_BLOOD / 3, 0.14, 0.12, 0.14, 0.02);
        }

        private void impact(ServerLevel level) {
            Vec3 at = target.position().add(0, target.getBbHeight() * 0.6, 0);

            // Expanding rings rather than a puff. The ring particle takes its size in the x speed
            // slot - StandEntity spawns them the same way.
            for (int ring = 0; ring < IMPACT_RINGS; ring++) {
                level.sendParticles(ModRegistries.IMPACT_RING.get(), at.x, at.y, at.z,
                        0, 1.2 + ring * 0.9, -1.0, 0.0, 1.0);
            }

            level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 20, 0.4, 0.4, 0.4, 0.5);

            // Actual debris alongside the model pieces - bone, because that is what just broke.
            //
            // The counts came down by roughly two thirds on the shards and not at all on the dust,
            // which is the whole optimisation: twenty-six item particles is twenty-six item models
            // being baked and drawn on a single frame, and it was the one thing in this move heavy
            // enough to be felt. Ten reads the same at the size they are drawn.
            level.sendParticles(BONE_SHARD, at.x, at.y, at.z, 10, 0.3, 0.3, 0.3, 0.35);
            level.sendParticles(BONE_DUST, at.x, at.y, at.z, 34, 0.35, 0.3, 0.35, 0.04);
            level.sendParticles(ParticleTypes.CLOUD, at.x, at.y, at.z, 12, 0.3, 0.2, 0.3, 0.12);

            // The Stand's own hit, rather than a vanilla crit. It is a Stand throwing this punch and
            // it should sound like the rest of them.
            level.playSound(null, target.blockPosition(), ModSounds.STAND_HIT.get(),
                    SoundSource.PLAYERS, 1.2F, 0.8F);
            level.playSound(null, target.blockPosition(), SoundEvents.SKELETON_DEATH,
                    SoundSource.PLAYERS, 0.7F, 1.6F);
        }
    }
}
