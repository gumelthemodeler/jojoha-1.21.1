package org.gumel.jojoha.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.player.LocalPlayer;
import org.gumel.jojoha.client.TimeStopView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TextureAtlas.class)
public abstract class TextureAnimationFreezeMixin {
    @Inject(method = "cycleAnimationFrames", at = @At("HEAD"), cancellable = true)
    private void jojoha$holdFramesInStoppedTime(CallbackInfo ci) {
        if (!TimeStopView.active()) {
            return;
        }

        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && TimeStopView.holdsPosition(player.getX(), player.getEyeY(), player.getZ())) {
            ci.cancel();
        }
    }
}
