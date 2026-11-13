package org.gumel.jojoha.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.gumel.jojoha.data.StatGrowth;
import org.gumel.jojoha.fortune.Gambler;
import org.gumel.jojoha.fortune.Lucky;

import java.util.List;

public final class StatCrystalItem extends Item {
    public static final int LEAST_LEVELS = 1;
    public static final int MOST_LEVELS = 3;

    public StatCrystalItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines,
                                TooltipFlag flag) {
        lines.add(Component.translatable("item.jojoha.stat_crystal.use", LEAST_LEVELS, MOST_LEVELS)
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        int levels = LEAST_LEVELS + serverPlayer.getRandom().nextInt(MOST_LEVELS - LEAST_LEVELS + 1);
        if (Lucky.luckyCrystal(serverPlayer)) {
            levels = Math.max(levels, LEAST_LEVELS + serverPlayer.getRandom().nextInt(MOST_LEVELS - LEAST_LEVELS + 1));
        }
        if (Gambler.calls(serverPlayer)) {
            if (!Gambler.flip(serverPlayer)) {
                stack.consume(1, player);
                return InteractionResultHolder.success(stack);
            }
            levels += Gambler.WIN_LEVELS;
        }
        if (StatGrowth.equalize(serverPlayer, levels) <= 0) {
            serverPlayer.displayClientMessage(Component.translatable("message.jojoha.stat_crystal.even"), true);
            return InteractionResultHolder.fail(stack);
        }

        server.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0, player.getZ(),
                12, 0.3, 0.5, 0.3, 0.02);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE,
                SoundSource.PLAYERS, 1.0F, 1.2F);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_BREAK,
                SoundSource.PLAYERS, 0.8F, 1.0F);

        stack.consume(1, player);
        return InteractionResultHolder.success(stack);
    }

}
