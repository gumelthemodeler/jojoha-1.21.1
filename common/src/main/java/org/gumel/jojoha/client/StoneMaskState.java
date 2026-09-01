package org.gumel.jojoha.client;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.gumel.jojoha.data.ClientPlayerDataCache;
import org.gumel.jojoha.data.VampireStage;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Who has a mask on their face, and whether it has woken.
 *
 * <p>Kept client-side per player rather than read from synced data, because only one player's data
 * is synced - your own. Everyone watching a transformation has to see the mask seat itself and turn,
 * and the broadcast that drives the animation is the same signal that can drive this: the server
 * says "this player has seated the mask" once, and every client that heard it remembers.
 *
 * <p>The local player is answered from their own synced data instead, which is the authority for
 * them and survives a relog - a remembered flag would be lost the moment the world reloaded, and the
 * mask is meant to stay on.
 */
public final class StoneMaskState {
    /** Player UUID -> whether their mask has woken. Presence in the map means the mask is on. */
    private static final Map<UUID, Boolean> WORN = new HashMap<>();

    private StoneMaskState() {
    }

    /**
     * The mask has left the hand and is on its way onto the face.
     *
     * <p>Only the fact is recorded here. The travel itself is drawn from the animation's own Head2
     * bone rather than timed from this moment - see StoneMaskLayer - so there is nothing for this to
     * remember beyond that the mask is now the layer's to draw rather than the hand's.
     */
    public static void seat(UUID playerId, float clientTimeTicks) {
        WORN.putIfAbsent(playerId, false);
    }

    /** The mask has begun to turn. The moment is kept so the colour can cross rather than snap. */
    public static void activate(UUID playerId, float clientTimeTicks) {
        WORN.put(playerId, true);
        TURNING.put(playerId, clientTimeTicks);
    }

    /**
     * How long the eyes take to come up, in ticks.
     *
     * <p>Two seconds, up from three quarters of one. The old length was a fade in arithmetic and a
     * snap to look at: additive light does most of its perceived work in the first third of its
     * range, so a ramp that short arrives almost immediately and then spends the rest of itself on
     * a difference nobody can see.
     */
    public static final float TURN_TICKS = 40F;

    private static final Map<UUID, Float> TURNING = new HashMap<>();

    /** How far the stone has gone over to red, 0 to 1. */
    public static float turnProgress(Player player, float clientTimeTicks) {
        Float start = TURNING.get(player.getUUID());
        if (start == null) {
            return activatedOn(player) ? 1F : 0F;
        }
        float linear = Mth.clamp((clientTimeTicks - start) / TURN_TICKS, 0F, 1F);

        // Eased in rather than ramped. The eyes should kindle - barely there, then unmistakable -
        // and squaring the curve spends the early part of the fade on the part of the range the eye
        // is most sensitive to, which is the difference between almost-black and dark red.
        return linear * linear;
    }

    /** Off the face - either taken out of the hand, or dropped as a FallingMask entity. */
    public static void remove(UUID playerId) {
        WORN.remove(playerId);
        TURNING.remove(playerId);
    }

    public static void clear() {
        WORN.clear();
        TURNING.clear();
    }

    public static boolean wornBy(Player player) {
        if (isLocal(player)) {
            return ClientPlayerDataCache.data.stoneMaskWorn;
        }
        return WORN.containsKey(player.getUUID());
    }

    public static boolean activatedOn(Player player) {
        if (isLocal(player)) {
            // The mask wakes when its wearer does. Reading the stage rather than a second flag means
            // there is only one answer to "has this transformation happened", and it is the one the
            // rest of the mod already asks.
            return ClientPlayerDataCache.data.vampireStage != VampireStage.NONE;
        }
        return Boolean.TRUE.equals(WORN.get(player.getUUID()));
    }

    private static boolean isLocal(Player player) {
        net.minecraft.client.player.LocalPlayer local = net.minecraft.client.Minecraft.getInstance().player;
        return local != null && local.getUUID().equals(player.getUUID());
    }
}
