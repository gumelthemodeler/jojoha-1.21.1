package org.gumel.jojoha.client.fx;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class HeavenHold {
    private HeavenHold() {
    }

    public static void hold(Entity entity, int ms) {
        if (entity != null) {
            HitStop.begin(entity, ms, Vec3.ZERO);
        }
    }
}
