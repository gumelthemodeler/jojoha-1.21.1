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
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.data.StatPerk;
import org.gumel.jojoha.stand.BondRank;

import java.util.List;

public final class RequiemArrowItem extends Item {
    private static final float REJECTION_DAMAGE = 8.0F;

    public RequiemArrowItem(Properties properties) {
        super(properties);
    }

    public static final BondRank REQUIRED_RANK = BondRank.RANK_II;

    public static boolean bonded(JojohaPlayerData data) {
        return BondRank.of(data.stand).ordinal() >= REQUIRED_RANK.ordinal();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand == InteractionHand.OFF_HAND) {
            return InteractionResultHolder.pass(stack);
        }
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        JojohaPlayerData data = PlayerDataAccess.get(serverPlayer);
        if (!data.stand.isPresent()) {
            serverPlayer.displayClientMessage(Component.translatable("message.jojoha.requiem_arrow.no_stand"), true);
            return InteractionResultHolder.fail(stack);
        }

        if (!bonded(data) || !StatPerk.CHOSEN.has(data)) {
            serverPlayer.hurt(serverLevel.damageSources().magic(), REJECTION_DAMAGE);
            serverLevel.playSound(null, serverPlayer.blockPosition(), SoundEvents.ITEM_BREAK,
                    SoundSource.PLAYERS, 1.0F, 0.6F);
            serverPlayer.displayClientMessage(Component.translatable(!bonded(data)
                    ? "message.jojoha.requiem_arrow.not_bonded"
                    : "message.jojoha.requiem_arrow.not_chosen", StatPerk.CHOSEN.threshold()), true);
            serverPlayer.getCooldowns().addCooldown(this, 40);
            return InteractionResultHolder.success(stack);
        }

        serverLevel.sendParticles(ParticleTypes.END_ROD, serverPlayer.getX(),
                serverPlayer.getY() + serverPlayer.getBbHeight() * 0.6, serverPlayer.getZ(), 24, 0.4, 0.6, 0.4, 0.05);
        serverLevel.playSound(null, serverPlayer.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE,
                SoundSource.PLAYERS, 1.0F, 0.7F);
        serverPlayer.displayClientMessage(Component.translatable("message.jojoha.requiem_arrow.answers"), true);
        serverPlayer.getCooldowns().addCooldown(this, 40);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.jojoha.requiem_arrow.unavailable").withStyle(ChatFormatting.RED));
        lines.add(Component.translatable("item.jojoha.requiem_arrow.useless").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("item.jojoha.requiem_arrow.part5").withStyle(ChatFormatting.GOLD));
    }
}
