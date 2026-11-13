package org.gumel.jojoha.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.RelativeMovement;
import org.gumel.jojoha.labyrinth.CatacombSeal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class SealedTeleportMixin {
    @Shadow
    public ServerPlayer player;

    @Inject(method = "teleport(DDDFFLjava/util/Set;)V", at = @At("HEAD"), cancellable = true)
    private void jojoha$sealed(double x, double y, double z, float yRot, float xRot, Set<RelativeMovement> relative,
                               CallbackInfo ci) {
        double toX = relative.contains(RelativeMovement.X) ? player.getX() + x : x;
        double toY = relative.contains(RelativeMovement.Y) ? player.getY() + y : y;
        double toZ = relative.contains(RelativeMovement.Z) ? player.getZ() + z : z;
        if (CatacombSeal.holds(player, toX, toY, toZ)) {
            ci.cancel();
        }
    }
}
