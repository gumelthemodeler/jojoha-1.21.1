package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.ClientPlayerDataCache;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.skilltree.SkillNode;
import org.gumel.jojoha.skilltree.SkillTrees;
import org.gumel.jojoha.stand.skill.StandSkills;
import org.gumel.jojoha.data.StatPoints;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.stand.StandType;
import org.gumel.jojoha.stand.StandTypes;
import org.gumel.jojoha.stand.passive.StandPassive;
import org.gumel.jojoha.stand.passive.StandPassives;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;
import java.util.Locale;

/**
 * The player menu: three panels side by side, built on the commissioned art in
 * {@code textures/gui/menu}.
 *
 * <p>Left is who the user is - their Stand's face, its kind, its passives, and their spec and
 * trait. Centre is the numbers, switchable between the player's five and their Stand's five. The
 * right page is deliberately empty and waiting for content.
 *
 * <h2>Where the coordinates came from</h2>
 *
 * <p>Measured, not guessed. Each panel's art has its content slots cut out as fully transparent
 * rectangles, so the slots were found by decoding the PNGs and taking the bounding boxes of their
 * transparent regions. Every slot constant below is one of those boxes, and each matches the native
 * size of the art meant to sit in it exactly - the 46x74 hole in the centre panel is the size of
 * {@code playerstand_display.png} to the pixel. Nothing here is eyeballed, which is why nothing
 * here needs a fudge factor.
 *
 * <h2>Why the panels overlap, and why the slots are painted black first</h2>
 *
 * <p>The pages are not standalone rectangles. {@code page_left} carries seven columns of
 * transparent padding down its left edge and {@code page_right} the same down its right, because
 * each is drawn to butt against the centre panel. So they are placed by their <em>visible</em>
 * edges and their bounding boxes overlap by design.
 *
 * <p>Those transparent slots are also why the interface looked dimmed. The backdrop wash is drawn
 * first and the art on top of it, so the panels themselves were never tinted - but a hole in the
 * art is a hole, and the dimmed world showed through every display box. Painting each slot opaque
 * before the frame goes down is what confines the wash to the world where it belongs.
 *
 * <h2>Scale</h2>
 *
 * <p>The art is 341 pixels across and the menu is meant to fill the screen, so everything is drawn
 * under one pose transform rather than at native size. That has a consequence worth stating: a
 * pose scales geometry and text but {@code enableScissor} ignores it entirely and takes raw screen
 * coordinates, so anything clipped has to convert first - see {@link #screenX}.
 */
public final class PlayerMenuScreen extends Screen {
    private static final ResourceLocation PAGE_LEFT = menu("page_left.png");
    private static final ResourceLocation PAGE_RIGHT = menu("page_right.png");
    private static final ResourceLocation CENTRE = menu("player_menu.png");
    private static final ResourceLocation DISPLAY_FRAME = menu("playerstand_display.png");
    private static final ResourceLocation STAND_FRAME = menu("stand_display_leftpage.png");
    private static final ResourceLocation PLAYER_ROWS = menu("player_stats.png");
    private static final ResourceLocation STAND_ROWS = menu("stand_stats.png");
    private static final ResourceLocation BUTTON_PLAYER = menu("player_stat_button.png");
    private static final ResourceLocation BUTTON_STAND = menu("stand_stat_button.png");
    private static final ResourceLocation TRUST_BAR = menu("stand_trust_tier_bar.png");
    private static final ResourceLocation STAT_BUTTON = menu("stat_button.png");
    private static final ResourceLocation STAT_BUTTON_OVER = menu("stat_button_highlighted.png");
    private static final ResourceLocation TOOLTIP = menu("tooltip_texture.png");
    private static final ResourceLocation SKILL_FRAME = menu("skill_display_frame.png");
    private static final ResourceLocation SKILL_BACKGROUND = menu("skills_background.png");
    private static final ResourceLocation SKILL_ICON_FRAME = menu("skill_icon_frame.png");
    private static final ResourceLocation SKILL_BASE_ICON = menu("skill_base_icon.png");
    private static final ResourceLocation CB_FLAG = menu("cb_flag.png");
    private static final ResourceLocation TREE_FRAME = menu("skill_tree_frame.png");
    private static final ResourceLocation TREE_BACKGROUND = menu("skilltree_background.png");
    private static final ResourceLocation COMBAT_BAR = menu("playermenu_combatbar.png");
    private static final ResourceLocation MODE_DEFAULT = menu("mode_combat_bar_default.png");
    private static final ResourceLocation MODE_UTILITY = menu("mode_combat_bar_utility.png");
    private static final ResourceLocation SEPARATOR = menu("display_separator.png");
    private static final ResourceLocation SCROLL_BAR = menu("scroll_bar.png");
    private static final ResourceLocation PAGE_BAR =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/gui/hud/central_page_bar.png");
    private static final ResourceLocation HOME_PAGE_BUTTON = menu("home_page_button.png");
    private static final ResourceLocation SKILLS_PAGE_BUTTON = menu("skills_page_button.png");
    private static final ResourceLocation ARROW_LEFT = menu("left_arrow.png");
    private static final ResourceLocation ARROW_RIGHT = menu("right_arrow.png");
    private static final ResourceLocation NUMBER_PLATE = menu("stat_number_display.png");

    /**
     * The tooltip frame, and the border it is cut on.
     *
     * <p>Three pixels, measured off the art rather than guessed: the gold runs to x=2 and the black
     * interior begins at x=3, on all four sides of a 64 square. Those three pixels are drawn at
     * their own size in the corners and stretched along the edges, which is what lets one fixed
     * square wrap a box of any shape without the frame distorting.
     */
    private static final int TOOLTIP_BORDER = 3;
    private static final int TOOLTIP_TEX = 64;

    /**
     * How wide a tooltip is allowed to get before it wraps.
     *
     * <p>It used to be however wide the longest line happened to be, which for a passive
     * description was most of the screen and then off the edge of it. A width to wrap at is the
     * whole difference between a line of text and a paragraph.
     */
    private static final int TOOLTIP_MAX_W = 150;
    private static final int TOOLTIP_PAD = 4;

    // ---- the three panels ------------------------------------------------------------------------
    private static final int PAGE_W = 105, PAGE_H = 157;
    private static final int CENTRE_W = 139, CENTRE_H = 161;

    private static final int LEFT_X = 0, LEFT_Y = 2;
    private static final int CENTRE_X = 101, CENTRE_Y = 0;
    private static final int RIGHT_X = 236, RIGHT_Y = 2;

    private static final int TOTAL_W = RIGHT_X + PAGE_W;


    /**
     * How much of the screen the menu is allowed to take before it steps down a size.
     *
     * <p>A ceiling, not a target. What the menu actually gets is the largest whole-number multiple
     * of the art that fits inside it - see {@link #render}.
     */
    private static final float FILL = 0.95F;

    // ---- opening ---------------------------------------------------------------------------------
    /**
     * The centre arrives, and only then do the pages come out from behind it.
     *
     * <p>Long enough to be a movement rather than a flicker. The centre also drops the last few
     * pixels into place instead of only fading, because a fade alone reads as a thing being
     * revealed and a drop reads as a thing arriving.
     */
    private static final float CENTRE_MS = 190F;
    private static final float PAGES_MS = 260F;

    /** How far above its place the centre panel starts. Whole pixels, like everything else. */
    private static final int CENTRE_DROP = 10;

    /** Where each page sits while still hidden behind the centre panel. */
    private static final int LEFT_CLOSED_X = CENTRE_X;
    private static final int RIGHT_CLOSED_X = CENTRE_X + CENTRE_W - PAGE_W;

    // ---- centre panel slots ----------------------------------------------------------------------
    private static final int DISPLAY_X = 22, DISPLAY_Y = 56, DISPLAY_W = 46, DISPLAY_H = 74;
    private static final int DISPLAY_INSET = 3;
    private static final int VIEW_W = 40, VIEW_H = 65;

    private static final int ROWS_X = 74, ROWS_Y = 59, ROWS_W = 40, ROWS_H = 65;

    /**
     * The teal plate's two number fields.
     *
     * <p>The plate's own glyphs are magenta, so they could be found the same way the slots were:
     * the chevrons and letters of the left label occupy x 26-53 and the right label 75-102, which
     * leaves the clear teal either side of them for the values.
     */
    private static final int HEADER_TEXT_Y = 40;
    private static final int LVL_VALUE_X = 63;
    private static final int PNT_VALUE_X = 111;

    private static final int TOGGLE_W = 19, TOGGLE_H = 19;
    private static final int TOGGLE_X = (CENTRE_W - TOGGLE_W) / 2, TOGGLE_Y = 134;

    /**
     * The Trust bar, laid into the ornament across the top of the centre panel.
     *
     * <p>Filled across the whole sprite rather than across the part of it that is not transparent.
     * Stopping at the last opaque column looked like the right idea and was not: the bar has a
     * point on its right end, and a bar that reaches full and still has its tip missing reads as
     * cut off rather than as finished.
     */
    private static final int TRUST_ART_W = 97, TRUST_ART_H = 9;
    private static final int TRUST_X = 23, TRUST_Y = 19;

    /** One per stat row, in the gap between the plate and the panel's right edge. */
    private static final int STAT_BUTTON_SIZE = 13;
    private static final int STAT_BUTTON_X = 116;

    /** The two gems in the centre panel's top corners, which fill with Trust. */
    private static final int GEM_SIZE = 9, GEM_Y = 4;
    private static final int GEM_LEFT_X = 6, GEM_RIGHT_X = 124;

    // ---- left page slots -------------------------------------------------------------------------
    private static final int FACE_X = 17, FACE_Y = 13, FACE_SIZE = 40;
    private static final int FACE_INSET = 4, FACE_VIEW = 32;

    private static final int NAME_X = FACE_X + FACE_SIZE + 3;
    private static final int NAME_W = 42;

    private static final int BOX_X = 20, BOX_Y = 57, BOX_W = 80, BOX_H = 48;

    /** The teal plates, measured to their interiors rather than their frames. */
    private static final int BAR_X = 20, BAR_W = 82, BAR_H = 12;
    private static final int BAR_TEXT_W = 78;
    private static final int SPEC_CB_Y = 112;
    private static final int TRAIT_CB_Y = 131;

    /**
     * The skill page, laid into the right page.
     *
     * <p>Measured off the frame rather than chosen: its main opening is 81 by 114 at (3, 13), which
     * is {@code skills_background} to the pixel, and its round opening is 18 square at (34, 3),
     * which is a category disc to the pixel. So the art tells you where everything goes and none of
     * these numbers is a judgement call.
     */
    private static final int SKILL_FRAME_X = 4, SKILL_FRAME_Y = 12;
    private static final int SKILL_FRAME_W = 87, SKILL_FRAME_H = 130;

    private static final int SKILL_BG_X = SKILL_FRAME_X + 3, SKILL_BG_Y = SKILL_FRAME_Y + 13;
    private static final int SKILL_BG_W = 81, SKILL_BG_H = 114;

    private static final int PRESET_X = SKILL_FRAME_X + 34, PRESET_Y = SKILL_FRAME_Y + 3;
    private static final int PRESET_SIZE = 18;

    private static final int ARROW_W = 8, ARROW_H = 12;

    /**
     * The grid of moves.
     *
     * <p>Drawn at the size the art is, like every other texture in this menu. Shrinking them to
     * fourteen was a mistake of exactly the kind this interface has already been through once: a
     * twenty-one pixel icon at two thirds is a different resolution from the panel behind it, and
     * two resolutions on one screen is the thing that reads as broken however carefully each is
     * placed. If they need to be smaller, the art needs to be smaller.
     */
    private static final int ICON = 21;
    private static final int ICON_GAP = 4;
    private static final int GRID_COLS = 3, GRID_ROWS = 4;

    /**
     * The scrolling area, which is what {@link #GRID_ROWS} now means: rows visible at once, not
     * rows that exist. The grid used to stop dead at twelve moves and silently drop the rest, which
     * was survivable while there were eight and stops being so the moment a category fills up.
     *
     * <p>The viewport is exactly four rows tall - no half row peeking - so at rest every icon in it
     * is whole. What is off the end is reached by the wheel, and the bar on the right says so.
     */
    private static final int ROW_PITCH = ICON + ICON_GAP;
    private static final int GRID_VIEW_H = GRID_ROWS * ICON + (GRID_ROWS - 1) * ICON_GAP;

    /** One notch of the wheel moves one row, so the grid never comes to rest misaligned. */
    private static final int SCROLL_STEP = ROW_PITCH;

    /**
     * The coloured rule that opens each group in the ability list.
     *
     * <p>Two pixels of the branch's own colour across the full width of the grid, with a little air
     * either side. The list used to be one undifferentiated run of icons in registration order, so
     * finding a particular move meant reading every icon until you hit it. Grouped and ruled, the
     * colour tells you which third of the list to look in before you have looked at anything.
     */
    /**
     * The separator art is 77 by 3 and pure white, which is the two things that matter: it spans the
     * 81-wide panel with a pixel either side, and being white it takes a tint cleanly - so the same
     * texture serves every branch and the colour still says which group is which.
     */
    private static final int RULE_W = 77, RULE_H = 3;
    private static final int RULE_X = SKILL_BG_X + 1;
    private static final int RULE_ABOVE = 3;
    private static final int RULE_BELOW = 3;

    /** For a move no tree mentions - Hamon and the rest, until they grow trees of their own. */
    private static final int COLOR_RULE_NONE = 0xFF6A6A78;

    /**
     * The scrollbar art: three wide, sixteen tall, with a cap at each end.
     *
     * <p>Which is why it is drawn in three pieces rather than stretched. The caps are three rows
     * each and the ten in between are identical, so the middle repeats to whatever length the thumb
     * needs and the ends stay the shape they were drawn. Stretching the whole sixteen would squash
     * the caps at a short thumb and smear them at a long one.
     */
    private static final int SCROLLBAR_W = 3;
    private static final int SCROLLBAR_TEX_H = 16;
    private static final int SCROLLBAR_CAP = 3;
    private static final int SCROLLBAR_MIDDLE = SCROLLBAR_TEX_H - 2 * SCROLLBAR_CAP;
    private static final int SCROLLBAR_MIN_THUMB = 2 * SCROLLBAR_CAP + 2;
    private static final int SCROLLBAR_X = SKILL_BG_X + SKILL_BG_W - SCROLLBAR_W;
    private static final int COLOR_SCROLL_TRACK = 0x50101018;

    /**
     * The menu's last solid row. The panel art is 161 tall but its bottom row is empty, so anything
     * meant to be hidden behind the menu has to be hidden behind 160, not 161 - a one pixel error
     * here would leave a line of the tucked button showing through the gap.
     */
    private static final int MENU_BOTTOM = CENTRE_Y + 160;

    /**
     * The two page tabs, side by side and centred on the whole menu.
     *
     * <p>Grouped at the left end of the centre panel. The inactive one is drawn <em>before</em> the
     * panel and sits fifteen pixels higher, so the panel covers all but the bottom three rows of it.
     * The panel is solid everywhere behind them - checked, because it is transparent in seven
     * thousand other places and a tab tucked behind a hole would simply hang there in mid air.
     */
    private static final int PAGE_BTN_W = 20, PAGE_BTN_H = 18;

    /**
     * How much of a hidden tab hangs below the panel, in pixels.
     *
     * <p>Was three, which is a hint rather than a handle - enough to see that something is there
     * once you know to look, and not enough to read as a thing you can press. Eight of eighteen
     * shows the shape of the tab, which is what makes it obviously the other page rather than a
     * seam in the art.
     *
     * <p>The click target follows it without being told: {@code tabHitHeight} is this number, and
     * the flag's is measured from wherever the flag currently hangs. Widening the affordance and
     * widening what answers the mouse are the same edit, which is the point of their sharing it.
     */
    private static final int PAGE_BTN_PEEK = 8;
    private static final int PAGE_BTN_GAP = 2;
    private static final int PAGE_BTN_ACTIVE_Y = MENU_BOTTOM;
    private static final int PAGE_BTN_TUCKED_Y = MENU_BOTTOM - (PAGE_BTN_H - PAGE_BTN_PEEK);
    /**
     * The skill tree, which takes over the middle panel rather than sitting beside it.
     *
     * <p>The frame is 119 by 140 against the panel's 139 by 161, so it centres inside it with ten
     * pixels of panel showing all round. Its window - the part that is actually cut out of the art,
     * measured rather than guessed - is 109 by 124 at an offset of five and eleven.
     *
     * <p>The background is a 16 square tile, so it repeats across the window instead of stretching.
     */
    private static final int TREE_W = 119, TREE_H = 140;
    private static final int TREE_X = CENTRE_X + (CENTRE_W - TREE_W) / 2;
    private static final int TREE_Y = CENTRE_Y + (CENTRE_H - TREE_H) / 2;
    private static final int TREE_VIEW_X = TREE_X + 5, TREE_VIEW_Y = TREE_Y + 11;
    private static final int TREE_VIEW_W = 109, TREE_VIEW_H = 124;
    private static final int TREE_TILE = 16;

    private static final int STRIP_MARGIN = 8;
    private static final int HOME_BTN_X = CENTRE_X + STRIP_MARGIN;
    private static final int SKILLS_BTN_X = HOME_BTN_X + PAGE_BTN_W + PAGE_BTN_GAP;

    /**
     * The flag that pulls the combat bar out, at the other end of the panel from the tabs.
     *
     * <p>It hides the same way the tabs do, but for a different reason: the tabs raise the one you
     * are not on, while the flag is raised whenever the bar is shut and lowers as it opens. So its
     * position is not a state but an animation - it rides the bar's own eased progress, which makes
     * it read as the thing pulling the bar down rather than a switch that happens to sit nearby.
     *
     * <p>Three pixels of it show while raised, like the tabs. On this sprite those three rows are
     * the point of the pennant, which is the part worth leaving out anyway.
     */
    private static final int FLAG_W = 13, FLAG_H = 18;

    /** How much brighter the flag draws under the cursor. Over one, so it lifts rather than tints. */
    private static final float FLAG_HOVER_LIFT = 1.35F;
    private static final int FLAG_X = CENTRE_X + CENTRE_W - FLAG_W - STRIP_MARGIN;
    private static final int FLAG_RAISED_Y = MENU_BOTTOM - (FLAG_H - PAGE_BTN_PEEK);
    private static final int FLAG_LOWERED_Y = MENU_BOTTOM;

    /**
     * The combat bar, and the slots along it.
     *
     * <p>The ribbon is 217 wide with pointed ends; only the middle 187 is flat, so the slots are
     * laid out in that and the points are left as points. Slot size and pitch are the HUD bar's own
     * (see CombatBarOverlay) - this is a picture of that bar, and a picture that disagrees with the
     * thing it depicts is worse than no picture.
     */
    private static final int CB_W = 217, CB_H = 31;
    private static final int CB_X = (TOTAL_W - CB_W) / 2;
    private static final int CB_Y = MENU_BOTTOM + PAGE_BTN_H + 2;
    private static final int CB_FLAT_X = 15, CB_FLAT_W = 187;
    private static final int CB_SLOTS = 8;

    /**
     * The wells, read off the texture rather than worked out.
     *
     * <p>They were being centred arithmetically - eight 21px squares at a pitch of 22, spread across
     * the ribbon's flat middle - on the assumption that the art was a plain band. It is not: there
     * are eight wells drawn into it, and the arithmetic put the slots near them rather than in them.
     *
     * <p>Measured, each well is a 15px dark interior with a two pixel bevel on every side, so the
     * well is 19 square - exactly the icon art - at x = 24 + i * 21 and y = 6. The bevels are what
     * makes the pitch 21 rather than 22, which is the error that accumulated across the row.
     */
    private static final int CB_SLOT_PITCH = 21;
    private static final int CB_SLOT_SIZE = SkillIcons.SIZE;
    private static final int CB_SLOT_X = CB_X + 24;
    private static final int CB_SLOT_Y = CB_Y + 6;

    /**
     * The mode button, at the left tip of the ribbon.
     *
     * <p>Sixteen square against a bar that is thirty-one tall, so it sits centred on the pointed end
     * with a couple of pixels either side. That end used to turn the page; the page arrows have moved
     * to the right tip alone, which now steps forward and wraps - two pages do not need two arrows.
     */
    private static final int MODE_SIZE = 16;
    private static final int MODE_X = CB_X - MODE_SIZE - 3;
    private static final int MODE_Y = CB_Y + (CB_H - MODE_SIZE) / 2;

    /**
     * A page marker either side of the slot row, one per page.
     *
     * <p>Four by nineteen, the same as the wells the HUD bar puts its own page markers in, and the
     * same height as a slot - so they read as part of the row rather than as furniture bolted to it.
     *
     * <p>They replace an arrow and a pair of pips. Two pages want two places to click, not one
     * control that cycles and a second that reports where it got to: with a marker at each end you
     * point at the page you want and you are on it.
     */
    private static final int PAGE_BAR_W = 4, PAGE_BAR_H = 19;
    private static final int PAGE_BAR_LEFT_X = CB_X + 17;

    /**
     * Three pixels further in than its mirror, because the ribbon is not symmetrical about the slot
     * row - the right point starts a little sooner, and matching the left inset exactly left this
     * one sitting on the taper rather than beside the slots.
     */
    private static final int PAGE_BAR_RIGHT_INSET = 3;
    private static final int PAGE_BAR_RIGHT_X =
            CB_X + CB_W - 17 - PAGE_BAR_W - PAGE_BAR_RIGHT_INSET;
    private static final int PAGE_BAR_Y = CB_Y + (CB_H - PAGE_BAR_H) / 2;

    private static final int COLOR_SLOT_EMPTY = 0x60101018;
    private static final int COLOR_SLOT_PICKED = 0x808FD8FF;

    /**
     * The composition the scale is derived from: the menu and the tabs under it, but not the bar.
     *
     * <p>The bar is left out deliberately. Including it would shrink the menu by a fifth at all
     * times to make room for something usually closed - so instead the whole menu lifts when the
     * bar opens, by exactly as much as the bar overhangs the screen and no more. On a tall enough
     * window that is nothing at all and the bar simply appears.
     *
     * <p>The tabs <em>are</em> included, because they are always there and cost nothing: at 1080p
     * the scale works out the same either way.
     */
    private static final int TOTAL_H = MENU_BOTTOM + PAGE_BTN_H;

    /** The same, counting the bar - what has to fit on screen once the flag is pulled. */
    private static final int TOTAL_H_WITH_BAR = CB_Y + CB_H;

    /** How long the lift takes, and how close to the screen edge it is willing to sit. */
    private static final float BAR_MS = 180F;
    private static final int SCREEN_EDGE = 4;
    private static final int GRID_X = SKILL_BG_X + (SKILL_BG_W - GRID_COLS * ICON
            - (GRID_COLS - 1) * ICON_GAP) / 2;

    /**
     * How far down the grid starts, which is not centred and should not be.
     *
     * <p>The category disc hangs over the top eight pixels of the background - it sits in the
     * frame's round opening at y=15 and the background begins at y=25 - so a grid centred in the
     * space would clear it by a single pixel and read as jammed against it. Fourteen leaves six
     * clear above the first row and four spare below the last.
     */
    private static final int GRID_TOP = 14;
    private static final int GRID_Y = SKILL_BG_Y + GRID_TOP;

    /** What a move nobody has yet is drawn through. */
    private static final int COLOR_LOCKED = 0xA0202028;

    /** And what marks one that is already on the bar. */

    /**
     * The step control: an arrow either side of a number, deciding how much one plus is worth.
     *
     * <p>Directly under the column of pluses it governs, on the right of the panel. It began in the
     * bottom left corner, which was clear ground but told you nothing - a control sitting under the
     * buttons it changes needs no label to explain what it is for.
     *
     * <p>Its right edge lands on 129, the same as the pluses above it, so the two read as one column.
     */
    private static final int STEP_X = 87, STEP_Y = 127;
    private static final int PLATE_W = 22, PLATE_H = 14;
    private static final int STEP_GAP = 2;

    /**
     * What one press can be worth.
     *
     * <p>A short list rather than a number that counts up one at a time, because the reason to have
     * this at all is not wanting to click forty times - and a control you have to click forty times
     * to set has simply moved the problem.
     */
    private static final int[] STEPS = {1, 5, 10, 25};

    /** Trust runs Dormant(0) to Bonded(3). */
    private static final int MAX_TRUST_TIER = 3;

    private static final int COLOR_TEXT = 0xFFFFFF;
    private static final int COLOR_ACCENT = 0xFFD700;
    private static final int COLOR_DIM = 0xFFA0A0A0;
    private static final int COLOR_SLOT = 0xFF000000;
    private static final int COLOR_GEM_UNLIT = 0xC8101014;
    private static final int COLOR_HOVER = 0xFF7FE3FF;
    private static final int COLOR_BACKDROP = 0xC0101010;

    /**
     * What a Stand's own passives are written in, as against the two its archetype hands out.
     *
     * <p>Four names in a list, two of which every brawler alive also has and two of which are the
     * reason this Stand is not one of the others. Colour is the cheapest way to say which is which,
     * and it does it without a heading, an indent or a second column - none of which the box has
     * room for.
     */
    private static final int COLOR_STAND_PASSIVE = 0xFF8FD8FF;

    /** What a figure inside a description is written in - a distance, a multiplier, a share. */
    private static final int COLOR_FIGURE = 0xFFFFC24D;

    /**
     * How much smaller the body text wants to be than the panels around it.
     *
     * <p>A wish rather than a value. What is actually used is the nearest size below it that still
     * lands the glyphs on exact pixels - see {@link #smallTextScale}.
     */
    private static final float TEXT_TARGET = 0.75F;

    private static final float FACE_SCALE = 27F;
    private static final float FACE_ANCHOR = 1.7F;
    private static final float DISPLAY_SCALE = 26F;
    /**
     * Where the display box is centred on the Stand.
     *
     * <p>Higher puts the box further up the model, which puts the model further down the box. It
     * was sitting centred and reading as floating; a Stand should have its feet nearer the bottom of
     * the frame than its head is to the top.
     */
    private static final float DISPLAY_ANCHOR = 1.32F;

    /** How large the player is drawn in the display. */
    private static final float PLAYER_SCALE = 26F;

    private boolean showingStand;
    private SkillBook.Category category = SkillBook.Category.STAND;
    private int stepIndex;

    /** How far the move grid is scrolled, in pixels from the top of the first row. */
    private int skillScroll;

    /**
     * Where every icon and every rule sits, worked out once a frame and read by everything else.
     *
     * <p>Grouping makes a position something you have to compute rather than something you can
     * derive from an index, and the drawing, the hit testing and the tooltip all need the same
     * answer. Computing it three times is how they end up disagreeing, so it is computed once and
     * they all read this.
     *
     * <p>Each slot is {@code {entryIndex, x, y}} and each rule {@code {y, colour}}, in menu
     * coordinates before scrolling. Reused between frames rather than reallocated.
     */
    private final List<int[]> gridSlots = new java.util.ArrayList<>();
    private final List<int[]> gridRules = new java.util.ArrayList<>();
    private int gridContentHeight;

    /** Where the skill tree is looking. Kept across page switches, reset when the menu opens. */
    private final SkillTreeView treeView = new SkillTreeView();

    /**
     * When the tab last changed, so the new page can arrive rather than appear.
     *
     * <p>The same slide the panel makes when the menu opens, reused: whatever the tab put in the
     * middle drops the last few pixels into place over a tenth of a second. Swapping the contents of
     * a panel between two frames reads as a glitch however correct both frames are.
     */
    private long pageChangedAt;

    private static final float PAGE_MS = 130F;
    private static final int PAGE_SLIDE = 6;

    /** Which of the two tabs is showing. */
    private enum MenuPage { HOME, SKILLS }

    private MenuPage page = MenuPage.HOME;

    /** Whether the combat bar is pulled out under the menu. */
    private boolean combatBarOpen;

    /**
     * Where the lift was when the flag was last clicked, and when that was.
     *
     * <p>Kept as a start value and a timestamp rather than a number stepped each frame, which is
     * what the rest of this screen does and for the same reason - but this one can be interrupted
     * halfway, so it has to remember where it was rather than assume it starts from rest.
     */
    private long barChangedAt = Long.MIN_VALUE;
    private float barProgressAtChange;

    /** How far the menu is currently lifted for the bar, in screen pixels. */
    private int barLift;

    /** Which half of the sixteen slots the bar is showing - the HUD bar has two pages too. */
    private int barPage;

    /**
     * Whether the bar being edited is the Utility stance's rather than the ordinary one.
     *
     * <p>A different bar, not a different page: the stance replaces the whole thing, so the two are
     * edited separately and both have their own two pages.
     */
    private boolean barUtility;

    /**
     * The move picked up from the grid, waiting for a slot to be put in.
     *
     * <p>Only meaningful while the bar is open. With it shut, clicking a move still drops it in the
     * first free slot as it always did - the two-step is for when you are looking at the slots and
     * can see which one you mean.
     */
    private ResourceLocation picked;
    private long openedAt;

    /**
     * Whole numbers only, and this is the entire reason the interface looks like one thing.
     *
     * <p>Scaling pixel art by 1.8 puts source pixels on fractions of a screen pixel, so some come
     * out a pixel wide and their neighbours two - and the font, scaled by the same 1.8, lands on a
     * different set of fractions again. The result reads as several resolutions sharing a screen.
     * At a whole multiple every pixel of every texture and every glyph is exactly the same size.
     */
    private int scale = 1;

    /**
     * What the body text is multiplied by inside the menu's own scale.
     *
     * <p>Always a fraction whose product with {@link #scale} is a whole number, which is the whole
     * of why this is not simply {@link #TEXT_TARGET}. Text at 0.8 of a 3x menu is drawn at 2.4x,
     * and a glyph two and a bit pixels wide is the mixed-resolution look this interface already
     * had once. Two thirds of 3x is 2x, which is smaller and still exact.
     */
    private float textScale = 1F;

    /**
     * Where the menu's top left corner sits, and whole pixels for a reason.
     *
     * <p>These were floats, and centring an odd number inside an even one gives a half. The art is
     * 341 by 161 - both odd - so at any odd scale the whole menu was translated by half a pixel on
     * both axes, and every texture in it sampled off the pixel grid. It reads as a faint,
     * unplaceable misalignment: nothing is in the wrong place relative to anything else, so it does
     * not look broken, it just looks slightly soft and slightly off.
     *
     * <p>Rounding the origin is the fix, and it has to be the origin rather than each blit. One
     * shared offset keeps every element on the same grid; rounding individually would let two
     * neighbouring pieces round opposite ways and open a seam between them.
     */
    private int offsetX;
    private int offsetY;

    /** Mouse position in menu space, refreshed each frame so hit tests can use panel coordinates. */
    private double localMouseX;
    private double localMouseY;

    /** What the cursor is over, collected during the scaled draw and shown once the pose is off. */
    private List<Component> tooltip;

    public PlayerMenuScreen() {
        super(Component.literal("JoJo: Heaven's Arrow"));
    }

    @Override
    protected void init() {
        openedAt = System.currentTimeMillis();

        // Nearest-neighbour, no mipmaps, said explicitly rather than relying on a default. Pixel art
        // scaled with a linear filter turns to mush, and this is now always scaled.
        Minecraft minecraft = Minecraft.getInstance();
        for (ResourceLocation texture : List.of(PAGE_LEFT, PAGE_RIGHT, CENTRE, DISPLAY_FRAME,
                STAND_FRAME, PLAYER_ROWS, STAND_ROWS, BUTTON_PLAYER, BUTTON_STAND)) {
            minecraft.getTextureManager().getTexture(texture).setFilter(false, false);
        }
    }

    /** Works out where the menu sits. Needed by the backdrop, which is drawn before the menu is. */
    private void layout() {
        // Floored, never rounded: rounding up would overflow the ceiling it was measured against.
        scale = Math.max(1, (int) Math.min(this.width * FILL / TOTAL_W,
                this.height * FILL / TOTAL_H));

        // The menu is sized without the bar, and lifts to make room when the bar opens - but a lift
        // can only go as far as the top of the screen, and above a certain scale even that is not
        // enough, because the two together are simply taller than the window. One step down solves
        // it wherever it happens (in practice only around 1440p, where the height allows a scale of
        // seven and 211 times seven does not fit).
        //
        // Derived from the window alone, never from whether the bar is open, so it is stable: the
        // menu is one size for a given window and does not resize when the flag is clicked. That is
        // the whole reason to lift rather than to reserve.
        while (scale > 1 && TOTAL_H_WITH_BAR * scale > this.height - 2 * SCREEN_EDGE) {
            scale--;
        }

        textScale = smallTextScale(scale);
        // Integer division, so the origin is always a whole pixel. Half a pixel of slack goes to
        // the right and bottom margins, where nobody can see it.
        offsetX = (this.width - TOTAL_W * scale) / 2;
        offsetY = (this.height - TOTAL_H * scale) / 2;
    }

    /**
     * Dims the world behind the menu. The whole screen, because the menu is not a rectangle.
     *
     * <p>This was briefly four bands laid around the menu's bounding box, on the theory that a wash
     * which never touches the panels cannot tint them. The panels are not a solid rectangle: seven
     * thousand pixels of the art are fully transparent - the rounded corners, the margins between
     * the pages - and every one of them showed bright undimmed world through a hole in the dark.
     */
    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, this.width, this.height, COLOR_BACKDROP);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // First, not last, and this is the whole of why the interface looked washed out.
        //
        // Screen.render begins by calling renderBackground. Calling it at the end of an override -
        // which is the habitual place for it, and where this had it - therefore paints the backdrop
        // over everything the override just drew. The menu was being dimmed by its own background
        // once per frame, and no amount of arranging the wash underneath could have helped, because
        // it was never underneath.
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // The shader colour is one global for the frame and this is drawn after the HUD, so
        // whatever last touched it is what the panels would be multiplied by. Asserting it costs
        // nothing and means a leak anywhere else cannot tint this screen.
        guiGraphics.setColor(1F, 1F, 1F, 1F);

        layout();

        barLift = Math.round(ease(barProgress()) * neededLift());

        localMouseX = (mouseX - offsetX) / (double) scale;
        localMouseY = (mouseY - offsetY + barLift) / (double) scale;
        tooltip = null;

        float elapsed = System.currentTimeMillis() - openedAt;
        float centreOpen = Mth.clamp(elapsed / CENTRE_MS, 0F, 1F);
        float pagesOpen = Mth.clamp((elapsed - CENTRE_MS) / PAGES_MS, 0F, 1F);

        JojohaPlayerData data = ClientPlayerDataCache.data;
        if (!data.stand.isPresent()) {
            // So the next Stand obtained materialises rather than snapping in.
            StandPortrait.forget();
        }

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(offsetX, offsetY - barLift, 0F);
        guiGraphics.pose().scale(scale, scale, 1F);

        // Eased, and the pages ride out from behind the centre rather than appearing beside it.
        float slide = ease(pagesOpen);
        int leftX = Math.round(Mth.lerp(slide, LEFT_CLOSED_X, LEFT_X));
        int rightX = Math.round(Mth.lerp(slide, RIGHT_CLOSED_X, RIGHT_X));

        if (pagesOpen > 0F) {
            // The black behind the cut-outs rides along with the page and fades with it. It used to
            // be painted by renderLeftPage, which only runs once the slide has finished - so for the
            // whole of the opening the page was a frame with holes in it, and the world showed
            // through the very boxes that are meant to read as solid. The gap was the animation.
            fillSlot(guiGraphics, leftX + FACE_X, LEFT_Y + FACE_Y, FACE_SIZE, FACE_SIZE, pagesOpen);
            fillSlot(guiGraphics, leftX + BOX_X, LEFT_Y + BOX_Y, BOX_W, BOX_H, pagesOpen);

            guiGraphics.setColor(1F, 1F, 1F, pagesOpen);
            blit(guiGraphics, PAGE_LEFT, leftX, LEFT_Y, PAGE_W, PAGE_H);
            blit(guiGraphics, PAGE_RIGHT, rightX, RIGHT_Y, PAGE_W, PAGE_H);
            guiGraphics.setColor(1F, 1F, 1F, 1F);
        }

        // Rounded, so the panel never lands between pixels on the way down.
        int drop = Math.round((1F - ease(centreOpen)) * CENTRE_DROP);

        // Alpha passed rather than set: fill draws its own colour and never consults setColor, so
        // these two were snapping in at full black on the first frame while the panel around them
        // was still fading up.
        fillSlot(guiGraphics, CENTRE_X + DISPLAY_X, CENTRE_Y + DISPLAY_Y - drop, DISPLAY_W, DISPLAY_H,
                centreOpen);
        fillSlot(guiGraphics, CENTRE_X + ROWS_X, CENTRE_Y + ROWS_Y - drop, ROWS_W, ROWS_H, centreOpen);

        guiGraphics.setColor(1F, 1F, 1F, centreOpen);

        // Before the panel, because the panel is what hides everything raised behind it: the tab you
        // are not on, and the flag while the bar is shut. All of it rides the same drop as the panel,
        // or it would sit still while the panel slid down over it.
        drawPageTabs(guiGraphics, drop);
        drawFlag(guiGraphics, drop, centreOpen);

        blit(guiGraphics, CENTRE, CENTRE_X, CENTRE_Y - drop, CENTRE_W, CENTRE_H);
        guiGraphics.setColor(1F, 1F, 1F, 1F);

        float bar = barProgress();
        if (bar > 0F && centreOpen >= 1F) {
            guiGraphics.setColor(1F, 1F, 1F, bar);
            drawCombatBar(guiGraphics, data);
            guiGraphics.setColor(1F, 1F, 1F, 1F);
        }

        if (centreOpen >= 1F) {
            // Offset through the pose rather than passed down, so the slide costs nothing at every
            // call site that draws part of a page. It is only ever a few pixels for a tenth of a
            // second, which is short enough that hit testing against the settled position is fine.
            int pageSlide = Math.round((1F - ease(pageProgress())) * PAGE_SLIDE);

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0F, pageSlide, 0F);

            // On the skills page the panel is a window onto the tree and nothing else: no portrait,
            // no stat rows, no trust gems. They are not hidden behind it - they are simply not what
            // this page is.
            if (page == MenuPage.SKILLS) {
                renderSkillTree(guiGraphics, data);
            } else {
                renderCentre(guiGraphics, data, mouseX, mouseY);
                renderGems(guiGraphics, data);
            }

            guiGraphics.pose().popPose();
        }

        if (pagesOpen >= 1F) {
            renderLeftPage(guiGraphics, data);

            // Drawn on both tabs. It is the list of what you have, and the tree beside it is where
            // you get it from - so unlocking a node and watching the move appear in the grid happens
            // on one screen rather than needing a tab switch to confirm it worked.
            renderSkillPage(guiGraphics, data);
        }

        guiGraphics.pose().popPose();

        // Outside the pose on purpose: a tooltip positions itself against the real cursor and the
        // real screen edges, neither of which know anything about the menu's scale.
        if (tooltip != null) {
            drawTooltip(guiGraphics, tooltip, mouseX, mouseY);
        }
    }

    /**
     * The largest step below full size that still multiplies out to whole pixels.
     *
     * <p>Floored, not rounded, or the "smaller" size comes back as the same size. At a 1x menu
     * there is no smaller whole number to land on, so the text simply stays as it is - the
     * alternative is a blurred half-size font, which is worse than text that is a little large.
     */
    private static float smallTextScale(int menuScale) {
        int steps = (int) (menuScale * TEXT_TARGET);
        if (steps < 1) {
            // A 1x menu has no smaller whole number to land on. Held at full size this text simply
            // never got smaller however often it was asked to, so the softness is taken instead -
            // it is a quarter off, which the font survives, and being readable in the box it is in
            // matters more than being exact.
            return TEXT_TARGET;
        }
        return steps / (float) menuScale;
    }

    /** Quick out, slow in - the pages arrive rather than merely translating. */
    private static float ease(float t) {
        float u = 1F - t;
        return 1F - u * u * u;
    }

    // ---- left page ---------------------------------------------------------------------------------

    private void renderLeftPage(GuiGraphics guiGraphics, JojohaPlayerData data) {
        int px = LEFT_X;
        int py = LEFT_Y;

        blit(guiGraphics, STAND_FRAME, px + FACE_X, py + FACE_Y, FACE_SIZE, FACE_SIZE);

        int faceX = px + FACE_X + FACE_INSET;
        int faceY = py + FACE_Y + FACE_INSET;
        if (data.stand.isPresent()) {
            clipped(guiGraphics, faceX, faceY, FACE_VIEW, FACE_VIEW,
                    () -> StandPortrait.render(guiGraphics, data, faceX, faceY,
                            FACE_VIEW, FACE_VIEW, FACE_SCALE, FACE_ANCHOR, StandPortrait.ROTATION));
        }

        StandType type = standType(data);
        if (type == null) {
            smallLeft(guiGraphics, "NO STAND", px + NAME_X, py + FACE_Y + 15, NAME_W, COLOR_DIM);
            return;
        }

        // The Stand is not named in the page any more. The picture is the name - a player who has
        // to be told which Stand is in the box is being told something the box already says - so
        // the words are kept for the one case where they add anything, which is wanting to know
        // exactly which skin is on it. See the hover above.
        standTooltip(guiGraphics, data, type, faceX, faceY, FACE_VIEW, FACE_VIEW);

        // The kind, with what that kind is for held back until asked for - there is no room in the
        // page for a paragraph, and a paragraph nobody reads twice is better as a tooltip.
        //
        // Labelled on its own line rather than inline, because "Archetype: BRAWLER" does not fit the
        // forty-two pixels beside the face at any size worth reading.
        int step = lineStep();
        int labelY = py + FACE_Y + 11;
        int kindY = labelY + step + 1;

        smallLeft(guiGraphics, "Archetype:", px + NAME_X, labelY, NAME_W, COLOR_DIM);

        boolean overKind = within(px + NAME_X, kindY, NAME_W, step);
        smallLeft(guiGraphics, Component.translatable(type.archetype().translationKey()).getString(),
                px + NAME_X, kindY, NAME_W, overKind ? COLOR_HOVER : COLOR_TEXT);
        if (overKind) {
            tooltip = List.of(
                    Component.translatable(type.archetype().translationKey())
                            .withStyle(style -> style.withColor(COLOR_ACCENT)),
                    figures(Component.translatable(type.archetype().descriptionKey())));
        }

        renderPassives(guiGraphics, type, px, py);

        barLabel(guiGraphics, "SPEC", upper(data.spec.name()), px + BAR_X, py + SPEC_CB_Y);
        barLabel(guiGraphics, "TRAIT", data.trait == null ? "NONE" : upper(data.trait.getPath()),
                px + BAR_X, py + TRAIT_CB_Y);
    }

    /**
     * The four passives, one per line, each explaining itself on hover.
     *
     * <p>Names only. Four descriptions would not fit the box at any size worth reading, and the
     * names are what a player is scanning for once they know what they have.
     */
    private void renderPassives(GuiGraphics guiGraphics, StandType type, int px, int py) {
        int step = lineStep();
        int lineY = py + BOX_Y + 2;
        smallLeft(guiGraphics, "PASSIVES", px + BOX_X + 3, lineY, BOX_W - 6, COLOR_DIM);
        lineY += step + 1;

        for (ResourceLocation passiveId : type.allPassives()) {
            StandPassive passive = StandPassives.byId(passiveId);
            if (passive == null || lineY + step > py + BOX_Y + BOX_H) {
                continue;
            }

            // Asked of the Stand's own list rather than of the archetype's, because a Stand knows
            // what it brought and the archetype's two are simply everything else in the line-up.
            boolean own = type.passives().contains(passiveId);
            boolean over = within(px + BOX_X + 3, lineY, BOX_W - 6, step);

            smallLeft(guiGraphics, Component.translatable(passive.translationKey()).getString(),
                    px + BOX_X + 3, lineY, BOX_W - 6,
                    over ? COLOR_HOVER : (own ? COLOR_STAND_PASSIVE : COLOR_TEXT));

            if (over) {
                tooltip = List.of(
                        Component.translatable(passive.translationKey())
                                .withStyle(style -> style.withColor(
                                        own ? COLOR_STAND_PASSIVE : COLOR_ACCENT)),
                        figures(Component.translatable(passive.translationKey() + ".desc")));
            }

            lineY += step;
        }
    }

    /**
     * One of the two teal plates: a label and its value, drawn as two pieces so the value keeps its
     * own colour, and measured as one so the pair sits centred on the plate.
     *
     * <p>Only the value is ever trimmed. The label is four or five characters and is what tells you
     * which plate you are reading; losing it to make room for a long spec name would be trimming
     * away the useful half.
     */
    private void barLabel(GuiGraphics guiGraphics, String label, String value, int x, int y) {
        String prefix = label + ": ";
        int prefixWidth = this.font.width(prefix);

        // Widths are measured in the font's own units and spent in the panel's, so the room the
        // value has is the plate's width converted back through the text scale.
        int roomInGlyphs = Math.max(0, Math.round(BAR_TEXT_W / textScale) - prefixWidth);
        String shown = trim(value, roomInGlyphs);

        int total = Math.round((prefixWidth + this.font.width(shown)) * textScale);
        int textX = x + (BAR_W - total) / 2;
        int textY = y + (BAR_H - lineStep()) / 2;

        smallRaw(guiGraphics, prefix, textX, textY, COLOR_TEXT);
        smallRaw(guiGraphics, shown, textX + Math.round(prefixWidth * textScale), textY,
                COLOR_ACCENT);
    }

    // ---- centre ------------------------------------------------------------------------------------

    private void renderCentre(GuiGraphics guiGraphics, JojohaPlayerData data, int mouseX, int mouseY) {
        int px = CENTRE_X;
        int py = CENTRE_Y;

        blit(guiGraphics, DISPLAY_FRAME, px + DISPLAY_X, py + DISPLAY_Y, DISPLAY_W, DISPLAY_H);

        int viewX = px + DISPLAY_X + DISPLAY_INSET;
        int viewY = py + DISPLAY_Y + DISPLAY_INSET;

        if (showingStand) {
            if (data.stand.isPresent()) {
                clipped(guiGraphics, viewX, viewY, VIEW_W, VIEW_H,
                        () -> StandPortrait.render(guiGraphics, data, viewX, viewY, VIEW_W, VIEW_H,
                                DISPLAY_SCALE, DISPLAY_ANCHOR, StandPortrait.ROTATION,
                                (float) localMouseX, (float) localMouseY));

            } else {
                centred(guiGraphics, "NO STAND", viewX + VIEW_W / 2, viewY + VIEW_H / 2, COLOR_DIM);
            }
        } else {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                clipped(guiGraphics, viewX, viewY, VIEW_W, VIEW_H,
                        () -> renderPlayerFollowingMouse(guiGraphics, player, viewX, viewY));
            }
        }

        blit(guiGraphics, showingStand ? STAND_ROWS : PLAYER_ROWS,
                px + ROWS_X, py + ROWS_Y, ROWS_W, ROWS_H);
        drawRowValues(guiGraphics, data, px + ROWS_X, py + ROWS_Y);

        blit(guiGraphics, showingStand ? BUTTON_STAND : BUTTON_PLAYER,
                px + TOGGLE_X, py + TOGGLE_Y, TOGGLE_W, TOGGLE_H);

        drawStatButtons(guiGraphics, data, px, py);
        drawStepper(guiGraphics, px, py);
        drawTrustBar(guiGraphics, data, px, py);
        drawHeader(guiGraphics, data, px, py);
    }

    /**
     * The player, turning to watch the cursor.
     *
     * <p>Vanilla has exactly this, and it cannot be used: {@code renderEntityInInventoryFollowsMouse}
     * calls {@code enableScissor} itself with the rectangle it is handed, and a scissor is applied
     * in raw screen pixels with no knowledge of the pose. Given this menu's coordinates it clipped a
     * small rectangle near the corner of the screen and the player vanished entirely - which is
     * exactly what happened. So the angles are worked out here and the drawing handed to the plain
     * renderer, which does no clipping of its own and leaves it to the caller.
     */
    private void renderPlayerFollowingMouse(GuiGraphics guiGraphics, LocalPlayer player,
                                            int viewX, int viewY) {
        float centreX = viewX + VIEW_W / 2F;
        float centreY = viewY + VIEW_H / 2F;

        float yaw = (float) Math.atan((centreX - localMouseX) / 40F);
        float pitch = (float) Math.atan((centreY - localMouseY) / 40F);

        Quaternionf facing = new Quaternionf().rotateZ((float) Math.PI);
        Quaternionf tilt = new Quaternionf().rotateX(pitch * 20F * ((float) Math.PI / 180F));
        facing.mul(tilt);

        float bodyRot = player.yBodyRot;
        float yRot = player.getYRot();
        float xRot = player.getXRot();
        float headRotO = player.yHeadRotO;
        float headRot = player.yHeadRot;

        player.yBodyRot = 180F + yaw * 20F;
        player.setYRot(180F + yaw * 40F);
        player.setXRot(-pitch * 20F);
        player.yHeadRot = player.getYRot();
        player.yHeadRotO = player.getYRot();

        float size = player.getScale();
        InventoryScreen.renderEntityInInventory(guiGraphics, centreX, centreY, PLAYER_SCALE / size,
                new Vector3f(0F, player.getBbHeight() / 2F, 0F), facing, tilt, player);

        player.yBodyRot = bodyRot;
        player.setYRot(yRot);
        player.setXRot(xRot);
        player.yHeadRotO = headRotO;
        player.yHeadRot = headRot;
    }

    /**
     * The step control, and the number it holds.
     *
     * <p>Only while there is something to spend. A dial for how fast to spend nothing is furniture.
     */
    private void drawStepper(GuiGraphics guiGraphics, int px, int py) {
        if (ClientPlayerDataCache.data.pointsFor(showingStand) <= 0) {
            return;
        }

        int arrowY = py + STEP_Y + (PLATE_H - ARROW_H) / 2;
        int leftX = px + STEP_X;
        int plateX = leftX + ARROW_W + STEP_GAP;
        int rightX = plateX + PLATE_W + STEP_GAP;

        blit(guiGraphics, ARROW_LEFT, leftX, arrowY, ARROW_W, ARROW_H);
        blit(guiGraphics, NUMBER_PLATE, plateX, py + STEP_Y, PLATE_W, PLATE_H);
        blit(guiGraphics, ARROW_RIGHT, rightX, arrowY, ARROW_W, ARROW_H);

        String amount = String.valueOf(step());
        int textW = Math.round(this.font.width(amount) * textScale);
        smallRaw(guiGraphics, amount, plateX + (PLATE_W - textW) / 2,
                py + STEP_Y + (PLATE_H - lineStep()) / 2, COLOR_TEXT);

        if (within(STEP_X + CENTRE_X, STEP_Y + CENTRE_Y,
                ARROW_W + STEP_GAP + PLATE_W + STEP_GAP + ARROW_W, PLATE_H)) {
            tooltip = List.of(
                    Component.literal("Points per press")
                            .withStyle(style -> style.withColor(COLOR_ACCENT)),
                    Component.literal("Each + spends this many, or as many as remain")
                            .withStyle(style -> style.withColor(COLOR_DIM)));
        }
    }

    /** How far through the tab swap we are, one being settled. */
    private float pageProgress() {
        return pageChangedAt == 0L ? 1F
                : Mth.clamp((System.currentTimeMillis() - pageChangedAt) / PAGE_MS, 0F, 1F);
    }

    /** Which tree a skill-page category is looking at. */
    private static SkillTrees.Tree treeFor(SkillBook.Category category) {
        return switch (category) {
            case HAMON -> SkillTrees.Tree.HAMON;
            case PLAYER -> SkillTrees.Tree.PLAYER;
            case VAMPIRE -> SkillTrees.Tree.VAMPIRE;
            default -> SkillTrees.Tree.STAND;
        };
    }

    /** How many points one press of a plus is worth. */
    private int step() {
        return STEPS[Math.floorMod(stepIndex, STEPS.length)];
    }

    /**
     * A plus beside each stat, but only while there is anything to spend.
     *
     * <p>Player stats only. The Stand's five are not bought with these points - they come from its
     * own growth - so the column is absent on that page rather than present and refusing to work.
     */
    private void drawStatButtons(GuiGraphics guiGraphics, JojohaPlayerData data, int px, int py) {
        if (data.pointsFor(showingStand) <= 0 || (showingStand && !data.stand.isPresent())) {
            return;
        }

        int rowHeight = ROWS_H / StatPoints.COUNT;
        for (int i = 0; i < StatPoints.COUNT; i++) {
            // Nothing offered on a stat already at the ceiling. The server refuses the point
            // anyway, and a button that does nothing when pressed is worse than no button.
            if (statAt(data, i) >= StatPoints.MAX_STAT) {
                continue;
            }

            int x = px + STAT_BUTTON_X;
            int y = py + ROWS_Y + i * rowHeight;
            boolean over = within(x, y, STAT_BUTTON_SIZE, STAT_BUTTON_SIZE);
            blit(guiGraphics, over ? STAT_BUTTON_OVER : STAT_BUTTON, x, y,
                    STAT_BUTTON_SIZE, STAT_BUTTON_SIZE);
        }
    }

    /** Whichever five the page is showing, by row. */
    private int statAt(JojohaPlayerData data, int row) {
        return showingStand ? data.stand.stats()[row] : StatPoints.playerStat(data, row);
    }

    /**
     * Trust, filling the ornament from the left a third at a time.
     *
     * <p>Nothing at all at Dormant: an empty channel and a bar at zero look the same, and the art
     * has no empty state of its own to draw.
     */
    private void drawTrustBar(GuiGraphics guiGraphics, JojohaPlayerData data, int px, int py) {
        if (!data.stand.isPresent()) {
            return;
        }

        int tier = Mth.clamp(data.stand.trustTier(), 0, MAX_TRUST_TIER);
        if (tier <= 0) {
            return;
        }

        int shown = TRUST_ART_W * tier / MAX_TRUST_TIER;
        guiGraphics.blit(TRUST_BAR, px + TRUST_X, py + TRUST_Y, 0F, 0F, shown, TRUST_ART_H,
                TRUST_ART_W, TRUST_ART_H);
    }

    private void drawRowValues(GuiGraphics guiGraphics, JojohaPlayerData data, int rowsX, int rowsY) {
        if (showingStand && !data.stand.isPresent()) {
            return;
        }

        // The art's five labels, in the order it draws them. The player's fourth row reads INT on
        // the plate and is fed by endurance, which is the one place the art and the data disagree
        // about a name; the order is what matters and the order is right.
        int[] values = showingStand
                ? new int[]{data.stand.power(), data.stand.speed(), data.stand.endurance(),
                        data.stand.protection(), data.stand.potential()}
                : new int[]{data.strength, data.vitality, data.agility, data.endurance,
                        data.worthiness};

        int rowHeight = ROWS_H / values.length;
        for (int i = 0; i < values.length; i++) {
            String text = String.valueOf(values[i]);
            int textY = rowsY + i * rowHeight + (rowHeight - this.font.lineHeight) / 2 + 1;
            guiGraphics.drawString(this.font, text, rowsX + ROWS_W - 3 - this.font.width(text),
                    textY, COLOR_TEXT, false);
        }
    }

    private void drawHeader(GuiGraphics guiGraphics, JojohaPlayerData data, int px, int py) {
        // Shown on both pages, because the points are one pool. They were hidden on the Stand page
        // on the reasoning that they are the player's - which was true and unhelpful, since the
        // Stand page is where half of them get spent and a plate that empties there just looks like
        // the interface has lost track of them.

        // No levelling system exists yet, so a flat zero rather than an invented figure.
        centred(guiGraphics, "0", px + LVL_VALUE_X, py + HEADER_TEXT_Y, COLOR_TEXT);
        centred(guiGraphics, String.valueOf(data.pointsFor(showingStand)), px + PNT_VALUE_X,
                py + HEADER_TEXT_Y, COLOR_TEXT);
    }

    /**
     * The two gems, lit from the bottom by how far the Stand trusts its user.
     *
     * <p>Darkened rather than drawn: the gems are baked into the panel, so what fills them is the
     * removal of a cover over the part that has not been earned. At Bonded nothing is drawn at all
     * and the art is simply left alone.
     */
    private void renderGems(GuiGraphics guiGraphics, JojohaPlayerData data) {
        int tier = data.stand.isPresent()
                ? Mth.clamp(data.stand.trustTier(), 0, MAX_TRUST_TIER)
                : 0;
        int lit = GEM_SIZE * tier / MAX_TRUST_TIER;
        if (lit >= GEM_SIZE) {
            return;
        }

        int unlit = GEM_SIZE - lit;
        for (int gemX : new int[]{GEM_LEFT_X, GEM_RIGHT_X}) {
            int x = CENTRE_X + gemX;
            int y = CENTRE_Y + GEM_Y;
            guiGraphics.fill(x, y, x + GEM_SIZE, y + unlit, COLOR_GEM_UNLIT);
        }
    }

    // ---- helpers -----------------------------------------------------------------------------------

    /**
     * Runs a draw clipped to a box given in menu coordinates.
     *
     * <p>The conversion is the whole point. A scissor rectangle is applied in raw screen pixels and
     * knows nothing about the pose, so handing it the same numbers used to place the model would
     * clip somewhere else entirely - near the top left corner, at native size, while the menu is
     * drawn three times larger in the middle of the screen.
     */
    private void clipped(GuiGraphics guiGraphics, int x, int y, int w, int h, Runnable draw) {
        guiGraphics.enableScissor(screenX(x), screenY(y), screenX(x + w), screenY(y + h));
        draw.run();
        guiGraphics.disableScissor();
    }

    /**
     * How far the whole menu has to rise for the bar to clear the bottom of the screen.
     *
     * <p>Never more than would push the top of the menu off instead - on a window too short for
     * both, the bar is the part that gets cut, because the menu is the thing being used.
     */
    private int neededLift() {
        if (!combatBarOpen && barProgress() <= 0F) {
            return 0;
        }

        int overhang = offsetY + (CB_Y + CB_H) * scale - (this.height - SCREEN_EDGE);
        return Mth.clamp(overhang, 0, Math.max(0, offsetY - SCREEN_EDGE));
    }

    /** The lift's eased progress, which survives being reversed part way through. */
    private float barProgress() {
        float target = combatBarOpen ? 1F : 0F;
        if (barChangedAt == Long.MIN_VALUE) {
            return target;
        }

        float t = Mth.clamp((System.currentTimeMillis() - barChangedAt) / BAR_MS, 0F, 1F);
        return Mth.lerp(t, barProgressAtChange, target);
    }

    private int screenX(int local) {
        return offsetX + local * scale;
    }

    private int screenY(int local) {
        return offsetY - barLift + local * scale;
    }

    private boolean within(int x, int y, int w, int h) {
        return localMouseX >= x && localMouseX < x + w && localMouseY >= y && localMouseY < y + h;
    }

    private static StandType standType(JojohaPlayerData data) {
        return data.stand.isPresent() ? StandTypes.byIdOrDefault(data.stand.standId()) : null;
    }

    /**
     * A floor under a cut-out slot, so the backdrop wash cannot show through it.
     *
     * <p>Takes its own alpha because {@code fill} carries the value in its colour and pays no
     * attention to {@code setColor} - so a slot drawn during a fade has to be told about the fade
     * rather than inheriting it the way every blit beside it does.
     */
    private static void fillSlot(GuiGraphics guiGraphics, int x, int y, int w, int h, float alpha) {
        int a = Mth.clamp(Math.round(alpha * 255F), 0, 255);
        guiGraphics.fill(x, y, x + w, y + h, (a << 24) | (COLOR_SLOT & 0xFFFFFF));
    }

    private static void blit(GuiGraphics guiGraphics, ResourceLocation texture, int x, int y,
                             int w, int h) {
        guiGraphics.blit(texture, x, y, 0F, 0F, w, h, w, h);
    }

    private void centred(GuiGraphics guiGraphics, String text, int centreX, int y, int colour) {
        guiGraphics.drawCenteredString(this.font, text, centreX, y, colour);
    }

    /**
     * Anything that looks like a measurement, and what may follow it.
     *
     * <p>A run of digits, optionally with a decimal, optionally carrying a percent or a multiplier
     * sign, and optionally followed by the unit it is in. The unit is part of the match on purpose -
     * "2 blocks" reads as one fact and colouring only the 2 splits it in half.
     */
    private static final java.util.regex.Pattern FIGURE = java.util.regex.Pattern.compile(
            "\\d+(?:\\.\\d+)?\\s*(?:%|x)?(?:\\s*(?:blocks?|seconds?|hearts?|damage))?",
            java.util.regex.Pattern.CASE_INSENSITIVE);

    /**
     * Picks the numbers out of a description so they can be read at a glance.
     *
     * <p>Done here rather than by writing colour codes into the language file, for two reasons. A
     * translator should be writing a sentence, not remembering which escape makes a number gold. And
     * a rule applied in code covers every description that will ever exist, including the ones
     * nobody has written yet.
     */
    private Component figures(Component text) {
        String plain = text.getString();
        net.minecraft.network.chat.MutableComponent out = Component.empty();

        java.util.regex.Matcher matcher = FIGURE.matcher(plain);
        int cut = 0;
        while (matcher.find()) {
            if (matcher.start() > cut) {
                out.append(Component.literal(plain.substring(cut, matcher.start()))
                        .withStyle(style -> style.withColor(COLOR_DIM)));
            }
            out.append(Component.literal(matcher.group())
                    .withStyle(style -> style.withColor(COLOR_FIGURE)));
            cut = matcher.end();
        }

        if (cut < plain.length()) {
            out.append(Component.literal(plain.substring(cut))
                    .withStyle(style -> style.withColor(COLOR_DIM)));
        }
        return out;
    }

    /**
     * The right page: every move there is, grouped, with what you have lit and the rest dark.
     *
     * <p>The background goes down first, then the frame over it, because the frame is a window and
     * everything it frames has to already be there to be seen through it.
     */
    private void renderSkillPage(GuiGraphics guiGraphics, JojohaPlayerData data) {
        int px = RIGHT_X;
        int py = RIGHT_Y;

        blit(guiGraphics, SKILL_BACKGROUND, px + SKILL_BG_X, py + SKILL_BG_Y,
                SKILL_BG_W, SKILL_BG_H);
        blit(guiGraphics, SKILL_FRAME, px + SKILL_FRAME_X, py + SKILL_FRAME_Y,
                SKILL_FRAME_W, SKILL_FRAME_H);

        // The disc naming the group, in the round opening the frame leaves for it - and it is the
        // control as well as the label. A pair of arrows and a heading were three pieces of
        // furniture doing what the disc could do alone, on a page with no room to spare.
        blit(guiGraphics, category.preset(), px + PRESET_X, py + PRESET_Y,
                PRESET_SIZE, PRESET_SIZE);

        if (within(PRESET_X + RIGHT_X, PRESET_Y + RIGHT_Y, PRESET_SIZE, PRESET_SIZE)) {
            tooltip = List.of(
                    Component.translatable(category.translationKey())
                            .withStyle(style -> style.withColor(COLOR_ACCENT)),
                    figures(Component.translatable(category.descriptionKey())),
                    Component.literal("Click for " + Component.translatable(
                            category.next().translationKey()).getString())
                            .withStyle(style -> style.withColor(COLOR_DIM)));
        }

        List<SkillBook.Entry> entries = SkillBook.of(category, data);
        layoutGrid(entries);

        // Clamped here rather than only where it is changed, because the list can shrink underneath
        // a scroll position that was valid when it was set - a move becoming locked, or the Stand
        // changing, both do it.
        skillScroll = Mth.clamp(skillScroll, 0, maxScroll());
        int scroll = skillScroll;

        // Clipped to the viewport, so a row leaving the top is cut off at the panel edge instead of
        // being drawn across the frame around it. See clipped() for why the rectangle has to be
        // converted out of menu coordinates first.
        clipped(guiGraphics, px + SKILL_BG_X, py + GRID_Y, SKILL_BG_W, GRID_VIEW_H, () -> {
            for (int[] rule : gridRules) {
                int y = py + GRID_Y + rule[0] - scroll;
                if (y + RULE_H <= py + GRID_Y || y >= py + GRID_Y + GRID_VIEW_H) {
                    continue;
                }
                // Tinted rather than filled: the art carries the shape, the branch carries the
                // colour. Alpha survives the tint, so the breaks in the line stay breaks.
                int colour = rule[1];
                guiGraphics.setColor(((colour >> 16) & 0xFF) / 255F, ((colour >> 8) & 0xFF) / 255F,
                        (colour & 0xFF) / 255F, 1F);
                blit(guiGraphics, SEPARATOR, px + RULE_X, y, RULE_W, RULE_H);
                guiGraphics.setColor(1F, 1F, 1F, 1F);
            }

            for (int[] place : gridSlots) {
                int x = px + GRID_X + place[1];
                int y = py + GRID_Y + place[2] - scroll;

                // Rows entirely outside the window are not drawn at all. The scissor would hide
                // them anyway; skipping them keeps a long list from costing draw calls per move.
                if (y + ICON <= py + GRID_Y || y >= py + GRID_Y + GRID_VIEW_H) {
                    continue;
                }

                SkillBook.Entry entry = entries.get(place[0]);

                drawSkillIcon(guiGraphics, entry.id(), x, y);

                // Locked moves are shown behind a wash rather than left out. Knowing a thing exists
                // and is not yours yet is the entire point of the page.
                if (!entry.unlocked()) {
                    guiGraphics.fill(x + 1, y + 1, x + ICON - 1, y + ICON - 1, COLOR_LOCKED);
                }

                int slot = data.slotOf(entry.id(), editingUtility());

                // The viewport test as well as the icon's own: half an icon may be showing at the
                // frame edge, and the half that is hidden must not answer the pointer.
                if (overSkill(place[0], scroll)) {
                    tooltip = List.of(
                            entry.name().copy().withStyle(style -> style.withColor(
                                    entry.unlocked() ? COLOR_ACCENT : COLOR_DIM)),
                            Component.literal(slotNote(entry, slot))
                                    .withStyle(style -> style.withColor(COLOR_DIM)));
                }
            }
        });

        drawSkillScrollbar(guiGraphics, px, py);
    }

    /**
     * Sorts the list into branches and works out where everything lands.
     *
     * <p>Branch order is the enum's own, so the groups always appear in the same sequence whatever
     * order the moves were registered in - which is the entire point of grouping them. A move that
     * belongs to no tree goes in a group of its own at the end rather than being dropped.
     */
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

    /** One group: its rule, then its icons, three to a row. Returns the y after it. */
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
                    (placed % GRID_COLS) * ROW_PITCH,
                    y + (placed / GRID_COLS) * ROW_PITCH});
            placed++;
        }

        return placed == 0 ? y
                : y + ((placed + GRID_COLS - 1) / GRID_COLS) * ROW_PITCH;
    }

    /** Which arm of the tree a listed move belongs to, or null if no tree mentions it. */
    private static SkillNode.Branch branchOf(SkillBook.Entry entry) {
        SkillNode node = SkillTrees.forSkill(entry.id());
        return node == null ? null : node.branch();
    }

    /**
     * The tree: tiled ground, the web over it, then the frame on top to mask the edges.
     *
     * <p>Order matters at both ends. The background has to be clipped to the window or it paints
     * over the panel around it, and the frame has to go on last or the nodes draw over its border
     * as they pan past.
     */
    private void renderSkillTree(GuiGraphics guiGraphics, JojohaPlayerData data) {
        // Worked out before the draw rather than after it, because the links leading to it are
        // drawn differently. Asking afterwards would light them one frame late.
        // Every Stand has its own tree, so the page has to be told which one before anything reads
        // it. Cheap and idempotent - showStand returns immediately unless the answer has changed.
        treeView.showStand(data.stand.isPresent() ? data.stand.standId() : null);

        SkillNode hovered = treeView.nodeAt(localMouseX, localMouseY,
                TREE_VIEW_X, TREE_VIEW_Y, TREE_VIEW_W, TREE_VIEW_H);

        clipped(guiGraphics, TREE_VIEW_X, TREE_VIEW_Y, TREE_VIEW_W, TREE_VIEW_H, () -> {
            // Repeated rather than stretched - it is a 16 square tile and stretching it to 109 would
            // land its pixels on fractions of one.
            // Bounded by the window itself, not by the window plus a tile. The last row and
            // column already overhang the edge by up to a tile, so going one further drew a whole
            // row and column of tiles entirely outside the scissor - 72 blits a frame where 56 do.
            for (int ty = 0; ty < TREE_VIEW_H; ty += TREE_TILE) {
                for (int tx = 0; tx < TREE_VIEW_W; tx += TREE_TILE) {
                    blit(guiGraphics, TREE_BACKGROUND, TREE_VIEW_X + tx, TREE_VIEW_Y + ty,
                            TREE_TILE, TREE_TILE);
                }
            }

            treeView.render(guiGraphics, data, TREE_VIEW_X, TREE_VIEW_Y, TREE_VIEW_W,
                    TREE_VIEW_H, hovered);
        });

        blit(guiGraphics, TREE_FRAME, TREE_X, TREE_Y, TREE_W, TREE_H);

        if (hovered != null && !treeView.dragging()) {
            tooltip = treeView.tooltip(data, hovered);
        }
    }

    /**
     * The two tabs, the active one clear of the panel and the other tucked behind it.
     *
     * <p>Drawn in that order deliberately - the tucked one first - so that if the two ever overlap
     * the one you are on is the one on top.
     */
    private void drawPageTabs(GuiGraphics guiGraphics, int drop) {
        boolean home = page == MenuPage.HOME;

        blit(guiGraphics, HOME_PAGE_BUTTON, HOME_BTN_X,
                (home ? PAGE_BTN_ACTIVE_Y : PAGE_BTN_TUCKED_Y) - drop, PAGE_BTN_W, PAGE_BTN_H);
        blit(guiGraphics, SKILLS_PAGE_BUTTON, SKILLS_BTN_X,
                (home ? PAGE_BTN_TUCKED_Y : PAGE_BTN_ACTIVE_Y) - drop, PAGE_BTN_W, PAGE_BTN_H);
    }

    /**
     * The flag, brighter while the pointer is on it.
     *
     * <p>Position alone was not saying it was a control. A tab at least looks like a tab; a pennant
     * hanging off the bottom of a panel looks like decoration, and decoration is not something
     * anybody tries to click. Lighting up under the cursor is the shortest way to say otherwise, and
     * it costs one test against the same rectangle the click already uses.
     */
    private void drawFlag(GuiGraphics guiGraphics, int drop, float centreOpen) {
        boolean over = within(FLAG_X, flagHitTop(), FLAG_W, flagHitHeight());

        if (over) {
            // Above one on purpose - the shader multiplies, so this lifts the sprite rather than
            // tinting it, and the pennant keeps its own colour while getting brighter.
            guiGraphics.setColor(FLAG_HOVER_LIFT, FLAG_HOVER_LIFT, FLAG_HOVER_LIFT, centreOpen);
        }

        blit(guiGraphics, CB_FLAG, FLAG_X, flagY() - drop, FLAG_W, FLAG_H);

        if (over) {
            guiGraphics.setColor(1F, 1F, 1F, centreOpen);
        }
    }

    /**
     * The top of the part of the flag that is not behind the panel.
     *
     * <p>Shared by the drawing and the click, so the lit region and the pressable one are the same
     * rectangle by construction rather than by two constants agreeing.
     */
    private int flagHitTop() {
        return Math.max(flagY(), MENU_BOTTOM);
    }

    private int flagHitHeight() {
        return flagY() + FLAG_H - flagHitTop();
    }

    /**
     * Where the flag hangs this frame: raised behind the panel with the bar shut, lowered clear of
     * it once the bar is out, and every position between on the way.
     */
    private int flagY() {
        return Math.round(Mth.lerp(ease(barProgress()), FLAG_RAISED_Y, FLAG_LOWERED_Y));
    }

    /** Where a tab sits this frame, which is also the only part of it that answers a click. */
    private int tabY(MenuPage which) {
        return page == which ? PAGE_BTN_ACTIVE_Y : PAGE_BTN_TUCKED_Y;
    }

    /**
     * The height of a tab that can actually be clicked.
     *
     * <p>A tucked tab is three pixels of sprite under the panel edge, and those three pixels are
     * the whole of it as far as the mouse is concerned. Using the full eighteen would put a
     * clickable strip underneath the panel, where the user can see the panel and not the tab.
     */
    private int tabHitHeight(MenuPage which) {
        return page == which ? PAGE_BTN_H : PAGE_BTN_PEEK;
    }

    /** Where a tucked tab's clickable strip starts - its bottom three rows. */
    private int tabHitY(MenuPage which) {
        return page == which ? PAGE_BTN_ACTIVE_Y : MENU_BOTTOM;
    }

    /**
     * The combat bar, and whatever is currently on it.
     *
     * <p>The point of drawing it here at all is that assigning a move is otherwise blind: the grid
     * knows which slot a move is in, but nothing showed what the bar looked like as a whole, so
     * building a loadout meant closing the menu to check and opening it again to fix.
     */
    private void drawCombatBar(GuiGraphics guiGraphics, JojohaPlayerData data) {
        blit(guiGraphics, COMBAT_BAR, CB_X, CB_Y, CB_W, CB_H);
        drawBarMode(guiGraphics);
        drawBarPages(guiGraphics);

        for (int i = 0; i < CB_SLOTS; i++) {
            int slot = barPage * CB_SLOTS + i;
            int x = CB_SLOT_X + i * CB_SLOT_PITCH;
            ResourceLocation held = heldInBar(data, slot);

            // Only the move goes in. The well is already drawn into the ribbon - painting a frame
            // over it stacked a second border on top of the one in the art, which is the other half
            // of why the row looked wrong.
            if (held != null) {
                drawBarSlot(guiGraphics, held, x, CB_SLOT_Y);
            }

            // The slot a picked move would land in, lit up, so the two-step has somewhere obvious
            // to finish.
            if (picked != null && within(x, CB_SLOT_Y, CB_SLOT_SIZE, CB_SLOT_SIZE)) {
                guiGraphics.fill(x, CB_SLOT_Y, x + CB_SLOT_SIZE, CB_SLOT_Y + CB_SLOT_SIZE,
                        COLOR_SLOT_PICKED);
            }

            if (within(x, CB_SLOT_Y, CB_SLOT_SIZE, CB_SLOT_SIZE)) {
                tooltip = List.of(
                        Component.literal((barUtility ? "Utility slot " : "Slot ") + (slot + 1))
                                .withStyle(style -> style.withColor(COLOR_ACCENT)),
                        Component.literal(picked != null ? "Click to place the picked move"
                                : held == null ? "Empty - pick a move first"
                                : "Right click to clear")
                                .withStyle(style -> style.withColor(COLOR_DIM)));
            }
        }
    }

    /**
     * Which bar the grid is talking about.
     *
     * <p>Only the Utility one while its bar is actually open and switched to it. With the bar shut
     * there is nothing on screen saying which is meant, and the ordinary one is what a click on a
     * move has always meant - so it stays the answer.
     */
    private boolean editingUtility() {
        return combatBarOpen && barUtility;
    }

    /**
     * What is in a slot of whichever bar is being edited.
     *
     * <p>The Utility bar shows the stock tools until it has been arranged, which is exactly what the
     * stance itself does - so opening it for the first time shows what you would actually get rather
     * than an empty row that lies about it.
     */
    private ResourceLocation heldInBar(JojohaPlayerData data, int slot) {
        if (!barUtility) {
            return data.equippedSkill(slot);
        }
        if (!data.utilityLoadoutUntouched()) {
            return data.utilityEquippedSkill(slot);
        }
        return slot < StandSkills.UTILITY_TOOLS.size()
                ? StandSkills.UTILITY_TOOLS.get(slot).id() : null;
    }

    /** The button that swaps which of the two bars is being arranged. */
    private void drawBarMode(GuiGraphics guiGraphics) {
        blit(guiGraphics, barUtility ? MODE_UTILITY : MODE_DEFAULT,
                MODE_X, MODE_Y, MODE_SIZE, MODE_SIZE);

        if (within(MODE_X, MODE_Y, MODE_SIZE, MODE_SIZE)) {
            tooltip = List.of(
                    Component.literal(barUtility ? "Utility bar" : "Combat bar")
                            .withStyle(style -> style.withColor(COLOR_ACCENT)),
                    Component.literal(barUtility
                            ? "What your Stand carries in the Utility stance"
                            : "Your ordinary moves")
                            .withStyle(style -> style.withColor(COLOR_DIM)),
                    Component.literal("Click to swap")
                            .withStyle(style -> style.withColor(COLOR_DIM)));
        }
    }

    /**
     * The page marker: one bar, on the side of the page you are on.
     *
     * <p>Only ever one is drawn. Two at different strengths read as two things you had to compare;
     * a single mark that is either left or right reads as a position, which is what a page is.
     *
     * <p>Both sides stay clickable regardless of which is lit, so the way to the other page is to
     * click where it would be - the same place it appears once you are there.
     */
    private void drawBarPages(GuiGraphics guiGraphics) {
        blit(guiGraphics, PAGE_BAR, barPage == 0 ? PAGE_BAR_LEFT_X : PAGE_BAR_RIGHT_X,
                PAGE_BAR_Y, PAGE_BAR_W, PAGE_BAR_H);

        pageTooltip(PAGE_BAR_LEFT_X, 0);
        pageTooltip(PAGE_BAR_RIGHT_X, 1);
    }

    private void pageTooltip(int x, int page) {
        if (!within(x, PAGE_BAR_Y, PAGE_BAR_W, PAGE_BAR_H)) {
            return;
        }

        tooltip = List.of(
                Component.literal("Page " + (page + 1))
                        .withStyle(style -> style.withColor(COLOR_ACCENT)),
                Component.literal("Slots " + (page * CB_SLOTS + 1)
                        + " to " + ((page + 1) * CB_SLOTS))
                        .withStyle(style -> style.withColor(COLOR_DIM)));
    }

    /**
     * The bar down the right of the grid, drawn only when there is somewhere to scroll to.
     *
     * <p>A scroll area with no visible sign that it scrolls is a scroll area nobody scrolls. The
     * thumb is sized to the fraction of the list on screen, so it doubles as the count - a short
     * thumb is the page saying there is a great deal more of this.
     */
    private void drawSkillScrollbar(GuiGraphics guiGraphics, int px, int py) {
        int max = maxScroll();
        if (max <= 0) {
            return;
        }

        int x = px + SCROLLBAR_X;
        int y = py + GRID_Y;
        guiGraphics.fill(x, y, x + SCROLLBAR_W, y + GRID_VIEW_H, COLOR_SCROLL_TRACK);

        int thumb = Math.max(SCROLLBAR_MIN_THUMB,
                GRID_VIEW_H * GRID_VIEW_H / Math.max(1, gridContentHeight));
        int top = y + (GRID_VIEW_H - thumb) * skillScroll / max;
        drawScrollThumb(guiGraphics, x, top, thumb);
    }

    /**
     * The thumb, in three pieces: top cap, as much middle as it takes, bottom cap.
     *
     * <p>The middle is repeated rather than scaled, and the last repeat is cut short by asking for a
     * shorter slice of the same rows - so the join is always on a whole pixel and the bar never
     * softens however long it gets.
     */
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

    /**
     * A move sitting in one of the bar's wells.
     *
     * <p>No plate and no frame - the ribbon has both already. A move whose art has not been drawn
     * yet falls back to the base plate <em>cropped</em> to 19 rather than scaled into it, so the
     * odd one out still lands on whole pixels like everything else here.
     */
    private void drawBarSlot(GuiGraphics guiGraphics, ResourceLocation skillId, int x, int y) {
        ResourceLocation icon = SkillIcons.of(skillId);
        if (icon != null) {
            blit(guiGraphics, icon, x, y, SkillIcons.SIZE, SkillIcons.SIZE);
            return;
        }

        guiGraphics.blit(SKILL_BASE_ICON, x, y, 1F, 1F,
                SkillIcons.SIZE, SkillIcons.SIZE, ICON, ICON);
    }

    /**
     * A move in a 21px frame: the base plate, its own art if it has any, then the border.
     *
     * <p>The art is 19 and the frame is a one pixel border, so the icon goes in at an offset of one
     * and lands exactly inside it. Nothing is scaled - this menu has been through mixed resolutions
     * once already, and 19 into 21 is the reason the frame is 21.
     */
    private void drawSkillIcon(GuiGraphics guiGraphics, ResourceLocation skillId, int x, int y) {
        blit(guiGraphics, SKILL_BASE_ICON, x, y, ICON, ICON);

        ResourceLocation icon = SkillIcons.of(skillId);
        if (icon != null) {
            blit(guiGraphics, icon, x + 1, y + 1, SkillIcons.SIZE, SkillIcons.SIZE);
        }

        blit(guiGraphics, SKILL_ICON_FRAME, x, y, ICON, ICON);
    }

    /** The furthest the grid may be scrolled, which is zero whenever it all fits. */
    private int maxScroll() {
        return Math.max(0, gridContentHeight - GRID_VIEW_H);
    }

    /** Whether the pointer is over the move at this index, given where the grid is scrolled to. */
    private boolean overSkill(int index, int scroll) {
        if (!within(RIGHT_X + SKILL_BG_X, RIGHT_Y + GRID_Y, SKILL_BG_W, GRID_VIEW_H)) {
            return false;
        }

        for (int[] place : gridSlots) {
            if (place[0] == index) {
                return within(RIGHT_X + GRID_X + place[1],
                        RIGHT_Y + GRID_Y + place[2] - scroll, ICON, ICON);
            }
        }
        return false;
    }

    /**
     * What a move's tooltip says about where it is.
     *
     * <p>Three states, and they are the whole of the interaction: something you cannot have,
     * something you can put on the bar, and something already on it.
     */
    private static String slotNote(SkillBook.Entry entry, int slot) {
        if (!entry.unlocked()) {
            return "Locked";
        }
        return slot >= 0 ? "On slot " + (slot + 1) + " - right click to remove"
                : "Click to put on the bar";
    }

    /**
     * A click on the skill grid.
     *
     * <p>Left puts a move on the bar and right takes it off, and neither says where - the server
     * picks the slot and answers with a sync. The client could work out which slot is free itself
     * and be wrong about it, because the only copy of that answer which matters is the one the
     * server holds.
     */
    /**
     * A click inside the tree window: a node if there is one there, otherwise the start of a drag.
     *
     * <p>The unlock is only ever a request. The server owns the tree, re-tests every requirement,
     * and the node lights up when the sync comes back - which is also what stops a double click
     * from spending items twice.
     */
    private boolean clickedTree(int button) {
        if (button != 0 || !within(TREE_VIEW_X, TREE_VIEW_Y, TREE_VIEW_W, TREE_VIEW_H)) {
            return false;
        }

        SkillNode node = treeView.nodeAt(localMouseX, localMouseY,
                TREE_VIEW_X, TREE_VIEW_Y, TREE_VIEW_W, TREE_VIEW_H);

        if (node != null) {
            JojohaPlayerData data = ClientPlayerDataCache.data;
            if (!data.hasNode(node.id())
                    && org.gumel.jojoha.skilltree.SkillTrees.parentDone(data, node)
                    && org.gumel.jojoha.skilltree.SkillTrees.unmetStats(data, node).isEmpty()) {
                NetworkHandler.sendUnlockNode(node.id());
            }
            // A sound either way: a click that lands on a node it cannot afford is still a click
            // that landed, and silence is indistinguishable from having missed.
            click();
            return true;
        }

        treeView.beginDrag(localMouseX, localMouseY);
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && treeView.dragging()) {
            treeView.drag((mouseX - offsetX) / (double) scale,
                    (mouseY - offsetY + barLift) / (double) scale);
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
        return super.mouseReleased(mouseX, mouseY, button);
    }

    /**
     * The flag, the two tabs, and the combat bar - everything in the strip under the menu.
     *
     * <p>Tested before anything else, because it all sits outside the panels and so cannot collide
     * with them; anything it does not claim falls through untouched.
     */
    private boolean clickedStrip(int button) {
        // Whatever of the flag is currently below the panel edge, and all of it once lowered. The
        // rest is behind the panel and must not be clickable. Same rectangle the hover lights.
        if (button == 0 && within(FLAG_X, flagHitTop(), FLAG_W, flagHitHeight())) {
            barProgressAtChange = barProgress();
            barChangedAt = System.currentTimeMillis();
            combatBarOpen = !combatBarOpen;

            // A move picked up for a bar that is no longer showing would sit there invisibly and
            // then land somewhere surprising the next time the bar was opened.
            if (!combatBarOpen) {
                picked = null;
            }
            click();
            return true;
        }

        if (button == 0 && within(HOME_BTN_X, tabHitY(MenuPage.HOME), PAGE_BTN_W,
                tabHitHeight(MenuPage.HOME))) {
            if (page != MenuPage.HOME) {
                page = MenuPage.HOME;
                pageChangedAt = System.currentTimeMillis();
            }
            click();
            return true;
        }

        if (button == 0 && within(SKILLS_BTN_X, tabHitY(MenuPage.SKILLS), PAGE_BTN_W,
                tabHitHeight(MenuPage.SKILLS))) {
            if (page != MenuPage.SKILLS) {
                page = MenuPage.SKILLS;
                pageChangedAt = System.currentTimeMillis();
            }
            click();
            return true;
        }

        return combatBarOpen && clickedCombatBar(button);
    }

    /**
     * A click somewhere on the combat bar.
     *
     * <p>The pointed ends turn the page. There are sixteen slots and room for eight, and the ends
     * were already chevrons pointing the way - a pair of arrows drawn on top of them would have
     * been the same control twice.
     */
    private boolean clickedCombatBar(int button) {
        // The left tip is now the mode button rather than a second page arrow. Two pages need one
        // arrow that wraps, not two that do the same thing in opposite directions.
        if (button == 0 && within(MODE_X, MODE_Y, MODE_SIZE, MODE_SIZE)) {
            barUtility = !barUtility;
            barPage = 0;
            picked = null;
            click();
            return true;
        }

        if (button == 0 && within(PAGE_BAR_LEFT_X, PAGE_BAR_Y, PAGE_BAR_W, PAGE_BAR_H)) {
            barPage = 0;
            click();
            return true;
        }

        if (button == 0 && within(PAGE_BAR_RIGHT_X, PAGE_BAR_Y, PAGE_BAR_W, PAGE_BAR_H)) {
            barPage = 1;
            click();
            return true;
        }

        for (int i = 0; i < CB_SLOTS; i++) {
            int x = CB_SLOT_X + i * CB_SLOT_PITCH;
            if (!within(x, CB_SLOT_Y, CB_SLOT_SIZE, CB_SLOT_SIZE)) {
                continue;
            }

            int slot = barPage * CB_SLOTS + i;

            if (button == 1) {
                NetworkHandler.sendClearSlot(slot, barUtility);
                click();
                return true;
            }

            if (button == 0 && picked != null) {
                // Asked for, not applied - the server owns the loadout, and this client will see it
                // when the sync comes back. Same bargain the stat buttons make.
                NetworkHandler.sendEquipSkill(picked, slot, barUtility);
                picked = null;
                click();
                return true;
            }

            // An empty-handed click on a slot is not nothing: it makes a sound, so a click that
            // landed is never mistaken for one that missed.
            click();
            return true;
        }

        return false;
    }

    private boolean clickedSkill(JojohaPlayerData data, int button) {
        if (button != 0 && button != 1) {
            return false;
        }

        List<SkillBook.Entry> entries = SkillBook.of(category, data);
        for (int i = 0; i < entries.size(); i++) {
            if (!overSkill(i, skillScroll)) {
                continue;
            }

            SkillBook.Entry entry = entries.get(i);
            if (!entry.unlocked()) {
                // Nothing happens, and it makes a sound anyway - silence is indistinguishable from
                // a click that missed.
                click();
                return true;
            }

            if (button == 1) {
                NetworkHandler.sendUnequipSkill(entry.id(), editingUtility());
            } else if (combatBarOpen) {
                // Picked up rather than placed. With the bar showing there is somewhere to aim, and
                // choosing the slot yourself is the whole reason to have opened it.
                picked = picked != null && picked.equals(entry.id()) ? null : entry.id();
            } else {
                NetworkHandler.sendEquipSkill(entry.id(), -1, editingUtility());
            }

            click();
            return true;
        }

        return false;
    }

    /** The one sound every button in here makes, so they all sound like the same interface. */
    private static void click() {
        Minecraft.getInstance().getSoundManager().play(
                net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                        net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1F));
    }

    /**
     * A tooltip that wraps, on the mod's own frame.
     *
     * <p>Vanilla's {@code renderComponentTooltip} lays each component out on one line however long
     * it is, which for a passive description was a sentence running off the side of the screen. Each
     * line is split to a width here first, so the box grows downward instead of sideways.
     */
    private void drawTooltip(GuiGraphics guiGraphics, List<Component> lines, int mouseX, int mouseY) {
        List<net.minecraft.util.FormattedCharSequence> wrapped = new java.util.ArrayList<>();
        for (Component line : lines) {
            wrapped.addAll(this.font.split(line, TOOLTIP_MAX_W));
        }
        if (wrapped.isEmpty()) {
            return;
        }

        int textW = 0;
        for (net.minecraft.util.FormattedCharSequence line : wrapped) {
            textW = Math.max(textW, this.font.width(line));
        }

        int boxW = textW + TOOLTIP_PAD * 2;
        int boxH = wrapped.size() * this.font.lineHeight + TOOLTIP_PAD * 2;

        // Offset from the cursor, then pulled back inside the screen rather than allowed off it.
        int x = Math.min(mouseX + 10, this.width - boxW - 2);
        int y = Math.min(mouseY - 10, this.height - boxH - 2);
        x = Math.max(2, x);
        y = Math.max(2, y);

        guiGraphics.pose().pushPose();
        // Above everything, including the entity renders, which draw with real depth.
        guiGraphics.pose().translate(0F, 0F, 400F);

        nineSlice(guiGraphics, TOOLTIP, x, y, boxW, boxH);

        int lineY = y + TOOLTIP_PAD;
        for (net.minecraft.util.FormattedCharSequence line : wrapped) {
            guiGraphics.drawString(this.font, line, x + TOOLTIP_PAD, lineY, COLOR_TEXT, true);
            lineY += this.font.lineHeight;
        }

        guiGraphics.pose().popPose();
    }

    /**
     * Draws a bordered box of any size from one square of art.
     *
     * <p>Corners at their own size so they never distort, edges stretched along their run, and the
     * middle stretched both ways. Stretching the whole texture instead - the obvious thing - would
     * put a three pixel border on a small box and a twenty pixel border on a large one.
     */
    private static void nineSlice(GuiGraphics guiGraphics, ResourceLocation texture,
                                  int x, int y, int w, int h) {
        int b = TOOLTIP_BORDER;
        int t = TOOLTIP_TEX;
        int inner = t - b * 2;
        int iw = Math.max(0, w - b * 2);
        int ih = Math.max(0, h - b * 2);
        int far = t - b;

        guiGraphics.blit(texture, x, y, b, b, 0F, 0F, b, b, t, t);
        guiGraphics.blit(texture, x + w - b, y, b, b, far, 0F, b, b, t, t);
        guiGraphics.blit(texture, x, y + h - b, b, b, 0F, far, b, b, t, t);
        guiGraphics.blit(texture, x + w - b, y + h - b, b, b, far, far, b, b, t, t);

        guiGraphics.blit(texture, x + b, y, iw, b, b, 0F, inner, b, t, t);
        guiGraphics.blit(texture, x + b, y + h - b, iw, b, b, far, inner, b, t, t);
        guiGraphics.blit(texture, x, y + b, b, ih, 0F, b, b, inner, t, t);
        guiGraphics.blit(texture, x + w - b, y + b, b, ih, far, b, b, inner, t, t);

        guiGraphics.blit(texture, x + b, y + b, iw, ih, b, b, inner, inner, t, t);
    }

    /** How tall one line of body text is, in panel pixels. */
    private int lineStep() {
        return Math.max(1, Math.round(this.font.lineHeight * textScale));
    }

    /**
     * Body text: a step smaller than the panels, with the shadow every other label in the game has.
     *
     * <p>Trimmed to {@code maxWidth} given in panel pixels, which is why the width is converted
     * before it is handed to the font - the font measures in its own units and knows nothing about
     * the scale it is about to be drawn under.
     */
    private void smallLeft(GuiGraphics guiGraphics, String text, int x, int y, int maxWidth,
                           int colour) {
        smallRaw(guiGraphics, trim(text, Math.round(maxWidth / textScale)), x, y, colour);
    }

    private void smallRaw(GuiGraphics guiGraphics, String text, int x, int y, int colour) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 0);
        guiGraphics.pose().scale(textScale, textScale, 1F);
        guiGraphics.drawString(this.font, text, 0, 0, colour, true);
        guiGraphics.pose().popPose();
    }

    /** Names the Stand and the skin it is wearing, but only when the picture is pointed at. */
    private void standTooltip(GuiGraphics guiGraphics, JojohaPlayerData data, StandType type,
                              int x, int y, int w, int h) {
        if (!within(x, y, w, h)) {
            return;
        }

        int skin = data.stand.skin();

        // The skin's name in the skin's own colour, which is the quickest way to say which one is on
        // without a second line explaining it - a manga Star Platinum reads pink because that is
        // what a manga Star Platinum is.
        int tint = type.auraColorFor(skin);

        tooltip = List.of(
                Component.literal(upper(data.stand.standId().getPath()))
                        .withStyle(style -> style.withColor(COLOR_ACCENT)),
                Component.literal("Skin - ")
                        .withStyle(style -> style.withColor(COLOR_DIM))
                        .append(Component.translatable(type.skinNameKey(skin))
                                .withStyle(style -> style.withColor(
                                        net.minecraft.network.chat.TextColor.fromRgb(tint)))));
    }

    /** The longest prefix that fits, with an ellipsis when anything had to go. */
    private String trim(String text, int maxWidth) {
        if (this.font.width(text) <= maxWidth) {
            return text;
        }

        String ellipsis = "...";
        int room = Math.max(0, maxWidth - this.font.width(ellipsis));
        return this.font.plainSubstrByWidth(text, room) + ellipsis;
    }

    private static String upper(String name) {
        return name.toUpperCase(Locale.ROOT).replace('_', ' ');
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        // Only over the grid. A wheel anywhere else on the menu is not aimed at this, and swallowing
        // it would make the page feel like it had captured the mouse.
        if (scrollY != 0 && page == MenuPage.SKILLS
                && within(TREE_VIEW_X, TREE_VIEW_Y, TREE_VIEW_W, TREE_VIEW_H)) {
            treeView.zoomBy((int) Math.signum(scrollY), localMouseX, localMouseY,
                    TREE_VIEW_X, TREE_VIEW_Y, TREE_VIEW_W, TREE_VIEW_H);
            return true;
        }

        if (scrollY != 0
                && within(RIGHT_X + SKILL_BG_X, RIGHT_Y + GRID_Y, SKILL_BG_W, GRID_VIEW_H)) {
            // The laid-out height from the last frame, which is what the wheel is scrolling
            // through - recomputing it from the entry count would ignore the group rules.
            int max = maxScroll();
            int moved = Mth.clamp(skillScroll - (int) Math.signum(scrollY) * SCROLL_STEP, 0, max);

            // Reported as handled only when it actually moved, so a wheel at the end of the list
            // still reaches whatever else might want it.
            if (moved != skillScroll) {
                skillScroll = moved;
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (clickedStrip(button)) {
            return true;
        }

        if (page == MenuPage.SKILLS && clickedTree(button)) {
            return true;
        }

        if (button == 0 && within(CENTRE_X + TOGGLE_X, CENTRE_Y + TOGGLE_Y, TOGGLE_W, TOGGLE_H)) {
            showingStand = !showingStand;
            click();
            return true;
        }

        // The category disc is its own next button.
        if (button == 0 && within(RIGHT_X + PRESET_X, RIGHT_Y + PRESET_Y,
                PRESET_SIZE, PRESET_SIZE)) {
            category = category.next();
            skillScroll = 0;
            treeView.show(treeFor(category));
            click();
            return true;
        }

        if (button == 0 && ClientPlayerDataCache.data.pointsFor(showingStand) > 0) {
            int stepArrowY = CENTRE_Y + STEP_Y + (PLATE_H - ARROW_H) / 2;
            int stepLeft = CENTRE_X + STEP_X;
            int stepRight = stepLeft + ARROW_W + STEP_GAP + PLATE_W + STEP_GAP;

            if (within(stepLeft, stepArrowY, ARROW_W, ARROW_H)) {
                stepIndex--;
                click();
                return true;
            }
            if (within(stepRight, stepArrowY, ARROW_W, ARROW_H)) {
                stepIndex++;
                click();
                return true;
            }
        }

        JojohaPlayerData data = ClientPlayerDataCache.data;

        if (clickedSkill(data, button)) {
            return true;
        }

        boolean spendable = data.pointsFor(showingStand) > 0
                && (!showingStand || data.stand.isPresent());

        if (button == 0 && spendable) {
            int rowHeight = ROWS_H / StatPoints.COUNT;
            for (int i = 0; i < StatPoints.COUNT; i++) {
                if (statAt(data, i) < StatPoints.MAX_STAT
                        && within(CENTRE_X + STAT_BUTTON_X, CENTRE_Y + ROWS_Y + i * rowHeight,
                                STAT_BUTTON_SIZE, STAT_BUTTON_SIZE)) {
                    // Asked for, not applied. The server owns the pool and the stat; this client
                    // will see the change when the sync comes back, which is also what stops a
                    // double click from spending a point that was never there.
                    NetworkHandler.sendSpendStatPoint(showingStand, i, step());
                    click();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static ResourceLocation menu(String fileName) {
        return ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/gui/menu/" + fileName);
    }
}
