package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.data.ClientPlayerDataCache;
import org.gumel.jojoha.stand.StandTypes;
import org.gumel.jojoha.stand.passive.SensoryPerceptionPassive;
import org.gumel.jojoha.stand.passive.StandPassives;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws the outlines Sensory Perception grants - see {@code SensoryPerceptionPassive} for why the
 * whole of this passive lives on the client.
 *
 * <h2>Gathered on a tick, drawn on a frame</h2>
 *
 * <p>The two are separated because they run at wildly different rates. Finding containers means
 * walking the block entities of every chunk in range, which is cheap a few times a second and
 * ruinous a hundred and fifty times a second - and the answer does not change between frames
 * anyway, since neither chests nor the player move far in a twentieth of a second.
 *
 * <p>So the scan runs on a timer and the render just replays what it found. The list is in world
 * coordinates rather than camera-relative, so a stale list drawn against a moved camera is still in
 * the right place.
 */
public final class LootSense {
    /**
     * How far the sense reaches, in blocks.
     *
     * <p>Close. It is meant to notice what is around a corner or under the floor of the room you are
     * standing in, not to survey a chunk - a range that reached as far as the eye does would make
     * exploring a matter of reading outlines through terrain instead of looking at the world.
     */
    private static final double RANGE = 12.0;

    /** How often the world is re-scanned, in client ticks. */
    private static final int SCAN_INTERVAL = 10;

    /**
     * How strongly the block itself glows, at the bottom and top of its breath.
     *
     * <p>This replaced an outline, which was the wrong shape for the job however heavy it was drawn.
     * A wireframe says "here is a box"; a lit block says "this one". Kept well under half, because
     * the glow is a wash over a chest and not a replacement for it - you should still be able to
     * tell a chest from a barrel through it.
     */
    private static final float GLOW_ALPHA_MIN = 0.16F;
    private static final float GLOW_ALPHA_MAX = 0.38F;

    /** How far the glow stands off the block, so it reads as light rather than paint. */
    private static final double GLOW_SWELL = 0.035;

    /** The mark that hangs over what has been noticed. */
    private static final ResourceLocation EYE =
            ResourceLocation.fromNamespaceAndPath(org.gumel.jojoha.Jojoha.MOD_ID,
                    "textures/particle/lootable_seen.png");

    /**
     * Where the mark sits: the middle of the block, and it does not move.
     *
     * <p>It hovered above the chest and then bobbed, and both were wrong. A mark that floats reads
     * as a separate thing that happens to be nearby; one planted on the body of the chest reads as
     * the chest being marked. Nothing here breathes in position any more - only in brightness.
     */
    private static final float EYE_HEIGHT = 0.5F;

    /** How large the mark is drawn, in blocks. Most of a block face, so it is legible at range. */
    private static final float EYE_SIZE = 0.72F;

    /** How faint it gets at the bottom of its breath. Never fully out. */
    private static final float EYE_ALPHA_MIN = 0.62F;
    private static final float EYE_ALPHA_MAX = 1.0F;

    /**
     * How fast the glow breathes, in radians a second, and how far apart two marks are in it.
     *
     * <p>Offset per block position rather than shared, so a room full of chests pulses as a room
     * full of separate things noticing you rather than as one blinking light wired to all of them.
     */
    private static final float PULSE_RATE = 2.4F;
    private static final float PULSE_SPREAD = 0.9F;

    private static final List<BlockPos> FOUND = new ArrayList<>();
    private static int sinceScan;

    private LootSense() {
    }

    /** Called once per client tick. */
    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;

        if (level == null || minecraft.player == null || !active()) {
            FOUND.clear();
            return;
        }

        if (--sinceScan > 0) {
            return;
        }
        sinceScan = SCAN_INTERVAL;

        FOUND.clear();
        Vec3 eye = minecraft.player.position();
        BlockPos centre = minecraft.player.blockPosition();

        int fromX = SectionPos.blockToSectionCoord(centre.getX() - (int) RANGE);
        int toX = SectionPos.blockToSectionCoord(centre.getX() + (int) RANGE);
        int fromZ = SectionPos.blockToSectionCoord(centre.getZ() - (int) RANGE);
        int toZ = SectionPos.blockToSectionCoord(centre.getZ() + (int) RANGE);

        for (int x = fromX; x <= toX; x++) {
            for (int z = fromZ; z <= toZ; z++) {
                // Never forced. A chunk the client has not been sent is one the player cannot see
                // into anyway, and asking for it here would be asking the renderer to generate.
                LevelChunk chunk = level.getChunkSource().getChunk(x, z, false);
                if (chunk == null) {
                    continue;
                }

                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    // Anything with slots in it, rather than anything that could have been filled
                    // from a loot table.
                    //
                    // RandomizableContainerBlockEntity is a narrower thing than its name suggests:
                    // it means a container a structure could have populated - chests, barrels,
                    // shulkers, hoppers, dispensers, droppers - and not furnaces, brewing stands,
                    // crafters, chiseled bookshelves or decorated pots. Which is exactly the "only
                    // some chests/containers" testers reported, and it reads as the mark being
                    // unreliable rather than as it being selective.
                    //
                    // The passive says it marks containers. Container is what that means, and it
                    // covers modded ones without this having to name any of them.
                    if (!(blockEntity instanceof net.minecraft.world.Container)) {
                        continue;
                    }

                    BlockPos pos = blockEntity.getBlockPos();
                    if (pos.getCenter().distanceToSqr(eye) <= RANGE * RANGE) {
                        FOUND.add(pos.immutable());
                    }
                }
            }
        }
    }

    /** Called once per frame from each loader's level render hook. */
    public static void render(PoseStack poseStack, MultiBufferSource buffers, Camera camera,
                              float partialTick) {
        if (FOUND.isEmpty() || !active()) {
            return;
        }

        Vec3 cameraPos = camera.getPosition();
        float[] tint = tint();

        // One clock for every mark, so the phase difference between them comes from where they are
        // rather than from when each was found.
        float seconds = (System.currentTimeMillis() % 100000L) / 1000F;

        for (BlockPos pos : FOUND) {
            float breath = breathAt(pos, seconds);

            // The block, lit. Depth tested, because a glow is light coming off a thing you can see -
            // one that ignored depth would be a coloured cube hanging in a wall.
            AABB box = new AABB(pos).inflate(GLOW_SWELL)
                    .move(-cameraPos.x, -cameraPos.y, -cameraPos.z);
            fill(poseStack, buffers.getBuffer(RenderType.lightning()), box, tint,
                    Mth.lerp(breath, GLOW_ALPHA_MIN, GLOW_ALPHA_MAX));

            drawEye(poseStack, buffers, camera, cameraPos, pos, breath);
        }

        // Flushed here rather than left to whoever owns the buffer. The emissive type is not in the
        // ordered list the level renderer drains by name, so an unflushed mark is one that either
        // never appears or appears inside somebody else's batch.
        if (buffers instanceof MultiBufferSource.BufferSource source) {
            source.endBatch();
        }
    }

    /**
     * The mark: a flat square on the body of the block, facing the camera, visible through anything.
     *
     * <p>Drawn on {@code textSeeThrough}, which is the type vanilla puts see-through nametags on. It
     * is the one stock type that carries a texture and turns the depth test off - so the mark shows
     * through a wall without this having to build a render type of its own, which is not as easy as
     * it sounds: {@code RenderType.create} is package private, so the only ways in are a class
     * smuggled into a vanilla package or a per-loader access widener, and neither is worth it for a
     * type that already exists.
     *
     * <p>It writes colour only and no depth, so nothing drawn afterwards is occluded by a mark
     * hanging in mid air.
     *
     * <p>Turned to face the viewer every frame rather than fixed to an axis, so walking round a
     * chest never edges the mark into nothing.
     */
    private static void drawEye(PoseStack poseStack, MultiBufferSource buffers, Camera camera,
                                Vec3 cameraPos, BlockPos pos, float breath) {
        float alpha = Mth.lerp(breath, EYE_ALPHA_MIN, EYE_ALPHA_MAX);

        double x = pos.getX() + 0.5 - cameraPos.x;
        double y = pos.getY() + EYE_HEIGHT - cameraPos.y;
        double z = pos.getZ() + 0.5 - cameraPos.z;

        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(camera.rotation());
        poseStack.scale(EYE_SIZE, EYE_SIZE, EYE_SIZE);

        VertexConsumer buffer = buffers.getBuffer(RenderType.textSeeThrough(EYE));
        org.joml.Matrix4f pose = poseStack.last().pose();

        // Wound anticlockwise from the bottom left. The V axis runs the other way to Y, which is why
        // the top corners take v=0.
        //
        // White, so the art shows the colours it was painted in. Tinting it to the Stand had the eye
        // taking whatever hue the aura was and losing its own, which for a mark that is a picture of
        // something rather than a glow was throwing the picture away.
        quad(buffer, pose, -0.5F, -0.5F, 0F, 1F, alpha);
        quad(buffer, pose, 0.5F, -0.5F, 1F, 1F, alpha);
        quad(buffer, pose, 0.5F, 0.5F, 1F, 0F, alpha);
        quad(buffer, pose, -0.5F, 0.5F, 0F, 0F, alpha);

        poseStack.popPose();
    }

    /**
     * One corner of the block glow.
     *
     * <p>Six faces of flat colour on {@code lightning}, which is the translucent untextured type the
     * placement preview already fills its ghost boxes with - so the two highlights in this mod are
     * made of the same thing.
     */
    private static void fill(PoseStack poseStack, VertexConsumer buffer, AABB box, float[] tint,
                             float alpha) {
        org.joml.Matrix4f pose = poseStack.last().pose();
        float r = tint[0];
        float g = tint[1];
        float b = tint[2];

        float x0 = (float) box.minX;
        float y0 = (float) box.minY;
        float z0 = (float) box.minZ;
        float x1 = (float) box.maxX;
        float y1 = (float) box.maxY;
        float z1 = (float) box.maxZ;

        face(buffer, pose, r, g, b, alpha, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0);
        face(buffer, pose, r, g, b, alpha, x1, y0, z1, x1, y1, z1, x0, y1, z1, x0, y0, z1);
        face(buffer, pose, r, g, b, alpha, x0, y0, z1, x0, y1, z1, x0, y1, z0, x0, y0, z0);
        face(buffer, pose, r, g, b, alpha, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1);
        face(buffer, pose, r, g, b, alpha, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);
        face(buffer, pose, r, g, b, alpha, x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1);
    }

    private static void face(VertexConsumer buffer, org.joml.Matrix4f pose,
                             float r, float g, float b, float a,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz) {
        buffer.addVertex(pose, ax, ay, az).setColor(r, g, b, a);
        buffer.addVertex(pose, bx, by, bz).setColor(r, g, b, a);
        buffer.addVertex(pose, cx, cy, cz).setColor(r, g, b, a);
        buffer.addVertex(pose, dx, dy, dz).setColor(r, g, b, a);
    }

    /**
     * How far through its pulse the mark at this block is, 0 to 1.
     *
     * <p>Phase taken from the block's own coordinates: cheap, stable from frame to frame, and
     * different for any two blocks that are not the same block - so a room full of chests pulses as
     * a room full of separate things noticing you rather than one blinking light wired to all of
     * them. Shared by the outline and the mark so the two breathe together.
     */
    private static float breathAt(BlockPos pos, float seconds) {
        float phase = (pos.getX() * 0.7F + pos.getY() * 1.3F + pos.getZ() * 0.4F) * PULSE_SPREAD;
        return (Mth.sin(seconds * PULSE_RATE + phase) + 1F) * 0.5F;
    }

    /**
     * One corner of the mark.
     *
     * <p>Position, colour, texture and light, and nothing else - that is the whole of what
     * {@code textSeeThrough} declares. The emissive type this used to sit on wanted an overlay and a
     * normal as well; feeding those to this one would be writing past the end of its vertex.
     */
    private static void quad(VertexConsumer buffer, org.joml.Matrix4f pose,
                             float x, float y, float u, float v, float alpha) {
        buffer.addVertex(pose, x, y, 0F)
                .setColor(1F, 1F, 1F, alpha)
                .setUv(u, v)
                .setLight(net.minecraft.client.renderer.LightTexture.FULL_BRIGHT);
    }

    /**
     * Whether the local player's Stand grants the passive right now.
     *
     * <p>Asked of the same list the server would read, rather than hardcoding "is this Star
     * Platinum" - so a second Stand that happens to be given Sensory Perception gets it here too,
     * with nothing to remember to change.
     */
    private static boolean active() {
        return StandPassives.grants(ClientPlayerDataCache.data, SensoryPerceptionPassive.ID);
    }

    /** The Stand's own colour, so the outlines read as coming from it. */
    private static float[] tint() {
        var stand = ClientPlayerDataCache.data.stand;
        var type = StandTypes.byIdOrDefault(stand.standId());
        return new float[]{
                type.auraRedFor(stand.skin()),
                type.auraGreenFor(stand.skin()),
                type.auraBlueFor(stand.skin())};
    }
}
