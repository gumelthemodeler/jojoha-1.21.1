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
import org.gumel.jojoha.data.ClientPlayerDataCache;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.stand.BuildMode;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.StandHands;
import org.gumel.jojoha.stand.StandRange;

import java.util.Optional;

public final class StandStretch {
    private static BlockPos anchor;
    private static BlockPos corner;
    private static BlockPos target;
    private static boolean wasUsing;

    private StandStretch() {
    }

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

        if (hit.getType() == HitResult.Type.BLOCK) {
            target = StandHands.fills(stack)
                    ? new BlockPlaceContext(player, hand, stack, hit).getClickedPos()
                    : hit.getBlockPos();
        }

        boolean stretching = gestureOwnsClick();
        boolean using = stretching && minecraft.options.keyUse.isDown();

        if (mode().fillsAVolume()) {
            if (using && !wasUsing) {
                if (player.isShiftKeyDown()) {
                    corner = null;
                } else if (corner == null) {
                    corner = target;
                } else if (target != null) {
                    NetworkHandler.sendStandUseItem(Optional.of(corner),
                            StandTurn.turns());
                    corner = null;
                }
            }
            anchor = null;
            wasUsing = using;
            return;
        }

        corner = null;

        if (using && !wasUsing) {
            anchor = target;
        } else if (!using && wasUsing && anchor != null) {
            NetworkHandler.sendStandUseItem(Optional.of(anchor),
                    StandTurn.turns());
            anchor = null;
        } else if (!stretching) {
            anchor = null;
        }

        wasUsing = using;
    }

    private static BuildMode mode() {
        return ClientPlayerDataCache.data.buildMode;
    }

    public static boolean gestureOwnsClick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;

        if (player == null || !StandHandsInput.shouldDelegate() || !mode().stretches()) {
            return false;
        }

        InteractionHand hand = StandHandsInput.delegatedHand(player);
        return hand != null && StandHands.shapes(player.getItemInHand(hand));
    }

    public static boolean dragging() {
        return anchor != null;
    }

    private static void clear() {
        anchor = null;
        corner = null;
        target = null;
        wasUsing = false;
    }

    private static double reachFor(StandEntity stand) {
        return stand.getStandType().range() == StandRange.LONG
                ? StandHands.LONG_REACH
                : StandHands.CLOSE_REACH;
    }

    public static Optional<BlockPos> anchor() {
        return Optional.ofNullable(anchor);
    }

    public static Optional<BlockPos> corner() {
        return Optional.ofNullable(corner);
    }
}
