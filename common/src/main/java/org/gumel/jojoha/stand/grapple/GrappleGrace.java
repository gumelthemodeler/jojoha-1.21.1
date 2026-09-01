package org.gumel.jojoha.stand.grapple;

import dev.architectury.event.events.common.TickEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * A moment after the vine lets go in which the ground cannot hurt you.
 *
 * <p>Every one of these moves ends with the player somewhere high and moving, which is the whole
 * point of them - and then charges them for it. Swinging up onto a roof and taking six hearts on
 * landing is not a difficulty curve, it is the move disagreeing with itself, and the answer players
 * find is to stop using it near the ground. Better to say plainly that arriving is free.
 *
 * <h2>Why the server owns it</h2>
 *
 * <p>Fall damage is worked out server-side from the fall distance the server has been accumulating,
 * so clearing the client's copy achieves nothing at all - the client would look fine and the player
 * would still be hit. The reset has to happen where the number lives.
 *
 * <p>It is a reset per tick rather than a flag checked at the moment of landing, deliberately. A
 * flag has to be read by whatever applies the damage, which means finding every path into it; a
 * distance that never gets the chance to accumulate cannot produce damage by any route.
 */
public final class GrappleGrace {
    /**
     * How long the grace lasts after the vine lets go, in ticks.
     *
     * <p>Long enough to cover the drop that the move itself set up, and short enough that it is not
     * a general licence to fall. Two seconds is about the time it takes to come down from the top of
     * a zip, which is the fall this exists to forgive.
     */
    private static final int GRACE_TICKS = 40;

    private static final Map<UUID, Integer> GRACE = new HashMap<>();

    private GrappleGrace() {
    }

    public static void init() {
        TickEvent.SERVER_POST.register(server -> tick());
    }

    /** Called as a vine releases. Re-granting simply restarts the clock. */
    public static void grant(Player player) {
        if (player instanceof ServerPlayer holder) {
            GRACE.put(holder.getUUID(), GRACE_TICKS);
            holder.resetFallDistance();
        }
    }

    private static void tick() {
        if (GRACE.isEmpty()) {
            return;
        }

        Iterator<Map.Entry<UUID, Integer>> entries = GRACE.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<UUID, Integer> entry = entries.next();

            int left = entry.getValue() - 1;
            if (left <= 0) {
                entries.remove();
                continue;
            }
            entry.setValue(left);

            ServerPlayer player = player(entry.getKey());
            if (player == null) {
                entries.remove();
                continue;
            }

            // The whole mechanism. Nothing accumulates, so nothing is charged for.
            player.resetFallDistance();
        }
    }

    /** The player behind an id, on whichever level they are on, or null if they have gone. */
    private static ServerPlayer player(UUID id) {
        net.minecraft.server.MinecraftServer server =
                net.minecraft.server.MinecraftServer.class.cast(
                        dev.architectury.utils.GameInstance.getServer());
        return server == null ? null : server.getPlayerList().getPlayer(id);
    }
}
