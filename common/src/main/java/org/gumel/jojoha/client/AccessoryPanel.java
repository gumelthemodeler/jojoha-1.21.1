package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.accessory.Accessories;

public final class AccessoryPanel {
    public static final ResourceLocation PANEL = menu("accessory/accessory_slots_inventory_menu.png");
    public static final ResourceLocation BUTTON = menu("accessory/accessory_button_inventory.png");
    public static final ResourceLocation BUTTON_OPEN = menu("accessory/accessory_button_inventory_toggled.png");

    public static final int BUTTON_X = 147, BUTTON_Y = 58, BUTTON_SIZE = 23;

    private static boolean open;

    private AccessoryPanel() {
    }

    private static ResourceLocation menu(String name) {
        return ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/gui/menu/" + name);
    }

    public static void init() {
        Accessories.shown = AccessoryPanel::shown;
    }

    public static boolean shown() {
        return open && Minecraft.getInstance().screen instanceof InventoryScreen screen
                && !screen.getRecipeBookComponent().isVisible();
    }

    public static void setOpen(boolean value) {
        open = value;
    }

    public static void draw(GuiGraphics guiGraphics, int left, int top) {
        if (!shown()) {
            return;
        }
        guiGraphics.blit(PANEL, left + Accessories.PANEL_X, top + Accessories.PANEL_Y, 0, 0,
                Accessories.PANEL_W, Accessories.PANEL_H, Accessories.PANEL_W, Accessories.PANEL_H);
    }

    public static boolean covers(double mouseX, double mouseY, int left, int top) {
        int x = left + Accessories.PANEL_X, y = top + Accessories.PANEL_Y;
        return shown() && mouseX >= x && mouseX < x + Accessories.PANEL_W
                && mouseY >= y && mouseY < y + Accessories.PANEL_H;
    }

    public static final class Toggle extends AbstractButton {
        private final Runnable onPress;

        public Toggle(int x, int y, Runnable onPress) {
            super(x, y, BUTTON_SIZE, BUTTON_SIZE, Component.translatable("gui.jojoha.accessories"));
            this.onPress = onPress;
            setTooltip(Tooltip.create(Component.translatable("gui.jojoha.accessories")));
        }

        @Override
        public void onPress() {
            onPress.run();
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            guiGraphics.blit(shown() ? BUTTON_OPEN : BUTTON, getX(), getY(), 0, 0,
                    BUTTON_SIZE, BUTTON_SIZE, BUTTON_SIZE, BUTTON_SIZE);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }
}
