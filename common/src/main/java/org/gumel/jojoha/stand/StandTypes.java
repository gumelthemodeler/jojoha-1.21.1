package org.gumel.jojoha.stand;

import org.gumel.jojoha.combat.fx.MovementFx;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.registry.ModSounds;
import org.gumel.jojoha.stand.magician.MagicianMove;
import org.gumel.jojoha.stand.magician.MagicianPassive;
import org.gumel.jojoha.stand.passive.AscensionGaugePassive;
import org.gumel.jojoha.stand.passive.CombatAdaptationPassive;
import org.gumel.jojoha.stand.passive.CrushingForcePassive;
import org.gumel.jojoha.stand.passive.DuelistsFocusPassive;
import org.gumel.jojoha.stand.passive.GrapplingVinePassive;
import org.gumel.jojoha.stand.passive.MasterOfTimePassive;
import org.gumel.jojoha.stand.passive.PiercingThrustPassive;
import org.gumel.jojoha.stand.passive.RealityIsMinePassive;
import org.gumel.jojoha.stand.passive.ReboundPassive;
import org.gumel.jojoha.stand.passive.RippleConductionPassive;
import org.gumel.jojoha.stand.passive.SecondSightPassive;
import org.gumel.jojoha.stand.passive.SensoryPerceptionPassive;
import org.gumel.jojoha.stand.passive.SplashFragmentsPassive;
import org.gumel.jojoha.stand.passive.SwordsmanshipPassive;
import org.gumel.jojoha.stand.passive.TangledPassive;
import org.gumel.jojoha.stand.passive.ThornbindPassive;
import org.gumel.jojoha.stand.passive.TwoMetersPassive;
import org.gumel.jojoha.stand.skill.moves.AfterimagesSkill;
import org.gumel.jojoha.stand.skill.moves.ArmorReleaseSkill;
import org.gumel.jojoha.stand.skill.moves.BalestraSkill;
import org.gumel.jojoha.stand.skill.moves.BeatdownSkill;
import org.gumel.jojoha.stand.skill.moves.BladeLaunchSkill;
import org.gumel.jojoha.stand.skill.moves.BodyPossessionSkill;
import org.gumel.jojoha.stand.skill.moves.CameraCrushSkill;
import org.gumel.jojoha.stand.skill.moves.ClairvoyanceSkill;
import org.gumel.jojoha.stand.skill.moves.DeflectionSkill;
import org.gumel.jojoha.stand.skill.moves.EmeraldRadiusSkill;
import org.gumel.jojoha.stand.skill.moves.EmeraldSplashBarrageSkill;
import org.gumel.jojoha.stand.skill.moves.EmeraldSplashShotgunSkill;
import org.gumel.jojoha.stand.skill.moves.ErasureSkill;
import org.gumel.jojoha.stand.skill.moves.GreatestHighSkill;
import org.gumel.jojoha.stand.skill.moves.GroundBreakerSkill;
import org.gumel.jojoha.stand.skill.moves.HeavenStepSkill;
import org.gumel.jojoha.stand.skill.moves.HeavenTimeStopSkill;
import org.gumel.jojoha.stand.skill.moves.HeavensUppercutSkill;
import org.gumel.jojoha.stand.skill.moves.HermitGrappleSkill;
import org.gumel.jojoha.stand.skill.moves.InfiniteServitudeSkill;
import org.gumel.jojoha.stand.skill.moves.InhaleSkill;
import org.gumel.jojoha.stand.skill.moves.JudgementFistSkill;
import org.gumel.jojoha.stand.skill.moves.KickFlurrySkill;
import org.gumel.jojoha.stand.skill.moves.KnifeBarrageSkill;
import org.gumel.jojoha.stand.skill.moves.KnifeHaloSkill;
import org.gumel.jojoha.stand.skill.moves.LassoOfThornsSkill;
import org.gumel.jojoha.stand.skill.moves.MiniatureTrapSkill;
import org.gumel.jojoha.stand.skill.moves.MudaRushSkill;
import org.gumel.jojoha.stand.skill.moves.OverwriteSkill;
import org.gumel.jojoha.stand.skill.moves.RealityBendSkill;
import org.gumel.jojoha.stand.skill.moves.RestorationSkill;
import org.gumel.jojoha.stand.skill.moves.RiposteSkill;
import org.gumel.jojoha.stand.skill.moves.SabreCycleSkill;
import org.gumel.jojoha.stand.skill.moves.SkullCrusherSkill;
import org.gumel.jojoha.stand.skill.moves.StandDashSkill;
import org.gumel.jojoha.stand.skill.moves.StarFingerSkill;
import org.gumel.jojoha.stand.skill.moves.SwordFlurrySkill;
import org.gumel.jojoha.stand.skill.moves.TendrilSpikeSkill;
import org.gumel.jojoha.stand.skill.moves.ThornWhipSkill;
import org.gumel.jojoha.stand.skill.moves.ThornZipSkill;
import org.gumel.jojoha.stand.skill.moves.TimeStopSkill;
import org.gumel.jojoha.stand.skill.moves.TimeStopUpgradeSkill;
import org.gumel.jojoha.stand.skill.moves.TwistingGutPunchSkill;
import org.gumel.jojoha.stand.skill.moves.UnravelTrapSkill;
import org.gumel.jojoha.stand.skill.moves.WebSwingSkill;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class StandTypes {
    private static final Map<ResourceLocation, StandType> TYPES = new LinkedHashMap<>();

    public static final ResourceLocation STAR_PLATINUM_ID = asset("star_platinum");
    public static final ResourceLocation HERMIT_PURPLE_ID = asset("hermit_purple");
    public static final ResourceLocation THE_WORLD_ID = asset("the_world");
    public static final ResourceLocation HIEROPHANT_GREEN_ID = asset("hierophant_green");
    public static final ResourceLocation SILVER_CHARIOT_ID = asset("silver_chariot");
    public static final ResourceLocation MAGICIANS_RED_ID = asset("magicians_red");
    public static final ResourceLocation ANUBIS_ID = asset("anubis");
    public static final ResourceLocation SECRET_THE_WORLD_ID = asset("the_world_over_heaven");

    private StandTypes() {
    }

    private static final int STAR_PLATINUM_COLOR = 0xB35CFF;

    private static final int MANGA_COLOR = 0xE163C6;
    private static final int OVA_COLOR = 0x1B8F94;
    private static final int P4_COLOR = 0x7FC6FF;
    private static final int BLUE_COLOR = 0x2E6BFF;

    private static final int ALLSTAR_COLOR = 0x36E04A;
    private static final int HERITAGE_COLOR = 0x7FA886;
    private static final int HERMIT_PURPLE_COLOR = 0x8A4FBF;

    private static final int HIEROPHANT_GREEN_COLOR = 0x21A313;

    private static final int HG_BLUE = 0x37C5C8;
    private static final int HG_OVA = 0x9F9F44;
    private static final int HG_PURPLE = 0xA83FD4;
    private static final int HG_RED = 0xB02424;
    private static final int HG_YELLOW = 0xCDD263;

    private static final int HP_BLUE = 0x4F7FE0;
    private static final int HP_GOLD = 0xE0B040;
    private static final int HP_GREEN = 0x4FBF6A;
    private static final int HP_PINK = 0xE86FB0;
    private static final int HP_RED = 0xD64545;
    private static final int HP_WHITE = 0xE8E4EE;

    private static final int THE_WORLD_COLOR = 0xE8C547;
    private static final int TW_MANGA = 0xFFD84A;
    private static final int TW_GREATEST_HIGH = 0x3B2A8C;
    private static final int TW_OVA = 0x9A8A55;
    private static final int TW_DESATURATED = 0xD9D9D9;
    private static final int TW_ALTERNATE = 0x6A665C;
    private static final int TW_SHADOW = 0x9372C5;
    private static final int TW_BLURAY = 0x4A9B9E;
    private static final int TW_FIGURE_1 = 0xC7B3D6;
    private static final int TW_FIGURE_2 = 0xBCA57D;
    private static final int TW_FIGURE_3 = 0x81BA4D;

    private static final int SILVER_CHARIOT_COLOR = 0xB4C3CC;
    private static final int SC_OVA = 0xD0E4E8;
    private static final int SC_SAND = 0xC8C09A;

    private static final int MAGICIANS_RED_COLOR = 0xE8502A;

    private static final int ANUBIS_COLOR = 0xAB7A2B;

    private static final int SECRET_THE_WORLD_COLOR = 0xFFF0C2;

    public static void bootstrap() {
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
                StandArchetype.BRAWLER,
                StandForm.HUMANOID,
                ModSounds.SP_STAR_PLATINUM,
                List.of(InhaleSkill.ID, StarFingerSkill.ID, TimeStopSkill.ID,
                        TimeStopUpgradeSkill.EXTENDED_ID,
                        SkullCrusherSkill.ID),
                List.of(TwoMetersPassive.ID, SensoryPerceptionPassive.ID)));

        registerHermitPurple();
        registerTheWorld();
        registerHierophantGreen();
        registerSilverChariot();
        registerMagiciansRed();
        registerAnubis();
        registerSecretTheWorld();
    }

    private static void registerMagiciansRed() {
        register(new StandType(MAGICIANS_RED_ID,
                asset("geo/magicians_red.geo.json"),
                List.of(new StandSkin(asset("textures/entity/stand/magicians_red/magicians_red.png"),
                        "skin.jojoha.magicians_red.original")),
                asset("animations/magicians_red.animation.json"),
                MAGICIANS_RED_COLOR,
                StandRange.CLOSE,
                StandArchetype.CONTROL,
                StandForm.HUMANOID,
                List.of(MagicianMove.CONTROLLED_BLAZE.id(),
                        MagicianMove.SWEEP_KICK.id(),
                        MagicianMove.CROSSFIRE_HURRICANE.id(),
                        MagicianMove.CROSSFIRE_HURRICANE_SPECIAL.id(),
                        MagicianMove.RED_BIND.id(),
                        MagicianMove.INFERNO.id(),
                        MagicianMove.CONTAINMENT.id(),
                        MagicianMove.LIFE_SENSE.id(),
                        MagicianMove.INCREASE_TEMPERATURE.id(),
                        MagicianMove.INFINITE_INFERNO_FIRE.id(),
                        MagicianMove.FINGER_SNAP.id()),
                List.of(MagicianPassive.FLAME_IMMUNITY.id(),
                        MagicianPassive.BURNING_PRESSURE.id())));
    }

    private static void registerAnubis() {
        register(new StandType(ANUBIS_ID,
                asset("geo/star_platinum.geo.json"),
                List.of(new StandSkin(asset("textures/item/anubis.png"), "skin.jojoha.anubis.original")),
                asset("animations/universal.animation.json"),
                ANUBIS_COLOR,
                StandRange.CLOSE,
                StandArchetype.SPECIALIST,
                StandForm.WEAPON,
                List.of(),
                List.of(SwordsmanshipPassive.ID,
                        CombatAdaptationPassive.ID)));
    }

    private static void registerSilverChariot() {
        register(new StandType(SILVER_CHARIOT_ID,
                asset("geo/silver_chariot.geo.json"),
                List.of(chariotSkin("silver_chariot", "original"),
                        chariotSkin("silver_chariot_ova", "ova", SC_OVA),
                        chariotSkin("silver_chariot_sand", "sand", SC_SAND)),
                asset("animations/silverchariot.animation.json"),
                SILVER_CHARIOT_COLOR,
                StandRange.CLOSE,
                StandArchetype.BRAWLER,
                StandForm.HUMANOID,
                ModSounds.SC_SILVER_CHARIOT,
                List.of(SwordFlurrySkill.ID,
                        BalestraSkill.ID,
                        BladeLaunchSkill.ID,
                        SabreCycleSkill.ID,
                        DeflectionSkill.ID,
                        RiposteSkill.ID,
                        ArmorReleaseSkill.ID,
                        AfterimagesSkill.ID),
                List.of(PiercingThrustPassive.ID,
                        DuelistsFocusPassive.ID)));
    }

    private static void registerTheWorld() {
        register(new StandType(THE_WORLD_ID,
                asset("geo/the_world.geo.json"),
                List.of(worldSkin("the_world", "original"),
                        worldSkin("the_world_manga", "manga", TW_MANGA),
                        worldSkin("the_world_greatest_high", "greatest_high", TW_GREATEST_HIGH),
                        worldSkin("the_world_ova", "ova", TW_OVA),
                        worldSkin("the_world_desaturated", "desaturated", TW_DESATURATED),
                        worldSkin("the_world_alternate", "alternate", TW_ALTERNATE),
                        worldSkin("shadow_theworld", "shadow", TW_SHADOW),
                        worldSkin("the_world_bluray", "bluray", TW_BLURAY),
                        worldSkin("the_world_figure_1", "figure_1", TW_FIGURE_1),
                        worldSkin("the_world_figure_2", "figure_2", TW_FIGURE_2),
                        worldSkin("the_world_figure_3", "figure_3", TW_FIGURE_3),
                        worldSkin("secret_the_world", "over_heaven", SECRET_THE_WORLD_COLOR)
                                .earnedBy("menu.jojoha.skins.unlock.over_heaven")),
                asset("animations/theworld.animation.json"),
                THE_WORLD_COLOR,
                StandRange.CLOSE,
                StandArchetype.BRAWLER,
                StandForm.HUMANOID,
                List.of(GroundBreakerSkill.ID,
                        KnifeBarrageSkill.ID,
                        KickFlurrySkill.ID,
                        BeatdownSkill.ID,
                        TimeStopSkill.ID,
                        TimeStopUpgradeSkill.PERFECT_ID),
                List.of(MasterOfTimePassive.ID,
                        CrushingForcePassive.ID)));
    }

    private static void registerSecretTheWorld() {
        register(new StandType(SECRET_THE_WORLD_ID,
                asset("geo/the_world.geo.json"),
                List.of(heavenSkin("secret_the_world", "original")),
                asset("animations/theworld.animation.json"),
                SECRET_THE_WORLD_COLOR,
                StandRange.CLOSE,
                StandArchetype.HEAVEN,
                StandForm.HUMANOID,
                ModSounds.TW_TIMESTOP,
                List.of(MudaRushSkill.ID,
                        GreatestHighSkill.ID,
                        HeavenTimeStopSkill.ID,
                        InfiniteServitudeSkill.ID,
                        RestorationSkill.ID,
                        ErasureSkill.ID,
                        OverwriteSkill.ID,
                        RealityBendSkill.ID,
                        HeavensUppercutSkill.ID,
                        JudgementFistSkill.ID,
                        HeavenStepSkill.ID,
                        KnifeHaloSkill.ID),
                List.of(AscensionGaugePassive.ID,
                        RealityIsMinePassive.ID)));
    }

    public static boolean secret(ResourceLocation id) {
        return SECRET_THE_WORLD_ID.equals(id);
    }

    private static void registerHierophantGreen() {
        register(new StandType(HIEROPHANT_GREEN_ID,
                asset("geo/hierophant_green.geo.json"),
                List.of(hierophantSkin("hierophant_green", "original"),
                        hierophantSkin("hierophant_green_blue_skin", "blue", HG_BLUE),
                        hierophantSkin("hierophant_green_ova_skin", "ova", HG_OVA),
                        hierophantSkin("hierophant_green_purple_skin", "purple", HG_PURPLE),
                        hierophantSkin("hierophant_green_red_skin", "red", HG_RED),
                        hierophantSkin("hierophant_green_yellow_skin", "yellow", HG_YELLOW)),
                asset("animations/hierophant_green.animation.json"),
                HIEROPHANT_GREEN_COLOR,
                StandRange.LONG,
                StandArchetype.RANGED,
                StandForm.HUMANOID,
                ModSounds.HG_HIEROPHANT_GREEN,
                List.of(EmeraldSplashShotgunSkill.ID,
                        EmeraldSplashBarrageSkill.ID,
                        TendrilSpikeSkill.ID,
                        WebSwingSkill.ID,
                        UnravelTrapSkill.ID,
                        MiniatureTrapSkill.ID,
                        BodyPossessionSkill.ID,
                        EmeraldRadiusSkill.ID),
                List.of(SplashFragmentsPassive.ID,
                        ReboundPassive.ID)));
    }

    private static void registerHermitPurple() {
        register(new StandType(HERMIT_PURPLE_ID,
                asset("geo/hermit_purple.geo.json"),
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
                List.of(HermitGrappleSkill.ID,
                        StandDashSkill.ID,
                        ThornZipSkill.ID,
                        LassoOfThornsSkill.ID,
                        CameraCrushSkill.ID,
                        ClairvoyanceSkill.ID,
                        ThornWhipSkill.ID,
                        TwistingGutPunchSkill.ID),
                List.of(GrapplingVinePassive.ID,
                        TangledPassive.ID,
                        GrapplingVinePassive.KEEN_ID,
                        ThornbindPassive.ID,
                        SecondSightPassive.ID,
                        RippleConductionPassive.ID)));
    }

    public static boolean hasBody(JojohaPlayerData data) {
        return !data.stand.isPresent() || byIdOrDefault(data.stand.standId()).form().hasBody();
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

    private static StandSkin hermitSkin(String texture, String name) {
        return new StandSkin(asset("textures/entity/stand/hermit_purple/" + texture + ".png"),
                "skin.jojoha.hermit_purple." + name);
    }

    private static StandSkin hermitSkin(String texture, String name, int auraColor) {
        return new StandSkin(asset("textures/entity/stand/hermit_purple/" + texture + ".png"),
                "skin.jojoha.hermit_purple." + name, auraColor);
    }

    private static StandSkin hierophantSkin(String texture, String name) {
        return new StandSkin(asset("textures/entity/stand/hierophant_green/" + texture + ".png"),
                "skin.jojoha.hierophant_green." + name);
    }

    private static StandSkin hierophantSkin(String texture, String name, int auraColor) {
        return new StandSkin(asset("textures/entity/stand/hierophant_green/" + texture + ".png"),
                "skin.jojoha.hierophant_green." + name, auraColor);
    }

    private static StandSkin chariotSkin(String texture, String name) {
        return new StandSkin(asset("textures/entity/stand/silver_chariot/" + texture + ".png"),
                "skin.jojoha.silver_chariot." + name);
    }

    private static StandSkin chariotSkin(String texture, String name, int auraColor) {
        return new StandSkin(asset("textures/entity/stand/silver_chariot/" + texture + ".png"),
                "skin.jojoha.silver_chariot." + name, auraColor);
    }

    private static StandSkin worldSkin(String texture, String name) {
        return new StandSkin(asset("textures/entity/stand/the_world/" + texture + ".png"),
                "skin.jojoha.the_world." + name);
    }

    private static StandSkin worldSkin(String texture, String name, int auraColor) {
        return new StandSkin(asset("textures/entity/stand/the_world/" + texture + ".png"),
                "skin.jojoha.the_world." + name, auraColor);
    }

    private static StandSkin heavenSkin(String texture, String name) {
        return new StandSkin(asset("textures/entity/stand/the_world/" + texture + ".png"),
                "skin.jojoha.secret_the_world." + name);
    }

    private static StandSkin skin(String texture, String name) {
        return new StandSkin(asset("textures/entity/stand/star_platinum/" + texture + ".png"),
                "skin.jojoha.star_platinum." + name);
    }

    private static StandSkin skin(String texture, String name, int auraColor) {
        return new StandSkin(asset("textures/entity/stand/star_platinum/" + texture + ".png"),
                "skin.jojoha.star_platinum." + name, auraColor);
    }

    private static final Map<ResourceLocation, String> SPAWN_ANIMATIONS =
            Map.of(THE_WORLD_ID, "tw_spawn");

    private static final Map<ResourceLocation, String> TIME_STOP_ANIMATIONS =
            Map.of(THE_WORLD_ID, "tw_timestop");

    private static final Map<ResourceLocation, String> CHARGE_ANIMATIONS =
            Map.of(THE_WORLD_ID, "charge_loop");

    private static final Map<ResourceLocation, String> USER_TIME_STOP_ANIMATIONS =
            Map.of(THE_WORLD_ID, MovementFx.THEWORLD_TIMESTOP_PLAY);

    private static final Map<ResourceLocation, String> USER_CHARGE_ANIMATIONS =
            Map.of(THE_WORLD_ID, MovementFx.THEWORLD_TIMESTOP_HOLD);

    public static final int USER_TIME_STOP_KEY_TICKS = 12;

    @Nullable
    public static String userTimeStopAnimation(@Nullable StandType type) {
        return type == null ? null : USER_TIME_STOP_ANIMATIONS.get(SECRET_THE_WORLD_ID.equals(type.id()) ? THE_WORLD_ID : type.id());
    }

    @Nullable
    public static String userChargeAnimation(@Nullable StandType type) {
        return type == null ? null : USER_CHARGE_ANIMATIONS.get(SECRET_THE_WORLD_ID.equals(type.id()) ? THE_WORLD_ID : type.id());
    }

    @Nullable
    public static String chargeAnimation(@Nullable StandType type) {
        if (type == null) {
            return null;
        }
        ResourceLocation id = SECRET_THE_WORLD_ID.equals(type.id()) ? THE_WORLD_ID : type.id();
        return CHARGE_ANIMATIONS.get(id);
    }

    @Nullable
    public static String timeStopAnimation(@Nullable StandType type) {
        if (type == null) {
            return null;
        }
        ResourceLocation id = SECRET_THE_WORLD_ID.equals(type.id()) ? THE_WORLD_ID : type.id();
        return TIME_STOP_ANIMATIONS.get(id);
    }

    public static String spawnAnimation(@Nullable StandType type) {
        if (type == null) {
            return "spawn";
        }
        ResourceLocation id = SECRET_THE_WORLD_ID.equals(type.id()) ? THE_WORLD_ID : type.id();
        return SPAWN_ANIMATIONS.getOrDefault(id, "spawn");
    }

    private static ResourceLocation asset(String path) {
        return ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, path);
    }
}
