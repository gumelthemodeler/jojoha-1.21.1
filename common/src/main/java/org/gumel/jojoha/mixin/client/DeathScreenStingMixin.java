package org.gumel.jojoha.mixin.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.DeathScreen;
import org.gumel.jojoha.client.StingFx;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DeathScreen.class)
public abstract class DeathScreenStingMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void jojoha$toBeContinued(GuiGraphics graphics, int mouseX, int mouseY, float partialTick,
                                      CallbackInfo ci) {
        StingFx.renderOverDeath(graphics);
    }
}
