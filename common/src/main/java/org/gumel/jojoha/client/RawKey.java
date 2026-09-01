package org.gumel.jojoha.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Reads this mod's keys from the keyboard rather than from vanilla's dispatch table.
 *
 * <h2>Why this exists</h2>
 *
 * <p>{@code KeyMapping} does not dispatch a key press to every binding on that key. It dispatches
 * to exactly one, and picks it arbitrarily. Read out of the 1.21.1 bytecode rather than assumed:
 *
 * <ul>
 *   <li>{@code KeyMapping.MAP} is a {@code Map<Key, KeyMapping>} - one binding per physical key;</li>
 *   <li>{@code resetMapping()} clears it and then does {@code MAP.put(mapping.key, mapping)} for
 *   every binding in turn, so when two share a key the later one silently replaces the earlier;</li>
 *   <li>{@code click(key)} increments {@code MAP.get(key).clickCount}, and {@code set(key, held)}
 *   calls {@code MAP.get(key).setDown(...)} - so the binding that lost the slot receives neither
 *   presses nor a held state.</li>
 * </ul>
 *
 * <p>The controls screen shows the clash, which is easy to mistake for a warning that both will
 * fire. Neither does. The loser is not degraded, it is deaf - and which one loses depends on map
 * iteration order, so the same clash can behave differently between two launches.
 *
 * <p>That is what stopped the Stand being summoned: summon shares X with vanilla's creative-toolbar
 * load, vanilla held the slot, and the key press never reached the mod at all. No packet, no log
 * line, nothing to find at the far end - the input simply did not exist.
 *
 * <h2>What this does instead</h2>
 *
 * <p>Asks GLFW whether the key is physically down, via the binding's own currently-bound key. The
 * {@code KeyMapping} objects still exist and still appear in the controls screen, so everything
 * stays rebindable; they are just no longer the thing that carries the input. A mod binding and a
 * vanilla one can now share a key and both do their jobs.
 *
 * <p>{@link #pressed} must be called exactly once per client tick per binding, because it detects
 * the edge itself rather than draining a queue. Calling it twice in a tick would report the press
 * to whichever caller ran first and hide it from the other.
 */
public final class RawKey {
    /**
     * Keyed on the binding object rather than its name, because the name is a translation key and
     * two bindings could plausibly share one; identity cannot collide.
     */
    private static final Map<KeyMapping, Boolean> WAS_DOWN = new IdentityHashMap<>();

    private RawKey() {
    }

    /**
     * Whether this binding's key is physically held.
     *
     * <p>Falls back to vanilla's own state for anything that is not a keyboard key - a binding on a
     * mouse button, or one that is unbound - since those either cannot be polled this way or have
     * nothing to poll.
     */
    public static boolean isDown(KeyMapping mapping) {
        Minecraft minecraft = Minecraft.getInstance();

        // Typing in a screen is not playing. Vanilla's own bindings are naturally quiet here because
        // handleKeybinds only runs in-world; polling the hardware is not, so the guard has to be
        // explicit or every letter typed into chat would fire a Stand move.
        if (minecraft.screen != null || minecraft.player == null) {
            return false;
        }

        InputConstants.Key key = InputConstants.getKey(mapping.saveString());
        if (key.getType() != InputConstants.Type.KEYSYM
                || key.getValue() == InputConstants.UNKNOWN.getValue()) {
            return mapping.isDown();
        }

        return InputConstants.isKeyDown(minecraft.getWindow().getWindow(), key.getValue());
    }

    /**
     * Whether the skill modifier - either alt key - is physically held.
     *
     * <p>Alt rather than shift, and rather than control, because it is the only one of the three
     * that vanilla leaves alone. Shift is sneak and control is sprint, so either of those would
     * have meant crouching or bolting every time a player reached for one of the upper skill slots.
     * That is not a modifier; it is a second action stapled to the first.
     *
     * <p>Both sides accepted, since which one falls under the hand depends on the keyboard and on
     * which hand is free.
     *
     * <p>Polled like everything else here, and for one extra reason: the obvious alternative,
     * {@code Screen.hasAltDown}, answers about the last key event the window saw rather than about
     * the keyboard now - a frame stale exactly when a modifier is being held down with the key it
     * modifies.
     */
    public static boolean modifierDown() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null || minecraft.player == null) {
            return false;
        }

        long window = minecraft.getWindow().getWindow();
        return InputConstants.isKeyDown(window, InputConstants.KEY_LALT)
                || InputConstants.isKeyDown(window, InputConstants.KEY_RALT);
    }

    /**
     * Whether this binding went down since the last tick.
     *
     * <p>The replacement for {@code consumeClick()}, and deliberately not a queue: a queue can hold
     * several presses from one tick and hand them out one call at a time, which is what let vanilla
     * and a mod drain each other's clicks. An edge is a fact about this tick that any number of
     * readers could ask about, and exactly one does.
     */
    public static boolean pressed(KeyMapping mapping) {
        boolean down = isDown(mapping);
        boolean was = Boolean.TRUE.equals(WAS_DOWN.put(mapping, down));
        return down && !was;
    }
}
