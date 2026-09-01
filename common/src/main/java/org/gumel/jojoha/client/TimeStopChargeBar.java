package org.gumel.jojoha.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.ClientPlayerDataCache;
import org.gumel.jojoha.stand.skill.moves.TimeStopSkill;

/**
 * The wind-up, drawn: a clock with the charge running out from it both ways.
 *
 * <p>Time stop is the one move in the kit whose strength you choose while casting it, and until now
 * the only ways to tell how far in you were were a camera tremble and how white the Stand had gone.
 * Both are atmosphere. This is the number.
 *
 * <p>The fill grows <em>outward from the centre</em>, in both directions at once, because that is
 * what the artwork asks for: the clock sits in the middle of the frame with chevrons running away
 * from it to either edge, and the overlay is those chevrons alone, cut into a left-pointing bank and
 * a right-pointing one with a gap where the clock is. Filling it left-to-right like an ordinary
 * progress bar would run half the chevrons backwards.
 *
 * <p>Every span below was measured out of the overlay rather than guessed - the two banks are
 * columns 4 to 85 and 106 to 187, exactly 82 wide each, on rows 10 to 14.
 */
public final class TimeStopChargeBar {
    private static ResourceLocation hud(String name) {
        return ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/gui/hud/" + name);
    }

    private static final ResourceLocation FRAME = hud("timestop_charge_bar.png");
    private static final ResourceLocation FILL = hud("timestop_charge_bar_overlay.png");

    private static final int TEX_W = 192, TEX_H = 22;

    /**
     * The two banks of chevrons, and how long each is.
     *
     * <p>The left one is filled from its right end backwards and the right one from its left end
     * forwards, so both grow away from the clock between them.
     */
    private static final int LEFT_BANK_END = 85;
    private static final int RIGHT_BANK_START = 106;
    private static final int BANK_W = 82;

    /**
     * How wide a slice of the fill gets one colour, in pixels.
     *
     * <p>The spectrum has to be laid across the bar somehow, and the only way to give a blit more
     * than one colour is to cut it up. Two pixels is fine enough that the bands are not visible as
     * bands and coarse enough that a full bar is about eighty draws rather than a hundred and
     * sixty - which is nothing for a HUD, but there is no reason to spend it.
     */
    private static final int SLICE_W = 2;

    /** Turns of hue per second, and how much of the spectrum one bank covers at any instant. */
    private static final float HUE_SPEED = 0.32F;
    private static final float HUE_SPREAD = 0.7F;

    /**
     * How saturated the spectrum runs.
     *
     * <p>Short of full. The overlay is drawn white, and a white sprite taken to fully saturated hues
     * gives you the primaries at their harshest - which against a gold frame reads as a fault rather
     * than an effect. Backing off leaves the colour unmistakable and the bar still legible.
     */
    private static final float SATURATION = 0.82F;

    /**
     * How far above the bottom of the screen the bar floats.
     *
     * <p>Clear of the hotbar and the status rows above it. The combat bar's own lift is added on
     * top, so when that is open the charge bar rises with everything else instead of being buried
     * under it - it asks the same question the hearts do rather than a similar one, which is what
     * keeps the two from drifting apart later.
     */
    private static final int BOTTOM_OFFSET = 68;

    /** How far above the frame the readout sits, and what colour it is drawn in. */
    private static final int LABEL_GAP = 4;
    private static final int LABEL_RGB = 0xF2D68A;

    /** Below this the readout is dropped rather than drawn nearly invisible. */
    private static final float LABEL_MIN_ALPHA = 0.06F;

    private TimeStopChargeBar() {
    }

    public static void render(GuiGraphics graphics) {
        float alpha;
        float progress;

        if (TimeStopCharge.charging()) {
            alpha = 1F;
            progress = TimeStopCharge.charge();
        } else if (TimeStopCharge.dispelling()) {
            // Fades out at the width it reached rather than collapsing back to nothing, so the last
            // thing on screen is how hard the stop you just threw actually was.
            alpha = TimeStopCharge.dispelFade();
            progress = TimeStopCharge.releasedCharge();
        } else {
            return;
        }

        int x = (graphics.guiWidth() - TEX_W) / 2;
        int y = graphics.guiHeight() - BOTTOM_OFFSET - CentralBarOverlay.statusLift();

        // Blending is not on by default for a plain blit, and without it the fade-out would pop.
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        graphics.setColor(1F, 1F, 1F, alpha);
        graphics.blit(FRAME, x, y, 0, 0, TEX_W, TEX_H, TEX_W, TEX_H);

        int filled = Math.round(BANK_W * Mth.clamp(progress, 0F, 1F));
        if (filled > 0) {
            // Off the wall clock rather than the game clock: the spectrum should keep turning at the
            // same rate whatever is happening to the tick rate - and this is a move that stops it.
            float seconds = (Util.getMillis() % 1000000L) / 1000F;
            bank(graphics, x, y, filled, seconds, alpha, false);
            bank(graphics, x, y, filled, seconds, alpha, true);
        }

        graphics.setColor(1F, 1F, 1F, 1F);
        RenderSystem.disableBlend();

        label(graphics, x, y, progress, alpha);
    }

    /**
     * How long a stop this hold has bought so far, in seconds, over the middle of the bar.
     *
     * <p>The bar says how far through the wind-up you are; this says what you are actually getting
     * for it, which is the question being asked. They are not the same question and neither answers
     * the other - a meter at sixty percent tells you nothing about whether that is four seconds or
     * fourteen, and how long the world stops for is the entire decision being made.
     *
     * <p>Asked of {@link TimeStopSkill} rather than worked out here, so the number on screen is
     * produced by the same method the server will use when the key comes up. A readout with its own
     * copy of the arithmetic is a readout that will eventually lie.
     */
    private static void label(GuiGraphics graphics, int x, int y, float progress, float alpha) {
        if (alpha < LABEL_MIN_ALPHA) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        int chargeTicks = Math.round(progress * TimeStopSkill.INSTANCE.chargeMaxTicks());
        int stopTicks = TimeStopSkill.durationTicks(ClientPlayerDataCache.data, chargeTicks);

        Component text = Component.literal(String.format("%.1fs", stopTicks / 20F));
        int width = minecraft.font.width(text);

        int argb = (Mth.clamp(Math.round(alpha * 255F), 0, 255) << 24) | LABEL_RGB;
        graphics.drawString(minecraft.font, text,
                x + TEX_W / 2 - width / 2, y - minecraft.font.lineHeight - LABEL_GAP, argb, true);
    }

    /**
     * One side of the fill, cut into slices and coloured across the spectrum.
     *
     * <p>Hue is taken from how far out the slice is as well as from the clock, so the colour travels
     * along the bar instead of the whole thing flashing as one. Measured outward from the centre in
     * both directions, which means the two banks mirror each other rather than one running the
     * gradient backwards.
     */
    private static void bank(GuiGraphics graphics, int x, int y, int filled, float seconds,
                             float alpha, boolean rightward) {
        for (int done = 0; done < filled; done += SLICE_W) {
            int width = Math.min(SLICE_W, filled - done);

            // Where in the texture this slice lives. The left bank is walked backwards from its
            // inner end, so `done` counts outward on both sides and the mirroring is free.
            int u = rightward
                    ? RIGHT_BANK_START + done
                    : LEFT_BANK_END + 1 - done - width;

            float outward = (done + width * 0.5F) / BANK_W;
            float hue = seconds * HUE_SPEED + outward * HUE_SPREAD;
            int rgb = Mth.hsvToRgb(hue - Mth.floor(hue), SATURATION, 1F);

            graphics.setColor(
                    ((rgb >> 16) & 0xFF) / 255F,
                    ((rgb >> 8) & 0xFF) / 255F,
                    (rgb & 0xFF) / 255F,
                    alpha);
            graphics.blit(FILL, x + u, y, u, 0, width, TEX_H, TEX_W, TEX_H);
        }

        // Put back. setColor is RenderSystem.setShaderColor underneath, which is one global for the
        // whole frame rather than anything belonging to this bar - so a hue left set here was still
        // set when the interface drew afterwards, and tinted every panel of it. The bar was only
        // ever visibly wrong for as long as it was on screen; everything drawn after it was wrong
        // too, and that is the half nobody thinks to check.
        graphics.setColor(1F, 1F, 1F, 1F);
    }
}
