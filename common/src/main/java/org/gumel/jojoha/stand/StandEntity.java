package org.gumel.jojoha.stand;

import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.StandTuning;
import org.gumel.jojoha.data.PlayerDataAccess;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.gumel.jojoha.registry.ModRegistries;
import org.gumel.jojoha.registry.ModSounds;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * A summoned Stand. Has no AI of its own - every action (punch, barrage, block) is a direct
 * result of the summoning player's input, mirroring canon: Stands act on their user's will,
 * not independently. Follows its owner with a spring-damper simulation each tick rather than
 * pathfinding, since it floats and has no obstacles to navigate around.
 */
public final class StandEntity extends LivingEntity implements GeoEntity {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final String CONTROLLER = "controller";
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    // Resting pose for a PARTIAL manifestation - just the two arms, posed to read as cast around
    // the user rather than belonging to a body that isn't there.
    private static final RawAnimation IDLE_PARTIAL = RawAnimation.begin().thenLoop("idle_partial");
    private static final RawAnimation SPAWN = RawAnimation.begin().thenPlay("spawn");
    private static final RawAnimation PUNCH = RawAnimation.begin().thenPlay("punch");
    private static final RawAnimation PUNCH2 = RawAnimation.begin().thenPlay("punch2");
    private static final RawAnimation BARRAGE = RawAnimation.begin().thenLoop("barrage");
    /** Star Finger's own thrust, which drives the finger bone the plain punch has no use for. */
    private static final RawAnimation STAR_FINGER = RawAnimation.begin().thenPlay("star_finger");
    private static final RawAnimation INHALE = RawAnimation.begin().thenPlay("inhale");
    /**
     * Played once, then the Stand returns to idle.
     *
     * <p>thenPlay overrides the loop flag the file itself carries - the animation is authored as
     * looping, which made the Stand replay the cast pose for the entire duration of the stop
     * instead of striking it once and holding its ground.
     */
    private static final RawAnimation TIMESTOP = RawAnimation.begin().thenPlay("timestop");
    /** Authored and available, but nothing calls for it yet - see triggerGrab. */
    private static final RawAnimation GRAB = RawAnimation.begin().thenPlay("grab");
    private static final RawAnimation BLOCK = RawAnimation.begin().thenLoop("block");
    private static final RawAnimation GUARDBROKEN = RawAnimation.begin().thenPlay("guardbroken");

    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER =
            SynchedEntityData.defineId(StandEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<String> DATA_STAND_TYPE =
            SynchedEntityData.defineId(StandEntity.class, EntityDataSerializers.STRING);
    // Synced because the client renderer needs it: the tier decides partial-vs-full body
    // (StandModel hides all non-arm bones for PARTIAL) and whether the translucent EMERGING
    // flicker applies (StandRenderer/StandModel read getRenderAlpha()).
    private static final EntityDataAccessor<Integer> DATA_TRUST_TIER =
            SynchedEntityData.defineId(StandEntity.class, EntityDataSerializers.INT);
    // The tick dismissal began, or -1 while the Stand is still held. Synced as a tick rather than
    // a plain boolean so both sides derive the same fade curve from their own tickCount, instead
    // of the client having to guess how long ago the flag flipped.
    private static final EntityDataAccessor<Integer> DATA_DISMISS_START_TICK =
            SynchedEntityData.defineId(StandEntity.class, EntityDataSerializers.INT);
    /** Synced so a watching client positions the Stand in the same stance its owner chose. */
    private static final EntityDataAccessor<Integer> DATA_MODE =
            SynchedEntityData.defineId(StandEntity.class, EntityDataSerializers.INT);

    // Stance inputs, synced purely so the client can run the follow simulation itself rather than
    // waiting on position packets. See tick() for why that matters.
    private static final EntityDataAccessor<Boolean> DATA_FRONT_STANCE =
            SynchedEntityData.defineId(StandEntity.class, EntityDataSerializers.BOOLEAN);

    /**
     * Squared up, but not mid-swing. Synced for the same reason the front stance is.
     *
     * <p>Without this the Stand only leaves its idle spot for the twelve ticks a punch lasts, so a
     * user throwing a blow every second or two sends it on a five-and-a-half block round trip per
     * swing - out to the attack position, all the way back behind them, and out again. That reads as
     * darting rather than as fighting, and it is the movement doc's point in saying combat is a
     * position the Stand holds rather than somewhere it visits per attack.
     */
    private static final EntityDataAccessor<Boolean> DATA_COMBAT_STANCE =
            SynchedEntityData.defineId(StandEntity.class, EntityDataSerializers.BOOLEAN);

    /**
     * Whether a flurry is being held open, as opposed to running itself out.
     *
     * <p>Synced, and it has to be. A held move is ended by the client noticing the key came up while
     * the move is running, so the client is the side that has to know it <em>is</em> running - and as
     * a server-only field this read false there, the release was never sent, and the barrage could be
     * started and then never stopped.
     */
    private static final EntityDataAccessor<Boolean> DATA_HELD_BARRAGE =
            SynchedEntityData.defineId(StandEntity.class, EntityDataSerializers.BOOLEAN);
    /**
     * The entity the guard is turned toward, by id, or -1 for nobody.
     *
     * <p>Synced for the same reason the guard flag is: the client runs its own copy of the follow
     * simulation, and the anchor it works out has to be the one the server worked out. Left as a
     * server-only field the client would place the guard dead ahead while the server placed it
     * across the owner's front, and the Stand would be dragged between the two every tick.
     */
    /**
     * How close the guard is to giving way, 0 to 1. Synced - the crack overlay is drawn from it.
     *
     * <p>A fraction rather than the hit count, so the client never has to know what the break
     * threshold is. Sending the raw count would mean the renderer and the combat handler had to
     * agree on a constant that belongs to neither of them.
     */
    /**
     * Whether the Stand is planted taking a breath. Synced, and it has to be.
     *
     * <p>The client predicts the follow locally, and a Stand that is deliberately refusing to
     * follow is precisely the case that prediction gets wrong - it would spend the whole channel
     * dragging the Stand back to the owner's shoulder while the server held it in place.
     */
    private static final EntityDataAccessor<Boolean> DATA_INHALING =
            SynchedEntityData.defineId(StandEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Float> DATA_GUARD_STRAIN =
            SynchedEntityData.defineId(StandEntity.class, EntityDataSerializers.FLOAT);

    /** Ticks left showing the shattered overlay. Synced, since it is purely something to look at. */
    private static final EntityDataAccessor<Integer> DATA_GUARD_BROKEN =
            SynchedEntityData.defineId(StandEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Integer> DATA_GUARD_THREAT =
            SynchedEntityData.defineId(StandEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Boolean> DATA_GUARDING =
            SynchedEntityData.defineId(StandEntity.class, EntityDataSerializers.BOOLEAN);
    /** True while the Stand is off fighting - the client stops predicting and defers to the server. */
    private static final EntityDataAccessor<Boolean> DATA_ENGAGED =
            SynchedEntityData.defineId(StandEntity.class, EntityDataSerializers.BOOLEAN);
    /**
     * True while a flurry is landing. Synced because the smear arms are drawn client-side, and the
     * hit counter that actually defines a barrage only exists on the server.
     */
    private static final EntityDataAccessor<Boolean> DATA_BARRAGING =
            SynchedEntityData.defineId(StandEntity.class, EntityDataSerializers.BOOLEAN);
    /**
     * Whether the user is flying this Stand by hand.
     *
     * <p>Synced, and it has to be. This was a server-only field, and the client kept running its
     * follow prediction straight through a flight because it had no way to know one was happening -
     * spring-yanking the Stand back onto the player every tick, and throwing away the server's real
     * positions in {@link #lerpTo} on top. The server was steering correctly the whole time and
     * none of it survived the trip. With a camera riding the Stand, that reads as the Stand being
     * unable to leave the player, the view sitting inside the player's own body, and the facing
     * lagging the mouse - three symptoms of one missing boolean.
     */
    private static final EntityDataAccessor<Boolean> DATA_PILOTED =
            SynchedEntityData.defineId(StandEntity.class, EntityDataSerializers.BOOLEAN);

    /**
     * Whatever the Stand is holding this instant, which is only ever something it is in the middle
     * of using - see {@link #showHeldItem}.
     *
     * <p>Synced rather than derived from the owner's hand, because the two are not the same thing
     * and cannot be. The item the Stand is using may be consumed by the use itself - a bucket
     * empties, a pearl leaves the stack entirely - so a client reading the owner's hand would find
     * the wrong item, or none, at exactly the moment it needs to draw one. What is sent is a
     * snapshot of what was used, and it survives the stack it came from.
     */
    private static final EntityDataAccessor<ItemStack> DATA_HELD_ITEM =
            SynchedEntityData.defineId(StandEntity.class, EntityDataSerializers.ITEM_STACK);
    /** Which of its type's skins this Stand is wearing - synced, since only the client draws it. */
    private static final EntityDataAccessor<Integer> DATA_SKIN =
            SynchedEntityData.defineId(StandEntity.class, EntityDataSerializers.INT);
    /**
     * The tick a skin swap began on, or -1.
     *
     * <p>A tick rather than a progress value, for the same reason the dismissal fade is one: both
     * sides count their own {@code tickCount}, so sending the moment it started lets each derive
     * the same curve at its own framerate instead of the client being sent a number twenty times a
     * second and interpolating between the ones it happened to receive.
     */
    private static final EntityDataAccessor<Integer> DATA_SKIN_SWAP_TICK =
            SynchedEntityData.defineId(StandEntity.class, EntityDataSerializers.INT);

    // How long the model takes to fade in from transparent once it becomes visible (see
    // getRenderAlpha(), read by StandRenderer/StandModel) - deliberately slower than the
    // spawn animation's own scale growth (which finishes at t=0.5s/10 ticks) for a more gradual
    // materialization rather than a fade that's already done before the pose settles.
    private static final int SPAWN_FADE_TICKS = 30;
    // Withdrawal fade. Deliberately quicker than the spawn fade - manifesting is an effort, but
    // letting go should read as releasing something rather than another slow ceremony.
    private static final int DISMISS_FADE_TICKS = 12;

    // EMERGING flicker: never fully opaque (floor keeps it readable rather than strobing to
    // invisible), oscillating slowly enough to read as unstable presence rather than a glitch.
    private static final float EMERGING_PULSE_FLOOR = 0.45F;
    private static final double EMERGING_PULSE_SPEED = 0.22;

    /** How faint a DORMANT Stand looks in the HUD portrait - present, but not answering. */
    private static final float DORMANT_PREVIEW_ALPHA = 0.35F;

    // Hovers behind and to the right of its owner, relative to where their head is looking -
    // except during a punch or while guarding, when it moves in front instead (see tick()).
    // Guarding also drops it down to the owner's own body height, rather than floating above.
    // How far behind, how far to the side and how high are per-Stand - see StandMovementProfile.

    /**
     * Where a Utility Stand waits: beside its user, level with them, not behind.
     *
     * <p>Behind is the right place for a Stand that is watching your back. It is the wrong place
     * for one you are about to send somewhere, because you cannot see it leave and you cannot see
     * it come back - the whole job happens off the edge of the screen. Standing it at your shoulder
     * makes the trip legible: it is there, it goes, it returns.
     */
    private static final double UTILITY_SIDE_OFFSET = 1.15;

    /**
     * How fast the Stand travels to a job, and how close counts as arrived.
     *
     * <p>Quick. The Stand crossing twenty blocks is not the interesting part of placing a block,
     * and a player laying a row would spend the whole time waiting for it. Arrival is generous for
     * the same reason - it has to be near the block, not on it.
     */
    private static final double UTILITY_STEP_SPEED = 1.1;
    private static final double UTILITY_ARRIVAL = 1.2;

    /**
     * How far out the Stand plants itself while blocking.
     *
     * <p>Much closer than the punch stance. A guard standing nearly two blocks out leaves a gap wide
     * enough to walk an arrow through, and worse, it looks like one - the block reads as covering
     * something the Stand is plainly not in front of. Pressed against the owner there is nothing
     * between them to disagree about.
     */
    private static final double GUARD_OFFSET = 0.85;

    /**
     * How far round the owner's front the guard may slide, as a dot product against their facing.
     *
     * <p>The Stand tracks the threat, but only across the arc it can still be said to be in front of
     * its user - past that it would be guarding their flank while they face the other way.
     */
    private static final double GUARD_ARC = 0.2;

    /** How long a threat stays worth guarding against after it last did anything, in ticks. */
    private static final int GUARD_THREAT_MEMORY = 60;

    /**
     * How far from its owner the guard holds ground, in blocks.
     *
     * <p>Deliberately <em>inside</em> melee reach, and that is the whole subtlety of this number.
     * It was 2.6, which is outside it - and since the guard only breaks after absorbing five blows,
     * a field that stopped every blow landing quietly made the stance unbreakable. The push has to
     * stop the crowding without stopping the fight.
     *
     * <p>Where reach actually is, from {@code Mob}: {@code DEFAULT_ATTACK_REACH} is
     * {@code sqrt(2.04) - 0.6}, about 0.83, and it is applied by inflating the attacker's bounding
     * box by that much and testing it against the target's - an intersection, not a distance. Two
     * 0.6-wide bodies therefore connect out to roughly 1.43 blocks centre to centre. The field
     * settles an approaching mob near a block out, well inside that, so it is left standing clear
     * of the player and still able to swing.
     *
     * <p>So this is set to just past bodies touching. An attacker is kept out of the player's own
     * space and left standing at the far edge of its reach, where it can still swing. Being shoved
     * back out of your face when it connects is {@link #GUARD_DEFLECT_KNOCKBACK}'s job, which is
     * safe precisely because it only ever fires on a hit that already landed.
     */
    private static final double GUARD_PUSH_RANGE = 1.35;

    /**
     * How hard the guard leans, in blocks per tick right at the owner.
     *
     * <p>Scaled down to nothing at the edge of the range, so this is a cushion that firms up as
     * something presses into it rather than a wall that flicks things away at a fixed distance.
     * Gentler than it was, for the same reason the range shrank: it is there to deny the last
     * half-block, not to win the approach.
     */
    private static final double GUARD_PUSH_STRENGTH = 0.16;

    /**
     * How hard a blow that the guard eats throws the thing that threw it, in blocks.
     *
     * <p>This is the part that actually keeps attackers out of your face, and it is the right shape
     * for the job: it fires only when a hit has already been absorbed, so it can push as hard as it
     * likes without ever making the guard unbreakable. A field strong enough to do the same thing
     * has to hold attackers outside their own reach, which is exactly the trap the range above got
     * caught in.
     */
    private static final double GUARD_DEFLECT_KNOCKBACK = 0.62;
    private static final double GUARD_DEFLECT_LIFT = 0.22;

    /** A little lift, so the shove slides things back instead of grinding them into the floor. */
    private static final double GUARD_PUSH_LIFT = 0.04;

    /**
     * How much of the guard's front the push covers, as a dot product against its facing.
     *
     * <p>Only what the Stand is actually between the owner and. Pushing in every direction would
     * make the stance a bubble, and it would also quietly cancel the counter: something circling
     * round the back is supposed to get through and be answered, not be held off by a guard that
     * was never facing it.
     */
    private static final double GUARD_PUSH_ARC = -0.1;

    /** How long the shattered overlay stays on the Stand after a break, in ticks. */
    private static final int GUARD_BROKEN_FLASH_TICKS = 14;

    // ---- What a guard break looks and sounds like - see GuardBreakParticle ------------------
    /**
     * How many pieces the guard comes apart into.
     *
     * <p>Enough to read as a sheet shattering rather than a handful of chips coming off. They are
     * cheap - one short-lived quad each - and the burst is a once-per-lockout event, so this is not
     * a number the frame rate has an opinion about.
     */
    private static final int GUARD_BREAK_SHARDS = 18;

    /**
     * How fast the pieces leave, in blocks per tick.
     *
     * <p>The floor matters as much as the range: every shard is thrown at least this fast so that
     * "standing still" is left free to mean the pane, which is how the two shapes are told apart
     * without a second particle type. See {@code GuardBreakParticle.PANE_SPEED_EPSILON}.
     */
    private static final double GUARD_BREAK_SHARD_SPEED = 0.18;
    private static final double GUARD_BREAK_SHARD_VARIANCE = 0.22;

    /** How far off the guard the pieces start, so they do not all leave from one point. */
    private static final double GUARD_BREAK_SCATTER = 0.3;

    /** Where the break sits on the Stand, as a share of its height - chest, where a guard is held. */
    private static final double GUARD_BREAK_HEIGHT = 0.55;

    /**
     * How long the counter rests before it can answer again, in ticks.
     *
     * <p>Long enough that holding block against a crowd is not a free strike on every blow that
     * lands. The guard still absorbs everything - only the reply is rationed.
     */
    private static final int COUNTER_COOLDOWN_TICKS = 70;
    private static final double FOLLOW_HEIGHT = 0.6;
    private static final double GUARD_HEIGHT = 0.0;
    private static final int PUNCH_STANCE_TICKS = 12;

    /**
     * How close to the front anchor the Stand must get before a held strike is released.
     *
     * <p>Attacks used to fire on the same tick they were requested, while the Stand was still
     * crossing from its resting place behind the user - so it threw its punches at the air on the
     * way past. Holding the animation until it arrives is what makes the swing land where the Stand
     * is actually standing.
     */
    private static final double STRIKE_ARRIVAL_DISTANCE = 0.55;

    /**
     * Longest a strike will wait for that arrival before going anyway.
     *
     * <p>The Stand can be prevented from ever reaching the anchor - the user backing away as fast
     * as it approaches, or a wall in between - and a queued attack that never fires would read as
     * the key being broken. Better a slightly early swing than a dead input.
     */
    private static final int STRIKE_WAIT_TIMEOUT_TICKS = 10;
    // Vertical nudge for the PARTIAL arms-only manifestation, which sits directly on the owner
    // rather than floating (see lockToOwner). 0 lines the Stand's shoulders up with the player's;
    // raise or lower it if the arms read as sitting too high or low on the body.
    private static final double PARTIAL_HEIGHT_OFFSET = 0.0;

    // Long-range punch: instead of swinging in place, the Stand flies out to whatever the
    // player's crosshair is on, strikes it, and flies straight back.
    private static final double PURSUIT_MELEE_RANGE = 2.0;
    // A little past StandCombatHandler's 7-block acquisition range, so a target backing off by a
    // step doesn't make the Stand give up the instant it commits.
    private static final double PURSUIT_ABANDON_RANGE = 9.0;
    /**
     * A far longer leash while the Stand is fighting something it chose.
     *
     * <p>The combo carries its target upward and throws it around, and a mob under attack rarely
     * holds still - on the ordinary leash the Stand would abandon its own combination halfway
     * through for drifting a few blocks. Focusing one enemy until it drops is the whole point of
     * DEFENSE, so the hunt is given room to finish.
     */
    private static final double DEFENSE_ABANDON_RANGE = 18.0;
    /**
     * What the Stand's fist is worth.
     *
     * <p>Three was tickle: with the half-second cadence that is six a second, which is under a
     * stone sword - an absurd number for the thing the whole mod is named after. Four at the
     * starting POWER of five comes out at 4.8 a swing, near ten a second, which lands just under a
     * diamond sword. Under rather than over on purpose: the swing is an area hit that costs no
     * durability and needs no item, so matching a sword outright would make every weapon in the
     * game pointless the moment you had a Stand.
     */
    private static final float PURSUIT_DAMAGE = 4.0F;
    private static final double PURSUIT_KNOCKBACK = 0.6;

    // Pursuit and the return trip both move at a flat speed rather than on a spring. A spring
    // reaches its target by decelerating into it and overshooting past it - which reads as the
    // Stand slingshotting out and rubber-banding back. Constant speed with a hard stop on arrival
    // gives the intended "flies out, hits, flies back" instead.
    private static final double PURSUIT_SPEED = 1.0;
    private static final double RETURN_SPEED = 0.9;

    /**
     * Autonomous hunting closes far slower than a commanded strike.
     *
     * <p>{@link #PURSUIT_SPEED} is deliberately violent: the player aimed at something and pressed
     * a key, so the Stand should cross the gap almost instantly. A DEFENSE Stand picking its own
     * fights is doing so continuously and unprompted, and at that speed it reads as a blur
     * teleporting between mobs rather than a Stand flying to them.
     */
    private static final double HUNT_SPEED = 0.42;

    /**
     * Beat of stillness after a kill before another target may be taken.
     *
     * <p>Without this, the idle re-scan fires on the tick immediately after {@code endEngagement},
     * so the Stand never gets so much as one frame back at its owner's side - it just snaps to the
     * next mob. The pause is what makes a sweep read as a sequence of fights instead of one
     * continuous streak across the field.
     */
    private static final int RETARGET_DELAY_TICKS = 28;

    // Engagement: once in range the Stand holds station a short way out and circles its target
    // while striking, instead of parking inside it.
    /** How far around its owner a DEFENSE Stand looks for something to fight. */
    private static final double DEFENSE_SCAN_RANGE = 8.0;

    /**
     * How far a commanded barrage reaches from its user.
     *
     * <p>Matched to the arms rather than to what the user can aim at. It was seven, which is the
     * acquisition range - the distance at which a target can be picked - and using it here meant the
     * flurry landed on anything within seven blocks while the Stand was visibly punching the air two
     * blocks in front of it. Acquisition and reach are different questions: the Stand travels to what
     * it has acquired, and only then can hit it.
     *
     * <p>The Stand holds station 1.6 blocks out and its arms carry roughly two more, so three and a
     * half is about where the animation actually stops.
     */
    private static final double COMMANDED_BARRAGE_REACH = 3.5;

    private static final double ENGAGE_RANGE = 1.6;
    private static final double ENGAGE_STRAFE_SPEED = 0.35;
    private static final double ORBIT_SPEED = 0.10;

    // Attack rhythm. Every few strikes becomes a barrage: a burst of fast, weaker hits landed over
    // the length of its looping animation, so the assault builds instead of metronoming.
    private static final int PUNCH_INTERVAL_TICKS = 11;

    // ---- The DEFENSE combo -------------------------------------------------------------------
    // A self-directed Stand does not simply trade punches: it opens with a rising blow, follows the
    // target up, holds it there, throws it down and finishes with a flurry. Written as a stage
    // machine because each beat has to finish before the next one reads correctly - launching and
    // barraging on the same tick is what made the earlier behaviour look like flailing.

    /** How long each stage runs before handing on, in ticks. */
    private static final int COMBO_LAUNCH_TICKS = 10;
    private static final int COMBO_CHASE_TICKS = 16;
    private static final int COMBO_HOLD_TICKS = 22;
    private static final int COMBO_SLAM_TICKS = 12;

    private static final float COMBO_LAUNCH_DAMAGE = 5.0F;
    private static final float COMBO_SLAM_DAMAGE = 6.0F;
    private static final double COMBO_LAUNCH_LIFT = 0.95;
    private static final double COMBO_SLAM_FORCE = 1.5;
    /** Where the Stand holds a grabbed target, relative to itself. */
    private static final double COMBO_HOLD_DISTANCE = 1.1;
    private static final double COMBO_CHASE_SPEED = 0.85;

    /**
     * Stand energy the combo spends per stage.
     *
     * <p>The design doc has a summoned Stand draining energy continuously, faster in combat. A combo
     * it chose to throw is the clearest case of that: it costs the user nothing to command, so it
     * has to cost something to run, or DEFENSE would be strictly better than fighting by hand.
     */
    private static final float COMBO_ENERGY_PER_STAGE = 3.5F;
    private static final int ATTACK_RECOVERY_TICKS = 8;
    private static final int BARRAGE_EVERY_N_ATTACKS = 3;
    // A barrage is meant to be a sustained assault, not a three-hit combo - it runs about three
    // seconds. Damage per blow stays low precisely because there are so many of them.
    private static final int BARRAGE_HITS = 20;
    private static final int BARRAGE_HIT_INTERVAL = 3;
    // Twenty of these land over three seconds, so the per-blow number is small and the total is
    // what matters: 36 at the starting POWER, against roughly 28 from three seconds of ordinary
    // swinging. A signature move that costs energy and locks the Stand into a six-second cooldown
    // should beat simply punching for the same length of time, but not by a multiple.
    private static final float BARRAGE_HIT_DAMAGE = 1.8F;
    // Close enough to the follow anchor to hand back to the spring without a visible jump.
    private static final double RETURN_ARRIVAL_DISTANCE = 0.35;

    // Spring-damper follow. The numbers moved to StandMovementProfile, one set per Stand - see
    // there for how they were measured and why the old pair, which overshot its target by 6.3% on
    // purpose, was wrong. Only ever used for the idle trail-behind, never for pursuit - see above.


    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    // Deferred to this entity's own first tick rather than fired immediately on spawn - a
    // trigger sent the same tick an entity is added commonly gets dropped client-side, since
    // the client hasn't finished registering the entity yet when the trigger packet arrives.
    private boolean spawnAnimationTriggered = false;
    // Server-side-only simulation state for the spring follow, not synced or persisted.
    private Vec3 followVelocity = Vec3.ZERO;

    /**
     * How far the Stand has pressed forward into the flurry it is throwing.
     *
     * <p>One push in, held, and one release out - not an oscillation. A per-blow rocking motion was
     * tried here and was wrong twice over. Mechanically it cannot be drawn: at twenty ticks a second
     * a lunge per punch is three samples a cycle, which aliases into a snap rather than a curve.
     * More importantly it was the wrong idea to begin with. The Stand already has a barrage
     * animation throwing the punches, and a second motion in world space at a different rate does
     * not add to it - the two disagree, and the result reads as rocking rather than as punching.
     *
     * <p>The doc asks for the body to move with the attack and says in the same breath that the
     * movement should be animation-driven. So the animation drives the punching, and this does the
     * one thing an animation cannot: it moves the Stand through the world, pressing into the target
     * over the flurry and easing back when it ends.
     *
     * <p>Kept off the anchor and applied on top of the sprung position. Measured, routing it through
     * the spring destroys it - a spring is a low-pass filter and only a few percent of a short
     * displacement survives. Last tick's offset is taken back off before the spring runs, so the
     * spring only ever sees the position without it and never fights a motion that was not an error.
     */
    private Vec3 barrageSway = Vec3.ZERO;

    /** How far into the press the Stand currently is, 0 to 1. */
    private float swayStrength = 0F;

    /**
     * How fast the press builds, and how fast it comes back.
     *
     * <p>Asymmetric on purpose, because the two directions are different things. Building is the
     * move: a flurry that keeps going drives the Stand further and further into whatever it is
     * hitting, so the ramp is slow enough that it is still gaining ground most of the way through a
     * full barrage rather than arriving at its limit in the first half second.
     *
     * <p>Coming back is not a move, it is the end of one. When the flurry stops - it ran out, or it
     * was interrupted - the Stand should return to where it stands normally promptly, not drift back
     * over three seconds. Roughly eight ticks, which the follow spring smooths anyway.
     */
    private static final float PRESS_IN_PER_TICK = 0.02F;
    private static final float PRESS_OUT_PER_TICK = 0.12F;


    // Server-side-only combat stance state, not synced or persisted - see StandCombatHandler.
    private boolean guarding = false;

    /** Who the guard is turned toward, and how long that is still worth believing. */
    private LivingEntity guardThreat;
    private int guardThreatTicks;
    private int counterCooldownTicks;
    private int punchStanceTicksRemaining = 0;

    /** How long the Stand stays squared up after its last blow. Two and a half seconds. */
    private static final int COMBAT_HOLD_TICKS = 50;

    /**
     * How far forward the ready position sits, as a share of the attack's reach.
     *
     * <p>Short of where a strike lands, so the blow itself is still a visible lunge out and back
     * rather than the Stand simply being there already.
     */
    private static final double COMBAT_REACH_FRACTION = 0.66;

    private int combatHoldTicks = 0;

    /**
     * Where the hand is, in the Stand's own space: out in front, on the side it fights from, at
     * about chest height.
     *
     * <p>Constants rather than a bone read off the animation. The doc asks for the grab point to
     * follow the animation, and that is the better version - but it needs the posed model, which
     * lives on the client, while the thing being held is positioned on the server. A fixed hand is
     * the honest half of that: it is in the right place, it turns with the Stand, and it does not
     * pretend to know what the arms are doing.
     */
    private static final double GRAB_HAND_FORWARD = 0.55;
    private static final double GRAB_HAND_SIDE = 0.35;
    private static final double GRAB_HAND_HEIGHT = 1.15;

    /**
     * How long the Stand will hold something before its grip opens on its own.
     *
     * <p>Six seconds. Something held indefinitely stops being a move and becomes a state the player
     * has to remember to leave, and a mob parked in a fist is a mob taken out of the game - which is
     * a strong enough effect that it should cost attention to maintain rather than being free once
     * paid for. Letting go on a timer also means no combination of disconnects, dimension changes or
     * forgotten inputs can leave something carried around for good.
     */
    private static final int GRAB_HOLD_TICKS = 120;

    /** How far out {@link #findFor} looks. A Stand never gets further from its user than this. */
    private static final double OWNER_SEARCH_RADIUS = 48.0;

    /**
     * The longest a flurry can be held.
     *
     * <p>Ten seconds. There has to be a ceiling or the move is not a move - a barrage nobody ever
     * stops is a permanent state with a key held down, and the press that builds while it runs would
     * sit at its limit indefinitely. Long enough that letting go is a decision rather than something
     * the game does for you.
     */
    private static final int HELD_BARRAGE_MAX_TICKS = 200;

    private int heldBarrageTicks = 0;

    /** What is being held, or null. Server-side: riding is synced by vanilla, so the client needs nothing. */
    private StandGrip grip;

    private int gripTicksRemaining = 0;

    /**
     * Ticks left before the Stand lets go of what it is holding.
     *
     * <p>Server-side only and counted down rather than being an expiry the client could derive,
     * because the clearing is a state change the client has to be told about anyway - it is the
     * synced stack going empty that makes the item disappear, not a clock either side reads.
     */
    private int heldItemTicks = 0;

    /**
     * Where the Stand has been sent to work, or null if it is simply following.
     *
     * <p>Server-owned. The client is told only that the Stand is engaged (see DATA_ENGAGED), which
     * is enough to make it stop predicting the follow and take the server's positions instead -
     * without that, the client would spring the Stand back to the player's shoulder every tick
     * while the server had it out at a block twenty metres away, and the two would fight.
     */
    private Vec3 utilityAnchor;
    private LivingEntity pursuitTarget = null;
    private boolean pursuitUsePunch2 = false;
    /** True when the Stand chose this fight itself (DEFENSE) rather than the player pointing at it. */
    private boolean huntingSelfDirected = false;
    /** Ticks left before DEFENSE may pick another target - see RETARGET_DELAY_TICKS. */
    private int retargetDelay = 0;
    /** Client-only: this instance is the combat bar's portrait, not a manifested Stand. See setPreviewMode. */
    private boolean previewMode = false;
    /**
     * Client-only: how the limbs are currently trailing the body - see {@link StandLimbFlow}.
     *
     * <p>Lives on the entity rather than on the model because GeckoLib caches one set of bones per
     * model file and shares it across every Stand on screen; state parked on the model would have
     * every Stand streaming its limbs according to whichever one was drawn last.
     */
    private final StandLimbFlow limbFlow = new StandLimbFlow();
    /**
     * The piloted flight's own velocity, kept apart from {@code deltaMovement} on purpose.
     *
     * <p>Vanilla's {@code travel()} consumes deltaMovement and moves the entity by it. Keeping the
     * flight there meant every tick was applied twice - once by the game, once by the pilot code -
     * so the Stand travelled at double the speed it was written to, and the two sides doubled it at
     * slightly different moments. Its own field is moved by exactly once, by the code that owns it.
     */
    private Vec3 pilotVelocity = Vec3.ZERO;

    /**
     * Client-side only: this is the Stand <em>this</em> client is flying.
     *
     * <p>Not synced and deliberately not derived from the owner UUID, which would need the local
     * player and so drag client-only types into an entity both sides load. Set by the pilot input
     * each tick. It is what separates "a Stand is being piloted" - true on every client watching -
     * from "I am the one piloting it", which is the only case that may ignore the server.
     */
    private boolean locallyPiloted;

    public Vec3 pilotVelocity() {
        return pilotVelocity;
    }

    public void setPilotVelocity(Vec3 velocity) {
        this.pilotVelocity = velocity;
    }

    public void setLocallyPiloted(boolean value) {
        this.locallyPiloted = value;
    }

    /** Flying home after a pursuit (or after giving up on one) - see tickReturn. */
    private boolean returningFromPursuit = false;
    // Engagement bookkeeping, server-side only.
    private int attackCooldown = 0;
    private int attacksThisEngagement = 0;
    private int barrageHitsRemaining = 0;
    private int barrageHitInterval = 0;
    /** Ticks left holding the front stance for a commanded barrage. */
    private int stationaryBarrageTicks = 0;

    /**
     * How far in front of its user the Stand plants itself to inhale.
     *
     * <p>Further out than the punch stance. The breath ends with everything it caught standing on
     * the spot the Stand is holding, and the point of anchoring the move there is that the pile
     * lands somewhere the user can reach without it landing on top of them - close enough to swing
     * at, far enough that it is the Stand being crowded and not the player.
     */
    private static final double INHALE_STAND_OFFSET = 2.6;

    /** How fast the Stand crosses to that spot before it settles, in blocks per tick. */
    private static final double INHALE_STEP_SPEED = 0.55;

    /** Close enough to the anchor to stop and hold, in blocks. */
    private static final double INHALE_ARRIVAL = 0.12;

    /** Ticks left holding the breath, where the Stand is standing, and which way it is aimed. */
    private int inhaleHoldTicks = 0;
    private Vec3 inhaleAnchor;
    private float inhaleYaw;
    /** Punishment absorbed since being cast - see hurt(). Server-side only. */
    private float strainTaken = 0F;
    /** Which beat of the DEFENSE combo is running, and how long it has left. */
    private ComboStage comboStage = ComboStage.NONE;
    private int comboStageTicks = 0;
    /** A strike waiting for the Stand to reach the front - see STRIKE_ARRIVAL_DISTANCE. */
    private PendingStrike pendingStrike = null;
    private int pendingStrikeTicks = 0;
    private double orbitAngle = 0.0;

    public StandEntity(EntityType<? extends StandEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.noPhysics = true;
        // A Stand does not stop you building. blocksBuilding is what Level.isUnobstructed walks
        // when deciding whether a block may go in a cell, and it defaults to true on every entity -
        // so a Stand hovering over the spot it was told to build on refuses its own placement. That
        // is not a hypothetical: aiming steeply down puts the target cell directly under the Stand,
        // which is exactly where it hovers, and the block silently never appeared.
        //
        // Cleared here rather than in the delegation, because it is not a fact about the delegation.
        // The thing is incorporeal - its owner walks through it - and nothing incorporeal should be
        // able to occupy a space a block wants.
        this.blocksBuilding = false;
        // Invisible from the moment it exists, not just until the spawn trigger arrives - see
        // the tick() comment on why relying on the animation's own scale-from-zero to hide it
        // left a visible "blip" at full size before the trigger took effect.
        this.setInvisible(true);
    }

    /** Registered against the entity type via architectury's EntityAttributeRegistry - placeholder values. */
    /**
     * How much punishment a manifestation absorbs before it collapses.
     *
     * <p>Not health, and deliberately not modelled as health. A Stand is a projection of its user's
     * will - it does not bleed and it cannot be killed, it can only be forced back. Letting it die
     * would leave a corpse, drop loot, play a death animation and orphan the user's summon state,
     * all of which are wrong for something that was never alive.
     */
    private static final float COLLAPSE_STRAIN = 24F;

    /**
     * Absorbs a hit without ever taking damage.
     *
     * <p>Vanilla's damage pipeline is bypassed entirely rather than being fed a large health pool,
     * because the two differ in every case that matters: no health means no death, no drops, no
     * damage tint, and nothing that can one-shot it regardless of the number. What accumulates
     * instead is strain, and reaching the limit forces the Stand back rather than killing it.
     *
     * <p>Returns true so whatever landed the blow still registers a hit - a mob that swings at a
     * Stand should not read it as having missed.
     */
    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (this.level().isClientSide() || isRemoved()) {
            return false;
        }

        strainTaken += amount;
        if (strainTaken >= COLLAPSE_STRAIN) {
            collapse();
        }

        return true;
    }

    /**
     * Forces the Stand back into its user, as if the cast had been broken.
     *
     * <p>Routed through the summon handler rather than simply discarding the entity, so the user's
     * own summon state and energy are cleared with it - a Stand removed behind the handler's back
     * leaves the player believing they still have one out.
     */
    private void collapse() {
        if (!(getOwner() instanceof ServerPlayer serverOwner)) {
            discard();
            return;
        }

        StandSummonHandler.collapse(serverOwner);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_OWNER, Optional.empty());
        builder.define(DATA_STAND_TYPE, "");
        builder.define(DATA_TRUST_TIER, TrustTier.BONDED.level());
        builder.define(DATA_DISMISS_START_TICK, -1);
        builder.define(DATA_MODE, StandMode.ANALOG.ordinal());
        builder.define(DATA_BARRAGING, false);
        builder.define(DATA_PILOTED, false);
        builder.define(DATA_FRONT_STANCE, false);
        builder.define(DATA_COMBAT_STANCE, false);
        builder.define(DATA_HELD_BARRAGE, false);
        builder.define(DATA_GUARDING, false);
        builder.define(DATA_GUARD_THREAT, -1);
        builder.define(DATA_GUARD_STRAIN, 0F);
        builder.define(DATA_INHALING, false);
        builder.define(DATA_GUARD_BROKEN, 0);
        builder.define(DATA_ENGAGED, false);
        builder.define(DATA_HELD_ITEM, ItemStack.EMPTY);
        builder.define(DATA_SKIN, 0);
        builder.define(DATA_SKIN_SWAP_TICK, -1);
    }

    /**
     * How long the Stand takes to reach full white, in ticks.
     *
     * <p>A rise, and then it holds there until something destroys it - it does not fade back,
     * because nothing it could fade back to survives. Short, because this is meant to land at the
     * same moment the player's own white-out does rather than trailing after it: the two are the
     * same event seen on two bodies, and a Stand that took a second to catch up would read as
     * reacting to its user instead of going with them.
     */
    public static final int SKIN_SWAP_WHITE_RISE_TICKS = 9;

    public void setSkin(int skin) {
        this.entityData.set(DATA_SKIN, Math.max(0, skin));
    }

    public int getSkin() {
        return this.entityData.get(DATA_SKIN);
    }

    /** Starts the white burn. The explosion and the new skin are the ritual's business, not ours. */
    public void beginSkinSwap() {
        this.entityData.set(DATA_SKIN_SWAP_TICK, this.tickCount);
    }

    /**
     * How white the swap has burned this Stand, 0 to 1.
     *
     * <p>Rises and then stays. Squared on the way up so it leaves its own colour quickly and
     * arrives gently, which reads as being overtaken by light rather than as a lamp being switched
     * on behind it.
     */
    public float skinSwapWhite(float partialTick) {
        int start = this.entityData.get(DATA_SKIN_SWAP_TICK);
        if (start < 0) {
            return 0F;
        }

        float progress = Mth.clamp((this.tickCount - start + partialTick) / SKIN_SWAP_WHITE_RISE_TICKS, 0F, 1F);
        return progress * progress;
    }

    /**
     * Starts the withdrawal fade. The entity keeps following and animating while it dissolves,
     * then removes itself once {@link #DISMISS_FADE_TICKS} have passed (see {@link #tick}) -
     * callers hand off here rather than discarding outright, so the Stand doesn't just blink out.
     */
    public void beginDismissal() {
        if (!isDismissing()) {
            this.entityData.set(DATA_DISMISS_START_TICK, this.tickCount);
        }
    }

    public boolean isDismissing() {
        return this.entityData.get(DATA_DISMISS_START_TICK) >= 0;
    }

    /** 0 at the moment withdrawal began, rising to 1 when the Stand should be gone. */
    private float dismissProgress() {
        int start = this.entityData.get(DATA_DISMISS_START_TICK);
        if (start < 0) {
            return 0F;
        }
        return Mth.clamp((this.tickCount - start) / (float) DISMISS_FADE_TICKS, 0F, 1F);
    }

    public void setTrustTier(TrustTier tier) {
        this.entityData.set(DATA_TRUST_TIER, tier.level());
    }

    public TrustTier getTrustTier() {
        return TrustTier.fromLevel(this.entityData.get(DATA_TRUST_TIER));
    }

    /** True once an {@link TrustTier#EMERGING} manifestation has outlived its window - see EnergySystem. */
    public boolean hasOutlivedEmergingWindow() {
        return getTrustTier().isTimeLimited() && this.tickCount >= TrustTier.EMERGING_DURATION_TICKS;
    }

    /**
     * Marks this instance as the HUD portrait stand-in rather than a real manifestation.
     *
     * <p>The portrait reuses this entity wholesale so it inherits every per-tier visual for free -
     * PARTIAL's arms-only body, EMERGING's flicker - but it isn't bound by the rules those visuals
     * exist to express, so the timed behaviours are skipped. See {@link #getRenderAlpha}.
     */
    /** Client-only. See {@link StandLimbFlow}. */
    public StandLimbFlow getLimbFlow() {
        return limbFlow;
    }

    public boolean isPreviewMode() {
        return previewMode;
    }

    public void setPreviewMode(boolean previewMode) {
        this.previewMode = previewMode;
    }

    public void setOwner(Player owner) {
        this.entityData.set(DATA_OWNER, Optional.of(owner.getUUID()));
    }

    /** The owner's UUID without resolving the player - the client uses this to spot its own Stand. */
    public Optional<UUID> getOwnerUuid() {
        return this.entityData.get(DATA_OWNER);
    }

    public Player getOwner() {
        return this.entityData.get(DATA_OWNER).map(uuid -> this.level().getPlayerByUUID(uuid)).orElse(null);
    }

    /**
     * A player's Stand, found by looking for it rather than by asking their data.
     *
     * <p>The usual lookup goes through the summon record, which only the server has. This one works
     * on either side, which is what a sustained move needs: the client has to know whether the move
     * it is holding the key for is actually running, and the only shared truth about that is the
     * entity itself.
     */
    public static StandEntity findFor(Player owner) {
        if (owner == null) {
            return null;
        }
        for (StandEntity stand : owner.level().getEntitiesOfClass(StandEntity.class,
                owner.getBoundingBox().inflate(OWNER_SEARCH_RADIUS))) {
            if (stand.getOwner() == owner) {
                return stand;
            }
        }
        return null;
    }

    public void setStandType(ResourceLocation id) {
        this.entityData.set(DATA_STAND_TYPE, id.toString());
    }

    /**
     * Overall model opacity, driving StandRenderer's tint alpha and StandModel's
     * cutout/translucent switch. Read client-side, but tickCount ticks locally on both sides
     * regardless, so this needs no synced state of its own.
     *
     * <p>Always includes the spawn fade-in. An {@link TrustTier#EMERGING} Stand additionally
     * never settles: it breathes in and out of transparency the whole time it's out, then
     * dissolves over the tail of its window rather than blinking out of existence.
     */
    public float getRenderAlpha() {
        float alpha = Mth.clamp(this.tickCount / (float) SPAWN_FADE_TICKS, 0F, 1F);

        // Withdrawal overrides everything else - once it starts, the Stand is on its way out
        // regardless of tier, so this multiplies down whatever it was already rendering at.
        if (isDismissing()) {
            return alpha * (1F - dismissProgress());
        }

        // A DORMANT Stand can't manifest at all, so the HUD portrait shows it as a faint impression
        // rather than a solid figure - the difference between owning a Stand and having one that
        // answers. Only ever reached in preview: DORMANT never spawns an entity in the world.
        if (previewMode && getTrustTier() == TrustTier.DORMANT) {
            return alpha * DORMANT_PREVIEW_ALPHA;
        }

        if (!getTrustTier().isTimeLimited()) {
            return alpha;
        }

        // The portrait keeps EMERGING's unstable flicker but skips its expiry: the timed fade-out
        // exists so a manifestation collapses on schedule, and a HUD icon has no schedule to keep.
        // Without this the portrait would quietly dissolve to nothing after twelve seconds on screen.
        if (!previewMode) {
            int remaining = TrustTier.EMERGING_DURATION_TICKS - this.tickCount;
            if (remaining < TrustTier.EMERGING_FADE_OUT_TICKS) {
                alpha = Math.min(alpha, Math.max(0F, remaining / (float) TrustTier.EMERGING_FADE_OUT_TICKS));
            }
        }

        float pulse = EMERGING_PULSE_FLOOR + (1F - EMERGING_PULSE_FLOOR)
                * (float) ((Math.sin(this.tickCount * EMERGING_PULSE_SPEED) + 1.0) * 0.5);
        return alpha * pulse;
    }

    public StandType getStandType() {
        String raw = this.entityData.get(DATA_STAND_TYPE);
        ResourceLocation id = raw.isEmpty() ? null : ResourceLocation.tryParse(raw);
        StandType type = id == null ? null : StandTypes.byId(id);
        return type != null ? type : StandTypes.byId(StandTypes.STAR_PLATINUM_ID);
    }

    @Override
    public void tick() {
        super.tick();
        tickTimeStopPose();

        Player owner = getOwner();
        if (owner == null || !owner.isAlive()) {
            if (!this.level().isClientSide()) {
                this.discard();
            }
            return;
        }

        boolean partial = getTrustTier().isPartialManifestation();

        if (this.level().isClientSide()) {
            // The client runs the follow itself rather than interpolating between position
            // packets. Those arrive only every few ticks, so a network-driven Stand visibly
            // stutters and lags behind its owner - it reads as a separate creature pathing after
            // them. Simulating the same spring locally, from the owner the client already tracks
            // perfectly, is what makes it move like something attached to the player.
            //
            // Only while it's simply following: once it's off fighting, the server owns where it
            // is and the client has no way to predict that, so it defers to the packets.
            if (isPiloted()) {
                // Nothing local moves a Stand being flown. The server owns its position, and the
                // corrections now arrive every tick (see the entity type's updateInterval) rather
                // than every third, so there is enough of them to ride a camera on.
                //
                // Facing follows the owner, who is the one holding the mouse. Only the tick values
                // are written here; the previous ones are deliberately left alone so the model
                // still interpolates between ticks like every other entity.
                //
                // The camera does not read these at all - see getViewYRot below. Forcing yRotO to
                // match yRot here was an attempt to stop the view arriving a tick late, and it did,
                // by removing the interpolation entirely: the camera then turned in twenty discrete
                // steps a second, which is the snap you can see rather than the lag you can feel.
                setYRot(owner.getYRot());
                setXRot(owner.getXRot());
                setYHeadRot(owner.getYRot());
            } else if (partial) {
                lockToOwner(owner);
            } else if (isClientPredictingFollow()) {
                tickFollow(owner);
            }

            // Sampled last, once something has actually moved the Stand this tick. The level calls
            // setOldPosAndRot immediately before tick(), so xo/yo/zo still equal the current
            // position on entry - measuring the travel any earlier reads a flat zero every time.
            limbFlow.tick(getX() - xo, getY() - yo, getZ() - zo, getYRot());
            return;
        }

        // Withdrawal runs its fade to completion and then removes the entity itself. It keeps
        // following its owner throughout, so it dissolves in place on them rather than detaching
        // and fading somewhere they've already walked away from.
        if (isDismissing() && dismissProgress() >= 1F) {
            this.discard();
            return;
        }

        if (!spawnAnimationTriggered) {
            spawnAnimationTriggered = true;
            LOGGER.info("[jojoha] StandEntity {} triggering spawn animation on tick {}", this.getId(), this.tickCount);
            this.setInvisible(false);
            this.triggerAnim(CONTROLLER, "spawn");
        }

        if (punchStanceTicksRemaining > 0) {
            punchStanceTicksRemaining--;
        }
        if (combatHoldTicks > 0) {
            combatHoldTicks--;
        }
        tickGrip();
        tickHeldBarrage();

        // Only the server counts this down: it owns the synched value, and letting the client
        // clear its own copy would put the item back the next time the server sent the field.
        if (heldItemTicks > 0 && --heldItemTicks <= 0 && !this.level().isClientSide()) {
            this.entityData.set(DATA_HELD_ITEM, ItemStack.EMPTY);
        }

        if (stationaryBarrageTicks > 0) {
            stationaryBarrageTicks--;
        }

        if (inhaleHoldTicks > 0 && --inhaleHoldTicks <= 0) {
            inhaleAnchor = null;
        }

        if (attackCooldown > 0) {
            attackCooldown--;
        }
        tickPendingStrike(owner);
        // Barrage hits land on their own clock, independent of repositioning - see tickBarrage.
        tickBarrage(owner);

        // Target died or slipped out of reach - break off, but still fly home properly rather
        // than letting the follow spring yank it back from wherever it got to.
        double abandonRange = huntingSelfDirected ? DEFENSE_ABANDON_RANGE : PURSUIT_ABANDON_RANGE;
        if (pursuitTarget != null && (!pursuitTarget.isAlive() || pursuitTarget.distanceTo(owner) > abandonRange)) {
            endEngagement();
        }

        if (retargetDelay > 0) {
            retargetDelay--;
        }

        if (guardThreatTicks > 0 && --guardThreatTicks <= 0) {
            guardThreat = null;
        }

        if (counterCooldownTicks > 0) {
            counterCooldownTicks--;
        }

        int brokenTicks = this.entityData.get(DATA_GUARD_BROKEN);
        if (brokenTicks > 0) {
            this.entityData.set(DATA_GUARD_BROKEN, brokenTicks - 1);
        }

        if (guarding) {
            pushBackFromGuard(owner);
        }

        // While blocking with nothing on record, the guard looks for something itself, so it is
        // already turned the right way when the first blow lands rather than a moment after.
        if (guarding && guardThreat == null) {
            LivingEntity nearby = findNearbyHostile(owner);
            if (nearby != null) {
                guardThreat = nearby;
                guardThreatTicks = GUARD_THREAT_MEMORY;
            }
        }

        // DEFENSE hunts on its own. Re-scanned only when idle and only once the post-kill pause has
        // run out, so it finishes the fight it's in and visibly disengages before looking for the
        // next one rather than flitting between whatever wanders closest.
        // stationaryBarrageTicks is part of the guard because a commanded barrage leaves
        // pursuitTarget null by design - without it, DEFENSE would spot a hostile on the next tick
        // and fly the Stand away in the middle of the flurry its user just asked for.
        if (pursuitTarget == null && retargetDelay <= 0 && stationaryBarrageTicks <= 0
                && inhaleHoldTicks <= 0
                && getMode().isAutonomous() && getTrustTier().canActAtRange()) {
            LivingEntity hostile = findNearbyHostile(owner);
            if (hostile != null) {
                pursueAndPunch(hostile, pursuitUsePunch2);
                huntingSelfDirected = true;
            }
        }

        // Published for the client's own copy of the follow simulation.
        this.entityData.set(DATA_FRONT_STANCE,
                guarding || punchStanceTicksRemaining > 0 || stationaryBarrageTicks > 0);
        this.entityData.set(DATA_COMBAT_STANCE, combatHoldTicks > 0);
        this.entityData.set(DATA_GUARDING, guarding);
        this.entityData.set(DATA_GUARD_THREAT,
                guarding && guardThreat != null && guardThreat.isAlive() && guardThreatTicks > 0
                        ? guardThreat.getId()
                        : -1);
        this.entityData.set(DATA_BARRAGING, barrageHitsRemaining > 0 || isHeldBarrageRunning());
        this.entityData.set(DATA_ENGAGED,
                pursuitTarget != null || returningFromPursuit || utilityAnchor != null);
        this.entityData.set(DATA_INHALING, inhaleHoldTicks > 0);

        if (isPiloted()) {
            // Position is owned by the pilot's client this tick; the Stand holds wherever it was
            // put. Deliberately ahead of every other branch so nothing else can fight it.
            faceOwnerlessIdle();
        } else if (inhaleHoldTicks > 0) {
            tickInhaleHold();
        } else if (utilityAnchor != null) {
            tickUtilityWork();
        } else if (pursuitTarget != null) {
            tickPursuit(owner);
        } else if (returningFromPursuit) {
            tickReturn(owner);
        } else if (partial) {
            lockToOwner(owner);
        } else {
            tickFollow(owner);
        }
    }

    /**
     * Ignores server position corrections while the client is predicting the follow itself.
     *
     * <p>This is what the bobbling was. Vanilla drip-feeds a networked entity toward each position
     * packet over several ticks, so the client was doing both jobs at once: nudging the Stand
     * toward a position the server sent a few ticks ago, then running its own spring from wherever
     * that nudge left it. The two pull in slightly different directions every tick, and the
     * disagreement is largest exactly when the player is moving - which is why it read as a wobble
     * while walking and looked fine standing still.
     *
     * <p>Corrections are still accepted whenever the client <em>isn't</em> predicting (during a
     * fight, or for a PARTIAL Stand), so the server stays authoritative wherever it actually owns
     * the position.
     */
    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        if (isClientPredictingFollow()) {
            return;
        }

        // The pilot's own client decides where its Stand is and tells the server, so anything
        // arriving here is that client's own position after a round trip - strictly staler than
        // what it already has. There is no threshold at which taking it would be an improvement,
        // and a threshold is exactly what the rubber-banding was: two simulations drifted apart,
        // the snap put them together, and the drift began again immediately.
        //
        // Only for the client actually flying it. Everyone else watching wants these packets, and
        // is the only way they ever see the Stand move.
        if (isPiloted() && locallyPiloted) {
            return;
        }
        super.lerpTo(x, y, z, yRot, xRot, steps);
    }

    /**
     * View rotation for a Stand its own pilot is flying: the pilot's, live.
     *
     * <p>{@code Camera.setup} builds the view from {@code getViewYRot(partialTick)}, and for an
     * ordinary entity that interpolates between the last two ticks - which caps the camera at the
     * tick rate. A player's own view does not suffer that, because the mouse writes their rotation
     * every frame and the interpolation lands on a value that is already current.
     *
     * <p>A camera riding a Stand has no such luck: this entity's rotation is only written once a
     * tick, so however it is interpolated the result moves in twenty steps a second. Deferring to
     * the owner sidesteps the whole problem - the answer comes from the entity the mouse is
     * actually turning, at whatever rate it is being turned.
     *
     * <p>Only for the pilot's own client. Everyone else is watching a networked entity and wants
     * the interpolated rotation they were sent.
     */
    @Override
    public float getViewYRot(float partialTicks) {
        Player owner = pilotView();
        return owner == null ? super.getViewYRot(partialTicks) : owner.getViewYRot(partialTicks);
    }

    @Override
    public float getViewXRot(float partialTicks) {
        Player owner = pilotView();
        return owner == null ? super.getViewXRot(partialTicks) : owner.getViewXRot(partialTicks);
    }

    /** The owner whose eyes this Stand is being flown through, or null if that is not the case. */
    private Player pilotView() {
        return locallyPiloted && isPiloted() ? getOwner() : null;
    }

    /** True when this client is simulating the follow locally rather than replaying the server's. */
    private boolean isClientPredictingFollow() {
        return this.level().isClientSide()
                && !isEngaged()
                && !isInhaling()
                && !isPiloted()
                && !getTrustTier().isPartialManifestation();
    }

    /** How this particular Stand carries itself. Same simulation for all of them, different numbers. */
    private StandMovementProfile movement() {
        return StandMovementProfile.forStand(getStandType());
    }

    /** The idle follow, run identically on both sides so the client's prediction matches. */
    private void tickFollow(Player owner) {
        StandMovementProfile profile = movement();

        this.setPos(this.position().subtract(barrageSway));
        springTo(followAnchor(owner), profile.springStiffness(), profile.springDamping());
        barrageSway = nextBarrageSway(owner, profile);
        this.setPos(this.position().add(barrageSway));

        if (guarding) {
            faceGuard(owner);
            return;
        }

        faceOwner(owner);
    }

    /** How far into its flurry the Stand should be leaning this tick. */
    private Vec3 nextBarrageSway(Player owner, StandMovementProfile profile) {
        boolean barraging = this.entityData.get(DATA_BARRAGING)
                && this.entityData.get(DATA_FRONT_STANCE);

        swayStrength = barraging
                ? Math.min(1F, swayStrength + PRESS_IN_PER_TICK)
                : Math.max(0F, swayStrength - PRESS_OUT_PER_TICK);

        if (swayStrength <= 0F) {
            return Vec3.ZERO;
        }

        Vec3 look = owner.getLookAngle();
        Vec3 forward = new Vec3(look.x, 0, look.z);
        forward = forward.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : forward.normalize();

        // The envelope is the whole motion: it presses in for as long as the flurry lasts, up to a
        // limit, and comes back when it ends. Under three hundredths of a block a tick going out and
        // under a seventh coming back, so neither direction is a movement the eye has to catch up to.
        return forward.scale(profile.barragePush() * swayStrength);
    }

    /**
     * Holds the ground in front of the owner while the stance is up.
     *
     * <p>Applied every tick rather than as one shove, for the same reason the breath is: anything
     * with an AI writes its own velocity each frame, so a single impulse is overwritten before it
     * has travelled anywhere. Leaning on it continuously means the mob and the guard are arguing
     * over the same value every tick, and the guard wins while it is up.
     *
     * <p>Deliberately narrow about what it touches. A barrier that shoved the owner's own pets and
     * passing villagers around would be a nuisance rather than a stance, so it moves what is
     * actually a threat: hostiles by type, other players, and any mob that has picked the owner as
     * its target - which is what catches the wolves and golems that are dangerous without being
     * {@link Enemy}.
     */
    private void pushBackFromGuard(Player owner) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        Vec3 look = owner.getLookAngle();
        Vec3 forward = new Vec3(look.x, 0, look.z).normalize();
        Vec3 facing = guardDirection(owner, forward);

        AABB area = owner.getBoundingBox().inflate(GUARD_PUSH_RANGE);

        for (LivingEntity threat : serverLevel.getEntitiesOfClass(LivingEntity.class, area,
                candidate -> candidate != owner && candidate.isAlive()
                        && !(candidate instanceof StandEntity))) {

            if (!isWorthPushing(threat, owner)) {
                continue;
            }

            Vec3 away = threat.position().subtract(owner.position());
            Vec3 flat = new Vec3(away.x, 0, away.z);
            double distance = flat.length();
            if (distance > GUARD_PUSH_RANGE || distance < 1.0E-4) {
                continue;
            }

            Vec3 outward = flat.scale(1.0 / distance);
            if (outward.dot(facing) < GUARD_PUSH_ARC) {
                // Behind the guard. Left alone on purpose - see GUARD_PUSH_ARC.
                continue;
            }

            // Firms up as it closes, so pressing into the guard costs more the further in you get.
            double strength = GUARD_PUSH_STRENGTH * (1.0 - distance / GUARD_PUSH_RANGE);

            threat.setDeltaMovement(threat.getDeltaMovement()
                    .add(outward.x * strength, GUARD_PUSH_LIFT, outward.z * strength));
            // Without this the pushed client keeps its own prediction and slides back through.
            threat.hurtMarked = true;
            threat.hasImpulse = true;
        }
    }

    /**
     * Throws an attacker clear of the owner after the guard has taken its blow.
     *
     * <p>Aimed away from the owner rather than away from the Stand, so something that got round the
     * side is pushed out rather than around. Straight up-and-out with the vertical set outright:
     * a shove that only worked horizontally would slide an attacker along the floor and leave it
     * walking straight back in on the same tick.
     */
    private void deflect(LivingEntity attacker, Player owner) {
        Vec3 away = attacker.position().subtract(owner.position());
        Vec3 flat = new Vec3(away.x, 0, away.z);
        if (flat.lengthSqr() < 1.0E-4) {
            return;
        }

        Vec3 outward = flat.normalize().scale(GUARD_DEFLECT_KNOCKBACK);
        attacker.setDeltaMovement(attacker.getDeltaMovement().x * 0.4 + outward.x,
                GUARD_DEFLECT_LIFT,
                attacker.getDeltaMovement().z * 0.4 + outward.z);
        attacker.hurtMarked = true;
        attacker.hasImpulse = true;
    }

    /** Whether the guard has any business shoving this - see pushBackFromGuard. */
    private static boolean isWorthPushing(LivingEntity candidate, Player owner) {
        if (candidate instanceof Enemy || candidate instanceof Player) {
            return true;
        }
        return candidate instanceof Mob mob && mob.getTarget() == owner;
    }

    /**
     * Crosses to the breathing spot and stays there.
     *
     * <p>Anchored to a point in the world rather than to the owner, which is the whole change: the
     * Stand is a fixed thing for the length of the breath and the user is free to walk around it.
     * Walking away does not drag the anchor along, so the pile of mobs it is gathering stays where
     * it was gathered.
     *
     * <p>Moved at a flat speed with a hard stop rather than on the follow spring. A spring reaches
     * its target by decelerating into it and drifting past, and a Stand that is supposed to be
     * standing still cannot be seen to settle - it has to arrive and stop.
     */
    /**
     * Sends the Stand out to a spot and keeps it there.
     *
     * <p>Deliberately a position rather than a task. The Stand's job is to be somewhere; what it
     * does once it arrives is decided by {@code StandUtilityWork}, which is watching for it to get
     * there. Splitting it that way means the entity never has to know what an item is.
     */
    public void sendToWork(Vec3 anchor) {
        if (pursuitTarget != null || returningFromPursuit) {
            endEngagement();
            returningFromPursuit = false;
        }
        this.utilityAnchor = anchor;
    }

    /** Hands the Stand back to following. The spring carries it home on its own. */
    public void clearWork() {
        this.utilityAnchor = null;
    }

    public boolean isWorking() {
        return this.utilityAnchor != null;
    }

    /** Whether it has got there yet - what the work system waits on before acting. */
    public boolean hasArrivedAtWork() {
        return utilityAnchor != null
                && this.position().distanceToSqr(utilityAnchor) <= UTILITY_ARRIVAL * UTILITY_ARRIVAL;
    }

    /**
     * Flies toward the work anchor and holds there.
     *
     * <p>Stepped rather than sprung, like the inhale hold and unlike the follow. A spring is right
     * for trailing a moving player, where the lag is the character of the thing; it is wrong for
     * crossing open ground to a fixed point, where it would overshoot and wobble in exactly the
     * place the player is watching to see whether the block landed.
     */
    private void tickUtilityWork() {
        Vec3 toAnchor = utilityAnchor.subtract(this.position());
        double distance = toAnchor.length();

        if (distance <= 1.0E-3) {
            this.setPos(utilityAnchor.x, utilityAnchor.y, utilityAnchor.z);
        } else {
            Vec3 step = toAnchor.scale(Math.min(UTILITY_STEP_SPEED, distance) / distance);
            this.setPos(this.getX() + step.x, this.getY() + step.y, this.getZ() + step.z);
        }

        this.setDeltaMovement(Vec3.ZERO);
        clearFollowMotion();
    }

    private void tickInhaleHold() {
        if (inhaleAnchor == null) {
            inhaleAnchor = this.position();
        }

        Vec3 toAnchor = inhaleAnchor.subtract(this.position());
        double distance = toAnchor.length();

        if (distance <= INHALE_ARRIVAL) {
            this.setPos(inhaleAnchor.x, inhaleAnchor.y, inhaleAnchor.z);
        } else {
            Vec3 step = toAnchor.scale(Math.min(INHALE_STEP_SPEED, distance) / distance);
            this.setPos(this.getX() + step.x, this.getY() + step.y, this.getZ() + step.z);
        }

        this.setDeltaMovement(Vec3.ZERO);

        float eased = Mth.approachDegrees(this.getYRot(), inhaleYaw, movement().rotationSpeed());
        this.setYRot(eased);
        this.setYBodyRot(eased);
        this.setYHeadRot(eased);
    }

    /**
     * Points the guard at what it is guarding against.
     *
     * <p>Not at the owner's facing, which is what it used to do - a user turning to run would swing
     * their own guard away from the thing chasing them. The stance holds its line and lets the
     * player look wherever they like.
     */
    private void faceGuard(Player owner) {
        Vec3 look = owner.getLookAngle();
        Vec3 forward = new Vec3(look.x, 0, look.z).normalize();
        Vec3 facing = guardDirection(owner, forward);

        float yaw = (float) (Mth.atan2(facing.z, facing.x) * (180F / Math.PI)) - 90F;
        float eased = Mth.approachDegrees(this.getYRot(), yaw, movement().combatRotationSpeed());

        this.setYRot(eased);
        this.setYBodyRot(eased);
        this.setYHeadRot(eased);
    }

    /** Synced: locked onto something. Read client-side to square the Stand up - see StandRenderer. */
    public boolean isEngaged() {
        return this.entityData.get(DATA_ENGAGED);
    }

    /** Where the Stand wants to sit while simply following - behind-right, or in front mid-punch/guard. */
    private Vec3 followAnchor(Player owner) {
        // A bound Stand has nowhere to stand. Its arms come out of its user, so there is no offset
        // to work out and no stance to read - the anchor is simply where they are. Gated here rather
        // than at the call sites because this one method is where every stance, mode and guard
        // decision about position already meets, and it deliberately runs on both sides.
        if (getStandType().form().isBound()) {
            return owner.position();
        }

        Vec3 look = owner.getLookAngle();
        Vec3 forward = new Vec3(look.x, 0, look.z).normalize();
        Vec3 right = new Vec3(-forward.z, 0, forward.x);

        // Read from synced data rather than the server-only fields, so the client's copy of this
        // simulation resolves to the same anchor the server does.
        //
        // Only guarding and mid-punch bring it forward. DEFENSE deliberately does NOT plant it
        // in front - that Stand is out hunting, not standing sentry over its user.
        StandMovementProfile profile = movement();
        boolean front = this.entityData.get(DATA_FRONT_STANCE);
        double height = this.entityData.get(DATA_GUARDING) ? GUARD_HEIGHT : profile.verticalOffset();

        // What the owner is doing moves the anchor before the spring ever sees it, so the reaction
        // costs nothing extra in smoothing - the Stand arrives at the new anchor the same controlled
        // way it arrives anywhere.
        //
        // Every input here is a synced flag rather than a derived quantity, because both sides run
        // this method and have to agree on its answer.
        if (owner.isCrouching()) {
            height -= profile.crouchDrop();
        }

        double behind = profile.followDistance();
        if (owner.isSprinting()) {
            // Leaning into the run: closing the gap at speed reads as the Stand driving forward
            // with its user rather than being towed along behind them.
            behind *= 1.0 - profile.sprintLean();
        }
        if (!owner.onGround()) {
            // Airborne, the owner covers ground far faster than a walk and the trail would stretch
            // into a separation. Pulling the anchor in keeps them together through the arc.
            behind *= profile.fallCatchUp();
        }

        if (this.entityData.get(DATA_GUARDING)) {
            // Pressed against the owner and turned across their front toward whatever is coming.
            return owner.position().add(guardDirection(owner, forward).scale(GUARD_OFFSET))
                    .add(0, height, 0);
        }

        if (front) {
            // Off the owner's line rather than dead ahead - see StandMovementProfile for why that
            // one offset is most of what makes an attack read as the Stand's rather than the user's.
            return owner.position()
                    .add(forward.scale(profile.attackForward()))
                    .add(right.scale(profile.attackSide()))
                    .add(0, height, 0);
        }

        // Squared up between blows: the same shoulder the strike comes over, drawn back short of
        // where it lands. Holding this rather than returning to the idle spot is what turns a run of
        // attacks into one exchange instead of a series of round trips.
        if (this.entityData.get(DATA_COMBAT_STANCE)) {
            return owner.position()
                    .add(forward.scale(profile.attackForward() * COMBAT_REACH_FRACTION))
                    .add(right.scale(profile.attackSide()))
                    .add(0, height, 0);
        }

        // Utility waits at the shoulder rather than at the back - see UTILITY_SIDE_OFFSET. Read
        // from synced mode data so the client's own copy of the follow resolves to the same spot.
        if (StandMode.fromOrdinal(this.entityData.get(DATA_MODE)).handlesItems()) {
            return owner.position().add(right.scale(UTILITY_SIDE_OFFSET)).add(0, height, 0);
        }

        // No artificial bob or sway here on purpose. Oscillating the anchor made the Stand wobble
        // even while standing still, and worse, that motion fed the renderer's velocity-driven
        // banking - so a wobble the anchor invented came back amplified as a tilt. The spring's
        // own lag against a moving player already supplies the life those were meant to add.
        return owner.position()
                .add(right.scale(profile.sideOffset()))
                .add(forward.scale(-behind))
                .add(0, height, 0);
    }

    /**
     * Which way the guard faces: toward the threat, clamped to the owner's front.
     *
     * <p>Clamped rather than followed outright, because a Stand that tracked a threat all the way
     * round would end up behind its user - which is not a block any more, it is an escort. Beyond
     * the arc the guard holds at the edge of it and the counter takes over instead.
     */
    private Vec3 guardDirection(Player owner, Vec3 forward) {
        Vec3 toThreat = guardThreatDirection(owner);
        if (toThreat == null) {
            return forward;
        }

        double alignment = toThreat.dot(forward);
        if (alignment >= GUARD_ARC) {
            return toThreat;
        }

        // Outside the arc: slide to its edge on the side the threat is on, rather than snapping to
        // dead ahead - the guard should still be leaning the right way.
        Vec3 right = new Vec3(-forward.z, 0, forward.x);
        double side = Math.signum(toThreat.dot(right));
        if (side == 0) {
            side = 1;
        }

        double lateral = Math.sqrt(Math.max(0, 1 - GUARD_ARC * GUARD_ARC));
        return forward.scale(GUARD_ARC).add(right.scale(lateral * side)).normalize();
    }

    /** The flat direction to whatever the guard is watching, or null if it is watching nothing. */
    private Vec3 guardThreatDirection(Player owner) {
        int id = this.entityData.get(DATA_GUARD_THREAT);
        if (id < 0) {
            return null;
        }

        Entity threat = this.level().getEntity(id);
        if (threat == null || !threat.isAlive()) {
            return null;
        }

        Vec3 away = threat.position().subtract(owner.position());
        Vec3 flat = new Vec3(away.x, 0, away.z);
        return flat.lengthSqr() < 1.0E-4 ? null : flat.normalize();
    }

    /**
     * Tells the guard something just hit its user, and lets it answer if it came from behind.
     *
     * <p>A blow from the front is what a block is for and needs no reply. One from behind got past
     * the guard because the guard was never facing it - so the Stand turns and hits back, which is
     * the only way the stance is worth holding against something circling you.
     *
     * <p>The reply is a real pursuit, not a gesture. It was a bare {@code triggerPunchAt}, which
     * plays the swing and sets the stance and deals no damage whatsoever - the Stand mimed a counter
     * at something standing behind it and nothing happened. Handing the attacker to the pursuit
     * machinery is what puts the Stand on it: it crosses the ground, connects, knocks them off their
     * feet and comes home.
     */
    public void onGuardHit(LivingEntity attacker, Player owner) {
        if (attacker == null || !attacker.isAlive()) {
            return;
        }

        guardThreat = attacker;
        guardThreatTicks = GUARD_THREAT_MEMORY;

        // Thrown off before anything else is decided. Every blocked blow buys ground back,
        // whichever direction it came from and whether or not a counter is owed for it.
        deflect(attacker, owner);

        Vec3 look = owner.getLookAngle();
        Vec3 forward = new Vec3(look.x, 0, look.z).normalize();
        // Straight off the attacker rather than through the synced id, which is not published until
        // the end of this tick - the counter has to decide now.
        Vec3 away = attacker.position().subtract(owner.position());
        Vec3 flat = new Vec3(away.x, 0, away.z);
        Vec3 toAttacker = flat.lengthSqr() < 1.0E-4 ? null : flat.normalize();

        if (toAttacker == null || toAttacker.dot(forward) >= 0 || counterCooldownTicks > 0) {
            return;
        }

        counterCooldownTicks = COUNTER_COOLDOWN_TICKS;

        // Turned on the spot first so the spin reads as the Stand noticing, then sent after them.
        faceTowards(attacker);
        pursueAndPunch(attacker, false);

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, this.blockPosition(), ModSounds.STAND_JUMP.get(),
                    SoundSource.PLAYERS, 0.7F, 1.25F);
        }
    }

    public void setMode(StandMode mode) {
        this.entityData.set(DATA_MODE, mode.ordinal());
    }

    /** Whether the block stance is up. Synced, so the renderer and the client follow can both see it. */
    public boolean isGuarding() {
        return this.entityData.get(DATA_GUARDING);
    }

    /** Read straight from synced data rather than a local copy, so both sides can never disagree. */
    public StandMode getMode() {
        return StandMode.fromOrdinal(this.entityData.get(DATA_MODE));
    }

    /**
     * Turns toward the owner's facing rather than snapping to it.
     *
     * <p>Copying the yaw outright makes the Stand pivot in the same instant the player's mouse
     * does, which is the single most mob-like tell there is - nothing physical turns with zero
     * inertia. Easing in gives the body a moment to swing round after the intent, so it reads as
     * being carried along by the user rather than glued to their camera.
     */
    private void faceOwner(Player owner) {
        float turn = movement().rotationSpeed();
        float yaw = Mth.approachDegrees(this.getYRot(), owner.getYRot(), turn);
        float headYaw = Mth.approachDegrees(this.getYHeadRot(), owner.getYHeadRot(), turn);

        this.setYRot(yaw);
        this.setYBodyRot(yaw);
        this.setYHeadRot(headYaw);

        // Pitch, so the head has something to look up and down with. The Stand had none at all -
        // nothing ever wrote xRot - so it stared dead level however far its user craned. Eased at the
        // same rate as the yaw, which is what keeps the head turning like a head rather than snapping
        // onto the mouse.
        //
        // Written on the entity rather than read off the owner in the renderer because entity pitch
        // is already synced to everyone watching. Doing it here means other players see the Stand
        // look where its user is looking, for free.
        this.setXRot(Mth.approachDegrees(this.getXRot(), owner.getXRot(), turn));
    }

    /** The trip home after a strike: straight back at a flat speed, then hand off to the follow spring. */
    private void tickReturn(Player owner) {
        Vec3 anchor = followAnchor(owner);
        moveTowardsAtSpeed(anchor, RETURN_SPEED);
        faceOwner(owner);

        if (this.position().distanceTo(anchor) <= RETURN_ARRIVAL_DISTANCE) {
            returningFromPursuit = false;
        }
    }

    /**
     * PARTIAL manifestation: the arms are cast around the user's own body rather than belonging
     * to a separate figure trailing them, so the entity is pinned flat onto the owner instead of
     * spring-following an offset point. No offsets, no overshoot, no punch/guard repositioning -
     * it simply *is* where the player is, and turns with them.
     *
     * <p>Runs on the client as well as the server. Positioning it server-side alone still visibly
     * drags when walking: the client only gets periodic position packets and interpolates toward
     * them, so the arms trail a few ticks behind. Copying the owner's <em>previous</em>-tick
     * position and rotation too (xo/yo/zo, yRotO and friends) matters just as much - the renderer
     * lerps between previous and current by the frame's partial tick, so matching both endpoints
     * is what makes the arms land on exactly the same interpolated spot as the player's own body
     * every frame, instead of on a slightly stale one.
     */
    private void lockToOwner(Player owner) {
        this.setPos(owner.getX(), owner.getY() + PARTIAL_HEIGHT_OFFSET, owner.getZ());
        this.xo = owner.xo;
        this.yo = owner.yo + PARTIAL_HEIGHT_OFFSET;
        this.zo = owner.zo;
        clearFollowMotion();

        this.setYRot(owner.getYRot());
        this.setYBodyRot(owner.yBodyRot);
        this.setYHeadRot(owner.getYHeadRot());
        this.yRotO = owner.yRotO;
        this.yBodyRotO = owner.yBodyRotO;
        this.yHeadRotO = owner.yHeadRotO;
    }

    /**
     * Stays on a target and works it over until it drops, rather than landing one hit and leaving.
     *
     * <p>Rather than parking on the target's position, the Stand circles it while striking - the
     * orbit angle advances every tick and it holds station a short way out. A Stand that flies to
     * a mob and then freezes inside it looks like a stuck entity; one that keeps moving around its
     * opponent reads as fighting it.
     */
    private void tickPursuit(Player owner) {
        Vec3 center = pursuitTarget.position().add(0, pursuitTarget.getBbHeight() * 0.5, 0);
        double distance = this.position().distanceTo(center);

        // A self-directed Stand runs its combo instead of the plain approach-and-swing, but only
        // once it has actually closed the distance - the chain opens with an uppercut, which needs
        // to be within arm's reach to mean anything.
        if (huntingSelfDirected && (comboStage != ComboStage.NONE || distance <= ENGAGE_RANGE + 0.5)
                && tickCombo(owner)) {
            return;
        }

        if (distance > ENGAGE_RANGE) {
            // Still closing. A commanded strike goes in at full speed; a self-chosen one flies.
            moveTowardsAtSpeed(center, huntingSelfDirected ? HUNT_SPEED : PURSUIT_SPEED);
        } else {
            // In range: strafe around it on a slow orbit, staying at fighting distance.
            orbitAngle += ORBIT_SPEED;
            Vec3 station = center.add(Math.cos(orbitAngle) * ENGAGE_RANGE, 0, Math.sin(orbitAngle) * ENGAGE_RANGE);
            moveTowardsAtSpeed(station, ENGAGE_STRAFE_SPEED);
        }

        faceTarget();

        if (distance <= ENGAGE_RANGE + 0.5 && attackCooldown <= 0) {
            performAttack(owner);
        }
    }

    /**
     * Puts something in the Stand's hand for a moment, and reaches out with it.
     *
     * <p>A copy of one, not the stack itself. The stack is about to be used and may be spent by
     * the using; what should be drawn is what the Stand reached out with, which is a single item
     * regardless of how many were in the hand it came from.
     */
    public void showHeldItem(ItemStack stack, int ticks) {
        this.entityData.set(DATA_HELD_ITEM, stack.copyWithCount(1));
        this.heldItemTicks = Math.max(this.heldItemTicks, ticks);
    }

    /**
     * Turns to a point and reaches for it.
     *
     * <p>The grab pose rather than a punch: reaching out and closing a hand on something is what
     * this animation already is, and it is the same gesture whether what is being reached for is a
     * mob's collar or a block face. Given the punch stance window so the Stand holds still through
     * it instead of drifting back into formation mid-reach.
     */
    public void reachTowards(Vec3 point) {
        Vec3 face = point.subtract(this.position());
        if (face.horizontalDistanceSqr() > 1.0E-4) {
            float yaw = (float) (Mth.atan2(face.z, face.x) * (180.0 / Math.PI)) - 90.0F;
            this.setYRot(yaw);
            this.setYBodyRot(yaw);
            this.setYHeadRot(yaw);
        }

        this.triggerAnim(CONTROLLER, "grab");
        punchStanceTicksRemaining = PUNCH_STANCE_TICKS;
    }

    private void faceTowards(LivingEntity target) {
        Vec3 face = target.position().subtract(this.position());
        float yaw = (float) (Mth.atan2(face.z, face.x) * (180.0 / Math.PI)) - 90.0F;
        this.setYRot(yaw);
        this.setYBodyRot(yaw);
        this.setYHeadRot(yaw);
    }

    /** While piloted the Stand keeps its own facing - the pilot sets it from their steering. */
    private void faceOwnerlessIdle() {
        // Intentionally empty: holding station is the correct behaviour, and writing it as a named
        // no-op keeps the branch in tick() readable rather than looking like a missing case.
    }

    /** Alternates single strikes with a longer barrage, so the assault has rhythm rather than a flat tempo. */
    /**
     * The stages of a self-directed assault, in order.
     *
     * <p>Loops back to LAUNCH rather than stopping, so a target that survives the whole chain simply
     * gets it again - the Stand stays on one enemy until it is dead.
     */
    private enum ComboStage {
        NONE,
        /** An uppercut that puts the target in the air. */
        LAUNCH,
        /** Following it up. */
        CHASE,
        /** Holding it off the ground, helpless. */
        HOLD,
        /** Driving it back down. */
        SLAM,
        /** The flurry that finishes the chain. */
        BARRAGE
    }

    /**
     * Runs the combo while the Stand is fighting something it picked itself.
     *
     * @return true if the combo owns this tick, meaning the ordinary attack rhythm should stand down
     */
    private boolean tickCombo(Player owner) {
        if (!huntingSelfDirected || pursuitTarget == null) {
            comboStage = ComboStage.NONE;
            return false;
        }

        if (comboStage == ComboStage.NONE) {
            beginComboStage(owner, ComboStage.LAUNCH);
            return comboStage != ComboStage.NONE;
        }

        switch (comboStage) {
            case CHASE -> {
                // Stay under the target as it rises, so the grab has something to close on.
                Vec3 below = pursuitTarget.position().add(0, pursuitTarget.getBbHeight() * 0.3, 0);
                moveTowardsAtSpeed(below, COMBO_CHASE_SPEED);
            }
            case HOLD -> holdTarget();
            case BARRAGE -> {
                // The slam threw it clear, so close the gap again rather than punching where it
                // used to be.
                Vec3 center = pursuitTarget.position().add(0, pursuitTarget.getBbHeight() * 0.5, 0);
                if (this.position().distanceTo(center) > ENGAGE_RANGE) {
                    moveTowardsAtSpeed(center, COMBO_CHASE_SPEED);
                }
            }
            default -> {
            }
        }

        faceTarget();

        if (--comboStageTicks > 0) {
            return true;
        }

        beginComboStage(owner, nextStage(comboStage));
        return comboStage != ComboStage.NONE;
    }

    private static ComboStage nextStage(ComboStage stage) {
        return switch (stage) {
            case LAUNCH -> ComboStage.CHASE;
            case CHASE -> ComboStage.HOLD;
            case HOLD -> ComboStage.SLAM;
            case SLAM -> ComboStage.BARRAGE;
            // Round again: the chain repeats until the target stops getting up.
            default -> ComboStage.LAUNCH;
        };
    }

    private void beginComboStage(Player owner, ComboStage stage) {
        comboStage = stage;

        if (!spendComboEnergy(owner)) {
            // Out of energy - fall back to trading ordinary punches rather than stalling mid-chain.
            comboStage = ComboStage.NONE;
            huntingSelfDirected = false;
            return;
        }

        switch (stage) {
            case LAUNCH -> {
                comboStageTicks = COMBO_LAUNCH_TICKS;
                triggerPunchAt(pursuitTarget, false);
                dealHit(owner, StandTuning.damage("combo_launch", COMBO_LAUNCH_DAMAGE));
                launchTarget();
            }
            case CHASE -> comboStageTicks = COMBO_CHASE_TICKS;
            case HOLD -> {
                comboStageTicks = COMBO_HOLD_TICKS;
                this.triggerAnim(CONTROLLER, "grab");
            }
            case SLAM -> {
                comboStageTicks = COMBO_SLAM_TICKS;
                triggerPunchAt(pursuitTarget, true);
                dealHit(owner, StandTuning.damage("combo_slam", COMBO_SLAM_DAMAGE));
                slamTarget();
            }
            case BARRAGE -> {
                comboStageTicks = BARRAGE_HITS * BARRAGE_HIT_INTERVAL + ATTACK_RECOVERY_TICKS;
                this.triggerAnim(CONTROLLER, "barrage");
                barrageHitsRemaining = BARRAGE_HITS;
                barrageHitInterval = BARRAGE_HIT_INTERVAL;
            }
            default -> comboStageTicks = 0;
        }
    }

    /** Vertical motion is replaced rather than added, so a falling target still gets picked up. */
    private void launchTarget() {
        Vec3 motion = pursuitTarget.getDeltaMovement();
        pursuitTarget.setDeltaMovement(motion.x * 0.2, COMBO_LAUNCH_LIFT, motion.z * 0.2);
        pursuitTarget.hurtMarked = true;
    }

    /**
     * Keeps a grabbed target pinned in front of the Stand.
     *
     * <p>Its position is written every tick rather than its velocity: a mob left to its own physics
     * would keep falling out of the hold, and fighting that with impulses reads as a struggle rather
     * than as being held.
     */
    private void holdTarget() {
        Vec3 held = this.position()
                .add(this.getLookAngle().scale(COMBO_HOLD_DISTANCE))
                .add(0, this.getBbHeight() * 0.4, 0);

        pursuitTarget.setPos(held.x, held.y, held.z);
        pursuitTarget.setDeltaMovement(Vec3.ZERO);
        pursuitTarget.fallDistance = 0F;
        pursuitTarget.hurtMarked = true;
    }

    /** Sends the held target back down and away, which is what sets up the flurry. */
    private void slamTarget() {
        Vec3 away = this.getLookAngle().scale(COMBO_SLAM_FORCE);
        pursuitTarget.setDeltaMovement(away.x, -COMBO_SLAM_FORCE, away.z);
        pursuitTarget.hurtMarked = true;
    }

    private boolean spendComboEnergy(Player owner) {
        if (!(owner instanceof ServerPlayer serverOwner)) {
            return false;
        }

        JojohaPlayerData data = PlayerDataAccess.get(serverOwner);
        if (data.standEnergy < COMBO_ENERGY_PER_STAGE) {
            return false;
        }

        data.standEnergy -= COMBO_ENERGY_PER_STAGE;
        PlayerDataAccess.set(serverOwner, data);
        PlayerDataAccess.sync(serverOwner);
        return true;
    }

    private void performAttack(Player owner) {
        boolean barrage = ++attacksThisEngagement % BARRAGE_EVERY_N_ATTACKS == 0;

        if (barrage) {
            this.triggerAnim(CONTROLLER, "barrage");
            barrageHitsRemaining = BARRAGE_HITS;
            attackCooldown = BARRAGE_HITS * BARRAGE_HIT_INTERVAL + ATTACK_RECOVERY_TICKS;
        } else {
            this.triggerAnim(CONTROLLER, pursuitUsePunch2 ? "punch2" : "punch");
            pursuitUsePunch2 = !pursuitUsePunch2;
            dealHit(owner, StandTuning.damage("pursuit", PURSUIT_DAMAGE));
            attackCooldown = PUNCH_INTERVAL_TICKS;
        }
    }

    /** One blow, with the knockback and the owner's combat timer refresh that go with it. */
    private void dealHit(Player owner, float damage) {
        LivingEntity target = pursuitTarget;
        if (target == null || !(this.level() instanceof ServerLevel serverLevel)
                || !(owner instanceof ServerPlayer serverOwner)) {
            return;
        }

        // Callers pass the base number and the stat is applied here, so there is exactly one place
        // that knows POWER exists rather than one per move. Passives get the same treatment for the
        // same reason - see StandPassives.scaleOutgoing.
        JojohaPlayerData data = PlayerDataAccess.get(serverOwner);
        float dealt = org.gumel.jojoha.stand.passive.StandPassives.scaleOutgoing(serverOwner, data, target,
                damage * data.stand.powerScale());

        target.hurt(serverLevel.damageSources().playerAttack(owner), dealt);
        impactRings(serverLevel, target, dealt, barrageHitsRemaining > 0 ? BARRAGE_RING_CLUSTER : 1);
        // Pitched slightly at random so a twenty-hit barrage is a flurry rather than a metronome.
        serverLevel.playSound(null, target.blockPosition(), ModSounds.STAND_HIT.get(),
                SoundSource.PLAYERS, 0.8F, 0.92F + this.random.nextFloat() * 0.16F);
        Vec3 push = target.position().subtract(this.position()).normalize().scale(PURSUIT_KNOCKBACK);
        target.setDeltaMovement(target.getDeltaMovement().add(push.x, 0.1, push.z));
        target.hurtMarked = true;

        data.combatTicks = StandCombatHandler.COMBAT_TIMER_TICKS;
        PlayerDataAccess.set(serverOwner, data);
    }

    /**
     * Lands the rapid hits of a barrage while its animation loops.
     *
     * <p>Kept out of {@link #tickPursuit} so the flurry keeps connecting even on the ticks the
     * Stand is repositioning, which is what makes a barrage feel continuous rather than like a
     * series of separate punches wearing one animation.
     */
    private void tickBarrage(Player owner) {
        if (barrageHitsRemaining <= 0) {
            return;
        }


        if (--barrageHitInterval <= 0) {
            barrageHitInterval = BARRAGE_HIT_INTERVAL;
            barrageHitsRemaining--;
            if (pursuitTarget != null) {
                dealHit(owner, StandTuning.damage("barrage_hit", BARRAGE_HIT_DAMAGE));
            } else {
                sweepBarrage(owner, StandTuning.damage("barrage_hit", BARRAGE_HIT_DAMAGE));
            }

            if (barrageHitsRemaining <= 0) {
                this.stopTriggeredAnim(CONTROLLER, "barrage");
            }
        }
    }

    /**
     * One blow of a stationary barrage, landing on everything in front of the user.
     *
     * <p>Aimed from the owner rather than from the Stand: the Stand is hovering at the user's
     * shoulder while this plays, so using its position would bias the arc a little to one side of
     * where the player is actually looking.
     */
    /**
     * Marks a landed blow with a ring at the point of contact.
     *
     * <p>Sized from the damage, so a barrage's rapid taps read as a scatter of small rings while a
     * heavy single strike lands one large one - the visual weight follows the actual weight rather
     * than every hit looking alike.
     */
    private void impactRing(ServerLevel level, LivingEntity target, float damage) {
        impactRings(level, target, damage, 1);
    }

    /** How many rings one barrage blow scatters, and how far they sit from the point of contact. */
    private static final int BARRAGE_RING_CLUSTER = 3;
    private static final double RING_SCATTER = 0.45;

    /**
     * Marks a landed blow with rings at the point of contact.
     *
     * <p>A single strike lands one ring, deliberately placed. A barrage blow scatters a handful in
     * mixed sizes instead, because the move is a wall of impacts rather than one - a lone ring per
     * hit reads as a series of separate punches, where a cluster of large and small together reads
     * as the flurry it is.
     */
    private void impactRings(ServerLevel level, LivingEntity target, float damage, int count) {
        Vec3 at = target.position().add(0, target.getBbHeight() * 0.6, 0);
        double scale = Mth.clamp(0.6 + damage * 0.12, 0.6, 1.8);

        for (int i = 0; i < count; i++) {
            double x = at.x + (count == 1 ? 0 : (this.random.nextDouble() - 0.5) * RING_SCATTER);
            double y = at.y + (count == 1 ? 0 : (this.random.nextDouble() - 0.5) * RING_SCATTER);
            double z = at.z + (count == 1 ? 0 : (this.random.nextDouble() - 0.5) * RING_SCATTER);

            // count=0 so the velocity slots carry data rather than a spread: size multiplier, then
            // the size band. -1 leaves the band to the particle, which is what mixes them.
            level.sendParticles(ModRegistries.IMPACT_RING.get(), x, y, z, 0, scale, -1.0, 0.0, 1.0);
        }
    }

    private void sweepBarrage(Player owner, float damage) {
        if (!(this.level() instanceof ServerLevel serverLevel) || !(owner instanceof ServerPlayer serverOwner)) {
            return;
        }

        JojohaPlayerData data = PlayerDataAccess.get(serverOwner);
        float dealt = damage * data.stand.powerScale();

        // Centred on the Stand rather than on its user. It used to be measured forward from the
        // player, which worked only while the Stand stood directly in front of them - now that it
        // fights from off to one side and further out, a box drawn around the player misses the
        // space the punches are actually being thrown in. Anything the Stand is holding sat right on
        // the edge of it and usually outside, which is why a flurry would not register on something
        // in its own fist.
        Vec3 look = owner.getLookAngle();
        Vec3 centre = this.position().add(0, this.getBbHeight() * 0.5, 0)
                .add(look.scale(COMMANDED_BARRAGE_REACH * 0.5));
        AABB area = new AABB(centre, centre).inflate(COMMANDED_BARRAGE_REACH * 0.5);

        for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != owner && entity.isAlive() && !(entity instanceof StandEntity))) {

            // Only what is genuinely in front - the box alone would also catch things behind.
            Vec3 toVictim = victim.position().subtract(owner.position());
            // Tightened along with the reach. A cone this wide at seven blocks was most of the
            // hemisphere in front of the player; at three and a half it is roughly what the swing
            // covers.
            if (toVictim.lengthSqr() > 1.0E-4 && look.dot(toVictim.normalize()) < 0.55) {
                continue;
            }

            // Per victim rather than once for the sweep: a passive that reads the distance to
            // what it is hitting has a different answer for each of them.
            float onVictim = org.gumel.jojoha.stand.passive.StandPassives.scaleOutgoing(serverOwner, data, victim, dealt);
            victim.hurt(serverLevel.damageSources().playerAttack(owner), onVictim);
            impactRings(serverLevel, victim, onVictim, BARRAGE_RING_CLUSTER);
            // Cleared so every blow of the flurry lands instead of most being eaten by the
            // invulnerability window a normal hit leaves behind.
            victim.invulnerableTime = 0;
            victim.hurtMarked = true;
        }

        data.combatTicks = StandCombatHandler.COMBAT_TIMER_TICKS;
        PlayerDataAccess.set(serverOwner, data);
    }

    private void faceTarget() {
        Vec3 face = pursuitTarget.position().subtract(this.position());
        float yaw = (float) (Mth.atan2(face.z, face.x) * (180.0 / Math.PI)) - 90.0F;
        this.setYRot(yaw);
        this.setYBodyRot(yaw);
        this.setYHeadRot(yaw);
    }

    /**
     * Nearest hostile worth attacking near the owner, or null if the coast is clear.
     *
     * <p>Scanned around the <em>owner</em> rather than the Stand, so a Stand that has chased
     * something to the edge of its leash doesn't then use its own position to justify wandering
     * further. Hostility is taken from {@link Enemy}, which is what every hostile mob implements,
     * so passive animals and other players are never picked up.
     */
    private LivingEntity findNearbyHostile(Player owner) {
        AABB search = owner.getBoundingBox().inflate(DEFENSE_SCAN_RANGE);
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;

        for (LivingEntity candidate : this.level().getEntitiesOfClass(LivingEntity.class, search,
                entity -> entity instanceof Enemy && entity.isAlive() && !(entity instanceof StandEntity))) {
            double distance = candidate.distanceToSqr(owner);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }

        return best;
    }

    // ---- Skill hooks. Everything below is driven by StandSkills rather than by the Stand itself. ----

    /**
     * Opens a barrage from where the Stand already is, held in front of its user.
     *
     * <p>Deliberately not an engagement. Sending the Stand out to circle its target is right when
     * it picked the fight itself, but a barrage the user called for should read as their Stand
     * throwing punches over their shoulder - it stays on the follow anchor, in the front stance, and
     * the blows reach out from there. Flying away to do it would put the spectacle somewhere the
     * user isn't looking.
     *
     * <p>Any engagement already running is dropped first, or the pursuit branch would keep moving
     * the Stand while this expects it to hold station.
     *
     * <p>Takes no target on purpose. A barrage thrown from where the user is standing is a wall of
     * punches in front of them, not a lock on one creature - so each blow sweeps whatever is in
     * reach at that moment. It also means the move can always be thrown, which matters: requiring
     * something under the crosshair made it fail silently whenever the aim was slightly off.
     */
    public void beginCommandedBarrage() {
        if (pursuitTarget != null || returningFromPursuit) {
            endEngagement();
            returningFromPursuit = false;
        }

        // The stance is taken now so the Stand starts moving forward; the flurry itself is held
        // until it gets there. Long enough to cover the approach as well as every hit, or the
        // Stand would drop out of stance partway through and drift back mid-barrage.
        stationaryBarrageTicks = STRIKE_WAIT_TIMEOUT_TICKS
                + BARRAGE_HITS * BARRAGE_HIT_INTERVAL + PUNCH_STANCE_TICKS;
        queueStrike(PendingStrike.BARRAGE);
    }

    /** Plays the leap flourish. Purely cosmetic - the throw itself is applied to the player. */
    public void triggerLeapPose() {
        this.triggerAnim(CONTROLLER, "punch2");
        punchStanceTicksRemaining = PUNCH_STANCE_TICKS;
    }

    /** Turns to face a target and throws an ordinary punch at it. */
    public void triggerPunchAt(LivingEntity target, boolean usePunch2) {
        faceTowards(target);
        this.triggerAnim(CONTROLLER, usePunch2 ? "punch2" : "punch");
        takeAttackStance();
    }

    /** Turns to face what was hit and throws Star Finger's own thrust, finger bone and all. */
    public void triggerStarFingerPose(LivingEntity target) {
        faceTowards(target);
        this.triggerAnim(CONTROLLER, "star_finger");
        takeAttackStance();
    }

    /**
     * Plants the Stand in front of its user and starts the breath.
     *
     * <p>The anchor is taken once, here, from where the user was aiming at the moment they cast -
     * not read from them each tick. That is what makes the move a place rather than an attachment:
     * the pull, the air and the Stand all belong to one spot on the ground, and the user can step
     * away from it, line up an uppercut on what is arriving, or simply stand next to it and swing.
     */
    public void beginInhale(Player owner, int ticks) {
        if (pursuitTarget != null || returningFromPursuit) {
            endEngagement();
            returningFromPursuit = false;
        }

        Vec3 look = owner.getLookAngle();
        Vec3 forward = new Vec3(look.x, 0, look.z);
        forward = forward.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : forward.normalize();

        inhaleAnchor = owner.position().add(forward.scale(INHALE_STAND_OFFSET))
                .add(0, movement().verticalOffset(), 0);
        inhaleYaw = (float) (Mth.atan2(forward.z, forward.x) * (180F / Math.PI)) - 90F;
        inhaleHoldTicks = ticks;

        triggerInhalePose();
    }

    /** Where the Stand is holding its breath, or null if it is not. Read by the pull. */
    public Vec3 inhaleAnchor() {
        return inhaleAnchor;
    }

    /** Synced: planted taking a breath. */
    public boolean isInhaling() {
        return this.entityData.get(DATA_INHALING);
    }

    /** The deep breath, played while Inhale drags the world around. */
    public void triggerInhalePose() {
        this.triggerAnim(CONTROLLER, "inhale");
        punchStanceTicksRemaining = PUNCH_STANCE_TICKS;
    }

    /**
     * Sends the Stand to intercept something in flight and swat it.
     *
     * <p>Placed just short of the projectile rather than on top of it, so the Stand appears to reach
     * out and strike rather than to occupy the same space as what it is hitting. Position is set
     * outright because this is an interception - a spring easing across would arrive after the shot
     * had already passed.
     *
     * <p>The follow spring is left holding no velocity, so once the swat is done the Stand drifts
     * back to its user from rest instead of being flung past them by momentum it never really had.
     */
    public void interceptAt(Vec3 point, boolean usePunch2) {
        Vec3 approach = point.subtract(this.position());
        double distance = approach.length();
        Vec3 stopAt = distance > INTERCEPT_STANDOFF
                ? point.subtract(approach.scale(INTERCEPT_STANDOFF / distance))
                : this.position();

        this.setPos(stopAt.x, stopAt.y, stopAt.z);
        clearFollowMotion();

        float yaw = (float) (Mth.atan2(approach.z, approach.x) * (180.0 / Math.PI)) - 90.0F;
        this.setYRot(yaw);
        this.setYBodyRot(yaw);
        this.setYHeadRot(yaw);

        this.triggerAnim(CONTROLLER, usePunch2 ? "punch2" : "punch");
        takeAttackStance();
    }

    /** How far short of the target the Stand stops when intercepting. */
    private static final double INTERCEPT_STANDOFF = 0.9;

    /**
     * Length of the time-stop animation, in ticks.
     *
     * <p>The pose has to be released on the animation's own length rather than at the end of the
     * wind-up. GeckoLib will not report a triggered animation as finished while the controller still
     * considers the trigger current, so an animation shorter than the cast simply starts again - and
     * at 1.375 seconds against a 2.4 second wind-up, it played through and then began a second time.
     */
    private static final int TIMESTOP_POSE_TICKS = 28;

    private int timeStopPoseTicks;

    /** The pose held while time is stopped. Released on its own length - see tickTimeStopPose. */
    public void triggerTimeStopPose() {
        this.triggerAnim(CONTROLLER, "timestop");
        this.timeStopPoseTicks = TIMESTOP_POSE_TICKS;
    }

    /** Counts the pose down and clears the trigger the moment the animation has run its length. */
    private void tickTimeStopPose() {
        if (this.timeStopPoseTicks > 0 && --this.timeStopPoseTicks == 0) {
            this.stopTriggeredAnim(CONTROLLER, "timestop");
        }
    }

    /**
     * Releases the time-stop pose early.
     *
     * <p>Still needed for interrupts, where the cast is broken before the animation has run out.
     */
    public void stopTimeStopPose() {
        this.timeStopPoseTicks = 0;
        this.stopTriggeredAnim(CONTROLLER, "timestop");
    }

    /** Turns to face what is being grabbed and plays the reach. */
    public void triggerGrabAt(LivingEntity target) {
        faceTowards(target);
        this.triggerAnim(CONTROLLER, "grab");
        takeAttackStance();
    }

    /**
     * Hands position control to the pilot.
     *
     * <p>Any engagement in progress is dropped: the follow spring and the pilot would otherwise
     * both be writing the Stand's position every tick, and the Stand would judder between the two
     * rather than obeying either.
     */
    public void beginPiloting() {
        endEngagement();
        returningFromPursuit = false;
        this.entityData.set(DATA_PILOTED, true);
    }

    public void endPiloting() {
        this.entityData.set(DATA_PILOTED, false);
        // Cleared so the follow spring resumes from rest rather than inheriting the flight's
        // momentum and slinging the Stand past its owner on the way home.
        clearFollowMotion();
    }

    /** Client-facing: whether to draw the arms smeared. See StandRenderer. */
    public boolean isBarraging() {
        return this.entityData.get(DATA_BARRAGING);
    }

    public boolean isPiloted() {
        return this.entityData.get(DATA_PILOTED);
    }

    /** Drops the current target and heads home, tidying up any barrage still in flight. */
    private void endEngagement() {
        comboStage = ComboStage.NONE;
        comboStageTicks = 0;
        pursuitTarget = null;
        returningFromPursuit = true;
        huntingSelfDirected = false;
        attacksThisEngagement = 0;
        attackCooldown = 0;
        retargetDelay = RETARGET_DELAY_TICKS;

        if (barrageHitsRemaining > 0) {
            barrageHitsRemaining = 0;
            this.stopTriggeredAnim(CONTROLLER, "barrage");
        }
    }

    /**
     * Moves a flat distance toward a point, stopping dead on arrival instead of easing into it.
     * Also clears {@code followVelocity} so the follow spring picks up from rest afterwards
     * rather than inheriting the flight's momentum and flinging the Stand past its anchor.
     */
    private void moveTowardsAtSpeed(Vec3 target, double speed) {
        clearFollowMotion();

        Vec3 delta = target.subtract(this.position());
        double distance = delta.length();
        if (distance <= speed) {
            this.setPos(target);
            return;
        }

        this.setPos(this.position().add(delta.scale(speed / distance)));
    }

    /**
     * Sends the Stand to travel to and melee a distant target, instead of swinging in place.
     * Refused at {@link TrustTier#PARTIAL}: only the arms are manifested there, anchored to the
     * user as reinforcement, so there's no body to send anywhere - it just swings in place.
     */
    public void pursueAndPunch(LivingEntity target, boolean usePunch2) {
        if (!getTrustTier().canActAtRange()) {
            punch(usePunch2);
            return;
        }

        // Re-ordering onto a new target keeps the engagement running rather than restarting it,
        // so switching victims mid-fight doesn't reset the attack rhythm back to a single punch.
        if (this.pursuitTarget != target) {
            this.attacksThisEngagement = 0;
            this.orbitAngle = this.random.nextDouble() * Math.PI * 2;
        }

        this.pursuitTarget = target;
        this.pursuitUsePunch2 = usePunch2;
        this.returningFromPursuit = false;
        // Assumed commanded; the DEFENSE scan flips this back on immediately after calling in.
        this.huntingSelfDirected = false;
    }

    /** Semi-implicit Euler integration of a spring toward target - the idle trail-behind follow only. */
    /** Starts a strike's pose and keeps the Stand squared up for a while after it. */
    private void takeAttackStance() {
        punchStanceTicksRemaining = PUNCH_STANCE_TICKS;
        combatHoldTicks = COMBAT_HOLD_TICKS;
    }

    /**
     * What the Stand is doing, in priority order.
     *
     * <p>The order is the point - it is the same one the tick's own branches run in, written down
     * where it can be read. Derived rather than stored; see {@link StandMovementState}.
     */
    public StandMovementState movementState() {
        if (isDismissing() || !hasOutlivedEmergingWindow()) return StandMovementState.SUMMON;
        if (isHolding()) return StandMovementState.HOLDING;
        if (isPiloted()) return StandMovementState.PILOTED;
        if (isInhaling()) return StandMovementState.WORKING;
        // isEngaged is the synced flag and covers being sent to a job as well as hunting. The
        // server-only isWorking() could tell those two apart, and deliberately is not used: a state
        // that answers differently depending on which side you ask it is worse than a coarse one,
        // and every caller so far treats both the same way anyway.
        if (isEngaged()) return StandMovementState.PURSUING;
        if (isGuarding()) return StandMovementState.GUARDING;
        if (isBarraging()) return StandMovementState.BARRAGE;
        if (this.entityData.get(DATA_FRONT_STANCE)) return StandMovementState.ATTACK;
        if (this.entityData.get(DATA_COMBAT_STANCE)) return StandMovementState.COMBAT;

        Player owner = getOwner();
        boolean moving = owner != null && owner.getDeltaMovement().horizontalDistanceSqr() > 1.0E-4;
        return moving ? StandMovementState.FOLLOW : StandMovementState.IDLE;
    }

    /**
     * Where something the Stand is carrying rides: in its hand.
     *
     * <p>This is the whole of the held thing's position. Vanilla asks for it every tick and again at
     * render time with a partial tick, which is why the yaw is interpolated here - using the raw
     * rotation would step the held mob around the Stand twenty times a second while the Stand itself
     * moved smoothly.
     *
     * <p>The offset is adjusted for how big the thing is, which is the doc's point about not using
     * one grab transform for every entity. A cow held on the same point as a bat would have the
     * Stand's arm buried in its ribs, so the hold is pushed out by half the passenger's width and
     * the passenger is dropped by half its height - the second part because an entity's position is
     * its feet, and what should line up with the hand is its middle.
     */
    @Override
    protected Vec3 getPassengerAttachmentPoint(net.minecraft.world.entity.Entity passenger,
                                               net.minecraft.world.entity.EntityDimensions dimensions,
                                               float partialTick) {
        float yaw = Mth.rotLerp(partialTick, this.yRotO, this.getYRot());
        float radians = yaw * ((float) Math.PI / 180F);
        Vec3 forward = new Vec3(-Mth.sin(radians), 0, Mth.cos(radians));
        Vec3 right = new Vec3(-forward.z, 0, forward.x);

        double clearance = passenger.getBbWidth() * 0.5;
        double drop = passenger.getBbHeight() * 0.5;

        return forward.scale(GRAB_HAND_FORWARD + clearance)
                .add(right.scale(GRAB_HAND_SIDE))
                .add(0, GRAB_HAND_HEIGHT - drop, 0);
    }

    /** Takes hold of something. False if it could not be held - see {@link StandGrip#take}. */
    public boolean grab(LivingEntity target) {
        if (grip != null) {
            return false;
        }
        grip = StandGrip.take(this, target);
        if (grip == null) {
            return false;
        }
        gripTicksRemaining = GRAB_HOLD_TICKS;
        triggerGrabAt(target);
        return true;
    }

    public boolean isHolding() {
        return grip != null;
    }

    public LivingEntity heldEntity() {
        return grip == null ? null : grip.held();
    }

    /**
     * Lets go, with force.
     *
     * <p>Takes the velocity rather than working one out, because the throw belongs to whatever asked
     * for it - a throw forward, a slam downward and simply setting something down are the same act
     * here and differ only in which way and how hard.
     */
    public void releaseHeld(Vec3 velocity) {
        if (grip == null) {
            return;
        }
        grip.release(velocity);
        grip = null;
        takeAttackStance();
    }

    /**
     * Whatever the Stand was holding is put down before the Stand itself goes.
     *
     * <p>Not optional housekeeping. The hold switches the mob's AI and gravity off and remembers what
     * they were, and that memory lives on the Stand - so a Stand that vanishes while gripping
     * something leaves a mob frozen and floating with nothing left in the world that knows how to
     * undo it. Dismissal, death, unloading and despawn all arrive here, which is why the release
     * hangs off removal rather than off any one of them.
     */
    @Override
    public void remove(RemovalReason reason) {
        releaseHeld(Vec3.ZERO);
        super.remove(reason);
    }

    /**
     * Starts a flurry that runs until it is let go of.
     *
     * <p>Built on the fixed one rather than beside it: the approach, the stance and the animation are
     * all the same, and the only difference is that this one refills itself. Two separate barrages
     * would be two places to fix anything about how a barrage works.
     */
    public void beginHeldBarrage() {
        setHeldBarrage(true);
        heldBarrageTicks = 0;
        beginCommandedBarrage();
    }

    /**
     * Lets go.
     *
     * <p>The blows already in flight are left to land. Cutting them off mid-swing would end the move
     * on a frame rather than on a beat, and the handful of ticks it takes to finish is exactly the
     * follow-through that makes a flurry look like it stopped rather than vanished.
     */
    public void endHeldBarrage() {
        setHeldBarrage(false);
    }

    public boolean isHeldBarrageRunning() {
        return this.entityData.get(DATA_HELD_BARRAGE);
    }

    private void setHeldBarrage(boolean held) {
        this.entityData.set(DATA_HELD_BARRAGE, held);
    }

    /** Tops the flurry back up for as long as it is being held, up to the ceiling. */
    private void tickHeldBarrage() {
        if (!isHeldBarrageRunning()) {
            return;
        }
        if (++heldBarrageTicks >= HELD_BARRAGE_MAX_TICKS) {
            setHeldBarrage(false);
            return;
        }
        // Refilled only once the previous burst has run out, so the hit rhythm is the same one a
        // fixed barrage has - this adds length, not speed.
        if (barrageHitsRemaining <= 0 && pendingStrike == null) {
            barrageHitsRemaining = BARRAGE_HITS;
            barrageHitInterval = BARRAGE_HIT_INTERVAL;
            this.triggerAnim(CONTROLLER, "barrage");
        }
        // The stance would otherwise lapse partway through and let the Stand drift home mid-flurry.
        stationaryBarrageTicks = Math.max(stationaryBarrageTicks, BARRAGE_HIT_INTERVAL * 2);
    }

    /** Keeps the hold honest, and drops it if the thing in it stopped being holdable. */
    private void tickGrip() {
        if (grip == null) {
            return;
        }
        if (!grip.stillHeld(this)) {
            // Died, despawned, or was pulled off. Nothing to restore - either it is gone, or
            // whatever removed it from the vehicle owns its state now.
            grip = null;
            return;
        }
        if (--gripTicksRemaining <= 0) {
            // Set down rather than thrown: the grip ran out, which is not a decision to do anything
            // with what was in it.
            releaseHeld(Vec3.ZERO);
            return;
        }
        grip.tick(this);
        // Something in your fist is a fight, so the Stand keeps its combat position rather than
        // wandering back to its idle spot while still holding a cow.
        combatHoldTicks = COMBAT_HOLD_TICKS;
    }

    /** Forgets the follow's momentum and any barrage displacement - for when something else takes over. */
    private void clearFollowMotion() {
        followVelocity = Vec3.ZERO;
        barrageSway = Vec3.ZERO;
        swayStrength = 0F;
        combatHoldTicks = 0;
        setHeldBarrage(false);
    }

    private void springTo(Vec3 target, double stiffness, double damping) {
        Vec3 toTarget = target.subtract(this.position());
        Vec3 acceleration = toTarget.scale(stiffness).subtract(followVelocity.scale(damping));
        followVelocity = followVelocity.add(acceleration);
        this.setPos(this.position().add(followVelocity));
    }

    /** Plays a punch (or its chained follow-up) and moves the Stand in front for the swing. */
    /**
     * Queues a punch, thrown once the Stand has moved in front of its user.
     *
     * <p>The stance is taken immediately - that is what sends it forward - but the animation waits.
     * See {@link #STRIKE_ARRIVAL_DISTANCE}.
     */
    public void punch(boolean usePunch2) {
        takeAttackStance();
        queueStrike(usePunch2 ? PendingStrike.PUNCH2 : PendingStrike.PUNCH);
    }

    private void queueStrike(PendingStrike strike) {
        takeAttackStance();
        pendingStrike = strike;
        pendingStrikeTicks = STRIKE_WAIT_TIMEOUT_TICKS;
    }

    /**
     * Releases a held strike once the Stand is in position, or when it has waited long enough.
     *
     * <p>Distance is measured against the anchor the follow is already heading for rather than
     * against the owner, so "in position" means the same thing here as it does everywhere else.
     */
    private void tickPendingStrike(Player owner) {
        if (pendingStrike == null) {
            return;
        }

        boolean arrived = this.position().distanceTo(followAnchor(owner)) <= STRIKE_ARRIVAL_DISTANCE;
        if (!arrived && --pendingStrikeTicks > 0) {
            return;
        }

        switch (pendingStrike) {
            case PUNCH -> this.triggerAnim(CONTROLLER, "punch");
            case PUNCH2 -> this.triggerAnim(CONTROLLER, "punch2");
            case BARRAGE -> releaseCommandedBarrage();
        }

        pendingStrike = null;
    }

    private void releaseCommandedBarrage() {
        barrageHitsRemaining = BARRAGE_HITS;
        barrageHitInterval = BARRAGE_HIT_INTERVAL;
        this.triggerAnim(CONTROLLER, "barrage");
    }


    /** What a queued strike will turn into once the Stand is in position. */
    private enum PendingStrike {
        PUNCH,
        PUNCH2,
        BARRAGE
    }

    /** Starts the (looping) block stance and moves the Stand in front. */
    public void startGuarding() {
        this.guarding = true;
        this.triggerAnim(CONTROLLER, "block");
    }

    /**
     * How strained the guard is, 0 to 1 - the combat handler's hit count, as a fraction.
     *
     * <p>Pushed in rather than counted here, so the number the overlay draws is the same number the
     * break actually fires on. Two counters that agree today is how they end up disagreeing later.
     */
    public void setGuardStrain(float strain) {
        this.entityData.set(DATA_GUARD_STRAIN, Mth.clamp(strain, 0F, 1F));
    }

    /** How close the guard is to giving way, 0 to 1. Read client-side by the crack overlay. */
    public float guardStrain() {
        return this.entityData.get(DATA_GUARD_STRAIN);
    }

    /** Ticks left showing the shattered overlay, counting down from the break. */
    public int guardBrokenTicks() {
        return this.entityData.get(DATA_GUARD_BROKEN);
    }

    /** How long that window is, so the client can turn the countdown back into a progress. */
    public static int guardBrokenFlashTicks() {
        return GUARD_BROKEN_FLASH_TICKS;
    }

    /** Ends the block stance normally (not a break) and returns to idle/behind-right. */
    public void stopGuarding() {
        this.guarding = false;
        this.guardThreat = null;
        this.guardThreatTicks = 0;
        this.entityData.set(DATA_GUARD_STRAIN, 0F);
        this.stopTriggeredAnim(CONTROLLER, "block");
    }

    /** The guard has taken too many hits - plays guardbroken and forces blocking off. */
    public void triggerGuardBroken() {
        this.guarding = false;
        this.entityData.set(DATA_GUARD_STRAIN, 0F);
        this.entityData.set(DATA_GUARD_BROKEN, GUARD_BROKEN_FLASH_TICKS);
        this.triggerAnim(CONTROLLER, "guardbroken");
        playGuardBreak();
    }

    /**
     * The break, seen and heard: a sheet of cracks at the guard and the pieces of it thrown out.
     *
     * <p>Thrown from the Stand rather than from its user, because the Stand is where the guard was
     * - a break that went off inside the player's own body would say nothing about what failed. The
     * height puts it at the Stand's chest, which is where the stance holds.
     *
     * <p>The audio is two layers on purpose. The mod's own cue leads and carries the identity; a
     * pitched-down shield break sits under it, which is where the weight comes from - a single
     * short clip on its own reads as a UI blip rather than as something giving way. Both are played
     * from the Stand for the same reason the particles are, so the moment has a direction.
     */
    private void playGuardBreak() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        Vec3 at = this.position().add(0, this.getBbHeight() * GUARD_BREAK_HEIGHT, 0);

        // Exactly still, which is what marks this one out as the pane.
        serverLevel.sendParticles(ModRegistries.GUARD_BREAK.get(), at.x, at.y, at.z, 0,
                0.0, 0.0, 0.0, 0.0);

        for (int i = 0; i < GUARD_BREAK_SHARDS; i++) {
            // A direction on the sphere, tipped upward: pieces of something that just gave way go
            // out and up before gravity takes them, rather than straight down.
            double yaw = this.random.nextDouble() * Math.PI * 2;
            double lift = 0.15 + this.random.nextDouble() * 0.85;
            double flat = Math.sqrt(Math.max(0, 1 - lift * lift));
            double speed = GUARD_BREAK_SHARD_SPEED + this.random.nextDouble() * GUARD_BREAK_SHARD_VARIANCE;

            double vx = Math.cos(yaw) * flat * speed;
            double vy = lift * speed;
            double vz = Math.sin(yaw) * flat * speed;

            double ox = (this.random.nextDouble() - 0.5) * GUARD_BREAK_SCATTER;
            double oy = (this.random.nextDouble() - 0.5) * GUARD_BREAK_SCATTER;
            double oz = (this.random.nextDouble() - 0.5) * GUARD_BREAK_SCATTER;

            serverLevel.sendParticles(ModRegistries.GUARD_BREAK.get(),
                    at.x + ox, at.y + oy, at.z + oz, 0, vx, vy, vz, 1.0);
        }

        serverLevel.playSound(null, this.blockPosition(), ModSounds.GUARD_BREAK.get(),
                SoundSource.PLAYERS, 1.1F, 1.0F);
        serverLevel.playSound(null, this.blockPosition(), SoundEvents.SHIELD_BREAK,
                SoundSource.PLAYERS, 0.9F, 0.7F);
    }

    /** Called once the guard-break lockout expires, to return to idle. */
    public void recoverFromGuardBreak() {
        this.stopTriggeredAnim(CONTROLLER, "guardbroken");
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // .receiveTriggeredAnimations() IS required here, but only alongside a trigger-aware
        // state handler (below). The naive combination is what bit us originally: with it
        // enabled, GeckoLib consults the handler every frame *including* mid-trigger, so a
        // handler that unconditionally calls setAndContinue(IDLE) overwrites "spawn" the instant
        // it starts. Leaving it off isn't the answer either, though - spawn, punch, punch2 and
        // guardbroken are all authored "hold_on_last_frame", and that LoopType parks the
        // controller in State.PAUSED rather than State.STOPPED. Since GeckoLib's
        // hasAnimationFinished() only reports true for STOPPED, such a trigger is never
        // considered finished and permanently locks the handler out - the Stand keeps holding
        // spawn's final frame forever. That went unnoticed for a long time purely because
        // spawn's last frame matches idle's first, so it read as a normal idle; it only became
        // obvious once PARTIAL needed a *different* idle and never got to switch to it.
        //
        // So: the handler below returns CONTINUE untouched while a trigger is genuinely playing,
        // and STOP once that trigger has parked on its last frame. Returning STOP is what lets
        // handleAnimationState() fall through, clear the trigger, and immediately re-consult the
        // handler - which then picks the correct idle for the current Trust Tier.
        //
        // transitionLength is 0 (no cross-fade): the root bone's own spawn scale keyframes
        // already animate 0 -> [0.6,1.4,0.6] -> 1 (small to big, by design). A nonzero
        // transition blends FROM the bind pose (scale 1) INTO spawn's own t=0 keyframe (scale
        // 0) before that authored growth even starts, producing a big -> small -> big flicker.
        // The spawn -> idle handoff loses nothing either: every bone's end pose in spawn already
        // exactly matches its start pose in idle, so a hard cut there looks identical to a blend.
        controllers.add(new AnimationController<>(this, CONTROLLER, 0, state -> {
                    AnimationController<StandEntity> controller = state.getController();
                    if (controller.getTriggeredAnimation() != null) {
                        return controller.getAnimationState() == AnimationController.State.PAUSED
                                ? PlayState.STOP
                                : PlayState.CONTINUE;
                    }
                    return state.setAndContinue(getTrustTier().isPartialManifestation() ? IDLE_PARTIAL : IDLE);
                }).receiveTriggeredAnimations()
                .triggerableAnim("spawn", SPAWN)
                .triggerableAnim("punch", PUNCH)
                .triggerableAnim("punch2", PUNCH2)
                .triggerableAnim("barrage", BARRAGE)
                .triggerableAnim("star_finger", STAR_FINGER)
                .triggerableAnim("inhale", INHALE)
                .triggerableAnim("timestop", TIMESTOP)
                .triggerableAnim("grab", GRAB)
                .triggerableAnim("block", BLOCK)
                .triggerableAnim("guardbroken", GUARDBROKEN));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    @Override
    public double getTick(Object animatable) {
        return this.tickCount;
    }

    @Override
    public Iterable<ItemStack> getArmorSlots() {
        return List.of();
    }

    /**
     * The main hand answers with whatever the Stand is holding; every other slot is empty.
     *
     * <p>Routed through the vanilla slot rather than left to a getter of its own so that anything
     * which asks an entity what it is holding - the render layer included - gets a true answer
     * without having to know this is a Stand.
     */
    @Override
    public ItemStack getItemBySlot(EquipmentSlot slot) {
        return slot == EquipmentSlot.MAINHAND ? this.entityData.get(DATA_HELD_ITEM) : ItemStack.EMPTY;
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
    }

    @Override
    public HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }

    /** No nameplate, ever - not even on close hover. */
    @Override
    public boolean shouldShowName() {
        return false;
    }

    /**
     * Invisible to the crosshair. {@code GameRenderer.pick} filters candidates on
     * {@code isPickable()}, so leaving this at LivingEntity's default made the Stand eat its own
     * user's attacks: the raycast would stop on the Stand, and the "don't let a player hit their
     * own Stand" guard in {@code Jojoha.init()} would then cancel the swing outright. Harmless
     * while the Stand floats behind the user, but at {@link TrustTier#PARTIAL} it's pinned
     * directly onto them, so it blocked every single M1. A Stand is a manifestation of its user's
     * will rather than a thing in the world to be clicked, so nothing should ever target it.
     */
    /**
     * Nothing physical moves a Stand, and a Stand moves nothing physical.
     *
     * <p>It already passes through blocks - noPhysics is set in the constructor - and these are the
     * same statement about entities. Without them a boat treated it as cargo to be shoved: the hull
     * pushes whatever is inside it outward every tick, the Stand is pinned to its user and pushed
     * straight back, and the two fight until something gives. What the testers saw was Star Platinum
     * wedged in a boat and left behind, still answering its keys and still guarding, because being
     * stuck somewhere does not stop it being yours.
     *
     * <p>Minecarts were fine for exactly this reason - they do not push what they touch - which is
     * what pointed at pushing rather than at collision in general.
     */
    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
    }
}
