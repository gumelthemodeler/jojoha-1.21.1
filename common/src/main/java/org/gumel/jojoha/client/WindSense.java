package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.gumel.jojoha.data.ClientPlayerDataCache;
import org.gumel.jojoha.pillar.PillarMen;
import org.gumel.jojoha.stand.StandEntity;

public final class WindSense {
    private static final int COLOUR = 0xCFEFE4;

    private WindSense() {
    }

    public static boolean reveals(Entity entity) {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null
                && entity != minecraft.player
                && entity instanceof LivingEntity
                && !(entity instanceof StandEntity)
                && minecraft.player.isShiftKeyDown()
                && PillarMen.windMode(ClientPlayerDataCache.data)
                && entity.distanceToSqr(minecraft.player) <= PillarMen.WIND_SENSE_RANGE * PillarMen.WIND_SENSE_RANGE;
    }

    public static int colour(Entity entity) {
        return reveals(entity) ? COLOUR : -1;
    }
}
