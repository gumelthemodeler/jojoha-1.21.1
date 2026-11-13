package org.gumel.jojoha.mixin.client;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.gumel.jojoha.client.Clairvoyance;
import org.gumel.jojoha.client.FortuneSense;
import org.gumel.jojoha.client.HeavenWorldFx;
import org.gumel.jojoha.client.MagicianFx;
import org.gumel.jojoha.client.StandGlowTracker;
import org.gumel.jojoha.client.WindSense;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityGlowMixin {
    @Inject(method = "isCurrentlyGlowing", at = @At("HEAD"), cancellable = true)
    private void jojoha$glowWhileStandIsOut(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof Player player && StandGlowTracker.isGlowing(player.getUUID())) {
            cir.setReturnValue(true);
            return;
        }

        if (Clairvoyance.reveals((Entity) (Object) this)
                || FortuneSense.reveals((Entity) (Object) this)
                || WindSense.reveals((Entity) (Object) this)
                || MagicianFx.reveals((Entity) (Object) this)
                || HeavenWorldFx.serving((Entity) (Object) this)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
    private void jojoha$standOutlineColor(CallbackInfoReturnable<Integer> cir) {
        int revealed = Clairvoyance.colour((Entity) (Object) this);
        if (revealed >= 0) {
            cir.setReturnValue(revealed);
            return;
        }
        int sensed = FortuneSense.colour((Entity) (Object) this);
        if (sensed >= 0) {
            cir.setReturnValue(sensed);
            return;
        }
        int felt = WindSense.colour((Entity) (Object) this);
        if (felt >= 0) {
            cir.setReturnValue(felt);
            return;
        }
        int served = HeavenWorldFx.servantColour((Entity) (Object) this);
        if (served >= 0) {
            cir.setReturnValue(served);
            return;
        }
        int warm = MagicianFx.colour((Entity) (Object) this);
        if (warm >= 0) {
            cir.setReturnValue(warm);
            return;
        }

        if (!((Object) this instanceof Player player)) {
            return;
        }

        int color = StandGlowTracker.glowColor(player.getUUID());
        if (color >= 0) {
            cir.setReturnValue(color);
        }
    }
}
