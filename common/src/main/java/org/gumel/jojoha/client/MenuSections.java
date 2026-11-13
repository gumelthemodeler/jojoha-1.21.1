package org.gumel.jojoha.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import java.util.List;

final class MenuSections {
    private final PlayerMenuScreen screen;

    MenuSections(PlayerMenuScreen screen) {
        this.screen = screen;
    }

    private static final float BUTTON_LIFT = 1.35F, BUTTON_DIM = 0.5F;

    void drawSectionTabs(GuiGraphics guiGraphics, int drop, boolean front) {
        PlayerMenuScreen.Section[] tabs = PlayerMenuScreen.Section.values();
        for (int i = 0; i < tabs.length; i++) {
            boolean showing = tabs[i] == PlayerMenuScreen.section;
            int x = PlayerMenuScreen.TREE_TAB_X + i * PlayerMenuScreen.TREE_TAB_PITCH;
            if (!front) {
                PlayerMenuScreen.blit(guiGraphics, showing ? PlayerMenuScreen.TREE_TAB_SELECTED : PlayerMenuScreen.TREE_TAB, x, PlayerMenuScreen.TREE_TAB_Y - drop,
                        PlayerMenuScreen.TREE_TAB_W, PlayerMenuScreen.TREE_TAB_H);
            }
        }
        if (front) {
            return;
        }
        for (int i = 0; i < tabs.length; i++) {
            boolean showing = tabs[i] == PlayerMenuScreen.section;
            int x = PlayerMenuScreen.TREE_TAB_X + i * PlayerMenuScreen.TREE_TAB_PITCH + PlayerMenuScreen.TREE_TAB_DISC_X + 1;
            int y = showing ? PlayerMenuScreen.TREE_TAB_Y + PlayerMenuScreen.TREE_TAB_DISC_Y + 1 - drop
                    : PlayerMenuScreen.TREE_TAB_Y + PlayerMenuScreen.TREE_TAB_LOW + PlayerMenuScreen.TREE_TAB_DISC_LOW_Y - drop;
            ItemStack icon = tabs[i].icon;
            screen.clipped(guiGraphics, x, y, PlayerMenuScreen.TREE_TAB_DISC, PlayerMenuScreen.TREE_TOP_EDGE_Y - drop - y,
                    () -> guiGraphics.renderItem(icon, x, y));
        }
    }

    private boolean overSectionTab(int i, PlayerMenuScreen.Section tab) {
        boolean open = tab == PlayerMenuScreen.section;
        int x = PlayerMenuScreen.TREE_TAB_X + i * PlayerMenuScreen.TREE_TAB_PITCH;
        int top = open ? PlayerMenuScreen.TREE_TAB_Y : PlayerMenuScreen.TREE_TAB_Y + PlayerMenuScreen.TREE_TAB_LOW;
        int bottom = open ? PlayerMenuScreen.TREE_TAB_Y + PlayerMenuScreen.TREE_TAB_H : 0;
        return screen.within(x, top, PlayerMenuScreen.TREE_TAB_W, bottom - top);
    }

    void sectionTabTooltip() {
        PlayerMenuScreen.Section[] tabs = PlayerMenuScreen.Section.values();
        for (int i = 0; i < tabs.length; i++) {
            if (overSectionTab(i, tabs[i])) {
                screen.tooltip = List.of(
                        Component.translatable("menu.jojoha.custom." + tabs[i].key)
                                .withStyle(style -> style.withColor(PlayerMenuScreen.COLOR_ACCENT)),
                        Component.translatable("menu.jojoha.custom." + tabs[i].key + ".hint")
                                .withStyle(style -> style.withColor(PlayerMenuScreen.COLOR_DIM)));
                return;
            }
        }
    }

    void open(PlayerMenuScreen.Section next) {
        if (PlayerMenuScreen.section == next) {
            return;
        }
        PlayerMenuScreen.section = next;
        if (PlayerMenuScreen.section == PlayerMenuScreen.Section.STYLE && !PlayerMenuScreen.idleScanned) {
            StandIdles.scan();
            PlayerMenuScreen.idleScanned = true;
        }
        PlayerMenuScreen.click();
    }

    boolean clickedSectionTab() {
        PlayerMenuScreen.Section[] tabs = PlayerMenuScreen.Section.values();
        for (int i = 0; i < tabs.length; i++) {
            if (overSectionTab(i, tabs[i])) {
                open(tabs[i]);
                return true;
            }
        }
        return false;
    }

    void button(GuiGraphics guiGraphics, int x, int y, int w, int h, Component label, boolean active) {
        boolean over = active && screen.within(x, y, w, h);
        float shade = !active ? BUTTON_DIM : over ? BUTTON_LIFT : 1F;
        guiGraphics.setColor(shade, shade, shade, 1F);
        JojohaTooltip.nineSlice(guiGraphics, JojohaTooltip.TOOLTIP, x, y, w, h, JojohaTooltip.BORDER);
        guiGraphics.setColor(1F, 1F, 1F, 1F);
        String text = label.getString();
        int width = Math.round(screen.font().width(text) * screen.textScale);
        screen.smallRaw(guiGraphics, text, x + (w - width) / 2, y + (h - screen.lineStep()) / 2 + 1,
                !active ? PlayerMenuScreen.COLOR_DIM : over ? PlayerMenuScreen.COLOR_HOVER : PlayerMenuScreen.COLOR_TEXT);
    }
}
