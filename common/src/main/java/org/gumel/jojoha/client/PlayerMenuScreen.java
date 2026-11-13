package org.gumel.jojoha.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.ClientPlayerDataCache;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.StandPlacement;
import org.gumel.jojoha.skill.Inflicts;
import org.gumel.jojoha.skilltree.SkillTrees;
import org.gumel.jojoha.stand.StandShadowForm;
import org.gumel.jojoha.stand.StandType;
import org.gumel.jojoha.stand.StandTypes;
import org.gumel.jojoha.stand.workshop.BarrageShout;
import org.gumel.jojoha.stand.workshop.StancePreset;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class PlayerMenuScreen extends Screen {
    private static final ResourceLocation BASE = menu("frames/player_menu_base.png");
    private static final ResourceLocation BASE_TREE = menu("frames/player_menu_skilltree.png");

    private static final ResourceLocation PASSIVE_FRAME = menu("frames/passive_trait_path_frame.png");

    private static final ResourceLocation POWER_CHART = menu("frames/power_chart.png");

    private static final ResourceLocation BUTTON_PLAYER = menu("buttons/player_stat_button.png");
    private static final ResourceLocation BUTTON_STAND = menu("buttons/stand_stat_button.png");

    static final ResourceLocation TRUST_FULL = menu("frames/stand_trust_bar.png");
    private static final ResourceLocation TRUST_EMPTY = menu("frames/stand_trust_bar_empty.png");

    private static final ResourceLocation SKILL_FRAME = menu("frames/skill_display_frame.png");
    private static final ResourceLocation SKILL_BACKGROUND = menu("frames/skills_background.png");
    static final ResourceLocation SKILL_ICON_FRAME = menu("frames/skill_icon_frame.png");
    static final ResourceLocation SKILL_BASE_ICON = menu("frames/skill_base_icon.png");
    private static final ResourceLocation CB_FLAG = menu("frames/cb_flag.png");

    static final int TOTAL_W = 341;
    private static final int BASE_H = 165;

    private static final float FILL = 0.95F;

    private static final float CENTRE_MS = 190F;

    private static final int CENTRE_DROP = 10;

    static final int LEFT_X = 5, LEFT_Y = 20, LEFT_W = 96, LEFT_H = 140;

    static final int TRUST_X = 109, TRUST_Y = 10, TRUST_W = 124, TRUST_H = 12;

    private static final int TRUST_EMPTY_X = 3, TRUST_EMPTY_Y = 2, TRUST_EMPTY_W = 117, TRUST_EMPTY_H = 8;

    static final int CHART_X = 135, CHART_Y = 31, CHART_W = 70, CHART_H = 70;

    static final int RIGHT_X = 240, RIGHT_Y = 11;
    private static final int SKILL_FRAME_X = 0, SKILL_FRAME_Y = 0;
    private static final int SKILL_FRAME_W = 96, SKILL_FRAME_H = 149;

    static final int SKILL_BG_X = 4, SKILL_BG_Y = 13;
    static final int SKILL_BG_W = 88, SKILL_BG_H = 132;

    private static final int SKILL_RING_X = 36, SKILL_RING_W = 25, SKILL_RING_H = 23;

    static final int PRESET_X = 40, PRESET_Y = 3;
    static final int PRESET_SIZE = 18;

    static final int ICON = 21;
    static final int ICON_GAP = 4;

    static final int ROW_PITCH = ICON + ICON_GAP;

    private static final int SCROLL_STEP = ROW_PITCH;

    private static final int GRID_TOP = 13;
    static final int GRID_Y = SKILL_BG_Y + GRID_TOP;

    static final int GRID_VIEW_H = SKILL_BG_Y + SKILL_BG_H - GRID_Y - 2;

    private static final int MENU_BOTTOM = BASE_H;

    private static final int PAGE_BTN_PEEK = 10;

    private static final int PAGE_BTN_LIFT = 4;
    private static final int PAGE_BTN_ACTIVE_Y = MENU_BOTTOM - PAGE_BTN_LIFT;

    private static final int PAGE_BTN_MAX_H = 23;

    enum Tab {
        HOME(104, 21, 23, "buttons/home_page_button.png"),
        SKILLS(126, 21, 23, "buttons/skills_page_button.png"),
        SKINS(148, 21, 23, "buttons/skins_page_button.png"),
        QUESTS(170, 21, 23, "buttons/quests_page_button.png");

        final int x;
        final int w;
        final int h;
        final ResourceLocation texture;

        Tab(int x, int w, int h, String texture) {
            this.x = x;
            this.w = w;
            this.h = h;
            this.texture = menu(texture);
        }

        MenuPage page() {
            return switch (this) {
                case HOME -> MenuPage.HOME;
                case SKILLS -> MenuPage.SKILLS;
                case SKINS -> MenuPage.SKINS;
                case QUESTS -> MenuPage.QUESTS;
            };
        }
    }

    static final int TREE_PAGE_X = 9, TREE_PAGE_Y = 18;
    static final int TREE_PAGE_W = 323, TREE_PAGE_H = 138;

    static final int TREE_TAB_W = 28, TREE_TAB_H = 32, TREE_TAB_PITCH = 32;
    static final int TREE_TAB_X = TREE_PAGE_X + 2;
    static final int TREE_TAB_Y = -18;
    static final int TREE_TAB_DISC = 18;
    static final int TREE_TAB_DISC_X = 5, TREE_TAB_DISC_Y = 8;

    static final ResourceLocation TREE_TAB_SELECTED = menu("buttons/tab_selected.png");
    static final ResourceLocation TREE_TAB = menu("buttons/tab_unselected.png");
    static final int TREE_TAB_LOW = 7;
    static final int TREE_TAB_DISC_LOW_Y = 4;

    private static final int VIEW_SWAP_SIZE = 19;
    private static final int VIEW_SWAP_PIN_Y = 1;

    static final int TREE_TOP_EDGE_Y = 6;
    private static final int VIEW_SWAP_X = LEFT_X + (LEFT_W - VIEW_SWAP_SIZE) / 2;
    private static final int VIEW_SWAP_Y = LEFT_Y + VIEW_SWAP_PIN_Y - VIEW_SWAP_SIZE / 2;

    static final int TOP_OVERHANG = Math.max(-VIEW_SWAP_Y, -TREE_TAB_Y);

    int treeX() {
        return TREE_PAGE_X;
    }

    int treeY() {
        return TREE_PAGE_Y;
    }

    int treeW() {
        return TREE_PAGE_W;
    }

    int treeH() {
        return TREE_PAGE_H;
    }

    static final int FLAG_W = 13, FLAG_H = 18;

    private static final float FLAG_HOVER_LIFT = 1.35F;
    static final int FLAG_X = 231;
    private static final int FLAG_RAISED_Y = MENU_BOTTOM - (FLAG_H - PAGE_BTN_PEEK);
    private static final int FLAG_LOWERED_Y = MENU_BOTTOM;

    static final int CB_W = 239, CB_H = 30;

    static final int CB_Y = MENU_BOTTOM + PAGE_BTN_MAX_H + 2;

    private static final int TOTAL_H = MENU_BOTTOM + PAGE_BTN_MAX_H + TOP_OVERHANG;

    private static final int TOTAL_H_WITH_BAR = CB_Y + CB_H + TOP_OVERHANG;

    static final int SCREEN_EDGE = 4;

    static final int COLOR_LOCKED = 0xA0202028;

    static final int COLOR_TEXT = 0xFFFFFF;
    static final int COLOR_ACCENT = 0xFFFFFF;
    static final int COLOR_DIM = 0xFFA0A0A0;
    private static final int COLOR_SLOT = 0xFF000000;
    static final int COLOR_HOVER = 0xFF7FE3FF;
    private static final int COLOR_BACKDROP = 0xC0101010;

    static final int COLOR_FIGURE = 0xFFFFFFFF;

    private static final float TEXT_TARGET = 0.75F;

    static boolean showingStand;
    static SkillBook.Category category = SkillBook.Category.STAND;

    int skillScroll;

    final SkillTreeView treeView = new SkillTreeView();

    long pageChangedAt;

    private static final int PAGE_SLIDE = 6;

    enum MenuPage { HOME, SKILLS, SKINS, QUESTS }

    List<Inflicts.Status> keywords = List.of();
    ResourceLocation previewMove;

    private PinnedTip pinned;

    private int[] pinAt;

    boolean detailed;

    boolean more;

    private static final ResourceLocation SKIN_BACKGROUND = menu("frames/player_menu_skin_background.png");
    static final ResourceLocation SKIN_ARROW_LEFT = menu("buttons/arrow_left.png");
    static final ResourceLocation SKIN_ARROW_LEFT_HOVER = menu("buttons/arrow_left_hovered.png");
    static final ResourceLocation SKIN_ARROW_RIGHT = menu("buttons/arrow_right.png");
    static final ResourceLocation SKIN_ARROW_RIGHT_HOVER = menu("buttons/arrow_right_hovered.png");

    static final int SKIN_BG_X = 6, SKIN_BG_Y = 15, SKIN_BG_W = 329, SKIN_BG_H = 144;

    static final float SKIN_ANCHOR = 1.0F;

    static ResourceLocation skinCursorStand;

    static MenuPage page = MenuPage.HOME;

    static boolean combatBarOpen;

    long barChangedAt = Long.MIN_VALUE;
    float barProgressAtChange;

    private int barLift;

    ResourceLocation picked;

    int awaitingSlot = -1;

    boolean utilityBar(JojohaPlayerData data) {
        return bar.utilityBar(data);
    }

    static boolean fitsBar(JojohaPlayerData data, ResourceLocation id, boolean utility) {
        return MenuCombatBar.fitsBar(data, id, utility);
    }

    void pick(ResourceLocation id) {
        picked = id;
        awaitingSlot = -1;
    }
    private long openedAt;

    int scale = 1;

    float textScale = 1F;

    private int offsetX;
    int offsetY;

    double localMouseX;
    double localMouseY;

    List<Component> tooltip;

    final MenuInfoPage info = new MenuInfoPage(this);
    final MenuSkillTree tree = new MenuSkillTree(this);
    final MenuCombatBar bar = new MenuCombatBar(this);
    final MenuSkins skins = new MenuSkins(this);
    final MenuQuests quests = new MenuQuests(this);
    final MenuStyle style = new MenuStyle(this);
    final MenuSections sections = new MenuSections(this);
    final MenuPlacement placement = new MenuPlacement(this);
    final MenuPoses poses = new MenuPoses(this);

    Font font() {
        return this.font;
    }

    Minecraft minecraft() {
        return this.minecraft;
    }

    public PlayerMenuScreen() {
        super(Component.translatable("menu.jojoha.title"));
    }

    @Override
    protected void init() {
        openedAt = System.currentTimeMillis();
        SkillIcons.forget();

        SkillBook.Category settled = category.settle(ClientPlayerDataCache.data);
        if (settled != category) {
            category = settled;
            skillScroll = 0;
        }

        treeView.show(treeFor(category));

        Minecraft minecraft = Minecraft.getInstance();
        for (ResourceLocation texture : List.of(BASE, BASE_TREE, PASSIVE_FRAME, SKILL_FRAME,
                SKILL_BACKGROUND, POWER_CHART, TRUST_FULL, TRUST_EMPTY,
                SKIN_BACKGROUND, SKIN_ARROW_LEFT,
                SKIN_ARROW_LEFT_HOVER, SKIN_ARROW_RIGHT, SKIN_ARROW_RIGHT_HOVER)) {
            minecraft.getTextureManager().getTexture(texture).setFilter(false, false);
        }
    }

    private void layout() {
        scale = Math.max(1, (int) Math.min(this.width * FILL / TOTAL_W,
                this.height * FILL / TOTAL_H));

        while (scale > 1 && TOTAL_H_WITH_BAR * scale > this.height - 2 * SCREEN_EDGE) {
            scale--;
        }

        textScale = smallTextScale(scale);
        offsetX = (this.width - TOTAL_W * scale) / 2;
        offsetY = (this.height - TOTAL_H * scale) / 2 + TOP_OVERHANG * scale;
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, this.width, this.height, COLOR_BACKDROP);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.setColor(1F, 1F, 1F, 1F);

        layout();

        barLift = Math.round(ease(barProgress()) * neededLift());

        localMouseX = (mouseX - offsetX) / (double) scale;
        localMouseY = (mouseY - offsetY + barLift) / (double) scale;
        tooltip = null;
        keywords = List.of();
        previewMove = null;
        more = false;
        detailed = hasShiftDown() || pinAt != null;

        float elapsed = System.currentTimeMillis() - openedAt;
        float centreOpen = Mth.clamp(elapsed / CENTRE_MS, 0F, 1F);

        JojohaPlayerData data = ClientPlayerDataCache.data;
        if (!data.stand.isPresent()) {
            StandPortrait.forget();
        }

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(offsetX, offsetY - barLift, 0F);
        guiGraphics.pose().scale(scale, scale, 1F);

        int drop = Math.round((1F - ease(centreOpen)) * CENTRE_DROP);

        guiGraphics.setColor(1F, 1F, 1F, centreOpen);

        drawPageTabs(guiGraphics, drop);
        drawFlag(guiGraphics, drop, centreOpen);

        drawPanel(guiGraphics, drop, centreOpen);
        guiGraphics.setColor(1F, 1F, 1F, 1F);

        float bar = barProgress();
        if (bar > 0F && centreOpen >= 1F) {
            guiGraphics.setColor(1F, 1F, 1F, bar);
            drawCombatBar(guiGraphics, data);
            guiGraphics.setColor(1F, 1F, 1F, 1F);
        }

        if (centreOpen >= 1F) {
            int pageSlide = Math.round((1F - ease(pageProgress())) * PAGE_SLIDE);

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0F, pageSlide, 0F);

            if (page == MenuPage.SKILLS) {
                renderSkillTree(guiGraphics, data);
            } else if (page == MenuPage.SKINS) {
                renderSkinsPage(guiGraphics, data);
            } else if (page == MenuPage.QUESTS) {
                quests.render(guiGraphics);
            } else {
                renderLeftPage(guiGraphics, data);
                renderCentre(guiGraphics, data);
                renderSkillPage(guiGraphics, data);
            }

            guiGraphics.pose().popPose();
        }

        drawTabTooltips();
        viewSwapTooltip();

        guiGraphics.pose().popPose();

        if (pinAt != null) {
            if (tooltip != null) {
                pinned = new PinnedTip(tooltip, keywords, pinAt[0], pinAt[1]);
            }
            pinAt = null;
        }

        if (pinned != null) {
            pinned.draw(guiGraphics, this.font, mouseX, mouseY, this.width, this.height, scale, textScale);
        }

        if (tooltip != null && (pinned == null || !pinned.contains(mouseX, mouseY))) {
            List<Component> shown = tooltip;
            if ((more || !keywords.isEmpty()) && pinned == null) {
                shown = new ArrayList<>(tooltip);
                if (more) {
                    shown.add(Component.translatable("menu.jojoha.hint.rules")
                            .withStyle(style -> style.withColor(COLOR_DIM)));
                }
                shown.add(Component.translatable("menu.jojoha.hint.pin")
                        .withStyle(style -> style.withColor(COLOR_DIM)));
            }
            drawTooltip(guiGraphics, shown, mouseX, mouseY);
        }

        if (tooltip == null) {
            MovePreview.idle();
        }
    }

    private static float smallTextScale(int menuScale) {
        int steps = (int) (menuScale * TEXT_TARGET);
        if (steps < 1) {
            return TEXT_TARGET;
        }
        return steps / (float) menuScale;
    }

    private void drawPanel(GuiGraphics guiGraphics, int drop, float alpha) {
        if (page == MenuPage.SKILLS) {
            fillSlot(guiGraphics, TREE_PAGE_X - 4, TREE_PAGE_Y - 7 - drop, TREE_PAGE_W + 8, TREE_PAGE_H + 11, alpha);
            drawTreeTabs(guiGraphics, drop, false);
            blit(guiGraphics, BASE_TREE, 0, -drop, TOTAL_W, BASE_H);
            drawTreeTabs(guiGraphics, drop, true);
            return;
        }
        if (page == MenuPage.SKINS) {
            fillSlot(guiGraphics, TREE_PAGE_X - 4, TREE_PAGE_Y - 7 - drop, TREE_PAGE_W + 8, TREE_PAGE_H + 11, alpha);
            drawSectionTabs(guiGraphics, drop, false);
            blit(guiGraphics, BASE_TREE, 0, -drop, TOTAL_W, BASE_H);
            drawSectionTabs(guiGraphics, drop, true);
            blit(guiGraphics, SKIN_BACKGROUND, SKIN_BG_X, SKIN_BG_Y - drop, SKIN_BG_W, SKIN_BG_H);
            return;
        }

        if (page == MenuPage.QUESTS) {
            fillSlot(guiGraphics, TREE_PAGE_X - 4, TREE_PAGE_Y - 7 - drop, TREE_PAGE_W + 8, TREE_PAGE_H + 11, alpha);
            blit(guiGraphics, BASE_TREE, 0, -drop, TOTAL_W, BASE_H);
            return;
        }

        blit(guiGraphics, BASE, 0, -drop, TOTAL_W, BASE_H);
        blit(guiGraphics, PASSIVE_FRAME, LEFT_X, LEFT_Y - drop, LEFT_W, LEFT_H);

        blit(guiGraphics, SKILL_FRAME, RIGHT_X + SKILL_FRAME_X, RIGHT_Y + SKILL_FRAME_Y - drop,
                SKILL_FRAME_W, SKILL_FRAME_H);
        blit(guiGraphics, SKILL_BACKGROUND, RIGHT_X + SKILL_BG_X, RIGHT_Y + SKILL_BG_Y - drop,
                SKILL_BG_W, SKILL_BG_H);
        blitRegion(guiGraphics, SKILL_FRAME, RIGHT_X + SKILL_RING_X, RIGHT_Y - drop, SKILL_RING_X, 0F,
                SKILL_RING_W, SKILL_RING_H, SKILL_FRAME_W, SKILL_FRAME_H);

        blit(guiGraphics, POWER_CHART, CHART_X, CHART_Y - drop, CHART_W, CHART_H);

        blit(guiGraphics, TRUST_FULL, TRUST_X, TRUST_Y - drop, TRUST_W, TRUST_H);
        blit(guiGraphics, TRUST_EMPTY, TRUST_X + TRUST_EMPTY_X, TRUST_Y + TRUST_EMPTY_Y - drop,
                TRUST_EMPTY_W, TRUST_EMPTY_H);

        blit(guiGraphics, showingStand ? BUTTON_STAND : BUTTON_PLAYER, VIEW_SWAP_X, VIEW_SWAP_Y - drop,
                VIEW_SWAP_SIZE, VIEW_SWAP_SIZE);
    }

    private void viewSwapTooltip() {
        if (!homeLayout() || !within(VIEW_SWAP_X, VIEW_SWAP_Y, VIEW_SWAP_SIZE, VIEW_SWAP_SIZE)) {
            return;
        }
        tooltip = List.of(
                Component.literal(showingStand ? "Stand stats" : "Player stats")
                        .withStyle(style -> style.withColor(COLOR_ACCENT)),
                Component.literal(showingStand ? "Click to show yours" : "Click to show your Stand's")
                        .withStyle(style -> style.withColor(COLOR_DIM)));
    }

    private static float ease(float t) {
        float u = 1F - t;
        return 1F - u * u * u;
    }

    private void renderLeftPage(GuiGraphics guiGraphics, JojohaPlayerData data) {
        info.renderLeftPage(guiGraphics, data);
    }

    void describe(List<Component> lines, String nameKey, String descKey) {
        info.describe(lines, nameKey, descKey);
    }

    private void renderCentre(GuiGraphics guiGraphics, JojohaPlayerData data) {
        info.renderCentre(guiGraphics, data);
    }

    private float pageProgress() {
        return tree.pageProgress();
    }

    private static SkillTrees.Tree treeFor(SkillBook.Category category) {
        return MenuSkillTree.treeFor(category);
    }

    static boolean contains(float[][] shape, float x, float y) {
        return MenuInfoPage.contains(shape, x, y);
    }

    static float progress(int value, int from, int to) {
        return MenuInfoPage.progress(value, from, to);
    }

    void clipped(GuiGraphics guiGraphics, int x, int y, int w, int h, Runnable draw) {
        info.clipped(guiGraphics, x, y, w, h, draw);
    }

    private int neededLift() {
        return info.neededLift();
    }

    float barProgress() {
        return info.barProgress();
    }

    int screenX(int local) {
        return offsetX + local * scale;
    }

    int screenY(int local) {
        return offsetY - barLift + local * scale;
    }

    boolean within(int x, int y, int w, int h) {
        return localMouseX >= x && localMouseX < x + w && localMouseY >= y && localMouseY < y + h;
    }

    static StandType standType(JojohaPlayerData data) {
        return data.stand.isPresent() ? StandTypes.byIdOrDefault(data.stand.standId()) : null;
    }

    private static void fillSlot(GuiGraphics guiGraphics, int x, int y, int w, int h, float alpha) {
        int a = Mth.clamp(Math.round(alpha * 255F), 0, 255);
        guiGraphics.fill(x, y, x + w, y + h, (a << 24) | (COLOR_SLOT & 0xFFFFFF));
    }

    static void blit(GuiGraphics guiGraphics, ResourceLocation texture, int x, int y,
                             int w, int h) {
        blitRegion(guiGraphics, texture, x, y, 0F, 0F, w, h, w, h);
    }

    static void blitRegion(GuiGraphics guiGraphics, ResourceLocation texture, int x, int y, float u, float v,
                           int w, int h, int textureW, int textureH) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        guiGraphics.blit(texture, x, y, u, v, w, h, textureW, textureH);
        RenderSystem.disableBlend();
    }

    void centred(GuiGraphics guiGraphics, String text, int centreX, int y, int colour) {
        guiGraphics.drawCenteredString(this.font, text, centreX, y, colour);
    }

    Component figures(Component text) {
        return Figures.highlight(text, COLOR_DIM, COLOR_FIGURE);
    }

    private void renderSkillPage(GuiGraphics guiGraphics, JojohaPlayerData data) {
        tree.renderSkillPage(guiGraphics, data);
    }

    private void drawTreeTabs(GuiGraphics guiGraphics, int drop, boolean front) {
        tree.drawTreeTabs(guiGraphics, drop, front);
    }

    private void renderSkillTree(GuiGraphics guiGraphics, JojohaPlayerData data) {
        tree.renderSkillTree(guiGraphics, data);
    }

    private void renderSkinsPage(GuiGraphics guiGraphics, JojohaPlayerData data) {
        skins.renderSkinsPage(guiGraphics, data);
    }

    static final StancePreset[] PRESETS = StancePreset.values();

    void drawPresets(GuiGraphics guiGraphics, JojohaPlayerData data, StandType type) {
        skins.drawPresets(guiGraphics, data, type);
    }

    boolean clickedPresets(JojohaPlayerData data, StandType type) {
        return skins.clickedPresets(data, type);
    }

    static void style(JojohaPlayerData data, int summon, int shout, int rest) {
        MenuStyle.style(data, summon, shout, rest);
    }

    void renderStyle(GuiGraphics guiGraphics, JojohaPlayerData data) {
        style.renderStyle(guiGraphics, data);
    }

    static Component shoutSource(BarrageShout shout) {
        return MenuStyle.shoutSource(shout);
    }

    boolean clickedStyle(int button) {
        return style.clickedStyle(button);
    }

    static Component standName(ResourceLocation id) {
        return MenuStyle.standName(id);
    }

    void renderLooks(GuiGraphics guiGraphics, JojohaPlayerData data) {
        style.renderLooks(guiGraphics, data);
    }

    boolean clickedLooks(int button) {
        return style.clickedLooks(button);
    }

    enum Section {
        SKIN("skin", Items.LEATHER_CHESTPLATE),
        STANCE("stance", Items.COMPASS),
        STYLE("style", Items.FIREWORK_ROCKET);

        final String key;
        final ItemStack icon;

        Section(String key, Item icon) {
            this.key = key;
            this.icon = new ItemStack(icon);
        }
    }

    static Section section = Section.SKIN;

    static final int BUTTON_W = 70, BUTTON_H = 16;

    static final int CARD_X = SKIN_BG_X + 10, CARD_Y = SKIN_BG_Y + 10;
    static final int CARD_W = 36, CARD_H = 48, CARD_PITCH_X = 40, CARD_PITCH_Y = 52;
    static final int CARD_COLS = 5, CARD_ROWS = 2;

    static final int LOOK_VIEW_X = SKIN_BG_X + 214, LOOK_VIEW_Y = SKIN_BG_Y + 6;
    static final int LOOK_VIEW_W = 108, LOOK_VIEW_H = 108;

    static final int COLOR_CARD = 0x66000000, COLOR_CARD_LOCKED = 0xB0000000;

    static int skinScroll;

    static final int REST_Y = SKIN_BG_Y + 124, REST_W = 42, REST_H = 14;

    static final int PLACE_VIEW_X = SKIN_BG_X + 98, PLACE_VIEW_Y = SKIN_BG_Y + 8;
    static final int PLACE_VIEW_W = 116, PLACE_VIEW_H = 128;

    static final int COLOR_MAP = 0x66000000, COLOR_MAP_LINE = 0x22FFFFFF, COLOR_ROW_OVER = 0x33FFFFFF;
    static final int COLOR_ROW_PICKED = 0x407FE3FF;

    static final int IDLE_LIST_X = SKIN_BG_X + 172, IDLE_LIST_Y = SKIN_BG_Y + 18, IDLE_LIST_W = 149;
    static final int IDLE_ROW_H = 12, IDLE_ROWS = 7;

    private static final float POSE_TURN_PER_PIXEL = 1.5F;

    static int poseSlot = -1;

    static int poseSlider = -1;
    static boolean poseTurning;
    static float poseYaw;

    static int idleScroll;
    static boolean idleScanned;

    private void drawSectionTabs(GuiGraphics guiGraphics, int drop, boolean front) {
        sections.drawSectionTabs(guiGraphics, drop, front);
    }

    void sectionTabTooltip() {
        sections.sectionTabTooltip();
    }

    void open(Section next) {
        sections.open(next);
    }

    boolean clickedSectionTab() {
        return sections.clickedSectionTab();
    }

    void button(GuiGraphics guiGraphics, int x, int y, int w, int h, Component label, boolean active) {
        sections.button(guiGraphics, x, y, w, h, label, active);
    }

    void renderPlacement(GuiGraphics guiGraphics, JojohaPlayerData data) {
        placement.renderPlacement(guiGraphics, data);
    }

    static String signed(float value, int decimals) {
        return MenuPlacement.signed(value, decimals);
    }

    static void dot(GuiGraphics guiGraphics, int x, int y, int colour) {
        MenuPlacement.dot(guiGraphics, x, y, colour);
    }

    static void mark(GuiGraphics guiGraphics, int x, int y, float turn, int colour) {
        MenuPlacement.mark(guiGraphics, x, y, turn, colour);
    }

    static boolean previewsPlacement(StandType type) {
        return MenuPlacement.previewsPlacement(type);
    }

    boolean clickedPlacement(int button) {
        return placement.clickedPlacement(button);
    }

    static float snap(float value) {
        return MenuPlacement.snap(value);
    }

    static void place(JojohaPlayerData data, StandPlacement placement) {
        MenuPlacement.place(data, placement);
    }

    static void closePose() {
        MenuPoses.closePose();
    }

    static void setPose(int channel, float value) {
        MenuPoses.setPose(channel, value);
    }

    private void slidePose() {
        poses.slidePose();
    }

    private boolean scrolledPoseEditor(double scrollY) {
        return poses.scrolledPoseEditor(scrollY);
    }

    void renderIdles(GuiGraphics guiGraphics) {
        poses.renderIdles(guiGraphics);
    }

    static String idleLabel(String key) {
        return MenuPoses.idleLabel(key);
    }

    void leftWrapped(GuiGraphics guiGraphics, String text, int x, int y, int maxWidth, int colour) {
        List<FormattedCharSequence> lines =
                this.font.split(Component.literal(text), Math.round(maxWidth / textScale));
        int step = lineStep();
        for (int i = 0; i < lines.size(); i++) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(x, y + i * step, 0);
            guiGraphics.pose().scale(textScale, textScale, 1F);
            guiGraphics.drawString(this.font, lines.get(i), 0, 0, colour, true);
            guiGraphics.pose().popPose();
        }
    }

    boolean clickedIdles(int button) {
        return poses.clickedIdles(button);
    }

    void centredWrapped(GuiGraphics guiGraphics, String text, int centreX, int y, int maxWidth, int colour) {
        List<FormattedCharSequence> lines =
                this.font.split(Component.literal(text), Math.round(maxWidth / textScale));
        int step = lineStep();
        int top = y - (lines.size() - 1) * step / 2;
        for (int i = 0; i < lines.size(); i++) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(centreX, top + i * step, 0);
            guiGraphics.pose().scale(textScale, textScale, 1F);
            guiGraphics.drawString(this.font, lines.get(i), -this.font.width(lines.get(i)) / 2, 0, colour, true);
            guiGraphics.pose().popPose();
        }
    }

    private boolean clickedSkins(int button) {
        return skins.clickedSkins(button);
    }

    private static boolean homeLayout() {
        return page == MenuPage.HOME;
    }

    private void drawPageTabs(GuiGraphics guiGraphics, int drop) {
        for (Tab tab : Tab.values()) {
            blit(guiGraphics, tab.texture, tab.x, tabY(tab) - drop, tab.w, tab.h);
        }
    }

    private void drawTabTooltips() {
        for (Tab tab : Tab.values()) {
            if (!within(tab.x, tabHitY(tab), tab.w, tabHitHeight(tab))) {
                continue;
            }
            String name = switch (tab) {
                case HOME -> "Home";
                case SKILLS -> "Skill tree";
                case SKINS -> "Workshop";
                case QUESTS -> "Quests";
            };
            tooltip = tab.page() != null
                    ? List.of(Component.literal(name).withStyle(style -> style.withColor(COLOR_ACCENT)))
                    : List.of(Component.literal(name).withStyle(style -> style.withColor(COLOR_ACCENT)),
                            Component.translatable("menu.jojoha.coming_soon").withStyle(style -> style.withColor(COLOR_DIM)));
            return;
        }
    }

    private void drawFlag(GuiGraphics guiGraphics, int drop, float centreOpen) {
        boolean over = within(FLAG_X, flagHitTop(), FLAG_W, flagHitHeight());

        if (over) {
            guiGraphics.setColor(FLAG_HOVER_LIFT, FLAG_HOVER_LIFT, FLAG_HOVER_LIFT, centreOpen);
        }

        blit(guiGraphics, CB_FLAG, FLAG_X, flagY() - drop, FLAG_W, FLAG_H);

        if (over) {
            guiGraphics.setColor(1F, 1F, 1F, centreOpen);
        }
    }

    int flagHitTop() {
        return Math.max(flagY(), MENU_BOTTOM);
    }

    int flagHitHeight() {
        return flagY() + FLAG_H - flagHitTop();
    }

    private int flagY() {
        return Math.round(Mth.lerp(ease(barProgress()), FLAG_RAISED_Y, FLAG_LOWERED_Y));
    }

    private int tabY(Tab tab) {
        return page == tab.page() ? PAGE_BTN_ACTIVE_Y : PAGE_BTN_ACTIVE_Y - (tab.h - PAGE_BTN_PEEK);
    }

    int tabHitHeight(Tab tab) {
        return tabY(tab) + tab.h - tabHitY(tab);
    }

    int tabHitY(Tab tab) {
        return Math.max(tabY(tab), MENU_BOTTOM);
    }

    private void drawCombatBar(GuiGraphics guiGraphics, JojohaPlayerData data) {
        bar.drawCombatBar(guiGraphics, data);
    }

    private int maxScroll() {
        return tree.maxScroll();
    }

    private boolean clickedTree(int button) {
        return tree.clickedTree(button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && treeView.dragging()) {
            treeView.drag((mouseX - offsetX) / (double) scale,
                    (mouseY - offsetY + barLift) / (double) scale);
            return true;
        }
        if (button == 0 && poseSlider >= 0) {
            slidePose();
            return true;
        }
        if (button == 0 && poseTurning) {
            poseYaw = Mth.wrapDegrees(poseYaw + (float) (dragX / scale) * POSE_TURN_PER_PIXEL);
            return true;
        }
        if (button == 0 && PlacementPreview.spinning()) {
            JojohaPlayerData data = ClientPlayerDataCache.data;
            StandPlacement placed = data.standPlacement;
            data.standPlacement = new StandPlacement(placed.side(), placed.height(), placed.ahead(),
                    PlacementPreview.spin(placed.turn(), dragX / scale));
            return true;
        }
        if (button == 0 && PlacementPreview.turning()) {
            PlacementPreview.turn(dragX / scale, dragY / scale);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && treeView.dragging()) {
            treeView.endDrag();
            return true;
        }
        if (button == 0 && (poseSlider >= 0 || poseTurning)) {
            poseSlider = -1;
            poseTurning = false;
            return true;
        }
        if (button == 0 && PlacementPreview.spinning()) {
            PlacementPreview.drop();
            JojohaPlayerData data = ClientPlayerDataCache.data;
            StandPlacement placed = data.standPlacement;
            place(data, new StandPlacement(placed.side(), placed.height(), placed.ahead(), Math.round(placed.turn())));
            return true;
        }
        if (button == 0 && PlacementPreview.turning()) {
            PlacementPreview.drop();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private boolean clickedStrip(int button) {
        return bar.clickedStrip(button);
    }

    private boolean clickedSkill(JojohaPlayerData data, int button) {
        return tree.clickedSkill(data, button);
    }

    static void click() {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(
                        SoundEvents.UI_BUTTON_CLICK, 1F));
    }

    private void drawTooltip(GuiGraphics guiGraphics, List<Component> lines, int mouseX, int mouseY) {
        JojohaTooltip.Pane pane = previewMove == null || pinned != null ? null
                : MovePreview.pane(ClientPlayerDataCache.data, previewMove);
        if (pane == null) {
            MovePreview.idle();
        }
        JojohaTooltip.drawCompact(guiGraphics, this.font, lines, mouseX, mouseY, this.width, this.height,
                scale, textScale, pane);
    }

    private static void nineSlice(GuiGraphics guiGraphics, ResourceLocation texture,
                                  int x, int y, int w, int h) {
        nineSlice(guiGraphics, texture, x, y, w, h, JojohaTooltip.BORDER);
    }

    private static void nineSlice(GuiGraphics guiGraphics, ResourceLocation texture,
                                  int x, int y, int w, int h, int border) {
        JojohaTooltip.nineSlice(guiGraphics, texture, x, y, w, h, border);
    }

    int lineStep() {
        return Math.max(1, Math.round(this.font.lineHeight * textScale));
    }

    int smallWidth(String text) {
        return Math.round(this.font.width(text) * textScale);
    }

    void smallLeft(GuiGraphics guiGraphics, String text, int x, int y, int maxWidth,
                           int colour) {
        smallRaw(guiGraphics, trim(text, Math.round(maxWidth / textScale)), x, y, colour);
    }

    void smallRaw(GuiGraphics guiGraphics, String text, int x, int y, int colour) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 0);
        guiGraphics.pose().scale(textScale, textScale, 1F);
        guiGraphics.drawString(this.font, text, 0, 0, colour, true);
        guiGraphics.pose().popPose();
    }

    void standTooltip(GuiGraphics guiGraphics, JojohaPlayerData data, StandType type,
                              int x, int y, int w, int h) {
        if (!within(x, y, w, h)) {
            return;
        }

        int skin = data.stand.skin();

        int tint = skinTint(data, type, skin);

        tooltip = List.of(
                Component.literal(upper(data.stand.standId().getPath()))
                        .withStyle(style -> style.withColor(COLOR_ACCENT)),
                Component.translatable("menu.jojoha.skin_prefix")
                        .withStyle(style -> style.withColor(COLOR_DIM))
                        .append(type.skinName(skin)
                                .withStyle(style -> style.withColor(
                                        TextColor.fromRgb(tint)))));
    }

    static int skinTint(JojohaPlayerData data, StandType type, int skin) {
        return StandShadowForm.auraColourFor(type, skin, data.stand.trust());
    }

    String trim(String text, int maxWidth) {
        if (this.font.width(text) <= maxWidth) {
            return text;
        }

        String ellipsis = "...";
        int room = Math.max(0, maxWidth - this.font.width(ellipsis));
        return this.font.plainSubstrByWidth(text, room) + ellipsis;
    }

    static String upper(String name) {
        return name.toUpperCase(Locale.ROOT).replace('_', ' ');
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0 && page == MenuPage.SKILLS
                && within(treeX(), treeY(), treeW(), treeH())) {
            boolean across = hasShiftDown();
            treeView.scrollBy(scrollX + (across ? scrollY : 0), across ? 0 : scrollY);
            return true;
        }

        if (scrollY != 0 && page == MenuPage.SKINS && previewsPlacement(standType(ClientPlayerDataCache.data))
                && (section == Section.STANCE && within(PLACE_VIEW_X, PLACE_VIEW_Y, PLACE_VIEW_W, PLACE_VIEW_H)
                || section == Section.SKIN && within(LOOK_VIEW_X, LOOK_VIEW_Y, LOOK_VIEW_W, LOOK_VIEW_H))) {
            PlacementPreview.zoom(scrollY);
            return true;
        }

        if (scrollY != 0 && page == MenuPage.SKINS && section == Section.SKIN
                && within(CARD_X, CARD_Y, CARD_COLS * CARD_PITCH_X, CARD_ROWS * CARD_PITCH_Y)) {
            skinScroll = Math.max(0, skinScroll - (int) Math.signum(scrollY));
            return true;
        }

        if (scrollY != 0 && page == MenuPage.SKINS && section == Section.STYLE && poseSlot >= 0) {
            return scrolledPoseEditor(scrollY) || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        if (scrollY != 0 && page == MenuPage.SKINS && section == Section.STYLE
                && within(IDLE_LIST_X, IDLE_LIST_Y, IDLE_LIST_W, IDLE_ROWS * IDLE_ROW_H)) {
            idleScroll = Math.max(0, idleScroll - (int) Math.signum(scrollY));
            return true;
        }

        if (scrollY != 0
                && within(RIGHT_X + SKILL_BG_X, RIGHT_Y + GRID_Y, SKILL_BG_W, GRID_VIEW_H)) {
            int max = maxScroll();
            int moved = Mth.clamp(skillScroll - (int) Math.signum(scrollY) * SCROLL_STEP, 0, max);

            if (moved != skillScroll) {
                skillScroll = moved;
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (pinned != null && keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            pinned = null;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (pinned != null) {
            if (pinned.contains(mouseX, mouseY)) {
                if (pinned.click(mouseX, mouseY)) {
                    click();
                }
                return true;
            }
            pinned = null;
            return true;
        }

        if (tooltip != null && button == 2) {
            pinAt = new int[]{(int) mouseX, (int) mouseY};
            click();
            return true;
        }

        if (clickedStrip(button)) {
            return true;
        }

        if (page == MenuPage.SKILLS && clickedTree(button)) {
            return true;
        }

        if (page == MenuPage.QUESTS && quests.clicked(button)) {
            return true;
        }

        if (page == MenuPage.SKINS) {
            return clickedSkins(button) || super.mouseClicked(mouseX, mouseY, button);
        }

        if (button == 0 && homeLayout()
                && within(VIEW_SWAP_X, VIEW_SWAP_Y, VIEW_SWAP_SIZE, VIEW_SWAP_SIZE)) {
            showingStand = !showingStand;
            click();
            return true;
        }

        if (button == 0 && within(RIGHT_X + PRESET_X, RIGHT_Y + PRESET_Y,
                PRESET_SIZE, PRESET_SIZE)) {
            category = category.next(ClientPlayerDataCache.data);
            skillScroll = 0;
            pick(null);
            treeView.show(treeFor(category));
            click();
            return true;
        }

        JojohaPlayerData data = ClientPlayerDataCache.data;

        if (clickedSkill(data, button)) {
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    static ResourceLocation menu(String fileName) {
        return ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/gui/menu/" + fileName);
    }
}
