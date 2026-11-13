package org.gumel.jojoha.combat;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.gumel.jojoha.stand.StandEntity;

public final class BarragePressure {
    private static final double MARGIN = 1.5;

    private BarragePressure() {
    }

    public static boolean on(Player player) {
        if (player == null || player.level() == null) {
            return false;
        }
        double range = StandEntity.barrageReach() + MARGIN;
        AABB near = player.getBoundingBox().inflate(range);
        for (StandEntity stand : player.level().getEntitiesOfClass(StandEntity.class, near,
                other -> other.isBarraging() && other.getOwner() != player)) {
            if (stand.distanceToSqr(player) <= range * range) {
                return true;
            }
        }
        return false;
    }
}
