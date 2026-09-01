package org.gumel.jojoha.client;

import net.minecraft.client.gui.GuiGraphics;

/**
 * The one white frame when Skull Crusher lands.
 *
 * <p>What used to be here was a vignette that closed in on the victim and dimmed everything else,
 * plus a cut to black with a skull turning red on it. Both are gone, and for the same reason: they
 * were shots. Darkening the edges of the screen and cutting away from the world are things a film
 * does to an audience, and the person throwing this punch is not an audience - they are still
 * holding the controls, still being shot at, still standing where they were.
 *
 * <p>What replaced them is {@link ImpactFramePost}, which drains the colour out of the world for
 * about a second without taking anything away from the player. This class keeps only the flash,
 * which is a single frame and reads as impact rather than as an edit.
 *
 * <p>Only the puncher sees it.
 */
public final class SkullCrushOverlay {
    /** How white the impact frame gets at its brightest. */
    private static final float FLASH_ALPHA = 0.78F;

    private SkullCrushOverlay() {
    }

    public static void render(GuiGraphics guiGraphics, int width, int height, float partialTick) {
        impactFrame(guiGraphics, width, height, partialTick);
    }

    /**
     * One white frame on contact.
     *
     * <p>A flat fill, deliberately. Anything cleverer - a radial burst, a shaped flare - would be a
     * picture the eye tries to read, and there is not enough time to read anything. What lands in
     * three ticks is a change in brightness and nothing else.
     */
    private static void impactFrame(GuiGraphics guiGraphics, int width, int height,
                                    float partialTick) {
        float flash = SkullFlashFx.flash(partialTick);
        if (flash <= 0.001F) {
            return;
        }

        int white = Math.round(Math.min(1F, flash) * FLASH_ALPHA * 255F) << 24 | 0xFFFFFF;
        guiGraphics.fill(0, 0, width, height, white);
    }

}
