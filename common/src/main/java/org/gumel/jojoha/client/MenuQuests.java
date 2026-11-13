package org.gumel.jojoha.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.gumel.jojoha.entity.npc.Quests;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class MenuQuests {
    private static final int PAD = 8;
    private static final int ROW_H = 30;
    private static final int ROW_GAP = 4;
    private static final int BAR_W = 90;
    private static final int FILTER_H = 18;
    private static final int ROW_FILL = 0x40000000;
    private static final int COLOR_READY = 0xFF70E070;
    private static final int ROW_HOVER = 0x20FFFFFF;
    private static final int TRACK_EDGE = 0xFFE0B040;
    private static final int FILTER_HOVER = 0xFFFFDD55;

    private final PlayerMenuScreen screen;
    private final List<int[]> rows = new ArrayList<>();
    private final List<String> rowIds = new ArrayList<>();
    @Nullable
    private Quests.Objective filter;
    private final int[] filterBox = new int[4];

    MenuQuests(PlayerMenuScreen screen) {
        this.screen = screen;
    }

    private String filterLabel() {
        String key = filter == null ? "all" : filter.name().toLowerCase(Locale.ROOT);
        return Component.translatable("menu.jojoha.quests.filter." + key).getString();
    }

    void render(GuiGraphics graphics) {
        rows.clear();
        rowIds.clear();
        int left = PlayerMenuScreen.TREE_PAGE_X + PAD;
        int top = PlayerMenuScreen.TREE_PAGE_Y + PAD;
        int width = PlayerMenuScreen.TREE_PAGE_W - PAD * 2;
        screen.smallLeft(graphics, Component.translatable("menu.jojoha.quests.title").getString(), left, top, width,
                PlayerMenuScreen.COLOR_ACCENT);

        String label = filterLabel();
        int filterWidth = QuestIcons.ICON + 6 + screen.smallWidth(label) + 8;
        int filterLeft = left + width - filterWidth;
        int filterTop = top - 5;
        filterBox[0] = filterLeft;
        filterBox[1] = filterTop;
        filterBox[2] = filterWidth;
        filterBox[3] = FILTER_H;
        boolean hover = screen.within(filterLeft, filterTop, filterWidth, FILTER_H);
        DialogueFrame.draw(graphics, filterLeft, filterTop, filterWidth, FILTER_H);
        QuestIcons.icon(graphics, filter, filterLeft + 3, filterTop + 1);
        screen.smallLeft(graphics, label, filterLeft + QuestIcons.ICON + 6, filterTop + (FILTER_H - screen.lineStep()) / 2 + 1,
                filterWidth, hover ? FILTER_HOVER : PlayerMenuScreen.COLOR_TEXT);

        List<ClientQuests.Entry> entries = new ArrayList<>();
        for (ClientQuests.Entry entry : ClientQuests.entries()) {
            if (filter == null || QuestIcons.objective(entry.id()) == filter) {
                entries.add(entry);
            }
        }
        if (entries.isEmpty()) {
            String empty = ClientQuests.entries().isEmpty() ? "menu.jojoha.quests.empty" : "menu.jojoha.quests.none_of_type";
            screen.centredWrapped(graphics, Component.translatable(empty).getString(),
                    PlayerMenuScreen.TREE_PAGE_X + PlayerMenuScreen.TREE_PAGE_W / 2,
                    PlayerMenuScreen.TREE_PAGE_Y + PlayerMenuScreen.TREE_PAGE_H / 2, width - PAD * 4,
                    PlayerMenuScreen.COLOR_DIM);
            return;
        }
        int y = top + FILTER_H;
        int bottom = PlayerMenuScreen.TREE_PAGE_Y + PlayerMenuScreen.TREE_PAGE_H - PAD;
        for (ClientQuests.Entry entry : entries) {
            if (y + ROW_H > bottom) {
                break;
            }
            rows.add(new int[]{left, y, width, ROW_H});
            rowIds.add(entry.id());
            row(graphics, entry, left, y, width);
            y += ROW_H + ROW_GAP;
        }
    }

    boolean clicked(int button) {
        if (button != 0) {
            return false;
        }
        if (screen.within(filterBox[0], filterBox[1], filterBox[2], filterBox[3])) {
            Quests.Objective[] all = Quests.Objective.GROUPS;
            filter = filter == null ? all[0] : filter.ordinal() + 1 < all.length ? all[filter.ordinal() + 1] : null;
            PlayerMenuScreen.click();
            return true;
        }
        for (int i = 0; i < rows.size(); i++) {
            int[] r = rows.get(i);
            if (screen.within(r[0], r[1], r[2], r[3])) {
                ClientQuests.track(rowIds.get(i));
                PlayerMenuScreen.click();
                return true;
            }
        }
        return false;
    }

    private void row(GuiGraphics graphics, ClientQuests.Entry entry, int left, int y, int width) {
        String base = "quest.jojoha." + entry.id();
        int step = screen.lineStep();
        graphics.fill(left, y, left + width, y + ROW_H, ROW_FILL);
        if (screen.within(left, y, width, ROW_H)) {
            graphics.fill(left, y, left + width, y + ROW_H, ROW_HOVER);
        }
        boolean tracked = ClientQuests.isTracked(entry);
        if (tracked) {
            graphics.fill(left, y, left + 2, y + ROW_H, TRACK_EDGE);
        }
        QuestIcons.icon(graphics, QuestIcons.objective(entry.id()), left + 5, y + (ROW_H - QuestIcons.ICON) / 2);
        int textLeft = left + 5 + QuestIcons.ICON + 5;
        int textWidth = width - BAR_W - (textLeft - left) - 12;
        screen.smallLeft(graphics, Component.translatable(base + ".name").getString(), textLeft, y + 3, textWidth,
                PlayerMenuScreen.COLOR_TEXT);
        screen.smallLeft(graphics, Component.translatable(base + ".goal", entry.needed()).getString(), textLeft,
                y + 3 + step, textWidth, PlayerMenuScreen.COLOR_DIM);
        String status = entry.ready()
                ? Component.translatable("menu.jojoha.quests.ready").getString()
                : Component.translatable(base + ".giver").getString();
        screen.smallLeft(graphics, status, textLeft, y + 3 + step * 2, textWidth,
                entry.ready() ? COLOR_READY : PlayerMenuScreen.COLOR_DIM);

        int barLeft = left + width - BAR_W - 4;
        int barTop = y + ROW_H / 2 + 1;
        String count = entry.done() + "/" + entry.needed();
        screen.smallLeft(graphics, count, barLeft, barTop - step - 1, BAR_W, PlayerMenuScreen.COLOR_TEXT);
        if (tracked) {
            String label = Component.translatable("menu.jojoha.quests.tracking").getString();
            screen.smallLeft(graphics, label, barLeft + BAR_W - screen.smallWidth(label), barTop - step - 1, BAR_W,
                    TRACK_EDGE);
        }
        QuestIcons.bar(graphics, barLeft, barTop, BAR_W, entry.done() / (float) Math.max(1, entry.needed()), entry.ready());
    }
}
