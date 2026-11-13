package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.hamon.HamonPath;
import org.gumel.jojoha.skill.DisciplinePassives;
import org.gumel.jojoha.skill.Inflicts;
import org.gumel.jojoha.skill.MoveNames;
import org.gumel.jojoha.skilltree.SkillNode;
import org.gumel.jojoha.skilltree.SkillTrees;
import org.gumel.jojoha.stand.passive.PassiveLoadout;
import org.gumel.jojoha.stand.passive.StandPassive;
import org.gumel.jojoha.stand.passive.StandPassives;
import org.gumel.jojoha.stand.skill.StandSkills;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class SkillTreeView {
    public static final int COLUMN = 28;
    public static final int ROW = 27;
    public static final int NODE = 26;

    private static final int ART = 19;
    private static final int ART_INSET = 3;

    private static final int MARGIN = 8;

    private static final int BAND_ROWS = 3;

    private static final int SPINE_GAP = 10;

    private static final int GROUP_GAP = 8;
    private static final int PATH_GAP = 20;

    private static final int PASSIVE_GAP = 6;

    private static final int WHEEL_STEP = 16;

    private static ResourceLocation sprite(String name) {
        return ResourceLocation.withDefaultNamespace("advancements/" + name);
    }

    private static final ResourceLocation TASK_DONE = sprite("task_frame_obtained");
    private static final ResourceLocation TASK = sprite("task_frame_unobtained");
    private static final ResourceLocation GOAL_DONE = sprite("goal_frame_obtained");
    private static final ResourceLocation GOAL = sprite("goal_frame_unobtained");
    private static final ResourceLocation CHALLENGE_DONE = sprite("challenge_frame_obtained");
    private static final ResourceLocation CHALLENGE = sprite("challenge_frame_unobtained");

    private static final int SHADE_LOCKED = 0x90121218;

    private static final int SHADE_IDLE = 0x70101018;

    private static final int RING_EQUIPPED = 0xFFF2E4B0;

    private static final int LINK_OUTLINE = 0xFF000000;
    private static final int LINK_SHUT = 0xFF3A3A42;
    private static final int LINK_OPEN = 0xFF8A8A94;
    private static final int LINK_DONE = 0xFFFFFFFF;

    private static final int LINK_TURN = 2;

    private static final int LINK_HOVER = 0xFFFFFFFF;

    private static final int[] LINK_STATES = {LINK_SHUT, LINK_OPEN, LINK_DONE, LINK_HOVER};

    private static final int BRACKET_READY = 0xFFF2E4B0;

    private static final int TEXT_MET = 0xFFFFFFFF;

    private static final int TEXT_BODY = 0xFFB8B8C4;
    private static final int TEXT_FIGURE = 0xFFFFFFFF;
    private static final int TEXT_PASSIVE = 0xFFFFB8D9;

    private SkillTrees.Tree tree = SkillTrees.Tree.STAND;

    private ResourceLocation standId;

    private List<SkillNode> laidOut = List.of();
    private int[] nodeX = new int[0];
    private int[] nodeY = new int[0];
    private int treeW;
    private int treeH;

    private boolean needsCentre = true;

    private float panX;
    private float panY;

    private boolean dragging;
    private double dragFromX;
    private double dragFromY;
    private float panFromX;
    private float panFromY;

    private int lastViewW = 1;
    private int lastViewH = 1;

    public SkillTrees.Tree tree() {
        return tree;
    }

    public void showStand(ResourceLocation which) {
        if (Objects.equals(which, standId)) {
            return;
        }
        standId = which;
        if (tree == SkillTrees.Tree.STAND) {
            reset();
        }
    }

    private List<SkillNode> nodes() {
        return SkillTrees.of(tree, standId);
    }

    public void show(SkillTrees.Tree which) {
        if (which != tree) {
            tree = which;
            reset();
        }
    }

    public void reset() {
        dragging = false;
        needsCentre = true;
        layout();
    }

    private void layout() {
        List<SkillNode> nodes = nodes();
        laidOut = nodes;
        nodeX = new int[nodes.size()];
        nodeY = new int[nodes.size()];
        treeW = 0;
        treeH = 0;
        if (nodes.isEmpty()) {
            return;
        }

        List<List<Integer>> children = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) {
            children.add(new ArrayList<>());
        }
        List<Integer> roots = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) {
            SkillNode node = nodes.get(i);
            int parent = node.isRoot() ? -1 : indexOf(nodes, node.parent());
            if (parent < 0 || parent == i) {
                roots.add(i);
            } else {
                children.get(parent).add(i);
            }
        }

        int root = rootIndex();

        float[] row = new float[nodes.size()];
        int[] depth = new int[nodes.size()];
        boolean[] placed = new boolean[nodes.size()];
        placed[root] = true;

        List<Branch> moves = new ArrayList<>();
        List<Branch> paths = new ArrayList<>();
        List<Branch> passives = new ArrayList<>();
        List<Integer> tops = new ArrayList<>(children.get(root));
        for (int each : roots) {
            if (each != root) {
                tops.add(each);
            }
        }
        for (int top : tops) {
            if (placed[top]) {
                continue;
            }
            Branch branch = branch(top, children, row, depth, placed);
            SkillNode first = nodes.get(top);
            (isPassive(first) ? passives : SkillTrees.pathOf(first) != null ? paths : moves).add(branch);
        }
        for (int i = 0; i < nodes.size(); i++) {
            if (!placed[i]) {
                moves.add(branch(i, children, row, depth, placed));
            }
        }

        List<List<Branch>> groups = new ArrayList<>();
        group(moves, groups);
        int pathsFrom = paths.isEmpty() ? -1 : groups.size();
        group(paths, groups);
        int bandRows = 0;
        for (List<Branch> group : groups) {
            bandRows = Math.max(bandRows, rowsOf(group));
        }
        int bandBottom = bandRows == 0 ? 0 : (bandRows - 1) * ROW + NODE;
        int spineY = bandRows == 0 ? NODE / 2 : bandBottom + SPINE_GAP / 2;

        int x = COLUMN;
        for (int g = 0; g < groups.size(); g++) {
            if (g > 0) {
                x += g == pathsFrom ? PATH_GAP : GROUP_GAP;
            }
            List<Branch> group = groups.get(g);
            int onRow = bandRows - rowsOf(group);
            int wide = 0;
            for (Branch branch : group) {
                for (int member : branch.members()) {
                    nodeX[member] = x + depth[member] * COLUMN;
                    nodeY[member] = Math.round((onRow + row[member]) * ROW);
                }
                onRow += branch.rows();
                wide = Math.max(wide, branch.columns());
            }
            x += wide * COLUMN;
        }

        int passiveTop = Math.max(spineY + SPINE_GAP / 2, bandBottom + SPINE_GAP);
        x = COLUMN;
        for (Branch branch : passives) {
            for (int member : branch.members()) {
                nodeX[member] = x + depth[member] * COLUMN;
                nodeY[member] = passiveTop + Math.round(row[member] * ROW);
            }
            x += branch.columns() * COLUMN + PASSIVE_GAP;
        }

        nodeX[root] = 0;
        nodeY[root] = spineY - NODE / 2;

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        for (int i = 0; i < nodes.size(); i++) {
            minX = Math.min(minX, nodeX[i]);
            minY = Math.min(minY, nodeY[i]);
        }
        for (int i = 0; i < nodes.size(); i++) {
            nodeX[i] -= minX;
            nodeY[i] -= minY;
            treeW = Math.max(treeW, nodeX[i] + NODE);
            treeH = Math.max(treeH, nodeY[i] + NODE);
        }
    }

    private record Branch(List<Integer> members, int columns, int rows) {
    }

    private static Branch branch(int top, List<List<Integer>> children, float[] row, int[] depth,
                                 boolean[] placed) {
        boolean[] before = placed.clone();
        int[] nextRow = {0};
        place(top, 0, children, row, depth, placed, nextRow);
        List<Integer> members = new ArrayList<>();
        int columns = 0;
        for (int i = 0; i < placed.length; i++) {
            if (placed[i] && !before[i]) {
                members.add(i);
                columns = Math.max(columns, depth[i] + 1);
            }
        }
        return new Branch(members, columns, nextRow[0]);
    }

    private static void group(List<Branch> branches, List<List<Branch>> groups) {
        List<Branch> open = null;
        for (Branch branch : branches) {
            if (open == null || rowsOf(open) + branch.rows() > BAND_ROWS) {
                open = new ArrayList<>();
                groups.add(open);
            }
            open.add(branch);
        }
    }

    private static int rowsOf(List<Branch> group) {
        int rows = 0;
        for (Branch branch : group) {
            rows += branch.rows();
        }
        return rows;
    }

    private static void place(int node, int at, List<List<Integer>> children, float[] row, int[] depth,
                              boolean[] placed, int[] nextRow) {
        placed[node] = true;
        depth[node] = at;
        float first = -1F;
        float last = -1F;
        for (int child : children.get(node)) {
            if (placed[child]) {
                continue;
            }
            place(child, at + 1, children, row, depth, placed, nextRow);
            if (first < 0F) {
                first = row[child];
            }
            last = row[child];
        }
        row[node] = first < 0F ? nextRow[0]++ : (first + last) / 2F;
    }

    private void centreIn(int viewW, int viewH) {
        lastViewW = viewW;
        lastViewH = viewH;
        if (!needsCentre) {
            return;
        }
        needsCentre = false;
        panX = treeW + MARGIN * 2 <= viewW ? (viewW - treeW) / 2F : MARGIN;
        int root = rootIndex();
        panY = treeH + MARGIN * 2 <= viewH || root < 0 ? (viewH - treeH) / 2F
                : viewH / 2F - (nodeY[root] + NODE / 2F);
        clampPan();
    }

    private int rootIndex() {
        for (int i = 0; i < laidOut.size(); i++) {
            if (laidOut.get(i).isRoot()) {
                return i;
            }
        }
        return laidOut.isEmpty() ? -1 : 0;
    }

    public void scrollBy(double across, double down) {
        panX += (float) across * WHEEL_STEP;
        panY += (float) down * WHEEL_STEP;
        clampPan();
    }

    public void beginDrag(double mouseX, double mouseY) {
        dragging = true;
        dragFromX = mouseX;
        dragFromY = mouseY;
        panFromX = panX;
        panFromY = panY;
    }

    public void drag(double mouseX, double mouseY) {
        if (!dragging) {
            return;
        }
        panX = panFromX + (float) (mouseX - dragFromX);
        panY = panFromY + (float) (mouseY - dragFromY);
        clampPan();
    }

    public void endDrag() {
        dragging = false;
    }

    public boolean dragging() {
        return dragging;
    }

    private void clampPan() {
        panX = clampAxis(panX, treeW, lastViewW);
        panY = clampAxis(panY, treeH, lastViewH);
    }

    private static float clampAxis(float pan, int size, int view) {
        if (size + MARGIN * 2 <= view) {
            return (view - size) / 2F;
        }
        return Mth.clamp(pan, view - size - MARGIN, MARGIN);
    }

    private int screenX(int i, int viewX) {
        return viewX + Math.round(panX) + nodeX[i];
    }

    private int screenY(int i, int viewY) {
        return viewY + Math.round(panY) + nodeY[i];
    }

    public SkillNode nodeAt(double x, double y, int viewX, int viewY, int viewW, int viewH) {
        if (x < viewX || x >= viewX + viewW || y < viewY || y >= viewY + viewH) {
            return null;
        }
        List<SkillNode> nodes = current();
        for (int i = 0; i < nodes.size(); i++) {
            int nx = screenX(i, viewX);
            int ny = screenY(i, viewY);
            if (x >= nx && x < nx + NODE && y >= ny && y < ny + NODE) {
                return nodes.get(i);
            }
        }
        return null;
    }

    private List<SkillNode> current() {
        List<SkillNode> nodes = nodes();
        if (!nodes.equals(laidOut)) {
            layout();
            clampPan();
        }
        return laidOut;
    }

    public void render(GuiGraphics guiGraphics, JojohaPlayerData data,
                       int viewX, int viewY, int viewW, int viewH, SkillNode hovered) {
        List<SkillNode> nodes = current();
        centreIn(viewW, viewH);

        for (int pass = 0; pass < 2; pass++) {
            for (int level = 0; level < (pass == 0 ? 1 : 4); level++) {
                for (int i = 0; i < nodes.size(); i++) {
                    SkillNode node = nodes.get(i);
                    int parent = node.isRoot() ? -1 : indexOf(nodes, node.parent());
                    if (parent < 0 || parent == i) {
                        continue;
                    }
                    if (pass == 0) {
                        link(guiGraphics, parent, i, viewX, viewY, LINK_OUTLINE, true);
                        continue;
                    }
                    boolean touched = hovered != null
                            && (hovered.id().equals(node.id()) || hovered.id().equals(nodes.get(parent).id()));
                    int state = touched ? 3 : SkillTrees.owned(data, node) ? 2
                            : SkillTrees.readyToUnlock(data, node) ? 1 : 0;
                    if (state == level) {
                        link(guiGraphics, parent, i, viewX, viewY, LINK_STATES[state], false);
                    }
                }
            }
        }

        int right = viewX + viewW;
        int bottom = viewY + viewH;
        for (int i = 0; i < nodes.size(); i++) {
            int x = screenX(i, viewX);
            int y = screenY(i, viewY);
            if (x + NODE < viewX || x > right || y + NODE < viewY || y > bottom) {
                continue;
            }

            SkillNode node = nodes.get(i);
            boolean done = SkillTrees.owned(data, node);
            guiGraphics.blitSprite(frameFor(node, done), x, y, NODE, NODE);

            ResourceLocation icon = SkillIcons.ofTreeNode(node.id(), node.skill());
            if (icon != null) {
                guiGraphics.blit(icon, x + ART_INSET, y + ART_INSET, 0F, 0F, ART, ART, ART, ART);
            } else if (!node.isRoot()) {
                SkillIcons.drawName(guiGraphics, Minecraft.getInstance().font,
                        node.skill() != null ? node.skill() : node.id(), x + ART_INSET, y + ART_INSET, ART);
            }
            StatusPips.draw(guiGraphics, node.skill(), x + ART_INSET + ART, y + ART_INSET + ART);

            boolean idle = done && isPassive(node) && !running(data, node);
            if (!done) {
                guiGraphics.fill(x + ART_INSET, y + ART_INSET, x + ART_INSET + ART, y + ART_INSET + ART,
                        SHADE_LOCKED);
            } else if (idle) {
                guiGraphics.fill(x + ART_INSET, y + ART_INSET, x + ART_INSET + ART, y + ART_INSET + ART,
                        SHADE_IDLE);
            }

            if (done && isPassive(node) && !idle) {
                guiGraphics.renderOutline(x - 1, y - 1, NODE + 2, NODE + 2, RING_EQUIPPED);
            }

            if (!done && SkillTrees.readyToUnlock(data, node)) {
                brackets(guiGraphics, x, y, BRACKET_READY);
            }
        }
    }

    private static ResourceLocation frameFor(SkillNode node, boolean done) {
        if (node.isRoot()) {
            return done ? CHALLENGE_DONE : CHALLENGE;
        }
        if (isPassive(node)) {
            return done ? GOAL_DONE : GOAL;
        }
        return done ? TASK_DONE : TASK;
    }

    private void link(GuiGraphics guiGraphics, int parent, int child, int viewX, int viewY, int colour,
                      boolean outline) {
        int fromX = screenX(parent, viewX) + NODE / 2;
        int turnX = screenX(child, viewX) + LINK_TURN;
        int fromY = screenY(parent, viewY) + NODE / 2;
        int toX = screenX(child, viewX) + NODE / 2;
        int toY = screenY(child, viewY) + NODE / 2;

        if (outline) {
            hLine(guiGraphics, fromX, turnX, fromY - 1, colour);
            hLine(guiGraphics, fromX, turnX + 1, fromY, colour);
            hLine(guiGraphics, fromX, turnX, fromY + 1, colour);
            hLine(guiGraphics, turnX - 1, toX, toY - 1, colour);
            hLine(guiGraphics, turnX - 1, toX, toY, colour);
            hLine(guiGraphics, turnX - 1, toX, toY + 1, colour);
            vLine(guiGraphics, turnX - 1, fromY, toY, colour);
            vLine(guiGraphics, turnX + 1, fromY, toY, colour);
        } else {
            hLine(guiGraphics, fromX, turnX, fromY, colour);
            hLine(guiGraphics, turnX, toX, toY, colour);
            vLine(guiGraphics, turnX, fromY, toY, colour);
        }
    }

    private static void hLine(GuiGraphics guiGraphics, int x0, int x1, int y, int colour) {
        guiGraphics.fill(Math.min(x0, x1), y, Math.max(x0, x1) + 1, y + 1, colour);
    }

    private static void vLine(GuiGraphics guiGraphics, int x, int y0, int y1, int colour) {
        guiGraphics.fill(x, Math.min(y0, y1), x + 1, Math.max(y0, y1) + 1, colour);
    }

    private static int indexOf(List<SkillNode> nodes, ResourceLocation id) {
        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i).id().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    private static void brackets(GuiGraphics guiGraphics, int x, int y, int colour) {
        final int arm = 6;
        final int out = 1;

        int left = x - out;
        int top = y - out;
        int right = x + NODE + out;
        int bottom = y + NODE + out;

        guiGraphics.fill(left, top, left + arm, top + 1, colour);
        guiGraphics.fill(left, top, left + 1, top + arm, colour);

        guiGraphics.fill(right - arm, top, right, top + 1, colour);
        guiGraphics.fill(right - 1, top, right, top + arm, colour);

        guiGraphics.fill(left, bottom - 1, left + arm, bottom, colour);
        guiGraphics.fill(left, bottom - arm, left + 1, bottom, colour);

        guiGraphics.fill(right - arm, bottom - 1, right, bottom, colour);
        guiGraphics.fill(right - 1, bottom - arm, right, bottom, colour);
    }

    private static Component nameOf(JojohaPlayerData data, SkillNode node) {
        if (node.skill() == null) {
            return Component.translatable(node.translationKey());
        }

        StandPassive passive =
                StandPassives.byId(node.skill());
        if (passive != null) {
            return Component.translatable(passive.translationKey());
        }

        DisciplinePassives.DisciplinePassive spec =
                DisciplinePassives.byId(node.skill());
        if (spec != null) {
            return Component.translatable(spec.translationKey());
        }

        return Component.translatable(
                MoveNames.key(data, node.skill()));
    }

    private static boolean isPassive(SkillNode node) {
        return node.skill() != null && StandSkills.byId(node.skill()) == null
                && (StandPassives.byId(node.skill()) != null
                        || DisciplinePassives.byId(node.skill()) != null);
    }

    private static boolean isSubTrait(SkillNode node) {
        DisciplinePassives.DisciplinePassive spec = node.skill() == null ? null : DisciplinePassives.byId(node.skill());
        return spec != null && spec.minor();
    }

    private static boolean running(JojohaPlayerData data, SkillNode node) {
        if (isStandPassive(node)) {
            return PassiveLoadout.isInEffect(data, node.skill());
        }
        DisciplinePassives.DisciplinePassive spec =
                DisciplinePassives.byId(node.skill());
        return spec != null && spec.active().test(data);
    }

    private static boolean isStandPassive(SkillNode node) {
        return node.skill() != null
                && StandPassives.byId(node.skill()) != null;
    }

    private boolean more;

    public boolean more() {
        return more;
    }

    public List<Component> tooltip(JojohaPlayerData data, SkillNode node, boolean detailed) {
        List<Component> lines = new ArrayList<>();
        more = false;
        boolean priced = false;

        boolean done = SkillTrees.owned(data, node);
        lines.add(nameOf(data, node).copy().withStyle(s -> s.withColor(isPassive(node) ? TEXT_PASSIVE : node.branch().colour())));

        String kind = isSubTrait(node) ? "Sub Trait" : "Passive";
        lines.add(Component.literal(node.innate() ? kind + ", granted"
                        : isPassive(node) ? kind
                        : node.branch().label())
                .withStyle(s -> s.withColor(isPassive(node) ? TEXT_PASSIVE : 0xFF8A8A98)));

        if (isPassive(node)) {
            StandPassive passive =
                    StandPassives.byId(node.skill());
            DisciplinePassives.DisciplinePassive spec =
                    DisciplinePassives.byId(node.skill());

            String key = passive != null ? passive.translationKey() + ".desc"
                    : spec != null ? spec.descriptionKey() : null;
            if (key != null) {
                more = Brief.add(lines, key.substring(0, key.length() - ".desc".length()), key, detailed, false,
                        TEXT_BODY, TEXT_FIGURE);
            }
        } else if (SkillTrees.pathOf(node) != null) {
            HamonPath path = SkillTrees.pathOf(node);
            if (path.teacher() != null) {
                lines.add(Component.translatable("menu.jojoha.skills.path_of", path.teacher())
                        .withStyle(s -> s.withColor(TEXT_BODY)));
            }
            if (!path.moveIds().isEmpty()) {
                MutableComponent teaches = Component.translatable("menu.jojoha.skills.teaches");
                for (int i = 0; i < path.moveIds().size(); i++) {
                    if (i > 0) {
                        teaches.append(i == path.moveIds().size() - 1 ? " and " : ", ");
                    }
                    teaches.append(Component.translatable(
                            MoveNames.key(data, path.moveIds().get(i)))
                            .withStyle(s -> s.withColor(TEXT_FIGURE)));
                }
                lines.add(teaches.append(".").withStyle(s -> s.withColor(TEXT_BODY)));
            }
        } else if (node.skill() == null
                && Language.getInstance().has(node.translationKey() + ".desc")) {
            lines.add(Figures.highlight(
                    Component.translatable(node.translationKey() + ".desc"), TEXT_BODY, TEXT_FIGURE));
        } else if (node.skill() != null
                && MoveNames.hasDescription(data, node.skill())) {
            more = Brief.move(lines, data, node.skill(), detailed, TEXT_BODY, TEXT_FIGURE);
            priced = true;
        }

        Component inflicts = priced || node.skill() == null ? null : Inflicts.line(node.skill());
        if (inflicts != null) {
            lines.add(inflicts);
        }

        if (done && !SkillTrees.active(data, node)) {
            lines.add(Component.translatable("menu.jojoha.skills.dormant")
                    .withStyle(s -> s.withColor(0xFFE07070)));
            return lines;
        }

        if (done) {
            DisciplinePassives.DisciplinePassive spec =
                    node.skill() == null ? null
                            : DisciplinePassives.byId(node.skill());
            if (spec != null) {
                boolean running = spec.active().test(data);
                lines.add(Component.literal(running ? "In effect" : "Not yet")
                        .withStyle(s -> s.withColor(running ? TEXT_MET : 0xFF8A8A98)));
                return lines;
            }

            if (isStandPassive(node)) {
                boolean on = PassiveLoadout.isInEffect(data, node.skill());
                lines.add(Component.literal(on ? "In effect" : "Not in effect")
                        .withStyle(s -> s.withColor(on ? TEXT_MET : 0xFF8A8A98)));

                String hint = null;
                ResourceLocation displaced =
                        PassiveLoadout.displacedBy(data, node.skill());
                if (displaced != null) {
                    StandPassive out =
                            StandPassives.byId(displaced);
                    hint = "Click to run it in place of "
                            + (out == null ? displaced.getPath()
                                    : Component.translatable(out.translationKey()).getString());
                } else if (PassiveLoadout.isArchetype(data, node.skill())) {
                    hint = "Always in effect: it comes with the archetype";
                } else if (!on) {
                    hint = "Replaced by its upgrade";
                } else if (PassiveLoadout.hasChoice(data)) {
                    hint = "Click another of this Stand's to swap it out";
                }
                if (hint != null) {
                    String shown = hint;
                    lines.add(Component.literal(shown).withStyle(s -> s.withColor(0xFF6E6E78)));
                }
                return lines;
            }
            lines.add(Component.translatable("menu.jojoha.skills.unlocked").withStyle(s -> s.withColor(TEXT_MET)));
            return lines;
        }

        List<SkillNode> missing = SkillTrees.missingPrerequisites(data, node);
        if (!missing.isEmpty()) {
            for (SkillNode before : missing) {
                lines.add(Component.literal("Requires " + nameOf(data, before).getString())
                        .withStyle(s -> s.withColor(0xFFA0A0A0)));
            }
            return lines;
        }

        if (!node.stats().isEmpty()) {
            MutableComponent costs = Component.empty();
            for (int i = 0; i < node.stats().size(); i++) {
                SkillNode.StatRequirement requirement = node.stats().get(i);
                if (i > 0) {
                    costs.append(Component.literal(", ")
                            .withStyle(s -> s.withColor(0xFF8A8A98)));
                }
                boolean met = requirement.met(data);
                costs.append(Component.literal(requirement.describe())
                        .withStyle(s -> s.withColor(met ? TEXT_MET : 0xFFD05050)));
            }
            lines.add(costs);
        }

        for (SkillNode.Gate gate : node.gates()) {
            boolean met = gate.met(data);
            lines.add(Component.literal(gate.describe(data))
                    .withStyle(s -> s.withColor(met ? TEXT_MET : 0xFFD05050)));
        }

        for (SkillNode.ItemRequirement requirement : node.items()) {
            lines.add(requirement.describe().copy()
                    .append(Component.literal(" x" + requirement.count()))
                    .withStyle(s -> s.withColor(0xFFD05050)));
        }

        boolean ready = SkillTrees.readyToUnlock(data, node);
        lines.add(Component.literal(ready ? "Click to unlock" : "Requirements not met")
                .withStyle(s -> s.withColor(ready ? BRACKET_READY : 0xFFA0A0A0)));
        return lines;
    }
}
