package org.gumel.jojoha.stand.skill;

import dev.architectury.event.events.common.TickEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.speech.Speech;
import org.gumel.jojoha.stand.heaven.Heaven;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TimeStopSettle {
    public static final int INTRO_TICKS = 62;

    public static final int GLOBAL_INTRO_TICKS = 88;

    public static final int TAP_WAVE_TICKS = 30;

    private static final int COUNT_EVERY_TICKS = 20;

    private record Settle(ServerPlayer player, long began, int intro, int total) {
    }

    private static final Map<UUID, Settle> ACTIVE = new ConcurrentHashMap<>();

    private TimeStopSettle() {
    }

    public static void init() {
        TickEvent.SERVER_POST.register(server -> tick());
    }

    public static int introTicks(JojohaPlayerData data) {
        return Heaven.is(data) ? GLOBAL_INTRO_TICKS : INTRO_TICKS;
    }

    public static int introTicks(boolean global) {
        return global ? GLOBAL_INTRO_TICKS : INTRO_TICKS;
    }

    public static void begin(ServerPlayer player, int intro, int total) {
        ACTIVE.put(player.getUUID(), new Settle(player, player.level().getGameTime(), intro, total));
    }

    public static void end(Player player) {
        ACTIVE.remove(player.getUUID());
    }

    public static boolean settling(Player player) {
        Settle settle = ACTIVE.get(player.getUUID());
        return settle != null && player.level().getGameTime() - settle.began() < settle.intro();
    }

    private static void tick() {
        if (ACTIVE.isEmpty()) {
            return;
        }
        Iterator<Settle> each = ACTIVE.values().iterator();
        while (each.hasNext()) {
            Settle settle = each.next();
            ServerPlayer player = settle.player();
            JojohaPlayerData data = PlayerDataAccess.get(player);
            long elapsed = player.level().getGameTime() - settle.began();
            if (!player.isAlive() || player.isRemoved() || data.timeStopHeldTicks <= 0 || elapsed >= settle.total()) {
                each.remove();
                continue;
            }
            long counted = elapsed - settle.intro();
            if (counted >= 0 && counted % COUNT_EVERY_TICKS == 0) {
                Speech.timeCount(player, (int) (counted / COUNT_EVERY_TICKS) + 1);
            }
        }
    }
}
