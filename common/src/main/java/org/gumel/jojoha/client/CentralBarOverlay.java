package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.ClientPlayerDataCache;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerSpec;
import org.gumel.jojoha.data.VampireStage;
import org.gumel.jojoha.stand.StandMode;
import org.gumel.jojoha.stand.skill.StandSkill;
import org.gumel.jojoha.stand.skill.StandSkills;


/**
 * The combat bar, drawn from the central artwork.
 *
 * <p>Every coordinate below was measured out of {@code combat_bar_central.png} rather than guessed,
 * which is the only way this holds together: the frame is drawn with the slots and gauges cut out of
 * it as transparent holes, so anything laid into one has to land on exactly the right pixel or the
 * gold shows through one edge and covers the other. The measurements agree with the artwork exactly
 * - the gauge wells are 76 by 6, which is the size of the gauge textures, and the eye recess is 10
 * by 10, which is the size of the eye textures.
 *
 * <p>Layered: frame, the two gauges, the page marker in whichever side well is live, then the
 * Stand's state and its eyes, then the move wells.
 */
public final class CentralBarOverlay {
    private static ResourceLocation hud(String name) {
        return ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/gui/hud/" + name);
    }

    private static final ResourceLocation FRAME = hud("combat_bar_central.png");
    private static final ResourceLocation PAGE_BAR = hud("central_page_bar.png");
    private static final ResourceLocation TRACK_LEFT = hud("central_spec_bar.png");
    private static final ResourceLocation TRACK_RIGHT = hud("central_hamon_bar.png");
    private static final ResourceLocation FILL_HAMON = hud("central_hamon_bar_filled.png");
    private static final ResourceLocation FILL_VAMPIRISM = hud("central_vampirism_bar_filled.png");
    private static final ResourceLocation FILL_STAND = hud("central_stand_bar_filled.png");
    private static final ResourceLocation STATE_FULL = hud("stand_state_full.png");
    private static final ResourceLocation STATE_UNSUMMONED = hud("stand_state_unsummoned.png");
    private static final ResourceLocation EYES_ATTACK = hud("stand_attack_eyes.png");
    private static final ResourceLocation EYES_DEFENSE = hud("stand_defense_eyes.png");
    /**
     * Green, for the stance where the Stand is working rather than watching.
     *
     * <p>The same six lit pixels as the other two - measured, four of the pale shade and two of the
     * darker - so it drops into the same recess without any of the surrounding art changing. Green
     * because the other two are already spoken for by the two halves of a fight, and the whole
     * point of this stance is that it is not one.
     */
    private static final ResourceLocation EYES_UTILITY = hud("stand_utility_eyes.png");

    private static final int FRAME_W = 222, FRAME_H = 61;

    /** How far the bar sits above the bottom of the screen. */
    private static final int BOTTOM_MARGIN = 0;

    /** The eight move wells: 19 by 19, starting at 28 and every 21 across, all on one row. */
    private static final int SLOT_X = 28, SLOT_Y = 17;
    private static final int SLOT_SIZE = 19, SLOT_PITCH = 21;
    private static final int SLOTS = 8;

    /** The two thin wells the page marker goes into, one either side of the row of slots. */
    private static final int PAGE_LEFT_X = 21, PAGE_RIGHT_X = 197;
    private static final int PAGE_Y = 17, PAGE_W = 4, PAGE_H = 19;

    /**
     * The two gauge wells across the top, each exactly the size of its texture.
     *
     * <p>Which artwork belongs in which well is decided by the slant, not by the filename. The wells
     * are parallelograms leaning opposite ways, and every one of these textures is cut for one lean
     * or the other, so each has exactly one well it can sit in without going against the grain of its
     * own hole:
     *
     * <pre>
     *   left  well  4-75 .. 0-70   central_spec_bar (track), hamon and vampirism fills
     *   right well  0-71 .. 5-75   central_hamon_bar (track), stand fill
     * </pre>
     *
     * <p>That settles a question that had been guesswork: hamon belongs on the left and stand energy
     * on the right. The track filenames happen to read the other way round, which is why the sides
     * were swapped once already - the fills, which arrived later, are unambiguous.
     *
     * <p>Each gauge is a grey track at full width with the coloured fill cropped over it, so an empty
     * gauge is still a visible groove rather than a hole in the frame.
     */
    private static final int GAUGE_W = 76, GAUGE_H = 6, GAUGE_Y = 7;
    private static final int GAUGE_LEFT_X = 7, GAUGE_RIGHT_X = 139;

    /**
     * The figure and its eyes.
     *
     * <p>The eyes are a separate 10 by 10 sprite laid over the head, which is itself 10 wide and
     * sits 15 across in the 40 wide figure - so the two line up by construction rather than by being
     * nudged into place.
     */
    private static final int STATE_X = 91, STATE_Y = 0;
    private static final int STATE_W = 40, STATE_H = 12;
    private static final int EYES_OFFSET = 15, EYES_SIZE = 10;

    /**
     * How far the summoned figure drops relative to the unsummoned one.
     *
     * <p>Applied by eye, and worth saying that it is: the two sprites were measured against each
     * other row by row and their alpha silhouettes are identical - same pixel counts, same spans,
     * every row of twelve - so on the artwork alone there is nothing between them to correct, and
     * both were already drawn from the same {@link #STATE_Y}. What differs between the files is
     * only colour: one is a pale figure with eyes in it, the other the same shape in near-black.
     *
     * <p>Which points at the likely cause. The unsummoned figure's top row is dark against a dark
     * frame and the summoned one's is pale, so the eye finds the top edge of one a row earlier than
     * the other even though the pixels start together. That is a real thing to see and worth
     * correcting for, but it is an optical alignment, not a geometric one - so this is deliberately
     * its own number rather than a change to {@code STATE_Y}, which would move both together and
     * fix nothing.
     */
    private static final int STATE_SUMMONED_Y_OFFSET = 1;

    /**
     * How small the key is drawn, as a share of the font's natural size.
     *
     * <p>Three quarters rather than three fifths. The badge is the one thing on the bar that has to
     * be read rather than recognised - the icon already says which move it is - so it is worth the
     * few extra pixels. "[Z]", the widest label there is, comes to about fourteen of the well's
     * nineteen at this size, so it still sits in the corner rather than across the whole icon.
     */
    private static final float KEY_SCALE = 0.75F;

    /**
     * White, whatever state the move is in.
     *
     * <p>It used to take the move's own colour, which meant a locked move's key was drawn in a dim
     * purple over an already dimmed icon and became nearly impossible to pick out. State is the
     * icon's job - it is shaded for cooling and for locked - and it is legible there. The key is a
     * label, and a label that fades exactly when you are hunting for it is the wrong trade.
     */
    private static final int KEY_COLOUR = 0xFFFFFFFF;

    private static final int SHADE_COOLING = 0xB0101010;
    private static final int SHADE_LOCKED = 0x90201020;

    /**
     * How far down an icon is drawn when it cannot be used, as a multiplier on its own colour.
     *
     * <p>Dark enough to read as unavailable at a glance, light enough that the picture is still
     * legible - the player has to be able to see which move it is they are waiting on. The locked
     * tint keeps a little more blue and red than green, which is what carried the purple cast the
     * old shaded square had.
     */
    private static final float COOLING_TINT = 0.34F;
    private static final float[] LOCKED_TINT = {0.40F, 0.30F, 0.44F};

    /** The vanilla hotbar's height, which is what the status bars normally sit on top of. */
    private static final int HOTBAR_H = 22;

    /**
     * How much of the lift to give back, so the status rows sit closer to the bar than the geometry
     * alone would put them.
     *
     * <p>Clearing the bar exactly leaves a gap the width of the bar's own top border, which reads as
     * the rows having drifted away from it rather than resting on it. They overlap the frame's upper
     * moulding by this much, where there is nothing drawn to collide with.
     */
    private static final int STATUS_SETTLE = 8;

    /** How long the bar takes to slide in or out, in seconds. */
    private static final float REVEAL_SECONDS = 0.22F;

    /**
     * How far in the bar is, 0 to 1.
     *
     * <p>Advanced per frame off the wall clock rather than per tick, because it is an animation and
     * twenty samples a second across a fifth of a second is four frames of motion - which reads as a
     * stutter, not a slide.
     */
    private static float reveal;
    private static long lastFrameNanos;

    /**
     * How fast a gauge chases the number it has been told, per second.
     *
     * <p>The drain itself is per tick and perfectly smooth. What was not smooth was the client's
     * knowledge of it: the server syncs the pools on an interval, so the bar redrew the same width
     * for every frame in between and then stepped. Easing toward the last figure received turns
     * each step into a slide, and because the steps arrive faster than the eye can resolve the
     * easing, what you see is a bar that simply falls.
     *
     * <p>Chosen against the sync interval rather than to taste: a little over half a second to
     * close a gap, against a figure arriving every half second, so the gauge is always still moving
     * when the next one lands and never sits waiting for it.
     */
    private static final float GAUGE_EASE_PER_SECOND = 4.5F;

    /**
     * A jump larger than this is taken instantly rather than slid to.
     *
     * <p>Easing is for watching a pool drain. A respawn, a command, a Stand being dismissed or the
     * bar being opened after being away are all step changes to a different situation, and sliding
     * through them would draw a drain that never happened.
     */
    private static final float GAUGE_SNAP = 0.4F;

    /** What the two gauges are currently drawing, as opposed to what the data says. */
    private static float shownSpec = -1F;
    private static float shownStand = -1F;

    /**
     * How far the left gauge has turned, 0 to 1.
     *
     * <p>Becoming a vampire is not a state the bar should simply be found in - it is something that
     * happens to the player, and the bar is the only place they can watch it happen to them. So the
     * red is faded in over the old fill rather than swapped for it, and the length below is chosen
     * to sit inside the transformation rather than outrun it.
     */
    private static float vampireBlend;

    /** How long the left gauge takes to turn, in seconds. */
    private static final float VAMPIRE_BLEND_SECONDS = 2.2F;

    private CentralBarOverlay() {
    }

    /**
     * How far vanilla's hearts, armour and hunger have to move up to clear this bar, in pixels.
     *
     * <p>Zero whenever the bar is not being drawn, and it asks the same question the drawing does
     * rather than a similar one - two conditions that agree today and drift apart later is how you
     * end up with hearts floating in the middle of the screen with nothing under them.
     */
    public static int statusLift() {
        // Scaled by the same reveal the bar slides on, so the hearts travel with it instead of
        // jumping to their new home while the bar is still on its way there.
        return Math.round((FRAME_H + BOTTOM_MARGIN - HOTBAR_H - STATUS_SETTLE) * eased());
    }

    /** True while anything of the bar is on screen, which outlasts the toggle by the slide. */
    public static boolean showing() {
        return reveal > 0.001F;
    }

    /**
     * Whether the bar should be heading in. The toggle, plus the reasons it would be pointless.
     *
     * <p>Having a Stand is deliberately not one of those reasons any more. The bar carries the
     * hamon and spec gauges, the vampirism track and the move slots, none of which are Stand
     * features - gating the whole thing on {@code stand.isPresent()} meant a Hamon user or a
     * vampire had no combat interface at all, and a new player had no way to see that one existed.
     * The pieces that genuinely need a Stand already say so themselves: the figure falls back to
     * its unsummoned sprite and the slots to whatever the player has actually unlocked.
     */
    private static boolean wanted() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null
                // Spectators are watching, not fighting. Vanilla takes their hotbar and health away
                // for the same reason, and a combat bar offering moves to somebody who cannot use
                // one is worse than no bar - it reads as the game being broken rather than as the
                // mode being what it is.
                && !minecraft.player.isSpectator()
                && CombatBarOverlay.isVisible()
                && !(minecraft.screen instanceof PlayerMenuScreen);
    }

    /** Smoothstepped, so it leaves and arrives at rest rather than starting at full speed. */
    private static float eased() {
        return reveal * reveal * (3F - 2F * reveal);
    }

    /** Call from the HUD render. */
    public static void render(GuiGraphics graphics) {
        advance();

        if (!showing()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        JojohaPlayerData data = ClientPlayerDataCache.data;

        int x = (graphics.guiWidth() - FRAME_W) / 2;
        int y = graphics.guiHeight() - FRAME_H - BOTTOM_MARGIN;

        // Slid down out of the bottom of the screen rather than faded. A fade would need every piece
        // of this to carry an alpha - the blits, the two gauges, and the text, which does not take a
        // tint at all - whereas one translation moves the lot and reads as the bar being put away.
        // Everything already queued goes down before any of this does.
        //
        // GuiGraphics batches some draws and performs others immediately, and an item in a slot is
        // one of the batched ones - the hotbar hands its stacks to a buffer source that is flushed
        // later, while a plain blit is uploaded and drawn on the spot. Left alone, an item still
        // sitting in that batch lands on top of the bar rather than under it, which is why the icons
        // only misbehaved when something was in hand. Draining it first puts the two back in the
        // order they were issued.
        graphics.flush();

        int slide = Math.round((1F - eased()) * (FRAME_H + BOTTOM_MARGIN));
        graphics.pose().pushPose();
        graphics.pose().translate(0F, slide, 0F);

        graphics.blit(FRAME, x, y, 0, 0, FRAME_W, FRAME_H, FRAME_W, FRAME_H);

        // The left gauge, in whichever of three states applies.
        //
        // Specless is the one that used to be wrong: the bar drew a full Hamon gauge for a player
        // who had never learned any, so everyone started the game apparently brimming with a power
        // they did not have. With no spec there is nothing to fill it with, and the empty groove is
        // the honest picture - black, until something puts colour in it.
        //
        // The turn is a fade rather than a swap, so the moment the blood takes is something the
        // player watches happen on their own bar. Whatever was there goes out underneath as the red
        // comes up over it.
        graphics.blit(TRACK_LEFT, x + GAUGE_LEFT_X, y + GAUGE_Y, 0, 0,
                GAUGE_W, GAUGE_H, GAUGE_W, GAUGE_H);

        if (data.spec == PlayerSpec.HAMON && vampireBlend < 1F) {
            fill(graphics, FILL_HAMON, x + GAUGE_LEFT_X, y + GAUGE_Y, shownSpec, 1F - vampireBlend);
        }
        if (vampireBlend > 0F) {
            fill(graphics, FILL_VAMPIRISM, x + GAUGE_LEFT_X, y + GAUGE_Y, shownSpec, vampireBlend);
        }
        gauge(graphics, TRACK_RIGHT, FILL_STAND, x + GAUGE_RIGHT_X, y + GAUGE_Y, shownStand);

        // The marker sits in the well on the side of the page that is up, so which page you are on
        // is read off which end of the bar is lit rather than off a number.
        int pageX = CombatBarOverlay.currentSkillPage() == 0 ? PAGE_LEFT_X : PAGE_RIGHT_X;
        graphics.blit(PAGE_BAR, x + pageX, y + PAGE_Y, 0, 0, PAGE_W, PAGE_H, PAGE_W, PAGE_H);

        renderState(graphics, data, x, y);
        renderSlots(graphics, data, x, y, minecraft);

        // And ours is down before anything after it starts, for the same reason in reverse.
        graphics.flush();
        graphics.pose().popPose();
    }

    /**
     * Moves the slide along by however long the last frame took.
     *
     * <p>Clamped, so a stutter or a pause on a loading screen cannot hand it a delta measured in
     * seconds and snap the bar to its destination - the animation exists to be watched.
     */
    private static void advance() {
        long now = System.nanoTime();
        float delta = lastFrameNanos == 0L ? 0F : (now - lastFrameNanos) / 1.0E9F;
        lastFrameNanos = now;

        // Capped so a stutter or a paused window does not resolve into one enormous step.
        float step = Math.min(delta, 0.1F);

        reveal = Mth.approach(reveal, wanted() ? 1F : 0F, step / REVEAL_SECONDS);

        // Advanced whether or not the bar is on screen, so opening it shows where the pools
        // actually are rather than animating up from wherever they were when it was last closed.
        JojohaPlayerData data = ClientPlayerDataCache.data;
        shownSpec = easeGauge(shownSpec, data.specEnergy / JojohaPlayerData.MAX_SPEC_ENERGY, step);
        shownStand = easeGauge(shownStand, data.standEnergy / data.maxStandEnergy(), step);

        // Forward only in practice, but written both ways so that reverting the spec - which the
        // debug command can do - puts the bar back rather than leaving it stuck red.
        float target = data.vampireStage == VampireStage.NONE ? 0F : 1F;
        vampireBlend = Mth.approach(vampireBlend, target, step / VAMPIRE_BLEND_SECONDS);
    }

    /** One gauge, chasing its figure - see GAUGE_EASE_PER_SECOND. */
    private static float easeGauge(float shown, float target, float step) {
        target = Mth.clamp(target, 0F, 1F);

        // Negative means nothing has been drawn yet, so there is nothing to ease from.
        if (shown < 0F || Math.abs(target - shown) > GAUGE_SNAP) {
            return target;
        }

        return shown + (target - shown) * (1F - (float) Math.exp(-GAUGE_EASE_PER_SECOND * step));
    }

    /**
     * One gauge, filled from the left.
     *
     * <p>The track is drawn whole and the fill cropped over it.
     */
    /**
     * One coloured fill over an already-drawn track, at a given opacity.
     *
     * <p>Split out from {@link #gauge} because a cross-fade needs the two halves separately - the
     * track drawn once, and each fill laid over it at its own alpha. Blending has to be turned on
     * by hand: a plain blit does not enable it, so without this the fading fill would pop in at
     * full strength instead of arriving.
     */
    private static void fill(GuiGraphics graphics, ResourceLocation fill, int x, int y,
                            float amount, float alpha) {
        int width = Math.round(GAUGE_W * Mth.clamp(amount, 0F, 1F));
        if (width <= 0 || alpha <= 0.004F) {
            return;
        }

        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        graphics.setColor(1F, 1F, 1F, Mth.clamp(alpha, 0F, 1F));

        graphics.blit(fill, x, y, 0, 0, width, GAUGE_H, GAUGE_W, GAUGE_H);

        graphics.setColor(1F, 1F, 1F, 1F);
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
    }

    private static void gauge(GuiGraphics graphics, ResourceLocation track, ResourceLocation fill,
                              int x, int y, float amount) {
        graphics.blit(track, x, y, 0, 0, GAUGE_W, GAUGE_H, GAUGE_W, GAUGE_H);

        int width = Math.round(GAUGE_W * Mth.clamp(amount, 0F, 1F));
        if (width <= 0) {
            return;
        }

        // Cropped rather than scaled: the artwork is a slanted band, and squeezing it would change
        // the angle of the slant as the value moved.
        graphics.blit(fill, x, y, 0, 0, width, GAUGE_H, GAUGE_W, GAUGE_H);
    }

    /** The figure in the middle, and the colour of its eyes. */
    private static void renderState(GuiGraphics graphics, JojohaPlayerData data, int x, int y) {
        // No Stand out, no eyes. The figure has an attack colour and a defence colour and nothing
        // else, so leaving one of them lit while the Stand is away would be claiming a stance for
        // something that is not there. The grey figure says "nothing is out" on its own, which is
        // the whole reason there is a third state sprite.
        if (!data.standSummoned) {
            graphics.blit(STATE_UNSUMMONED, x + STATE_X, y + STATE_Y, 0, 0,
                    STATE_W, STATE_H, STATE_W, STATE_H);
            return;
        }

        int stateY = y + STATE_Y + STATE_SUMMONED_Y_OFFSET;
        graphics.blit(STATE_FULL, x + STATE_X, stateY, 0, 0, STATE_W, STATE_H, STATE_W, STATE_H);

        // The eyes travel with the head they sit in. Nudging the figure and leaving these behind
        // would trade a one pixel misalignment for a worse one, on the part of the sprite the eye
        // is actually looking at.
        ResourceLocation eyes = switch (data.standMode) {
            case DEFENSE -> EYES_DEFENSE;
            case UTILITY -> EYES_UTILITY;
            default -> EYES_ATTACK;
        };
        graphics.blit(eyes, x + STATE_X + EYES_OFFSET, stateY, 0, 0,
                EYES_SIZE, EYES_SIZE, EYES_SIZE, EYES_SIZE);
    }

    /**
     * The eight move wells: the move's icon filling the well, its key in the corner.
     *
     * <p>The name used to be written across the well, wrapped and shrunk to well under half size,
     * because nineteen pixels is about three characters at full size. It was never really readable -
     * an icon says the same thing at a glance and at the size the well actually is.
     *
     * <p>Art is 19x19 and the well is 19, so it is drawn at its own size. A move that is locked or
     * cooling is its own icon <em>drawn darker</em> - the tint goes on the blit rather than a shaded
     * square going on top of it.
     *
     * <p>That distinction is the whole of a bug worth remembering. A square of near-black laid over
     * the well covers the icon and the gap around it alike, so any pixel the art does not reach
     * shows the square at full strength - which reads as a black bar biting into the icon rather
     * than as the icon being dimmed. Tinting cannot do that, because there is nothing being drawn
     * except the icon: transparent stays transparent, and the frame behind shows through as it does
     * when the move is ready.
     *
     * <p>An empty slot keeps its key badge. The bar is a map of the keys, and a key with nothing on
     * it yet is worth showing as available rather than leaving blank.
     */
    private static void renderSlots(GuiGraphics graphics, JojohaPlayerData data,
                                    int x, int y, Minecraft minecraft) {
        long now = minecraft.level == null ? 0L : minecraft.level.getGameTime();
        int pageOffset = CombatBarOverlay.currentSkillPage() * StandSkills.SLOTS_PER_PAGE;

        for (int i = 0; i < SLOTS; i++) {
            int slotX = x + SLOT_X + i * SLOT_PITCH;
            int slotY = y + SLOT_Y;

            StandSkill skill = StandSkills.skillInSlot(data, pageOffset + i);
            if (skill == null) {
                drawKey(graphics, minecraft, StandSkillInput.slotKeyLabel(i), slotX, slotY);
                continue;
            }

            boolean locked = !skill.isUnlocked(data);

            // Timed off a clock that cannot be corrected out from under it - see SkillCooldownView.
            boolean cooling = !locked
                    && SkillCooldownView.cooling(data, skill.id(), now, minecraft.level);

            ResourceLocation icon = SkillIcons.of(skill.id());
            if (icon != null) {
                if (locked) {
                    graphics.setColor(LOCKED_TINT[0], LOCKED_TINT[1], LOCKED_TINT[2], 1F);
                } else if (cooling) {
                    graphics.setColor(COOLING_TINT, COOLING_TINT, COOLING_TINT, 1F);
                }

                graphics.blit(icon, slotX, slotY, 0F, 0F,
                        SLOT_SIZE, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE);

                // Put back every time rather than only when it was changed. A tint left set leaks
                // into whatever the HUD draws next, and the next thing is somebody else's code.
                graphics.setColor(1F, 1F, 1F, 1F);
            } else if (locked || cooling) {
                // No art for this move yet, so there is nothing to darken. The square is the
                // fallback rather than the rule, and an empty well has no icon for it to eat into.
                graphics.fill(slotX, slotY, slotX + SLOT_SIZE, slotY + SLOT_SIZE,
                        locked ? SHADE_LOCKED : SHADE_COOLING);
            }

            drawKey(graphics, minecraft, StandSkillInput.slotKeyLabel(i), slotX, slotY);
        }
    }

    /** The key, tucked into the bottom right of the well. */
    private static void drawKey(GuiGraphics graphics, Minecraft minecraft, Component key,
                                int slotX, int slotY) {
        if (key == null) {
            return;
        }

        float width = minecraft.font.width(key) * KEY_SCALE;
        float height = minecraft.font.lineHeight * KEY_SCALE;

        graphics.pose().pushPose();
        graphics.pose().scale(KEY_SCALE, KEY_SCALE, 1F);
        graphics.drawString(minecraft.font, key,
                Math.round((slotX + SLOT_SIZE - 1 - width) / KEY_SCALE),
                Math.round((slotY + SLOT_SIZE - 1 - height) / KEY_SCALE),
                KEY_COLOUR, true);
        graphics.pose().popPose();
    }
}
