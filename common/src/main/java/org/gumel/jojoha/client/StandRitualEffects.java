package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.gumel.jojoha.registry.ModSounds;
import org.gumel.jojoha.data.ClientPlayerDataCache;
import org.gumel.jojoha.data.StandData;
import org.gumel.jojoha.item.StandArrowRitual;
import org.gumel.jojoha.network.packet.StandRitualEffectPacket;
import org.gumel.jojoha.stand.StandTypes;

import java.util.Locale;
import java.util.UUID;

/**
 * Client-side entry point for {@link StandRitualEffectPacket} - routes each ritual beat to the
 * effect that renders it, and ages both out once their windows close.
 *
 * <p>Kept apart from the effects themselves so {@code NetworkHandler} has a single client-side
 * call site rather than reaching into each renderer.
 */
public final class StandRitualEffects {
    /**
     * The colour the mask's rays run in.
     *
     * <p>Red, but not pure red - the beams are additive and a fully saturated primary through an
     * additive pass comes out as a flat wall with no shape in it. Keeping a little green and blue
     * leaves the falloff readable while the burst still reads as blood rather than as light.
     */
    /**
     * The colour the mask speaks in.
     *
     * <p>Pure red, and every one of its lines in it. The first used to borrow the arrow's pink
     * narration colour, which made the sequence look like two different things talking - the ritual
     * in one voice and then the mask in another, halfway through.
     */
    private static final int MASK_TEXT_COLOR = 0xFF0000;

    /**
     * How long the closing line waits behind the one before it.
     *
     * <p>A flat number rather than the previous line's typing time, which was worked out from a copy
     * of its English text held here - so translating the mod, or editing the line, silently moved
     * the pause. Long enough to cover the line at the current speed and to leave a beat after it.
     */
    private static final int MASK_CLOSING_DELAY_TICKS = 70;

    private static final float MASK_RAY_RED = 1.0F;
    private static final float MASK_RAY_GREEN = 0.13F;
    private static final float MASK_RAY_BLUE = 0.09F;

    private StandRitualEffects() {
    }

    public static void handle(StandRitualEffectPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        float now = (float) minecraft.level.getGameTime();
        boolean isLocalPlayer = minecraft.player != null && minecraft.player.getUUID().equals(packet.playerId());

        switch (packet.effect()) {
            case STAB -> {
                PlayerStabAnimation.begin(packet.playerId(), PlayerStabAnimation.STAB_ANIMATION, now);
                if (isLocalPlayer) {
                    StandRitualText.showAfter(Component.translatable("message.jojoha.arrow.resonate"),
                            now, StandArrowRitual.RESONATE_TEXT_DELAY_TICKS, StandRitualText.NARRATION_COLOR);
                }
            }
            // One packet drives the whole climax so the recoil, the rays, the white-out and the
            // camera shake all start on the same frame - splitting them risks them drifting apart.
            case AWAKENING_RAYS -> {
                PlayerStabAnimation.begin(packet.playerId(), PlayerStabAnimation.AWAKEN_ANIMATION, now);
                StandAwakeningRays.begin(packet.playerId(), now);
                playArrowSound(minecraft, packet.playerId());
                if (isLocalPlayer) {
                    announceStandAfterAwakening(now);
                }
            }
            // The same eruption, announcing a skin instead of a Stand.
            case SKIN_RAYS -> {
                PlayerStabAnimation.begin(packet.playerId(), PlayerStabAnimation.AWAKEN_ANIMATION, now);
                StandAwakeningRays.begin(packet.playerId(), now);
                playArrowSound(minecraft, packet.playerId());
                if (isLocalPlayer) {
                    announceSkinAfterAwakening(now);
                }
            }

            // The new Stand is standing there and the client already has its skin. Say it.
            case SKIN_NAMED -> {
                if (isLocalPlayer) {
                    StandRitualText.show(
                            Component.translatable("message.jojoha.arrow.skin_name", standName(), skinName()),
                            now, StandRitualText.STAND_NAME_COLOR);
                }
            }

            case LAND -> PlayerStabAnimation.begin(packet.playerId(), PlayerStabAnimation.LAND_ANIMATION, now);

            // Raising the mask. Nothing else happens here - the mask is still a held item and the
            // arm animation is what carries it, exactly as the arrow is carried during a stab.
            case MASK_EQUIP -> {
                PlayerStabAnimation.begin(packet.playerId(), PlayerStabAnimation.EQUIP_MASK_ANIMATION, now);
                if (isLocalPlayer) {
                    StandRitualText.show(Component.translatable("message.jojoha.mask.raise"),
                            now, MASK_TEXT_COLOR);
                }
            }

            // The hand-off. The server has already taken the item, so all that is left is to start
            // drawing it on the face - see StoneMaskLayer.
            case MASK_SEATED -> {
                StoneMaskState.seat(packet.playerId(), now);
                if (isLocalPlayer) {
                    StandRitualText.show(Component.translatable("message.jojoha.mask.seated"),
                            now, MASK_TEXT_COLOR);
                }
            }

            // It wakes. Same climax the Stand awakening uses - the recoil, the rays and the shake
            // on one frame - but the mask turns and the rays run red.
            // The stone alone. The transformation is a separate packet a second later.
            case MASK_TURNING -> StoneMaskState.activate(packet.playerId(), now);

            case MASK_AWAKENING -> {
                PlayerStabAnimation.begin(packet.playerId(), PlayerStabAnimation.AWAKEN_ANIMATION, now);
                StandAwakeningRays.begin(packet.playerId(), now,
                        MASK_RAY_RED, MASK_RAY_GREEN, MASK_RAY_BLUE);
                if (isLocalPlayer) {
                    VampireColour.begin(now);
                }
                if (isLocalPlayer) {
                    StandRitualText.show(Component.translatable("message.jojoha.mask.turn"),
                            now, MASK_TEXT_COLOR);
                    // Landed after the line above has finished typing, so the two read as one
                    // sentence arriving in two parts rather than overwriting each other.
                    StandRitualText.showAfter(Component.translatable("message.jojoha.mask.become"),
                            now, MASK_CLOSING_DELAY_TICKS, MASK_TEXT_COLOR);
                }
            }

            case MASK_SPENT -> {
                StoneMaskState.remove(packet.playerId());
                if (isLocalPlayer) {
                    // All at once, on the tick the mask lets go. The world rushing back is the
                    // ending; a grade that faded out would be the transition merely stopping.
                    VampireColour.release();
                }
            }

            // Called off. The animation is stopped rather than left to finish, or the player would
            // keep raising a mask that is back in their inventory.
            case MASK_CANCELLED -> {
                PlayerStabAnimation.stop(packet.playerId());
                StoneMaskState.remove(packet.playerId());
                if (isLocalPlayer) {
                    VampireColour.release();
                }
            }
        }
    }

    /**
     * Lays the arrow's own sound under the ritual.
     *
     * <p>Started here rather than server-side with {@code playSound} because it needs to fade in
     * and out, and the engine only re-reads volume for a sound the client owns and ticks itself -
     * see {@link FadingRitualSound}. Played for onlookers too, positioned on whoever used the
     * arrow, so it carries across the world like any other sound rather than being private to the
     * person it is happening to.
     */
    private static void playArrowSound(Minecraft minecraft, UUID playerId) {
        if (minecraft.level == null) {
            return;
        }

        Player user = minecraft.level.getPlayerByUUID(playerId);
        if (user == null) {
            return;
        }

        minecraft.getSoundManager().play(new FadingRitualSound(ModSounds.STAND_ARROW.get(), user,
                ARROW_SOUND_VOLUME, ARROW_SOUND_TICKS, ARROW_SOUND_FADE_IN_TICKS, ARROW_SOUND_FADE_OUT_TICKS));
    }

    /**
     * The arrow clip, held well under the rest of the mix.
     *
     * <p>Started on the eruption rather than on the click, so it lands after the stab is over
     * instead of running underneath it. Length is the clip's own 4.25 seconds rather than a slice
     * of the ritual, so the fade-out sits on the end of the recording instead of chopping it off
     * or leaving silence to fade; that carries it through the glow and out over the landing.
     */
    private static final float ARROW_SOUND_VOLUME = 0.4F;
    private static final int ARROW_SOUND_TICKS = 85;
    private static final int ARROW_SOUND_FADE_IN_TICKS = 14;
    private static final int ARROW_SOUND_FADE_OUT_TICKS = 22;

    /**
     * Schedules the two halves of the reveal around the eruption's fade.
     *
     * <p>The setup line types while the body is still lit, then the white bleeds away, and the
     * name itself only lands once the glow is gone - on the same tick the server bursts the
     * particles and grants the Stand. Splitting it in two is what gives the name somewhere to
     * land: a single line would have to be typing over the top of the fade it's meant to follow.
     */
    private static void announceStandAfterAwakening(float now) {
        StandRitualText.showAfter(Component.translatable("message.jojoha.arrow.stand_revealed"),
                now, StandArrowRitual.REVEAL_PAUSE_TICKS, StandRitualText.NARRATION_COLOR);

        // Supplied lazily: the Stand is granted on the very tick this line is due, so resolving
        // the name now would read the data from before the grant.
        StandRitualText.showAfter(() -> Component.translatable("message.jojoha.arrow.stand_name", standName()),
                now, StandArrowRitual.BOOM_AFTER_AWAKEN_TICKS, StandRitualText.STAND_NAME_COLOR);
    }

    /**
     * The first half of the skin reveal - the setup line, which lands where the Stand reveal's does.
     *
     * <p>The name itself is not scheduled alongside it. It has to be said on the tick the new Stand
     * actually arrives, and what it says depends on a roll the server has not made yet, so it comes
     * back as its own packet instead of as a countdown that has to guess.
     */
    private static void announceSkinAfterAwakening(float now) {
        StandRitualText.showAfter(Component.translatable("message.jojoha.arrow.skin_revealed"),
                now, StandArrowRitual.REVEAL_PAUSE_TICKS, StandRitualText.NARRATION_COLOR);

        // Lazily, and it matters more here than it does for the Stand: this line is resolved after
        // the roll has happened and the new skin has synced, so reading it now would name the skin
        // that was just destroyed.
        // The name is not scheduled here. It arrives as its own packet the moment the new Stand
        // does - see Effect.SKIN_NAMED.
    }

    /** The skin's own tag - "P6", "OVA" - shouted alongside the Stand's name. */
    private static Component skinName() {
        StandData stand = ClientPlayerDataCache.data.stand;
        ResourceLocation id = stand.isPresent() ? stand.standId() : StandTypes.STAR_PLATINUM_ID;
        String key = StandTypes.byIdOrDefault(id).skinNameKey(stand.skin());
        return Component.literal(Component.translatable(key).getString().toUpperCase(Locale.ROOT));
    }

    /**
     * Upper-cased for the reveal so the name reads as a shout. Done here rather than in the lang
     * file so the natural-case name stays available for anywhere else it's shown.
     */
    private static Component standName() {
        StandData stand = ClientPlayerDataCache.data.stand;
        // Falls back to the one Stand that exists if the sync somehow hasn't landed yet.
        ResourceLocation id = stand.isPresent() ? stand.standId() : StandTypes.STAR_PLATINUM_ID;
        String key = "stand." + id.getNamespace() + "." + id.getPath();
        return Component.literal(Component.translatable(key).getString().toUpperCase(Locale.ROOT));
    }

    /** Call once per client tick. */
    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            // Left a world - drop everything, or a rejoin would replay whatever was mid-flight.
            PlayerStabAnimation.clear();
            StandAwakeningRays.clear();
            StandAfterimages.clear();
            TimeStopShader.clear();
            TimeStopClient.clear();
            FrozenEntityFx.clear();
            StandCastGlow.clear();
            TimeStopCharge.clear();
            TimeStopView.clear();
            StandBarrageAudio.clear();
            StandRitualText.clear();
            return;
        }

        // The shader follows the synced counters rather than an event, so it survives relogging
        // mid-stop and can never be left switched on by a packet that went missing.
        TimeStopView.tick();
        TimeStopShader.setPhase(TimeStopView.active());

        float now = (float) minecraft.level.getGameTime();
        PlayerStabAnimation.tick(now);
        StandAwakeningRays.tick(now);
        StandAfterimages.tick();
        TimeStopClient.tick(now);
        FrozenEntityFx.tick(TimeStopView.active());
        StandCastGlow.tick(ClientPlayerDataCache.data.isCastingTimeStop());
        TimeStopCastMotes.tick();
        TimeStopCharge.tick();
        StandRitualText.tick(now);
    }
}
