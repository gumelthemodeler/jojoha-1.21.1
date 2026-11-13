package org.gumel.jojoha.mixin.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.item.ItemStack;
import org.gumel.jojoha.client.JojohaTooltip;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(GuiGraphics.class)
public abstract class GuiTooltipMixin {

    @Inject(method = "renderTooltip(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;II)V",
            at = @At("HEAD"))
    private void jojoha$noteTooltipItem(Font font, ItemStack stack, int mouseX, int mouseY,
                                        CallbackInfo ci) {
        JojohaTooltip.arm(stack);
    }

    @ModifyVariable(method = "renderTooltipInternal", at = @At("HEAD"), argsOnly = true)
    private List<ClientTooltipComponent> jojoha$compactOurTooltips(
            List<ClientTooltipComponent> parts) {
        return JojohaTooltip.consumeArmed() ? JojohaTooltip.compact(parts) : parts;
    }
}
