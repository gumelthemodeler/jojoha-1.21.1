package org.gumel.jojoha.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
import org.gumel.jojoha.JojohaAdvancements;
import org.gumel.jojoha.data.Discipline;
import org.gumel.jojoha.data.Fate;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.hamon.HamonPath;
import org.gumel.jojoha.hamon.HamonPaths;
import org.gumel.jojoha.registry.ModComponents;
import org.gumel.jojoha.stand.anubis.Anubis;

import java.util.List;

public class PathScrollItem extends Item {

    public PathScrollItem(Properties properties) {
        super(properties);
    }

    public static HamonPath pathOn(ItemStack stack) {
        ResourceLocation id = stack.get(ModComponents.HAMON_PATH.get());
        return id == null ? null : HamonPaths.byId(id);
    }

    public static final float BOOK_STEP = 1F / 16F;

    public static float bookNumber(ItemStack stack) {
        HamonPath path = pathOn(stack);
        if (path == null) {
            return 0F;
        }
        int index = HamonPaths.isMode(path)
                ? HamonPaths.all().size() + List.copyOf(HamonPaths.modes()).indexOf(path)
                : List.copyOf(HamonPaths.all()).indexOf(path);
        return (index + 1) * BOOK_STEP;
    }

    public static ItemStack of(ItemStack stack, HamonPath path) {
        stack.set(ModComponents.HAMON_PATH.get(), path.id());
        return stack;
    }

    @Override
    public Component getName(ItemStack stack) {
        HamonPath path = pathOn(stack);
        if (path == HamonPaths.HERMIT) {
            return Component.translatable("item.jojoha.path_scroll.hermit");
        }
        return path == null
                ? super.getName(stack)
                : Component.translatable("item.jojoha.path_scroll.named", path.displayName());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines,
                                TooltipFlag flag) {
        HamonPath path = pathOn(stack);
        if (path != null) {
            lines.add(Component.translatable("item.jojoha.path_scroll.path", path.displayName())
                    .withStyle(ChatFormatting.GOLD));
        }
        if (path != null && path.teacher() != null) {
            lines.add(Component.translatable("item.jojoha.path_scroll.teacher", path.teacher())
                    .withStyle(ChatFormatting.GRAY));
        }
        lines.add(Component.translatable("item.jojoha.path_scroll.use")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!(level instanceof ServerLevel)
                || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        JojohaPlayerData data = PlayerDataAccess.get(serverPlayer);

        if (HamonPaths.isMode(pathOn(stack))) {
            return readMode(serverPlayer, data, stack, pathOn(stack));
        }

        if (Fate.barredFromHamon(data)) {
            serverPlayer.displayClientMessage(
                    Component.translatable("message.jojoha.path_scroll.undead"), true);
            return InteractionResultHolder.fail(stack);
        }

        if (Anubis.is(data)) {
            serverPlayer.displayClientMessage(
                    Component.translatable("message.jojoha.path_scroll.anubis"), true);
            return InteractionResultHolder.fail(stack);
        }

        HamonPath path = pathOn(stack);
        if (path == null) {
            path = HamonPaths.random(serverPlayer.getRandom());
        }

        if (data.hasHamonPath(path.id())) {
            serverPlayer.displayClientMessage(
                    Component.translatable("message.jojoha.path_scroll.known", path.displayName()),
                    true);
            return InteractionResultHolder.fail(stack);
        }

        boolean first = data.discipline != Discipline.HAMON;
        data.discipline = Discipline.HAMON;
        data.grantHamonPath(path.id());

        PlayerDataAccess.set(serverPlayer, data);
        PlayerDataAccess.sync(serverPlayer);

        JojohaAdvancements.award(serverPlayer, "part2/path_" + path.id().getPath());
        if (HamonPaths.all().stream().allMatch(known -> data.hasHamonPath(known.id()))) {
            JojohaAdvancements.award(serverPlayer, "part2/every_path");
        }

        serverPlayer.displayClientMessage(Component.translatable(
                first ? "message.jojoha.path_scroll.learned_first" : "message.jojoha.path_scroll.learned",
                path.displayName()), false);
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP,
                SoundSource.PLAYERS, 0.7F, 1.3F);

        stack.consume(1, player);
        return InteractionResultHolder.success(stack);
    }

    private static InteractionResultHolder<ItemStack> readMode(ServerPlayer player, JojohaPlayerData data,
                                                             ItemStack stack, HamonPath mode) {
        if (!Fate.pillar(data)) {
            player.displayClientMessage(Component.translatable("message.jojoha.path_scroll.not_pillar"), true);
            return InteractionResultHolder.fail(stack);
        }
        if (data.hasHamonPath(mode.id())) {
            player.displayClientMessage(
                    Component.translatable("message.jojoha.path_scroll.mode_known", mode.displayName()), true);
            return InteractionResultHolder.fail(stack);
        }

        data.grantHamonPath(mode.id());
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);

        player.displayClientMessage(Component.translatable("message.jojoha.path_scroll.mode_learned",
                mode.displayName()), false);
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP,
                SoundSource.PLAYERS, 0.7F, 0.8F);

        stack.consume(1, player);
        return InteractionResultHolder.success(stack);
    }
}
