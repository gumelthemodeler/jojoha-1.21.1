package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import org.gumel.jojoha.stand.StandAim;

/**
 * What the local player has lined up for their Stand.
 *
 * <p>A thin client-side front door onto {@link StandAim}, which owns the actual aiming. Keeping the
 * logic there rather than here is what guarantees the crosshair marker and the move that swings can
 * never disagree about what is targeted.
 */
public final class StandTargeting {
    /** How far the crosshair reaches - the Stand's own strike range. */
    public static final double TARGET_RANGE = StandAim.DEFAULT_REACH;

    private StandTargeting() {
    }

    /** The entity under the crosshair within reach, or null. */
    public static LivingEntity lookTarget() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return null;
        }

        return StandAim.lookTarget(minecraft.player, TARGET_RANGE);
    }
}
