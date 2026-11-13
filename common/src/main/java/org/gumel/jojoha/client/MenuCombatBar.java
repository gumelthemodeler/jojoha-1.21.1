package org.gumel.jojoha.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.ClientPlayerDataCache;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.skill.MoveNames;
import org.gumel.jojoha.stand.skill.StandSkills;
import java.util.ArrayList;
import java.util.List;

final class MenuCombatBar {
    private final PlayerMenuScreen screen;

    MenuCombatBar(PlayerMenuScreen screen) {
        this.screen = screen;
    }

    private static final ResourceLocation COMBAT_BAR = PlayerMenuScreen.menu("frames/playermenu_combatbar.png");

    private static final ResourceLocation MODE_DEFAULT = PlayerMenuScreen.menu("frames/mode_combat_bar_default.png");

    private static final ResourceLocation MODE_UTILITY = PlayerMenuScreen.menu("frames/mode_combat_bar_utility.png");

    private static final ResourceLocation MODE_PLAYER = PlayerMenuScreen.menu("frames/mode_combat_bar_player.png");

    private static final ResourceLocation PAGE_BAR =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/gui/hud/combat_bar/central_page_bar.png");

    private static final int CB_X = (PlayerMenuScreen.TOTAL_W - PlayerMenuScreen.CB_W) / 2;

    private static final int CB_SLOTS = 8;

    private static final int CB_SLOT_PITCH = 21;

    private static final int CB_SLOT_SIZE = SkillIcons.SIZE;

    private static final int CB_SLOT_X = CB_X + 37;

    private static final int CB_SLOT_Y = PlayerMenuScreen.CB_Y + 5;

    private static final int MODE_SIZE = 16;

    private static final int MODE_X = CB_X + 12;

    private static final int MODE_Y = PlayerMenuScreen.CB_Y + 6;

    private static final int PAGE_BAR_W = 4, PAGE_BAR_H = 19;

    private static final int PAGE_BAR_LEFT_X = CB_X + 30;

    private static final int PAGE_BAR_RIGHT_X = CB_X + 206;

    private static final int PAGE_BAR_Y = PlayerMenuScreen.CB_Y + 5;

    private static final int COLOR_SLOT_PICKED = 0x808FD8FF;

    private static int barPage;

    private Boolean editUtility;

    boolean utilityBar(JojohaPlayerData data) {
        if (!data.stand.isPresent()) {
            return false;
        }
        return editUtility != null ? editUtility : data.inUtilityStance();
    }

    static boolean fitsBar(JojohaPlayerData data, ResourceLocation id, boolean utility) {
        return utility ? StandSkills.fitsUtility(data, id)
                : StandSkills.fitsCombat(data, id);
    }

    void drawCombatBar(GuiGraphics guiGraphics, JojohaPlayerData data) {
        if (screen.picked != null && screen.awaitingSlot >= 0 && screen.picked.equals(heldInBar(data, screen.awaitingSlot))) {
            screen.picked = null;
            screen.awaitingSlot = -1;
        }

        boolean utility = utilityBar(data);
        PlayerMenuScreen.blit(guiGraphics, COMBAT_BAR, CB_X, PlayerMenuScreen.CB_Y, PlayerMenuScreen.CB_W, PlayerMenuScreen.CB_H);
        drawBarMode(guiGraphics, data);
        if (!utility) {
            drawBarPages(guiGraphics);
        }

        for (int i = 0; i < CB_SLOTS; i++) {
            int slot = utility ? StandSkills.UTILITY_SLOT + i : barPage * CB_SLOTS + i;
            int row = i;
            int x = CB_SLOT_X + i * CB_SLOT_PITCH;
            ResourceLocation held = heldInBar(data, slot);

            if (held != null) {
                drawBarSlot(guiGraphics, held, x, CB_SLOT_Y);
            }

            if (screen.picked != null && screen.within(x, CB_SLOT_Y, CB_SLOT_SIZE, CB_SLOT_SIZE)) {
                guiGraphics.fill(x, CB_SLOT_Y, x + CB_SLOT_SIZE, CB_SLOT_Y + CB_SLOT_SIZE,
                        COLOR_SLOT_PICKED);
            }

            if (screen.within(x, CB_SLOT_Y, CB_SLOT_SIZE, CB_SLOT_SIZE)) {
                List<Component> lines = new ArrayList<>();
                lines.add(Component.literal(utility ? "Utility slot " + (row + 1) : "Slot " + (slot + 1))
                        .withStyle(style -> style.withColor(PlayerMenuScreen.COLOR_ACCENT)));

                if (held != null) {
                    lines.add(Component.translatable(MoveNames.key(data, held))
                            .withStyle(style -> style.withColor(PlayerMenuScreen.COLOR_TEXT)));
                }
                lines.add(Component.literal(screen.picked != null ? "Click to place the picked move"
                        : held == null ? "Empty - pick a move first"
                        : "Right click to clear")
                        .withStyle(style -> style.withColor(PlayerMenuScreen.COLOR_DIM)));

                int keyRow = utility ? row : slot;
                Component keyLabel = StandSkillInput.slotKeyLabel(keyRow);
                if (keyLabel != null && !keyLabel.getString().isEmpty()) {
                    lines.add(Component.translatable("menu.jojoha.press", keyLabel)
                            .withStyle(style -> style.withColor(PlayerMenuScreen.COLOR_DIM)));
                    if (StandSkillInput.slotNeedsModifier(keyRow)) {
                        lines.add(Component.translatable("menu.jojoha.brackets", RawKey.modifierName())
                                .withStyle(style -> style.withColor(PlayerMenuScreen.COLOR_ACCENT)));
                    }
                }

                screen.tooltip = List.copyOf(lines);
            }
        }
    }

    private ResourceLocation heldInBar(JojohaPlayerData data, int slot) {
        int base = StandSkills.UTILITY_SLOT;
        return slot >= base ? data.barSkill(true, slot - base) : data.barSkill(false, slot);
    }

    private void drawBarMode(GuiGraphics guiGraphics, JojohaPlayerData data) {
        boolean utility = utilityBar(data);
        boolean out = data.standOut();
        PlayerMenuScreen.blit(guiGraphics, utility ? MODE_UTILITY : out ? MODE_DEFAULT : MODE_PLAYER,
                MODE_X, MODE_Y, MODE_SIZE, MODE_SIZE);

        if (screen.within(MODE_X, MODE_Y, MODE_SIZE, MODE_SIZE)) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.literal(utility ? "Utility bar" : "Combat bar")
                    .withStyle(style -> style.withColor(PlayerMenuScreen.COLOR_ACCENT)));
            lines.add(Component.literal(utility ? "Used in the Utility stance: tools and movement"
                            : out ? "Shared moves use your Stand"
                            : "Shared moves are your own, Stand moves wait")
                    .withStyle(style -> style.withColor(PlayerMenuScreen.COLOR_DIM)));
            if (data.stand.isPresent()) {
                lines.add(Component.literal(utility ? "Click to edit the Combat bar" : "Click to edit the Utility bar")
                        .withStyle(style -> style.withColor(PlayerMenuScreen.COLOR_DIM)));
            }
            screen.tooltip = List.copyOf(lines);
        }
    }

    private void drawBarPages(GuiGraphics guiGraphics) {
        PlayerMenuScreen.blit(guiGraphics, PAGE_BAR, barPage == 0 ? PAGE_BAR_LEFT_X : PAGE_BAR_RIGHT_X,
                PAGE_BAR_Y, PAGE_BAR_W, PAGE_BAR_H);

        pageTooltip(PAGE_BAR_LEFT_X, 0);
        pageTooltip(PAGE_BAR_RIGHT_X, 1);
    }

    private void pageTooltip(int x, int page) {
        if (!screen.within(x, PAGE_BAR_Y, PAGE_BAR_W, PAGE_BAR_H)) {
            return;
        }

        screen.tooltip = List.of(
                Component.literal("Page " + (page + 1))
                        .withStyle(style -> style.withColor(PlayerMenuScreen.COLOR_ACCENT)),
                Component.literal("Slots " + (page * CB_SLOTS + 1)
                        + " to " + ((page + 1) * CB_SLOTS))
                        .withStyle(style -> style.withColor(PlayerMenuScreen.COLOR_DIM)));
    }

    private void drawBarSlot(GuiGraphics guiGraphics, ResourceLocation skillId, int x, int y) {
        ResourceLocation icon = SkillIcons.of(skillId, !ClientPlayerDataCache.data.standOut());
        if (icon != null) {
            PlayerMenuScreen.blit(guiGraphics, icon, x, y, SkillIcons.SIZE, SkillIcons.SIZE);
        } else {
            guiGraphics.blit(PlayerMenuScreen.SKILL_BASE_ICON, x, y, 1F, 1F,
                    SkillIcons.SIZE, SkillIcons.SIZE, PlayerMenuScreen.ICON, PlayerMenuScreen.ICON);
            SkillIcons.drawName(guiGraphics, screen.font(), skillId, x, y, SkillIcons.SIZE);
        }
        StatusPips.draw(guiGraphics, skillId, x + SkillIcons.SIZE, y + SkillIcons.SIZE);
    }

    boolean clickedStrip(int button) {
        if (button == 0 && screen.within(PlayerMenuScreen.FLAG_X, screen.flagHitTop(), PlayerMenuScreen.FLAG_W, screen.flagHitHeight())) {
            screen.barProgressAtChange = screen.barProgress();
            screen.barChangedAt = System.currentTimeMillis();
            PlayerMenuScreen.combatBarOpen = !PlayerMenuScreen.combatBarOpen;

            if (!PlayerMenuScreen.combatBarOpen) {
                screen.pick(null);
            }
            PlayerMenuScreen.click();
            return true;
        }

        for (PlayerMenuScreen.Tab tab : PlayerMenuScreen.Tab.values()) {
            if (button != 0 || !screen.within(tab.x, screen.tabHitY(tab), tab.w, screen.tabHitHeight(tab))) {
                continue;
            }
            if (tab.page() != null && PlayerMenuScreen.page != tab.page()) {
                PlayerMenuScreen.page = tab.page();
                screen.pageChangedAt = System.currentTimeMillis();
                PlayerMenuScreen.skinCursorStand = null;
            }
            PlayerMenuScreen.click();
            return true;
        }

        return PlayerMenuScreen.combatBarOpen && clickedCombatBar(button);
    }

    private boolean clickedCombatBar(int button) {
        JojohaPlayerData barData = ClientPlayerDataCache.data;
        boolean utility = utilityBar(barData);

        if (button == 0 && barData.stand.isPresent() && screen.within(MODE_X, MODE_Y, MODE_SIZE, MODE_SIZE)) {
            editUtility = !utility;
            screen.pick(null);
            PlayerMenuScreen.click();
            return true;
        }

        if (!utility && button == 0 && screen.within(PAGE_BAR_LEFT_X, PAGE_BAR_Y, PAGE_BAR_W, PAGE_BAR_H)) {
            barPage = 0;
            PlayerMenuScreen.click();
            return true;
        }

        if (!utility && button == 0 && screen.within(PAGE_BAR_RIGHT_X, PAGE_BAR_Y, PAGE_BAR_W, PAGE_BAR_H)) {
            barPage = 1;
            PlayerMenuScreen.click();
            return true;
        }

        for (int i = 0; i < CB_SLOTS; i++) {
            int x = CB_SLOT_X + i * CB_SLOT_PITCH;
            if (!screen.within(x, CB_SLOT_Y, CB_SLOT_SIZE, CB_SLOT_SIZE)) {
                continue;
            }

            int slot = utility ? StandSkills.UTILITY_SLOT + i : barPage * CB_SLOTS + i;

            if (button == 1) {
                NetworkHandler.sendClearSlot(slot);
                PlayerMenuScreen.click();
                return true;
            }

            if (button == 0 && screen.picked != null) {
                NetworkHandler.sendEquipSkill(screen.picked, slot);
                screen.awaitingSlot = slot;
                PlayerMenuScreen.click();
                return true;
            }

            PlayerMenuScreen.click();
            return true;
        }

        return false;
    }
}
