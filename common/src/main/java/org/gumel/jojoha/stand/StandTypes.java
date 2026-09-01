package org.gumel.jojoha.stand;

import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

import org.gumel.jojoha.stand.passive.SensoryPerceptionPassive;
import org.gumel.jojoha.stand.passive.TwoMetersPassive;
import org.gumel.jojoha.stand.skill.moves.InhaleSkill;
import org.gumel.jojoha.stand.skill.moves.StarFingerSkill;
import org.gumel.jojoha.stand.skill.moves.TimeStopSkill;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Registry of known Stands' GeckoLib assets, keyed by the same id stored in StandData. */
public final class StandTypes {
    private static final Map<ResourceLocation, StandType> TYPES = new LinkedHashMap<>();

    public static final ResourceLocation STAR_PLATINUM_ID = asset("star_platinum");
    public static final ResourceLocation HERMIT_PURPLE_ID = asset("hermit_purple");

    private StandTypes() {
    }

    /** Star Platinum's violet - the colour of both its aura motes and the glow on its user. */
    private static final int STAR_PLATINUM_COLOR = 0xB35CFF;

    /**
     * What each alternate look does to that colour.
     *
     * <p>Taken from the sheet each one is wearing rather than chosen freely, because the aura is
     * read as light coming off the Stand - a body in one colour throwing another is two objects.
     * The looks not named here keep the violet: the original because it is the violet, and P6
     * because its sheet does not argue with it.
     */
    private static final int MANGA_COLOR = 0xE163C6;
    private static final int OVA_COLOR = 0x1B8F94;
    private static final int P4_COLOR = 0x7FC6FF;
    private static final int BLUE_COLOR = 0x2E6BFF;

    /**
     * Two greens that have to stay told apart.
     *
     * <p>The same hue would have made them one skin in two textures, so they are separated on the
     * two axes that are not hue. All Star is near-full saturation at full brightness - a green that
     * is being emitted. Heritage keeps the hue and drops both, which is what faded means: the same
     * colour, with the light and the intensity taken out of it rather than a different colour that
     * happens to be duller.
     */
    private static final int ALLSTAR_COLOR = 0x36E04A;
    private static final int HERITAGE_COLOR = 0x7FA886;
    private static final int HERMIT_PURPLE_COLOR = 0x8A4FBF;

    /**
     * What each recoloured Hermit Purple reads as - its motes, its outline, its afterimages.
     *
     * <p>Given rather than inherited, because the vines are the whole of this Stand: a green Hermit
     * Purple trailing violet aura would be the one thing on screen disagreeing with itself.
     */
    private static final int HP_BLUE = 0x4F7FE0;
    private static final int HP_GOLD = 0xE0B040;
    private static final int HP_GREEN = 0x4FBF6A;
    private static final int HP_PINK = 0xE86FB0;
    private static final int HP_RED = 0xD64545;
    private static final int HP_WHITE = 0xE8E4EE;

    public static void bootstrap() {
        // Close-range, so it inherits Barrage, Uppercut, Stand Leap and Stand Dash; Inhale, Star
        // Finger and Time Stop are its own. Seven moves across two pages of five.
        //
        // Six looks, the anime's own first and the alternates after it. Order matters only
        // in that index 0 is the default; the rest are rolled between and are peers. All five share
        // one 128x128 UV layout, so they hang on the same geometry with nothing else to change.
        register(new StandType(STAR_PLATINUM_ID,
                asset("geo/star_platinum.geo.json"),
                List.of(skin("star_platinum", "original"),
                        skin("starplat_manga", "manga", MANGA_COLOR),
                        skin("starplat_ovaskin", "ova", OVA_COLOR),
                        skin("starplat_p4skin", "p4", P4_COLOR),
                        skin("starplat_p6skin", "p6"),
                        skin("starplat_blueskin", "blue", BLUE_COLOR),
                        skin("starplat_allstarskin", "allstar", ALLSTAR_COLOR),
                        skin("starplat_heritageskin", "heritage", HERITAGE_COLOR)),
                asset("animations/star_platinum.animation.json"),
                STAR_PLATINUM_COLOR,
                StandRange.CLOSE,
                // A brawler, which is where Enhanced Reflexes and Unwavering went: they are what
                // any Stand that fights at arm's length needs, not anything particular to this one.
                // What is particular to this one is below - the reach at point blank, and the eye.
                StandArchetype.BRAWLER,
                StandForm.HUMANOID,
                // The one Stand that shouts its own name. Anything else summoned gets the
                // manifestation whoosh and nothing on top - see StandType.voice.
                org.gumel.jojoha.registry.ModSounds.SP_STAR_PLATINUM,
                // Extended is Star Platinum's the same way Time Stop is. Time Skip is generic -
                // it belongs to whatever Stand learned the dash - so it lives with the generics.
                List.of(InhaleSkill.ID, StarFingerSkill.ID, TimeStopSkill.ID,
                        org.gumel.jojoha.stand.skill.moves.TimeStopExtendedSkill.ID,
                        org.gumel.jojoha.stand.skill.moves.SkullCrusherSkill.ID),
                List.of(TwoMetersPassive.ID, SensoryPerceptionPassive.ID)));

        registerHermitPurple();
    }

    /** The Stand's colour, or Star Platinum's if the id is unknown - callers shouldn't have to null-check. */
    /**
     * Hermit Purple, which is a good deal less than a Stand usually is.
     *
     * <p>Bound rather than humanoid, and that is the whole point of it being here. Joseph's Stand is
     * a handful of thorned vines coming off his arms - it has no body, no legs and nowhere to stand,
     * so everything the pipeline does for a figure that walks about beside you is wrong for it. See
     * StandForm.
     *
     * <p>Long range in the sense that matters for a bound Stand: the vine reaches a long way even
     * though the Stand itself never leaves the arm it grows from. Range here is a property of what
     * it throws rather than of how far the body may wander.
     *
     * <p>Specialist rather than brawler. It is not a fighting Stand and never was - what it does is
     * get Joseph somewhere and show him things.
     */
    private static void registerHermitPurple() {
        register(new StandType(HERMIT_PURPLE_ID,
                asset("geo/hermit_purple.geo.json"),
                // Order matters beyond taste: the index into this list is what HermitSkins uses to
                // pick the matching vine and barb, so a skin added here needs its three sheets and
                // its place in that table together.
                List.of(hermitSkin("hermit_purple", "original"),
                        hermitSkin("hermit_purple_blue", "blue", HP_BLUE),
                        hermitSkin("hermit_purple_gold", "gold", HP_GOLD),
                        hermitSkin("hermit_purple_green", "green", HP_GREEN),
                        hermitSkin("hermit_purple_pink", "pink", HP_PINK),
                        hermitSkin("hermit_purple_red", "red", HP_RED),
                        hermitSkin("hermit_purple_white", "white", HP_WHITE)),
                asset("animations/hermit_purple.animation.json"),
                HERMIT_PURPLE_COLOR,
                StandRange.LONG,
                StandArchetype.SPECIALIST,
                StandForm.BOUND,
                List.of(org.gumel.jojoha.stand.skill.moves.HermitGrappleSkill.ID,
                        // Listed by name rather than arriving with the humanoid generics, which a
                        // bound Stand does not get - see StandSkills.movesetFor. A dash is the one
                        // of that set that survives having no body: it is the user moving, with the
                        // Stand pulling, and needs no legs to do it.
                        org.gumel.jojoha.stand.skill.moves.StandDashSkill.ID,
                        org.gumel.jojoha.stand.skill.moves.ThornZipSkill.ID,
                        org.gumel.jojoha.stand.skill.moves.LassoOfThornsSkill.ID,
                        org.gumel.jojoha.stand.skill.moves.CameraCrushSkill.ID,
                        org.gumel.jojoha.stand.skill.moves.ThornWhipSkill.ID,
                        org.gumel.jojoha.stand.skill.moves.TwistingGutPunchSkill.ID),
                // Only the two that are Hermit Purple's own. Made For This and Unorthodox Method
                // come with being a specialist - see StandArchetype.
                List.of(org.gumel.jojoha.stand.passive.GrapplingVinePassive.ID,
                        org.gumel.jojoha.stand.passive.TangledPassive.ID)));
    }

    public static StandType byIdOrDefault(ResourceLocation id) {
        StandType type = id == null ? null : byId(id);
        return type != null ? type : byId(STAR_PLATINUM_ID);
    }

    private static void register(StandType type) {
        TYPES.put(type.id(), type);
    }

    public static StandType byId(ResourceLocation id) {
        return TYPES.get(id);
    }

    public static Map<ResourceLocation, StandType> all() {
        return TYPES;
    }

    /** The same pair, for Hermit Purple's own naming. */
    private static StandSkin hermitSkin(String texture, String name) {
        return new StandSkin(asset("textures/entity/" + texture + ".png"),
                "skin.jojoha.hermit_purple." + name);
    }

    private static StandSkin hermitSkin(String texture, String name, int auraColor) {
        return new StandSkin(asset("textures/entity/" + texture + ".png"),
                "skin.jojoha.hermit_purple." + name, auraColor);
    }

    /** A skin, from the sheet's filename and the short tag the reveal shouts. */
    private static StandSkin skin(String texture, String name) {
        return new StandSkin(asset("textures/entity/" + texture + ".png"),
                "skin.jojoha.star_platinum." + name);
    }

    /** The same, for a look that changes what colour the Stand reads as. */
    private static StandSkin skin(String texture, String name, int auraColor) {
        return new StandSkin(asset("textures/entity/" + texture + ".png"),
                "skin.jojoha.star_platinum." + name, auraColor);
    }

    private static ResourceLocation asset(String path) {
        return ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, path);
    }
}
