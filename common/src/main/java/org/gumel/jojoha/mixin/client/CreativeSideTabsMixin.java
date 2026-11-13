package org.gumel.jojoha.mixin.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.CreativeModeTab;
import org.gumel.jojoha.client.CreativeSideTabs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeSideTabsMixin
        extends AbstractContainerScreen<CreativeModeInventoryScreen.ItemPickerMenu> {

    @Shadow
    private static CreativeModeTab selectedTab;

    private CreativeSideTabsMixin(CreativeModeInventoryScreen.ItemPickerMenu menu,
                                  Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Shadow
    private void selectTab(CreativeModeTab tab) {
        throw new AssertionError();
    }

    @Inject(method = "selectTab", at = @At("TAIL"))
    private void jojoha$sortOnOpen(CreativeModeTab tab, CallbackInfo ci) {
        jojoha$sort();
    }

    @Inject(method = "refreshCurrentTabContents", at = @At("TAIL"))
    private void jojoha$sortOnRebuild(CallbackInfo ci) {
        jojoha$sort();
    }

    private void jojoha$sort() {
        if (CreativeSideTabs.showsOn(selectedTab)) {
            CreativeSideTabs.filter(selectedTab, this.menu.items);
            this.menu.scrollTo(0F);
        }
    }

    @Inject(method = "renderBg", at = @At("HEAD"))
    private void jojoha$drawTabs(GuiGraphics graphics, float partialTick, int mouseX, int mouseY,
                                 CallbackInfo ci) {
        if (CreativeSideTabs.showsOn(selectedTab)) {
            CreativeSideTabs.render(selectedTab, graphics, this.leftPos, this.topPos, this.imageWidth);
        } else {
            CreativeSideTabs.hidden();
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void jojoha$drawTabName(GuiGraphics graphics, int mouseX, int mouseY, float partialTick,
                                    CallbackInfo ci) {
        if (!CreativeSideTabs.showsOn(selectedTab)) {
            return;
        }
        Component name = CreativeSideTabs.tooltip(selectedTab, mouseX, mouseY, this.leftPos, this.topPos,
                this.imageWidth);
        if (name != null) {
            graphics.renderTooltip(this.font, name, mouseX, mouseY);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void jojoha$clickTabs(double mouseX, double mouseY, int button,
                                  CallbackInfoReturnable<Boolean> cir) {
        if (CreativeSideTabs.showsOn(selectedTab)
                && CreativeSideTabs.click(selectedTab, mouseX, mouseY, this.leftPos, this.topPos,
                        this.imageWidth)) {
            selectTab(selectedTab);
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void jojoha$forgetFilter(CallbackInfo ci) {
        CreativeSideTabs.reset();
    }
}
