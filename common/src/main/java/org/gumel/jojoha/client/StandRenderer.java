package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.StandMovementState;
import org.gumel.jojoha.stand.StandShadowForm;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.util.Color;
import software.bernie.geckolib.util.RenderUtil;

import java.util.Set;
import java.util.function.ToIntFunction;

public final class StandRenderer extends GeoEntityRenderer<StandEntity> {
    private void addGlowLayer() {
        for (int i = 0; i < ThornRope.GLOW_PASSES; i++) {
            addRenderLayer(new StandGlowLayer(this));
        }
    }

    private static final float BANK_PER_SPEED = 45F;
    private static final float MAX_BANK_DEGREES = 20F;
    private static final float BANK_SMOOTHING = 0.25F;
    private static final double BANK_DEADZONE = 0.012;

    private float bankRoll;
    private float bankPitch;

    private int bankEntity = -1;
    private int bankTick = -1;
    private float bankPartial = -1F;

    private Matrix4f standFrame = new Matrix4f();

    private ManifestFigure.Look look;
    private Matrix4f toStand;
    private final float[] cubeFlash = new float[1];
    private final int[] shadedOverlay = new int[1];

    private float outline;
    private Set<String> framed = Set.of();
    private ToIntFunction<GeoBone> tint;

    void outlined(float width, Set<String> framedBones, ToIntFunction<GeoBone> boneTint, Runnable draw) {
        outline = width;
        framed = framedBones;
        tint = boneTint;
        try {
            draw.run();
        } finally {
            outline = 0F;
            framed = Set.of();
            tint = null;
        }
    }

    Matrix4f standFrame() {
        return standFrame;
    }

    @Override
    public void scaleModelForRender(float widthScale, float heightScale, PoseStack poseStack, StandEntity animatable,
                                    BakedGeoModel model, boolean isReRender, float partialTick,
                                    int packedLight, int packedOverlay) {
        super.scaleModelForRender(widthScale, heightScale, poseStack, animatable, model, isReRender,
                partialTick, packedLight, packedOverlay);
        if (!isReRender) {
            standFrame = new Matrix4f(poseStack.last().pose());
            if (look != null) {
                look.plan = ManifestFigure.plan(model);
                toStand = new Matrix4f(standFrame).invert();
            }
        }
    }

    @Override
    public void renderCubesOfBone(PoseStack poseStack, GeoBone bone, VertexConsumer buffer, int packedLight,
                                  int packedOverlay, int colour) {
        if (look == null && outline > 0F) {
            StandOutline.cubes(poseStack, bone, buffer, outline, framed.contains(bone.getName()), packedLight,
                    packedOverlay, tint == null ? colour : tint.applyAsInt(bone));
            return;
        }
        if (look == null) {
            super.renderCubesOfBone(poseStack, bone, buffer, packedLight, packedOverlay, colour);
            return;
        }
        if (bone.isHidden()) {
            return;
        }

        for (GeoCube cube : bone.getCubes()) {
            float scale = ManifestFigure.snapPop(look, cube, cubeFlash);
            if (scale <= 0F) {
                continue;
            }
            poseStack.pushPose();
            RenderUtil.translateToPivotPoint(poseStack, cube);
            RenderUtil.rotateMatrixAroundCube(poseStack, cube);
            RenderUtil.translateAwayFromPivotPoint(poseStack, cube);
            if (scale != 1F) {
                Vector3f centre = ManifestFigure.centre(cube);
                poseStack.translate(centre.x, centre.y, centre.z);
                poseStack.scale(scale, scale, scale);
                poseStack.translate(-centre.x, -centre.y, -centre.z);
            }

            Matrix3f normalState = poseStack.last().normal();
            Matrix4f poseState = new Matrix4f(poseStack.last().pose());
            for (GeoQuad quad : cube.quads()) {
                if (quad == null) {
                    continue;
                }
                Vector3f normal = normalState.transform(new Vector3f(quad.normal()));
                RenderUtil.fixInvertedFlatCube(cube, normal);
                createVerticesOfQuad(quad, poseState, normal, buffer, packedLight, packedOverlay, colour);
            }
            poseStack.popPose();
        }
        cubeFlash[0] = 0F;

        ManifestFigure.noteHead(look, poseStack, bone);
    }

    @Override
    public void applyRenderLayers(PoseStack poseStack, StandEntity animatable, BakedGeoModel model,
                                  RenderType renderType, MultiBufferSource bufferSource,
                                  VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        if (look != null) {
            if (look.glows() && !look.glowPass) {
                RenderType glow = ManifestFigure.glowType();
                look.glowPass = true;
                try {
                    reRender(model, poseStack, bufferSource, animatable, glow, bufferSource.getBuffer(glow),
                            partialTick, packedLight, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
                } finally {
                    look.glowPass = false;
                }
            }
            return;
        }
        super.applyRenderLayers(poseStack, animatable, model, renderType, bufferSource, buffer, partialTick,
                packedLight, packedOverlay);
    }

    @Override
    public void createVerticesOfQuad(GeoQuad quad, Matrix4f poseState, Vector3f normal, VertexConsumer buffer,
                                     int packedLight, int packedOverlay, int colour) {
        if (look == null || toStand == null) {
            super.createVerticesOfQuad(quad, poseState, normal, buffer, packedLight, packedOverlay, colour);
            return;
        }

        for (GeoVertex vertex : quad.vertices()) {
            Vector3f position = vertex.position();
            Vector4f placed = poseState.transform(new Vector4f(position.x(), position.y(), position.z(), 1F));
            float height = toStand.transform(new Vector4f(placed)).y();
            if (look.glowPass) {
                int g = look.glow(height, cubeFlash[0]);
                buffer.addVertex(placed.x(), placed.y(), placed.z(), 0xFF000000 | (g << 16) | (g << 8) | g,
                        vertex.texU(), vertex.texV(), OverlayTexture.NO_OVERLAY,
                        LightTexture.FULL_BRIGHT, normal.x(), normal.y(), normal.z());
                continue;
            }
            int shaded = look.shade(height, colour, cubeFlash[0], shadedOverlay);

            buffer.addVertex(placed.x(), placed.y(), placed.z(), shaded, vertex.texU(), vertex.texV(),
                    shadedOverlay[0], packedLight, normal.x(), normal.y(), normal.z());
        }
    }

    public StandRenderer(EntityRendererProvider.Context context) {
        super(context, new StandModel());
        addRenderLayer(new StandBarrageArmsLayer(this));
        addRenderLayer(new StandHeldItemLayer(this));
        addRenderLayer(new StandKnivesLayer(this));
        addRenderLayer(new StandShadowGlowLayer(this));
        addRenderLayer(new TimeStopChargeGlowLayer(this));
        addRenderLayer(new StandRippleLayer(this));
        addRenderLayer(new MagicianFlameOverlayLayer(this));
        addRenderLayer(new ChariotAfterimageLayer(this));
        addRenderLayer(new StandEmergeLayer(this));
        addRenderLayer(new HeavenGhostLayer(this));
    }

    @Override
    public void render(StandEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        if (StandViewAlpha.of(entity) <= 0.001F) {
            return;
        }

        int light = StandShadowForm.of(entity) != null
                ? LightTexture.FULL_BRIGHT
                : packedLight;

        if (entity.isManifesting()) {
            look = ManifestFigure.look(entity, partialTick);
            toStand = null;
            try {
                super.render(entity, entityYaw, partialTick, poseStack, bufferSource, look.light(light));
                ManifestFigure.drawEyes(look, bufferSource);
            } finally {
                look = null;
                toStand = null;
            }
            return;
        }

        MultiBufferSource drawInto = !entity.isPreviewMode() && (StandViewAlpha.of(entity) < 1F
                || StandShadowForm.of(entity) != null)
                ? WorldVfx.afterEntities(bufferSource) : bufferSource;
        super.render(entity, entityYaw, partialTick, poseStack, drawInto, light);
        StandGuardPlate.render(entity, partialTick, poseStack, drawInto);
    }

    {
        addGlowLayer();
    }

    private static boolean riding(StandEntity stand) {
        return stand.getStandType().form().isBound()
                || stand.getTrustTier().isPartialManifestation() && !stand.isPiloted();
    }

    @Override
    public Vec3 getRenderOffset(StandEntity entity, float partialTick) {
        Player owner = entity.getOwner();
        if (owner == null || !riding(entity)) {
            return super.getRenderOffset(entity, partialTick).add(entity.glideOffset(partialTick))
                    .add(BarrageFlow.bodyOffset(entity, partialTick)).add(SummonFlair.offset(entity, partialTick));
        }

        return new Vec3(
                Mth.lerp(partialTick, owner.xo, owner.getX())
                        - Mth.lerp(partialTick, entity.xo, entity.getX()),
                Mth.lerp(partialTick, owner.yo, owner.getY())
                        - Mth.lerp(partialTick, entity.yo, entity.getY()),
                Mth.lerp(partialTick, owner.zo, owner.getZ())
                        - Mth.lerp(partialTick, entity.zo, entity.getZ()));
    }

    @Override
    protected void applyRotations(StandEntity animatable, PoseStack poseStack, float ageInTicks,
                                  float rotationYaw, float partialTick, float nativeScale) {
        Player boundTo = riding(animatable) ? animatable.getOwner() : null;
        if (boundTo != null) {
            super.applyRotations(animatable, poseStack, ageInTicks,
                    Mth.rotLerp(partialTick, boundTo.yBodyRotO, boundTo.yBodyRot),
                    partialTick, nativeScale);
            return;
        }

        rotationYaw = SummonFlair.yaw(animatable, partialTick, rotationYaw) + animatable.glideYaw(partialTick);
        super.applyRotations(animatable, poseStack, ageInTicks, rotationYaw, partialTick, nativeScale);

        if (PilotView.drawing(animatable)) {
            return;
        }

        StandMovementState state = animatable.movementState();
        boolean squaredUp = state.isCombatStance()
                || state == StandMovementState.PURSUING
                || state == StandMovementState.WORKING
                || animatable.getTrustTier().isPartialManifestation();

        double dx = squaredUp ? 0.0 : animatable.getX() - animatable.xo;
        double dz = squaredUp ? 0.0 : animatable.getZ() - animatable.zo;

        float yawRadians = (float) Math.toRadians(rotationYaw);
        double forward = -dx * Math.sin(yawRadians) + dz * Math.cos(yawRadians);
        double lateral = dx * Math.cos(yawRadians) + dz * Math.sin(yawRadians);

        if (Math.abs(lateral) < BANK_DEADZONE) {
            lateral = 0.0;
        }
        if (Math.abs(forward) < BANK_DEADZONE) {
            forward = 0.0;
        }

        float targetRoll = (float) Mth.clamp(lateral * BANK_PER_SPEED, -MAX_BANK_DEGREES, MAX_BANK_DEGREES);
        float targetPitch = (float) Mth.clamp(-forward * BANK_PER_SPEED, -MAX_BANK_DEGREES, MAX_BANK_DEGREES);

        if (animatable.getId() != bankEntity || animatable.tickCount != bankTick || partialTick != bankPartial) {
            bankEntity = animatable.getId();
            bankTick = animatable.tickCount;
            bankPartial = partialTick;
            bankRoll = Mth.lerp(BANK_SMOOTHING, bankRoll, targetRoll);
            bankPitch = Mth.lerp(BANK_SMOOTHING, bankPitch, targetPitch);
        }

        poseStack.mulPose(Axis.ZP.rotationDegrees(bankRoll));
        poseStack.mulPose(Axis.XP.rotationDegrees(bankPitch));
    }

    @Override
    public int getPackedOverlay(StandEntity animatable, float u, float partialTick) {
        float white = Math.max(animatable.skinSwapWhite(partialTick), SummonFlair.white(animatable));
        return OverlayTexture.pack(OverlayTexture.u(Math.max(u, white)),
                OverlayTexture.v(white <= 0.001F && sharesHurt(animatable)));
    }

    private static boolean sharesHurt(StandEntity stand) {
        if (stand.hurtTime > 0 || stand.deathTime > 0) {
            return true;
        }
        Player owner = stand.getOwner();
        return owner != null && (owner.hurtTime > 0 || owner.deathTime > 0);
    }

    @Override
    public Color getRenderColor(StandEntity animatable, float partialTick, int packedLight) {
        float shade = StandShadowForm.of(animatable) != null
                ? StandShadowGlowLayer.BODY_ALPHA
                : 1F;
        float summon = SummonFlair.bodyShade(animatable);
        return Color.ofRGBA(summon, summon, summon, StandViewAlpha.of(animatable) * shade);
    }

    @Override
    public boolean shouldShowName(StandEntity animatable) {
        return false;
    }

}
