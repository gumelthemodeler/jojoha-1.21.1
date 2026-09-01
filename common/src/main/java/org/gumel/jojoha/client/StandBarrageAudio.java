package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.gumel.jojoha.stand.StandEntity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Starts a flurry's shout for any Stand that begins barraging, once each.
 *
 * <p>Scanned per tick off the entities already being rendered rather than driven by a packet. The
 * barraging flag is entity data and therefore already known to every client that can see the Stand,
 * so this needs no network traffic of its own and works the same for a bystander as for the owner -
 * which a packet sent only to the user would not.
 */
public final class StandBarrageAudio {
    /**
     * The shout each Stand currently has running, so one flurry does not start twenty of them.
     *
     * <p>The instance is kept, not just the fact of one. A flurry used to last exactly three seconds
     * against a recording of just under five, so a shout could only ever outlive its barrage - but a
     * held flurry can now run to ten, and a set of ids alone had no way to notice the recording had
     * finished halfway through. Holding the instance means the next tick can see it has stopped and
     * start the next one.
     */
    private static final Map<UUID, StandBarrageSound> PLAYING = new HashMap<>();

    private StandBarrageAudio() {
    }

    public static void clear() {
        PLAYING.clear();
    }

    /** Call once per client tick. */
    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            PLAYING.clear();
            return;
        }

        Set<UUID> barraging = null;

        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (!(entity instanceof StandEntity stand) || !stand.isBarraging()) {
                continue;
            }

            if (barraging == null) {
                barraging = new HashSet<>();
            }
            barraging.add(stand.getUUID());

            // A finished recording is forgotten here rather than left in place, so a flurry held
            // past the length of the clip picks straight up with the next one instead of going
            // quiet for the rest of it.
            StandBarrageSound running = PLAYING.get(stand.getUUID());
            if (running == null || running.isStopped()) {
                StandBarrageSound shout = new StandBarrageSound(stand);
                PLAYING.put(stand.getUUID(), shout);
                minecraft.getSoundManager().play(shout);
            }
        }

        // Forgotten as soon as the flurry ends, so the next one starts a fresh shout. The sound
        // itself is responsible for stopping - all this has to do is stop remembering.
        if (barraging == null) {
            PLAYING.clear();
        } else {
            PLAYING.keySet().retainAll(barraging);
        }
    }
}
