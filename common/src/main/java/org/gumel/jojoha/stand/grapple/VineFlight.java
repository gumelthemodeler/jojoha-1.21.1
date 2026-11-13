package org.gumel.jojoha.stand.grapple;

import net.minecraft.server.level.ServerPlayer;
import org.gumel.jojoha.mixin.FloatingKickAccessor;

public final class VineFlight {

    private VineFlight() {
    }

    public static void heldUp(ServerPlayer player) {
        if (player.connection instanceof FloatingKickAccessor counter) {
            counter.jojoha$setAboveGroundTickCount(0);
        }
    }
}
