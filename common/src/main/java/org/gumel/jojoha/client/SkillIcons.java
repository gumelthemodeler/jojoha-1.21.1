package org.gumel.jojoha.client;

import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

import java.util.Map;

/**
 * The picture for each move.
 *
 * <p>One table, read by both places a move is ever drawn - the HUD bar and the menu - so the two
 * cannot disagree about what a move looks like. Keyed by the move's own id rather than by its slot
 * or its position in a moveset, because those change and the move does not.
 *
 * <p>Art is 19x19, which is the HUD's slot exactly and fits inside the menu's 21x21 frame with the
 * frame's one-pixel border showing all the way round. Neither is scaled.
 *
 * <p>A move with no entry here draws the plain base icon. That is a real state, not an oversight to
 * be guarded against: moves get added before their art does, and the bar has to keep working in the
 * meantime.
 */
public final class SkillIcons {
    /**
     * Move id to file, for everything a Stand can do.
     *
     * <p>{@code Map.ofEntries} rather than {@code Map.of}, which stops at ten pairs - a limit this
     * passed the moment the utility tools arrived.
     *
     * <p>The build modes are keyed {@code build_single} and so on because that is what
     * {@code BuildModeSkill} names them: the id is built from the enum constant, not written out,
     * so these follow the enum rather than the file names.
     */
    private static final Map<String, String> STAND = Map.ofEntries(
            Map.entry("barrage", "stand_barrage_icon.png"),
            Map.entry("stand_dash", "stand_dash_icon.png"),
            Map.entry("stand_leap", "stand_leap_icon.png"),
            Map.entry("uppercut", "stand_uppercut_icon.png"),
            Map.entry("pilot", "stand_pilot_icon.png"),
            Map.entry("inhale", "starplatinum_inhale_icon.png"),
            Map.entry("star_finger", "starplatinum_starfinger_icon.png"),
            Map.entry("time_stop", "timestop_icon.png"),
            Map.entry("time_stop_extended", "extended_timestop_icon.png"),
            Map.entry("time_skip", "timeskip_icon.png"),
            Map.entry("skull_crusher", "skull_crusher_icon.png"),
            Map.entry("hermit_grapple", "hermit_purple_grapple_icon.png"),
            Map.entry("thorn_zip", "hermit_purple_thorn_zip.png"),
            Map.entry("lasso_of_thorns", "hermit_purple_lasso_of_thorns.png"),
            Map.entry("camera_crush", "hermit_purple_camera_crush_icon.png"),
            // The file really is "wip" rather than "whip". Matched as it is on disk rather than
            // quietly corrected, because a name that disagrees with the file is a blank slot.
            Map.entry("thorn_whip", "hermit_purple_thorn_wip_icon.png"),
            Map.entry("twisting_gut_punch", "hermit_purple_twisting_gut_punch_icon.png"),

            // The Utility stance's tools. These never sit in an equipped slot - the stance replaces
            // the whole bar with them - but they are drawn through the same slots, so they need art
            // the same way.
            Map.entry("build_single", "stand_utility_single_icon.png"),
            Map.entry("build_row", "stand_utility_row_icon.png"),
            Map.entry("build_column", "stand_utility_column_icon.png"),
            Map.entry("build_free", "stand_utility_free_icon.png"),
            Map.entry("recall_stand", "stand_utility_return_icon.png"));

    /**
     * Art keyed by <em>node</em> rather than by move, for the nodes that grant no move.
     *
     * <p>The one at the centre of each tree opens the path and hands over nothing, so it has no move
     * to take a picture from - but it is the first thing anyone looks at and a blank plate is a poor
     * showing for it.
     */
    private static final Map<String, String> NODES = Map.of(
            "unlock_stand", "stand_unlock_icon.png");

    /** The size the art is, and the size it is drawn at. */
    public static final int SIZE = 19;

    private SkillIcons() {
    }

    /** The icon for a move, or null if it has none yet. */
    public static ResourceLocation of(ResourceLocation skillId) {
        return skillId == null ? null : lookup(STAND.get(skillId.getPath()));
    }

    /** The icon for a node that grants no move, or null. */
    public static ResourceLocation ofNode(ResourceLocation nodeId) {
        return nodeId == null ? null : lookup(NODES.get(nodeId.getPath()));
    }

    private static ResourceLocation lookup(String file) {
        return file == null ? null : ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID,
                "textures/gui/menu/stand_abilities/" + file);
    }
}
