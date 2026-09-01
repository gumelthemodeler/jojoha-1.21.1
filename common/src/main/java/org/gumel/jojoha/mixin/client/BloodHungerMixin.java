package org.gumel.jojoha.mixin.client;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import org.gumel.jojoha.client.BloodBar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws blood where a vampire's hunger would be.
 *
 * <p>A vampire does not eat, so the row that says how full they are is the wrong question asked in
 * the wrong icon. Replacing the whole row rather than recolouring the drumsticks: the meter still
 * tracks the same value underneath - vanilla's saturation and starvation rules keep running, and a
 * vampire still needs to fill it - but what it is asking for is different, and the icon should say
 * so.
 *
 * <p>Vanilla's own draw is cancelled rather than drawn over. Both rows occupy the same strip, so
 * leaving the original beneath would show drumsticks poking out from behind whichever blood icon
 * happened to be narrower.
 */
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
