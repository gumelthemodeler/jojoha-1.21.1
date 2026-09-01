package org.gumel.jojoha.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.gumel.jojoha.client.StandSkillInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps the user's own arms out of the view while they are flying their Stand.
 *
 * <p>This renderer takes a {@link LocalPlayer} and always draws that player's hands, wherever the
 * camera happens to be. Attaching the camera to the Stand therefore left the user's arms hanging in
 * front of it - so the view was the Stand's, and the hands in it were the player's.
 *
 * <p>Which read, understandably, as the player having been moved into the Stand: the body appeared
 * to be at the camera, so there seemed to be nothing left behind. Nothing had moved. The arms are
 * simply drawn from a different entity than everything else in the frame, and hiding them puts the
 * two back in agreement.
 *
 * <p>The Stand itself is not drawn in its place, and should not be. It is the camera entity, and
 * the game does not draw the thing you are looking out of - the same reason a player sees no body
 * of their own in first person.
 */
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
    @Inject(method = "renderHandsWithItems(FLcom/mojang/blaze3d/vertex/PoseStack;"
            + "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;"
            + "Lnet/minecraft/client/player/LocalPlayer;I)V",
            at = @At("HEAD"), cancellable = true)
    private void jojoha$hideHandsWhilePiloting(float partialTicks, PoseStack poseStack,
                                               MultiBufferSource.BufferSource buffer,
                                               LocalPlayer player, int combinedLight,
                                               CallbackInfo ci) {
        if (StandSkillInput.isPiloting()) {
            ci.cancel();
        }
    }
}
