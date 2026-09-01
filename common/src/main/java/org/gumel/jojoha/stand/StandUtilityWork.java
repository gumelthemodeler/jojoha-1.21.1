package org.gumel.jojoha.stand;

import dev.architectury.event.events.common.TickEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The Stand as a worker that travels, rather than a tool with a reach.
 *
 * <p>The reach was the wrong shape for this. A number that says how far the Stand can act from
 * where it hovers makes every job a question about whether you are standing close enough, which is
 * the opposite of what having a Stand should feel like - and it put a hard edge on the world at
 * eight blocks with nothing on the far side of it. So there is no reach in Utility. You point at
 * something, the Stand goes there, does it, and stays out until you are done.
 *
 * <h2>The shape of a job</h2>
 *
 * <p>Aim is captured at the click and never re-read. That matters more than it looks: the Stand
 * takes time to cross the distance, and a player laying a row is already looking at the next spot
 * before the last block lands. Re-tracing on arrival would put every block one aim behind.
 *
 * <p>What actually runs on arrival is the ordinary delegated use - see {@link StandHands}. The only
 * difference is where the Stand is standing when it happens, which is the entire point: by then it
 * is at the block, so an item that raycasts for itself inside {@code use} (a bucket) finds the same
 * cell the player was pointing at rather than something back where they stood.
 *
 * <h2>Coming home</h2>
 *
 * <p>Two ways, and both matter. Leaving Utility is the deliberate one. Going quiet for a while is
 * the one that saves the player from having to think about it - a Stand left standing in a hole
 * because its user wandered off is a bug report, not a feature.
 */
public final class StandUtilityWork {
    /**
     * How far from the target the Stand stands to work.
     *
     * <p>Backed off along the face it is placing against, so it is beside the block rather than
     * inside the space the block is about to occupy - and so the player can see it working instead
     * of seeing a block appear out of a Stand-shaped silhouette.
     */
    private static final double WORK_STANDOFF = 2.1;

    /** As close as it will get. Below this it is inside the block it is working on. */
    private static final double MIN_STANDOFF = 1.0;

    /** Lifted so it hovers at the block rather than standing on the floor beside it. */
    private static final double WORK_HEIGHT = 0.35;

    /**
     * How long the Stand waits, doing nothing, before it comes back on its own.
     *
     * <p>Long enough to line up the next block without it leaving mid-thought; short enough that
     * walking away ends the session without a ceremony. Five seconds.
     */
    private static final int IDLE_RETURN_TICKS = 100;

    /**
     * How long a job may take before it is written off.
     *
     * <p>The Stand can be prevented from arriving - the anchor ends up inside terrain, or the
     * target is somewhere it cannot reach in a straight line. Without this the job would sit in the
     * queue for ever and every later click would land behind it.
     */
    private static final int JOB_TIMEOUT_TICKS = 60;

    private static final Map<UUID, Job> JOBS = new HashMap<>();
    private static final Map<UUID, Integer> IDLE = new HashMap<>();

    private StandUtilityWork() {
    }

    public static void init() {
        TickEvent.PLAYER_POST.register(StandUtilityWork::tick);
    }

    /**
     * One queued job: where to act, and with what.
     *
     * <p>The hit result is stored whole rather than as a position, because the face is half of what
     * a placement means - clicking the top of a block and the side of it put the block in different
     * cells, and the item is the thing that knows that.
     */
    /**
     * A run of cells to fill, and how far through it the Stand is.
     *
     * <p>A list rather than one cell, because a stretched row is one decision the player made once
     * and should not have to keep making. The Stand walks it: place, move to the next, place. That
     * is slower than filling them all at the instant it arrives, and it is the right kind of slow -
     * the row appears at the speed something is building it.
     */
    private record Job(List<BlockPos> cells, Direction face, InteractionHand hand, int index, int age) {
        BlockPos current() {
            return cells.get(index);
        }

        BlockHitResult hit() {
            return new BlockHitResult(Vec3.atCenterOf(current()), face, current(), false);
        }

        boolean finished() {
            return index >= cells.size();
        }

        Job next() {
            return new Job(cells, face, hand, index + 1, 0);
        }

        Job older() {
            return new Job(cells, face, hand, index, age + 1);
        }
    }

    /** Whether a job is already in flight, so a second click does not jump the queue. */
    public static boolean isBusy(ServerPlayer player) {
        return JOBS.containsKey(player.getUUID());
    }

    /**
     * Takes a job and sends the Stand.
     *
     * <p>Sneaking continues the previous run instead of starting a new one - see {@link #extend}.
     */
    public static void queue(ServerPlayer player, StandEntity stand, List<BlockPos> cells,
                             Direction face, InteractionHand hand) {
        if (cells.isEmpty()) {
            return;
        }

        Job job = new Job(cells, face, hand, 0, 0);
        JOBS.put(player.getUUID(), job);
        IDLE.put(player.getUUID(), 0);
        stand.sendToWork(anchorFor(player, job.hit()));
    }

    /**
     * Where the Stand stands to do the job: between the block and its user, in open air.
     *
     * <p>Backing toward the player rather than straight out along the clicked face, which is what
     * this did first and what made building out from a pillar so awkward. The face normal points
     * wherever the click landed, so working along a wall put the Stand flat against it - between
     * the player and the block, covering the very thing they were trying to aim at, and sometimes
     * inside the geometry it was meant to be standing off from.
     *
     * <p>Toward the player is the one direction that is guaranteed to be the side the player is
     * looking from, so the Stand is never in front of its own work. It walks outward from the
     * block and takes the furthest spot that is actually open, so a tight shaft gets a close hover
     * and open ground gets a comfortable one.
     */
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

    /** Whether a Stand could hover here without being buried in something. */
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

        // Out of the stance, or out of a Stand. Either way there is nothing to come home.
        if (stand == null || !data.standMode.handlesItems()) {
            release(player, stand);
            return;
        }

        Job job = JOBS.get(player.getUUID());
        if (job == null) {
            tickIdle(player, stand);
            return;
        }

        if (stand.hasArrivedAtWork()) {
            IDLE.put(player.getUUID(), 0);
            StandHands.performQueued(player, stand, job.hand(), job.hit());

            Job remaining = job.next();
            if (remaining.finished()) {
                JOBS.remove(player.getUUID());
            } else {
                JOBS.put(player.getUUID(), remaining);
                stand.sendToWork(anchorFor(player, remaining.hit()));
            }
            return;
        }

        // Not there yet, and possibly never going to be.
        if (job.age() >= JOB_TIMEOUT_TICKS) {
            JOBS.remove(player.getUUID());
            IDLE.put(player.getUUID(), 0);
            return;
        }
        JOBS.put(player.getUUID(), job.older());
    }

    private static void tickIdle(ServerPlayer player, StandEntity stand) {
        if (!stand.isWorking()) {
            return;
        }

        int idle = IDLE.merge(player.getUUID(), 1, Integer::sum);
        if (idle >= IDLE_RETURN_TICKS) {
            release(player, stand);
        }
    }

    /** Ends the session: the Stand comes back and the run is forgotten. */
    public static void release(ServerPlayer player, StandEntity stand) {
        JOBS.remove(player.getUUID());
        IDLE.remove(player.getUUID());
        if (stand != null) {
            stand.clearWork();
        }
    }

    /** Forgets a player entirely - on logout, or when their Stand goes away under them. */
    public static void forget(UUID playerId) {
        JOBS.remove(playerId);
        IDLE.remove(playerId);
    }
}
