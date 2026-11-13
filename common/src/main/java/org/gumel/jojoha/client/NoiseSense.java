package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Map;

public final class NoiseSense {
    private static final Map<Integer, Long> HEARD = new HashMap<>();

    private NoiseSense() {
    }

    public static void heard(int entityId, int ticks) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            HEARD.put(entityId, minecraft.level.getGameTime() + ticks);
        }
    }

    public static boolean glows(Entity entity) {
        Long until = HEARD.get(entity.getId());
        if (until == null) {
            return false;
        }
        if (entity.level().getGameTime() > until) {
            HEARD.remove(entity.getId());
            return false;
        }
        return true;
    }

    public static void clear() {
        HEARD.clear();
    }
}
