package org.gumel.jojoha.stand;

import dev.architectury.event.events.common.TickEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class DiscToss {
    private static final double SPEED = 1.1;
    private static final double LIFT = 0.08;
    private static final int PICKUP_DELAY = 20;
    private static final int TRACK_TICKS = 100;
    private static final double STEP = 0.25;
    private static final double CATCH = 0.6;

    private static final List<Flight> FLYING = new ArrayList<>();

    private DiscToss() {
    }

    public static void init() {
        TickEvent.SERVER_POST.register(server -> FLYING.removeIf(flight -> flight.landed(server)));
    }

    public static boolean plays(ItemStack stack) {
        return stack.has(DataComponents.JUKEBOX_PLAYABLE);
    }

    public static void toss(ServerPlayer player, StandEntity stand, InteractionHand hand, double reach) {
        ServerLevel level = player.serverLevel();
        BlockHitResult hit = StandHands.aimFrom(player, player.getEyePosition(), player.getViewVector(1.0F), reach);
        boolean struck = hit.getType() == HitResult.Type.BLOCK;
        Vec3 target = struck ? hit.getLocation()
                : player.getEyePosition().add(player.getViewVector(1.0F).scale(reach));
        Vec3 from = stand.position().add(0, stand.getBbHeight() * 0.7, 0);
        Vec3 heading = target.subtract(from);
        heading = heading.lengthSqr() < 1.0E-4 ? player.getViewVector(1.0F) : heading.normalize();

        ItemStack disc = player.getItemInHand(hand).split(1);
        ItemEntity item = new ItemEntity(level, from.x, from.y, from.z, disc);
        item.setDeltaMovement(heading.scale(SPEED).add(0, LIFT, 0));
        item.setPickUpDelay(PICKUP_DELAY);
        item.setThrower(player);
        level.addFreshEntity(item);
        FLYING.add(new Flight(item, player.getUUID(), struck && jukebox(level, hit.getBlockPos()) ? hit.getBlockPos() : null));

        stand.reachTowards(target);
        stand.triggerThrowPose();
        level.playSound(null, player.blockPosition(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.8F, 0.6F);
    }

    private static boolean jukebox(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.hasProperty(JukeboxBlock.HAS_RECORD) && !state.getValue(JukeboxBlock.HAS_RECORD)
                && level.getBlockEntity(pos) instanceof JukeboxBlockEntity;
    }

    private static final class Flight {
        private final ItemEntity item;
        private final UUID thrower;
        private final BlockPos aimed;
        private Vec3 last;
        private int left = TRACK_TICKS;

        private Flight(ItemEntity item, UUID thrower, BlockPos aimed) {
            this.item = item;
            this.thrower = thrower;
            this.aimed = aimed;
            this.last = item.position();
        }

        private boolean landed(MinecraftServer server) {
            if (!item.isAlive() || --left <= 0 || !(item.level() instanceof ServerLevel level)) {
                return true;
            }
            ServerPlayer player = server.getPlayerList().getPlayer(thrower);
            if (player == null) {
                return true;
            }
            Vec3 now = item.position();
            for (BlockPos pos : passed(now)) {
                if (jukebox(level, pos) && level.mayInteract(player, pos) && insert(level, pos, player)) {
                    return true;
                }
            }
            last = now;
            return item.onGround() && item.getDeltaMovement().horizontalDistanceSqr() < 1.0E-3;
        }

        private Set<BlockPos> passed(Vec3 now) {
            Set<BlockPos> near = new LinkedHashSet<>();
            if (aimed != null && new AABB(aimed).inflate(CATCH).intersects(item.getBoundingBox().expandTowards(last.subtract(now)))) {
                near.add(aimed);
            }
            int steps = Math.max(1, (int) Math.ceil(now.distanceTo(last) / STEP));
            for (int i = 0; i <= steps; i++) {
                BlockPos at = BlockPos.containing(last.lerp(now, i / (double) steps));
                near.add(at);
                for (Direction side : Direction.values()) {
                    near.add(at.relative(side));
                }
            }
            return near;
        }

        private boolean insert(ServerLevel level, BlockPos pos, ServerPlayer player) {
            if (!(level.getBlockEntity(pos) instanceof JukeboxBlockEntity jukebox)) {
                return false;
            }
            BlockState state = level.getBlockState(pos);
            jukebox.setTheItem(item.getItem().copyWithCount(1));
            level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, state));
            player.awardStat(Stats.PLAY_RECORD);
            item.discard();
            return true;
        }
    }
}
