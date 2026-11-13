package org.gumel.jojoha.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.gumel.jojoha.level.MeteorFall;
import org.gumel.jojoha.registry.ModComponents;

import java.util.List;

public class MeteorMapItem extends Item {
    private static final int MARK_MIN_DISTANCE = 300;
    private static final int MARK_MAX_DISTANCE = 700;
    private static final int TRIGGER_RADIUS = 90;

    public MeteorMapItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity holder, int slot, boolean selected) {
        if (!(level instanceof ServerLevel server) || !(holder instanceof ServerPlayer player)) {
            return;
        }

        BlockPos target = stack.get(ModComponents.METEOR_TARGET.get());
        if (target == null) {
            stack.set(ModComponents.METEOR_TARGET.get(), mark(server, player));
            return;
        }

        double dx = player.getX() - target.getX();
        double dz = player.getZ() - target.getZ();
        if (dx * dx + dz * dz > (double) TRIGGER_RADIUS * TRIGGER_RADIUS) {
            return;
        }

        if (!MeteorFall.isFalling(target)) {
            MeteorFall.begin(server, target);
            player.displayClientMessage(
                    Component.translatable("message.jojoha.meteor.incoming")
                            .withStyle(ChatFormatting.GOLD), true);
        }
        stack.shrink(1);
    }

    private static BlockPos mark(ServerLevel level, ServerPlayer player) {
        double angle = level.random.nextDouble() * Math.PI * 2;
        double distance = MARK_MIN_DISTANCE
                + level.random.nextDouble() * (MARK_MAX_DISTANCE - MARK_MIN_DISTANCE);

        int x = (int) (player.getX() + Math.cos(angle) * distance);
        int z = (int) (player.getZ() + Math.sin(angle) * distance);
        return new BlockPos(x, 0, z);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines,
                                TooltipFlag flag) {
        BlockPos target = stack.get(ModComponents.METEOR_TARGET.get());
        if (target == null) {
            lines.add(Component.translatable("item.jojoha.meteor_map.blank")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }

        lines.add(Component.translatable("item.jojoha.meteor_map.target",
                target.getX(), target.getZ()).withStyle(ChatFormatting.GOLD));
    }
}
