package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.ClientPlayerDataCache;
import org.gumel.jojoha.data.Fate;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.joml.Matrix4f;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoObjectRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;
import software.bernie.geckolib.util.RenderUtil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class UltimateWingsLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "geo/ulf_wings.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/entity/ultimate_lifeform/wings.png");
    private static final ResourceLocation ANIMATIONS =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "animations/ulf_wingsanims.json");

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation CROUCH = RawAnimation.begin().thenLoop("crouch");
    private static final RawAnimation GLIDE = RawAnimation.begin().thenLoop("glide");
    private static final RawAnimation FLAP = RawAnimation.begin().thenLoop("flap");
    private static final RawAnimation DIVE = RawAnimation.begin().thenLoop("dive");

    private static final int BLEND_TICKS = 4;
    private static final int STALE_TICKS = 50;
    private static final int FLAP_TICKS = 14;
    private static final float DIVE_PITCH = 40F;
    private static final double NECK_HEIGHT = 1.5;

    private static final float OPEN_TICKS = 22F;
    private static final float CLOSE_TICKS = 14F;
    private static final float SWELL_BY = 0.34F;
    private static final float BURST_AT = 0.4F;
    private static final float ARM_GONE_BY = 0.47F;
    private static final float ARM_SWELL = 0.45F;
    private static final float ARM_STRETCH = 0.15F;
    private static final float ARM_TREMOR = 0.12F;
    private static final float ARM_THINNEST = 0.3F;
    private static final float WING_FROM = 0.36F;
    private static final float GROW_OVERSHOOT = 2.2F;
    private static final float UNFURL_OVERSHOOT = 3.2F;
    private static final float UNFURL = 2.2F;
    private static final float WING_SCALE = 1.3F;
    private static final int TRAIL_EVERY = 2;
    private static final int OOZE_EVERY = 2;
    private static final Set<String> SHOULDERS = Set.of("left_arm", "right_arm");

    private static final Map<Integer, float[]> MORPH = new HashMap<>();
    private static final Set<Integer> FLYING = new HashSet<>();
    private static final Map<Integer, Seen> SEEN = new HashMap<>();
    private static final Map<Integer, WingRig> RIGS = new HashMap<>();
    private static ClientLevel seenIn;
    private static WingRenderer renderer;

    public UltimateWingsLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    public static void state(int entityId, boolean out, boolean flap) {
        ClientLevel level = level();
        if (level == null) {
            return;
        }
        long now = level.getGameTime();
        Seen seen = SEEN.computeIfAbsent(entityId, id -> new Seen());
        seen.out = out;
        seen.at = now;
        if (flap) {
            seen.flapAt = now;
            if (level.getEntity(entityId) instanceof AbstractClientPlayer player) {
                WingFlair.boost(player);
            }
        }
    }

    public static boolean armless(AbstractClientPlayer player) {
        return morph(player, Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true)) >= BURST_AT;
    }

    public static boolean out(AbstractClientPlayer player) {
        ClientLevel level = level();
        if (level == null || player.isSpectator()) {
            return false;
        }
        if (player == Minecraft.getInstance().player) {
            JojohaPlayerData data = ClientPlayerDataCache.data;
            return data != null && Fate.ultimate(data) && data.ultimateWings;
        }
        Seen seen = SEEN.get(player.getId());
        return seen != null && seen.out && level.getGameTime() - seen.at <= STALE_TICKS;
    }

    public static void tick() {
        ClientLevel level = level();
        if (level == null) {
            MORPH.clear();
            FLYING.clear();
            return;
        }
        if (Minecraft.getInstance().isPaused()) {
            return;
        }
        Set<Integer> present = new HashSet<>();
        for (AbstractClientPlayer player : level.players()) {
            present.add(player.getId());
            boolean out = out(player);
            flight(player, out);
            float[] morph = MORPH.get(player.getId());
            if (morph == null) {
                if (!out) {
                    continue;
                }
                morph = new float[2];
                MORPH.put(player.getId(), morph);
            }
            morph[0] = morph[1];
            morph[1] = Mth.clamp(morph[1] + (out ? 1F / OPEN_TICKS : -1F / CLOSE_TICKS), 0F, 1F);
            if (morph[0] <= 0F && morph[1] > 0F) {
                WingFlair.swell(player);
            }
            if (out && morph[1] > 0F && morph[1] < BURST_AT && player.tickCount % OOZE_EVERY == 0) {
                WingFlair.ooze(player);
            }
            if (morph[0] < BURST_AT && morph[1] >= BURST_AT) {
                WingFlair.burst(player);
            } else if (morph[0] >= BURST_AT && morph[1] < BURST_AT) {
                WingFlair.fold(player);
            }
            if (!out && morph[0] <= 0F) {
                MORPH.remove(player.getId());
            }
        }
        MORPH.keySet().retainAll(present);
        FLYING.retainAll(present);
    }

    private static void flight(AbstractClientPlayer player, boolean out) {
        boolean flying = out && player.isFallFlying();
        if (!flying) {
            FLYING.remove(player.getId());
            return;
        }
        if (FLYING.add(player.getId())) {
            WingFlair.takeoff(player);
        } else if (player.tickCount % TRAIL_EVERY == 0) {
            WingFlair.trail(player);
        }
    }

    private static float backOut(float t, float overshoot) {
        float u = t - 1F;
        return 1F + u * u * ((overshoot + 1F) * u + overshoot);
    }

    private static float morph(AbstractClientPlayer player, float partialTick) {
        float[] morph = MORPH.get(player.getId());
        return morph == null ? 0F : Mth.lerp(partialTick, morph[0], morph[1]);
    }

    private static float stage(float morph, float from, float to) {
        float t = Mth.clamp((morph - from) / (to - from), 0F, 1F);
        return t * t * (3F - 2F * t);
    }

    public static void shapeArms(PlayerModel<AbstractClientPlayer> model, AbstractClientPlayer player) {
        float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
        float morph = morph(player, partialTick);
        float swell = stage(morph, 0F, SWELL_BY);
        float gone = stage(morph, SWELL_BY, ARM_GONE_BY);
        float time = player.tickCount + partialTick;
        for (ModelPart part : List.of(model.leftArm, model.rightArm,
                model.leftSleeve, model.rightSleeve)) {
            float tremor = Mth.sin(time * 2.7F + (part == model.leftArm || part == model.leftSleeve ? 1.3F : 0F))
                    * ARM_TREMOR * swell * (1F - gone);
            float width = 1F + ARM_SWELL * swell + tremor;
            part.xScale = Mth.lerp(gone, width, ARM_THINNEST);
            part.zScale = Mth.lerp(gone, width - tremor * 2F, ARM_THINNEST);
            part.yScale = (1F + ARM_STRETCH * swell) * (1F - gone);
            if (gone >= 1F) {
                part.visible = false;
            }
        }
    }

    private static ClientLevel level() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != seenIn) {
            seenIn = level;
            SEEN.clear();
            RIGS.clear();
            MORPH.clear();
            FLYING.clear();
        }
        return level;
    }

    private static RawAnimation pose(AbstractClientPlayer player) {
        if (player.onGround() || player.isInWater() || player.isPassenger() || player.getAbilities().flying) {
            return player.isCrouching() ? CROUCH : IDLE;
        }
        Seen seen = SEEN.get(player.getId());
        if (seen != null && player.level().getGameTime() - seen.flapAt <= FLAP_TICKS) {
            return FLAP;
        }
        return player.getXRot() >= DIVE_PITCH ? DIVE : GLIDE;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw,
                       float headPitch) {
        if (player.isInvisible()) {
            return;
        }
        float t = Mth.clamp((morph(player, partialTick) - WING_FROM) / (1F - WING_FROM), 0F, 1F);
        if (t <= 0F) {
            return;
        }
        if (renderer == null) {
            renderer = new WingRenderer();
        }
        WingRig rig = RIGS.computeIfAbsent(player.getId(), id -> new WingRig());
        rig.pose = pose(player);
        rig.grown = Math.max(0F, backOut(t, GROW_OVERSHOOT)) * WING_SCALE;
        rig.unfurl = (1F - backOut(t, UNFURL_OVERSHOOT)) * UNFURL;

        poseStack.pushPose();
        getParentModel().body.translateAndRotate(poseStack);
        poseStack.scale(-1F, -1F, 1F);
        poseStack.translate(0.0, -NECK_HEIGHT, 0.0);
        RenderType type = RenderType.entityCutoutNoCull(TEXTURE);
        renderer.render(poseStack, rig, buffers, type, buffers.getBuffer(type), light, partialTick);
        poseStack.popPose();
    }

    private static final class Seen {
        private boolean out;
        private long at;
        private long flapAt = Long.MIN_VALUE / 2;
    }

    private static final class WingRig implements GeoAnimatable {
        private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this, false);
        private RawAnimation pose = IDLE;
        private float grown = 1F;
        private float unfurl;

        @Override
        public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
            controllers.add(new AnimationController<>(this, "wings", BLEND_TICKS,
                    state -> state.setAndContinue(pose)));
        }

        @Override
        public AnimatableInstanceCache getAnimatableInstanceCache() {
            return cache;
        }

        @Override
        public double getTick(Object object) {
            return RenderUtil.getCurrentTick();
        }
    }

    private static final class WingModel extends GeoModel<WingRig> {
        @Override
        public ResourceLocation getModelResource(WingRig animatable) {
            return MODEL;
        }

        @Override
        public ResourceLocation getTextureResource(WingRig animatable) {
            return TEXTURE;
        }

        @Override
        public ResourceLocation getAnimationResource(WingRig animatable) {
            return ANIMATIONS;
        }
    }

    private static final class WingRenderer extends GeoObjectRenderer<WingRig> {
        private WingRenderer() {
            super(new WingModel());
        }

        @Override
        public void preRender(PoseStack poseStack, WingRig animatable, BakedGeoModel model,
                              MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                              float partialTick, int packedLight, int packedOverlay, int colour) {
            this.objectRenderTranslations = new Matrix4f(poseStack.last().pose());
        }

        @Override
        public void renderRecursively(PoseStack poseStack, WingRig animatable, GeoBone bone, RenderType renderType,
                                      MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                                      float partialTick, int packedLight, int packedOverlay, int colour) {
            if (SHOULDERS.contains(bone.getName())) {
                bone.updateScale(animatable.grown, animatable.grown, animatable.grown);
                bone.setRotY(animatable.unfurl * (bone.getName().startsWith("left") ? 1F : -1F));
            }
            super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender,
                    partialTick, packedLight, packedOverlay, colour);
        }
    }
}
