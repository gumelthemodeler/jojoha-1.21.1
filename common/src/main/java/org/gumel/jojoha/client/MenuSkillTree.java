package org.gumel.jojoha.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.gumel.jojoha.data.ClientPlayerDataCache;
import org.gumel.jojoha.data.Fate;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.skill.Inflicts;
import org.gumel.jojoha.skill.MoveNames;
import org.gumel.jojoha.skilltree.SkillNode;
import org.gumel.jojoha.skilltree.SkillTrees;
import org.gumel.jojoha.stand.passive.PassiveLoadout;
import org.gumel.jojoha.stand.passive.StandPassive;
import org.gumel.jojoha.stand.passive.StandPassives;
import org.gumel.jojoha.stand.skill.StandReinforced;
import org.gumel.jojoha.stand.skill.StandSkills;
import org.gumel.jojoha.stand.skill.moves.TimeSkipSkill;
import java.util.ArrayList;
import java.util.List;

final class MenuSkillTree {
    private final PlayerMenuScreen screen;

    MenuSkillTree(PlayerMenuScreen screen) {
        this.screen = screen;
    }

    private static final ResourceLocation TREE_BACKGROUND = PlayerMenuScreen.menu("frames/skilltree_background.png");

    private static final ResourceLocation SEPARATOR = PlayerMenuScreen.menu("frames/display_separator.png");

    private static final ResourceLocation SCROLL_BAR = PlayerMenuScreen.menu("frames/scroll_bar.png");

    private static final int GRID_COLS = 3;

    private static final int SCROLLBAR_W = 3;

    private static final int SCROLLBAR_TEX_H = 16;

    private static final int SCROLLBAR_CAP = 3;

    private static final int SCROLLBAR_MIDDLE = SCROLLBAR_TEX_H - 2 * SCROLLBAR_CAP;

    private static final int SCROLLBAR_MIN_THUMB = 2 * SCROLLBAR_CAP + 2;

    private static final int SCROLLBAR_X = PlayerMenuScreen.SKILL_BG_X + PlayerMenuScreen.SKILL_BG_W - SCROLLBAR_W - 1;

    private static final int COLOR_SCROLL_TRACK = 0x50101018;

    private static final int GRID_X = PlayerMenuScreen.SKILL_BG_X + (PlayerMenuScreen.SKILL_BG_W - SCROLLBAR_W - GRID_COLS * PlayerMenuScreen.ICON
            - (GRID_COLS - 1) * PlayerMenuScreen.ICON_GAP) / 2;

    private static final int RULE_W = 77, RULE_H = 3;

    private static final int RULE_X = PlayerMenuScreen.SKILL_BG_X + (PlayerMenuScreen.SKILL_BG_W - SCROLLBAR_W - RULE_W) / 2;

    private static final int RULE_ABOVE = 3;

    private static final int RULE_BELOW = 3;

    private static final int COLOR_RULE_NONE = 0xFF6A6A78;

    private static final int TREE_TILE = 16;

    private final List<int[]> gridSlots = new ArrayList<>();

    private final List<int[]> gridRules = new ArrayList<>();

    private int gridContentHeight;

    private static final float PAGE_MS = 130F;

    float pageProgress() {
        return screen.pageChangedAt == 0L ? 1F
                : Mth.clamp((System.currentTimeMillis() - screen.pageChangedAt) / PAGE_MS, 0F, 1F);
    }

    static SkillTrees.Tree treeFor(SkillBook.Category category) {
        return switch (category) {
            case HAMON -> SkillTrees.Tree.HAMON;
            case PLAYER -> SkillTrees.Tree.PLAYER;
            case VAMPIRE -> Fate.ultimate(ClientPlayerDataCache.data)
                    ? SkillTrees.Tree.ULTIMATE : SkillTrees.Tree.VAMPIRE;
            case PILLAR -> SkillTrees.Tree.PILLAR;
            default -> SkillTrees.Tree.STAND;
        };
    }

    void renderSkillPage(GuiGraphics guiGraphics, JojohaPlayerData data) {
        int px = PlayerMenuScreen.RIGHT_X;
        int py = PlayerMenuScreen.RIGHT_Y;

        PlayerMenuScreen.blit(guiGraphics, PlayerMenuScreen.category.preset(), px + PlayerMenuScreen.PRESET_X, py + PlayerMenuScreen.PRESET_Y,
                PlayerMenuScreen.PRESET_SIZE, PlayerMenuScreen.PRESET_SIZE);

        if (screen.within(PlayerMenuScreen.PRESET_X + PlayerMenuScreen.RIGHT_X, PlayerMenuScreen.PRESET_Y + PlayerMenuScreen.RIGHT_Y, PlayerMenuScreen.PRESET_SIZE, PlayerMenuScreen.PRESET_SIZE)) {
            screen.tooltip = List.of(
                    Component.translatable(PlayerMenuScreen.category.translationKey(data))
                            .withStyle(style -> style.withColor(PlayerMenuScreen.COLOR_ACCENT)),
                    screen.figures(Component.translatable(PlayerMenuScreen.category.descriptionKey(data))),
                    Component.translatable("menu.jojoha.click_for",
                            Component.translatable(PlayerMenuScreen.category.next(data).translationKey(data)))
                            .withStyle(style -> style.withColor(PlayerMenuScreen.COLOR_DIM)));
        }

        List<SkillBook.Entry> entries = SkillBook.of(PlayerMenuScreen.category, data);
        layoutGrid(entries);

        String empty = entries.isEmpty() ? SkillBook.emptyReason(PlayerMenuScreen.category, data) : null;
        if (empty != null) {
            screen.centred(guiGraphics, empty, px + PlayerMenuScreen.SKILL_BG_X + PlayerMenuScreen.SKILL_BG_W / 2,
                    py + PlayerMenuScreen.GRID_Y + PlayerMenuScreen.GRID_VIEW_H / 2 - screen.lineStep(), PlayerMenuScreen.COLOR_DIM);
        }

        screen.skillScroll = Mth.clamp(screen.skillScroll, 0, maxScroll());
        int scroll = screen.skillScroll;

        screen.clipped(guiGraphics, px + PlayerMenuScreen.SKILL_BG_X, py + PlayerMenuScreen.GRID_Y, PlayerMenuScreen.SKILL_BG_W, PlayerMenuScreen.GRID_VIEW_H, () -> {
            for (int[] rule : gridRules) {
                int y = py + PlayerMenuScreen.GRID_Y + rule[0] - scroll;
                if (y + RULE_H <= py + PlayerMenuScreen.GRID_Y || y >= py + PlayerMenuScreen.GRID_Y + PlayerMenuScreen.GRID_VIEW_H) {
                    continue;
                }
                int colour = rule[1];
                guiGraphics.setColor(((colour >> 16) & 0xFF) / 255F, ((colour >> 8) & 0xFF) / 255F,
                        (colour & 0xFF) / 255F, 1F);
                PlayerMenuScreen.blit(guiGraphics, SEPARATOR, px + RULE_X, y, RULE_W, RULE_H);
                guiGraphics.setColor(1F, 1F, 1F, 1F);
            }

            for (int[] place : gridSlots) {
                int x = px + GRID_X + place[1];
                int y = py + PlayerMenuScreen.GRID_Y + place[2] - scroll;

                if (y + PlayerMenuScreen.ICON <= py + PlayerMenuScreen.GRID_Y || y >= py + PlayerMenuScreen.GRID_Y + PlayerMenuScreen.GRID_VIEW_H) {
                    continue;
                }

                SkillBook.Entry entry = entries.get(place[0]);

                drawSkillIcon(guiGraphics, entry.id(), x, y);

                if (!entry.unlocked() || entry.dashForm()
                        && !StandReinforced.dashes(data, entry.id())
                        || !entry.dashForm() && !PlayerMenuScreen.fitsBar(data, entry.id(), screen.utilityBar(data))) {
                    guiGraphics.fill(x + 1, y + 1, x + PlayerMenuScreen.ICON - 1, y + PlayerMenuScreen.ICON - 1, PlayerMenuScreen.COLOR_LOCKED);
                }

                int slot = data.slotOf(screen.utilityBar(data), entry.id());

                if (overSkill(place[0], scroll)) {
                    List<Component> lines = new ArrayList<>();
                    MutableComponent title = entry.name().copy()
                            .withStyle(style -> style.withColor(entry.unlocked() ? PlayerMenuScreen.COLOR_ACCENT : PlayerMenuScreen.COLOR_DIM));
                    SkillNode.Branch branch = branchOf(entry);
                    if (branch != null && Brief.has(MoveNames.key(data, entry.id()))) {
                        title.append(Component.literal("  "))
                                .append(Inflicts.tag(branch.colour()))
                                .append(Component.literal(branch.label())
                                        .withStyle(style -> style.withColor(branch.colour())));
                    }
                    lines.add(title);

                    screen.more |= Brief.move(lines, data, entry.id(), screen.detailed, PlayerMenuScreen.COLOR_DIM, PlayerMenuScreen.COLOR_FIGURE);
                    screen.keywords = Inflicts.of(entry.id());
                    screen.previewMove = entry.id();

                    lines.add(Component.literal(slotNote(entry, slot))
                            .withStyle(style -> style.withColor(PlayerMenuScreen.COLOR_DIM)));
                    screen.tooltip = List.copyOf(lines);
                }
            }
        });

        drawSkillScrollbar(guiGraphics, px, py);
    }

    private void layoutGrid(List<SkillBook.Entry> entries) {
        gridSlots.clear();
        gridRules.clear();

        int y = 0;
        for (SkillNode.Branch branch : SkillNode.Branch.values()) {
            y = layoutGroup(entries, branch, branch.colour(), y);
        }
        y = layoutGroup(entries, null, COLOR_RULE_NONE, y);

        gridContentHeight = y;
    }

    private int layoutGroup(List<SkillBook.Entry> entries, SkillNode.Branch branch,
                            int colour, int y) {
        int placed = 0;
        for (int i = 0; i < entries.size(); i++) {
            if (branchOf(entries.get(i)) != branch) {
                continue;
            }

            if (placed == 0) {
                y += RULE_ABOVE;
                gridRules.add(new int[]{y, colour});
                y += RULE_H + RULE_BELOW;
            }

            gridSlots.add(new int[]{i,
                    (placed % GRID_COLS) * PlayerMenuScreen.ROW_PITCH,
                    y + (placed / GRID_COLS) * PlayerMenuScreen.ROW_PITCH});
            placed++;
        }

        return placed == 0 ? y
                : y + ((placed + GRID_COLS - 1) / GRID_COLS) * PlayerMenuScreen.ROW_PITCH;
    }

    private static SkillNode.Branch branchOf(SkillBook.Entry entry) {
        if (entry.dashForm()) {
            return SkillNode.Branch.MOBILITY;
        }
        SkillNode node = SkillTrees.forSkill(entry.id());
        return node == null ? null : node.branch();
    }

    private static List<SkillBook.Category> treeTabs() {
        JojohaPlayerData data = ClientPlayerDataCache.data;
        List<SkillBook.Category> tabs = new ArrayList<>();
        for (SkillBook.Category each : SkillBook.Category.values()) {
            if (each.reachable(data)) {
                tabs.add(each);
            }
        }
        return tabs;
    }

    void drawTreeTabs(GuiGraphics guiGraphics, int drop, boolean front) {
        List<SkillBook.Category> tabs = treeTabs();
        for (int i = 0; i < tabs.size(); i++) {
            boolean showing = tabs.get(i) == PlayerMenuScreen.category;
            int x = PlayerMenuScreen.TREE_TAB_X + i * PlayerMenuScreen.TREE_TAB_PITCH;
            if (!front) {
                PlayerMenuScreen.blit(guiGraphics, showing ? PlayerMenuScreen.TREE_TAB_SELECTED : PlayerMenuScreen.TREE_TAB, x, PlayerMenuScreen.TREE_TAB_Y - drop,
                        PlayerMenuScreen.TREE_TAB_W, PlayerMenuScreen.TREE_TAB_H);
            }
        }
        if (front) {
            return;
        }
        for (int i = 0; i < tabs.size(); i++) {
            boolean showing = tabs.get(i) == PlayerMenuScreen.category;
            int x = PlayerMenuScreen.TREE_TAB_X + i * PlayerMenuScreen.TREE_TAB_PITCH;
            if (showing) {
                PlayerMenuScreen.blit(guiGraphics, tabs.get(i).preset(), x + PlayerMenuScreen.TREE_TAB_DISC_X, PlayerMenuScreen.TREE_TAB_Y + PlayerMenuScreen.TREE_TAB_DISC_Y - drop,
                        PlayerMenuScreen.TREE_TAB_DISC, PlayerMenuScreen.TREE_TAB_DISC);
            } else {
                PlayerMenuScreen.blit(guiGraphics, tabs.get(i).preset(), x + PlayerMenuScreen.TREE_TAB_DISC_X + 1,
                        PlayerMenuScreen.TREE_TAB_Y + PlayerMenuScreen.TREE_TAB_LOW + PlayerMenuScreen.TREE_TAB_DISC_LOW_Y - drop,
                        PlayerMenuScreen.TREE_TAB_DISC - 2, PlayerMenuScreen.TREE_TAB_DISC - 2);
            }
        }
    }

    private boolean overTreeTab(int i, SkillBook.Category tab) {
        int x = PlayerMenuScreen.TREE_TAB_X + i * PlayerMenuScreen.TREE_TAB_PITCH;
        int top = tab == PlayerMenuScreen.category ? PlayerMenuScreen.TREE_TAB_Y : PlayerMenuScreen.TREE_TAB_Y + PlayerMenuScreen.TREE_TAB_LOW;
        int bottom = tab == PlayerMenuScreen.category ? PlayerMenuScreen.TREE_TAB_Y + PlayerMenuScreen.TREE_TAB_H : 0;
        return screen.within(x, top, PlayerMenuScreen.TREE_TAB_W, bottom - top);
    }

    private void collectTreeTabTooltip() {
        if (PlayerMenuScreen.page != PlayerMenuScreen.MenuPage.SKILLS) {
            return;
        }
        List<SkillBook.Category> tabs = treeTabs();
        for (int i = 0; i < tabs.size(); i++) {
            if (overTreeTab(i, tabs.get(i))) {
                screen.tooltip = List.of(
                        Component.translatable(tabs.get(i).translationKey())
                                .withStyle(style -> style.withColor(PlayerMenuScreen.COLOR_ACCENT)),
                        screen.figures(Component.translatable(tabs.get(i).descriptionKey())));
                return;
            }
        }
    }

    void renderSkillTree(GuiGraphics guiGraphics, JojohaPlayerData data) {
        screen.treeView.showStand(data.stand.isPresent() ? data.stand.standId() : null);

        int viewX = screen.treeX(), viewY = screen.treeY(), viewW = screen.treeW(), viewH = screen.treeH();
        SkillNode hovered = screen.treeView.nodeAt(screen.localMouseX, screen.localMouseY, viewX, viewY, viewW, viewH);

        screen.clipped(guiGraphics, viewX, viewY, viewW, viewH, () -> {
            for (int ty = 0; ty < viewH; ty += TREE_TILE) {
                for (int tx = 0; tx < viewW; tx += TREE_TILE) {
                    PlayerMenuScreen.blit(guiGraphics, TREE_BACKGROUND, viewX + tx, viewY + ty,
                            TREE_TILE, TREE_TILE);
                }
            }

            screen.treeView.render(guiGraphics, data, viewX, viewY, viewW, viewH, hovered);
        });

        collectTreeTabTooltip();

        if (hovered != null && !screen.treeView.dragging()) {
            screen.tooltip = screen.treeView.tooltip(data, hovered, screen.detailed);
            screen.more |= screen.treeView.more();
            screen.keywords = Inflicts.of(hovered.skill());
            screen.previewMove = hovered.skill();
        }
    }

    private void drawSkillScrollbar(GuiGraphics guiGraphics, int px, int py) {
        int max = maxScroll();
        if (max <= 0) {
            return;
        }

        int x = px + SCROLLBAR_X;
        int y = py + PlayerMenuScreen.GRID_Y;
        guiGraphics.fill(x, y, x + SCROLLBAR_W, y + PlayerMenuScreen.GRID_VIEW_H, COLOR_SCROLL_TRACK);

        int thumb = Math.max(SCROLLBAR_MIN_THUMB,
                PlayerMenuScreen.GRID_VIEW_H * PlayerMenuScreen.GRID_VIEW_H / Math.max(1, gridContentHeight));
        int top = y + (PlayerMenuScreen.GRID_VIEW_H - thumb) * screen.skillScroll / max;
        drawScrollThumb(guiGraphics, x, top, thumb);
    }

    private void drawScrollThumb(GuiGraphics guiGraphics, int x, int y, int height) {
        guiGraphics.blit(SCROLL_BAR, x, y, 0F, 0F,
                SCROLLBAR_W, SCROLLBAR_CAP, SCROLLBAR_W, SCROLLBAR_TEX_H);

        int filled = 0;
        int middle = height - 2 * SCROLLBAR_CAP;
        while (filled < middle) {
            int slice = Math.min(SCROLLBAR_MIDDLE, middle - filled);
            guiGraphics.blit(SCROLL_BAR, x, y + SCROLLBAR_CAP + filled, 0F, SCROLLBAR_CAP,
                    SCROLLBAR_W, slice, SCROLLBAR_W, SCROLLBAR_TEX_H);
            filled += slice;
        }

        guiGraphics.blit(SCROLL_BAR, x, y + height - SCROLLBAR_CAP,
                0F, SCROLLBAR_TEX_H - SCROLLBAR_CAP,
                SCROLLBAR_W, SCROLLBAR_CAP, SCROLLBAR_W, SCROLLBAR_TEX_H);
    }

    private void drawSkillIcon(GuiGraphics guiGraphics, ResourceLocation skillId, int x, int y) {
        PlayerMenuScreen.blit(guiGraphics, PlayerMenuScreen.SKILL_BASE_ICON, x, y, PlayerMenuScreen.ICON, PlayerMenuScreen.ICON);

        ResourceLocation icon = SkillIcons.of(skillId, PlayerMenuScreen.category == SkillBook.Category.PLAYER);
        if (icon != null) {
            PlayerMenuScreen.blit(guiGraphics, icon, x + 1, y + 1, SkillIcons.SIZE, SkillIcons.SIZE);
        } else {
            SkillIcons.drawName(guiGraphics, screen.font(), skillId, x + 1, y + 1, SkillIcons.SIZE);
        }

        PlayerMenuScreen.blit(guiGraphics, PlayerMenuScreen.SKILL_ICON_FRAME, x, y, PlayerMenuScreen.ICON, PlayerMenuScreen.ICON);
        StatusPips.draw(guiGraphics, skillId, x + PlayerMenuScreen.ICON - 2, y + PlayerMenuScreen.ICON - 2);
    }

    int maxScroll() {
        return Math.max(0, gridContentHeight - PlayerMenuScreen.GRID_VIEW_H);
    }

    private boolean overSkill(int index, int scroll) {
        if (!screen.within(PlayerMenuScreen.RIGHT_X + PlayerMenuScreen.SKILL_BG_X, PlayerMenuScreen.RIGHT_Y + PlayerMenuScreen.GRID_Y, PlayerMenuScreen.SKILL_BG_W, PlayerMenuScreen.GRID_VIEW_H)) {
            return false;
        }

        for (int[] place : gridSlots) {
            if (place[0] == index) {
                return screen.within(PlayerMenuScreen.RIGHT_X + GRID_X + place[1],
                        PlayerMenuScreen.RIGHT_Y + PlayerMenuScreen.GRID_Y + place[2] - scroll, PlayerMenuScreen.ICON, PlayerMenuScreen.ICON);
            }
        }
        return false;
    }

    private static String slotNote(SkillBook.Entry entry, int slot) {
        if (!entry.unlocked()) {
            return "Locked";
        }
        if (entry.dashForm()) {
            return StandReinforced.dashes(
                    ClientPlayerDataCache.data, entry.id())
                    ? "Your Dodge becomes screen while your Stand is out"
                    : "Click to dash with screen instead";
        }
        return slot >= 0 ? "On slot " + (slot + 1) + " - right click to remove"
                : "Click to put on the bar";
    }

    boolean clickedTree(int button) {
        if (button != 0) {
            return false;
        }

        if (PlayerMenuScreen.page == PlayerMenuScreen.MenuPage.SKILLS) {
            List<SkillBook.Category> tabs = treeTabs();
            for (int i = 0; i < tabs.size(); i++) {
                if (!overTreeTab(i, tabs.get(i))) {
                    continue;
                }
                if (tabs.get(i) != PlayerMenuScreen.category) {
                    PlayerMenuScreen.category = tabs.get(i);
                    screen.skillScroll = 0;
                    screen.pick(null);
                    screen.treeView.show(treeFor(PlayerMenuScreen.category));
                }
                PlayerMenuScreen.click();
                return true;
            }
        }

        if (!screen.within(screen.treeX(), screen.treeY(), screen.treeW(), screen.treeH())) {
            return false;
        }

        SkillNode node = screen.treeView.nodeAt(screen.localMouseX, screen.localMouseY,
                screen.treeX(), screen.treeY(), screen.treeW(), screen.treeH());

        if (node != null) {
            JojohaPlayerData data = ClientPlayerDataCache.data;
            StandPassive passive = node.skill() == null ? null
                    : StandPassives.byId(node.skill());

            boolean owned = SkillTrees.owned(data, node);

            if (!owned
                    && SkillTrees.parentDone(data, node)
                    && SkillTrees.unmetStats(data, node).isEmpty()) {
                NetworkHandler.sendUnlockNode(node.id());
            } else if (passive != null && owned) {
                if (PassiveLoadout.displacedBy(data, passive.id()) != null) {
                    NetworkHandler.sendEquipPassive(passive.id());
                }
            }
            PlayerMenuScreen.click();
            return true;
        }

        screen.treeView.beginDrag(screen.localMouseX, screen.localMouseY);
        return true;
    }

    boolean clickedSkill(JojohaPlayerData data, int button) {
        if (button != 0 && button != 1) {
            return false;
        }

        List<SkillBook.Entry> entries = SkillBook.of(PlayerMenuScreen.category, data);
        for (int i = 0; i < entries.size(); i++) {
            if (!overSkill(i, screen.skillScroll)) {
                continue;
            }

            SkillBook.Entry entry = entries.get(i);
            if (!entry.unlocked()) {
                PlayerMenuScreen.click();
                return true;
            }

            if (entry.dashForm()) {
                if (button == 0 && !StandReinforced.dashes(data, entry.id())) {
                    NetworkHandler.sendChooseDash(TimeSkipSkill.ID.equals(entry.id()));
                }
                PlayerMenuScreen.click();
                return true;
            }

            boolean utility = screen.utilityBar(data);
            int base = StandSkills.UTILITY_SLOT;
            int onBar = data.slotOf(utility, entry.id());
            if (button == 1) {
                if (onBar >= 0) {
                    NetworkHandler.sendClearSlot(utility ? base + onBar : onBar);
                }
            } else if (!PlayerMenuScreen.fitsBar(data, entry.id(), utility)) {
                PlayerMenuScreen.click();
                return true;
            } else if (PlayerMenuScreen.combatBarOpen) {
                screen.pick(screen.picked != null && screen.picked.equals(entry.id()) ? null : entry.id());
            } else if (utility) {
                int free = data.slotOf(true, null);
                if (free >= 0) {
                    NetworkHandler.sendEquipSkill(entry.id(), base + free);
                }
            } else {
                NetworkHandler.sendEquipSkill(entry.id(), -1);
            }

            PlayerMenuScreen.click();
            return true;
        }

        return false;
    }
}
