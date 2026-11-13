package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.gumel.jojoha.data.ClientPlayerDataCache;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.trait.TraitEffects;

public final class KeenEyes {
    private KeenEyes() {
    }

    public static boolean sees(Entity entity, Player viewer) {
        if (viewer == null || !entity.level().isClientSide() || viewer != Minecraft.getInstance().player
                || !(entity instanceof LivingEntity) || entity instanceof StandEntity || entity == viewer
                || !entity.isInvisible()) {
            return false;
        }
        return TraitEffects.keenEyes(ClientPlayerDataCache.data)
                && entity.distanceToSqr(viewer) <= TraitEffects.KEEN_EYES_RANGE * TraitEffects.KEEN_EYES_RANGE;
    }
}
