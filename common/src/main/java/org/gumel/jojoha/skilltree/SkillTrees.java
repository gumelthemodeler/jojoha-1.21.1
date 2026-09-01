package org.gumel.jojoha.skilltree;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.StatPoints;
import org.gumel.jojoha.skilltree.SkillNode.Branch;
import org.gumel.jojoha.skilltree.SkillNode.Gate;
import org.gumel.jojoha.skilltree.SkillNode.ItemRequirement;
import org.gumel.jojoha.skilltree.SkillNode.StatRequirement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The trees themselves, and the rules for walking them.
 *
 * <p>One tree per discipline. Each begins at a root that grants nothing and only opens the path -
 * "Unlock Stand" - and branches out into the moves, so the first thing a player does is commit to a
 * discipline rather than pick a move out of a list.
 *
 * <p>Positions are in tree space with the root at the origin, in pixels. Laying them out here rather
 * than in the renderer keeps the shape of the tree a property of the tree.
 */
public final class SkillTrees {
    /** The discipline a tree belongs to, matching the categories the skill page already had. */
    public enum Tree { STAND, HAMON, PLAYER, VAMPIRE }

    public static final ResourceLocation STAND_ROOT = id("unlock_stand");

    /**
     * The Stand's five stats, in the order {@code StandData.stats()} returns them.
     *
     * <p>Spelled out here rather than borrowed from {@link StatPoints}, whose constants name the
     * <em>player's</em> five. The two lists are different: index 2 is Agility for a player and
     * Endurance for a Stand, and index 3 is Endurance for a player and Protection for a Stand. This
     * tree was originally written with the player constants, which quietly gated the dash on the
     * Stand's Endurance while the tooltip - reading its label from the right list - said so.
     */
    private static final int POWER = 0;
    private static final int SPEED = 1;
    private static final int STAND_ENDURANCE = 2;
    private static final int PROTECTION = 3;
    private static final int POTENTIAL = 4;

    private static final Map<Tree, List<SkillNode>> TREES = new HashMap<>();
    private static final Map<ResourceLocation, SkillNode> BY_ID = new HashMap<>();
    private static final Map<ResourceLocation, SkillNode> BY_SKILL = new HashMap<>();

    private SkillTrees() {
    }

    /**
     * The Stand tree: a fan, rooted at the middle and sweeping up and out.
     *
     * <p>Laid out in polar coordinates rather than by hand. Each discipline is an <em>arm</em> - a
     * base angle out of the hub - and each node on it sits one ring further out than the one before,
     * with the arm sweeping a few degrees as it goes. That is what gives the web its curve: the
     * chain bends as it travels rather than running dead straight, and the links bend with it.
     *
     * <p>Writing it as angles and rings rather than as sixteen pairs of numbers means the shape can
     * be adjusted by moving an arm, and every node on it follows.
     *
     * <h2>What the numbers are for</h2>
     *
     * <p>Rings are 66 apart and neighbouring arms far enough apart that no two nodes come within
     * about 57 units of each other. A node draws at a fixed 21 pixels however far out the view is
     * zoomed, so that figure is what sets how far out it can go before they touch.
     *
     * <p>Opened up from 54, which was tight enough that the links between neighbouring arms read as
     * one mass at anything but the closest zoom. The fan is larger for it and needs more panning,
     * which is the trade - a web is navigated, not surveyed.
     */
    /**
     * The moves any Stand with a body can learn, and the trunk they hang off.
     *
     * <p>Shared because they are not really moves the Stand knows - they are things a person can do
     * once they have a Stand at all. Anything humanoid can throw a flurry of punches, lunge, and put
     * a block down, and there is no version of Star Platinum where that is a different skill from
     * The World's.
     *
     * <h2>The node ids are shared too, and that is the point</h2>
     *
     * <p>Every Stand's copy of Barrage is literally the node {@code jojoha:barrage}, not a per-Stand
     * variant of it. Unlocked nodes are stored as one flat set, so learning to barrage once carries
     * to every Stand a player ever has - which is the honest reading of a universal move. Making
     * them per-Stand would mean re-buying the same punch for each new Stand, and the tree would be
     * charging twice for one thing.
     *
     * <p>Signature moves are the opposite: their nodes belong to the Stand and nothing else grants
     * them. See starPlatinumTree.
     */
    private static List<SkillNode> universalStandNodes() {
        List<SkillNode> nodes = new ArrayList<>();

        nodes.add(SkillNode.root(STAND_ROOT));

        // ---- combat sweeps out to the left ---------------------------------------------------
        nodes.add(arm("barrage", "barrage", STAND_ROOT, Branch.COMBAT, COMBAT, 1,
                stand(POWER, 6)));
        nodes.add(arm("uppercut", "uppercut", id("barrage"), Branch.COMBAT, COMBAT, 2,
                stand(POWER, 10)));
        // Off the combat arm rather than along it: the COMBAT line past ring 2 is where each Stand's
        // own signature moves are drawn, and a universal node sitting in it would collide with them.
        nodes.add(arm("grab", "grab", id("barrage"), Branch.COMBAT, INHALE, 1,
                stand(POWER, 8)));
        // ---- movement, just right of the top ----------------------------------------------------
        nodes.add(arm("stand_dash", "stand_dash", STAND_ROOT, Branch.MOBILITY, MOVEMENT, 1,
                stand(SPEED, 6)));
        nodes.add(arm("stand_leap", "stand_leap", id("stand_dash"), Branch.MOBILITY, MOVEMENT, 2,
                stand(SPEED, 10)));

        // ---- the Utility stance's whole kit, out along the right --------------------------------
        // Return heads the chain rather than sitting on an arm of its own. It is the first thing the
        // stance is for - calling the Stand back - and everything else the stance carries follows
        // from it, so the tree now says that instead of leaving it stranded beside the tools it
        // belongs with.
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

        return nodes;
    }

    /**
     * Star Platinum: everything universal, and then the things only it can do.
     *
     * <p>The signature nodes extend the shared arms rather than starting new ones - Star Finger
     * carries on past Uppercut, Inhale forks off Barrage, Time Skip continues the movement chain.
     * That is what makes a personalised tree read as this Stand's tree rather than as a common tree
     * with a Star Platinum section bolted to the side.
     */
    private static List<SkillNode> starPlatinumTree() {
        List<SkillNode> nodes = universalStandNodes();

        nodes.add(arm("star_finger", "star_finger", id("uppercut"), Branch.COMBAT, COMBAT, 3,
                stand(POWER, 18)));
        nodes.add(arm("skull_crusher", "skull_crusher", id("star_finger"), Branch.COMBAT, COMBAT, 4,
                stand(POWER, 28)));

        // Inhale is a combat move, not one of the Utility stance's tools - it was filed with the
        // support arm, which put it on the wrong half of the tree entirely.
        nodes.add(arm("inhale", "inhale", id("barrage"), Branch.COMBAT, INHALE, 2,
                stand(STAND_ENDURANCE, 14)));

        // Drawn on the movement arm, because that is what it is - but it also needs Time Stop, which
        // lives on the arm above. Carried as a requirement rather than drawn as a line across the
        // fan, so the arm still reads as one thing.
        nodes.add(armNeeding("time_skip", "time_skip", id("stand_leap"), Branch.MOBILITY, MOVEMENT, 3,
                List.of(stand(SPEED, 20), stand(POTENTIAL, 16)),
                List.of(), List.of(id("time_stop"))));

        // ---- and Time Stop, straight up the middle ----------------------------------------------
        // Hung off the root rather than off another move, because it is not the end of a line of
        // punches - it is its own thing, and the tree should say so. Starting at the second ring
        // leaves the space directly above the hub clear, which is what makes it read as a column of
        // its own rather than as one more spoke. The exposures gate the node and not merely the
        // move: a node you can buy and then cannot use would be the tree lying to you.
        nodes.add(armNeeding("time_stop", "time_stop", STAND_ROOT, Branch.SPECIAL, SPECIAL, 2,
                List.of(stand(POTENTIAL, 24), player(StatPoints.WORTHINESS, 20)),
                List.of(Gate.TIME_STOP_EXPOSURE), List.of()));
        nodes.add(armNeeding("time_stop_extended", "time_stop_extended", id("time_stop"),
                Branch.SPECIAL, SPECIAL, 3,
                List.of(stand(POTENTIAL, 34), player(StatPoints.WORTHINESS, 30)),
                List.of(Gate.TIME_STOP_EXPOSURE), List.of()));

        return nodes;
    }

    /**
     * Hermit Purple, which shares none of it.
     *
     * <p>Not an oversight and not a stub. The universal set is the moves a Stand with a body can
     * perform, and Hermit Purple has no body - it cannot barrage because it has no fists, cannot
     * leap because it has nowhere to leap from, and cannot be recalled because it never left. A
     * bound Stand starts at the root with nothing but what it can actually do.
     *
     * <p>Which is the argument for splitting the tree per Stand rather than per archetype. Two
     * humanoids differ by a handful of signature moves at the ends of shared arms; a bound Stand
     * differs by not having the arms.
     */
    private static List<SkillNode> hermitPurpleTree() {
        List<SkillNode> nodes = new ArrayList<>();

        nodes.add(SkillNode.root(STAND_ROOT));

        // Off the root on its own spoke rather than in front of the grapple, which is where the
        // universal tree puts it. An arm here is a position on the fan, not a category - the branch
        // is what files this under Mobility - and BUILD is the one spoke this Stand leaves empty, so
        // it sits just below the grapple line without taking its place at the front of it.
        //
        // Deliberately not made the grapple's prerequisite. The grapple is the first thing this
        // Stand is for, and putting a generic movement trick in front of it would change what the
        // opening of Hermit Purple is.
        nodes.add(arm("stand_dash", "stand_dash", STAND_ROOT, Branch.MOBILITY, BUILD, 1,
                stand(SPEED, 6)));

        nodes.add(arm("hermit_grapple", "hermit_grapple", STAND_ROOT, Branch.MOBILITY, MOVEMENT, 1,
                stand(SPEED, 5)));

        // The zip follows the grapple because it is the same trick used harder - you have to be
        // able to throw one vine before you throw two and let them carry you.
        nodes.add(arm("thorn_zip", "thorn_zip", id("hermit_grapple"), Branch.MOBILITY, MOVEMENT, 2,
                stand(SPEED, 12)));

        // On its own arm rather than after the zip. Catching something is not a way of getting
        // about, and hanging it off the movement chain would say that it was.
        // Moved onto the combat arm now that it has moves growing out of it. It is still a tool,
        // but it is the root of the only fighting this Stand does, and a chain that starts on one
        // arm and continues on another does not read as a chain.
        nodes.add(arm("lasso_of_thorns", "lasso_of_thorns", STAND_ROOT, Branch.COMBAT, COMBAT, 1,
                stand(POWER, 6)));

        // The one this Stand is actually for. Filed as special rather than utility because it is not
        // a tool - it is the thing Hermit Purple does that no other Stand can.
        nodes.add(arm("camera_crush", "camera_crush", STAND_ROOT, Branch.SPECIAL, SPECIAL, 2,
                stand(POTENTIAL, 10)));

        // The whip follows the lasso because it is the lasso, used harder - and it takes its place
        // on the bar rather than sitting beside it. See ThornWhipSkill.replaces.
        nodes.add(arm("thorn_whip", "thorn_whip", id("lasso_of_thorns"), Branch.COMBAT, COMBAT, 2,
                stand(POWER, 14)));

        // And the only real punch, at the end of the short combat line this Stand has.
        nodes.add(arm("twisting_gut_punch", "twisting_gut_punch", id("thorn_whip"),
                Branch.COMBAT, COMBAT, 3, stand(POWER, 22)));

        return nodes;
    }

    /**
     * An arm of the fan: the angle it leaves the hub at, and how far it sweeps per ring.
     *
     * <p>The sweep is what curves each chain. Without it the arms would be straight spokes, which
     * reads as a diagram rather than as a web.
     */
    private record Arm(double degrees, double sweep) {
    }

    // Every sweep bends toward the vertical, so the arms rise into a canopy instead of splaying
    // outward. That is the shape the reference has - and it is not only cosmetic: turning the sweeps
    // inward took the fan from 456 wide to 342 without moving a single node closer to its
    // neighbours, which is a quarter of the panning gone for free.
    private static final Arm COMBAT = new Arm(155, -6);

    /**
     * Inhale, on a stub of its own between the combat arm and the Time Stop column.
     *
     * <p>It hangs off Barrage rather than continuing the punching line, because it is a second thing
     * you can do with a fight rather than a harder punch - so it forks off the first combat node
     * instead of extending the chain past Star Finger.
     */
    private static final Arm INHALE = new Arm(123, 0);
    private static final Arm SPECIAL = new Arm(90, 0);
    private static final Arm MOVEMENT = new Arm(63, 4);
    private static final Arm BUILD = new Arm(25, 6);

    /** How far out each ring sits. Even spacing, so depth in a chain reads as distance from the hub. */
    private static final int[] RINGS = {0, 88, 154, 220, 286, 352};

    private static int fanX(Arm on, int ring) {
        return (int) Math.round(RINGS[ring] * Math.cos(Math.toRadians(angle(on, ring))));
    }

    /** Screen coordinates grow downward, so "up and out" is a negative y. */
    private static int fanY(Arm on, int ring) {
        return (int) Math.round(-RINGS[ring] * Math.sin(Math.toRadians(angle(on, ring))));
    }

    private static double angle(Arm on, int ring) {
        return on.degrees() + on.sweep() * (ring - 1);
    }

    private static SkillNode arm(String nodeId, String skillId, ResourceLocation parent,
                                 Branch branch, Arm on, int ring, StatRequirement... stats) {
        return new SkillNode(id(nodeId), skillId == null ? null : id(skillId), parent, branch,
                fanX(on, ring), fanY(on, ring), List.of(stats), List.of(), List.of(), List.of());
    }

    /** The same, for a node carrying gates or needing a second node as well as its parent. */
    private static SkillNode armNeeding(String nodeId, String skillId, ResourceLocation parent,
                                        Branch branch, Arm on, int ring, List<StatRequirement> stats,
                                        List<Gate> gates, List<ResourceLocation> requires) {
        return new SkillNode(id(nodeId), id(skillId), parent, branch,
                fanX(on, ring), fanY(on, ring), stats, List.of(), gates, requires);
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

    /**
     * The per-Stand trees, and the one used when a Stand has none of its own.
     *
     * <p>A Stand without an entry falls back rather than showing an empty page. A newly added Stand
     * with no tree written yet is far better off with the universal moves than with a lone root
     * node - it is playable immediately, and the tree says the true thing that it has no signature
     * moves yet.
     */
    private static final Map<ResourceLocation, List<SkillNode>> STAND_TREES = new HashMap<>();

    private static void registerStand(ResourceLocation standId, List<SkillNode> nodes) {
        STAND_TREES.put(standId, nodes);
        index(nodes);
    }

    /**
     * The tree for one Stand, or the universal set if it has not been given one.
     *
     * <p>Null asks for the fallback, which is what the menu wants before a Stand has been awakened.
     */
    public static List<SkillNode> forStand(ResourceLocation standId) {
        List<SkillNode> own = standId == null ? null : STAND_TREES.get(standId);
        // Tree.STAND is registered with the universal set, so it is already the fallback and there
        // is no second copy of it to keep in step with the first.
        return own != null ? own : of(Tree.STAND);
    }

    /** Every Stand that has a tree of its own. */
    public static java.util.Set<ResourceLocation> standsWithTrees() {
        return STAND_TREES.keySet();
    }

    /**
     * Every node id that belongs to a Stand, of any Stand.
     *
     * <p>For forgetting a tree when the Stand it described is replaced. It has to be this rather
     * than clearing the unlocked set outright, because that set is shared: Hamon paths, the player
     * tree and vampirism are all filed in it too, and they belong to the person rather than to
     * whatever they are currently carrying.
     *
     * <p>Built from every registered Stand tree and the universal one together, so a node learned on
     * a Stand that has since been given its own tree is still recognised as a Stand node.
     */
    public static java.util.Set<ResourceLocation> allStandNodeIds() {
        java.util.Set<ResourceLocation> ids = new java.util.HashSet<>();
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

    /**
     * Files a tree's nodes under their ids.
     *
     * <p>Shared between the two registries, and it has to tolerate being handed the same node twice
     * - the universal nodes appear in every humanoid Stand's tree, by design and by the same id.
     * Putting them again is a no-op, which is exactly what should happen.
     */
    private static void index(List<SkillNode> nodes) {
        for (SkillNode node : nodes) {
            BY_ID.put(node.id(), node);
            if (node.skill() != null) {
                BY_SKILL.put(node.skill(), node);
            }
        }
    }

    public static List<SkillNode> of(Tree tree) {
        return TREES.getOrDefault(tree, List.of());
    }

    /**
     * A tree by page and Stand.
     *
     * <p>Only the Stand page varies by Stand; Hamon, the player tree and vampirism belong to the
     * person rather than to whatever they are carrying.
     */
    public static List<SkillNode> of(Tree tree, ResourceLocation standId) {
        return tree == Tree.STAND ? forStand(standId) : of(tree);
    }

    public static SkillNode byId(ResourceLocation nodeId) {
        return BY_ID.get(nodeId);
    }

    /** The node that grants this move, or null if no tree gates it. */
    public static SkillNode forSkill(ResourceLocation skillId) {
        return BY_SKILL.get(skillId);
    }

    /**
     * Whether a move is available to this player.
     *
     * <p>A move with no node on any tree is ungated and always available - which is what keeps a
     * move added before its node from vanishing out of the game.
     */
    public static boolean skillUnlocked(JojohaPlayerData data, ResourceLocation skillId) {
        SkillNode node = BY_SKILL.get(skillId);
        return node == null || data.hasNode(node.id());
    }

    /**
     * Whether everything this node hangs off has been taken.
     *
     * <p>The drawn parent, plus anything in {@code requires}. The two are separate because the tree
     * is a picture as well as a rule: Time Skip belongs on the movement arm and is drawn there, but
     * it also needs Time Stop, which is on the other side of the tree entirely. Drawing a line all
     * the way across would say the wrong thing about which arm it is on, so the extra condition is
     * carried rather than drawn - and the tooltip names it instead.
     */
    public static boolean parentDone(JojohaPlayerData data, SkillNode node) {
        if (!node.isRoot() && !data.hasNode(node.parent())) {
            return false;
        }

        // Indexed, for the reason given on readyToUnlock - this is on the per-frame path.
        List<ResourceLocation> also = node.requires();
        for (int i = 0; i < also.size(); i++) {
            if (!data.hasNode(also.get(i))) {
                return false;
            }
        }
        return true;
    }

    /** The nodes this one still needs, for the tooltip to name. */
    public static List<SkillNode> missingPrerequisites(JojohaPlayerData data, SkillNode node) {
        List<SkillNode> missing = new ArrayList<>();
        if (!node.isRoot() && !data.hasNode(node.parent())) {
            SkillNode parent = byId(node.parent());
            if (parent != null) {
                missing.add(parent);
            }
        }
        for (ResourceLocation also : node.requires()) {
            if (!data.hasNode(also)) {
                SkillNode other = byId(also);
                if (other != null) {
                    missing.add(other);
                }
            }
        }
        return missing;
    }

    /**
     * Everything standing between this player and this node, as a list of unmet reasons.
     *
     * <p>Returned rather than reduced to a boolean because the tooltip has to say <em>which</em>
     * requirement is short, and computing that twice - once to test and once to explain - is how the
     * two end up disagreeing.
     */
    public static List<StatRequirement> unmetStats(JojohaPlayerData data, SkillNode node) {
        List<StatRequirement> unmet = new ArrayList<>();
        for (StatRequirement requirement : node.stats()) {
            if (!requirement.met(data)) {
                unmet.add(requirement);
            }
        }
        return unmet;
    }

    /** Conditions that are neither stats nor items, such as having witnessed Time Stop. */
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

    /**
     * Everything a client can answer for itself, which is everything except the contents of a bag.
     *
     * <p>Used to decide whether a node is drawn as available, and whether a click on it is worth
     * sending at all. The server still asks the whole question again.
     *
     * <h2>Why this does not build lists</h2>
     *
     * <p>It used to answer by calling {@link #unmetStats} and {@link #unmetGates} and checking
     * whether they came back empty - which meant two {@code ArrayList}s allocated per call. The tree
     * view asks this for every node and every link, every frame, so at sixty frames a second that
     * was a few thousand short-lived lists a second thrown at the collector to answer a question
     * whose whole content is one boolean.
     *
     * <p>The list-building versions are still here, because the tooltip genuinely needs to know
     * <em>which</em> requirement is short. They are just no longer on the path that runs constantly.
     */
    public static boolean readyToUnlock(JojohaPlayerData data, SkillNode node) {
        return node != null
                && !data.hasNode(node.id())
                && parentDone(data, node)
                && statsMet(data, node)
                && gatesMet(data, node);
    }

    /** Indexed rather than for-each: an enhanced for loop over a list allocates an iterator. */
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

    /**
     * The whole test, asked on the server of a request that arrived from a client.
     *
     * <p>Deliberately re-checks everything the client already checked. The client copy of this is
     * for drawing a node lit or dark; this one is what actually decides.
     */
    public static boolean canUnlock(Player player, JojohaPlayerData data, SkillNode node) {
        return readyToUnlock(data, node) && unmetItems(player, node).isEmpty();
    }
    /**
     * Built last, and that placement is not cosmetic.
     *
     * <p>Java runs static field initialisers and static blocks in source order, so a block that
     * builds the trees has to come after every constant the trees are laid out from - the ring radii
     * and the arm angles. It used to sit at the top of the class, where {@code RINGS} was still null
     * when it ran, and the whole mod fell over on the first frame that asked whether a move was
     * unlocked. Keeping it at the bottom means anything added above it is ready before it runs.
     */
    static {
        registerStand(org.gumel.jojoha.stand.StandTypes.STAR_PLATINUM_ID, starPlatinumTree());
        registerStand(org.gumel.jojoha.stand.StandTypes.HERMIT_PURPLE_ID, hermitPurpleTree());

        // The Stand page with nothing equipped, and the safety net for any Stand added without a
        // tree of its own - see STAND_TREES.
        register(Tree.STAND, universalStandNodes());

        // Nothing registered for these yet. They are real trees with no nodes rather than missing
        // ones, so the page can open on them and say so instead of failing to open.
        register(Tree.HAMON, List.of());
        register(Tree.PLAYER, List.of());
        register(Tree.VAMPIRE, List.of());
    }
}
