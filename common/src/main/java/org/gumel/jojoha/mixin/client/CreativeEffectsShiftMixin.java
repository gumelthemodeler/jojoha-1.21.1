package org.gumel.jojoha.mixin.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.gumel.jojoha.client.CreativeSideTabs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EffectRenderingInventoryScreen.class)
public abstract class CreativeEffectsShiftMixin<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
    @Unique
    private int jojoha$shifted;

    private CreativeEffectsShiftMixin(T menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "renderEffects", at = @At("HEAD"))
    private void jojoha$clearSideTabs(GuiGraphics graphics, int mouseX, int mouseY, CallbackInfo ci) {
        jojoha$shifted = (Object) this instanceof CreativeModeInventoryScreen ? CreativeSideTabs.effectShift() : 0;
        this.imageWidth += jojoha$shifted;
    }

    @Inject(method = "renderEffects", at = @At("RETURN"))
    private void jojoha$restoreWidth(GuiGraphics graphics, int mouseX, int mouseY, CallbackInfo ci) {
        this.imageWidth -= jojoha$shifted;
        jojoha$shifted = 0;
    }
}
