package org.gumel.jojoha.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.gumel.jojoha.client.ClackerClient;
import org.gumel.jojoha.combat.Clackers;
import org.gumel.jojoha.hamon.Conductivity;
import org.gumel.jojoha.skill.Indicator;

import java.util.List;

public class ClackersItem extends Item {
    public static final float HAMON_BONUS = 0.25F;

    public ClackersItem(Properties properties) {
        super(properties);
    }

    public static boolean held(Player player) {
        return player.getMainHandItem().getItem() instanceof ClackersItem
                || player.getOffhandItem().getItem() instanceof ClackersItem;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer) {
            Clackers.handleSpin(serverPlayer, hand);
        } else if (level.isClientSide && Clackers.spin(player, hand)) {
            ClackerClient.spun();
        }
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines,
                                TooltipFlag flag) {
        lines.add(Indicator.HAMON.line(Component.translatable("item.jojoha.clackers.desc")
                .withStyle(ChatFormatting.GRAY)));
        lines.add(Indicator.ABILITY.line(Component.translatable("item.jojoha.clackers.controls")
                .withStyle(ChatFormatting.DARK_GRAY)));
        Conductivity.appendLine(stack, lines);
    }
}
