package org.gumel.jojoha.combat;

import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class M1String {
    public static final int LENGTH = 4;
    public static final int RESET_TICKS = 24;
    public static final int ENDER_GAP_TICKS = 20;
    public static final float ENDER_DAMAGE = 1.25F;
    public static final double ENDER_KNOCKBACK = 1.1;
    public static final double ENDER_LIFT = 0.35;

    private static final Map<UUID, Step> STEPS = new HashMap<>();

    private M1String() {
    }

    public static int swing(ServerPlayer player) {
        long now = player.level().getGameTime();
        Step last = STEPS.get(player.getUUID());
        int at = last == null || now - last.at > RESET_TICKS || last.count >= LENGTH ? 1 : last.count + 1;
        STEPS.put(player.getUUID(), new Step(at, now));
        return at;
    }

    public static boolean ender(int at) {
        return at >= LENGTH;
    }

    public static int gap(int at, int ticks, int base) {
        return ender(at) ? Math.max(ticks, Math.round(ticks * (float) ENDER_GAP_TICKS / Math.max(1, base))) : ticks;
    }

    private record Step(int count, long at) {
    }
}
