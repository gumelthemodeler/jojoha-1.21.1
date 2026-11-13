package org.gumel.jojoha.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.registry.ItemShelves;
import org.gumel.jojoha.registry.ModBlocks;
import org.gumel.jojoha.registry.ModRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class CreativeSideTabs {
    private static final int WIDTH = 22;
    private static final int HEIGHT = 21;

    private static final int FIRST_TAB_Y = 4;
    private static final int TAB_SPACING = HEIGHT + 2;

    private static final int OUTSET = WIDTH - 3;
    private static final int SELECTED_LEAN = 1;

    private static final int ICON_INSET = 3;

    private static final int LEFT_COLUMN = 5;

    private static final ResourceLocation BLANK_SELECTED = menu("creative_sidetab_selected");
    private static final ResourceLocation BLANK_UNSELECTED = menu("creative_side_tab_unselected");
    private static final ResourceLocation BLANK_SELECTED_RIGHT = menu("creative_sidetab_selected_right");
    private static final ResourceLocation BLANK_UNSELECTED_RIGHT = menu("creative_side_tab_unselected_right");

    private static final Map<String, String[]> SHELF_ART = Map.of(
            "stands", new String[]{"creative_sidetab_selected_stands", "creative_side_tab_unselected_stands"},
            "hamon", new String[]{"creative_sidetab_selected_hamonvamp", "creative_side_tab_unselected_hamonvamp"},
            "weapons", new String[]{"creative_sidetab_selected_weapons", "creative_side_tab_unselected_weapons"},
            "gear", new String[]{"creative_sidetab_selected_armor", "creative_side_tab_unselected_armor"},
            "accessories", new String[]{"creative_sidetab_selected_accessory", "creative_side_tab_unselected_accessory"},
            "materials", new String[]{"creative_sidetab_selected_right_materials",
                    "creative_side_tab_unselected_right_meteorite"},
            "spawn_eggs", new String[]{"creative_sidetab_selected_right_spawneggs",
                    "creative_side_tab_unselected_right_spawneggs"},
            "music", new String[]{"creative_sidetab_selected_right_discs", "creative_side_tab_unselected_right_discs"});

    private static int effectShift;
    private static CreativeModeTab activeTab;
    private static String active;

    private record Side(String key, ResourceLocation selected, ResourceLocation unselected,
                        Supplier<ItemStack> icon, Predicate<ItemStack> holds) {
    }

    private CreativeSideTabs() {
    }

    public static boolean showsOn(CreativeModeTab tab) {
        return tab != null && (ModRegistries.BLOCKS_TAB.get() == tab || ModRegistries.ITEMS_TAB.get() == tab);
    }

    private static ResourceLocation menu(String name) {
        return ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/gui/menu/creative/" + name + ".png");
    }

    private static List<Side> sides(CreativeModeTab tab) {
        List<Side> shown = new ArrayList<>();
        if (tab == ModRegistries.BLOCKS_TAB.get()) {
            for (ModBlocks.Part part : ModBlocks.Part.values()) {
                if (ModBlocks.hasBlocksIn(part)) {
                    String key = key(part);
                    shown.add(new Side(key, menu("creative_sidetab_" + key + "_selected"),
                            menu("creative_sidetab_" + key + "_unselected"), null,
                            stack -> stack.getItem() instanceof BlockItem block
                                    && ModBlocks.partOf(block.getBlock()) == part));
                }
            }
        } else if (tab == ModRegistries.ITEMS_TAB.get()) {
            for (ItemShelves shelf : ItemShelves.values()) {
                String[] art = SHELF_ART.get(shelf.key());
                boolean right = onRight(shown.size());
                if (art != null) {
                    shown.add(new Side(shelf.key(), menu(art[0]), menu(art[1]), null, shelf::holds));
                } else {
                    shown.add(new Side(shelf.key(), right ? BLANK_SELECTED_RIGHT : BLANK_SELECTED,
                            right ? BLANK_UNSELECTED_RIGHT : BLANK_UNSELECTED, shelf::icon, shelf::holds));
                }
            }
        }
        return shown;
    }

    private static Side activeSide(CreativeModeTab tab) {
        if (active == null || tab != activeTab) {
            return null;
        }
        for (Side side : sides(tab)) {
            if (side.key().equals(active)) {
                return side;
            }
        }
        return null;
    }

    public static void filter(CreativeModeTab tab, NonNullList<ItemStack> items) {
        Side side = activeSide(tab);
        if (side != null) {
            items.removeIf(side.holds().negate());
        }
    }

    public static void render(CreativeModeTab tab, GuiGraphics graphics, int leftPos, int topPos, int imageWidth) {
        List<Side> shown = sides(tab);
        Side current = activeSide(tab);
        effectShift = shown.size() > LEFT_COLUMN ? OUTSET + SELECTED_LEAN : 0;
        for (int i = 0; i < shown.size(); i++) {
            Side side = shown.get(i);
            boolean selected = current != null && side.key().equals(current.key());
            boolean right = onRight(i);
            int x = tabX(i, leftPos, imageWidth) + (selected ? (right ? SELECTED_LEAN : -SELECTED_LEAN) : 0);
            int y = topPos + tabY(i);
            ResourceLocation art = selected ? side.selected() : side.unselected();
            graphics.blit(art, x, y, 0, 0, WIDTH, HEIGHT, WIDTH, HEIGHT);
            if (side.icon() != null) {
                graphics.renderItem(side.icon().get(), x + ICON_INSET, y + ICON_INSET);
            }
        }
    }

    public static Component tooltip(CreativeModeTab tab, int mouseX, int mouseY, int leftPos, int topPos,
                                    int imageWidth) {
        Side over = at(tab, mouseX, mouseY, leftPos, topPos, imageWidth);
        return over == null ? null : Component.translatable("jojoha.creative." + over.key());
    }

    public static boolean click(CreativeModeTab tab, double mouseX, double mouseY, int leftPos, int topPos,
                                int imageWidth) {
        Side hit = at(tab, (int) mouseX, (int) mouseY, leftPos, topPos, imageWidth);
        if (hit == null) {
            return false;
        }
        Side current = activeSide(tab);
        active = current != null && current.key().equals(hit.key()) ? null : hit.key();
        activeTab = tab;
        return true;
    }

    public static void reset() {
        active = null;
        activeTab = null;
        effectShift = 0;
    }

    public static void hidden() {
        effectShift = 0;
    }

    public static int effectShift() {
        return effectShift;
    }

    private static Side at(CreativeModeTab tab, int mouseX, int mouseY, int leftPos, int topPos, int imageWidth) {
        List<Side> shown = sides(tab);
        for (int i = 0; i < shown.size(); i++) {
            int x = tabX(i, leftPos, imageWidth);
            int y = topPos + tabY(i);
            if (mouseX >= x && mouseX < x + WIDTH && mouseY >= y && mouseY < y + HEIGHT) {
                return shown.get(i);
            }
        }
        return null;
    }

    private static boolean onRight(int index) {
        return index >= LEFT_COLUMN;
    }

    private static int tabX(int index, int leftPos, int imageWidth) {
        return onRight(index) ? leftPos + imageWidth + OUTSET - WIDTH : leftPos - OUTSET;
    }

    private static int tabY(int index) {
        return FIRST_TAB_Y + (index % LEFT_COLUMN) * TAB_SPACING;
    }

    private static String key(ModBlocks.Part part) {
        return switch (part) {
            case ONE -> "part1";
            case TWO -> "part2";
            case THREE -> "part3";
        };
    }
}
