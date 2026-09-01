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
import org.gumel.jojoha.data.VampireStage;

/**
 * The Stone Mask. Put it on and it puts something else on you.
 *
 * <p>Right-click starts the equipping animation; nothing is decided at the click. The mask is not
 * consumed here and the transformation does not happen here - both belong to
 * {@link StoneMaskRitual}, which runs them against the animation so that the mask leaves the hand
 * on the frame it reaches the face, and the blood runs on the frame it wakes.
 *
 * <p>Main hand only, for the same reason the Stand Arrow is: there is one animation and it raises
 * the right arm. Passing rather than failing on the off hand leaves that click free to fall through
 * to whatever is behind it.
 */
public class StoneMaskItem extends Item {
    public StoneMaskItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (hand == InteractionHand.OFF_HAND) {
            return InteractionResultHolder.pass(stack);
        }

        if (!(level instanceof ServerLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        JojohaPlayerData data = PlayerDataAccess.get(serverPlayer);

        // Already wearing one, already turning, or already turned. The mask has one use and it has
        // had it - a second would have nothing left to do and would strand the animation on a
        // player whose face is already covered.
        if (data.stoneMaskWorn || data.stoneMaskRitualTicks > 0
                || data.vampireStage != VampireStage.NONE) {
            return InteractionResultHolder.fail(stack);
        }

        // Inert until it has been fed. The mask does not wake because somebody put it on - it wakes
        // because blood reached it, and the wearing is only what happens next. Refusing a clean mask
        // is what makes the kill part of the ritual rather than a decoration on it.
        if (!MaskBlood.isBloodied(stack)) {
            player.displayClientMessage(
                    Component.translatable("message.jojoha.mask.unfed"), true);
            return InteractionResultHolder.fail(stack);
        }

        // Night only. The mask makes something the sun then spends every day trying to kill, and
        // letting it be put on at noon would mean the first thing a new vampire ever did was catch
        // fire - the weakness arriving before they had any idea it existed. Refusing until dark is
        // the same rule stated in advance instead of as a punishment.
        if (level.isDay()) {
            player.displayClientMessage(
                    Component.translatable("message.jojoha.mask.daylight"), true);
            return InteractionResultHolder.fail(stack);
        }

        StoneMaskRitual.begin(serverPlayer, data);
        return InteractionResultHolder.success(stack);
    }
}
