package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;

import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphics;
import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.ClientPlayerDataCache;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.registry.ModRegistries;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.StandTypes;
import org.gumel.jojoha.stand.TrustTier;
import org.gumel.jojoha.stand.skill.StandSkill;
import org.gumel.jojoha.stand.skill.StandSkills;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.slf4j.Logger;

/**
 * In-game combat HUD: the corner bar frame, a Stand-state icon, spec/stand energy gauges and
 * a page toggle. Built from the commissioned combat-bar art. Always shown while in-game (see
 * {@link #toggleVisibility} for the player-facing on/off switch).
 *
 * <p>Registered per-platform (Fabric's {@code HudRenderCallback}, NeoForge's
 * {@code RenderGuiEvent.Post}) - both just forward to {@link #render}.
 */
public final class CombatBarOverlay {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final ResourceLocation BAR = hud("combat_bar.png");
    private static final ResourceLocation PAGE = hud("combat_bar_page.png");
    private static final ResourceLocation SPEC_BAR = hud("combat_bar_spec_bar.png");
    private static final ResourceLocation STAND_BAR = hud("combat_bar_standenergy_bar.png");
    private static final ResourceLocation STAND_STATES = hud("stand_states.png");
    private static final ResourceLocation STAND_BACKGROUND = hud("stand_background.png");

    // Native pixel size of the actually-drawn art within combat_bar.png (the source canvas is
    // much larger than the bar itself - scanned directly from the art).
    private static final int BAR_W = 155, BAR_H = 165;
    // The real full size of the combat_bar.png file - needed as blit()'s UV-normalization
    // denominator even though we only ever draw the BAR_W x BAR_H corner of it. Passing the
    // cropped size there instead (as this used to) stretches the whole 427x240 canvas - padding
    // included - to fit the crop box, squashing the real art down to a fraction of it.
    private static final int BAR_TEX_W = 427, BAR_TEX_H = 240;

    // The black square in the bar's top-left corner (scanned from the art: outer square roughly
    // x:[2,35] y:[8,41]) - no longer holds the Stand-state icon (moved below the whole bar, see
    // STAND_STATE_ICON_X/Y below), but other elements are still positioned relative to it.
    // stand_background is drawn BEHIND the bar frame (before BAR's own blit) so the frame's
    // border renders on top of it.
    private static final int SQUARE_X = 3, SQUARE_Y = 9, SQUARE_SIZE = 32;
    private static final int STAND_BACKGROUND_SIZE = 31;
    private static final int STAND_BACKGROUND_X = SQUARE_X + 3, STAND_BACKGROUND_Y = SQUARE_Y + 3;

    // Live 3D portrait, drawn into the same slot as stand_background (in front of it, behind the
    // bar frame - see render()).
    //
    // star_platinum.geo.json spans y=0 (feet) to y~36 Blockbench units => ~2.25 blocks tall, and
    // its origin is at the feet. Framed to the upper body: the upperTorso bone starts at y=17
    // units (~1.06 blocks) and the hair tops out around y=36 (~2.25), so that span is ~1.19
    // blocks centred on ~1.65. SCALE is slotPx / spanBlocks (31 / 1.19 ~= 26) and ANCHOR_Y is the
    // model-space height that lands at the slot's centre.
    private static final float PORTRAIT_SCALE = 26F;
    private static final float PORTRAIT_MODEL_ANCHOR_Y = 1.65F;

    // The rotateZ(180 degrees) is NOT decorative and must not be dropped: renderEntityInInventory
    // does scale(s, s, -s), which mirrors the Y axis, so an un-rotated model renders upside down
    // AND its translation/extent run the wrong way down the screen (which is exactly how this
    // silently rendered "nothing" - the model was flipped below the slot and scissored away).
    // Vanilla's own call site builds its pose the same way; see
    // InventoryScreen.renderEntityInInventoryFollowsMouse (bytecode-verified: `new Quaternionf()
    // .rotateZ(3.1415927f)`). The rotateY afterwards turns the model to face the viewer: the
    // model's own forward axis points away from the camera in this space, so it needs 180
    // degrees to face front, plus 20 for a slight three-quarter angle rather than dead-on.
    private static final Quaternionf PORTRAIT_ROTATION =
            new Quaternionf().rotateZ((float) Math.PI).rotateY((float) Math.toRadians(200));

    private static StandEntity previewStand;
    /** Tracks the moment a Stand is first obtained, so the portrait can fade in - see renderStandPortrait. */
    private static boolean hadStandLastFrame;
    // Most GUI/HUD render dispatchers swallow exceptions from individual overlays to avoid one
    // broken element crashing the whole frame - which means a bug here would otherwise fail
    // dead-silent every frame with no log trace at all. Log it once instead of not at all, and
    // stop retrying so a persistent failure doesn't spam the log 20x/sec.
    private static boolean portraitRenderFailed = false;

    // Stand-state icon sits below the entire bar frame. Toggles DEFAULT/FULL with
    // data.standSummoned - PARTIAL is unused, no partial-manifestation mechanic exists yet.
    private static final int STAND_STATE_FRAME_SIZE = 24;
    private static final int STAND_STATE_ICON_X = 4;
    private static final int STAND_STATE_ICON_Y = BAR_H + 6;

    // Energy gauges sit stacked to the right of the square, flush against each other so the
    // full/empty pair reads as one two-row gauge rather than two disconnected bars.
    private static final int SPEC_BAR_FRAME_W = 81, SPEC_BAR_FRAME_H = 9;
    private static final int STAND_BAR_FRAME_W = 68, STAND_BAR_FRAME_H = 8;
    private static final int GAUGE_X = SQUARE_X + SQUARE_SIZE + 8 - 4;
    private static final int SPEC_BAR_Y = SQUARE_Y + 3 + 2;
    private static final int STAND_BAR_Y = SQUARE_Y + 3 + SPEC_BAR_FRAME_H + 1;

    // Page toggle in the empty gold-bordered notch directly below the square icon.
    private static final int PAGE_FRAME_W = 21, PAGE_FRAME_H = 6;
    private static final int PAGE_X = 6 - 1 + 1, PAGE_Y = 43 + 1;

    // HUD anchor: small margin from the screen's top-left corner.
    private static final int MARGIN_X = 6, MARGIN_Y = 6;

    private static boolean texturesFiltered = false;
    private static boolean showingPage2 = false;
    /**
     * Off until asked for.
     *
     * <p>It used to start on, so every new world opened with the bar already across the screen
     * whether or not anything had been done to earn it. Nothing here is usable before a Stand
     * exists, so showing it on arrival is furniture rather than information.
     */
    private static boolean visible = false;

    private CombatBarOverlay() {
    }

    public static void togglePage() {
        showingPage2 = !showingPage2;
    }

    /**
     * Puts the bar away when the world goes.
     *
     * <p>The flag is static, so without this a session that turned the bar on once would carry that
     * into every world opened afterwards - which is the same complaint as defaulting to on, just
     * delayed. Keyed on the level being gone rather than on a join, because leaving is the moment
     * both quitting to the menu and switching worlds pass through.
     */
    public static void tickClientState() {
        if (Minecraft.getInstance().level == null) {
            visible = false;
        }
    }

    public static void toggleVisibility() {
        visible = !visible;
    }

    /** Which skill page the bar is showing - 0 or 1. Read by the input handler. */
    /** Whether the bar is switched on. Read by CentralBarOverlay, which does the drawing now. */
    public static boolean isVisible() {
        return visible;
    }

    public static int currentSkillPage() {
        return showingPage2 ? 1 : 0;
    }

    public static void render(GuiGraphics guiGraphics) {
        if (!visible || Minecraft.getInstance().player == null || Minecraft.getInstance().screen instanceof PlayerMenuScreen) {
            return;
        }

        JojohaPlayerData data = ClientPlayerDataCache.data;

        ensureFiltered();

        int x = MARGIN_X;
        int y = MARGIN_Y;

        guiGraphics.blit(STAND_BACKGROUND, x + STAND_BACKGROUND_X, y + STAND_BACKGROUND_Y, 0F, 0F,
                STAND_BACKGROUND_SIZE, STAND_BACKGROUND_SIZE, STAND_BACKGROUND_SIZE, STAND_BACKGROUND_SIZE);

        if (data.stand.isPresent() && !portraitRenderFailed) {
            try {
                // The portrait used to need shielding from the time stop, because the stop was
                // applied by the entity shaders and this is a real entity drawn with them - so the
                // picture in the corner of the interface turned into a negative along with the
                // world. It is graded over the finished frame now, before the interface is drawn at
                // all, and the portrait is simply never reached.
                renderStandPortrait(guiGraphics, data, x + STAND_BACKGROUND_X, y + STAND_BACKGROUND_Y);
            } catch (Throwable t) {
                portraitRenderFailed = true;
                LOGGER.error("[jojoha] Stand portrait render failed - disabling it for the rest of this session", t);
            }
        } else {
            // Armed for next time: losing the Stand (or switching worlds) means the next one to
            // arrive should fade in again rather than appearing instantly.
            StandPortrait.forget();
        }

        guiGraphics.blit(BAR, x, y, 0F, 0F, BAR_W, BAR_H, BAR_TEX_W, BAR_TEX_H);

        // Frames: 0 = DEFAULT (nothing manifested - covers both "not cast" and a DORMANT cast,
        // which raises only the aura), 1 = PARTIAL (arms only), 2 = FULL (whole Stand out).
        int standStateFrame = 0;
        if (data.standSummoned && data.stand.isPresent()) {
            TrustTier tier = data.stand.trust();
            standStateFrame = tier.isPartialManifestation() ? 1 : (tier.isFullManifestation() ? 2 : 0);
        }
        guiGraphics.blit(STAND_STATES, x + STAND_STATE_ICON_X, y + STAND_STATE_ICON_Y,
                (float) (standStateFrame * STAND_STATE_FRAME_SIZE), 0F, STAND_STATE_FRAME_SIZE, STAND_STATE_FRAME_SIZE,
                STAND_STATE_FRAME_SIZE * 3, STAND_STATE_FRAME_SIZE);

        drawEnergyBar(guiGraphics, SPEC_BAR, x + GAUGE_X, y + SPEC_BAR_Y, SPEC_BAR_FRAME_W, SPEC_BAR_FRAME_H,
                data.specEnergy / JojohaPlayerData.MAX_SPEC_ENERGY);
        drawEnergyBar(guiGraphics, STAND_BAR, x + GAUGE_X, y + STAND_BAR_Y, STAND_BAR_FRAME_W, STAND_BAR_FRAME_H,
                data.standEnergy / data.maxStandEnergy());

        int pageFrame = showingPage2 ? 1 : 0;
        guiGraphics.blit(PAGE, x + PAGE_X, y + PAGE_Y, (float) (pageFrame * PAGE_FRAME_W), 0F,
                PAGE_FRAME_W, PAGE_FRAME_H, PAGE_FRAME_W * 2, PAGE_FRAME_H);

        drawSkillSlots(guiGraphics, data, x, y);
    }

    // The five slots painted into combat_bar.png, measured off the sprite: each is 19x19 at x=7,
    // starting at y=53 and repeating every 22 pixels down the left column.
    private static final int SKILL_SLOT_X = 7;
    private static final int SKILL_SLOT_Y = 53;
    private static final int SKILL_SLOT_SIZE = 19;
    private static final int SKILL_SLOT_PITCH = 22;
    private static final int SKILL_COOLDOWN_SHADE = 0xB0101010;
    private static final int SKILL_LOCKED_SHADE = 0x90201020;
    /** Keybind badge, tucked into the slot's bottom-right and overhanging it slightly. */
    private static final float SKILL_KEY_SCALE = 0.6F;
    private static final int SKILL_KEY_COLOR = 0xFFB35CFF;
    private static final int SKILL_KEY_BACKING = 0xC0000000;

    /**
     * Fills the bar's five move slots for whichever page is showing.
     *
     * <p>The page toggle that already flips the bar drives this too, rather than a second control:
     * ten moves across two pages is exactly what the bar was drawn for.
     *
     * <p>Cooldown is drawn as a shade draining out of the square rather than as a number. In a
     * fight the only question is whether the move is back yet, which a filling square answers
     * without being read; the exact seconds never matter.
     *
     * <p>Moves the user has not earned are still drawn, dimmed. Hiding them would make the moveset
     * look shorter than it is and give no hint that anything unlocks.
     *
     * <h2>What goes in a slot</h2>
     *
     * <p>Whatever {@link StandSkills#skillInSlot} says, which is the same question the keybinds ask.
     * This used to walk the Stand's moveset in order instead - so the bar showed the moves a Stand
     * has while the keys fired the moves the player had equipped, and the two agreed only until
     * somebody rearranged their loadout. Assigning a move on the skill page changed nothing here,
     * which rather defeated the point of being able to assign one.
     *
     * <p>An empty slot is drawn as an empty slot rather than skipped, and keeps its key badge: the
     * bar is a map of the keys, and a key that does nothing yet is worth showing as available.
     */
    private static void drawSkillSlots(GuiGraphics guiGraphics, JojohaPlayerData data, int x, int y) {
        if (!data.stand.isPresent() || !data.standSummoned) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        long now = minecraft.level == null ? 0L : minecraft.level.getGameTime();
        TrustTier trust = data.stand.trust();

        int firstSlot = (showingPage2 ? 1 : 0) * StandSkills.SLOTS_PER_PAGE;

        for (int row = 0; row < StandSkills.SLOTS_PER_PAGE; row++) {
            int index = firstSlot + row;
            int slotX = x + SKILL_SLOT_X;
            int slotY = y + SKILL_SLOT_Y + row * SKILL_SLOT_PITCH;

            StandSkill skill = StandSkills.skillInSlot(data, index);
            if (skill == null) {
                drawSlotKey(guiGraphics, minecraft,
                        StandSkillInput.slotKeyLabel(row).getString(), slotX, slotY);
                continue;
            }

            boolean available = skill.isUnlocked(data) && trust.level() >= skill.minimumTrust().level();
            float cooling = cooldownFraction(data, skill, now);

            // Before the shades, so a cooling move is its own icon draining rather than a shade
            // with a picture sitting on top of it.
            ResourceLocation icon = SkillIcons.of(skill.id());
            if (icon != null) {
                guiGraphics.blit(icon, slotX, slotY, 0F, 0F,
                        SKILL_SLOT_SIZE, SKILL_SLOT_SIZE, SKILL_SLOT_SIZE, SKILL_SLOT_SIZE);
            }

            if (!available) {
                guiGraphics.fill(slotX, slotY, slotX + SKILL_SLOT_SIZE, slotY + SKILL_SLOT_SIZE,
                        SKILL_LOCKED_SHADE);
            } else if (cooling > 0F) {
                // Drains from the top, so a nearly-clear square reads as nearly ready.
                int shaded = Math.round(SKILL_SLOT_SIZE * cooling);
                guiGraphics.fill(slotX, slotY, slotX + SKILL_SLOT_SIZE, slotY + shaded, SKILL_COOLDOWN_SHADE);
            }

            drawSlotKey(guiGraphics, minecraft, StandSkillInput.slotKeyLabel(row).getString(), slotX, slotY);
        }
    }

    /** The bound key, in the corner, over a backing chip so it stays readable on top of the name. */
    private static void drawSlotKey(GuiGraphics guiGraphics, Minecraft minecraft, String key,
                                    int slotX, int slotY) {
        if (key.isEmpty()) {
            return;
        }

        int width = Math.round(minecraft.font.width(key) * SKILL_KEY_SCALE);
        int height = Math.round(minecraft.font.lineHeight * SKILL_KEY_SCALE);
        // Overhangs the square by a pixel, which is what stops it reading as part of the name.
        int keyX = slotX + SKILL_SLOT_SIZE - width;
        int keyY = slotY + SKILL_SLOT_SIZE - height;

        guiGraphics.fill(keyX - 1, keyY - 1, keyX + width + 1, keyY + height, SKILL_KEY_BACKING);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(keyX, keyY, 0F);
        guiGraphics.pose().scale(SKILL_KEY_SCALE, SKILL_KEY_SCALE, 1F);
        guiGraphics.drawString(minecraft.font, key, 0, 0, SKILL_KEY_COLOR, false);
        guiGraphics.pose().popPose();
    }

    /** 1 at the moment of use, easing to 0 as the cooldown expires; 0 when ready. */
    private static float cooldownFraction(JojohaPlayerData data, StandSkill skill, long now) {
        Long expiry = data.moveCooldowns.get(skill.id());
        if (expiry == null || expiry <= now) {
            return 0F;
        }

        int total = Math.max(1, StandSkills.scaledCooldown(skill, data));
        return Math.min(1F, (expiry - now) / (float) total);
    }

    /**
     * The Stand portrait in the corner of the bar.
     *
     * <p>The entity, its clock and the reasoning about both moved to {@link StandPortrait} when the
     * player menu needed the same picture - two preview Stands would have shared one animation
     * clock between them without either knowing, which is the failure that class exists to
     * document.
     */
    private static void renderStandPortrait(GuiGraphics guiGraphics, JojohaPlayerData data, int slotX, int slotY) {
        // Drawn straight onto the screen with no pose of its own, so the slot's own coordinates
        // are already screen coordinates and the scissor can use them directly.
        guiGraphics.enableScissor(slotX, slotY,
                slotX + STAND_BACKGROUND_SIZE, slotY + STAND_BACKGROUND_SIZE);
        StandPortrait.render(guiGraphics, data, slotX, slotY,
                STAND_BACKGROUND_SIZE, STAND_BACKGROUND_SIZE,
                PORTRAIT_SCALE, PORTRAIT_MODEL_ANCHOR_Y, StandPortrait.ROTATION);
        guiGraphics.disableScissor();
    }

    /**
     * Two-layer partial-fill technique (the same one vanilla uses for the XP/food bars): frame 1
     * (empty) draws full width as a backdrop, frame 0 (full) draws on top cropped to the current
     * fraction, so the bar visually fills/drains left-to-right.
     */
    private static void drawEnergyBar(GuiGraphics guiGraphics, ResourceLocation texture, int x, int y,
                                       int frameW, int frameH, float fraction) {
        int sheetW = frameW * 2;
        guiGraphics.blit(texture, x, y, (float) frameW, 0F, frameW, frameH, sheetW, frameH);

        int filled = Math.round(frameW * Math.max(0F, Math.min(1F, fraction)));
        if (filled > 0) {
            guiGraphics.blit(texture, x, y, 0F, 0F, filled, frameH, sheetW, frameH);
        }
    }

    private static void ensureFiltered() {
        if (texturesFiltered) {
            return;
        }
        texturesFiltered = true;
        Minecraft mc = Minecraft.getInstance();
        for (ResourceLocation texture : new ResourceLocation[] {BAR, PAGE, SPEC_BAR, STAND_BAR, STAND_STATES, STAND_BACKGROUND}) {
            mc.getTextureManager().getTexture(texture).setFilter(false, false);
        }
    }

    private static ResourceLocation hud(String fileName) {
        return ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/gui/hud/" + fileName);
    }
}
