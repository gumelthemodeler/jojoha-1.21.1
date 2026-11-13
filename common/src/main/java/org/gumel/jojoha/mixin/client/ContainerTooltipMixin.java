package org.gumel.jojoha.mixin.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.gumel.jojoha.client.JojohaTooltip;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class ContainerTooltipMixin {

    @Shadow
    protected Slot hoveredSlot;

    @Inject(method = "renderTooltip", at = @At("HEAD"))
    private void jojoha$noteHoveredItem(GuiGraphics guiGraphics, int mouseX, int mouseY,
                                        CallbackInfo ci) {
        JojohaTooltip.arm(hoveredSlot == null ? ItemStack.EMPTY : hoveredSlot.getItem());
    }
}
