package org.gumel.jojoha.mixin.client;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.gumel.jojoha.client.StandGlowTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Outlines a player in their Stand's colour while it's manifested.
 *
 * <p>Rides vanilla's existing glow pipeline rather than adding a render pass: {@code LevelRenderer}
 * already re-routes any entity reporting {@code isCurrentlyGlowing()} through the outline buffer,
 * and already tints that outline with whatever {@code getTeamColor()} returns. Answering those two
 * questions differently is the entire feature - the silhouette extraction, the post-processing and
 * the depth handling are all vanilla's, so it looks native and costs nothing extra.
 *
 * <p>Both hooks are client-only in effect: the colour comes from {@link StandGlowTracker}, which is
 * only ever populated on a client, so a server evaluating these falls straight through to vanilla.
 */
@Mixin(Entity.class)
public abstract class EntityGlowMixin {
    @Inject(method = "isCurrentlyGlowing", at = @At("HEAD"), cancellable = true)
    private void jojoha$glowWhileStandIsOut(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof Player player && StandGlowTracker.isGlowing(player.getUUID())) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
    private void jojoha$standOutlineColor(CallbackInfoReturnable<Integer> cir) {
        if (!((Object) this instanceof Player player)) {
            return;
        }

        int color = StandGlowTracker.glowColor(player.getUUID());
        if (color >= 0) {
            cir.setReturnValue(color);
        }
    }
}
