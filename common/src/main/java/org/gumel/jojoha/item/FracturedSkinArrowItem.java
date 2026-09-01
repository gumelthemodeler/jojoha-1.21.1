package org.gumel.jojoha.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.stand.StandTypes;

/**
 * An arrow that rewrites what a Stand looks like rather than whether you have one.
 *
 * <p>It is the Stand Arrow's opposite in the one way that matters: the Stand Arrow refuses anybody
 * who already has a Stand, and this refuses anybody who does not. Everything else about using it is
 * the same - the same stab, the same wind-up, the same held beat before the payoff - because it is
 * the same act being performed on somebody who is already through it once. Only what comes out the
 * other side differs. See {@link StandArrowRitual}, which owns the timeline for both.
 *
 * <p>No worthiness gate and no roll. Worthiness is the measure of whether a Stand will answer at
 * all, and this arrow is not asking one to; it is being driven into a Stand that already answered.
 * Adding a chance of failure would make it a lottery ticket for a cosmetic, which is a worse thing
 * to hand a player than a guarantee.
 */
public final class FracturedSkinArrowItem extends Item {
    public FracturedSkinArrowItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Main hand only, for the reason the Stand Arrow gives: there is one stab animation and it
        // swings the right arm. Passing rather than failing leaves the off-hand click free to fall
        // through to whatever is behind it.
        if (hand == InteractionHand.OFF_HAND) {
            return InteractionResultHolder.pass(stack);
        }

        if (!(level instanceof ServerLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        JojohaPlayerData data = PlayerDataAccess.get(serverPlayer);

        // Nothing to re-skin. Told rather than silently refused - an arrow that does nothing when
        // you click it is indistinguishable from an arrow that is broken.
        if (!data.stand.isPresent()) {
            serverPlayer.displayClientMessage(
                    Component.translatable("message.jojoha.skin_arrow.no_stand"), true);
            return InteractionResultHolder.fail(stack);
        }

        // One ritual at a time, and that includes the swap's own tail: the Stand is in pieces for
        // part of it, and a second arrow landing in that gap would be asking to change the skin of
        // something that does not currently exist.
        if (data.standArrowRitualTicks > 0 || data.standSkinSwapTicks > 0) {
            return InteractionResultHolder.fail(stack);
        }

        // A Stand with one look has nothing to roll between. Better to refuse the click than to
        // spend the arrow on a sequence that ends with the Stand looking exactly as it did.
        if (StandTypes.byIdOrDefault(data.stand.standId()).skinCount() < 2) {
            serverPlayer.displayClientMessage(
                    Component.translatable("message.jojoha.skin_arrow.no_skins"), true);
            return InteractionResultHolder.fail(stack);
        }

        // Not consumed here. It stays in hand for the whole stab and is spent when the arrow
        // actually goes in - the same deferral the Stand Arrow makes, so the item leaving the hand
        // lines up with the payoff rather than with the click.
        data.standArrowRitualSkin = true;
        StandArrowRitual.begin(serverPlayer, data);
        return InteractionResultHolder.success(stack);
    }
}
