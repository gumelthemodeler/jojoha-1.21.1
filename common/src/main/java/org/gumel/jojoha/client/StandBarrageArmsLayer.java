package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.client.anim.BedrockAnimation;
import org.gumel.jojoha.stand.StandEntity;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.util.Optional;

/**
 * The fan of arms a barrage throws, drawn as its own animated model over the Stand.
 *
 * <p>The shape of a barrage smear is an artistic decision - which arms, at what angles, how far
 * apart, and in what order they fire - so it is authored rather than derived. The model holds six
 * arms and the animation pops each one through a staggered scale of 0 to 1 and back, which is what
 * turns a static fan into a sequence of blows.
 *
 * <p>Lit the same way the aura motes are: additively. That is the whole difference between something
 * that glows and something that is merely bright - additive blending sums toward white where shapes
 * overlap, so the fan lights the world behind it instead of being pasted over it. The arms texture
 * is dark on average, and additive output is the texture times the tint, so a single pass would
 * come out as a dim wash; stacking a few passes is what carries it up to the aura's brightness
 * while keeping the texture's own shading as variation within the glow.
 *
 * <p>Animated by sampling the Blockbench file directly rather than through GeckoLib's own animation
 * system. GeckoLib drives animations per <em>animatable</em>, and the Stand already owns one
 * controller playing its main animation file; a second model attached to the same entity would have
 * to share that controller and would go looking for "idle" in an arms file that has no such thing.
 * Reading the keyframes straight off is both simpler and completely stateless - the pose is a pure
 * function of the clock, so nothing has to be tracked, reset, or synced.
 */
public final class StandBarrageArmsLayer extends GeoRenderLayer<StandEntity> {
    private static final ResourceLocation ARMS_MODEL =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "geo/starplat_arms.geo.json");
    private static final ResourceLocation ARMS_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/entity/star_plat_arms.png");
    private static final ResourceLocation ARMS_ANIMATION =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "animations/starplat_arms.animation.json");
    private static final String ARMS_ANIMATION_NAME = "barrage";

    /**
     * Additive passes stacked to build brightness, outermost first.
     *
     * <p>Each entry is one full draw at that strength. Three is enough to lift a texture averaging
     * about a third brightness up to something that reads as emitted light; the descending values
     * mean the later passes act as reinforcement on the brightest parts rather than flattening the
     * whole shape to a solid colour.
     */
    private static final float[] GLOW_PASSES = {0.85F, 0.6F, 0.4F};

    private static Optional<BedrockAnimation> animation;
    /**
     * The model the cached animation was loaded alongside.
     *
     * <p>GeckoLib rebuilds its baked models on every resource reload, so a different instance here
     * is proof that resources changed - which lets the animation re-read itself without needing a
     * reload hook of its own. Without it, editing the file and pressing F3+T would silently keep
     * playing the version loaded at startup.
     */
    private static BakedGeoModel animationLoadedFor;

    public StandBarrageArmsLayer(GeoRenderer<StandEntity> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, StandEntity animatable, BakedGeoModel bakedModel, RenderType renderType,
                       MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick,
                       int packedLight, int packedOverlay) {

        if (!animatable.isBarraging()) {
            return;
        }

        // Looked up rather than held: the cache is rebuilt on every resource reload, so a model
        // captured once would quietly become a stale reference the first time resources change.
        BakedGeoModel arms = GeckoLibCache.getBakedModels().get(ARMS_MODEL);
        if (arms == null) {
            return;
        }

        BedrockAnimation cycle = animation(arms);
        if (cycle != null) {
            float seconds = (animatable.tickCount + partialTick) / 20F;
            for (GeoBone bone : arms.topLevelBones()) {
                poseBone(bone, cycle, seconds);
            }
        }

        int standColor = animatable.getStandType().auraColorFor(animatable.getSkin());
        float viewAlpha = StandViewAlpha.of(animatable);

        // RenderType.eyes is vanilla's additive, colour-only pass - the same blend the aura motes
        // use (SRC_ALPHA, ONE). It writes no depth, so the passes layer cleanly over each other.
        RenderType armsType = RenderType.eyes(ARMS_TEXTURE);
        VertexConsumer armsBuffer = bufferSource.getBuffer(armsType);

        for (float strength : GLOW_PASSES) {
            getRenderer().reRender(arms, poseStack, bufferSource, animatable, armsType,
                    armsBuffer, partialTick, LightTexture.FULL_BRIGHT, packedOverlay,
                    argb(standColor, strength * viewAlpha));
        }
    }

    /**
     * Applies one frame of the cycle to a bone and everything under it.
     *
     * <p>Every driven bone is written on every frame rather than only when it changes, because
     * baked bones are shared by every Stand on screen - a bone left holding the last Stand's pose
     * would show up on the next one.
     */
    private static void poseBone(GeoBone bone, BedrockAnimation cycle, float seconds) {
        if (cycle.drives(bone.getName())) {
            Vector3f rotation = cycle.sampleLooping(bone.getName(), BedrockAnimation.Channel.ROTATION, seconds);
            if (rotation != null) {
                // GeckoLib stores rotations with X and Y negated against the authored values - it
                // feeds them straight into Axis.XP/YP, where Blockbench's own sense is opposite.
                bone.setRotX(-rotation.x);
                bone.setRotY(-rotation.y);
                bone.setRotZ(rotation.z);
            }

            Vector3f scale = cycle.sampleLooping(bone.getName(), BedrockAnimation.Channel.SCALE, seconds);
            if (scale != null) {
                bone.setScaleX(scale.x);
                bone.setScaleY(scale.y);
                bone.setScaleZ(scale.z);
            }
        }

        for (GeoBone child : bone.getChildBones()) {
            poseBone(child, cycle, seconds);
        }
    }

    private static BedrockAnimation animation(BakedGeoModel arms) {
        if (animation == null || animationLoadedFor != arms) {
            animation = BedrockAnimation.load(ARMS_ANIMATION, ARMS_ANIMATION_NAME);
            animationLoadedFor = arms;
        }
        return animation.orElse(null);
    }

    private static int argb(int rgb, float alpha) {
        int a = Mth.clamp(Math.round(alpha * 255F), 0, 255);
        return (a << 24) | (rgb & 0x00FFFFFF);
    }
}
