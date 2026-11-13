package org.gumel.jojoha.block;

import dev.architectury.event.events.common.PlayerEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.combat.fx.Palette;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.data.TotemBinding;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.registry.ModItems;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RespawnTotems {
    private static final Map<UUID, Raised> RAISED = new ConcurrentHashMap<>();

    private record Raised(ServerLevel level, BlockPos pos, int livesLeft) {
    }

    private RespawnTotems() {
    }

    private static final int REVIVE_PALETTE_TICKS = 40;

    public static void init() {
        PlayerEvent.PLAYER_RESPAWN.register((player, conqueredEnd, reason) -> {
            Raised raised = RAISED.remove(player.getUUID());
            if (raised == null) {
                return;
            }
            if (player instanceof ServerPlayer revived) {
                NetworkHandler.sendPalette(revived, Palette.DREAD, REVIVE_PALETTE_TICKS);
            }
            raised.level().playSound(null, raised.pos(), SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(),
                    SoundSource.BLOCKS, 1.0F, 1.0F);
            player.displayClientMessage(raised.livesLeft() > 0
                    ? Component.translatable("block.jojoha.respawn_totem.raised", lives(raised.livesLeft()))
                    : Component.translatable("block.jojoha.respawn_totem.spent"), true);
        });
    }

    private static Component lives(int lives) {
        return Component.translatable("block.jojoha.respawn_totem.lives." + Math.max(1, Math.min(lives, TotemBinding.LIVES)));
    }

    public static void wake(ServerPlayer player, ServerLevel level, BlockPos pos) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (data.totem.at(level.dimension(), pos)) {
            player.displayClientMessage(Component.translatable("block.jojoha.respawn_totem.holding",
                    lives(data.totem.lives())), true);
            return;
        }
        ItemStack key = keyIn(player);
        if (key.isEmpty()) {
            level.playSound(null, pos, SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.BLOCKS, 0.5F, 1.4F);
            player.displayClientMessage(Component.translatable("block.jojoha.respawn_totem.locked"), true);
            return;
        }
        key.consume(1, player);
        data.totem = TotemBinding.woken(level.dimension(), pos);
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
        level.playSound(null, pos, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.playSound(null, pos, SoundEvents.RESPAWN_ANCHOR_SET_SPAWN, SoundSource.BLOCKS, 1.0F, 1.0F);
        player.displayClientMessage(Component.translatable("block.jojoha.respawn_totem.woken"), true);
    }

    private static ItemStack keyIn(ServerPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack held = player.getItemInHand(hand);
            if (held.is(ModItems.TOTEM_KEY.get())) {
                return held;
            }
        }
        return ItemStack.EMPTY;
    }

    public static void broken(ServerLevel level, BlockPos pos) {
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            JojohaPlayerData data = PlayerDataAccess.get(player);
            if (data.totem.at(level.dimension(), pos)) {
                data.totem = TotemBinding.NONE;
                PlayerDataAccess.set(player, data);
                PlayerDataAccess.sync(player);
            }
        }
    }

    public static DimensionTransition respawn(ServerPlayer player, boolean conqueredEnd,
                                              DimensionTransition.PostDimensionTransition after) {
        if (conqueredEnd) {
            return null;
        }
        JojohaPlayerData data = PlayerDataAccess.get(player);
        TotemBinding totem = data.totem;
        if (!totem.holds()) {
            return null;
        }
        ResourceLocation id = ResourceLocation.tryParse(totem.dimension());
        ServerLevel level = id == null ? null
                : player.server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
        Optional<Vec3> standing = level == null || !standing(level, totem.pos()) ? Optional.empty()
                : RespawnAnchorBlock.findStandUpPosition(EntityType.PLAYER, level, totem.pos());
        if (standing.isEmpty()) {
            data.totem = TotemBinding.NONE;
            PlayerDataAccess.set(player, data);
            return null;
        }
        data.totem = totem.spent();
        PlayerDataAccess.set(player, data);
        RAISED.put(player.getUUID(), new Raised(level, totem.pos(), data.totem.lives()));

        Vec3 at = standing.get();
        float yaw = (float) Math.toDegrees(Math.atan2(totem.pos().getZ() + 0.5 - at.z, totem.pos().getX() + 0.5 - at.x)) - 90F;
        return new DimensionTransition(level, at, Vec3.ZERO, yaw, 0F, after);
    }

    private static boolean standing(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof RespawnTotemBlock
                && state.getValue(RespawnTotemBlock.HALF) == DoubleBlockHalf.LOWER;
    }
}
