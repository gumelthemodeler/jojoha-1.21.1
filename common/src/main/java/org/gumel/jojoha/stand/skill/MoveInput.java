package org.gumel.jojoha.stand.skill;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MoveInput {
    private static final Map<UUID, int[]> HELD = new ConcurrentHashMap<>();

    private MoveInput() {
    }

    public static void report(ServerPlayer player, int forward, int left) {
        HELD.put(player.getUUID(), new int[]{Mth.clamp(forward, -1, 1), Mth.clamp(left, -1, 1)});
    }

    public static void forget(UUID player) {
        HELD.remove(player);
    }

    @Nullable
    public static Vec3 heading(ServerPlayer player) {
        int[] held = HELD.get(player.getUUID());
        if (held == null) {
            return null;
        }
        if (held[0] == 0 && held[1] == 0) {
            return Vec3.ZERO;
        }
        Vec3 local = new Vec3(held[1], 0.0, held[0]).normalize();
        float yaw = player.getYRot() * Mth.DEG_TO_RAD;
        float sin = Mth.sin(yaw);
        float cos = Mth.cos(yaw);
        return new Vec3(local.x * cos - local.z * sin, 0.0, local.z * cos + local.x * sin);
    }
}
