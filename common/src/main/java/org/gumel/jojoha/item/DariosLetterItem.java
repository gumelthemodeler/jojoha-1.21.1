package org.gumel.jojoha.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.fortune.Fortunes;
import org.gumel.jojoha.level.Continents;

import java.util.List;

public class DariosLetterItem extends Item {
    public static final int READ_COOLDOWN = 100;

    public DariosLetterItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.8F, 0.9F);
        if (player instanceof ServerPlayer server) {
            server.sendSystemMessage(Component.translatable("message.jojoha.darios_letter.read")
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            BlockPos moors = Continents.nearest(server.serverLevel(), server.blockPosition(),
                    Continents.Continent.PHANTOM_MOORS);
            if (moors != null) {
                Fortunes.whisper(server, Component.translatable("message.jojoha.darios_letter.hint",
                        Fortunes.direction(server.position(), Vec3.atCenterOf(moors))));
            }
        }
        player.getCooldowns().addCooldown(this, READ_COOLDOWN);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.jojoha.darios_letter.use").withStyle(ChatFormatting.GRAY));
    }
}
