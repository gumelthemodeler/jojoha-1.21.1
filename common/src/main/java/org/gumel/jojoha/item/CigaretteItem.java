package org.gumel.jojoha.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.gumel.jojoha.registry.ModItems;

import java.util.List;

public class CigaretteItem extends Item {
    public static final int PUFFS = 3;
    public static final int PUFF_COOLDOWN = 60;

    private final boolean lit;

    public CigaretteItem(Properties properties, boolean lit) {
        super(properties);
        this.lit = lit;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!lit) {
            InteractionHand other = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            ItemStack lighter = player.getItemInHand(other);
            if (!lighter.is(Items.FLINT_AND_STEEL) && !lighter.is(Items.FIRE_CHARGE)) {
                player.displayClientMessage(Component.translatable("message.jojoha.cigarette.no_light"), true);
                return InteractionResultHolder.fail(stack);
            }
            level.playSound(null, player.blockPosition(), SoundEvents.FLINTANDSTEEL_USE, SoundSource.PLAYERS, 0.8F, 1.2F);
            if (!level.isClientSide()) {
                if (lighter.is(Items.FLINT_AND_STEEL)) {
                    lighter.hurtAndBreak(1, player, other == InteractionHand.MAIN_HAND
                            ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
                } else {
                    lighter.consume(1, player);
                }
                stack.consume(1, player);
                ItemStack burning = new ItemStack(ModItems.CIGARETTE_LIT.get());
                if (stack.isEmpty()) {
                    return InteractionResultHolder.success(burning);
                }
                if (!player.getInventory().add(burning)) {
                    player.drop(burning, false);
                }
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        if (player instanceof ServerPlayer server) {
            Provisions.puff(server);
            stack.hurtAndBreak(1, server, hand == InteractionHand.MAIN_HAND
                    ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        }
        player.getCooldowns().addCooldown(this, PUFF_COOLDOWN);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        String id = BuiltInRegistries.ITEM.getKey(this).getPath();
        lines.add(Component.translatable("item.jojoha." + id + ".use").withStyle(ChatFormatting.GRAY));
    }
}
