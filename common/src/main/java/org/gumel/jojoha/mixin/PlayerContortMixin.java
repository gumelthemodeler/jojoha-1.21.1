package org.gumel.jojoha.mixin;

import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.gumel.jojoha.body.Body;
import org.gumel.jojoha.combat.Climbing;
import org.gumel.jojoha.pillar.moves.ContortionMove;
import org.gumel.jojoha.vampire.Headless;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerContortMixin {
    @Unique
    private static final EntityDimensions JOJOHA$SQUEEZED = EntityDimensions.scalable(ContortionMove.SQUEEZE_WIDTH,
            ContortionMove.SQUEEZE_HEIGHT).withEyeHeight(ContortionMove.SQUEEZE_EYES);

    @Inject(method = "updatePlayerPose", at = @At("HEAD"), cancellable = true)
    private void jojoha$contort(CallbackInfo ci) {
        Player player = (Player) (Object) this;
        if ((Climbing.contorting(player) || Body.crawling(player)) && !player.isSpectator() && !player.isPassenger()) {
            player.setPose(Pose.SWIMMING);
            ci.cancel();
        }
    }

    @Inject(method = "getDefaultDimensions", at = @At("HEAD"), cancellable = true)
    private void jojoha$squeeze(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        Player player = (Player) (Object) this;
        if (Headless.is(player)) {
            cir.setReturnValue(Headless.HEAD_SIZE);
            return;
        }
        if (pose == Pose.SWIMMING && Climbing.contorting(player)) {
            cir.setReturnValue(JOJOHA$SQUEEZED);
        }
    }
}
