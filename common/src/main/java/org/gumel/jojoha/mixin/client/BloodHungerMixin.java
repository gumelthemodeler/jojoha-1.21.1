package org.gumel.jojoha.mixin.client;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import org.gumel.jojoha.client.BloodBar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class BloodHungerMixin {
    @Inject(method = "renderFood", at = @At("HEAD"), cancellable = true)
    private void jojoha$bloodInsteadOfFood(GuiGraphics graphics, Player player, int y, int x,
                                           CallbackInfo ci) {
        if (BloodBar.render(graphics, player, y, x)) {
            ci.cancel();
        }
    }
}
