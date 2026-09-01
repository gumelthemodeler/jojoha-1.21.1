package org.gumel.jojoha.stand;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.registry.ModRegistries;

/**
 * The aura clinging to a player while their Stand is out.
 *
 * <p>Motes are born inside the player's silhouette and pushed outward, so the aura looks like it
 * is leaking out of them rather than orbiting them. Successive spawns leave at a golden-angle
 * offset from one another, which keeps the emission spread evenly around the body without ever
 * settling into a visible ring.
 *
 * <p>Deliberately never sent to the owning player themselves - see
 * {@code org.gumel.jojoha.client.LocalStandAuraEffect}, which spawns the owner's own copy purely
 * client-side so it can be skipped in first person (you don't want your own aura cluttering your
 * own view, but everyone else should still see it on you).
 */
public final class StandAuraEffect {
    private static final int SPAWN_INTERVAL_TICKS = 1;
    /** Enough at once to read as a continuous sheet of flame rather than countable sparks. */
    private static final int MOTES_PER_SPAWN = 4;

    /**
     * Motes are born <em>inside</em> the body, within this radius of the player's centre line, and
     * pushed outward from there.
     *
     * <p>That's the whole difference between an aura and a halo. Spawning on a ring at arm's length
     * draws a shell around the player that reads as a separate object they happen to be standing
     * in; starting them within the silhouette means the model occludes them for their first frames
     * and they appear to seep out of the person, which is what makes the energy look like theirs.
     */
    private static final double EMIT_RADIUS = 0.32;
    /** Spans the whole body, ankles to just over the head, so the flame sheathes all of them. */
    private static final double EMIT_MIN_HEIGHT = 0.05;
    private static final double EMIT_HEIGHT_RANGE = 1.85;

    /**
     * Angle advanced between consecutive motes, in radians. Near the golden angle so successive
     * motes leave from all around the body instead of clumping on one side the way a neat fraction
     * of a turn does.
     */
    private static final double EMIT_ANGLE_STEP = 2.39996;

    private StandAuraEffect() {
    }

    /** Called every server tick a player has their Stand cast - internally throttled. */
    public static void tick(ServerPlayer player) {
        if (player.tickCount % SPAWN_INTERVAL_TICKS != 0) {
            return;
        }

        ServerLevel level = player.serverLevel();
        RandomSource random = level.getRandom();

        // Asked once, not once per mote. This is a data fetch and a registry lookup, and the
        // answer cannot change between two motes of the same spawn.
        boolean bound = org.gumel.jojoha.stand.StandTypes
                .byIdOrDefault(org.gumel.jojoha.data.PlayerDataAccess.get(player).stand.standId())
                .form().isBound();

        for (int i = 0; i < MOTES_PER_SPAWN; i++) {
            int seed = player.tickCount * MOTES_PER_SPAWN + i;

            // A bound Stand emits from where it actually is - see handOffset.
            Vec3 offset = bound
                    ? handOffset(seed, random, player.yBodyRot)
                    : auraOffset(seed, random);
            double x = player.getX() + offset.x;
            double y = player.getY() + offset.y;
            double z = player.getZ() + offset.z;

            // Every OTHER nearby player, deliberately excluding the owner - see class javadoc.
            for (Player other : level.players()) {
                if (other == player) {
                    continue;
                }
                // count=0 sends exactly one particle at this exact position. Velocity is sent as
                // zero because StandAuraParticle ignores it outright - it tracks its owner instead
                // of coasting, which is what keeps the aura welded to a moving player.
                level.sendParticles((ServerPlayer) other, ModRegistries.STAND_AURA.get(), false,
                        x, y, z, 0, 0.0, 0.0, 0.0, 0.0);
            }
        }
    }

    /**
     * Where a mote is born, relative to the player's feet - a point inside the body rather than on
     * a ring around it. Shared with the owner's client-side copy so both views agree.
     */
    public static Vec3 auraOffset(int index, RandomSource random) {
        double angle = index * EMIT_ANGLE_STEP;
        // Square-rooted so points spread evenly through the disc instead of bunching at the centre.
        double radius = Math.sqrt(random.nextDouble()) * EMIT_RADIUS;
        double height = EMIT_MIN_HEIGHT + random.nextDouble() * EMIT_HEIGHT_RANGE;

        return new Vec3(Math.cos(angle) * radius, height, Math.sin(angle) * radius);
    }

    /**
     * Where a mote is born for a Stand that is only a pair of arms.
     *
     * <p>The ordinary offset fills the whole body, which is right for a figure standing beside you
     * and wrong for Hermit Purple - the aura came out of the user's chest and legs, where there is
     * no Stand at all, and none of it came off the vines.
     *
     * <p>Two clusters at shoulder height instead, alternating left and right so both arms are lit
     * rather than one. Rotated with the body, so walking about does not leave the aura hanging off
     * the wrong side.
     *
     * @param bodyYaw the wearer's body rotation in degrees
     */
    public static Vec3 handOffset(int index, RandomSource random, float bodyYaw) {
        double yaw = Math.toRadians(bodyYaw);

        // The body's own right, and the sign that picks which arm this mote belongs to.
        double acrossX = Math.cos(yaw);
        double acrossZ = Math.sin(yaw);
        double side = (index % 2 == 0 ? 1 : -1) * HAND_REACH;

        double scatter = HAND_SCATTER;
        return new Vec3(
                acrossX * side + (random.nextDouble() - 0.5) * scatter,
                HAND_HEIGHT + (random.nextDouble() - 0.5) * scatter,
                acrossZ * side + (random.nextDouble() - 0.5) * scatter);
    }

    /** How far out the hands sit, how high, and how loosely the motes gather round them. */
    private static final double HAND_REACH = 0.42;
    private static final double HAND_HEIGHT = 0.98;
    private static final double HAND_SCATTER = 0.28;

    public static int motesPerSpawn() {
        return MOTES_PER_SPAWN;
    }

    public static int spawnIntervalTicks() {
        return SPAWN_INTERVAL_TICKS;
    }
}
