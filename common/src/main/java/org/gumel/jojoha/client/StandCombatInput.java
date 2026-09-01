package org.gumel.jojoha.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.gumel.jojoha.data.ClientPlayerDataCache;
import org.gumel.jojoha.network.NetworkHandler;

/**
 * M1 (punch chain) and guard-key input while a Stand is summoned. Shared between platforms
 * since it's pure client-tick logic with no platform-specific API involved.
 *
 * <p>M1 is deliberately peeked via {@code isDown()}, never {@code consumeClick()} - the latter
 * would drain vanilla's own shared {@code keyAttack} click queue, breaking normal
 * attacking/mining while a Stand happens to be summoned. Peeking and edge-detecting locally
 * leaves vanilla's own handling completely untouched.
 */
public final class StandCombatInput {
    private static boolean wasAttackDown = false;
    private static boolean wasGuardDown = false;

    /**
     * How long the summon key has been held, and whether the hold has already been spent.
     *
     * <p>One key doing two things needs both: the counter to tell a tap from a hold, and the latch
     * so a long hold cycles the stance once rather than once per tick.
     */
    private static int summonHeld;
    private static boolean summonSpent;

    /**
     * How long a hold has to last before it means the other thing. Ticks.
     *
     * <p>Six, about a third of a second. Long enough that no ordinary tap reaches it, short enough
     * that holding for it does not feel like waiting - and the cost of the arrangement is that
     * summoning now resolves when the key comes up rather than when it goes down, because until
     * then there is no telling which of the two was meant.
     */
    private static final int MODE_HOLD_TICKS = 6;

    /**
     * The platform's guard key, kept so that code running earlier in the frame can ask about it.
     *
     * <p>The binding lives in the platform client class and is handed in once a tick, but the
     * offhand-swap suppression has to read it from inside {@code Minecraft.handleKeybinds}, which
     * runs well before this does. Stashing the reference is the whole trick - it is the same
     * KeyMapping object either way, and its pressed state is live rather than something this class
     * samples and caches.
     */
    private static KeyMapping guardKeyRef;

    private StandCombatInput() {
    }

    /** Call once per client tick, passing the platform's own guard and summon keys. */
    public static void tick(KeyMapping guardKey, KeyMapping summonKey) {
        guardKeyRef = guardKey;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            wasAttackDown = false;
            wasGuardDown = false;
            summonHeld = 0;
            summonSpent = false;
            return;
        }

        // Read straight off the keyboard rather than from vanilla's dispatch table - see RawKey.
        // A binding that shares its key with a vanilla one receives nothing at all, silently, so
        // no mod key in this codebase goes through KeyMapping's own state any more.
        //
        // Unconditional, and that is the fix rather than a simplification. This key used to be
        // context-sensitive: with something lined up in the crosshair it committed the Stand to
        // that target instead of switching stance. Which meant the stance could not be changed
        // while looking at anything - including the mob you were changing stance because of - and
        // the failure was silent, since a Stand already on its way looks the same as a key that
        // did nothing.
        //
        // Nothing is lost by dropping it. M1 at a target beyond punching range already sends the
        // Stand after it (see StandCombatHandler.handlePunchRequest) - the same pursueAndPunch
        // call, by a route the player is already using. The commit press was left over from a
        // COMBAT stance that no longer exists.
        // One key, two jobs. Tapping it summons or dismisses; holding it changes the Stand's
        // stance. They cannot collide, because a Stand only has a stance while it is out - and the
        // key that puts it out is this one - so the two are never both meaningful at once.
        //
        // This replaced a second binding on H. Polled rather than consumed either way: see RawKey.
        // The summon key shares its key with a vanilla one, and the failure was invisible - no
        // press, no packet, nothing to find.
        if (RawKey.isDown(summonKey)) {
            summonHeld++;
            if (!summonSpent && summonHeld >= MODE_HOLD_TICKS) {
                NetworkHandler.sendCycleStandMode();
                summonSpent = true;
            }
        } else {
            // Released without ever reaching the threshold, so it was a tap and meant the summon.
            if (summonHeld > 0 && !summonSpent) {
                NetworkHandler.sendToggleStandSummon();
            }
            summonHeld = 0;
            summonSpent = false;
        }

        boolean attackDown = mc.options.keyAttack.isDown();
        if (attackDown && !wasAttackDown && ClientPlayerDataCache.data.standSummoned) {
            NetworkHandler.sendRequestStandPunch();
        }
        wasAttackDown = attackDown;

        boolean guardDown = RawKey.isDown(guardKey);
        if (guardDown != wasGuardDown) {
            wasGuardDown = guardDown;
            NetworkHandler.sendSetStandGuard(guardDown);
        }
    }

    /**
     * Whether the guard key is held right now.
     *
     * <p>Asked from outside this class's own tick - see OffhandSwapMixin - so it reads the mapping
     * directly rather than a flag this class caches, which would be a frame stale for exactly the
     * caller that needs it. Null until the first tick has handed the binding over, which is one
     * frame at startup with no Stand out in it.
     */
    public static boolean guardKeyDown() {
        return guardKeyRef != null && RawKey.isDown(guardKeyRef);
    }
}
