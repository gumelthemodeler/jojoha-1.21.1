package org.gumel.jojoha.item;

import dev.architectury.event.events.common.TickEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.registry.ModSounds;
import org.gumel.jojoha.data.VampireStage;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.network.packet.StandRitualEffectPacket;
import org.gumel.jojoha.registry.ModItems;

/**
 * Putting the mask on, and what it does once it is on.
 *
 * <p>Sequenced against the animation rather than run all at once, because the animation is the
 * event: {@code equip_mask} raises the mask, turns it over and seats it, and the mask has to leave
 * the hand at the moment the animation puts it on the face and not a tick earlier or later. The
 * timings below are read off that file - see {@link #SEAT_TICKS}.
 *
 * <p>The hand is emptied by <em>consuming the item</em> rather than by hiding it in the renderer.
 * That is the whole trick of the hand-off: the stack really is gone, so every client stops drawing
 * it for free, in every hand slot and every perspective, with no renderer aware that anything
 * happened. All that is left for the client to do is start drawing the mask on the face.
 */
public final class StoneMaskRitual {
    /**
     * When the animation seats the mask, in ticks.
     *
     * <p>1.25 seconds - tick 25 - which is where {@code equip_mask}'s Head2 track finishes moving.
     * The mask is simply on from that instant; nothing is drawn approaching the face.
     *
     * <p>Two attempts at animating that approach were made and both reverted. Whatever is drawn
     * between the hand and the face is a second version of a movement the animation is already
     * performing with the arm, and two descriptions of one motion cannot help but disagree - the
     * result read as the mask sliding independently of the hand that was supposed to be holding it.
     * Landing it on the keyframe and letting the arm do the carrying is the version where there is
     * only one account of what happened.
     */
    private static final int SEAT_TICKS = 25;

    /** The full length of {@code equip_mask}, 1.7917s, after which the transformation begins. */
    private static final int EQUIP_TICKS = 36;

    /**
     * A beat between the mask seating and the thing waking up.
     *
     * <p>Nothing happens here on purpose. Running the two together made the whole sequence one
     * continuous event that was over before it registered - the mask went on and the blood came up
     * in the same breath. The pause is what turns it into two: it is on, and then, a moment later,
     * it does something.
     */
    private static final int DWELL_TICKS = 50;

    /**
     * How long the stone spends turning red before anything else happens.
     *
     * <p>Its own beat, and the reason is order. The mask reddening and the wearer being taken were
     * one event, so the cause and the effect landed together and the mask looked like it was
     * reacting to the transformation rather than causing it. Separated, the stone goes over first
     * and the world answers - which is the way round it actually happens.
     *
     * <p>Long enough for the eyes to finish. {@code StoneMaskState.TURN_TICKS} is how long they take
     * to come up, and the awakening starting while they were still kindling would step on the one
     * thing this beat exists to show.
     */
    private static final int TURN_TICKS = 45;

    /**
     * How long the awakening runs before the ritual lets go.
     *
     * <p>Well past the animation, because the transformation is the payoff and it was going by too
     * quickly to watch. The rays and the white-out run their own courses inside this, and so does
     * the narration - four lines that now take most of it to read, which is the length this is
     * really set by.
     */
    private static final int TRANSFORM_TICKS = 150;

    private StoneMaskRitual() {
    }

    public static void init() {
        TickEvent.PLAYER_POST.register(player -> {
            if (player instanceof ServerPlayer serverPlayer) {
                tick(serverPlayer);
            }
        });
    }

    /** Called from the item. The mask is not consumed here - see SEAT_TICKS. */
    public static void begin(ServerPlayer player, JojohaPlayerData data) {
        data.stoneMaskRitualTicks = EQUIP_TICKS + DWELL_TICKS + TURN_TICKS + TRANSFORM_TICKS;
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);

        NetworkHandler.broadcastRitualEffect(player, StandRitualEffectPacket.Effect.MASK_EQUIP);
    }

    private static void tick(ServerPlayer player) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (data.stoneMaskRitualTicks <= 0) {
            return;
        }

        int remaining = --data.stoneMaskRitualTicks;
        int elapsed = (EQUIP_TICKS + DWELL_TICKS + TURN_TICKS + TRANSFORM_TICKS) - remaining;

        // Put it away before it reaches your face and nothing happens. The window closes at the
        // seat because that is when the mask stops being something you are holding - after that it
        // is on you, and there is no longer a hand to take it out of.
        if (elapsed < SEAT_TICKS && !holdingMask(player)) {
            cancel(player, data);
            return;
        }

        if (elapsed == SEAT_TICKS) {
            seat(player, data);
        } else if (elapsed == EQUIP_TICKS + DWELL_TICKS) {
            turn(player);
        } else if (elapsed == EQUIP_TICKS + DWELL_TICKS + TURN_TICKS) {
            transform(player, data);
        } else if (remaining <= 0) {
            finish(player, data);
        } else if (elapsed > EQUIP_TICKS + DWELL_TICKS + TURN_TICKS) {
            spawnBloodMotes(player.serverLevel(), player);
        }

        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
    }

    /** The mask reaches the face: it leaves the hand, and every client starts drawing it there. */
    private static void seat(ServerPlayer player, JojohaPlayerData data) {
        consumeMask(player);
        data.stoneMaskWorn = true;

        seatSound(player.serverLevel(), player);
        NetworkHandler.broadcastRitualEffect(player, StandRitualEffectPacket.Effect.MASK_SEATED);
    }

    /**
     * What it sounds like when the mask goes on.
     *
     * <p>One recording now. This used to be three borrowed vanilla sounds - a sword sweep, a bone
     * block breaking and stone being placed - stacked and pitched far down, because nothing in the
     * game is the sound of spines going through the back of a skull and the parts of it existed
     * separately. With a real clip for it, the impersonation is not needed.
     */
    private static void seatSound(ServerLevel level, ServerPlayer player) {
        level.playSound(null, player.blockPosition(), ModSounds.STONEMASK_STAB.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    /**
     * The stone goes over.
     *
     * <p>Only the mask changes here - no stage is set, no rays, no burst. It is the quiet half of
     * the pair, and it has to happen alone or it cannot be seen to precede anything.
     */
    private static void turn(ServerPlayer player) {
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.STONE_PLACE,
                SoundSource.PLAYERS, 1.0F, 0.4F);

        NetworkHandler.broadcastRitualEffect(player, StandRitualEffectPacket.Effect.MASK_TURNING);
    }

    /**
     * The mask wakes, and takes its wearer with it.
     *
     * <p>The stage is set here rather than at the seat, so the moment the blood runs and the moment
     * the player becomes something else are the same moment - which is also the moment the mask's
     * own texture turns, since that is read from the stage.
     */
    private static void transform(ServerPlayer player, JojohaPlayerData data) {
        data.vampireStage = VampireStage.VAMPIRE;

        ServerLevel level = player.serverLevel();
        level.playSound(null, player.blockPosition(), SoundEvents.WITHER_SPAWN,
                SoundSource.PLAYERS, 0.7F, 1.4F);

        spawnBloodBurst(level, player);
        NetworkHandler.broadcastRitualEffect(player, StandRitualEffectPacket.Effect.MASK_AWAKENING);
    }

    /** Whether the mask is still in the hand that was raising it. */
    private static boolean holdingMask(ServerPlayer player) {
        return player.getItemInHand(InteractionHand.MAIN_HAND).is(ModItems.STONE_MASK.get());
    }

    /**
     * Called off before it began.
     *
     * <p>Nothing is consumed and nothing is granted - the mask is still in the inventory somewhere,
     * because the only way to get here is by having taken it out of your hand. The animation is
     * stopped on every client that started it rather than left to run itself out, or the player
     * would keep miming a mask they are no longer holding.
     */
    private static void cancel(ServerPlayer player, JojohaPlayerData data) {
        data.stoneMaskRitualTicks = 0;
        data.stoneMaskWorn = false;
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);

        NetworkHandler.broadcastRitualEffect(player, StandRitualEffectPacket.Effect.MASK_CANCELLED);
    }

    /**
     * The mask has done its work and comes off.
     *
     * <p>It is not kept, and it is not given back. The mask is a key rather than a helmet - what it
     * unlocked is now in the wearer, and leaving it welded to their face would mean every vampire in
     * the world wore the same expression for ever. The stage it left behind is the permanent part.
     */
    private static void finish(ServerPlayer player, JojohaPlayerData data) {
        data.stoneMaskWorn = false;

        ServerLevel level = player.serverLevel();

        // Thrown clear rather than dropped straight down: it bursts off the face, which is why it
        // gets the player's own facing plus a lift. Falling on the spot would look like the mask
        // had simply been let go of.
        Vec3 look = player.getLookAngle();
        Vec3 burst = new Vec3(look.x, 0, look.z).normalize()
                .scale(BURST_FORWARD).add(0, BURST_LIFT, 0);

        FallingMask falling = new FallingMask(level,
                player.getX(), player.getEyeY() - 0.15, player.getZ(), burst);
        level.addFreshEntity(falling);

        level.playSound(null, player.blockPosition(), SoundEvents.STONE_PLACE,
                SoundSource.PLAYERS, 0.8F, 0.5F);

        NetworkHandler.broadcastRitualEffect(player, StandRitualEffectPacket.Effect.MASK_SPENT);
    }

    /** How hard it comes off the face, forward and up. It breaks where it lands - see FallingMask. */
    private static final double BURST_FORWARD = 0.24;
    private static final double BURST_LIFT = 0.28;

    /** The burst that goes with the turn, in the colour of the thing it is doing. */
    private static void spawnBloodBurst(ServerLevel level, ServerPlayer player) {
        double originY = player.getY() + player.getBbHeight() * 0.6;
        var random = level.getRandom();

        for (int i = 0; i < BURST_PARTICLES; i++) {
            // Gaussian rather than spherical angles, for the reason spelled out in StandArrowRitual:
            // yaw-and-pitch clusters everything at the poles.
            double dx = random.nextGaussian();
            double dy = random.nextGaussian();
            double dz = random.nextGaussian();
            double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (length < 1.0E-4) {
                continue;
            }

            double speed = BURST_MIN_SPEED + random.nextDouble() * BURST_SPEED_SPREAD;
            level.sendParticles(org.gumel.jojoha.registry.ModRegistries.STAND_AWAKEN_RED.get(),
                    player.getX(), originY, player.getZ(), 0,
                    dx / length * speed, dy / length * speed, dz / length * speed, 1.0);
        }
    }

    /**
     * The motes that rise while the turn is happening.
     *
     * <p>Spread across the whole transformation rather than thrown at the start of it: the burst
     * marks the instant, and these are what fill the seconds after it so the long window has
     * something happening in it rather than being a wait.
     */
    private static void spawnBloodMotes(ServerLevel level, ServerPlayer player) {
        var random = level.getRandom();
        for (int i = 0; i < MOTES_PER_TICK; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double radius = MOTE_RADIUS * (0.4 + random.nextDouble() * 0.6);
            level.sendParticles(org.gumel.jojoha.registry.ModRegistries.BLOOD_MOTE.get(),
                    player.getX() + Math.cos(angle) * radius,
                    player.getY() + random.nextDouble() * player.getBbHeight(),
                    player.getZ() + Math.sin(angle) * radius,
                    1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    private static final int MOTES_PER_TICK = 2;
    private static final double MOTE_RADIUS = 1.1;

    private static final int BURST_PARTICLES = 140;
    private static final double BURST_MIN_SPEED = 0.35;
    private static final double BURST_SPEED_SPREAD = 0.55;

    /**
     * Takes the mask out of the world.
     *
     * <p>Main hand first, then anywhere else - the ritual runs for over a second and the player can
     * shuffle their hotbar in that time. If it is genuinely gone by then the transformation happens
     * anyway; the mask has already done what it was going to do.
     */
    private static void consumeMask(ServerPlayer player) {
        ItemStack inHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (inHand.is(ModItems.STONE_MASK.get())) {
            inHand.shrink(1);
            return;
        }

        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(ModItems.STONE_MASK.get())) {
                stack.shrink(1);
                return;
            }
        }
    }
}
