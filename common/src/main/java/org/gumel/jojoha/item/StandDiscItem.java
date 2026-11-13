package org.gumel.jojoha.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Locale;

public final class StandDiscItem extends Item {
    public StandDiscItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        ItemStack blank = StandDiscs.commit(serverPlayer, stack);
        if (blank.isEmpty()) {
            return InteractionResultHolder.fail(stack);
        }
        return InteractionResultHolder.success(
                ItemUtils.createFilledResult(stack, player, blank, false));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        StandDiscs.Skin skin = StandDiscs.skinOn(stack);
        if (skin == null) {
            lines.add(Component.translatable("item.jojoha.stand_disc.blank").withStyle(ChatFormatting.GRAY));
            return;
        }
        lines.add(Component.literal(skin.standId().getPath().replace('_', ' ').toUpperCase(Locale.ROOT))
                .withStyle(ChatFormatting.GOLD));
        lines.add(Component.translatable("item.jojoha.stand_disc.skin",
                        StandDiscs.skinName(skin.standId(), skin.skin()))
                .withStyle(ChatFormatting.GRAY));
        if (StandDiscs.standOn(stack) != null) {
            lines.add(Component.translatable("item.jojoha.stand_disc.whole_stand").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        String owner = StandDiscs.ownerOf(stack);
        if (!owner.isEmpty()) {
            lines.add(Component.translatable("item.jojoha.stand_disc.owner", owner).withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
