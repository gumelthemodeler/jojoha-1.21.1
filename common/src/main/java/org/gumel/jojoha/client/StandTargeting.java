package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import org.gumel.jojoha.stand.StandAim;

public final class StandTargeting {
    public static final double TARGET_RANGE = StandAim.DEFAULT_REACH;

    private StandTargeting() {
    }

    public static LivingEntity lookTarget() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return null;
        }

        return StandAim.lookTarget(minecraft.player, TARGET_RANGE);
    }
}
