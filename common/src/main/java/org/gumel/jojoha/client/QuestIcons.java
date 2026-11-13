package org.gumel.jojoha.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.entity.npc.Quests;
import org.jetbrains.annotations.Nullable;

final class QuestIcons {
    static final ResourceLocation ALL = PlayerMenuScreen.menu("frames/all_quest_indicator.png");
    static final ResourceLocation SLAY = PlayerMenuScreen.menu("frames/slaying_quest_indicator.png");
    static final ResourceLocation GATHER = PlayerMenuScreen.menu("frames/delivery_quest_indicator.png");
    static final ResourceLocation HUNT = PlayerMenuScreen.menu("frames/investigation_quest_indicator.png");
    static final ResourceLocation SURVIVE = PlayerMenuScreen.menu("frames/survival_quest_indicator.png");
    private static final ResourceLocation BAR_EMPTY = PlayerMenuScreen.menu("frames/quest_bar_empty.png");
    private static final ResourceLocation BAR_FILL = PlayerMenuScreen.menu("frames/quest_bar_fill.png");
    private static final ResourceLocation BAR_DONE = PlayerMenuScreen.menu("frames/quest_bar_done.png");
    static final int ICON = 16;
    static final int DIVIDER_SIZE = 12;
    static final int BAR_H = 5;
    private static final int BAR_TEX_W = 64;

    private QuestIcons() {
    }

    @Nullable
    static Quests.Objective objective(String id) {
        Quests.Quest quest = Quests.byId(id);
        return quest == null ? null : quest.objective().group();
    }

    static ResourceLocation icon(@Nullable Quests.Objective objective) {
        if (objective == null) {
            return ALL;
        }
        return switch (objective) {
            case SLAY -> SLAY;
            case GATHER -> GATHER;
            case HUNT -> HUNT;
            case SURVIVE -> SURVIVE;
            default -> ALL;
        };
    }

    static void icon(GuiGraphics graphics, @Nullable Quests.Objective objective, int x, int y) {
        graphics.blit(icon(objective), x, y, 0F, 0F, ICON, ICON, ICON, ICON);
    }

    static void divider(GuiGraphics graphics, int x, int y) {
        DialogueFrame.draw(graphics, x, y, DIVIDER_SIZE, DIVIDER_SIZE);
    }

    static void bar(GuiGraphics graphics, int x, int y, int width, float filled, boolean done) {
        graphics.blit(BAR_EMPTY, x, y, width, BAR_H, 0F, 0F, BAR_TEX_W, BAR_H, BAR_TEX_W, BAR_H);
        int inner = Math.round((width - 2) * Math.max(0F, Math.min(1F, filled)));
        if (inner > 0) {
            graphics.blit(done ? BAR_DONE : BAR_FILL, x + 1, y + 1, inner, BAR_H - 2, 0F, 0F, BAR_TEX_W, BAR_H - 2,
                    BAR_TEX_W, BAR_H - 2);
        }
    }
}
