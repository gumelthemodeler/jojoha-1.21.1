package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.stand.PlacementRun;

import java.util.List;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.StandHands;
import org.joml.Matrix4f;

/**
 * Shows where the Stand's next block would land.
 *
 * <p>Vanilla draws a block outline only as far as your own arms go. Past that it draws nothing at
 * all - which is exactly the range this stance operates in, so from four and a half blocks out to
 * eight you would otherwise be aiming at nothing and finding out where the block went afterwards.
 * The box is the outline vanilla stops drawing.
 *
 * <p>The ray is the same one the server will cast: the player's own eye, the player's own look, and
 * {@link StandHands#REACH}. Identical inputs, so the box is not an approximation of where the block
 * will go - it is the same calculation, run a frame earlier.
 *
 * <p>The target cell is resolved through {@link BlockPlaceContext} rather than by adding the hit
 * face by hand, because vanilla's rule is not simply "the next block over" - clicking something
 * replaceable, like tall grass or snow, places <em>into</em> it instead of beside it. Asking the
 * same class the placement will ask is the only way to be sure the box is drawn where the block
 * actually goes.
 */
public final class StandPlacePreview {
    /**
     * Pulled in off the block's faces so the box never fights with geometry already there.
     *
     * <p>Small enough to read as the cell itself rather than as a smaller box inside it.
     */
    private static final double INSET = 0.002;

    /** How hard the ghost fill glows, and how far it breathes either side of that. */
    private static final float FILL_ALPHA = 0.22F;
    private static final float FILL_PULSE = 0.07F;
    private static final float EDGE_ALPHA = 0.85F;

    /** A slow breath - fast enough to read as alive, slow enough not to draw the eye off the aim. */
    private static final float PULSE_SPEED = 0.12F;

    /**
     * How much of the glow the cells behind the aimed one keep.
     *
     * <p>The far end of a stretch is the one being aimed; everything behind it follows from that.
     */
    private static final float TRAIL_DIM = 0.55F;

    /** What a cell that cannot take the block is drawn in. */
    private static final float[] BLOCKED = {0.92F, 0.22F, 0.18F};

    private StandPlacePreview() {
    }

    public static void render(PoseStack poseStack, MultiBufferSource buffers, Camera camera,
                              float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }

        // The same question the click itself asks, so the box appears exactly when the button would
        // be handed over - mode, held key, Stand out, something usable in hand.
        if (!StandHandsInput.shouldDelegate()) {
            return;
        }

        InteractionHand hand = StandHandsInput.delegatedHand(minecraft.player);
        if (hand == null) {
            return;
        }

        ItemStack stack = minecraft.player.getItemInHand(hand);

        Entity stand = StandEntityLookup.localStand(minecraft).orElse(null);
        if (!(stand instanceof StandEntity standEntity)) {
            return;
        }

        BlockHitResult hit = StandHands.aimFrom(minecraft.player,
                minecraft.player.getEyePosition(partialTick),
                minecraft.player.getViewVector(partialTick),
                reachFor(standEntity));
        boolean fills = StandHands.fills(stack);

        if (hit.getType() != HitResult.Type.BLOCK) {
                        // Pointing at sky. With a stretch planted and something that fills a cell in hand, that
            // is a pillar being run up rather than a miss - see PlacementRun.farEnd.
            BlockPos anchor = fills ? StandStretch.anchor().orElse(null) : null;
            if (anchor != null) {
                drawRun(poseStack, buffers, camera, minecraft, standEntity, partialTick, true,
                        PlacementRun.between(anchor, PlacementRun.farEnd(anchor,
                                minecraft.player.getEyePosition(partialTick),
                                minecraft.player.getViewVector(partialTick),
                                reachFor(standEntity), mode()), mode()),
                        null);
            }
            return;
        }

        BlockPlaceContext context = new BlockPlaceContext(minecraft.player, hand, stack, hit);
        BlockPos far = fills ? context.getClickedPos() : hit.getBlockPos();

        // A planted stretch draws its whole run, because the run is what the click will place.
        // Sizing a row you cannot see would be worse than the one-at-a-time placement this replaced.
        // Only things that fill a cell stretch - there is no sensible row of flint and steel.
        List<BlockPos> cells = fills
                ? StandStretch.anchor().map(anchor -> PlacementRun.between(anchor, far, mode()))
                        .orElseGet(() -> List.of(far))
                : List.of(far);

        drawRun(poseStack, buffers, camera, minecraft, standEntity, partialTick,
                fills && context.canPlace(), cells, far);
    }

    /**
     * Draws a run of cells, brightest at the end the player is aiming.
     *
     * @param lead the cell being aimed, drawn at full strength, or null when every cell is a
     *             consequence rather than a target - a pillar has no cell under the crosshair
     */
    private static void drawRun(PoseStack poseStack, MultiBufferSource buffers, Camera camera,
                                Minecraft minecraft, StandEntity stand, float partialTick,
                                boolean fills, List<BlockPos> cells, BlockPos lead) {
        float[] tint = fills ? auraTint(stand) : BLOCKED;
        float pulse = FILL_ALPHA + FILL_PULSE
                * Mth.sin((minecraft.level.getGameTime() + partialTick) * PULSE_SPEED);
        Vec3 cameraPos = camera.getPosition();

        for (BlockPos cell : cells) {
            // The aimed cell is the one the player is steering; the rest follow from it. Dimming
            // the trail keeps a long run from becoming a wall of light over the terrain they are
            // lining it up against.
            float strength = lead == null || cell.equals(lead) ? 1F : TRAIL_DIM;

            AABB box = new AABB(cell).inflate(-INSET)
                    .move(-cameraPos.x, -cameraPos.y, -cameraPos.z);

            // Fill first, edges second. Both are depth-tested so terrain in front hides them, which
            // is what keeps the boxes in the world rather than pasted over it.
            if (fills) {
                fill(poseStack, buffers.getBuffer(RenderType.lightning()), box, tint,
                        pulse * strength);
            }
            LevelRenderer.renderLineBox(poseStack, buffers.getBuffer(RenderType.lines()), box,
                    tint[0], tint[1], tint[2], EDGE_ALPHA * strength);
        }
    }

    /** The shape the player picked from the bar - see BuildMode. */
    private static org.gumel.jojoha.stand.BuildMode mode() {
        return org.gumel.jojoha.data.ClientPlayerDataCache.data.buildMode;
    }

    /** A close-range Stand runs shorter errands than a long-range one - see StandHands. */
    private static double reachFor(StandEntity stand) {
        return stand.getStandType().range() == org.gumel.jojoha.stand.StandRange.LONG
                ? StandHands.LONG_REACH
                : StandHands.CLOSE_REACH;
    }

    /** The Stand's own colour, so the box reads as belonging to it rather than to the HUD. */
    private static float[] auraTint(StandEntity stand) {
        return new float[]{
                stand.getStandType().auraRedFor(stand.getSkin()),
                stand.getStandType().auraGreenFor(stand.getSkin()),
                stand.getStandType().auraBlueFor(stand.getSkin())};
    }

    /** Six faces, wound outward, in the additive quad format {@code lightning} takes. */
    private static void fill(PoseStack poseStack, VertexConsumer buffer, AABB box,
                             float[] tint, float alpha) {
        Matrix4f pose = poseStack.last().pose();
        float r = tint[0];
        float g = tint[1];
        float b = tint[2];

        float x0 = (float) box.minX;
        float y0 = (float) box.minY;
        float z0 = (float) box.minZ;
        float x1 = (float) box.maxX;
        float y1 = (float) box.maxY;
        float z1 = (float) box.maxZ;

        quad(buffer, pose, r, g, b, alpha, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0);
        quad(buffer, pose, r, g, b, alpha, x1, y0, z1, x1, y1, z1, x0, y1, z1, x0, y0, z1);
        quad(buffer, pose, r, g, b, alpha, x0, y0, z1, x0, y1, z1, x0, y1, z0, x0, y0, z0);
        quad(buffer, pose, r, g, b, alpha, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1);
        quad(buffer, pose, r, g, b, alpha, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);
        quad(buffer, pose, r, g, b, alpha, x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1);
    }

    private static void quad(VertexConsumer buffer, Matrix4f pose, float r, float g, float b, float a,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz) {
        buffer.addVertex(pose, ax, ay, az).setColor(r, g, b, a);
        buffer.addVertex(pose, bx, by, bz).setColor(r, g, b, a);
        buffer.addVertex(pose, cx, cy, cz).setColor(r, g, b, a);
        buffer.addVertex(pose, dx, dy, dz).setColor(r, g, b, a);
    }
}
