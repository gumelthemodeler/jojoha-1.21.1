package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.StandShadowForm;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.util.Map;
import java.util.WeakHashMap;

public final class StandShadowGlowLayer extends GeoRenderLayer<StandEntity> {
    static final float BODY_ALPHA = 0.5F;

    private static final float COAT_ALPHA_TOP = 0.28F;
    private static final float COAT_ALPHA_BOTTOM = 0.5F;

    private static final float GLOW_STRENGTH = 0.75F;

    private static final Map<BakedGeoModel, float[]> BOUNDS = new WeakHashMap<>();

    public StandShadowGlowLayer(GeoRenderer<StandEntity> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, StandEntity animatable, BakedGeoModel bakedModel,
                       RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                       float partialTick, int packedLight, int packedOverlay) {
        StandShadowForm shadow = StandShadowForm.of(animatable);
        if (shadow == null || buffer == null) {
            return;
        }

        float alpha = StandViewAlpha.of(animatable);
        if (alpha <= 0.001F) {
            return;
        }

        float[] bounds = BOUNDS.computeIfAbsent(bakedModel, StandShadowGlowLayer::measure);
        float scale = animatable.getScale();
        Matrix4f toStand = new Matrix4f(poseStack.last().pose()).invert();
        ResourceLocation texture = getRenderer().getTextureLocation(animatable);

        RenderType coat = RenderType.entityTranslucentEmissive(texture);
        getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable, coat,
                new Gradient(bufferSource.getBuffer(coat), toStand, bounds, scale, shadow,
                        COAT_ALPHA_TOP * alpha, COAT_ALPHA_BOTTOM * alpha, 1F),
                partialTick, LightTexture.FULL_BRIGHT, packedOverlay, 0xFFFFFFFF);

        RenderType glow = RenderType.eyes(texture);
        getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable, glow,
                new Gradient(bufferSource.getBuffer(glow), toStand, bounds, scale, shadow,
                        1F, 1F, GLOW_STRENGTH * alpha),
                partialTick, LightTexture.FULL_BRIGHT, packedOverlay, 0xFFFFFFFF);
    }

    static VertexConsumer gradient(VertexConsumer delegate, Matrix4f standFrame, BakedGeoModel model,
                                   StandEntity stand, StandShadowForm shadow,
                                   float alphaTop, float alphaBottom, float brightness) {
        return new Gradient(delegate, new Matrix4f(standFrame).invert(),
                BOUNDS.computeIfAbsent(model, StandShadowGlowLayer::measure), stand.getScale(), shadow,
                alphaTop, alphaBottom, brightness);
    }

    private static float[] measure(BakedGeoModel model) {
        float[] bounds = {Float.MAX_VALUE, -Float.MAX_VALUE};
        for (GeoBone bone : model.topLevelBones()) {
            measure(bone, bounds);
        }
        if (bounds[0] > bounds[1]) {
            return new float[] {0F, 2F};
        }
        return bounds;
    }

    private static void measure(GeoBone bone, float[] bounds) {
        for (GeoCube cube : bone.getCubes()) {
            for (GeoQuad quad : cube.quads()) {
                if (quad == null) {
                    continue;
                }
                for (GeoVertex vertex : quad.vertices()) {
                    Vector3f position = vertex.position();
                    bounds[0] = Math.min(bounds[0], position.y());
                    bounds[1] = Math.max(bounds[1], position.y());
                }
            }
        }
        for (GeoBone child : bone.getChildBones()) {
            measure(child, bounds);
        }
    }

    private static final class Gradient implements VertexConsumer {
        private final VertexConsumer delegate;
        private final Matrix4f toStand;
        private final float bottom;
        private final float span;
        private final StandShadowForm shadow;
        private final float alphaTop;
        private final float alphaBottom;
        private final float brightness;

        private final Vector4f scratch = new Vector4f();
        private float lastX;
        private float lastY;
        private float lastZ;

        private Gradient(VertexConsumer delegate, Matrix4f toStand, float[] bounds, float scale,
                         StandShadowForm shadow, float alphaTop, float alphaBottom, float brightness) {
            this.delegate = delegate;
            this.toStand = toStand;
            this.bottom = bounds[0] * scale;
            this.span = Math.max(1.0E-3F, (bounds[1] - bounds[0]) * scale);
            this.shadow = shadow;
            this.alphaTop = alphaTop;
            this.alphaBottom = alphaBottom;
            this.brightness = brightness;
        }

        private int colourAt(float x, float y, float z) {
            toStand.transform(scratch.set(x, y, z, 1F));
            float height = Mth.clamp((scratch.y() - bottom) / span, 0F, 1F);
            int rgb = shadow.colourAt(height);

            float red = channel(rgb, 16) * brightness;
            float green = channel(rgb, 8) * brightness;
            float blue = channel(rgb, 0) * brightness;
            float alpha = Mth.lerp(height * height * (3F - 2F * height), alphaBottom, alphaTop);
            return (Math.round(Mth.clamp(alpha, 0F, 1F) * 255F) << 24)
                    | (Math.round(red * 255F) << 16)
                    | (Math.round(green * 255F) << 8)
                    | Math.round(blue * 255F);
        }

        private static float channel(int rgb, int shift) {
            return ((rgb >> shift) & 0xFF) / 255F;
        }

        @Override
        public void addVertex(float x, float y, float z, int colour, float u, float v, int overlay,
                              int light, float normalX, float normalY, float normalZ) {
            delegate.addVertex(x, y, z, colourAt(x, y, z), u, v, overlay, light, normalX, normalY, normalZ);
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            lastX = x;
            lastY = y;
            lastZ = z;
            delegate.addVertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            int colour = colourAt(lastX, lastY, lastZ);
            delegate.setColor((colour >> 16) & 0xFF, (colour >> 8) & 0xFF, colour & 0xFF, colour >>> 24);
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            delegate.setUv(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            delegate.setUv1(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            delegate.setUv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            delegate.setNormal(x, y, z);
            return this;
        }
    }
}
