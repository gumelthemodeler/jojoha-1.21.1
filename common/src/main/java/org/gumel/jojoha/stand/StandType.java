package org.gumel.jojoha.stand;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.List;

/**
 * Describes one Stand's GeckoLib assets - geometry, texture and animation JSON - plus the colour
 * it presents itself in.
 *
 * @param skins           every texture this Stand can wear, the default one first. A list rather
 *                        than a single texture because a Stand's appearance is not fixed - see
 *                        {@code FracturedSkinArrowItem} - and because the alternates have to be
 *                        enumerable to be rolled between. Index 0 is what a freshly awakened Stand
 *                        comes out wearing.
 * @param auraColor       packed RGB driving both the aura particles and the outline glow on the
 *                        user, so a Stand's presence reads the same whether you see the motes or
 *                        the silhouette
 * @param range           decides which generic moves this Stand is handed - see {@link StandRange}
 * @param signatureSkills the moves that are this Stand's own, appended after the generics to build
 *                        its slot bar. Only ids are held here so a Stand stays a pure description
 *                        of itself, with no compile-time dependency on the move implementations.
 * @param archetype       what kind of fight it is built for, which supplies two of its four
 *                        passives - see {@link StandArchetype}
 * @param passives        the standing effects that are this Stand's <em>own</em>, granted simply by
 *                        being manifested - see {@code StandPassive}. Its archetype's two are not
 *                        listed here; {@link #allPassives()} is what puts the four together. Ids for
 *                        the same reason as the moves.
 */
public record StandType(ResourceLocation id, ResourceLocation model, List<StandSkin> skins,
                        ResourceLocation animation, int auraColor,
                        StandRange range, StandArchetype archetype,
                        StandForm form,
                        java.util.function.Supplier<net.minecraft.sounds.SoundEvent> voice,
                        List<ResourceLocation> signatureSkills,
                        List<ResourceLocation> passives) {

    /**
     * The Stand's name being called as it manifests, or null if it does not announce itself.
     *
     * <p>This used to be Star Platinum's cry played for every Stand in the game, because there was
     * only ever one Stand and the sound was written where the summon happens rather than where the
     * Stand is described. Every Stand added after that inherited a shout of somebody else's name.
     *
     * <p>Null is the default and the honest one. The manifestation whoosh already plays for
     * everybody; a voice line on top is a thing a particular Stand does, and most do not.
     */

    /**
     * The old shape, for the Stands that are people.
     *
     * <p>Kept so that adding the concept of a form did not mean editing every Stand that already
     * existed to say the obvious thing about itself. A Stand registered without one is humanoid,
     * which is what all of them were before there was a choice.
     */
    public StandType(ResourceLocation id, ResourceLocation model, List<StandSkin> skins,
                     ResourceLocation animation, int auraColor,
                     StandRange range, StandArchetype archetype,
                     List<ResourceLocation> signatureSkills,
                     List<ResourceLocation> passives) {
        this(id, model, skins, animation, auraColor, range, archetype,
                StandForm.HUMANOID, null, signatureSkills, passives);
    }

    /** With a form but no voice, which is every Stand that does not shout its own name. */
    public StandType(ResourceLocation id, ResourceLocation model, List<StandSkin> skins,
                     ResourceLocation animation, int auraColor,
                     StandRange range, StandArchetype archetype, StandForm form,
                     List<ResourceLocation> signatureSkills,
                     List<ResourceLocation> passives) {
        this(id, model, skins, animation, auraColor, range, archetype, form, null,
                signatureSkills, passives);
    }

    /**
     * Everything this Stand grants: its archetype's two, then its own two.
     *
     * <p>Archetype first, because that is the order they are read in - what kind of thing this is,
     * and then what makes it itself.
     *
     * <p>Built here rather than being flattened into the record at registration, so the two stay
     * separable. Anything that wants to say "shared with every brawler" as against "Star Platinum's
     * own" can still tell them apart, and retuning an archetype does not mean editing every Stand
     * that wears it.
     */
    public List<ResourceLocation> allPassives() {
        if (archetype.passives().isEmpty()) {
            return passives;
        }

        List<ResourceLocation> all = new java.util.ArrayList<>(archetype.passives());
        all.addAll(passives);
        return List.copyOf(all);
    }

    /**
     * The default look - what this Stand wears until something changes it.
     *
     * <p>Kept as a method under the old name so nothing that only ever wanted "the texture" had to
     * learn about skins. The component itself is the list, because a default that lives outside the
     * list would be a special case every roll had to remember to include.
     */
    public ResourceLocation texture() {
        return skins.get(0).texture();
    }

    /**
     * The texture for a skin index, clamped rather than checked.
     *
     * <p>Clamped because the index comes off saved player data and outlives the asset list: a world
     * where somebody was wearing skin 4 opened against a build where that skin has been removed
     * should show them skin 3, not crash them out of it. An out-of-range skin is a content change,
     * not a corrupt save.
     */
    public ResourceLocation textureFor(int skin) {
        return skinAt(skin).texture();
    }

    /** What the reveal calls the skin at this index. */
    public String skinNameKey(int skin) {
        return skinAt(skin).translationKey();
    }

    /**
     * What colour this Stand reads as while wearing a given skin.
     *
     * <p>The aura, the outline on its user and the afterimages it leaves all come from here, so a
     * skin that changes it changes all three together - which is the point. A blue Star Platinum
     * trailing violet motes would look like two Stands wearing one body.
     *
     * <p>Falls back to the Stand's own colour, so a wardrobe only has to name the looks that
     * actually differ.
     */
    public int auraColorFor(int skin) {
        int own = skinAt(skin).auraColor();
        return own == StandSkin.INHERIT ? auraColor : own;
    }

    public float auraRedFor(int skin) {
        return ((auraColorFor(skin) >> 16) & 0xFF) / 255F;
    }

    public float auraGreenFor(int skin) {
        return ((auraColorFor(skin) >> 8) & 0xFF) / 255F;
    }

    public float auraBlueFor(int skin) {
        return (auraColorFor(skin) & 0xFF) / 255F;
    }

    /**
     * How likely the skin at this index is, relative to the others.
     *
     * <p>Read through the tuning system so a pack can retune rarity without touching the wardrobe -
     * the key is the skin's own name, so {@code jojoha:skin.jojoha.star_platinum.p6} is the weight
     * of that one skin. Falls back to whatever the wardrobe declared, which is one unless the author
     * said otherwise.
     */
    public float skinWeight(int skin) {
        StandSkin entry = skinAt(skin);
        return Math.max(0F, org.gumel.jojoha.data.StandTuning.value(
                entry.translationKey().replace('.', '_'), entry.weight()));
    }

    private StandSkin skinAt(int skin) {
        return skins.get(Mth.clamp(skin, 0, skins.size() - 1));
    }

    /** How many looks this Stand has, default included. */
    public int skinCount() {
        return skins.size();
    }

    public float auraRed() {
        return ((auraColor >> 16) & 0xFF) / 255F;
    }

    public float auraGreen() {
        return ((auraColor >> 8) & 0xFF) / 255F;
    }

    public float auraBlue() {
        return (auraColor & 0xFF) / 255F;
    }
}
