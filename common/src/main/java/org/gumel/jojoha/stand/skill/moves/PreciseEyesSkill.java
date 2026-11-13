package org.gumel.jojoha.stand.skill.moves;

import dev.architectury.event.events.common.TickEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.StandSkill;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class PreciseEyesSkill implements StandSkill {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "precise_eyes");

    public static final PreciseEyesSkill INSTANCE = new PreciseEyesSkill();

    private static final int COOLDOWN_TICKS = 10;

    private static final Set<UUID> FOCUSED = new HashSet<>();

    private PreciseEyesSkill() {
    }

    public static void init() {
        TickEvent.PLAYER_POST.register(player -> {
            if (player instanceof ServerPlayer serverPlayer && FOCUSED.contains(player.getUUID())
                    && (!player.isAlive() || !PlayerDataAccess.get(serverPlayer).inUtilityStance())) {
                FOCUSED.remove(player.getUUID());
                NetworkHandler.sendPreciseEyes(serverPlayer, false);
            }
        });
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "skill.jojoha.precise_eyes";
    }

    @Override
    public boolean isUnlocked(JojohaPlayerData data) {
        return data.stand.isPresent();
    }

    @Override
    public int cooldownTicks() {
        return COOLDOWN_TICKS;
    }

    @Override
    public float energyCost() {
        return EnergyWeight.TRIVIAL.cost();
    }

    @Override
    public boolean activate(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        boolean on = !FOCUSED.remove(player.getUUID());
        if (on) {
            FOCUSED.add(player.getUUID());
        }
        NetworkHandler.sendPreciseEyes(player, on);
        player.serverLevel().playSound(null, player.blockPosition(),
                on ? SoundEvents.SPYGLASS_USE : SoundEvents.SPYGLASS_STOP_USING, SoundSource.PLAYERS, 1F, 1F);
        return true;
    }

    public static boolean focused(Player player) {
        return FOCUSED.contains(player.getUUID());
    }

    public static Runnable steady(ServerLevel level, BlockPos cell) {
        Map<BlockPos, BlockState> resting = new HashMap<>();
        for (Direction side : Direction.values()) {
            BlockPos next = cell.relative(side);
            BlockState state = level.getBlockState(next);
            if (!state.isAir() && !state.hasBlockEntity()
                    && !state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
                resting.put(next, state);
            }
        }
        return () -> {
            for (Map.Entry<BlockPos, BlockState> entry : resting.entrySet()) {
                BlockPos pos = entry.getKey();
                BlockState now = level.getBlockState(pos);
                if (now == entry.getValue() || !now.canBeReplaced()) {
                    continue;
                }
                level.setBlock(pos, entry.getValue(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, new AABB(pos),
                        item -> item.tickCount == 0)) {
                    item.discard();
                }
            }
        };
    }
}
