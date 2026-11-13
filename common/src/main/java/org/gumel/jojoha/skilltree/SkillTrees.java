package org.gumel.jojoha.skilltree;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.StatPoints;
import org.gumel.jojoha.hamon.HamonPath;
import org.gumel.jojoha.hamon.HamonPaths;
import org.gumel.jojoha.skilltree.SkillNode.Branch;
import org.gumel.jojoha.skilltree.SkillNode.Gate;
import org.gumel.jojoha.skilltree.SkillNode.ItemRequirement;
import org.gumel.jojoha.skilltree.SkillNode.StatRequirement;
import org.gumel.jojoha.stand.StandTypes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SkillTrees {
    public enum Tree { STAND, HAMON, PLAYER, VAMPIRE, ULTIMATE, PILLAR }

    public static final ResourceLocation STAND_ROOT = id("unlock_stand");
    public static final ResourceLocation HAMON_ROOT = id("unlock_hamon");
    public static final ResourceLocation PLAYER_ROOT = id("unlock_player");
    public static final ResourceLocation VAMPIRE_ROOT = id("unlock_vampirism");
    public static final ResourceLocation ULTIMATE_ROOT = id("unlock_ultimate");
    public static final ResourceLocation PILLAR_ROOT = id("unlock_pillar");

    private static final int POWER = 0;
    private static final int SPEED = 1;
    private static final int STAND_ENDURANCE = 2;
    private static final int PROTECTION = 3;
    private static final int POTENTIAL = 4;

    private static final Map<Tree, List<SkillNode>> TREES = new HashMap<>();
    private static final Map<ResourceLocation, SkillNode> BY_ID = new HashMap<>();
    private static final Map<ResourceLocation, List<SkillNode>> BY_SKILL = new HashMap<>();

    private SkillTrees() {
    }

    private static List<SkillNode> universalStandNodes() {
        List<SkillNode> nodes = new ArrayList<>();

        nodes.add(SkillNode.root(STAND_ROOT));

        nodes.add(arm("uppercut", "uppercut", STAND_ROOT, Branch.COMBAT, COMBAT, 1,
                stand(POWER, 10)));
        nodes.add(arm("grab", "grab", STAND_ROOT, Branch.COMBAT, PASSIVE, 1,
                stand(POWER, 8)));

        nodes.add(arm("recall_stand", "recall_stand", STAND_ROOT, Branch.UTILITY, BUILD, 1,
                stand(STAND_ENDURANCE, 4)));
        nodes.add(arm("build_single", "build_single", id("recall_stand"), Branch.UTILITY, BUILD, 2,
                stand(PROTECTION, 6)));
        nodes.add(arm("build_row", "build_row", id("build_single"), Branch.UTILITY, BUILD, 3,
                stand(PROTECTION, 10)));
        nodes.add(arm("build_column", "build_column", id("build_row"), Branch.UTILITY, BUILD, 4,
                stand(PROTECTION, 14)));
        nodes.add(arm("build_free", "build_free", id("build_column"), Branch.UTILITY, BUILD, 5,
                stand(PROTECTION, 18)));
        nodes.add(arm("build_selection", "build_selection", id("build_free"), Branch.UTILITY,
                CHANNEL, 5, stand(PROTECTION, 22)));

        return nodes;
    }

    private static List<SkillNode> starPlatinumTree() {
        List<SkillNode> nodes = universalStandNodes();

        nodes.add(arm("star_finger", "star_finger", id("uppercut"), Branch.COMBAT, COMBAT, 2,
                stand(POWER, 18)));
        nodes.add(arm("skull_crusher", "skull_crusher", id("star_finger"), Branch.COMBAT, COMBAT, 3,
                stand(POWER, 28)));

        nodes.add(arm("inhale", "inhale", STAND_ROOT, Branch.COMBAT, INHALE, 2,
                stand(STAND_ENDURANCE, 14)));

        nodes.add(armNeeding("time_skip", "time_skip", id("time_stop"), Branch.MOBILITY, MOVEMENT, 3,
                List.of(stand(SPEED, 20), stand(POTENTIAL, 16)),
                List.of(), List.of()));

        nodes.add(armNeeding("time_stop", "time_stop", STAND_ROOT, Branch.SPECIAL, SPECIAL, 2,
                List.of(stand(POTENTIAL, 24), player(StatPoints.WORTHINESS, 20)),
                List.of(Gate.TIME_STOP_EXPOSURE), List.of()));
        nodes.add(armNeeding("time_stop_extended", "time_stop_extended", id("time_stop"),
                Branch.SPECIAL, SPECIAL, 3,
                List.of(stand(POTENTIAL, 34), player(StatPoints.WORTHINESS, 30)),
                List.of(Gate.TIME_STOP_EXPOSURE), List.of()));

        nodes.add(innate("two_meters", "two_meters", STAND_ROOT, Branch.COMBAT, -2));
        nodes.add(innate("sensory_perception", "sensory_perception", STAND_ROOT,
                Branch.SPECIAL, -1));
        nodes.add(innate("enhanced_reflexes", "enhanced_reflexes", STAND_ROOT,
                Branch.SPECIAL, 1));
        nodes.add(innate("unwavering", "unwavering", STAND_ROOT, Branch.SPECIAL, 2));

        return nodes;
    }

    private static List<SkillNode> theWorldTree() {
        List<SkillNode> nodes = universalStandNodes();

        nodes.add(arm("ground_breaker", "ground_breaker", id("uppercut"), Branch.COMBAT, COMBAT, 2,
                stand(POWER, 16)));
        nodes.add(arm("knife_barrage", "knife_barrage", id("ground_breaker"), Branch.COMBAT, COMBAT, 3,
                stand(POWER, 20), stand(SPEED, 14)));

        nodes.add(arm("kick_flurry", "kick_flurry", STAND_ROOT, Branch.COMBAT, INHALE, 2,
                stand(POWER, 14), stand(SPEED, 12)));
        nodes.add(arm("beatdown", "beatdown", id("grab"), Branch.COMBAT, PASSIVE, 2,
                stand(POWER, 22)));

        nodes.add(armNeeding("time_stop", "time_stop", STAND_ROOT, Branch.SPECIAL, SPECIAL, 2,
                List.of(stand(POTENTIAL, 24), player(StatPoints.WORTHINESS, 20)),
                List.of(), List.of()));
        nodes.add(armNeeding("time_stop_perfect", "time_stop_perfect", id("time_stop"),
                Branch.SPECIAL, SPECIAL, 3,
                List.of(stand(POTENTIAL, 34), player(StatPoints.WORTHINESS, 30)),
                List.of(Gate.TURNED), List.of()));
        nodes.add(armNeeding("time_skip", "time_skip", id("time_stop"), Branch.MOBILITY, MOVEMENT, 3,
                List.of(stand(SPEED, 20), stand(POTENTIAL, 16)),
                List.of(), List.of()));

        nodes.add(innate("enhanced_reflexes", "enhanced_reflexes", STAND_ROOT, Branch.SPECIAL, -2));
        nodes.add(innate("unwavering", "unwavering", STAND_ROOT, Branch.SPECIAL, -1));
        nodes.add(innate("master_of_time", "master_of_time", STAND_ROOT, Branch.SPECIAL, 1));
        nodes.add(innate("crushing_force", "crushing_force", STAND_ROOT, Branch.SPECIAL, 2));
        return nodes;
    }

    private static List<SkillNode> secretTheWorldTree() {
        List<SkillNode> nodes = universalStandNodes();

        nodes.add(arm("muda_rush", "muda_rush", id("uppercut"), Branch.COMBAT, COMBAT, 2));
        nodes.add(arm("greatest_high", "greatest_high", id("muda_rush"), Branch.COMBAT, COMBAT, 3));
        nodes.add(arm("restoration", "restoration", STAND_ROOT, Branch.COMBAT, INHALE, 2));
        nodes.add(arm("infinite_servitude", "infinite_servitude", id("grab"), Branch.COMBAT, PASSIVE, 2));

        nodes.add(arm("heaven_time_stop", "heaven_time_stop", STAND_ROOT, Branch.SPECIAL, SPECIAL, 2));
        nodes.add(arm("erasure", "erasure", id("heaven_time_stop"), Branch.SPECIAL, SPECIAL, 3));
        nodes.add(arm("overwrite", "overwrite", id("erasure"), Branch.SPECIAL, SPECIAL, 4));
        nodes.add(arm("reality_bend", "reality_bend", STAND_ROOT, Branch.SPECIAL, CHANNEL, 2));
        nodes.add(arm("heavens_uppercut", "heavens_uppercut", id("uppercut"), Branch.COMBAT, PASSIVE_B, 2));
        nodes.add(arm("judgement_fist", "judgement_fist", id("heavens_uppercut"), Branch.COMBAT, PASSIVE_B, 3));
        nodes.add(arm("heaven_step", "heaven_step", STAND_ROOT, Branch.MOBILITY, MOVEMENT, 2));
        nodes.add(arm("knife_halo", "knife_halo", STAND_ROOT, Branch.COMBAT, PASSIVE_C, 2));
        nodes.add(armNeeding("heaven_time_skip", "time_skip", id("heaven_time_stop"), Branch.MOBILITY, MOVEMENT, 3,
                List.of(), List.of(), List.of()));

        nodes.add(innate("boss_presence", "boss_presence", STAND_ROOT, Branch.SPECIAL, -3));
        nodes.add(innate("ascended", "ascended", STAND_ROOT, Branch.SPECIAL, -2));
        nodes.add(innate("transcendent", "transcendent", STAND_ROOT, Branch.SPECIAL, -1));
        nodes.add(innate("heaven", "heaven", STAND_ROOT, Branch.SPECIAL, 1));
        nodes.add(innate("ascension_gauge", "ascension_gauge", STAND_ROOT, Branch.SPECIAL, 2));
        nodes.add(innate("reality_is_mine", "reality_is_mine", STAND_ROOT, Branch.SPECIAL, 3));
        return nodes;
    }

    private static List<SkillNode> playerNodes() {
        List<SkillNode> nodes = new ArrayList<>();
        nodes.add(SkillNode.root(PLAYER_ROOT));

        nodes.add(arm("heavy_punch", "heavy_punch", PLAYER_ROOT, Branch.COMBAT, COMBAT, 1,
                player(StatPoints.STRENGTH, 6)));
        nodes.add(arm("player_uppercut", "uppercut", id("heavy_punch"), Branch.COMBAT, COMBAT, 2,
                player(StatPoints.STRENGTH, 14)));
        nodes.add(arm("player_barrage", "barrage", id("player_uppercut"), Branch.COMBAT, COMBAT, 3,
                player(StatPoints.STRENGTH, 22), player(StatPoints.ENDURANCE, 12)));

        nodes.add(arm("gut_punch", "gut_punch", id("heavy_punch"), Branch.COMBAT, INHALE, 2,
                player(StatPoints.STRENGTH, 12), player(StatPoints.ENDURANCE, 8)));

        nodes.add(arm("dodge", "dodge", PLAYER_ROOT, Branch.MOBILITY, BUILD, 1,
                player(StatPoints.AGILITY, 8)));
        nodes.add(arm("double_jump", "double_jump", id("dodge"), Branch.MOBILITY, BUILD, 2,
                player(StatPoints.AGILITY, 18)));

        nodes.add(arm("burst", "burst", id("double_jump"), Branch.MOBILITY, MOVEMENT, 2,
                player(StatPoints.AGILITY, 22)));

        nodes.add(arm("ground_slam", "ground_slam", id("double_jump"), Branch.COMBAT, BUILD, 3,
                player(StatPoints.STRENGTH, 18), player(StatPoints.AGILITY, 16)));

        nodes.add(innate("might", "might", PLAYER_ROOT, Branch.COMBAT, -2));
        nodes.add(innate("constitution", "constitution", PLAYER_ROOT, Branch.CORE, -1));
        nodes.add(innate("fleetness", "fleetness", PLAYER_ROOT, Branch.MOBILITY, 0));
        nodes.add(innate("hardiness", "hardiness", PLAYER_ROOT, Branch.UTILITY, 1));
        nodes.add(innate("resolve", "resolve", PLAYER_ROOT, Branch.SPECIAL, 2));

        return nodes;
    }

    private static List<SkillNode> hamonNodes() {
        List<SkillNode> nodes = new ArrayList<>();
        nodes.add(SkillNode.root(HAMON_ROOT));

        nodes.add(armNeeding("zoom_punch", "zoom_punch", HAMON_ROOT, Branch.COMBAT, COMBAT, 1,
                List.of(player(StatPoints.STRENGTH, 8)), List.of(Gate.HAMON_BOOK), List.of()));

        nodes.add(armNeeding("ripple_pulse", "ripple_pulse", id("zoom_punch"), Branch.COMBAT, COMBAT, 2,
                List.of(player(StatPoints.STRENGTH, 14)), List.of(Gate.HAMON_BOOK), List.of()));

        nodes.add(armNeeding("hamon_overdrive", "hamon_overdrive", HAMON_ROOT, Branch.SPECIAL, BUILD, 1,
                List.of(player(StatPoints.ENDURANCE, 10)), List.of(Gate.HAMON_BOOK), List.of()));

        nodes.add(armNeeding("ripple_barrier", "ripple_barrier", id("hamon_overdrive"),
                Branch.SPECIAL, BUILD, 2,
                List.of(player(StatPoints.ENDURANCE, 18), player(StatPoints.VITALITY, 14)),
                List.of(Gate.HAMON_BOOK), List.of()));

        nodes.add(armNeeding("channeling", "channeling", id("hamon_overdrive"),
                Branch.SPECIAL, CHANNEL, 2,
                List.of(player(StatPoints.STRENGTH, 16), player(StatPoints.ENDURANCE, 12)),
                List.of(Gate.HAMON_BOOK), List.of()));

        nodes.add(armNeeding("sunlight_yellow_overdrive", "sunlight_yellow_overdrive",
                id("ripple_barrier"), Branch.SPECIAL, BUILD, 3,
                List.of(player(StatPoints.STRENGTH, 24), player(StatPoints.ENDURANCE, 24)),
                List.of(Gate.HAMON_BOOK), List.of(id("ripple_pulse"))));

        nodes.add(armNeeding("path_trickster", null, HAMON_ROOT, Branch.SPECIAL, PASSIVE_B, 2,
                List.of(player(StatPoints.AGILITY, 10), player(StatPoints.WORTHINESS, 8)),
                List.of(Gate.modeBook(HamonPaths.TRICKSTER)), List.of()));
        nodes.add(armNeeding("clacker_volley", "clacker_volley", id("path_trickster"),
                Branch.COMBAT, PASSIVE_B, 3,
                List.of(player(StatPoints.STRENGTH, 12), player(StatPoints.AGILITY, 12)),
                List.of(Gate.hamonPath(HamonPaths.TRICKSTER)), List.of()));
        nodes.add(armNeeding("bottle_cap_shot", "bottle_cap_shot", id("clacker_volley"),
                Branch.COMBAT, PASSIVE_B, 4,
                List.of(player(StatPoints.AGILITY, 16), player(StatPoints.WORTHINESS, 12)),
                List.of(Gate.hamonPath(HamonPaths.TRICKSTER)), List.of()));
        nodes.add(armNeeding("next_line", "next_line", id("bottle_cap_shot"),
                Branch.SPECIAL, PASSIVE_B, 5,
                List.of(player(StatPoints.AGILITY, 20), player(StatPoints.WORTHINESS, 16)),
                List.of(Gate.hamonPath(HamonPaths.TRICKSTER)), List.of()));

        nodes.add(armNeeding("path_wisdom", null, HAMON_ROOT, Branch.SPECIAL, SPECIAL, 1,
                List.of(player(StatPoints.WORTHINESS, 10), player(StatPoints.VITALITY, 8)),
                List.of(Gate.modeBook(HamonPaths.WISDOM)), List.of()));
        nodes.add(armNeeding("forced_ripple_breathing", "forced_ripple_breathing",
                id("path_wisdom"), Branch.UTILITY, SPECIAL, 2,
                List.of(player(StatPoints.WORTHINESS, 12), player(StatPoints.ENDURANCE, 10)),
                List.of(Gate.hamonPath(HamonPaths.WISDOM)), List.of()));

        nodes.add(armNeeding("path_prideful", null, HAMON_ROOT, Branch.SPECIAL, INHALE, 1,
                List.of(player(StatPoints.AGILITY, 10), player(StatPoints.WORTHINESS, 8)),
                List.of(Gate.modeBook(HamonPaths.PRIDEFUL)), List.of()));
        nodes.add(armNeeding("bubble_cutter", "bubble_cutter", id("path_prideful"),
                Branch.COMBAT, INHALE, 2,
                List.of(player(StatPoints.AGILITY, 14)),
                List.of(Gate.hamonPath(HamonPaths.PRIDEFUL)), List.of()));
        nodes.add(armNeeding("bubble_launcher", "bubble_launcher", id("bubble_cutter"),
                Branch.COMBAT, INHALE, 3,
                List.of(player(StatPoints.AGILITY, 22), player(StatPoints.STRENGTH, 14)),
                List.of(Gate.hamonPath(HamonPaths.PRIDEFUL)), List.of()));

        nodes.add(armNeeding("path_stylish", null, HAMON_ROOT, Branch.SPECIAL, PASSIVE_C, 2,
                List.of(player(StatPoints.AGILITY, 12), player(StatPoints.WORTHINESS, 10)),
                List.of(Gate.modeBook(HamonPaths.STYLISH)), List.of()));
        nodes.add(armNeeding("snake_muffler", "snake_muffler", id("path_stylish"),
                Branch.UTILITY, PASSIVE_C, 3,
                List.of(player(StatPoints.AGILITY, 16)),
                List.of(Gate.hamonPath(HamonPaths.STYLISH)), List.of()));
        nodes.add(armNeeding("ripple_walk", "ripple_walk", id("snake_muffler"),
                Branch.MOBILITY, PASSIVE_C, 4,
                List.of(player(StatPoints.AGILITY, 20), player(StatPoints.ENDURANCE, 14)),
                List.of(Gate.hamonPath(HamonPaths.STYLISH)), List.of()));

        nodes.add(armNeeding("path_warrior", null, HAMON_ROOT, Branch.SPECIAL, MOVEMENT, 1,
                List.of(player(StatPoints.STRENGTH, 12), player(StatPoints.ENDURANCE, 10)),
                List.of(Gate.modeBook(HamonPaths.WARRIOR)), List.of()));
        nodes.add(armNeeding("turquoise_blue_overdrive", "turquoise_blue_overdrive",
                id("path_warrior"), Branch.COMBAT, MOVEMENT, 2,
                List.of(player(StatPoints.STRENGTH, 20), player(StatPoints.ENDURANCE, 16)),
                List.of(Gate.hamonPath(HamonPaths.WARRIOR)), List.of()));
        nodes.add(armNeeding("scarlet_overdrive", "scarlet_overdrive",
                id("turquoise_blue_overdrive"), Branch.COMBAT, MOVEMENT, 3,
                List.of(player(StatPoints.STRENGTH, 24), player(StatPoints.ENDURANCE, 18)),
                List.of(Gate.hamonPath(HamonPaths.WARRIOR)), List.of()));

        nodes.add(armNeeding("path_defender", null, HAMON_ROOT, Branch.SPECIAL, PASSIVE, 1,
                List.of(player(StatPoints.ENDURANCE, 12), player(StatPoints.STRENGTH, 10)),
                List.of(Gate.modeBook(HamonPaths.DEFENDER)), List.of()));
        nodes.add(armNeeding("thunder_cross_split_attack", "thunder_cross_split_attack", id("path_defender"),
                Branch.COMBAT, PASSIVE, 2,
                List.of(player(StatPoints.STRENGTH, 16), player(StatPoints.AGILITY, 12)),
                List.of(Gate.hamonPath(HamonPaths.DEFENDER)), List.of()));
        nodes.add(armNeeding("ripple_rebuke", "ripple_rebuke", id("thunder_cross_split_attack"),
                Branch.SPECIAL, PASSIVE, 3,
                List.of(player(StatPoints.ENDURANCE, 20), player(StatPoints.VITALITY, 14)),
                List.of(Gate.hamonPath(HamonPaths.DEFENDER)), List.of()));

        nodes.add(armNeeding("path_envious", null, HAMON_ROOT, Branch.SPECIAL, CHANNEL, 1,
                List.of(player(StatPoints.VITALITY, 12), player(StatPoints.WORTHINESS, 8)),
                List.of(Gate.modeBook(HamonPaths.ENVIOUS)), List.of()));
        nodes.add(armNeeding("youthful_breath", "youthful_breath", id("path_envious"),
                Branch.UTILITY, ENVIOUS_LEAN, 3,
                List.of(player(StatPoints.VITALITY, 16), player(StatPoints.ENDURANCE, 12)),
                List.of(Gate.hamonPath(HamonPaths.ENVIOUS)), List.of()));

        nodes.add(innate("ripple_breathing", "ripple_breathing", HAMON_ROOT, Branch.CORE, -1));
        nodes.add(innate("hermit_bloom", "hermit_bloom", HAMON_ROOT, Branch.UTILITY, 0));
        nodes.add(innate("held_breath", "held_breath", HAMON_ROOT, Branch.UTILITY, 1));

        return nodes;
    }

    private static List<SkillNode> vampireNodes() {
        List<SkillNode> nodes = new ArrayList<>();
        nodes.add(SkillNode.root(VAMPIRE_ROOT));

        nodes.add(armNeeding("drain", "drain", VAMPIRE_ROOT, Branch.COMBAT, COMBAT, 1,
                List.of(player(StatPoints.STRENGTH, 6)), List.of(Gate.TURNED), List.of()));

        nodes.add(armNeeding("vampiric_drive", "vampiric_drive", id("drain"),
                Branch.COMBAT, COMBAT, 2,
                List.of(player(StatPoints.STRENGTH, 16)), List.of(Gate.TURNED), List.of()));

        nodes.add(armNeeding("flesh_bud", "flesh_bud", id("vampiric_drive"),
                Branch.SPECIAL, COMBAT, 3,
                List.of(player(StatPoints.STRENGTH, 26), player(StatPoints.WORTHINESS, 14)),
                List.of(Gate.TURNED), List.of()));

        nodes.add(armNeeding("blitz", "blitz", VAMPIRE_ROOT, Branch.MOBILITY, BUILD, 1,
                List.of(player(StatPoints.AGILITY, 10)), List.of(Gate.TURNED), List.of()));

        nodes.add(armNeeding("subzero", "subzero", id("blitz"), Branch.SPECIAL, BUILD, 2,
                List.of(player(StatPoints.ENDURANCE, 18)), List.of(Gate.TURNED), List.of()));

        nodes.add(armNeeding("space_ripper_stingy_eyes", "space_ripper_stingy_eyes",
                id("subzero"), Branch.SPECIAL, BUILD, 3,
                List.of(player(StatPoints.ENDURANCE, 28), player(StatPoints.WORTHINESS, 20)),
                List.of(Gate.TURNED), List.of()));

        nodes.add(armNeeding("vaporous_freeze", "vaporous_freeze", id("subzero"),
                Branch.COMBAT, INHALE, 3,
                List.of(player(StatPoints.STRENGTH, 20), player(StatPoints.ENDURANCE, 18)),
                List.of(Gate.TURNED), List.of()));

        nodes.add(armNeeding("zombify", "zombify", VAMPIRE_ROOT, Branch.UTILITY, CHANNEL, 2,
                List.of(player(StatPoints.WORTHINESS, 12)),
                List.of(Gate.TURNED), List.of()));
        nodes.add(armNeeding("amalgamate", "amalgamate", id("zombify"), Branch.UTILITY, CHANNEL, 3,
                List.of(player(StatPoints.WORTHINESS, 22), player(StatPoints.ENDURANCE, 16)),
                List.of(Gate.TURNED), List.of()));

        nodes.add(armNeeding("wall_crawl", "wall_crawl", VAMPIRE_ROOT, Branch.MOBILITY, MOVEMENT, 1,
                List.of(player(StatPoints.AGILITY, 12)),
                List.of(Gate.TURNED), List.of()));

        nodes.add(armNeeding("regeneration", "regeneration", VAMPIRE_ROOT, Branch.SPECIAL, SPECIAL, 2,
                List.of(player(StatPoints.VITALITY, 20), player(StatPoints.ENDURANCE, 20)),
                List.of(Gate.TURNED), List.of()));

        nodes.add(innate("undead_physiology", "undead_physiology", VAMPIRE_ROOT, Branch.CORE, -2));
        nodes.add(innate("blood_mending", "blood_mending", VAMPIRE_ROOT, Branch.UTILITY, -1));
        nodes.add(innate("unbroken_fall", "unbroken_fall", VAMPIRE_ROOT, Branch.MOBILITY, 0));
        nodes.add(innate("sun_scorch", "sun_scorch", VAMPIRE_ROOT, Branch.SPECIAL, 1));

        return nodes;
    }

    private static List<SkillNode> pillarNodes() {
        List<SkillNode> nodes = new ArrayList<>();
        nodes.add(SkillNode.root(PILLAR_ROOT));

        nodes.add(pillar("infiltrate", PILLAR_ROOT, Branch.SPECIAL, SPECIAL, 1, null,
                player(StatPoints.STRENGTH, 14), player(StatPoints.VITALITY, 12)));
        nodes.add(pillar("rib_blades", id("infiltrate"), Branch.COMBAT, SPECIAL, 2, null,
                player(StatPoints.STRENGTH, 20), player(StatPoints.ENDURANCE, 14)));
        nodes.add(pillar("contortion", PILLAR_ROOT, Branch.MOBILITY, MOVEMENT, 1, null,
                player(StatPoints.AGILITY, 12)));

        HamonPath wind = HamonPaths.WIND_MODE;
        nodes.add(modePath(wind, PASSIVE_C));
        nodes.add(pillar("divine_sandstorm", id("path_wind_mode"), Branch.COMBAT, PASSIVE_C, 2, wind,
                player(StatPoints.STRENGTH, 18), player(StatPoints.AGILITY, 14)));
        nodes.add(pillar("atmospheric_rift", id("divine_sandstorm"), Branch.COMBAT, PASSIVE_C, 3, wind,
                player(StatPoints.STRENGTH, 28), player(StatPoints.WORTHINESS, 22)));
        nodes.add(pillar("gale_cut", id("path_wind_mode"), Branch.MOBILITY, BUILD, 2, wind,
                player(StatPoints.AGILITY, 16)));
        nodes.add(pillar("tornado", id("gale_cut"), Branch.COMBAT, BUILD, 3, wind,
                player(StatPoints.AGILITY, 22), player(StatPoints.WORTHINESS, 16)));
        nodes.add(pillar("wind_cloak", id("path_wind_mode"), Branch.UTILITY, CHANNEL, 2, wind,
                player(StatPoints.AGILITY, 18), player(StatPoints.ENDURANCE, 14)));

        HamonPath heat = HamonPaths.HEAT_MODE;
        nodes.add(modePath(heat, INHALE));
        nodes.add(pillar("boiling_blood_needles", id("path_heat_mode"), Branch.COMBAT, INHALE, 2, heat,
                player(StatPoints.STRENGTH, 18), player(StatPoints.VITALITY, 14)));
        nodes.add(pillar("blood_boil", id("boiling_blood_needles"), Branch.COMBAT, INHALE, 3, heat,
                player(StatPoints.STRENGTH, 26), player(StatPoints.WORTHINESS, 24)));
        nodes.add(pillar("vessel_grab", id("path_heat_mode"), Branch.UTILITY, PASSIVE, 2, heat,
                player(StatPoints.STRENGTH, 16)));
        nodes.add(pillar("heat_body", id("vessel_grab"), Branch.SPECIAL, PASSIVE, 3, heat,
                player(StatPoints.VITALITY, 20), player(StatPoints.ENDURANCE, 18)));
        nodes.add(pillar("crying_vent", id("heat_body"), Branch.UTILITY, PASSIVE, 4, heat,
                player(StatPoints.VITALITY, 26), player(StatPoints.ENDURANCE, 22)));

        HamonPath light = HamonPaths.LIGHT_MODE;
        nodes.add(modePath(light, COMBAT));
        nodes.add(pillar("brilliant_blades", id("path_light_mode"), Branch.COMBAT, COMBAT, 2, light,
                player(StatPoints.STRENGTH, 18), player(StatPoints.AGILITY, 14)));
        nodes.add(pillar("shining_slash", id("brilliant_blades"), Branch.MOBILITY, COMBAT, 3, light,
                player(StatPoints.AGILITY, 24), player(StatPoints.STRENGTH, 20)));
        nodes.add(pillar("blade_flash", id("path_light_mode"), Branch.SPECIAL, PASSIVE_B, 2, light,
                player(StatPoints.WORTHINESS, 16)));
        nodes.add(pillar("saw_grip", id("blade_flash"), Branch.COMBAT, PASSIVE_B, 3, light,
                player(StatPoints.STRENGTH, 24), player(StatPoints.ENDURANCE, 18)));

        nodes.add(innate("flesh_absorption", "flesh_absorption", PILLAR_ROOT, Branch.CORE, -2));
        nodes.add(innate("stone_in_sunlight", "stone_in_sunlight", PILLAR_ROOT, Branch.SPECIAL, -1));
        nodes.add(innate("wind_sense", "wind_sense", PILLAR_ROOT, Branch.UTILITY, 0));
        nodes.add(innate("boiling_blood", "boiling_blood", PILLAR_ROOT, Branch.COMBAT, 1));
        nodes.add(innate("blade_guard", "blade_guard", PILLAR_ROOT, Branch.SPECIAL, 2));

        return nodes;
    }

    private static SkillNode modePath(HamonPath mode, Arm on) {
        return armNeeding("path_" + mode.id().getPath(), null, PILLAR_ROOT, Branch.SPECIAL, on, 1,
                List.of(), List.of(Gate.PILLAR_MAN, Gate.modeBook(mode)), List.of());
    }

    private static SkillNode pillar(String skillId, ResourceLocation parent, Branch branch, Arm on, int ring,
                                    HamonPath mode, StatRequirement... stats) {
        List<Gate> gates = mode == null ? List.of(Gate.PILLAR_MAN) : List.of(Gate.PILLAR_MAN, Gate.hamonPath(mode));
        return armNeeding(skillId, skillId, parent, branch, on, ring, List.of(stats), gates, List.of());
    }

    private static List<SkillNode> ultimateNodes() {
        List<SkillNode> nodes = new ArrayList<>();
        nodes.add(SkillNode.root(ULTIMATE_ROOT));

        nodes.add(armNeeding("ultimate_strength", "ultimate_strength", ULTIMATE_ROOT, Branch.COMBAT, COMBAT, 1,
                List.of(player(StatPoints.STRENGTH, 14)), List.of(Gate.ULTIMATE), List.of()));

        nodes.add(armNeeding("adaptive_flight", "adaptive_flight", ULTIMATE_ROOT, Branch.MOBILITY, MOVEMENT, 1,
                List.of(player(StatPoints.AGILITY, 14)), List.of(Gate.ULTIMATE), List.of()));

        nodes.add(armNeeding("adapt", "adapt", ULTIMATE_ROOT, Branch.SPECIAL, SPECIAL, 1,
                List.of(player(StatPoints.WORTHINESS, 12)), List.of(Gate.ULTIMATE), List.of()));

        nodes.add(armNeeding("adapted_ability", "adapted_ability", id("adapt"), Branch.SPECIAL, SPECIAL, 2,
                List.of(player(StatPoints.WORTHINESS, 18)), List.of(Gate.ULTIMATE), List.of()));

        nodes.add(armNeeding("adapted_ability_2", "adapted_ability_2", id("adapted_ability"), Branch.SPECIAL, SPECIAL, 3,
                List.of(player(StatPoints.WORTHINESS, 22)), List.of(Gate.ULTIMATE), List.of()));

        nodes.add(armNeeding("adapted_ability_3", "adapted_ability_3", id("adapted_ability_2"), Branch.SPECIAL, SPECIAL, 4,
                List.of(player(StatPoints.WORTHINESS, 26)), List.of(Gate.ULTIMATE), List.of()));

        nodes.add(innate("ultimate_physiology", "undead_physiology", ULTIMATE_ROOT, Branch.CORE, -2));
        nodes.add(innate("ultimate_mending", "blood_mending", ULTIMATE_ROOT, Branch.UTILITY, -1));
        nodes.add(innate("ultimate_fall", "unbroken_fall", ULTIMATE_ROOT, Branch.MOBILITY, 0));
        nodes.add(innate("perfect_biology", "perfect_biology", ULTIMATE_ROOT, Branch.CORE, 1));
        nodes.add(innate("sun_immunity", "sun_immunity", ULTIMATE_ROOT, Branch.SPECIAL, 2));

        return nodes;
    }

    private static List<SkillNode> hermitPurpleTree() {
        List<SkillNode> nodes = new ArrayList<>();

        nodes.add(SkillNode.root(STAND_ROOT));

        nodes.add(arm("hermit_grapple", "hermit_grapple", STAND_ROOT, Branch.MOBILITY, MOVEMENT, 1,
                stand(SPEED, 5)));

        nodes.add(arm("thorn_zip", "thorn_zip", id("hermit_grapple"), Branch.MOBILITY, MOVEMENT, 2,
                stand(SPEED, 12)));

        nodes.add(arm("lasso_of_thorns", "lasso_of_thorns", STAND_ROOT, Branch.COMBAT, COMBAT, 1,
                stand(POWER, 6)));

        nodes.add(arm("camera_crush", "camera_crush", STAND_ROOT, Branch.SPECIAL, SPECIAL, 2,
                stand(POTENTIAL, 10)));

        nodes.add(arm("clairvoyance", "clairvoyance", id("camera_crush"), Branch.SPECIAL, SPECIAL, 3,
                stand(POTENTIAL, 18)));

        nodes.add(arm("thorn_whip", "thorn_whip", id("lasso_of_thorns"), Branch.COMBAT, COMBAT, 2,
                stand(POWER, 14)));

        nodes.add(arm("twisting_gut_punch", "twisting_gut_punch", id("thorn_whip"),
                Branch.COMBAT, COMBAT, 3, stand(POWER, 22)));

        nodes.add(arm("dowsing", "dowsing", STAND_ROOT, Branch.UTILITY, BUILD, 1,
                stand(POTENTIAL, 6)));

        nodes.add(innate("grappling_vine", "grappling_vine", STAND_ROOT, Branch.MOBILITY, -3));
        nodes.add(innate("tangled", "tangled", STAND_ROOT, Branch.SPECIAL, -1));
        nodes.add(innate("thornbind", "thornbind", STAND_ROOT, Branch.COMBAT, 0));
        nodes.add(innate("second_sight", "second_sight", STAND_ROOT, Branch.SPECIAL, 1));

        nodes.add(innate("ripple_conduction", "ripple_conduction", STAND_ROOT, Branch.CORE, 4));

        nodes.add(innate("made_for_this", "made_for_this", STAND_ROOT, Branch.SPECIAL, 2));
        nodes.add(innate("unorthodox_method", "unorthodox_method", STAND_ROOT,
                Branch.SPECIAL, 3));

        nodes.add(shelfUpgrade("grappling_vine_ii", "grappling_vine_ii", id("grappling_vine"),
                Branch.MOBILITY, -2, stand(POTENTIAL, 20)));

        return nodes;
    }

    private static List<SkillNode> hierophantGreenTree() {
        List<SkillNode> nodes = universalStandNodes();

        nodes.add(arm("emerald_splash_shotgun_blast", "emerald_splash_shotgun_blast", id("uppercut"),
                Branch.COMBAT, COMBAT, 2, stand(POWER, 16)));
        nodes.add(arm("emerald_splash_barrage", "emerald_splash_barrage", id("emerald_splash_shotgun_blast"),
                Branch.COMBAT, COMBAT, 3, stand(POWER, 24)));
        nodes.add(arm("tendril_spike", "tendril_spike", id("grab"), Branch.COMBAT, INHALE, 2,
                stand(POWER, 14)));

        nodes.add(arm("web_swing", "web_swing", STAND_ROOT, Branch.MOBILITY, MOVEMENT, 1,
                stand(SPEED, 10)));

        nodes.add(arm("unravel_trap", "unravel_trap", STAND_ROOT, Branch.SPECIAL, CHANNEL, 2,
                stand(POTENTIAL, 10)));
        nodes.add(arm("miniature_trap", "miniature_trap", id("unravel_trap"), Branch.SPECIAL, CHANNEL, 3,
                stand(POTENTIAL, 14)));

        nodes.add(arm("body_possession", "body_possession", STAND_ROOT, Branch.SPECIAL, SPECIAL, 2,
                stand(POTENTIAL, 20)));
        nodes.add(armNeeding("twenty_meter_emerald_splash", "twenty_meter_emerald_splash", id("body_possession"),
                Branch.SPECIAL, SPECIAL, 3, List.of(stand(POTENTIAL, 30), stand(POWER, 24)),
                List.of(), List.of(id("emerald_splash_barrage"))));

        nodes.add(innate("deadeye", "deadeye", STAND_ROOT, Branch.SPECIAL, -2));
        nodes.add(innate("opportunist", "opportunist", STAND_ROOT, Branch.SPECIAL, -1));
        nodes.add(innate("splash_fragments", "splash_fragments", STAND_ROOT, Branch.SPECIAL, 1));
        nodes.add(innate("rebound", "rebound", STAND_ROOT, Branch.SPECIAL, 2));
        return nodes;
    }

    private static List<SkillNode> silverChariotTree() {
        List<SkillNode> nodes = universalStandNodes();

        nodes.add(arm("balestra", "balestra", id("uppercut"), Branch.COMBAT, COMBAT, 2,
                stand(POWER, 14), stand(SPEED, 12)));
        nodes.add(arm("blade_launch", "blade_launch", id("balestra"), Branch.COMBAT, COMBAT, 3,
                stand(POWER, 20), stand(SPEED, 16)));
        nodes.add(arm("sabre_cycle", "sabre_cycle", id("grab"), Branch.COMBAT, PASSIVE, 2,
                stand(POWER, 18)));

        nodes.add(arm("deflection", "deflection", STAND_ROOT, Branch.SPECIAL, CHANNEL, 2,
                stand(PROTECTION, 10), stand(SPEED, 8)));
        nodes.add(arm("riposte", "riposte", id("deflection"), Branch.SPECIAL, CHANNEL, 3,
                stand(SPEED, 18), stand(PROTECTION, 14)));

        nodes.add(arm("armor_release", "armor_release", STAND_ROOT, Branch.SPECIAL, SPECIAL, 2,
                stand(SPEED, 14), stand(POTENTIAL, 10)));
        nodes.add(arm("afterimages", "afterimages", id("armor_release"), Branch.SPECIAL, SPECIAL, 3,
                stand(SPEED, 22), stand(POTENTIAL, 16)));

        nodes.add(innate("piercing_thrust", "piercing_thrust", STAND_ROOT, Branch.SPECIAL, -2));
        nodes.add(innate("enhanced_reflexes", "enhanced_reflexes", STAND_ROOT, Branch.SPECIAL, -1));
        nodes.add(innate("unwavering", "unwavering", STAND_ROOT, Branch.SPECIAL, 1));
        nodes.add(innate("duelists_focus", "duelists_focus", STAND_ROOT, Branch.SPECIAL, 2));
        return nodes;
    }

    private static List<SkillNode> magiciansRedTree() {
        List<SkillNode> nodes = universalStandNodes();

        nodes.add(arm("sweep_kick", "sweep_kick", id("uppercut"), Branch.COMBAT, COMBAT, 2,
                stand(POWER, 12)));
        nodes.add(arm("crossfire_hurricane", "crossfire_hurricane", id("sweep_kick"), Branch.COMBAT, COMBAT, 3,
                stand(POWER, 18)));
        nodes.add(arm("crossfire_hurricane_special", "crossfire_hurricane_special", id("crossfire_hurricane"),
                Branch.COMBAT, COMBAT, 4, stand(POWER, 26), stand(POTENTIAL, 16)));
        nodes.add(arm("red_bind", "red_bind", id("grab"), Branch.COMBAT, PASSIVE, 2,
                stand(POWER, 14)));

        nodes.add(arm("controlled_blaze", "controlled_blaze", STAND_ROOT, Branch.SPECIAL, CHANNEL, 2,
                stand(POTENTIAL, 10)));
        nodes.add(arm("inferno", "inferno", id("controlled_blaze"), Branch.SPECIAL, CHANNEL, 3,
                stand(POTENTIAL, 16), stand(POWER, 16)));

        nodes.add(arm("containment", "containment", id("inferno"), Branch.SPECIAL, CHANNEL, 4,
                stand(POTENTIAL, 20)));

        nodes.add(arm("life_sense", "life_sense", STAND_ROOT, Branch.SPECIAL, SPECIAL, 2,
                stand(POTENTIAL, 8)));
        nodes.add(arm("increase_temperature", "increase_temperature", id("life_sense"), Branch.SPECIAL, SPECIAL, 3,
                stand(POTENTIAL, 14)));
        nodes.add(armNeeding("infinite_inferno_fire", "infinite_inferno_fire", id("increase_temperature"),
                Branch.SPECIAL, SPECIAL, 4, List.of(stand(POTENTIAL, 30), stand(POWER, 24)),
                List.of(), List.of(id("inferno"))));

        nodes.add(innate("territorial", "territorial", STAND_ROOT, Branch.SPECIAL, -2));
        nodes.add(innate("encroachment", "encroachment", STAND_ROOT, Branch.SPECIAL, -1));
        nodes.add(innate("flame_immunity", "flame_immunity", STAND_ROOT, Branch.SPECIAL, 1));
        nodes.add(innate("burning_pressure", "burning_pressure", STAND_ROOT, Branch.SPECIAL, 2));
        nodes.add(innate("finger_snap", "finger_snap", STAND_ROOT, Branch.SPECIAL, 3));
        return nodes;
    }

    private static List<SkillNode> anubisTree() {
        List<SkillNode> nodes = new ArrayList<>();
        nodes.add(SkillNode.root(STAND_ROOT));
        nodes.add(innate("made_for_this", "made_for_this", STAND_ROOT, Branch.SPECIAL, -2));
        nodes.add(innate("unorthodox_method", "unorthodox_method", STAND_ROOT, Branch.SPECIAL, -1));
        nodes.add(innate("draw", "draw", STAND_ROOT, Branch.COMBAT, 0));
        nodes.add(innate("swordsmanship", "swordsmanship", STAND_ROOT, Branch.COMBAT, 1));
        nodes.add(innate("combat_adaptation", "combat_adaptation", STAND_ROOT, Branch.SPECIAL, 2));
        return nodes;
    }

    private record Arm(double degrees, double sweep) {
    }

    private static final double SPOKE_STEP = 25;
    private static final double SPOKE_CENTRE = 90;

    private static final double SPOKE_SQUASH = 0.78;

    private static Arm spoke(int fromCentre, double sweep) {
        return new Arm(SPOKE_CENTRE + fromCentre * SPOKE_STEP, sweep);
    }

    private static final Arm COMBAT = spoke(4, -6);

    private static final Arm PASSIVE_B = spoke(3, 0);

    private static final Arm INHALE = spoke(2, 0);
    private static final Arm PASSIVE = spoke(1, 0);
    private static final Arm SPECIAL = spoke(0, 0);
    private static final Arm MOVEMENT = spoke(-1, 4);

    private static final Arm CHANNEL = spoke(-2, 0);
    private static final Arm PASSIVE_C = spoke(-3, 0);
    private static final Arm BUILD = spoke(-4, 6);

    private static final Arm ENVIOUS_LEAN = new Arm(SPOKE_CENTRE - 1.4 * SPOKE_STEP, 0);

    private static final int[] RINGS = {0, 50, 82, 114, 146, 178};

    private static int fanX(Arm on, int ring) {
        return (int) Math.round(RINGS[ring] * Math.cos(Math.toRadians(angle(on, ring))));
    }

    private static int fanY(Arm on, int ring) {
        return (int) Math.round(-RINGS[ring] * Math.sin(Math.toRadians(angle(on, ring)))
                * SPOKE_SQUASH);
    }

    private static double angle(Arm on, int ring) {
        return on.degrees() + on.sweep() * (ring - 1);
    }

    private static SkillNode arm(String nodeId, String skillId, ResourceLocation parent,
                                 Branch branch, Arm on, int ring, StatRequirement... stats) {
        return new SkillNode(id(nodeId), skillId == null ? null : id(skillId), parent, branch,
                fanX(on, ring), fanY(on, ring), List.of(stats), List.of(), List.of(), List.of(),
                false);
    }

    private static SkillNode innate(String nodeId, String passiveId, ResourceLocation root,
                                    Branch branch, int column) {
        return SkillNode.innate(id(nodeId), id(passiveId), root, branch,
                column * SHELF_COLUMN, SHELF_Y);
    }

    private static SkillNode shelfUpgrade(String nodeId, String passiveId, ResourceLocation parent,
                                          Branch branch, int column, StatRequirement... stats) {
        return new SkillNode(id(nodeId), id(passiveId), parent, branch,
                column * SHELF_COLUMN, SHELF_Y, List.of(stats), List.of(), List.of(), List.of(),
                false);
    }

    private static final int SHELF_Y = 42;
    private static final int SHELF_COLUMN = 34;

    private static SkillNode armNeeding(String nodeId, String skillId, ResourceLocation parent,
                                        Branch branch, Arm on, int ring, List<StatRequirement> stats,
                                        List<Gate> gates, List<ResourceLocation> requires) {
        return new SkillNode(id(nodeId), skillId == null ? null : id(skillId), parent, branch,
                fanX(on, ring), fanY(on, ring), stats, List.of(), gates, requires, false);
    }

    private static StatRequirement stand(int stat, int minimum) {
        return new StatRequirement(true, stat, minimum);
    }

    private static StatRequirement player(int stat, int minimum) {
        return new StatRequirement(false, stat, minimum);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, path);
    }

    private static final Map<ResourceLocation, List<SkillNode>> STAND_TREES = new HashMap<>();

    private static void registerStand(ResourceLocation standId, List<SkillNode> nodes) {
        STAND_TREES.put(standId, nodes);
        index(nodes);
    }

    public static List<SkillNode> forStand(ResourceLocation standId) {
        List<SkillNode> own = standId == null ? null : STAND_TREES.get(standId);
        return own != null ? own : of(Tree.STAND);
    }

    public static Set<ResourceLocation> standsWithTrees() {
        return STAND_TREES.keySet();
    }

    public static Set<ResourceLocation> allStandNodeIds() {
        Set<ResourceLocation> ids = new HashSet<>();
        for (SkillNode node : of(Tree.STAND)) {
            ids.add(node.id());
        }
        for (List<SkillNode> tree : STAND_TREES.values()) {
            for (SkillNode node : tree) {
                ids.add(node.id());
            }
        }
        return ids;
    }

    private static void register(Tree tree, List<SkillNode> nodes) {
        TREES.put(tree, nodes);
        index(nodes);
    }

    private static void index(List<SkillNode> nodes) {
        for (SkillNode node : nodes) {
            BY_ID.put(node.id(), node);
            if (node.skill() != null) {
                BY_SKILL.computeIfAbsent(node.skill(), key -> new ArrayList<>()).add(node);
            }
        }
    }

    public static List<SkillNode> of(Tree tree) {
        return TREES.getOrDefault(tree, List.of());
    }

    public static List<SkillNode> of(Tree tree, ResourceLocation standId) {
        return tree == Tree.STAND ? forStand(standId) : of(tree);
    }

    public static SkillNode byId(ResourceLocation nodeId) {
        return BY_ID.get(nodeId);
    }

    public static boolean owned(JojohaPlayerData data, SkillNode node) {
        if (node == null) {
            return false;
        }
        if (node.innate() || data.hasNode(node.id())) {
            return true;
        }

        HamonPath path = pathOf(node);
        return path != null && data.hasHamonPath(path.id());
    }

    private static final String PATH_NODE = "path_";

    public static HamonPath pathOf(SkillNode node) {
        if (node == null || node.skill() != null || !node.id().getPath().startsWith(PATH_NODE)) {
            return null;
        }
        return HamonPaths.byId(ResourceLocation.fromNamespaceAndPath(
                Jojoha.MOD_ID, node.id().getPath().substring(PATH_NODE.length())));
    }

    public static boolean owned(JojohaPlayerData data, ResourceLocation nodeId) {
        return owned(data, byId(nodeId));
    }

    public static SkillNode forSkill(ResourceLocation skillId) {
        List<SkillNode> nodes = BY_SKILL.get(skillId);
        return nodes == null || nodes.isEmpty() ? null : nodes.get(0);
    }

    public static boolean firstOnStandTree(ResourceLocation skillId) {
        List<SkillNode> nodes = BY_SKILL.get(skillId);
        if (nodes == null) {
            return false;
        }
        for (SkillNode node : nodes) {
            if (STAND_ROOT.equals(node.parent())) {
                return true;
            }
        }
        return false;
    }

    public static boolean skillUnlocked(JojohaPlayerData data, ResourceLocation skillId) {
        List<SkillNode> nodes = BY_SKILL.get(skillId);
        if (nodes == null || nodes.isEmpty()) {
            return true;
        }
        for (SkillNode node : nodes) {
            if (active(data, node)) {
                return true;
            }
        }
        return false;
    }

    public static boolean relock(JojohaPlayerData data) {
        boolean any = false;
        boolean changed = true;
        while (changed) {
            changed = false;
            for (ResourceLocation id : List.copyOf(data.getUnlockedNodes())) {
                SkillNode node = byId(id);
                if (node == null || node.innate()) {
                    continue;
                }
                if (!statsMet(data, node) || !parentDone(data, node)) {
                    data.getUnlockedNodes().remove(id);
                    changed = true;
                    any = true;
                }
            }
        }
        return any;
    }

    public static boolean active(JojohaPlayerData data, SkillNode node) {
        for (int depth = 0; node != null && depth < 64; depth++) {
            if (!owned(data, node) || (!node.innate() && !statsMet(data, node))) {
                return false;
            }
            if (node.isRoot()) {
                return true;
            }
            node = byId(node.parent());
        }
        return node == null;
    }

    public static boolean parentDone(JojohaPlayerData data, SkillNode node) {
        if (!node.isRoot() && !owned(data, node.parent())) {
            return false;
        }

        List<ResourceLocation> also = node.requires();
        for (int i = 0; i < also.size(); i++) {
            if (!owned(data, also.get(i))) {
                return false;
            }
        }
        return true;
    }

    public static List<SkillNode> missingPrerequisites(JojohaPlayerData data, SkillNode node) {
        List<SkillNode> missing = new ArrayList<>();
        if (!node.isRoot() && !owned(data, node.parent())) {
            SkillNode parent = byId(node.parent());
            if (parent != null) {
                missing.add(parent);
            }
        }
        for (ResourceLocation also : node.requires()) {
            if (!owned(data, also)) {
                SkillNode other = byId(also);
                if (other != null) {
                    missing.add(other);
                }
            }
        }
        return missing;
    }

    public static List<StatRequirement> unmetStats(JojohaPlayerData data, SkillNode node) {
        List<StatRequirement> unmet = new ArrayList<>();
        for (StatRequirement requirement : node.stats()) {
            if (!requirement.met(data)) {
                unmet.add(requirement);
            }
        }
        return unmet;
    }

    public static List<Gate> unmetGates(JojohaPlayerData data, SkillNode node) {
        List<Gate> unmet = new ArrayList<>();
        for (Gate gate : node.gates()) {
            if (!gate.met(data)) {
                unmet.add(gate);
            }
        }
        return unmet;
    }

    public static List<ItemRequirement> unmetItems(Player player, SkillNode node) {
        List<ItemRequirement> unmet = new ArrayList<>();
        for (ItemRequirement requirement : node.items()) {
            if (!requirement.met(player)) {
                unmet.add(requirement);
            }
        }
        return unmet;
    }

    public static boolean readyToUnlock(JojohaPlayerData data, SkillNode node) {
        return node != null
                && !node.innate()
                && !owned(data, node)
                && parentDone(data, node)
                && statsMet(data, node)
                && gatesMet(data, node);
    }

    public static boolean statsMet(JojohaPlayerData data, SkillNode node) {
        List<StatRequirement> stats = node.stats();
        for (int i = 0; i < stats.size(); i++) {
            if (!stats.get(i).met(data)) {
                return false;
            }
        }
        return true;
    }

    public static boolean gatesMet(JojohaPlayerData data, SkillNode node) {
        List<Gate> gates = node.gates();
        for (int i = 0; i < gates.size(); i++) {
            if (!gates.get(i).met(data)) {
                return false;
            }
        }
        return true;
    }

    public static boolean canUnlock(Player player, JojohaPlayerData data, SkillNode node) {
        return readyToUnlock(data, node) && unmetItems(player, node).isEmpty();
    }
    static {
        registerStand(StandTypes.STAR_PLATINUM_ID, starPlatinumTree());
        registerStand(StandTypes.HERMIT_PURPLE_ID, hermitPurpleTree());
        registerStand(StandTypes.THE_WORLD_ID, theWorldTree());
        registerStand(StandTypes.HIEROPHANT_GREEN_ID, hierophantGreenTree());
        registerStand(StandTypes.SILVER_CHARIOT_ID, silverChariotTree());
        registerStand(StandTypes.MAGICIANS_RED_ID, magiciansRedTree());
        registerStand(StandTypes.ANUBIS_ID, anubisTree());
        registerStand(StandTypes.SECRET_THE_WORLD_ID, secretTheWorldTree());

        register(Tree.STAND, universalStandNodes());

        register(Tree.HAMON, hamonNodes());
        register(Tree.VAMPIRE, vampireNodes());
        register(Tree.ULTIMATE, ultimateNodes());
        register(Tree.PILLAR, pillarNodes());

        register(Tree.PLAYER, playerNodes());
    }
}
