package org.gumel.jojoha.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.gumel.jojoha.vampire.Headless;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public final class HeadJarItem extends BlockItem {
    private static final String HEAD = "jojoha_head";
    private static final String NAME = "jojoha_head_name";

    public HeadJarItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Nullable
    public static UUID occupant(ItemStack stack) {
        if (!(stack.getItem() instanceof HeadJarItem)) {
            return null;
        }
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.hasUUID(HEAD) ? tag.getUUID(HEAD) : null;
    }

    public static String occupantName(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString(NAME);
    }

    public static void fill(ItemStack stack, ServerPlayer head) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putUUID(HEAD, head.getUUID());
            tag.putString(NAME, head.getGameProfile().getName());
        });
        stack.set(DataComponents.MAX_STACK_SIZE, 1);
    }

    public static void clear(ItemStack stack) {
        stack.remove(DataComponents.CUSTOM_DATA);
        stack.remove(DataComponents.MAX_STACK_SIZE);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity holder, int slot, boolean selected) {
        UUID head = occupant(stack);
        if (head != null && !level.isClientSide() && !holder.getUUID().equals(Headless.carrierOf(head))) {
            clear(stack);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (occupant(stack) != null) {
            tooltip.add(Component.translatable("item.jojoha.head_jar.holds", occupantName(stack))
                    .withStyle(ChatFormatting.WHITE));
        } else {
            tooltip.add(Component.translatable("item.jojoha.head_jar.desc").withStyle(ChatFormatting.GRAY));
        }
    }
}
