package org.gumel.jojoha.stand;

import dev.architectury.event.events.common.TickEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.network.NetworkHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class Standoff {
    public static final double RANGE = 24.0;
    public static final double FACING = 0.85;
    public static final int PULSES = 4;
    public static final int PULSE_EVERY_TICKS = 20;
    public static final int PAIR_COOLDOWN_TICKS = 1200;

    private static final List<Pair> RUNNING = new ArrayList<>();
    private static final Map<String, Long> LAST = new HashMap<>();

    private record Pair(ServerPlayer one, ServerPlayer other, int[] pulses, int[] delay) {
    }

    private Standoff() {
    }

    public static void init() {
        TickEvent.SERVER_POST.register(server -> RUNNING.removeIf(Standoff::advance));
    }

    public static void summoned(ServerPlayer player) {
        for (ServerPlayer other : player.serverLevel().players()) {
            if (other == player || !eligible(other) || other.distanceToSqr(player) > RANGE * RANGE
                    || !facing(player, other) || !facing(other, player) || !player.hasLineOfSight(other)) {
                continue;
            }
            String key = key(player.getUUID(), other.getUUID());
            long now = player.serverLevel().getGameTime();
            Long last = LAST.get(key);
            if (last != null && now - last < PAIR_COOLDOWN_TICKS) {
                continue;
            }
            LAST.put(key, now);
            RUNNING.add(new Pair(player, other, new int[]{PULSES}, new int[]{0}));
        }
    }

    private static boolean advance(Pair pair) {
        if (!eligible(pair.one) || !eligible(pair.other)) {
            return true;
        }
        if (--pair.delay[0] > 0) {
            return false;
        }
        NetworkHandler.broadcastMenace(pair.one);
        NetworkHandler.broadcastMenace(pair.other);
        pair.delay[0] = PULSE_EVERY_TICKS;
        return --pair.pulses[0] <= 0;
    }

    private static boolean eligible(ServerPlayer player) {
        if (player.isRemoved() || !player.isAlive() || player.isSpectator()) {
            return false;
        }
        JojohaPlayerData data = PlayerDataAccess.get(player);
        return data.stand.isPresent() && data.standSummoned;
    }

    private static boolean facing(ServerPlayer from, ServerPlayer to) {
        Vec3 toward = to.getEyePosition().subtract(from.getEyePosition());
        if (toward.lengthSqr() < 1.0E-6) {
            return false;
        }
        return from.getLookAngle().dot(toward.normalize()) >= FACING;
    }

    private static String key(UUID a, UUID b) {
        return a.compareTo(b) < 0 ? a + "|" + b : b + "|" + a;
    }

    public static void forget(UUID player) {
        String id = player.toString();
        LAST.keySet().removeIf(key -> key.contains(id));
        RUNNING.removeIf(pair -> pair.one.getUUID().equals(player) || pair.other.getUUID().equals(player));
    }
}
