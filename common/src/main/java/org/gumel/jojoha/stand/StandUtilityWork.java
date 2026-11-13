package org.gumel.jojoha.stand;

import dev.architectury.event.events.common.TickEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.stand.skill.StandSkills;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class StandUtilityWork {
    private static final double WORK_STANDOFF = 2.1;

    private static final double MIN_STANDOFF = 1.0;

    private static final double WORK_HEIGHT = 0.35;

    private static final int IDLE_RETURN_TICKS = 100;

    private static final int JOB_TIMEOUT_TICKS = 60;

    private static final Map<UUID, Job> JOBS = new HashMap<>();
    private static final Map<UUID, Integer> IDLE = new HashMap<>();

    private StandUtilityWork() {
    }

    public static void init() {
        TickEvent.PLAYER_POST.register(StandUtilityWork::tick);
    }

    private record Job(List<BlockPos> cells, Direction face, Vec3 grip, int turns,
                       InteractionHand hand, int index, int age) {
        BlockPos current() {
            return cells.get(index);
        }

        BlockHitResult hit() {
            return PlacementRun.hitAt(current(), face, grip);
        }

        boolean finished() {
            return index >= cells.size();
        }

        Job next() {
            return new Job(cells, face, grip, turns, hand, index + 1, 0);
        }

        Job older() {
            return new Job(cells, face, grip, turns, hand, index, age + 1);
        }
    }

    public static boolean isBusy(ServerPlayer player) {
        return JOBS.containsKey(player.getUUID());
    }

    public static void queue(ServerPlayer player, StandEntity stand, List<BlockPos> cells,
                             Direction face, Vec3 grip, int turns, InteractionHand hand) {
        if (cells.isEmpty()) {
            return;
        }

        Job job = new Job(cells, face, grip, turns, hand, 0, 0);
        JOBS.put(player.getUUID(), job);
        IDLE.put(player.getUUID(), 0);
        stand.sendToUtilityWork(anchorFor(player, job.hit()));
    }

    private static Vec3 anchorFor(ServerPlayer player, BlockHitResult hit) {
        Vec3 target = Vec3.atCenterOf(hit.getBlockPos());
        Vec3 away = player.getEyePosition().subtract(target);
        double distance = away.length();
        Vec3 direction = distance < 1.0E-4 ? new Vec3(0, 1, 0) : away.scale(1.0 / distance);

        for (double out = WORK_STANDOFF; out >= MIN_STANDOFF; out -= 0.35) {
            Vec3 candidate = target.add(direction.scale(out)).add(0, WORK_HEIGHT, 0);
            if (isOpen(player, candidate)) {
                return candidate;
            }
        }

        return target.add(direction.scale(MIN_STANDOFF)).add(0, WORK_HEIGHT, 0);
    }

    private static boolean isOpen(ServerPlayer player, Vec3 at) {
        BlockState state = player.level().getBlockState(BlockPos.containing(at));
        return state.isAir() || !state.isSolidRender(player.level(), BlockPos.containing(at));
    }

    private static void tick(Player raw) {
        if (!(raw instanceof ServerPlayer player)) {
            return;
        }

        JojohaPlayerData data = PlayerDataAccess.get(player);
        StandEntity stand = StandSummonHandler.findStand(player, data);

        if (stand == null || !data.standMode.handlesItems()) {
            release(player, stand);
            return;
        }

        Job job = JOBS.get(player.getUUID());
        if (job == null) {
            tickIdle(player, stand);
            return;
        }

        if (!stand.isUtilityWorking()) {
            JOBS.remove(player.getUUID());
            IDLE.remove(player.getUUID());
            return;
        }

        if (stand.hasArrivedAtWork()) {
            IDLE.put(player.getUUID(), 0);

            if (!StandHands.canAfford(player)) {
                JOBS.remove(player.getUUID());
                StandSkills.notify(player,
                        "message.jojoha.skill.no_energy");
                return;
            }

            StandHands.performQueued(player, stand, job.hand(), job.hit(), job.turns());

            Job remaining = job.next();
            if (remaining.finished()) {
                JOBS.remove(player.getUUID());
            } else {
                JOBS.put(player.getUUID(), remaining);
                stand.sendToUtilityWork(anchorFor(player, remaining.hit()));
            }
            return;
        }

        if (job.age() >= JOB_TIMEOUT_TICKS) {
            JOBS.remove(player.getUUID());
            IDLE.put(player.getUUID(), 0);
            return;
        }
        JOBS.put(player.getUUID(), job.older());
    }

    private static void tickIdle(ServerPlayer player, StandEntity stand) {
        if (!stand.isUtilityWorking()) {
            IDLE.remove(player.getUUID());
            return;
        }

        int idle = IDLE.merge(player.getUUID(), 1, Integer::sum);
        if (idle >= IDLE_RETURN_TICKS) {
            release(player, stand);
        }
    }

    public static void release(ServerPlayer player, StandEntity stand) {
        JOBS.remove(player.getUUID());
        IDLE.remove(player.getUUID());
        if (stand != null) {
            stand.clearUtilityWork();
        }
    }

    public static void forget(UUID playerId) {
        JOBS.remove(playerId);
        IDLE.remove(playerId);
    }
}
