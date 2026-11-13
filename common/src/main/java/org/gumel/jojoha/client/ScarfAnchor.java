package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.combat.ScarfPhysics;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ScarfAnchor {
    private static final long FRESH_NANOS = 150_000_000L;
    private static final long OFFSET_NANOS = 500_000_000L;
    private static final double MAX_DRIFT = 1.0;

    private record Seen(Vec3 at, long nanos) {
    }

    private static final Map<UUID, Seen> SEEN = new HashMap<>();
    private static final Map<UUID, Seen> OFFSETS = new HashMap<>();

    private ScarfAnchor() {
    }

    public static void seen(Player player, Vector3f cameraRelative) {
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        SEEN.put(player.getUUID(), new Seen(camera.add(cameraRelative.x(), cameraRelative.y(), cameraRelative.z()),
                System.nanoTime()));
    }

    public static Vec3 resolve(Player player, float partialTick) {
        Vec3 body = ScarfPhysics.anchor(player, partialTick);
        long now = System.nanoTime();
        Seen seen = SEEN.get(player.getUUID());
        Vec3 at = seen != null && now - seen.nanos() < FRESH_NANOS && seen.at().distanceTo(body) < MAX_DRIFT
                ? seen.at() : body;
        OFFSETS.put(player.getUUID(), new Seen(at.subtract(player.getPosition(partialTick)), now));
        return at;
    }

    public static Vec3 forTick(Player player) {
        Seen offset = OFFSETS.get(player.getUUID());
        if (offset == null || System.nanoTime() - offset.nanos() > OFFSET_NANOS) {
            return null;
        }
        return player.position().add(offset.at());
    }

    public static void forget(UUID player) {
        SEEN.remove(player);
        OFFSETS.remove(player);
    }

    public static void clear() {
        SEEN.clear();
        OFFSETS.clear();
    }
}
