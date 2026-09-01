package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.gumel.jojoha.data.ClientPlayerDataCache;

/**
 * Whether the next right-click belongs to the player or to their Stand.
 *
 * <p>The stance decides, and nothing else. This used to want a held key as well, on the reasoning
 * that a mode you can forget you are in is a mode that eventually pours a bucket of lava somewhere
 * you did not mean. That reasoning was sound for a stance the Stand could fight in - it is not
 * sound for this one, because Utility no longer does anything else. Its punches, its guard and its
 * moves are all stood down (see {@code StandMode.handlesItems} and the checks that read it), so
 * there is nothing left for a right-click in this stance to have been meant for.
 *
 * <p>What that costs, plainly: while you are in Utility with a block in hand, right-clicking a
 * chest places the block rather than opening it. Switching stance is the way out, and the eyes on
 * the HUD are green the whole time you are in it.
 */
public final class StandHandsInput {
    /**
     * How long the intercepted click suppresses itself for, in ticks.
     *
     * <p>The same four ticks vanilla writes into {@code rightClickDelay}, and it has to be set by
     * hand: cancelling {@code startUseItem} at its head means cancelling it before the line that
     * would have set the delay, so without this the method is re-entered every single tick the
     * button is down and the server is asked to act twenty times a second.
     */
    public static final int REPEAT_DELAY_TICKS = 4;

    private StandHandsInput() {
    }

    /**
     * Whether this click should be sent to the Stand instead of being used by the player.
     *
     * <p>Checked against the client's cached data rather than assumed, so leaving the stance or
     * losing the Stand hands the button straight back to the player rather than silently eating
     * their clicks until they notice.
     */
    public static boolean shouldDelegate() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null
                && ClientPlayerDataCache.data.standSummoned
                && ClientPlayerDataCache.data.standMode.handlesItems()
                && delegatedHand(player) != null;
    }

    /**
     * Which hand the Stand would actually use, or null if neither holds anything it can.
     *
     * <p>Main first, then off - the same order the server walks, so that anything drawn from this
     * (the placement preview, in particular) is describing the hand that will really be used rather
     * than the one the player assumes. A bucket in the main hand and a block in the off hand is the
     * case that separates the two.
     *
     * <p>The use-duration test is the same one the server makes - see {@code StandHands.canDelegate}.
     * Asked here as well because declining means something different on each side: there it is not
     * acting, here it is getting out of the way. The intercept cancels vanilla's own use outright,
     * so a click the server was always going to refuse would disappear rather than fall through to
     * the player's own hands - a bow in Utility would draw nothing and give no reason why.
     */
    public static InteractionHand delegatedHand(LocalPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (!stack.isEmpty() && stack.getUseDuration(player) <= 0) {
                return hand;
            }
        }
        return null;
    }
}
