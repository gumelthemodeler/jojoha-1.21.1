package org.gumel.jojoha.item;

import dev.architectury.event.events.common.TickEvent;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.data.StandData;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.network.packet.StandRitualEffectPacket;
import org.gumel.jojoha.registry.ModItems;
import org.gumel.jojoha.registry.ModRegistries;
import org.gumel.jojoha.registry.ModSounds;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.StandSummonHandler;
import org.gumel.jojoha.stand.StandTypes;

/**
 * The Stand Arrow's awakening, sequenced over time rather than resolved in a single frame.
 *
 * <p>Using the arrow starts the stab animation; the Stand itself isn't granted until the moment
 * the arrow actually goes in, which is when the awakening rays erupt. Landing the payoff on the
 * strike rather than on the click is the whole point - a Stand arriving before the arrow has
 * moved reads as an inventory transaction instead of a moment.
 */
public final class StandArrowRitual {
    /** What a piece that burns out without waking anything costs its user, in half-hearts. */
    private static final float REJECTION_DAMAGE = 4.0F;

    /**
     * The ritual runs in three beats: the stab plays out, a held pause sits on it, and only then
     * does the Stand tear its way out.
     *
     * <p>The pause is the point. Landing the awakening immediately on the end of the stab makes it
     * read as one continuous flourish; letting it hang for half a second first makes the eruption
     * feel like a reaction to the arrow rather than part of the same motion.
     */
    private static final int STAB_DURATION_TICKS = 93;
    private static final int AWAKEN_DELAY_TICKS = 12;
    /** Where in the ritual the body erupts - rays, white-out, camera shake, recoil animation. */
    private static final int AWAKEN_TICK = STAB_DURATION_TICKS + AWAKEN_DELAY_TICKS;

    /**
     * Beat of silence after the eruption before the Stand is named. Shared with the client so the
     * reveal line starts typing exactly when the server expects it to.
     */
    public static final int REVEAL_PAUSE_TICKS = 26;

    /**
     * How long the rays burn and the body stays white after the eruption, and how long the white
     * then takes to bleed off. Shared with the client renderer ({@code StandAwakeningRays}) so the
     * glow it draws and the beats the server schedules are working from one clock.
     */
    public static final int AWAKEN_RAYS_TICKS = 43;
    public static final int AWAKEN_GLOW_FADE_TICKS = 14;

    /**
     * The climax, measured from the eruption: "Your stand is..." types while the body is still
     * lit, the white bleeds away, and the moment it's gone the Stand bursts into being - particles,
     * name, combat bar. Ordering it this way means the reveal isn't competing with the glow for
     * attention, and the burst lands in the quiet the fade leaves behind.
     */
    public static final int BOOM_AFTER_AWAKEN_TICKS = AWAKEN_RAYS_TICKS + AWAKEN_GLOW_FADE_TICKS;

    /** Public so the client can size the arrow's glow to the exact life of the ritual. */
    public static final int RITUAL_DURATION_TICKS = AWAKEN_TICK + BOOM_AFTER_AWAKEN_TICKS;

    /**
     * Tick within the stab where the arrow drives home and the wince lands.
     *
     * <p>Read off the animation rather than guessed. In {@code stab2} the right arm snaps from
     * +102 to -179 degrees on tick 43 and drives 8 units forward on the same frame, with the torso
     * folding to 20 degrees alongside it - a single-frame impact, not a curve to sit in the middle
     * of. It then <em>holds</em> that pose all the way to tick 70 with the arrow buried, so 43 is
     * both the hit and the last moment before the wound is just being endured.
     */
    private static final int STRIKE_TICK = 43;

    /**
     * How long the opening line waits before it starts typing.
     *
     * <p>Starting it on the click meant it finished well before the arrow moved, leaving the line
     * sitting there through the rest of the stab. Holding it a beat instead lets it type across the
     * wind-up and finish just as the arrow drives in, so the words build toward the impact rather
     * than idling in front of it. Shared with the client, which owns the actual typing.
     */
    public static final int RESONATE_TEXT_DELAY_TICKS = 14;

    /**
     * How long the awakening lifts the player off the ground, and how much of that is the rise.
     *
     * <p>Runs the full length of the glow so they touch back down on the tick the Stand bursts
     * out - the body drops back to earth exactly as the thing that pulled it up arrives. The lift
     * is short and the rest is a hover: a player who keeps climbing for three seconds ends up on
     * the roof, whereas one who rises a metre and hangs there reads as held up by the awakening.
     */
    private static final int FLOAT_DURATION_TICKS = BOOM_AFTER_AWAKEN_TICKS;
    private static final int FLOAT_RISE_TICKS = 14;
    private static final double FLOAT_RISE_SPEED = 0.06;

    /**
     * How long to keep watching for the player to touch down after the levitation lets go.
     *
     * <p>The drop is under a block and takes a handful of ticks, but the ground can move out from
     * under them - stepping off a ledge as they're released, or landing in water. The cap is what
     * stops the watch from following them for the rest of the session and firing a landing crouch
     * minutes later at the bottom of some unrelated fall.
     */
    private static final int LAND_WATCH_TICKS = 40;


    private StandArrowRitual() {
    }

    public static void init() {
        TickEvent.PLAYER_POST.register(StandArrowRitual::onPlayerTick);
    }

    /**
     * Kicks off the ritual. The Stand isn't granted until the stab finishes - see {@link #awaken}.
     *
     * <p>There is only the one stab, swung with the right arm; the item refuses off-hand use
     * (see {@code StandArrowItem}) so the animation can never disagree with the hand holding it.
     */
    public static void begin(ServerPlayer player, JojohaPlayerData data) {
        data.standArrowRitualTicks = RITUAL_DURATION_TICKS;
        PlayerDataAccess.set(player, data);

        NetworkHandler.broadcastRitualEffect(player, StandRitualEffectPacket.Effect.STAB);

        // The narration is client-side (StandRitualText) rather than an action-bar message, so it
        // can type itself out and carry its own glow.
    }

    private static void onPlayerTick(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        JojohaPlayerData data = PlayerDataAccess.get(serverPlayer);

        // Checked ahead of the ritual's own guard: the watch outlives the ritual by design, since
        // the player is still in the air on the tick the sequence ends.
        if (data.standArrowLandWatchTicks > 0) {
            tickLandWatch(serverPlayer, data);
        }

        // Likewise, and for the same reason: the swap begins on the tick the ritual ends and runs
        // on past it.
        if (data.standSkinSwapTicks > 0) {
            tickSkinSwap(serverPlayer, data);
        }

        if (data.standArrowRitualTicks <= 0) {
            return;
        }

        data.standArrowRitualTicks--;

        // Counted down, so a checkpoint "N ticks into the ritual" is this many ticks from the end.
        int remaining = data.standArrowRitualTicks;

        // Energy spiralling off the player for the whole back half of the ritual - from the
        // moment the arrow bites, building through the eruption and the naming.
        if (remaining <= RITUAL_DURATION_TICKS - STRIKE_TICK) {
            spawnTransformSpiral(serverPlayer, remaining);
        }

        if (remaining == RITUAL_DURATION_TICKS - STRIKE_TICK) {
            serverPlayer.serverLevel().playSound(null, serverPlayer.blockPosition(), SoundEvents.PLAYER_HURT,
                    SoundSource.PLAYERS, 0.7F, 0.7F);
        }

        // A skin arrow needs something to work on, and it needs it to be solid before the white
        // hits. Called out on the strike - the moment the arrow drives in - which leaves it a full
        // three seconds to fade in and finish its spawn animation before the eruption. Summoning it
        // any later would have it materialising and burning white at the same time, which reads as
        // a Stand arriving already broken rather than as one being taken apart.
        if (data.standArrowRitualSkin && remaining == RITUAL_DURATION_TICKS - STRIKE_TICK) {
            StandSummonHandler.forceSummon(serverPlayer, data);
            PlayerDataAccess.sync(serverPlayer);
        }

        // The eruption. Nothing is granted here - this is spectacle only, and the Stand itself
        // doesn't arrive until the reveal line has finished naming it.
        // The Stand goes white on the same tick its user does. Both are the same event - the arrow
        // reaching whatever it is that answers it - so they burn together and come apart together,
        // rather than the Stand waiting its turn.
        if (data.standArrowRitualSkin && remaining == RITUAL_DURATION_TICKS - AWAKEN_TICK) {
            StandEntity burning = StandSummonHandler.findStand(serverPlayer, data);
            if (burning != null) {
                burning.beginSkinSwap();
            }
        }

        // Shed for the whole lit stretch rather than only at the end, so the Stand is visibly
        // coming apart the entire time it is white instead of standing there and then bursting.
        if (data.standArrowRitualSkin && remaining < RITUAL_DURATION_TICKS - AWAKEN_TICK) {
            StandEntity shedding = StandSummonHandler.findStand(serverPlayer, data);
            if (shedding != null) {
                spawnSkinMotes(serverPlayer.serverLevel(), shedding,
                        (RITUAL_DURATION_TICKS - AWAKEN_TICK) - remaining);
            }
        }

        if (remaining == RITUAL_DURATION_TICKS - AWAKEN_TICK) {
            NetworkHandler.broadcastRitualEffect(serverPlayer,
                    data.standArrowRitualSkin
                            ? StandRitualEffectPacket.Effect.SKIN_RAYS
                            : StandRitualEffectPacket.Effect.AWAKENING_RAYS);
            // Runs underneath the whole glowing stretch rather than punctuating it, so it starts
            // on the same tick the body whites out and the rays begin.
            serverPlayer.serverLevel().playSound(null, serverPlayer.blockPosition(), ModSounds.STAND_AWAKEN.get(),
                    SoundSource.PLAYERS, 1.0F, 1.0F);
        }

        // The lift, held across the whole glowing stretch and released as the Stand arrives.
        int elapsed = RITUAL_DURATION_TICKS - remaining;
        if (elapsed == AWAKEN_TICK) {
            beginFloat(serverPlayer);
        } else if (elapsed > AWAKEN_TICK && elapsed <= AWAKEN_TICK + FLOAT_DURATION_TICKS) {
            tickFloat(serverPlayer, elapsed - AWAKEN_TICK);
        }

        if (remaining == 0) {
            endFloat(serverPlayer, data);
            if (data.standArrowRitualSkin) {
                beginSkinSwap(serverPlayer, data);
            } else {
                awaken(serverPlayer, data);
            }
        }

        PlayerDataAccess.set(serverPlayer, data);
    }

    /**
     * Lifts the player off the ground for the awakening.
     *
     * <p>Gravity is switched off server-side rather than fought with an upward shove. Momentum
     * alone can't do this: the client only applies 0.98 drag to vertical motion when gravity is
     * off, so a push big enough to lift them at all carries them tens of blocks before it decays.
     * Cutting gravity and then dictating the vertical speed outright is what turns it into a
     * controlled rise-and-hover instead of a launch.
     *
     * <p>{@code setNoGravity} rides the synced entity flags, so the player's own client stops
     * pulling them down too - without that the server and the client would spend the whole
     * sequence disagreeing about where the player is, and the body would judder.
     */
    private static void beginFloat(ServerPlayer player) {
        player.setNoGravity(true);
        player.fallDistance = 0F;
    }

    /**
     * Holds the player at the height the lift put them at.
     *
     * <p>Player movement is client-authoritative, so the vertical speed has to be pushed to them
     * explicitly every tick; setting it only once would leave the client's own physics free to
     * drift it away over the following three seconds.
     */
    private static void tickFloat(ServerPlayer player, int floatTick) {
        double rise = floatTick <= FLOAT_RISE_TICKS ? FLOAT_RISE_SPEED : 0.0;
        Vec3 motion = player.getDeltaMovement();
        Vec3 held = new Vec3(motion.x, rise, motion.z);

        player.setDeltaMovement(held);
        player.connection.send(new ClientboundSetEntityMotionPacket(player.getId(), held));
        player.fallDistance = 0F;
    }

    /**
     * Hands the player back to gravity.
     *
     * <p>The fall counter is cleared as well: the drop back down is under a block, so it would
     * never hurt on its own, but the client has been accumulating fall distance throughout the
     * hover and would otherwise bill them for the whole thing on landing.
     */
    private static void endFloat(ServerPlayer player, JojohaPlayerData data) {
        player.setNoGravity(false);
        player.fallDistance = 0F;
        data.standArrowLandWatchTicks = LAND_WATCH_TICKS;
    }

    /**
     * Plays the landing crouch on the frame the player actually hits the ground.
     *
     * <p>Fired on contact rather than scheduled a fixed number of ticks after the release: the
     * drop is short but its length depends on what is underneath them, and a landing animation
     * that plays while someone is still falling - or well after they've stopped - is worse than
     * none at all.
     */
    private static void tickLandWatch(ServerPlayer player, JojohaPlayerData data) {
        data.standArrowLandWatchTicks--;

        if (player.onGround()) {
            data.standArrowLandWatchTicks = 0;
            NetworkHandler.broadcastRitualEffect(player, StandRitualEffectPacket.Effect.LAND);
        } else if (player.isInWater()) {
            // Caught by water instead of ground. The animation is an impact absorb - knees, braced
            // arms - which reads as nonsense on someone who just splashed down, so the watch is
            // simply dropped.
            data.standArrowLandWatchTicks = 0;
        }

        PlayerDataAccess.set(player, data);
    }

    /**
     * The name has landed: the Stand arrives, or it does not.
     *
     * <p>The roll is here rather than at the stab, and that is deliberate - the whole ritual plays
     * out either way, so a shard that fails looks exactly like a shard that was going to work right
     * up until the moment it does not. Deciding at the click and then acting it out would be a
     * cutscene whose ending was already written.
     */
    private static void awaken(ServerPlayer player, JojohaPlayerData data) {
        // Re-checked rather than assumed: the ritual takes over a second, and anything could have
        // granted this player a Stand in the meantime.
        if (data.stand.isPresent()) {
            return;
        }

        StandArrowItem source = sourceItem(data);
        float chance = source.successChance(data.worthiness);

        // Spent whatever happens. A shard is used up by the attempt, not by the success - if it
        // were refunded on a failure there would be no cost to trying and no reason to care about
        // the odds at all.
        consumeArrow(player, data);

        if (chance < 1F && player.serverLevel().getRandom().nextFloat() >= chance) {
            rejected(player, data);
            return;
        }

        // Syncing the Stand is what makes the combat bar portrait appear and fade in, so it lands
        // here with the sound and the particle burst rather than back at the eruption.
        data.stand = new StandData(StandTypes.STAR_PLATINUM_ID, 0, 5, 5, 5, 5, 5, 0);
        data.standArrowRitualShard = false;
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);

        spawnAwakeningEffects(player.serverLevel(), player);
    }

    /**
     * How long the world goes without a Stand in it between the burst and the return, in ticks.
     *
     * <p>A held beat, and the whole reason the swap reads as a swap. Without it the burst and the
     * re-summon land on the same frame and it looks like a texture being changed on a Stand that
     * never left. Public so the client can land the name on the tick the new one actually arrives.
     */
    public static final int SKIN_SWAP_GAP_TICKS = 16;

    /** Motes shed per tick while it burns, and how far out they start. */
    private static final int SKIN_MOTES_PER_TICK = 7;
    private static final double SKIN_MOTE_RADIUS = 0.75;
    private static final double SKIN_MOTE_DRIFT = 0.06;

    /** How much comes off it when it finally goes. */
    private static final int SKIN_BURST_PARTICLES = 110;
    private static final double SKIN_BURST_MIN_SPEED = 0.3;
    private static final double SKIN_BURST_SPEED_SPREAD = 0.5;

    /**
     * Drives the arrow home on a Stand that already exists.
     *
     * <p>Forces the cast first. The whole sequence is something done <em>to</em> the Stand - burned
     * white, broken apart, put back together differently - and none of that can be watched happening
     * to something that is not in the world. A player who used this with their Stand away would
     * otherwise get a spiral of particles and a Stand that quietly looked different next time they
     * summoned it, which is the payoff arriving without the moment.
     */
    /**
     * The Stand comes apart.
     *
     * <p>By this point it has been out since the strike and white since the eruption; all that is
     * left is to break it and count down to what comes back.
     */
    private static void beginSkinSwap(ServerPlayer player, JojohaPlayerData data) {
        consumeSkinArrow(player);
        data.standArrowRitualSkin = false;
        data.standSkinSwapTicks = SKIN_SWAP_GAP_TICKS;

        StandEntity stand = StandSummonHandler.findStand(player, data);
        if (stand != null) {
            spawnSkinBurst(player.serverLevel(), stand);
        }

        // Outright, not faded. It is supposed to have come apart; a Stand dissolving politely over
        // twelve ticks is a withdrawal, and this is not one.
        StandSummonHandler.dismissImmediately(player, data);
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
    }

    /**
     * One tick of the swap: shed, break, return.
     *
     * <p>The Stand is looked up fresh every tick rather than held across them, because for part of
     * this there is deliberately not one.
     */
    private static void tickSkinSwap(ServerPlayer player, JojohaPlayerData data) {
        data.standSkinSwapTicks--;
        if (data.standSkinSwapTicks > 0) {
            return;
        }

        // The roll reads the old skin and the write replaces it, so the order matters: rolled
        // first, assigned second, and only then is the Stand called back wearing it.
        data.stand = data.stand.withSkin(rollSkin(player, data));
        StandSummonHandler.forceSummon(player, data);

        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);

        // Strictly after the sync above, so the client has the new skin in hand before it is asked
        // to say what it is.
        NetworkHandler.broadcastRitualEffect(player, StandRitualEffectPacket.Effect.SKIN_NAMED);
    }

    /**
     * Picks a skin that is not the one it was wearing.
     *
     * <p>Rolling within the remaining set rather than rolling over all of them and retrying: a
     * plain reroll can return the current skin, and an arrow that visibly does nothing is
     * indistinguishable from an arrow that failed. Picking an index in a range one smaller and
     * stepping over the current one gives a uniform choice among the alternatives in one draw.
     */
    private static int rollSkin(ServerPlayer player, JojohaPlayerData data) {
        org.gumel.jojoha.stand.StandType type = StandTypes.byIdOrDefault(data.stand.standId());
        int count = type.skinCount();
        if (count < 2) {
            return data.stand.skin();
        }

        int current = Mth.clamp(data.stand.skin(), 0, count - 1);

        // Weighted, and the current skin is left out of the draw entirely rather than rerolled -
        // an arrow that visibly does nothing is indistinguishable from an arrow that failed.
        //
        // Summed over what is actually eligible rather than over everything, because the weights are
        // relative: dropping one of five from the pool has to raise the other four in proportion, and
        // it does that for free as long as the total is taken after the exclusion rather than before.
        float total = 0F;
        for (int skin = 0; skin < count; skin++) {
            if (skin != current) {
                total += type.skinWeight(skin);
            }
        }

        // Every alternative weighted to nothing. A pack is allowed to say that - it is how you pin a
        // Stand to one look - and the honest answer is to leave the skin alone.
        if (total <= 0F) {
            return current;
        }

        float roll = player.serverLevel().getRandom().nextFloat() * total;
        for (int skin = 0; skin < count; skin++) {
            if (skin == current) {
                continue;
            }
            roll -= type.skinWeight(skin);
            if (roll <= 0F) {
                return skin;
            }
        }

        // Only reachable on floating point drift at the very top of the range.
        return current == count - 1 ? 0 : count - 1;
    }

    /**
     * The blue and pink it sheds while it burns.
     *
     * <p>Thrown inward from a shrinking ring rather than outward from the middle, so the effect
     * reads as the Stand being pulled apart into it rather than as the Stand emitting it. The two
     * colours alternate per mote, which is what keeps the pair reading as one unstable thing
     * instead of two effects playing at once.
     */
    private static void spawnSkinMotes(ServerLevel level, StandEntity stand, int elapsed) {
        RandomSource random = level.getRandom();
        float through = Mth.clamp(elapsed / (float) BOOM_AFTER_AWAKEN_TICKS, 0F, 1F);
        double radius = SKIN_MOTE_RADIUS * (1F - through * 0.6F);

        for (int i = 0; i < SKIN_MOTES_PER_TICK; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double height = random.nextDouble() * stand.getBbHeight();

            level.sendParticles(
                    i % 2 == 0 ? ModRegistries.STAND_AWAKEN_BLUE.get() : ModRegistries.STAND_AWAKEN_PINK.get(),
                    stand.getX() + Math.cos(angle) * radius,
                    stand.getY() + height,
                    stand.getZ() + Math.sin(angle) * radius,
                    1, 0, SKIN_MOTE_DRIFT, 0, 0.02);
        }
    }

    /** Everything it had, at once, in both colours. */
    private static void spawnSkinBurst(ServerLevel level, StandEntity stand) {
        RandomSource random = level.getRandom();
        double centreY = stand.getY() + stand.getBbHeight() * 0.5;

        for (int i = 0; i < SKIN_BURST_PARTICLES; i++) {
            // A direction off the unit sphere rather than three independent offsets, which would
            // pile the burst into the corners of a cube.
            double theta = random.nextDouble() * Math.PI * 2.0;
            double z = random.nextDouble() * 2.0 - 1.0;
            double r = Math.sqrt(1.0 - z * z);
            double speed = SKIN_BURST_MIN_SPEED + random.nextDouble() * SKIN_BURST_SPEED_SPREAD;

            level.sendParticles(
                    i % 2 == 0 ? ModRegistries.STAND_AWAKEN_BLUE.get() : ModRegistries.STAND_AWAKEN_PINK.get(),
                    stand.getX(), centreY, stand.getZ(),
                    0, Math.cos(theta) * r * speed, z * speed, Math.sin(theta) * r * speed, 1);
        }

        level.playSound(null, stand.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.PLAYERS, 0.7F, 1.6F);
    }

    /**
     * Spends the skin arrow.
     *
     * <p>Its own rather than {@link #consumeArrow}, which resolves which item to take from the
     * shard flag and would take a Stand Arrow out of the inventory of somebody who used this.
     */
    private static void consumeSkinArrow(ServerPlayer player) {
        ItemStack inHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (inHand.is(ModItems.FRACTURED_SKIN_ARROW.get())) {
            inHand.shrink(1);
            return;
        }

        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(ModItems.FRACTURED_SKIN_ARROW.get())) {
                stack.shrink(1);
                return;
            }
        }
    }

    /** Which of the two started this, so the odds and the item spent are the ones actually used. */
    private static StandArrowItem sourceItem(JojohaPlayerData data) {
        return (StandArrowItem) (data.standArrowRitualShard
                ? ModItems.STAND_ARROW_SHARD.get()
                : ModItems.STAND_ARROW.get());
    }

    /**
     * The piece burned out without waking anything.
     *
     * <p>Given a sound and a wound rather than nothing at all. A failure that simply ends leaves the
     * player unsure whether the move even fired, and the arrow's own rejection - the one for being
     * unworthy - already reads as the thing refusing you. This is the same refusal arriving later.
     */
    private static void rejected(ServerPlayer player, JojohaPlayerData data) {
        data.standArrowRitualShard = false;
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);

        player.hurt(player.serverLevel().damageSources().magic(), REJECTION_DAMAGE);
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.ITEM_BREAK,
                SoundSource.PLAYERS, 1.0F, 0.7F);
        player.displayClientMessage(
                Component.translatable("message.jojoha.stand_arrow.rejected"), true);
    }

    /**
     * Spends the arrow, now that it has actually done something.
     *
     * <p>Checks the main hand first - the only hand the arrow can be used from - then falls back
     * to anywhere in the inventory: the ritual runs for several seconds, which is more than long
     * enough for the player to have shuffled their hotbar in the meantime. If the arrow is
     * genuinely gone by then they simply got a free Stand - not worth cancelling the whole
     * awakening over.
     */
    private static void consumeArrow(ServerPlayer player, JojohaPlayerData data) {
        net.minecraft.world.item.Item spent = sourceItem(data);

        ItemStack inHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (inHand.is(spent)) {
            inHand.shrink(1);
            return;
        }

        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(spent)) {
                stack.shrink(1);
                return;
            }
        }
    }

    /** How many particles the awakening throws off, and how fast they leave. */
    private static final int BURST_PARTICLES = 140;
    private static final double BURST_MIN_SPEED = 0.35;
    private static final double BURST_SPEED_SPREAD = 0.45;

    /** How many spiral motes are released each tick, and how wide the helix starts. */
    private static final int SPIRAL_MOTES_PER_TICK = 5;
    private static final double SPIRAL_RADIUS = 0.85;

    /**
     * Releases the upward spiral of energy that runs through the transformation.
     *
     * <p>Intensity ramps as the ritual proceeds, so the column thickens toward the awakening
     * rather than pouring out at a flat rate from the first tick. Motes are spread around the
     * circle by a golden-angle step so consecutive ones never stack on the same side.
     */
    private static void spawnTransformSpiral(ServerPlayer player, int remaining) {
        ServerLevel level = player.serverLevel();
        RandomSource random = level.getRandom();

        int elapsed = RITUAL_DURATION_TICKS - remaining;
        float intensity = Mth.clamp(elapsed / (float) RITUAL_DURATION_TICKS, 0.25F, 1F);
        int motes = Math.max(1, Math.round(SPIRAL_MOTES_PER_TICK * intensity));

        for (int i = 0; i < motes; i++) {
            double angle = (elapsed * SPIRAL_MOTES_PER_TICK + i) * 2.39996;
            double radius = SPIRAL_RADIUS * (0.55 + random.nextDouble() * 0.45);

            double x = player.getX() + Math.cos(angle) * radius;
            double y = player.getY() + random.nextDouble() * 0.4;
            double z = player.getZ() + Math.sin(angle) * radius;

            // The particle steers its own helix, so the "velocity" slots carry its starting angle
            // and radius instead - see StandTransformParticle.Provider.
            level.sendParticles(ModRegistries.STAND_TRANSFORM.get(), x, y, z, 0, angle, 0.0, radius, 1.0);
        }
    }

    private static void spawnAwakeningEffects(ServerLevel level, ServerPlayer player) {
        RandomSource random = level.getRandom();
        SimpleParticleType burst = ModRegistries.STAND_AWAKEN.get();
        double originY = player.getY() + player.getBbHeight() * 0.5;

        for (int i = 0; i < BURST_PARTICLES; i++) {
            // Directions drawn from a normalised gaussian rather than from spherical angles:
            // picking a random yaw and pitch clusters points at the poles, which shows up as two
            // dense knots above and below the player instead of an even shell.
            double dx = random.nextGaussian();
            double dy = random.nextGaussian();
            double dz = random.nextGaussian();
            double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (length < 1.0E-4) {
                continue;
            }

            double speed = BURST_MIN_SPEED + random.nextDouble() * BURST_SPEED_SPREAD;
            dx = dx / length * speed;
            dy = dy / length * speed;
            dz = dz / length * speed;

            // count=0 sends exactly one particle at this exact position, with (dx,dy,dz) used
            // directly as its velocity rather than as a random spread radius - which is what lets
            // the shell be aimed here instead of scattered by the client.
            level.sendParticles(burst, player.getX(), originY, player.getZ(), 0, dx, dy, dz, 1.0);
        }

        level.playSound(null, player.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
    }
}
