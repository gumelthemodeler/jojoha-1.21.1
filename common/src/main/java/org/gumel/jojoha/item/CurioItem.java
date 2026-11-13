package org.gumel.jojoha.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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

public final class CurioItem extends Item {
    public static final int DRINK_TICKS = 32;
    public static final int SNACK_TICKS = 16;

    private final UseAnim animation;
    private final int useTicks;
    private final Predicate<ServerPlayer> effect;

    public CurioItem(Properties properties, UseAnim animation, int useTicks, Predicate<ServerPlayer> effect) {
        super(properties);
        this.animation = animation;
        this.useTicks = useTicks;
        this.effect = effect;
    }

    public static CurioItem drink(Properties properties, Predicate<ServerPlayer> effect) {
        return new CurioItem(properties, UseAnim.DRINK, DRINK_TICKS, effect);
    }

    public static CurioItem snack(Properties properties, Predicate<ServerPlayer> effect) {
        return new CurioItem(properties, UseAnim.EAT, SNACK_TICKS, effect);
    }

    public static CurioItem instant(Properties properties, Predicate<ServerPlayer> effect) {
        return new CurioItem(properties, UseAnim.NONE, 0, effect);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        String id = BuiltInRegistries.ITEM.getKey(this).getPath();
        lines.add(Component.translatable("item.jojoha." + id + ".use").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return animation;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return useTicks;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (useTicks > 0) {
            return ItemUtils.startUsingInstantly(level, player, hand);
        }
        if (!(player instanceof ServerPlayer server)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        if (!effect.test(server)) {
            return InteractionResultHolder.fail(stack);
        }
        player.getCooldowns().addCooldown(this, 10);
        stack.consume(1, player);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (user instanceof ServerPlayer server && effect.test(server)) {
            stack.consume(1, server);
        }
        return stack;
    }
}
