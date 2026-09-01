package org.gumel.jojoha.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.skilltree.SkillNode;
import org.gumel.jojoha.skilltree.SkillTrees;
import org.gumel.jojoha.stand.skill.StandSkill;
import org.gumel.jojoha.stand.skill.StandSkills;

import java.util.ArrayList;
import java.util.List;

/**
 * The navigable skill tree: where it is looking, how far apart it is spread, and what is under the
 * cursor.
 *
 * <p>Holds the view state and the geometry; the screen owns the drawing surface and hands this a
 * rectangle to work in. Keeping the two apart means the tree can be shown somewhere else later
 * without the panel it currently lives in coming along with it.
 *
 * <h2>What zooming actually does</h2>
 *
 * <p>It spreads the nodes apart. It does not make them bigger.
 *
 * <p>The first version scaled everything, which had two problems at once. Zooming out shrank the
 * icons until they were unreadable squares, and any zoom that was not a whole fraction put 19-pixel
 * art on fractional pixels, in a menu where everything else is drawn at a whole-number scale.
 * Holding the nodes at a fixed size and moving only their positions fixes both: a node is always
 * exactly as legible as it is anywhere else in the interface, the art is never resampled, and the
 * zoom is then free to take any value at all - which is what lets it be smooth.
 */
public final class SkillTreeView {
    private static ResourceLocation menu(String file) {
        return ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/gui/menu/" + file);
    }

    private static final ResourceLocation BASE = menu("skill_base_icon.png");
    private static final ResourceLocation FRAME = menu("skill_icon_frame.png");

    /** The frame is 21 with a one pixel border; the art inside it is 19. */
    public static final int NODE = 21;
    private static final int ART = 19;

    /**
     * How far apart the tree is spread, as a multiple of its own coordinates.
     *
     * <p>The lower bound is set by the nodes themselves. They are drawn at a fixed 21 pixels, and
     * the tightest pair on the Stand tree is 57 units apart, so below about 0.37 they would begin to
     * overlap. It stops at 0.42, which still leaves a gap and shows a good deal more of the fan than
     * the old floor did now that the rings are further out.
     */
    private static final float SPREAD_MIN = 0.42F;
    private static final float SPREAD_MAX = 1.6F;

    /**
     * How much one notch of the wheel moves the spread.
     *
     * <p>A twelfth of the range rather than a fifth. At a quarter each, three notches crossed the
     * whole span and every one of them was a jump - the glide had barely started before it was
     * asked to go somewhere else. Small steps make the wheel read as a dial you turn rather than a
     * switch with five positions, and the eased glide has room to actually be an ease.
     */
    private static final float SPREAD_PER_NOTCH = 0.1F;

    /**
     * Where the view opens.
     *
     * <p>Near the bottom of the range rather than in the middle of it, because the first thing you
     * want from a web is its shape - one notch out from here already shows the whole fan, and the
     * way in is by scrolling toward what you are interested in.
     */
    private static final float REST_SPREAD = 0.8F;

    /**
     * How quickly the spread catches up to where it is going, as a time constant in milliseconds.
     *
     * <p>Eased against elapsed time rather than stepped per frame, so the glide is the same on a
     * machine drawing 30 frames a second as on one drawing 300.
     */
    private static final float GLIDE_MS = 130F;

    private static final int SHADE_LOCKED = 0xB0121218;

    /**
     * The links, in the only three states they have.
     *
     * <p>Black by default, brightening as a path opens and white once it has been walked. Colour was
     * doing too much here - the branches already read as separate arms from their shape alone, and
     * four hues of link on a patterned background made the web busy rather than informative. One
     * value ramp says the only thing a link needs to say: whether you have been down it.
     *
     * <p>Black also removes the need for the outline pass the coloured links wanted. A black line
     * carries its own contrast against this background at any brightness.
     */
    private static final int LINK_SHUT = 0xFF0A0A0C;
    private static final int LINK_OPEN = 0xFF6E6E78;
    private static final int LINK_DONE = 0xFFFFFFFF;

    /** What any link touching the node under the cursor turns, whatever state it is in. */
    private static final int LINK_HOVER = 0xFFFFFFFF;

    /**
     * Laid a pixel down and right of every link before the link itself.
     *
     * <p>Half transparent and only one pixel off, so it reads as the web sitting above the ground
     * rather than as a second set of lines. It matters most for the white ones: a lit link over a
     * light patch of the tile had nothing separating the two, and the shadow gives it an edge
     * without needing an outline around it.
     */
    private static final int LINK_SHADOW = 0x80000000;
    private static final int SHADOW_OFFSET = 1;

    /**
     * Plain white for a node you have taken, and the warm tone kept for one you can take now.
     *
     * <p>The blue read as another accent colour among several. White belongs to nothing in
     * particular, which is exactly what an "already done" marker wants.
     */
    private static final int BRACKET_DONE = 0xFFFFFFFF;
    private static final int BRACKET_READY = 0xFFF2E4B0;

    /** Met requirements read in the same white as the marker, for the same reason. */
    private static final int TEXT_MET = 0xFFFFFFFF;

    /** Per-frame scratch, kept between frames so a settled view allocates nothing. */
    private int[] screenXs = new int[0];
    private int[] screenYs = new int[0];
    private boolean[] ready = new boolean[0];

    private float spread = 1F;
    private float spreadTarget = 1F;
    private long lastFrame;

    /** Where in tree space the middle of the window is looking. */
    private float panX;
    private float panY;

    private boolean dragging;
    private double dragFromX;
    private double dragFromY;
    private float panFromX;
    private float panFromY;

    private SkillTrees.Tree tree = SkillTrees.Tree.STAND;

    /**
     * Which Stand's tree the Stand page is showing.
     *
     * <p>Held here rather than looked up per call, because the view is drawn many times a frame and
     * the answer cannot change in the middle of one. Null before a Stand has been awakened, which
     * asks for the universal set - see SkillTrees.forStand.
     */
    private net.minecraft.resources.ResourceLocation standId;

    public SkillTrees.Tree tree() {
        return tree;
    }

    /**
     * Points the Stand page at a Stand.
     *
     * <p>Recentres when it changes, because two Stands do not have the same shape - Hermit Purple
     * has one arm off the hub where Star Platinum has five, and keeping the old pan would leave the
     * player looking at empty space where a branch used to be.
     */
    public void showStand(net.minecraft.resources.ResourceLocation which) {
        if (java.util.Objects.equals(which, standId)) {
            return;
        }
        standId = which;
        if (tree == SkillTrees.Tree.STAND) {
            reset();
        }
    }

    /** The nodes currently on screen: the page, narrowed to this Stand where that matters. */
    private java.util.List<SkillNode> nodes() {
        return SkillTrees.of(tree, standId);
    }

    public void show(SkillTrees.Tree which) {
        if (which != tree) {
            tree = which;
            reset();
        }
    }

    /**
     * Back to a view of the whole fan at the resting spread.
     *
     * <p>Centred on the middle of the tree rather than on the root. The root sits at the bottom of a
     * fan that grows upward, so centring on it would put half the window below everything there is
     * to look at and leave the canopy off the top of the screen.
     */
    public void reset() {
        spread = REST_SPREAD;
        spreadTarget = REST_SPREAD;
        dragging = false;

        List<SkillNode> nodes = nodes();
        if (nodes.isEmpty()) {
            panX = 0F;
            panY = 0F;
            return;
        }

        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        for (SkillNode node : nodes) {
            minX = Math.min(minX, node.x());
            maxX = Math.max(maxX, node.x());
            minY = Math.min(minY, node.y());
            maxY = Math.max(maxY, node.y());
        }

        panX = (minX + maxX) / 2F;
        panY = (minY + maxY) / 2F;
    }

    /**
     * Moves the spread toward its target by however much time has passed.
     *
     * <p>Exponential rather than linear, so it leaves quickly and arrives gently, and so a change of
     * mind part way through simply becomes the new starting point.
     */
    private void glide() {
        long now = System.currentTimeMillis();
        long elapsed = lastFrame == 0 ? 16 : Math.min(now - lastFrame, 200);
        lastFrame = now;

        spread = spreadTarget + (spread - spreadTarget) * (float) Math.exp(-elapsed / GLIDE_MS);
        if (Math.abs(spread - spreadTarget) < 0.001F) {
            spread = spreadTarget;
        }
    }

    /**
     * Steps the spread, keeping whatever is under the cursor under the cursor.
     *
     * <p>Zooming about the middle of the window instead would walk the thing you were looking at off
     * the edge, which is the difference between a view you can navigate and one you have to fight.
     * The pan is corrected against the target rather than the gliding value, so it is right by the
     * time the glide finishes instead of being chased every frame.
     */
    public void zoomBy(int steps, double mouseX, double mouseY, int viewX, int viewY,
                       int viewW, int viewH) {
        float next = Mth.clamp(spreadTarget + steps * SPREAD_PER_NOTCH, SPREAD_MIN, SPREAD_MAX);
        if (next == spreadTarget) {
            return;
        }

        double treeX = toTreeX(mouseX, viewX, viewW);
        double treeY = toTreeY(mouseY, viewY, viewH);

        float before = spreadTarget;
        spreadTarget = next;

        panX += (float) (treeX - panX) * (1F - before / next);
        panY += (float) (treeY - panY) * (1F - before / next);
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
        panX = panFromX - (float) (mouseX - dragFromX) / spread;
        panY = panFromY - (float) (mouseY - dragFromY) / spread;
        clampPan();
    }

    public void endDrag() {
        dragging = false;
    }

    public boolean dragging() {
        return dragging;
    }

    /**
     * Keeps the tree from being dragged off into empty space.
     *
     * <p>The limit is the tree's own extent, so the furthest you can go is with the outermost node
     * in the middle of the window - there is never a drag that shows nothing at all.
     */
    private void clampPan() {
        List<SkillNode> nodes = nodes();
        if (nodes.isEmpty()) {
            panX = 0F;
            panY = 0F;
            return;
        }

        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        for (SkillNode node : nodes) {
            minX = Math.min(minX, node.x());
            maxX = Math.max(maxX, node.x());
            minY = Math.min(minY, node.y());
            maxY = Math.max(maxY, node.y());
        }

        // A node's worth of margin past the outermost node, so the edge of the web can sit clear of
        // the frame rather than jammed against it.
        panX = Mth.clamp(panX, minX - NODE, maxX + NODE);
        panY = Mth.clamp(panY, minY - NODE, maxY + NODE);
    }

    // ---- tree space to window space and back -------------------------------------------------

    private int screenX(int treeX, int viewX, int viewW) {
        return Math.round(viewX + viewW / 2F + (treeX - panX) * spread);
    }

    private int screenY(int treeY, int viewY, int viewH) {
        return Math.round(viewY + viewH / 2F + (treeY - panY) * spread);
    }

    private double toTreeX(double screenX, int viewX, int viewW) {
        return (screenX - viewX - viewW / 2F) / spread + panX;
    }

    private double toTreeY(double screenY, int viewY, int viewH) {
        return (screenY - viewY - viewH / 2F) / spread + panY;
    }

    /** The node under this point, or null. */
    public SkillNode nodeAt(double x, double y, int viewX, int viewY, int viewW, int viewH) {
        if (x < viewX || x >= viewX + viewW || y < viewY || y >= viewY + viewH) {
            return null;
        }

        for (SkillNode node : nodes()) {
            int nx = screenX(node.x(), viewX, viewW) - NODE / 2;
            int ny = screenY(node.y(), viewY, viewH) - NODE / 2;
            if (x >= nx && x < nx + NODE && y >= ny && y < ny + NODE) {
                return node;
            }
        }
        return null;
    }

    // ---- drawing --------------------------------------------------------------------------------

    /**
     * The links first, then the nodes over them, so a line never crosses a face.
     *
     * <p>The caller is expected to have clipped to the window already - but clipping only stops a
     * pixel reaching the screen, it does not stop the work of producing it. Anything whose bounds
     * fall outside the window is skipped outright here, which matters because the fan is three times
     * wider than the window it is seen through: at most zooms the majority of it is off-screen, and
     * drawing it was most of the cost.
     */
    public void render(GuiGraphics guiGraphics, JojohaPlayerData data,
                       int viewX, int viewY, int viewW, int viewH, SkillNode hovered) {
        glide();

        List<SkillNode> nodes = nodes();

        // Positions once per frame into a scratch array, rather than recomputed for every link and
        // then again for the node itself. Grown on demand and kept, so a settled view allocates
        // nothing at all.
        if (screenXs.length < nodes.size()) {
            screenXs = new int[nodes.size()];
            screenYs = new int[nodes.size()];
            ready = new boolean[nodes.size()];
        }

        for (int i = 0; i < nodes.size(); i++) {
            SkillNode node = nodes.get(i);
            screenXs[i] = screenX(node.x(), viewX, viewW);
            screenYs[i] = screenY(node.y(), viewY, viewH);
            ready[i] = SkillTrees.readyToUnlock(data, node);
        }

        int right = viewX + viewW;
        int bottom = viewY + viewH;

        for (int i = 0; i < nodes.size(); i++) {
            SkillNode node = nodes.get(i);
            if (node.isRoot()) {
                continue;
            }

            int parentIndex = indexOf(nodes, node.parent());
            if (parentIndex < 0) {
                continue;
            }

            int x0 = screenXs[parentIndex];
            int y0 = screenYs[parentIndex];
            int x1 = screenXs[i];
            int y1 = screenYs[i];

            // The link's bounding box against the window. A line wholly left of, right of, above or
            // below the view cannot contribute a pixel to it.
            if (Math.max(x0, x1) < viewX || Math.min(x0, x1) > right
                    || Math.max(y0, y1) < viewY || Math.min(y0, y1) > bottom) {
                continue;
            }

            // Three states, and they are the whole legend: walked, open to you now, and still shut.
            // Plus one that is not a state at all - a link into or out of whatever the cursor is over
            // lights up regardless, so hovering a node shows you what it hangs from.
            boolean lit = data.hasNode(node.id());
            boolean touched = hovered != null
                    && (hovered.id().equals(node.id()) || hovered.id().equals(nodes.get(parentIndex).id()));

            // Shadow first, whole, then the link over it - one pass each. Interleaving them would
            // let a later shadow pixel paint over an earlier lit one and speckle the line.
            line(guiGraphics, x0 + SHADOW_OFFSET, y0 + SHADOW_OFFSET,
                    x1 + SHADOW_OFFSET, y1 + SHADOW_OFFSET, LINK_SHADOW);
            line(guiGraphics, x0, y0, x1, y1,
                    touched ? LINK_HOVER : lit ? LINK_DONE : ready[i] ? LINK_OPEN : LINK_SHUT);
        }

        for (int i = 0; i < nodes.size(); i++) {
            int x = screenXs[i] - NODE / 2;
            int y = screenYs[i] - NODE / 2;

            if (x + NODE < viewX || x > right || y + NODE < viewY || y > bottom) {
                continue;
            }

            SkillNode node = nodes.get(i);
            boolean done = data.hasNode(node.id());

            // The same plate, art and border every other move icon in this menu gets, so a node
            // reads as the move it grants rather than as a diagram of one.
            guiGraphics.blit(BASE, x, y, 0F, 0F, NODE, NODE, NODE, NODE);

            ResourceLocation icon = node.skill() == null
                    ? SkillIcons.ofNode(node.id())
                    : SkillIcons.of(node.skill());
            if (icon != null) {
                guiGraphics.blit(icon, x + 1, y + 1, 0F, 0F, ART, ART, ART, ART);
            }

            if (!done) {
                guiGraphics.fill(x + 1, y + 1, x + NODE - 1, y + NODE - 1, SHADE_LOCKED);
            }

            guiGraphics.blit(FRAME, x, y, 0F, 0F, NODE, NODE, NODE, NODE);

            // Only on a node you can actually take. An unlocked node had brackets too, which put a
            // heavy white box around most of the tree the further in you got - and said nothing,
            // because an unlocked node is already the only kind that is not dimmed and already the
            // only kind with white links running into it. Brackets now mean one thing: act on this.
            if (!done && ready[i]) {
                brackets(guiGraphics, x, y, BRACKET_READY);
            }
        }
    }

    /** Where a node sits in the list, by id. The lists are short enough that a scan beats a map. */
    private static int indexOf(List<SkillNode> nodes, ResourceLocation id) {
        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i).id().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Four L-shaped corners just outside the node.
     *
     * <p>Drawn one pixel out so they frame the icon instead of covering its border, which is what
     * keeps them legible against art of any colour.
     */
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

    /**
     * A link drawn as a true 45 degree run and a true straight run, and nothing in between.
     *
     * <p>Bresenham between two arbitrary points produces stair-steps of uneven length - three pixels
     * across, then two, then three - which is what read as ragged. An octilinear route has no such
     * thing: the diagonal part is exactly one pixel over for one pixel down, and the rest is dead
     * level or dead upright. Every segment is a line you could have drawn with a ruler.
     *
     * <p>It is also much cheaper. The straight remainder is a single rectangle rather than one fill
     * per pixel, so a link that used to cost sixty fills now costs the length of its diagonal plus
     * one - and for the near-horizontal links across the fan, that is a handful instead of a hundred.
     *
     * <p>One pixel thick in every state. The lit links were drawn at two, which around a node came
     * out looking like a thick white outline rather than a route.
     */
    private static void line(GuiGraphics guiGraphics, int x0, int y0, int x1, int y1, int colour) {
        int dx = x1 - x0;
        int dy = y1 - y0;
        int stepX = Integer.signum(dx);
        int stepY = Integer.signum(dy);
        int across = Math.abs(dx);
        int down = Math.abs(dy);

        int diagonal = Math.min(across, down);
        int x = x0;
        int y = y0;

        for (int i = 0; i < diagonal; i++) {
            guiGraphics.fill(x, y, x + 1, y + 1, colour);
            x += stepX;
            y += stepY;
        }

        // Whatever is left over after the diagonal is axis aligned by construction, so it goes down
        // as one rectangle however long it is.
        if (across > down) {
            guiGraphics.fill(Math.min(x, x1), y, Math.max(x, x1) + 1, y + 1, colour);
        } else if (down > across) {
            guiGraphics.fill(x, Math.min(y, y1), x + 1, Math.max(y, y1) + 1, colour);
        } else {
            guiGraphics.fill(x, y, x + 1, y + 1, colour);
        }
    }

    /**
     * What to call a node.
     *
     * <p>Asked of the move rather than assembled from its id. Move names live under
     * {@code skill.jojoha.<path>}, and a second place that builds that string is a second place to
     * get it wrong - which is exactly what happened here the first time: this guessed at
     * {@code move.jojoha.<path>}, and every node on the tree would have shown a raw key.
     */
    private static Component nameOf(SkillNode node) {
        StandSkill skill = node.skill() == null ? null : StandSkills.byId(node.skill());
        return Component.translatable(skill != null ? skill.translationKey() : node.translationKey());
    }

    /**
     * What a node's tooltip says: what it is, which arm it is on, and what is still in the way.
     *
     * <p>Requirements are listed whether or not they are met, with the unmet ones called out - a
     * list that only shows what you are missing tells you nothing about what a node costs until you
     * already cannot afford it.
     */
    public List<Component> tooltip(JojohaPlayerData data, SkillNode node) {
        List<Component> lines = new ArrayList<>();

        boolean done = data.hasNode(node.id());
        lines.add(nameOf(node).copy().withStyle(s -> s.withColor(node.branch().colour())));
        lines.add(Component.literal(node.branch().label())
                .withStyle(s -> s.withColor(0xFF8A8A98)));

        if (done) {
            lines.add(Component.literal("Unlocked").withStyle(s -> s.withColor(TEXT_MET)));
            return lines;
        }

        List<SkillNode> missing = SkillTrees.missingPrerequisites(data, node);
        if (!missing.isEmpty()) {
            // Every one of them, because a node can need something that is not drawn joined to it -
            // Time Skip hangs off the movement arm and also wants Time Stop. Naming only the drawn
            // parent would leave the other condition invisible.
            for (SkillNode before : missing) {
                lines.add(Component.literal("Requires " + nameOf(before).getString())
                        .withStyle(s -> s.withColor(0xFFA0A0A0)));
            }
            return lines;
        }

        if (!node.stats().isEmpty()) {
            // One line, comma separated, each figure coloured by whether it is already met - so the
            // whole cost reads as a phrase rather than as a column to work down.
            net.minecraft.network.chat.MutableComponent costs = Component.empty();
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
