package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.StandTypes;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.texture.AnimatableTexture;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.util.Map;
import java.util.Set;

public final class MagicianFlameOverlayLayer extends GeoRenderLayer<StandEntity> {
    public static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "geo/magicians_red_overlay.geo.json");
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID,
            "textures/entity/stand/magicians_red/magicians_red_overlay.png");

    private static final float OUTLINE_PIXELS = 0.15F;
    private static final float BRIGHTNESS = 0.8F;
    private static final float FLICKER_FLOOR = 0.3F;
    private static final float FLICKER_TICKS = 1.5F;
    private static final float SWAY_SPEED = 0.4F;

    private static final Set<String> FRAMED = Set.of("Body");

    private static final Map<String, String> FOLLOWS = Map.of(
            "outline2", "bone2",
            "outline", "bone3");

    public MagicianFlameOverlayLayer(GeoRenderer<StandEntity> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, StandEntity animatable, BakedGeoModel bakedModel,
                       RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                       float partialTick, int packedLight, int packedOverlay) {
        if (!StandTypes.MAGICIANS_RED_ID.equals(animatable.getStandType().id())
                || !animatable.getStandType().model().equals(getGeoModel().getModelResource(animatable))) {
            return;
        }
        float alpha = StandViewAlpha.of(animatable);
        BakedGeoModel overlay = GeckoLibCache.getBakedModels().get(MODEL);
        if (alpha <= 0.001F || overlay == null) {
            return;
        }
        for (GeoBone bone : overlay.topLevelBones()) {
            follow(bone, bakedModel);
        }
        AnimatableTexture.setAndUpdate(TEXTURE);
        RenderType flames = RenderType.eyes(TEXTURE);
        float peak = Mth.clamp(alpha, 0F, 1F) * BRIGHTNESS;
        float time = animatable.tickCount + partialTick;
        Runnable draw = () -> getRenderer().reRender(overlay, poseStack, bufferSource, animatable, flames,
                bufferSource.getBuffer(flames), partialTick, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                grey(peak));
        if (getRenderer() instanceof StandRenderer stand) {
            stand.outlined(OUTLINE_PIXELS, FRAMED, bone -> grey(peak * flicker(bone.getName().hashCode(), time)),
                    draw);
        } else {
            draw.run();
        }
    }

    private static float flicker(int seed, float time) {
        float beat = time / FLICKER_TICKS;
        int step = Mth.floor(beat);
        float flame = Mth.lerp(beat - step, jitter(seed, step), jitter(seed, step + 1));
        float sway = Mth.sin(time * SWAY_SPEED + seed) * 0.5F + 0.5F;
        return FLICKER_FLOOR + (1F - FLICKER_FLOOR) * (0.65F * flame + 0.35F * sway);
    }

    private static float jitter(int seed, int step) {
        return (Mth.murmurHash3Mixer(seed * 31 + step) & 0xFFFF) / 65535F;
    }

    private static int grey(float level) {
        int glow = Math.round(Mth.clamp(level, 0F, 1F) * 255F);
        return FastColor.ARGB32.color(255, glow, glow, glow);
    }

    private static void follow(GeoBone bone, BakedGeoModel model) {
        GeoBone source = model.getBone(FOLLOWS.getOrDefault(bone.getName(), bone.getName())).orElse(null);
        if (source != null) {
            bone.setPivotX(source.getPivotX());
            bone.setPivotY(source.getPivotY());
            bone.setPivotZ(source.getPivotZ());
            bone.updateRotation(source.getRotX(), source.getRotY(), source.getRotZ());
            bone.updatePosition(source.getPosX(), source.getPosY(), source.getPosZ());
            bone.updateScale(source.getScaleX(), source.getScaleY(), source.getScaleZ());
            bone.setHidden(source.isHidden());
            bone.setChildrenHidden(source.isHidingChildren());
        }
        for (GeoBone child : bone.getChildBones()) {
            follow(child, model);
        }
    }
}
