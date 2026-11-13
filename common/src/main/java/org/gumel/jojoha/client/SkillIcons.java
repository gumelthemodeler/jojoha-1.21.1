package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.gumel.jojoha.Jojoha;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class SkillIcons {
    private static final Map<String, String> MOVES = Map.ofEntries(
            Map.entry("barrage", "stand/shared/moves/stand_barrage_icon.png"),
            Map.entry("stand_dash", "stand/shared/moves/stand_dash_icon.png"),
            Map.entry("stand_leap", "stand/shared/moves/stand_leap_icon.png"),
            Map.entry("uppercut", "stand/shared/moves/stand_uppercut_icon.png"),
            Map.entry("pilot", "stand/shared/moves/stand_pilot_icon.png"),
            Map.entry("inhale", "stand/star_platinum/moves/starplatinum_inhale_icon.png"),
            Map.entry("star_finger", "stand/star_platinum/moves/starplatinum_starfinger_icon.png"),
            Map.entry("time_stop", "stand/shared/moves/timestop_icon.png"),
            Map.entry("time_stop_extended", "stand/shared/moves/extended_timestop_icon.png"),
            Map.entry("heaven_time_stop", "stand/shared/moves/extended_timestop_icon.png"),
            Map.entry("time_skip", "stand/shared/moves/timeskip_icon.png"),
            Map.entry("skull_crusher", "stand/shared/moves/skull_crusher_icon.png"),
            Map.entry("hermit_grapple", "stand/hermit_purple/moves/hermit_purple_grapple_icon.png"),
            Map.entry("thorn_zip", "stand/hermit_purple/moves/hermit_purple_thorn_zip.png"),
            Map.entry("lasso_of_thorns", "stand/hermit_purple/moves/hermit_purple_lasso_of_thorns.png"),
            Map.entry("camera_crush", "stand/hermit_purple/moves/hermit_purple_camera_crush_icon.png"),
            Map.entry("thorn_whip", "stand/hermit_purple/moves/hermit_purple_thorn_wip_icon.png"),
            Map.entry("twisting_gut_punch", "stand/hermit_purple/moves/hermit_purple_twisting_gut_punch_icon.png"),
            Map.entry("grab", "stand/shared/moves/stand_grab.png"),
            Map.entry("clairvoyance", "stand/hermit_purple/moves/clairvoyance.png"),

            Map.entry("ground_breaker", "stand/shared/moves/ground_breaker.png"),
            Map.entry("knife_barrage", "stand/the_world/moves/knife_barrage.png"),
            Map.entry("kick_flurry", "stand/the_world/moves/kick_flurry.png"),
            Map.entry("beatdown", "stand/shared/moves/beatdown.png"),
            Map.entry("time_stop_perfect", "stand/the_world/moves/timestop_icon_perfect.png"),

            Map.entry("emerald_splash_shotgun_blast", "stand/hierophant_green/moves/emerald_splash_shotgun_blast.png"),
            Map.entry("emerald_splash_barrage", "stand/hierophant_green/moves/emerald_splash_barrage.png"),
            Map.entry("tendril_spike", "stand/hierophant_green/moves/tendril_spike.png"),
            Map.entry("web_swing", "stand/hierophant_green/moves/web_swing.png"),
            Map.entry("unravel_trap", "stand/hierophant_green/moves/unravel_trap.png"),
            Map.entry("miniature_trap", "stand/hierophant_green/moves/miniature_trap.png"),
            Map.entry("body_possession", "stand/hierophant_green/moves/body_possession.png"),
            Map.entry("twenty_meter_emerald_splash", "stand/hierophant_green/moves/20_meter_radius_emerald_splash.png"),

            Map.entry("sword_flurry", "stand/silver_chariot/moves/sword_flurry.png"),
            Map.entry("balestra", "stand/silver_chariot/moves/balestra.png"),
            Map.entry("blade_launch", "stand/silver_chariot/moves/blade_launch.png"),
            Map.entry("sabre_cycle", "stand/silver_chariot/moves/sabre_cycle.png"),
            Map.entry("deflection", "stand/silver_chariot/moves/deflection.png"),
            Map.entry("riposte", "stand/silver_chariot/moves/riposte.png"),
            Map.entry("armor_release", "stand/silver_chariot/moves/armor_release.png"),
            Map.entry("afterimages", "stand/silver_chariot/moves/afterimages.png"),

            Map.entry("two_meters", "stand/star_platinum/passives/two_meters.png"),
            Map.entry("sensory_perception", "stand/star_platinum/passives/sensory_perception.png"),
            Map.entry("enhanced_reflexes", "stand/shared/passives/enhanced_reflexes.png"),
            Map.entry("unwavering", "stand/shared/passives/unwavering.png"),
            Map.entry("master_of_time", "stand/the_world/passives/master_of_time.png"),
            Map.entry("crushing_force", "stand/the_world/passives/crushing_force.png"),

            Map.entry("grappling_vine", "stand/hermit_purple/passives/grapple_vine.png"),
            Map.entry("grappling_vine_ii", "stand/hermit_purple/passives/grapple_vine_ii.png"),
            Map.entry("tangled", "stand/hermit_purple/passives/tangled.png"),
            Map.entry("thornbind", "stand/hermit_purple/passives/thornbind.png"),
            Map.entry("second_sight", "stand/hermit_purple/passives/second_sight.png"),
            Map.entry("ripple_conduction", "stand/hermit_purple/passives/ripple_conduction.png"),
            Map.entry("made_for_this", "stand/hermit_purple/passives/made_for_this.png"),
            Map.entry("unorthodox_method", "stand/hermit_purple/passives/unorthodox_method.png"),

            Map.entry("build_single", "stand/utility/stand_utility_single_icon.png"),
            Map.entry("build_row", "stand/utility/stand_utility_row_icon.png"),
            Map.entry("build_column", "stand/utility/stand_utility_column_icon.png"),
            Map.entry("build_free", "stand/utility/stand_utility_free_icon.png"),
            Map.entry("build_selection", "stand/utility/stand_utility_selection_icon.png"),
            Map.entry("recall_stand", "stand/utility/stand_utility_return_icon.png"),
            Map.entry("stand_fetch", "stand/utility/fetch_icon.png"),
            Map.entry("stand_harvest", "stand/utility/harvest_icon.png"),
            Map.entry("stand_reach", "stand/utility/reach_icon.png"),
            Map.entry("stand_fell", "stand/utility/fell_icon.png"),
            Map.entry("stand_carry", "stand/utility/carry_icon.png"),

            Map.entry("precise_eyes", "stand/star_platinum/moves/precise_eyes.png"),
            Map.entry("delivery", "stand/the_world/moves/delivery.png"),
            Map.entry("dowsing", "stand/hermit_purple/moves/dowsing.png"),
            Map.entry("tendril_work", "stand/hierophant_green/moves/tendril_work.png"),
            Map.entry("clearing", "stand/silver_chariot/moves/clearing.png"),

            Map.entry("muda_rush", "stand/the_world/moves/muda_rush.png"),
            Map.entry("heavens_uppercut", "stand/the_world/moves/h_uppercut.png"),

            Map.entry("controlled_blaze", "stand/magicians_red/moves/controlled_blaze.png"),
            Map.entry("sweep_kick", "stand/magicians_red/moves/sweep_kick.png"),
            Map.entry("crossfire_hurricane", "stand/magicians_red/moves/crossfire_hurricane.png"),
            Map.entry("crossfire_hurricane_special", "stand/magicians_red/moves/crossfire_hurricane_special.png"),
            Map.entry("red_bind", "stand/magicians_red/moves/red_bind.png"),
            Map.entry("infinite_inferno_fire", "stand/magicians_red/moves/infinite_inferno_fire.png"),
            Map.entry("inferno", "stand/magicians_red/moves/inferno.png"),
            Map.entry("life_sense", "stand/magicians_red/moves/life_sense.png"),
            Map.entry("increase_temperature", "stand/magicians_red/moves/increase_temperature.png"),
            Map.entry("containment", "stand/magicians_red/moves/containment.png"),
            Map.entry("kiln", "stand/magicians_red/moves/kiln_icon.png"),
            Map.entry("burning_pressure", "stand/magicians_red/passives/burning_pressure.png"),
            Map.entry("encroachment", "stand/magicians_red/passives/encroachment.png"),
            Map.entry("flame_immunity", "stand/magicians_red/passives/flame_immunity.png"),
            Map.entry("territorial", "stand/magicians_red/passives/territorial.png"),

            Map.entry("ripple_pulse", "hamon/moves/ripple_pulse.png"),
            Map.entry("zoom_punch", "hamon/moves/hamon_zoom_punch.png"),
            Map.entry("hamon_overdrive", "hamon/moves/hamon_overdrive.png"),
            Map.entry("ripple_barrier", "hamon/moves/hamon_ripple_barrier.png"),
            Map.entry("channeling", "hamon/moves/channeling.png"),
            Map.entry("sunlight_yellow_overdrive",
                    "hamon/moves/hamon_sunlight_yellow_overdrive.png"),

            Map.entry("clacker_volley", "hamon/moves/clacker_volley.png"),
            Map.entry("forced_ripple_breathing", "hamon/moves/forced_ripple_breathing.png"),
            Map.entry("bubble_cutter", "hamon/moves/bubble_cutter.png"),
            Map.entry("bubble_launcher", "hamon/moves/bubble_launcher.png"),
            Map.entry("snake_muffler", "hamon/moves/snake_muffler.png"),
            Map.entry("ripple_walk", "hamon/moves/ripple_walk.png"),
            Map.entry("turquoise_blue_overdrive", "hamon/moves/turquoise_blue_overdrive.png"),

            Map.entry("ripple_breathing", "hamon/passives/ripple_breathing.png"),
            Map.entry("hermit_bloom", "hamon/passives/bloom.png"),
            Map.entry("held_breath", "hamon/passives/held_breath.png"),
            Map.entry("thunder_cross_split_attack", "hamon/moves/thunder_cross_split_attack.png"),
            Map.entry("ripple_rebuke", "hamon/moves/ripple_rebuke.png"),
            Map.entry("next_line", "hamon/moves/next_line.png"),
            Map.entry("youthful_breath", "hamon/moves/youthful_breath.png"),
            Map.entry("scarlet_overdrive", "hamon/moves/scarlet_overdrive.png"),
            Map.entry("bottle_cap_shot", "hamon/moves/bottle_cap_shot.png"),

            Map.entry("drain", "vampire/moves/drain.png"),
            Map.entry("vampiric_drive", "vampire/moves/vampiric_drive.png"),
            Map.entry("vaporous_freeze", "vampire/moves/vaporous_freeze.png"),
            Map.entry("zombify", "vampire/moves/zombify.png"),
            Map.entry("amalgamate", "vampire/moves/amalgamate.png"),
            Map.entry("wall_crawl", "vampire/moves/wall_crawl.png"),
            Map.entry("regeneration", "vampire/moves/regeneration.png"),
            Map.entry("flesh_bud", "vampire/moves/flesh_bud.png"),
            Map.entry("blitz", "vampire/moves/blitz.png"),
            Map.entry("subzero", "vampire/moves/subzero.png"),
            Map.entry("space_ripper_stingy_eyes",
                    "vampire/moves/spaceripper_stingy_eyes.png"),
            Map.entry("undead_physiology", "vampire/passives/undead_physiology.png"),
            Map.entry("blood_mending", "vampire/passives/blood_mending.png"),
            Map.entry("unbroken_fall", "vampire/passives/unbroken.png"),
            Map.entry("sun_scorch", "vampire/passives/sun_scorch.png"),

            Map.entry("infiltrate", "pillar_man/moves/infiltrate.png"),
            Map.entry("rib_blades", "pillar_man/moves/rib_blades.png"),
            Map.entry("contortion", "pillar_man/moves/contortion.png"),
            Map.entry("divine_sandstorm", "pillar_man/moves/divine_sandstorm.png"),
            Map.entry("atmospheric_rift", "pillar_man/moves/atmospheric_rift.png"),
            Map.entry("gale_cut", "pillar_man/moves/gale_cut.png"),
            Map.entry("tornado", "pillar_man/moves/tornado.png"),
            Map.entry("wind_cloak", "pillar_man/moves/wind_cloak.png"),
            Map.entry("boiling_blood_needles", "pillar_man/moves/boiling_blood_needles.png"),
            Map.entry("blood_boil", "pillar_man/moves/blood_boil.png"),
            Map.entry("vessel_grab", "pillar_man/moves/vessel_grab.png"),
            Map.entry("heat_body", "pillar_man/moves/heat_body.png"),
            Map.entry("crying_vent", "pillar_man/moves/crying_vent.png"),
            Map.entry("brilliant_blades", "pillar_man/moves/brilliant_blades.png"),
            Map.entry("shining_slash", "pillar_man/moves/shining_slash.png"),
            Map.entry("blade_flash", "pillar_man/moves/blade_flash.png"),
            Map.entry("saw_grip", "pillar_man/moves/saw_grip.png"),
            Map.entry("flesh_absorption", "pillar_man/passives/flesh_absorption.png"),
            Map.entry("stone_in_sunlight", "pillar_man/passives/stone_in_sunlight.png"),
            Map.entry("wind_sense", "pillar_man/passives/wind_sense.png"),
            Map.entry("boiling_blood", "pillar_man/passives/boiling_blood.png"),
            Map.entry("blade_guard", "pillar_man/passives/blade_guard.png"),

            Map.entry("adapt", "ultimate_lifeform/moves/adapt.png"),
            Map.entry("adapted_ability", "ultimate_lifeform/moves/adapted_ability.png"),
            Map.entry("adapted_ability_2", "ultimate_lifeform/moves/adapted_ability_ii.png"),
            Map.entry("adapted_ability_3", "ultimate_lifeform/moves/adapted_ability_iii.png"),
            Map.entry("adaptive_flight", "ultimate_lifeform/moves/adaptive_flight.png"),
            Map.entry("ultimate_strength", "ultimate_lifeform/moves/ultimate_strength.png"),
            Map.entry("perfect_biology", "ultimate_lifeform/passives/perfect_biology.png"),
            Map.entry("sun_immunity", "ultimate_lifeform/passives/sun_immunity.png"),

            Map.entry("piercing_thrust", "stand/silver_chariot/passives/piercing_thrust.png"),
            Map.entry("duelists_focus", "stand/silver_chariot/passives/duelists_focus.png"),
            Map.entry("deadeye", "stand/shared/deadeye.png"),
            Map.entry("opportunist", "stand/shared/opportunist.png"),
            Map.entry("splash_fragments", "stand/hierophant_green/passives/emerald_splash.png"),
            Map.entry("rebound", "stand/hierophant_green/passives/rebound.png"),

            Map.entry("finger_snap", "stand/magicians_red/moves/finger_snap.png"),
            Map.entry("draw", "stand/anubis/moves/draw.png"),
            Map.entry("swordsmanship", "stand/anubis/passives/swordsmanship.png"),
            Map.entry("combat_adaptation", "stand/anubis/passives/combat_adaptation.png"),
            Map.entry("greatest_high", "stand/the_world_over_heaven/moves/greatest_high.png"),
            Map.entry("infinite_servitude", "stand/the_world_over_heaven/moves/infinite_servitude.png"),
            Map.entry("restoration", "stand/the_world_over_heaven/moves/restoration.png"),
            Map.entry("erasure", "stand/the_world_over_heaven/moves/erasure.png"),
            Map.entry("overwrite", "stand/the_world_over_heaven/moves/overwrite.png"),
            Map.entry("reality_bend", "stand/the_world_over_heaven/moves/reality_bend.png"),
            Map.entry("judgement_fist", "stand/the_world_over_heaven/moves/judgement_fist.png"),
            Map.entry("heaven_step", "stand/the_world_over_heaven/moves/heaven_step.png"),
            Map.entry("knife_halo", "stand/the_world_over_heaven/moves/knife_halo.png"),
            Map.entry("boss_presence", "stand/the_world_over_heaven/passives/boss_presence.png"),
            Map.entry("ascended", "stand/the_world_over_heaven/passives/ascended.png"),
            Map.entry("transcendent", "stand/the_world_over_heaven/passives/transcendent.png"),
            Map.entry("heaven", "stand/the_world_over_heaven/passives/heaven.png"),
            Map.entry("ascension_gauge", "stand/the_world_over_heaven/passives/ascension_gauge.png"),
            Map.entry("reality_is_mine", "stand/the_world_over_heaven/passives/reality_is_mine.png"),

            Map.entry("heavy_punch", "player/moves/heavy_punch.png"),
            Map.entry("gut_punch", "player/moves/gut_punch.png"),
            Map.entry("dodge", "player/moves/dodge.png"),
            Map.entry("double_jump", "player/moves/double_jump.png"),
            Map.entry("burst", "player/moves/burst.png"),
            Map.entry("ground_slam", "player/moves/ground_slam.png"),
            Map.entry("might", "player/passives/might.png"),
            Map.entry("constitution", "player/passives/constitution.png"),
            Map.entry("fleetness", "player/passives/fleetness.png"),
            Map.entry("hardiness", "player/passives/hardiness.png"),
            Map.entry("resolve", "player/passives/resolve.png"));

    private static final Map<String, String> PLAYER_MOVES = Map.of(
            "uppercut", "player/moves/uppercut.png",
            "barrage", "player/moves/player_barrage.png");

    private static final Map<String, String> NODES = Map.ofEntries(
            Map.entry("unlock_stand", "stand/shared/stand_unlock_icon.png"),
            Map.entry("unlock_hamon", "hamon/unlock_hamon.png"),
            Map.entry("unlock_player", "player/unlock_player.png"),
            Map.entry("unlock_vampirism", "vampire/unlock_vampirism.png"),
            Map.entry("unlock_pillar", "pillar_man/pillarman_unlock_icon.png"),
            Map.entry("unlock_ultimate", "ultimate_lifeform/ultimate_lifeform_icon.png"),

            Map.entry("path_wind_mode", "pillar_man/moves/wind_mode.png"),
            Map.entry("path_heat_mode", "pillar_man/moves/heat_mode.png"),
            Map.entry("path_light_mode", "pillar_man/moves/light_mode.png"),

            Map.entry("path_trickster", "hamon/paths/trickster_path.png"),
            Map.entry("path_wisdom", "hamon/paths/wisdom_path.png"),
            Map.entry("path_prideful", "hamon/paths/prideful_path.png"),
            Map.entry("path_stylish", "hamon/paths/stylish_path.png"),
            Map.entry("path_warrior", "hamon/paths/warrior_path.png"),
            Map.entry("path_defender", "hamon/paths/defender_path.png"),

            Map.entry("player_uppercut", "player/moves/uppercut.png"),
            Map.entry("player_barrage", "player/moves/player_barrage.png"));

    public static final int SIZE = 19;

    private SkillIcons() {
    }

    public static ResourceLocation of(ResourceLocation skillId) {
        return skillId == null ? null : lookup(MOVES.get(skillId.getPath()));
    }

    public static ResourceLocation of(ResourceLocation skillId, boolean asPlayer) {
        if (skillId != null && asPlayer) {
            String own = PLAYER_MOVES.get(skillId.getPath());
            if (own != null) {
                return lookup(own);
            }
        }
        return of(skillId);
    }

    public static ResourceLocation ofTreeNode(ResourceLocation nodeId, ResourceLocation skillId) {
        ResourceLocation own = ofNode(nodeId);
        return own != null ? own : of(skillId);
    }

    public static ResourceLocation ofNode(ResourceLocation nodeId) {
        return nodeId == null ? null : lookup(NODES.get(nodeId.getPath()));
    }

    private static final String[] NAME_KEYS = {"skill.jojoha.", "passive.jojoha.", "node.jojoha."};
    private static final float NAME_SCALE = 0.5F;
    private static final int NAME_LINES = 4;

    public static Component nameOf(ResourceLocation id) {
        if (id == null) {
            return Component.empty();
        }
        for (String prefix : NAME_KEYS) {
            if (I18n.exists(prefix + id.getPath())) {
                return Component.translatable(prefix + id.getPath());
            }
        }
        return Component.literal(id.getPath().replace('_', ' '));
    }

    public static void drawName(GuiGraphics graphics, Font font, ResourceLocation id, int x, int y, int size) {
        drawName(graphics, font, nameOf(id), x, y, size);
    }

    public static void drawName(GuiGraphics graphics, Font font, Component name, int x, int y, int size) {
        List<FormattedCharSequence> lines = font.split(name, Math.round((size - 2) / NAME_SCALE));
        int shown = Math.min(NAME_LINES, lines.size());
        if (shown == 0) {
            return;
        }
        graphics.pose().pushPose();
        graphics.pose().translate(x + size / 2F, y + size / 2F, 0F);
        graphics.pose().scale(NAME_SCALE, NAME_SCALE, 1F);
        int top = -shown * font.lineHeight / 2;
        for (int i = 0; i < shown; i++) {
            FormattedCharSequence line = lines.get(i);
            graphics.drawString(font, line, -font.width(line) / 2, top + i * font.lineHeight, 0xFFFFFFFF, true);
        }
        graphics.pose().popPose();
    }

    private static final Map<String, Optional<ResourceLocation>> FOUND = new HashMap<>();

    public static void forget() {
        FOUND.clear();
    }

    private static ResourceLocation lookup(String path) {
        if (path == null) {
            return null;
        }
        return FOUND.computeIfAbsent(path, key -> {
            ResourceLocation icon = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/gui/icons/" + key);
            return Minecraft.getInstance().getResourceManager().getResource(icon).map(found -> icon);
        }).orElse(null);
    }
}
