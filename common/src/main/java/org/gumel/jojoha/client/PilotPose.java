package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.gumel.jojoha.stand.StandEntity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The pose a pilot's body holds while they are not in it.
 *
 * <h2>Why the body was spinning</h2>
 *
 * <p>Flying a Stand puts the camera on the Stand and leaves the body standing where it was. The
 * mouse still turns the <em>player</em> though - that is not incidental, it is the steering: the
 * Stand takes its heading from {@code player.getYRot()} every tick, so the pilot's rotation is the
 * control stick. Freezing it would ground the Stand.
 *
 * <p>What was wrong was only ever the picture. A body left behind pirouetting on the spot as its
 * owner banks around a valley reads as a glitch, and it is the one part of piloting that can be
 * fixed without touching how it flies: keep the rotation, stop drawing it.
 *
 * <h2>Remembered per viewer rather than synced</h2>
 *
 * <p>Nothing new goes over the wire. Each client watches for a Stand it can already see turning
 * piloted, and notes down its owner's rotation at that moment - the same moment, give or take the
 * tick their copies arrive on. Two viewers might disagree by a degree, which is invisible on a body
 * that is standing perfectly still and is the entire reason this can be a local observation instead
 * of another packet.
 *
 * <p>Scanned once a tick rather than asked per player per frame. The question "is this player
 * piloting" has no cheap answer from the player's side - their Stand may be a hundred blocks away by
 * then, so no proximity search finds it - but one pass over the entities already being rendered
 * answers it for everybody at once.
 */
public final class PilotPose {
    /** Body yaw, head yaw and pitch, as they were when the flight began. */
    private static final Map<Integer, float[]> HELD = new HashMap<>();

    private PilotPose() {
    }

    /**
     * Notes who is flying and who has stopped. Call once per client tick.
     *
     * <p>Entries are added on the tick a Stand is first seen piloted and dropped the moment it is
     * not, so a landing puts the body back under the player's own control immediately.
     */
    public static void tick(Minecraft minecraft) {
        if (minecraft.level == null) {
            HELD.clear();
            return;
        }

        Set<Integer> flying = new HashSet<>();

        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (!(entity instanceof StandEntity stand) || !stand.isPiloted()) {
                continue;
            }

            Player owner = stand.getOwner();
            if (owner == null) {
                continue;
            }

            flying.add(owner.getId());

            // Captured once, on the tick the flight starts. Recapturing every tick would simply be
            // the spin again with extra steps.
            HELD.computeIfAbsent(owner.getId(), id -> new float[]{
                    owner.yBodyRot, owner.getYHeadRot(), owner.getXRot()});
        }

        HELD.keySet().retainAll(flying);
    }

    /** The held pose for this player, or null if they are flying nothing. */
    public static float[] of(Player player) {
        return HELD.get(player.getId());
    }

    /** Dropped with the world, since entity ids mean nothing in the next one. */
    public static void forget() {
        HELD.clear();
    }
}
