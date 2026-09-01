package org.gumel.jojoha.stand;

import net.minecraft.resources.ResourceLocation;

/**
 * One look a Stand can wear: the sheet, and what to call it.
 *
 * <p>A pair rather than a bare texture because the skin is announced out loud when it changes -
 * "STAR PLATINUM [P6]!" - and a name derived from the texture's filename would be one rename away
 * from reading {@code starplat_p6skin} at the player. Naming it here also means the label is
 * translatable, which a filename never could be.
 *
 * @param texture        the entity sheet, which must share the default's UV layout - the geometry
 *                       does not change with the skin
 * @param translationKey what the reveal calls it
 * @param weight         how often it comes up relative to the others; see {@link #DEFAULT_WEIGHT}
 * @param auraColor      what colour this look makes the Stand read as - its motes, the glow on its
 *                       user, its afterimages - or {@link #INHERIT} to keep the Stand's own
 */
public record StandSkin(ResourceLocation texture, String translationKey, float weight, int auraColor) {
    /** The ordinary case: as likely as every other skin, in the Stand's own colour. */
    public StandSkin(ResourceLocation texture, String translationKey) {
        this(texture, translationKey, DEFAULT_WEIGHT, INHERIT);
    }

    /** A skin that changes what colour the Stand reads as, without changing how often it comes up. */
    public StandSkin(ResourceLocation texture, String translationKey, int auraColor) {
        this(texture, translationKey, DEFAULT_WEIGHT, auraColor);
    }

    /**
     * What an unremarkable skin is worth.
     *
     * <p>One, and every skin gets it unless something says otherwise - so out of the box the roll is
     * flat and no look is rarer than another. Weights are a relative measure, not a percentage:
     * doubling one is the same as halving all the others, and the roll normalises whatever it is
     * handed. That is what makes a single skin adjustable without recomputing the rest.
     */
    public static final float DEFAULT_WEIGHT = 1.0F;

    /**
     * What a skin says when it has no opinion about colour: use the Stand's own.
     *
     * <p>A sentinel rather than a copy of the Stand's value, so a skin that never meant to change
     * the colour cannot quietly pin it - if the Stand's own violet is ever retuned, every skin that
     * inherits follows it, and only the ones that deliberately said otherwise stay put.
     *
     * <p>Negative because a packed RGB never is, so there is no colour it could be mistaken for.
     */
    public static final int INHERIT = -1;
}
