package org.gumel.jojoha.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Predicate;

public class TossableItem extends Item {
    public static final float THROW_SPEED = 1.2F;
    public static final int THROW_COOLDOWN = 10;

    private final Predicate<ServerPlayer> drink;

    public TossableItem(Properties properties, Predicate<ServerPlayer> drink) {
        super(properties);
        this.drink = drink;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) {
            return ItemUtils.startUsingInstantly(level, player, hand);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.5F, 0.6F);
        if (!level.isClientSide()) {
            ThrownTrinket thrown = new ThrownTrinket(level, player);
            thrown.setItem(stack.copyWithCount(1));
            thrown.shootFromRotation(player, player.getXRot(), player.getYRot(), 0F, THROW_SPEED, 1F);
            level.addFreshEntity(thrown);
        }
        player.getCooldowns().addCooldown(this, THROW_COOLDOWN);
        stack.consume(1, player);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return CurioItem.DRINK_TICKS;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (user instanceof ServerPlayer server && drink.test(server)) {
            stack.consume(1, server);
        }
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        String id = BuiltInRegistries.ITEM.getKey(this).getPath();
        lines.add(Component.translatable("item.jojoha." + id + ".use").withStyle(ChatFormatting.GRAY));
    }
}
