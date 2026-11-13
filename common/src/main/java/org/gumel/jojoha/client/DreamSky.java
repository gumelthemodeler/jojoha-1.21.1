package org.gumel.jojoha.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class DreamSky {
    private static final int STARS = 1600;
    private static final int GROUPS = 6;
    private static final float STAR_DISTANCE = 100F;

    private static final float STAR_SMALL = 0.11F, STAR_LARGE = 0.26F, STAR_BRIGHT = 0.42F;
    private static final float BRIGHT_SHARE = 0.03F;

    private static final float STARS_FROM = 0.03F, STARS_FULL = 0.32F;

    private static final float REFLECTED = 0.42F;
    private static final float SHIMMER = 2.6F;

    private static final float[] TWINKLE = {0.041F, 0.067F, 0.029F, 0.083F, 0.053F, 0.037F};
    private static final float TWINKLE_DEPTH = 0.35F;

    private static final float[] HAZE_HEIGHT = {0F, 0.1F, 0.42F};
    private static final float[][] HAZE = {
            {0.15F, 0.2F, 0.33F, 0.62F},
            {0.09F, 0.12F, 0.24F, 0.34F},
            {0.05F, 0.07F, 0.16F, 0F},
    };
    private static final int HAZE_SEGMENTS = 48;
    private static final float HAZE_DISTANCE = 90F;

    private static final int NIGHT_FROM = 10;
    private static final int NIGHT_TICKS = 90;

    private static VertexBuffer[] stars;

    private DreamSky() {
    }

    public static void render(Matrix4f view, Matrix4f projection, float partialTick) {
        if (!DreamSpirit.dreaming()) {
            return;
        }
        float time = DreamSpirit.age() + partialTick;
        float night = Mth.clamp((time - NIGHT_FROM) / NIGHT_TICKS, 0F, 1F);
        night = night * night * (3F - 2F * night);
        if (night <= 0.001F) {
            return;
        }

        RenderSystem.enableBlend();
        DreamPost.skyDepth();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        RenderSystem.defaultBlendFunc();
        haze(view, 1F, night);
        haze(view, -1F, night * REFLECTED);

        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        if (stars == null) {
            stars = bake();
        }
        ShaderInstance shader = GameRenderer.getPositionColorShader();
        Matrix4f mirrored = new Matrix4f(view).scale(1F, -1F, 1F);
        for (int group = 0; group < GROUPS; group++) {
            float wave = TWINKLE[group];
            float above = 1F - TWINKLE_DEPTH * (0.5F + 0.5F * Mth.sin(time * wave + group * 1.7F));
            float below = 1F - TWINKLE_DEPTH * (0.5F + 0.5F * Mth.sin(time * wave * SHIMMER + group * 2.9F));
            stars[group].bind();
            RenderSystem.setShaderColor(1F, 1F, 1F, night * above);
            stars[group].drawWithShader(view, projection, shader);
            RenderSystem.setShaderColor(1F, 1F, 1F, night * REFLECTED * below);
            stars[group].drawWithShader(mirrored, projection, shader);
        }
        VertexBuffer.unbind();

        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private static void haze(Matrix4f view, float side, float alpha) {
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_COLOR);
        for (int row = 0; row < HAZE_HEIGHT.length - 1; row++) {
            for (int i = 0; i < HAZE_SEGMENTS; i++) {
                float a0 = i * Mth.TWO_PI / HAZE_SEGMENTS, a1 = (i + 1) * Mth.TWO_PI / HAZE_SEGMENTS;
                edge(buffer, view, a0, HAZE_HEIGHT[row] * side, HAZE[row], alpha);
                edge(buffer, view, a0, HAZE_HEIGHT[row + 1] * side, HAZE[row + 1], alpha);
                edge(buffer, view, a1, HAZE_HEIGHT[row + 1] * side, HAZE[row + 1], alpha);
                edge(buffer, view, a1, HAZE_HEIGHT[row] * side, HAZE[row], alpha);
            }
        }
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    private static void edge(BufferBuilder buffer, Matrix4f view, float around, float rise, float[] colour,
                             float alpha) {
        float flat = Mth.sqrt(1F - rise * rise);
        buffer.addVertex(view, Mth.cos(around) * flat * HAZE_DISTANCE, rise * HAZE_DISTANCE,
                        Mth.sin(around) * flat * HAZE_DISTANCE)
                .setColor(colour[0], colour[1], colour[2], colour[3] * alpha);
    }

    private static VertexBuffer[] bake() {
        VertexBuffer[] baked = new VertexBuffer[GROUPS];
        RandomSource random = RandomSource.create(0x5DA6E17L);
        for (int group = 0; group < GROUPS; group++) {
            BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,
                    DefaultVertexFormat.POSITION_COLOR);
            int placed = 0;
            while (placed < STARS / GROUPS) {
                Vector3f at = new Vector3f(random.nextFloat() * 2F - 1F, random.nextFloat() * 2F - 1F,
                        random.nextFloat() * 2F - 1F);
                float length = at.length();
                if (length < 0.01F || length > 1F) {
                    continue;
                }
                at.div(length);
                if (at.y < STARS_FROM) {
                    continue;
                }
                star(buffer, at, random);
                placed++;
            }
            baked[group] = new VertexBuffer(VertexBuffer.Usage.STATIC);
            baked[group].bind();
            baked[group].upload(buffer.buildOrThrow());
        }
        VertexBuffer.unbind();
        return baked;
    }

    private static void star(BufferBuilder buffer, Vector3f toward, RandomSource random) {
        boolean bright = random.nextFloat() < BRIGHT_SHARE;
        float size = bright ? STAR_BRIGHT : Mth.lerp(random.nextFloat() * random.nextFloat(), STAR_SMALL, STAR_LARGE);
        float glow = bright ? 1F : 0.35F + 0.65F * random.nextFloat();
        float rise = Mth.clamp((toward.y - STARS_FROM) / (STARS_FULL - STARS_FROM), 0F, 1F);
        glow *= rise * rise * (3F - 2F * rise);

        float pick = random.nextFloat();
        float r = 0.92F, g = 0.94F, b = 1F;
        if (pick < 0.2F) {
            r = 0.72F;
            g = 0.82F;
            b = 1F;
        } else if (pick < 0.3F) {
            r = 1F;
            g = 0.9F;
            b = 0.74F;
        }

        Vector3f centre = new Vector3f(toward).mul(STAR_DISTANCE);
        Vector3f right = new Vector3f(toward).cross(0F, 1F, 0F);
        if (right.lengthSquared() < 1.0E-4F) {
            right.set(1F, 0F, 0F);
        }
        right.normalize();
        Vector3f up = new Vector3f(right).cross(toward).normalize();
        float turn = random.nextFloat() * Mth.TWO_PI;
        Vector3f across = new Vector3f(right).mul(Mth.cos(turn)).add(new Vector3f(up).mul(Mth.sin(turn)));
        Vector3f along = new Vector3f(toward).cross(across).normalize();
        across.mul(size);
        along.mul(size);
        for (int corner = 0; corner < 4; corner++) {
            float sa = corner == 0 || corner == 1 ? -1F : 1F;
            float sb = corner == 1 || corner == 2 ? -1F : 1F;
            buffer.addVertex(centre.x + across.x * sa + along.x * sb, centre.y + across.y * sa + along.y * sb,
                            centre.z + across.z * sa + along.z * sb)
                    .setColor(r, g, b, glow);
        }
    }
}
