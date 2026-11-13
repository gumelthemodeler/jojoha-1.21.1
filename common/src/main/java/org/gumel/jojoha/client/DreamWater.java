package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.dream.DreamRealm;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class DreamWater {
    private static final int SCAN_EVERY = 20;
    private static final int SCAN_REACH = DreamRealm.FLOOR_RADIUS + 2;

    private static final double REFLECT_REACH = 72.0;

    private static final double[][][] WICKS = {
            {},
            {{0.5, 0.5, 0.5}},
            {{0.375, 0.44, 0.5}, {0.625, 0.5, 0.44}},
            {{0.5, 0.313, 0.625}, {0.375, 0.44, 0.5}, {0.56, 0.5, 0.44}},
            {{0.44, 0.313, 0.56}, {0.625, 0.44, 0.56}, {0.375, 0.44, 0.375}, {0.56, 0.5, 0.375}},
    };
    private static final double FLAME_ABOVE = 0.03;

    private static final float[] FLAME = {1F, 0.7F, 0.36F};
    private static final float[] WICK = {1F, 0.9F, 0.7F};
    private static final float HALO_SIZE = 0.42F;
    private static final float HALO_ALPHA = 0.3F;
    private static final float POINT_SIZE = 0.05F;
    private static final float POINT_ALPHA = 0.85F;

    private static final float POINT_PER_BLOCK = 0.0028F;

    private static final float FAR_DIMMING = 0.55F;
    private static final float FAR = 80F;

    private static final float STREAK_WIDE = 0.12F;
    private static final float STREAK_LONG = 0.85F;
    private static final float STREAK_ALPHA = 0.34F;
    private static final float REFLECTED_POINT = 0.45F;

    private static final int SIDES = 10;

    private static final float[] SURFACE = {0.012F, 0.02F, 0.045F};
    private static final float SURFACE_BELOW = 0.62F;
    private static final float SURFACE_OUT = 0.2F;
    private static final float SURFACE_REACH = 22F;
    private static final int SURFACE_RINGS = 9;
    private static final int SURFACE_SIDES = 40;

    private static final double STEP = 1.5;
    private static final int RIPPLE_TICKS = 55;
    private static final float RIPPLE_SPREAD = 0.05F;
    private static final float RIPPLE_WIDTH = 0.07F;
    private static final float RIPPLE_ALPHA = 0.28F;
    private static final float[] RIPPLE_COLOUR = {0.78F, 0.86F, 0.8F};
    private static final int RIPPLE_SIDES = 28;

    private static final int DRIP_MIN = 50, DRIP_MAX = 170;
    private static final float DRIP_VOLUME = 0.3F;

    private static final RandomSource RANDOM = RandomSource.create();

    private record Standing(BlockPos pos, BlockState state, int light, List<Vec3> wicks) {
    }

    private record Ripple(double x, double z, int born, float size) {
    }

    private static final List<Standing> STANDING = new ArrayList<>();
    private static final List<Ripple> RIPPLES = new ArrayList<>();
    private static final Set<Integer> LANDED = new HashSet<>();

    private static int here;
    private static double lastX, lastZ, walked;
    private static boolean grounded;
    private static int nextDrip = DRIP_MIN;
    private static LayeredBuffers buffers;

    private DreamWater() {
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;
        if (level == null || player == null || !DreamSpirit.dreaming()) {
            if (here != 0) {
                reset();
            }
            return;
        }
        if (here++ == 0) {
            lastX = player.getX();
            lastZ = player.getZ();
        }
        if (here % SCAN_EVERY == 1) {
            scan(level, player);
        }
        steps(player);
        landings(level);
        drips(level, player);
        RIPPLES.removeIf(ripple -> here - ripple.born() > RIPPLE_TICKS);
    }

    private static void reset() {
        here = 0;
        STANDING.clear();
        RIPPLES.clear();
        LANDED.clear();
        walked = 0;
        grounded = false;
    }

    private static void scan(ClientLevel level, LocalPlayer player) {
        STANDING.clear();
        BlockPos centre = DreamRealm.room(player.getUUID());
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int dx = -SCAN_REACH; dx <= SCAN_REACH; dx++) {
            for (int dz = -SCAN_REACH; dz <= SCAN_REACH; dz++) {
                at.set(centre.getX() + dx, DreamRealm.FLOOR_Y, centre.getZ() + dz);
                BlockState state = level.getBlockState(at);
                if (state.isAir() || state.is(Blocks.BARRIER)) {
                    continue;
                }
                BlockPos pos = at.immutable();
                STANDING.add(new Standing(pos, state, LevelRenderer.getLightColor(level, pos), wicks(state, pos)));
            }
        }
    }

    private static List<Vec3> wicks(BlockState state, BlockPos pos) {
        if (!(state.getBlock() instanceof CandleBlock) || !state.getValue(CandleBlock.LIT)) {
            return List.of();
        }
        List<Vec3> lit = new ArrayList<>();
        for (double[] wick : WICKS[state.getValue(CandleBlock.CANDLES)]) {
            lit.add(new Vec3(pos.getX() + wick[0], pos.getY() + wick[1] + FLAME_ABOVE, pos.getZ() + wick[2]));
        }
        return lit;
    }

    private static void steps(LocalPlayer player) {
        boolean on = player.onGround() && Math.abs(player.getY() - DreamRealm.FLOOR_Y) < 0.2;
        if (on) {
            if (!grounded) {
                ripple(player.getX(), player.getZ(), 1.5F);
                walked = 0;
            }
            walked += Math.sqrt(Mth.square(player.getX() - lastX) + Mth.square(player.getZ() - lastZ));
            if (walked >= STEP) {
                walked = 0;
                ripple(player.getX(), player.getZ(), 1F);
            }
        }
        grounded = on;
        lastX = player.getX();
        lastZ = player.getZ();
    }

    private static void landings(ClientLevel level) {
        for (Entity entity : level.entitiesForRendering()) {
            if (entity instanceof ItemEntity item && item.onGround() && LANDED.add(item.getId())) {
                ripple(item.getX(), item.getZ(), 0.7F);
            }
        }
    }

    private static void drips(ClientLevel level, LocalPlayer player) {
        if (--nextDrip > 0) {
            return;
        }
        nextDrip = DRIP_MIN + RANDOM.nextInt(DRIP_MAX - DRIP_MIN);
        double x, z;
        if (!STANDING.isEmpty() && RANDOM.nextBoolean()) {
            BlockPos near = STANDING.get(RANDOM.nextInt(STANDING.size())).pos();
            double turn = RANDOM.nextDouble() * Math.PI * 2, off = 0.8 + RANDOM.nextDouble();
            x = near.getX() + 0.5 + Math.cos(turn) * off;
            z = near.getZ() + 0.5 + Math.sin(turn) * off;
        } else {
            double turn = RANDOM.nextDouble() * Math.PI * 2, off = 4 + RANDOM.nextDouble() * 18;
            x = player.getX() + Math.cos(turn) * off;
            z = player.getZ() + Math.sin(turn) * off;
        }
        ripple(x, z, 0.6F);
        level.playLocalSound(x, DreamRealm.FLOOR_Y, z, SoundEvents.POINTED_DRIPSTONE_DRIP_WATER,
                SoundSource.AMBIENT, DRIP_VOLUME, 0.7F + RANDOM.nextFloat() * 0.3F, false);
    }

    private static void ripple(double x, double z, float size) {
        RIPPLES.add(new Ripple(x, z, here, size));
    }

    public static void render(PoseStack poseStack, Camera camera, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || here == 0 || !DreamSpirit.dreaming()) {
            return;
        }
        Vec3 eye = camera.getPosition();
        if (eye.y < DreamRealm.FLOOR_Y) {
            return;
        }
        if (buffers == null) {
            buffers = new LayeredBuffers();
        }
        float time = here + partialTick;

        reflect(poseStack, eye, level, minecraft, partialTick);
        candlelight(poseStack, eye, camera, time, true);
        buffers.endBatch();
        DreamPillars.render(poseStack, camera, partialTick, true);
        surface(poseStack, eye, minecraft);
        buffers.endBatch();
        ripples(poseStack, eye, time);
        candlelight(poseStack, eye, camera, time, false);
        buffers.endBatch();
        DreamPillars.render(poseStack, camera, partialTick, false);
    }

    private static void reflect(PoseStack poseStack, Vec3 eye, ClientLevel level, Minecraft minecraft,
                                float partialTick) {
        poseStack.pushPose();
        poseStack.translate(0.0, 2.0 * (DreamRealm.FLOOR_Y - eye.y), 0.0);
        poseStack.scale(1F, -1F, 1F);

        BlockRenderDispatcher blocks = minecraft.getBlockRenderer();
        for (Standing standing : STANDING) {
            poseStack.pushPose();
            poseStack.translate(standing.pos().getX() - eye.x, standing.pos().getY() - eye.y,
                    standing.pos().getZ() - eye.z);
            blocks.renderSingleBlock(standing.state(), poseStack, buffers, standing.light(),
                    OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }

        EntityRenderDispatcher entities = minecraft.getEntityRenderDispatcher();
        entities.setRenderShadow(false);
        for (Entity entity : level.entitiesForRendering()) {
            if (!reflected(entity, eye)) {
                continue;
            }
            double x = Mth.lerp(partialTick, entity.xOld, entity.getX()) - eye.x;
            double y = Mth.lerp(partialTick, entity.yOld, entity.getY()) - eye.y;
            double z = Mth.lerp(partialTick, entity.zOld, entity.getZ()) - eye.z;
            float yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
            entities.render(entity, x, y, z, yaw, partialTick, poseStack, buffers,
                    entities.getPackedLightCoords(entity, partialTick));
        }
        entities.setRenderShadow(true);

        GL11.glFrontFace(GL11.GL_CW);
        buffers.endBatch();
        GL11.glFrontFace(GL11.GL_CCW);
        poseStack.popPose();
    }

    private static boolean reflected(Entity entity, Vec3 eye) {
        if (entity instanceof Player player && player.isSpectator()) {
            return false;
        }
        if (entity.getY() < DreamRealm.FLOOR_Y - 0.5) {
            return false;
        }
        double dx = entity.getX() - eye.x, dz = entity.getZ() - eye.z;
        return dx * dx + dz * dz < REFLECT_REACH * REFLECT_REACH;
    }

    private static void candlelight(PoseStack poseStack, Vec3 eye, Camera camera, float time, boolean below) {
        Matrix4f pose = poseStack.last().pose();
        VertexConsumer light = buffers.getBuffer(Pass.GLOW);
        Vector3f look = camera.getLookVector();
        for (Standing standing : STANDING) {
            List<Vec3> wicks = standing.wicks();
            for (int w = 0; w < wicks.size(); w++) {
                Vec3 wick = wicks.get(w);
                float x = (float) (wick.x - eye.x);
                float y = (float) ((below ? 2.0 * DreamRealm.FLOOR_Y - wick.y : wick.y) - eye.y);
                float z = (float) (wick.z - eye.z);
                float distance = Mth.sqrt(x * x + y * y + z * z);
                if (distance > 2F && x * look.x + y * look.y + z * look.z < 0F) {
                    continue;
                }
                float seed = standing.pos().hashCode() * 0.013F + w * 2.1F;
                float flicker = 0.82F + 0.11F * Mth.sin(time * 0.37F + seed)
                        + 0.07F * Mth.sin(time * 1.13F + seed * 1.7F);
                float far = 1F - FAR_DIMMING * Mth.clamp(distance / FAR, 0F, 1F);
                float point = Math.max(POINT_SIZE, distance * POINT_PER_BLOCK);

                Vector3f right = new Vector3f(-z, 0F, x);
                if (right.lengthSquared() < 1.0E-4F) {
                    right.set(1F, 0F, 0F);
                }
                right.normalize();
                Vector3f toward = new Vector3f(x, y, z).normalize();
                Vector3f up = new Vector3f(right).cross(toward).normalize();

                if (!below) {
                    disc(light, pose, x, y, z, right, up, HALO_SIZE * flicker, HALO_SIZE * flicker * 1.2F,
                            FLAME, HALO_ALPHA * flicker * far);
                    disc(light, pose, x, y, z, right, up, point, point * 1.4F, WICK, POINT_ALPHA * far);
                    continue;
                }

                disc(light, pose, x, y, z, right, up, point, point * 1.4F, WICK,
                        POINT_ALPHA * REFLECTED_POINT * flicker * far);
                Vector3f upright = new Vector3f(0F, 1F, 0F);
                for (int piece = 0; piece < 2; piece++) {
                    float reach = STREAK_LONG * (0.45F + 0.55F * piece);
                    float sway = 0.045F * (1 + piece) * Mth.sin(time * (0.19F + 0.07F * piece) + seed + piece * 2F);
                    float shimmer = 0.7F + 0.3F * Mth.sin(time * (0.5F + 0.2F * piece) + seed * 1.3F + piece);
                    disc(light, pose, x + right.x * sway, y - reach * 0.5F, z + right.z * sway, right, upright,
                            STREAK_WIDE * (1F + 0.4F * piece), reach * 0.6F, FLAME,
                            STREAK_ALPHA * shimmer * flicker * far / (1 + piece));
                }
            }
        }
    }

    private static void disc(VertexConsumer buffer, Matrix4f pose, float x, float y, float z, Vector3f right,
                             Vector3f up, float wide, float tall, float[] colour, float alpha) {
        if (alpha <= 0.004F) {
            return;
        }
        float inner = 0.35F;
        for (int side = 0; side < SIDES; side++) {
            float a0 = side * Mth.TWO_PI / SIDES, a1 = (side + 1) * Mth.TWO_PI / SIDES;
            float c0 = Mth.cos(a0), s0 = Mth.sin(a0), c1 = Mth.cos(a1), s1 = Mth.sin(a1);
            float ix0 = c0 * wide * inner, iy0 = s0 * tall * inner, ix1 = c1 * wide * inner, iy1 = s1 * tall * inner;
            float ox0 = c0 * wide, oy0 = s0 * tall, ox1 = c1 * wide, oy1 = s1 * tall;

            point(buffer, pose, x, y, z, right, up, 0F, 0F, colour, alpha);
            point(buffer, pose, x, y, z, right, up, ix0, iy0, colour, alpha * 0.33F);
            point(buffer, pose, x, y, z, right, up, ix1, iy1, colour, alpha * 0.33F);

            point(buffer, pose, x, y, z, right, up, ix0, iy0, colour, alpha * 0.33F);
            point(buffer, pose, x, y, z, right, up, ox0, oy0, colour, 0F);
            point(buffer, pose, x, y, z, right, up, ox1, oy1, colour, 0F);

            point(buffer, pose, x, y, z, right, up, ix0, iy0, colour, alpha * 0.33F);
            point(buffer, pose, x, y, z, right, up, ox1, oy1, colour, 0F);
            point(buffer, pose, x, y, z, right, up, ix1, iy1, colour, alpha * 0.33F);
        }
    }

    private static void point(VertexConsumer buffer, Matrix4f pose, float x, float y, float z, Vector3f right,
                              Vector3f up, float across, float above, float[] colour, float alpha) {
        buffer.addVertex(pose, x + right.x * across + up.x * above, y + right.y * across + up.y * above,
                        z + right.z * across + up.z * above)
                .setColor(colour[0], colour[1], colour[2], alpha);
    }

    private static void surface(PoseStack poseStack, Vec3 eye, Minecraft minecraft) {
        Matrix4f pose = poseStack.last().pose();
        VertexConsumer sheet = buffers.getBuffer(Pass.SURFACE);
        float y = (float) (DreamRealm.FLOOR_Y - eye.y);
        float edge = minecraft.gameRenderer.getDepthFar() * 0.8F;
        float[] radius = new float[SURFACE_RINGS + 2];
        for (int ring = 1; ring <= SURFACE_RINGS; ring++) {
            float step = ring / (float) SURFACE_RINGS;
            radius[ring] = SURFACE_REACH * step * step;
        }
        radius[SURFACE_RINGS + 1] = Math.max(edge, SURFACE_REACH + 1F);
        for (int ring = 0; ring < radius.length - 1; ring++) {
            float inner = radius[ring], outer = radius[ring + 1];
            float innerAlpha = shade(inner), outerAlpha = shade(outer);
            for (int side = 0; side < SURFACE_SIDES; side++) {
                float a0 = side * Mth.TWO_PI / SURFACE_SIDES, a1 = (side + 1) * Mth.TWO_PI / SURFACE_SIDES;
                float c0 = Mth.cos(a0), s0 = Mth.sin(a0), c1 = Mth.cos(a1), s1 = Mth.sin(a1);
                flat(sheet, pose, c0 * inner, y, s0 * inner, SURFACE, innerAlpha);
                flat(sheet, pose, c0 * outer, y, s0 * outer, SURFACE, outerAlpha);
                flat(sheet, pose, c1 * outer, y, s1 * outer, SURFACE, outerAlpha);

                flat(sheet, pose, c0 * inner, y, s0 * inner, SURFACE, innerAlpha);
                flat(sheet, pose, c1 * outer, y, s1 * outer, SURFACE, outerAlpha);
                flat(sheet, pose, c1 * inner, y, s1 * inner, SURFACE, innerAlpha);
            }
        }
    }

    private static float shade(float out) {
        float t = Mth.clamp(out / SURFACE_REACH, 0F, 1F);
        return Mth.lerp(Mth.sqrt(t), SURFACE_BELOW, SURFACE_OUT);
    }

    private static void flat(VertexConsumer buffer, Matrix4f pose, float x, float y, float z, float[] colour,
                             float alpha) {
        buffer.addVertex(pose, x, y, z).setColor(colour[0], colour[1], colour[2], alpha);
    }

    private static void ripples(PoseStack poseStack, Vec3 eye, float time) {
        if (RIPPLES.isEmpty()) {
            return;
        }
        Matrix4f pose = poseStack.last().pose();
        VertexConsumer light = buffers.getBuffer(Pass.GLOW);
        float y = (float) (DreamRealm.FLOOR_Y - eye.y) + 0.01F;
        for (Ripple ripple : RIPPLES) {
            float age = (time - ripple.born()) / RIPPLE_TICKS;
            if (age < 0F || age >= 1F) {
                continue;
            }
            float fade = (1F - age) * (1F - age) * RIPPLE_ALPHA * Math.min(1F, ripple.size());
            float x = (float) (ripple.x() - eye.x), z = (float) (ripple.z() - eye.z);
            float spread = (time - ripple.born()) * RIPPLE_SPREAD * ripple.size();
            ring(light, pose, x, y, z, spread + 0.05F, fade);
            ring(light, pose, x, y, z, spread * 0.6F, fade * 0.5F);
        }
    }

    private static void ring(VertexConsumer buffer, Matrix4f pose, float x, float y, float z, float radius,
                             float alpha) {
        if (alpha <= 0.004F) {
            return;
        }
        float in = Math.max(0F, radius - RIPPLE_WIDTH), out = radius + RIPPLE_WIDTH;
        for (int side = 0; side < RIPPLE_SIDES; side++) {
            float a0 = side * Mth.TWO_PI / RIPPLE_SIDES, a1 = (side + 1) * Mth.TWO_PI / RIPPLE_SIDES;
            float c0 = Mth.cos(a0), s0 = Mth.sin(a0), c1 = Mth.cos(a1), s1 = Mth.sin(a1);
            for (float[] band : new float[][] {{in, 0F, radius, alpha}, {radius, alpha, out, 0F}}) {
                flat(buffer, pose, x + c0 * band[0], y, z + s0 * band[0], RIPPLE_COLOUR, band[1]);
                flat(buffer, pose, x + c0 * band[2], y, z + s0 * band[2], RIPPLE_COLOUR, band[3]);
                flat(buffer, pose, x + c1 * band[2], y, z + s1 * band[2], RIPPLE_COLOUR, band[3]);

                flat(buffer, pose, x + c0 * band[0], y, z + s0 * band[0], RIPPLE_COLOUR, band[1]);
                flat(buffer, pose, x + c1 * band[2], y, z + s1 * band[2], RIPPLE_COLOUR, band[3]);
                flat(buffer, pose, x + c1 * band[0], y, z + s1 * band[0], RIPPLE_COLOUR, band[1]);
            }
        }
    }

    private static final class Pass extends RenderType {

        static final RenderType GLOW = new Pass("jojoha_dream_glow", LIGHTNING_TRANSPARENCY);
        static final RenderType SURFACE = new Pass("jojoha_dream_surface", TRANSLUCENT_TRANSPARENCY);

        private Pass(String name, TransparencyStateShard blend) {
            super(name, DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLES, 1536, false, false,
                    () -> {
                        POSITION_COLOR_SHADER.setupRenderState();
                        blend.setupRenderState();
                        COLOR_WRITE.setupRenderState();
                        NO_CULL.setupRenderState();
                        LEQUAL_DEPTH_TEST.setupRenderState();
                    },
                    () -> {
                        LEQUAL_DEPTH_TEST.clearRenderState();
                        NO_CULL.clearRenderState();
                        COLOR_WRITE.clearRenderState();
                        blend.clearRenderState();
                        POSITION_COLOR_SHADER.clearRenderState();
                    });
        }
    }
}
