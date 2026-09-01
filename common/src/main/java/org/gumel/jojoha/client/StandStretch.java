package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.StandHands;
import org.gumel.jojoha.stand.StandRange;

import java.util.Optional;

/**
 * Where the player started pulling a row from.
 *
 * <p>A stretch, not a lock. Holding the use button plants a corner, the crosshair sizes the run,
 * and nothing is placed until you let go - so a stretch you don't like costs nothing to abandon.
 * Which axis it runs along is the bar's business, not this class's; see {@code BuildMode}.
 *
 * <p>Client-owned, because it is a selection rather than a rule. The anchor rides along with the
 * click that spends it and the server clamps whatever arrives - see {@code StandUseItemPacket}.
 *
 * <h2>Why the aim is computed here and not read from the preview</h2>
 *
 * <p>It used to be the other way round: the renderer worked out which cell was under the crosshair
 * and handed it to this class, which took a copy whenever sneak went down. That made a selection
 * depend on a draw call, which is the wrong way round for two reasons and broke outright for a
 * third.
 *
 * <p>The renderer only draws when it has something to draw. Aiming at open sky is a miss, so it
 * bailed - and told this class the target was nothing on its way out. Which meant that pressing
 * sneak while looking upward, the exact gesture that starts a pillar, planted an anchor of null and
 * the stretch never existed. The one case the feature was asked for was the one case that could not
 * work.
 *
 * <p>So the aim is worked out here, on the tick, from the same {@link StandHands#aimFrom} the server
 * will use. A miss now keeps the last cell that <em>was</em> under the crosshair rather than
 * clearing it, so looking from the ground up to the sky carries the corner with it.
 */
public final class StandStretch {
    private static BlockPos anchor;
    private static BlockPos target;
    private static boolean wasUsing;

    private StandStretch() {
    }

    /** Call once per client tick. */
    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;

        if (player == null || minecraft.level == null || !StandHandsInput.shouldDelegate()) {
            clear();
            return;
        }

        Entity found = StandEntityLookup.localStand(minecraft).orElse(null);
        if (!(found instanceof StandEntity stand)) {
            clear();
            return;
        }

        InteractionHand hand = StandHandsInput.delegatedHand(player);
        ItemStack stack = player.getItemInHand(hand);

        BlockHitResult hit = StandHands.aimFrom(player, player.getEyePosition(),
                player.getViewVector(1.0F), reachFor(stand));

        // Kept rather than cleared on a miss. Sky is not a target, but it is also not a reason to
        // forget the ground you were looking at a moment ago - and looking up is how a pillar
        // starts.
        if (hit.getType() == HitResult.Type.BLOCK) {
            target = StandHands.fills(stack)
                    ? new BlockPlaceContext(player, hand, stack, hit).getClickedPos()
                    : hit.getBlockPos();
        }

        // Hold the use button and aim. Press plants the near corner, the far end follows the
        // crosshair for as long as it is held, and letting go is what places the run.
        //
        // This used to want sneak held as well, which was left over from when the shape had to be
        // inferred and the modifier was how you said "I mean a row, not a block". Now that the bar
        // says which shape, the modifier was a second, invisible thing to be holding - so every
        // click without it fell through to a single block and the modes looked broken. They were
        // not being ignored; they were never being asked.
        boolean stretching = gestureOwnsClick();
        boolean using = stretching && minecraft.options.keyUse.isDown();

        if (using && !wasUsing) {
            anchor = target;
        } else if (!using && wasUsing && anchor != null) {
            // Released. The run the player has been looking at is the run they get - sent from here
            // rather than from the click intercept, because the intercept fires on the way down and
            // this gesture is only finished on the way up.
            org.gumel.jojoha.network.NetworkHandler.sendStandUseItem(Optional.of(anchor));
            anchor = null;
        } else if (!stretching) {
            anchor = null;
        }

        wasUsing = using;
    }

    /**
     * Whether the drag gesture has claim on the use button.
     *
     * <p>Two things, and the second is the one that was missing. The shape has to be one that runs -
     * Single places on the press like an ordinary click - and the item has to be something a run
     * could be made of.
     *
     * <p>Without that second test the gesture claimed every click in a stretching shape, including
     * ones it had no way to finish. A pearl has no cell to fill, so no corner was ever planted, so
     * the release had nothing to send - and the press had already cancelled vanilla. The click was
     * taken and then dropped, and throwing anything in Utility silently did nothing at all.
     */
    public static boolean gestureOwnsClick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;

        if (player == null || !StandHandsInput.shouldDelegate()
                || !org.gumel.jojoha.data.ClientPlayerDataCache.data.buildMode.stretches()) {
            return false;
        }

        InteractionHand hand = StandHandsInput.delegatedHand(player);
        return hand != null && StandHands.fills(player.getItemInHand(hand));
    }

    /**
     * Whether a run is being dragged out right now.
     *
     * <p>Read by the click intercept, which must stay quiet while this is true: a drag places once,
     * on release, and the intercept would otherwise fire a single block every four ticks the button
     * was held.
     */
    public static boolean dragging() {
        return anchor != null;
    }

    private static void clear() {
        anchor = null;
        target = null;
        wasUsing = false;
    }

    /** A close-range Stand runs shorter errands than a long-range one - see StandHands. */
    private static double reachFor(StandEntity stand) {
        return stand.getStandType().range() == StandRange.LONG
                ? StandHands.LONG_REACH
                : StandHands.CLOSE_REACH;
    }

    /** The planted corner, or empty when the player is not stretching anything. */
    public static Optional<BlockPos> anchor() {
        return Optional.ofNullable(anchor);
    }
}
