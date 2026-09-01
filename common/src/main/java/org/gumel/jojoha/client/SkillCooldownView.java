package org.gumel.jojoha.client;

import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.gumel.jojoha.data.JojohaPlayerData;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

/**
 * How long a move still has to cool, measured on a clock that cannot jump.
 *
 * <h2>Why not just compare game times</h2>
 *
 * <p>A cooldown is stored as the absolute game time it expires at, which is exactly right for the
 * server and for saving - both ends share a clock, so no latency can shorten or lengthen it. Drawing
 * from it directly is where it goes wrong.
 *
 * <p>The client's game time is its own counter. It advances a tick at a time in
 * {@code ClientLevel.tickTime}, and the server overwrites it outright with a set-time packet once a
 * second. Between those corrections the two clocks drift - a stutter, a slow frame, a moment of
 * catch-up ticking - and the correction closes the gap in one step. If the client had fallen behind,
 * that step is forward, and every cooldown on the bar loses however many ticks the correction was
 * worth.
 *
 * <p>Which is precisely the reported symptom: a shade that runs most of the way and then vanishes
 * early, at no particular point, because the moment it disappears is wherever the once-a-second
 * correction happened to land.
 *
 * <h2>What this does instead</h2>
 *
 * <p>The game clock is read exactly once per cooldown - the first time a new expiry is seen - and
 * converted there and then into a wall-clock deadline. Everything after that is measured against
 * {@code Util.getMillis}, which only ever moves forward and is never corrected by anybody. A shade
 * therefore always runs its full length, whatever the two tick counters do to each other in the
 * meantime.
 *
 * <p>The server stays the authority on whether the move can actually be used: it re-checks the real
 * cooldown when the press arrives, and this map is only consulted for drawing. Should the two ever
 * disagree by a frame at the very end, the cost is a shade that clears a moment late, not a move
 * that fires when it should not.
 */
final class SkillCooldownView {
    /** How long a tick is, for turning a count of them into a deadline. */
    private static final long MILLIS_PER_TICK = 50L;

    /** The expiry each move was last seen carrying, so a changed one is noticed. */
    private static final Map<ResourceLocation, Long> KNOWN_EXPIRY = new HashMap<>();

    /** And when that works out to on a clock that does not get corrected. */
    private static final Map<ResourceLocation, Long> ENDS_AT = new HashMap<>();

    /**
     * The world these deadlines were worked out in.
     *
     * <p>Weak on purpose. This is a static field on a client class, and a hard reference to a level
     * from one would keep the whole of a disconnected world - entities, chunks and all - alive for
     * as long as the game runs.
     */
    private static WeakReference<Level> countedIn = new WeakReference<>(null);

    private SkillCooldownView() {
    }

    /**
     * Whether this move should be drawn as cooling.
     *
     * @param gameTime the client's current game time, read only when a cooldown is first seen
     */
    static boolean cooling(JojohaPlayerData data, ResourceLocation moveId, long gameTime,
                           Level level) {
        if (countedIn.get() != level) {
            countedIn = new WeakReference<>(level);
            forget();
        }

        Long expiry = data.moveCooldowns.get(moveId);
        if (expiry == null) {
            // The server says there is no cooldown at all, which outranks anything remembered here -
            // a move cleared early is ready, and the bar should say so immediately.
            KNOWN_EXPIRY.remove(moveId);
            ENDS_AT.remove(moveId);
            return false;
        }

        if (!expiry.equals(KNOWN_EXPIRY.get(moveId))) {
            // A cooldown we have not converted yet: a fresh one, a re-cast, or a rejoin. This is the
            // single point at which the game clock is trusted, and after it the deadline is fixed.
            KNOWN_EXPIRY.put(moveId, expiry);
            long remaining = Math.max(0L, expiry - gameTime);
            ENDS_AT.put(moveId, Util.getMillis() + remaining * MILLIS_PER_TICK);
        }

        Long deadline = ENDS_AT.get(moveId);
        return deadline != null && Util.getMillis() < deadline;
    }

    /**
     * Dropped when the world changes, because the deadlines are meaningless in the next one.
     *
     * <p>Not strictly required - a changed expiry re-converts on its own - but a move whose cooldown
     * happened to end at the same game time in two worlds would otherwise carry a stale deadline
     * across, and that is a bug waiting to be confusing rather than one worth having.
     */
    private static void forget() {
        KNOWN_EXPIRY.clear();
        ENDS_AT.clear();
    }
}
