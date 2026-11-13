package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import org.gumel.jojoha.network.packet.StandFistPacket;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.combat.StandFist;

import java.util.HashMap;
import java.util.Map;

public final class StandFistView {
    private static final Map<Integer, StandFist[]> LIVE = new HashMap<>();

    private StandFistView() {
    }

    public static void add(StandFistPacket packet) {
        StandFist fist = packet.fist();
        if (fist.slot() < 0 || fist.slot() >= StandFist.SLOTS) {
            return;
        }
        LIVE.computeIfAbsent(packet.standId(), id -> new StandFist[StandFist.SLOTS])[fist.slot()] = fist;
    }

    public static double age(StandFist fist, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level == null ? Double.MAX_VALUE
                : minecraft.level.getGameTime() + partialTick - fist.spawnTick();
    }

    public static StandFist live(StandEntity stand, int slot, float partialTick) {
        StandFist[] fists = LIVE.get(stand.getId());
        if (fists == null || slot < 0 || slot >= fists.length) {
            return null;
        }
        StandFist fist = fists[slot];
        double age = fist == null ? 0 : age(fist, partialTick);
        if (fist != null && (fist.finished(age) || age < -StandFist.OUT_TICKS)) {
            fists[slot] = null;
            return null;
        }
        return fist;
    }

    public static boolean anyLive(StandEntity stand, float partialTick) {
        StandFist[] fists = LIVE.get(stand.getId());
        if (fists == null) {
            return false;
        }
        boolean any = false;
        for (int slot = 0; slot < fists.length; slot++) {
            any |= live(stand, slot, partialTick) != null;
        }
        if (!any) {
            LIVE.remove(stand.getId());
        }
        return any;
    }

    public static void clear() {
        LIVE.clear();
    }
}
