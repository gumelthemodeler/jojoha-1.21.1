package org.gumel.jojoha.mixin.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import org.gumel.jojoha.accessory.Accessories;
import org.gumel.jojoha.client.AccessoryPanel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenAccessoriesMixin extends EffectRenderingInventoryScreen<InventoryMenu> {
    @Shadow
    @Final
    private RecipeBookComponent recipeBookComponent;

    @Shadow
    private boolean buttonClicked;

    @Unique
    private AccessoryPanel.Toggle jojoha$toggle;

    private InventoryScreenAccessoriesMixin(InventoryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void jojoha$addAccessoryToggle(CallbackInfo ci) {
        if (this.minecraft == null || this.minecraft.player == null || this.minecraft.gameMode == null
                || this.minecraft.gameMode.hasInfiniteItems()) {
            return;
        }
        Accessories.showWorn(this.minecraft.player);
        jojoha$toggle = this.addRenderableWidget(new AccessoryPanel.Toggle(
                this.leftPos + AccessoryPanel.BUTTON_X, this.topPos + AccessoryPanel.BUTTON_Y, this::jojoha$press));
    }

    @Unique
    private void jojoha$press() {
        if (AccessoryPanel.shown()) {
            AccessoryPanel.setOpen(false);
            return;
        }
        AccessoryPanel.setOpen(true);
        if (this.recipeBookComponent.isVisible()) {
            this.recipeBookComponent.toggleVisibility();
            this.leftPos = this.recipeBookComponent.updateScreenPosition(this.width, this.imageWidth);
            for (GuiEventListener child : this.children()) {
                if (child instanceof ImageButton book) {
                    book.setPosition(this.leftPos + 104, this.height / 2 - 22);
                }
            }
        }
        this.buttonClicked = true;
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void jojoha$placeAccessoryToggle(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick,
                                             CallbackInfo ci) {
        if (jojoha$toggle != null) {
            jojoha$toggle.setPosition(this.leftPos + AccessoryPanel.BUTTON_X, this.topPos + AccessoryPanel.BUTTON_Y);
        }
    }

    @Inject(method = "renderBg", at = @At("HEAD"))
    private void jojoha$drawAccessoryPanel(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY,
                                           CallbackInfo ci) {
        AccessoryPanel.draw(guiGraphics, this.leftPos, this.topPos);
    }

    @Inject(method = "hasClickedOutside", at = @At("RETURN"), cancellable = true)
    private void jojoha$panelIsInside(double mouseX, double mouseY, int left, int top, int button,
                                      CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && AccessoryPanel.covers(mouseX, mouseY, left, top)) {
            cir.setReturnValue(false);
        }
    }
}
