package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.client.fp.FirstPersonPose;
import org.gumel.jojoha.client.fp.FirstPersonPoses;
import org.gumel.jojoha.combat.fx.MovementFx;
import org.gumel.jojoha.hamon.HamonFx;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class HamonHealHand extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public static final String FIRST_PERSON = MovementFx.HEAL;
    public static final String THIRD_PERSON = "heal_hold";

    private static final float HAND_X = 1F / 16F;
    private static final float HAND_Y = 11F / 16F;
    private static final float FIST_Y = 8.5F / 16F;

    private static final ResourceLocation[] AURA = frames("hamon_particle", 8);
    private static final ResourceLocation[] SPARK = frames("hamon_electricity_particle", 9);

    private static final float AURA_SIZE = 0.22F;
    private static final float AURA_SWELL = 0.04F;
    private static final float SPARK_SIZE = 0.16F;
    private static final int AURA_FRAME_TICKS = 2;
    private static final int SPARK_FRAME_TICKS = 1;
    private static final int SPARK_EVERY = 6;
    private static final int SPARK_LASTS = 3;
    private static final ResourceLocation[] FLAME = auraFrames();
    private static final int FLAMES = 4;
    private static final int FLAME_TICKS = 12;
    private static final float FLAME_RISE = 0.08F;
    private static final float FLAME_SWAY = 0.035F;
    private static final float FLAME_SIZE = 0.5F;
    private static final float FLAME_GROWTH = 0.1F;
    private static final float FLAME_BASE = 0.08F;
    private static final float FLAME_RING = 0.07F;
    private static final int FLAME_PLANES = 3;

    public HamonHealHand(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    private static ResourceLocation[] auraFrames() {
        ResourceLocation[] out = new ResourceLocation[8];
        for (int i = 0; i < out.length; i++) {
            out[i] = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/particle/stand_aura_" + i + ".png");
        }
        return out;
    }

    private static ResourceLocation[] frames(String name, int count) {
        ResourceLocation[] out = new ResourceLocation[count];
        for (int i = 0; i < count; i++) {
            out[i] = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/particle/" + name + (i + 1) + ".png");
        }
        return out;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw,
                       float headPitch) {
        if (player.isInvisible() || !PlayerMoveAnimation.playing(player, THIRD_PERSON)) {
            return;
        }
        draw(poseStack, buffers, getParentModel().leftArm,
                Minecraft.getInstance().gameRenderer.getMainCamera().rotation(), new Quaternionf(), ageInTicks);
    }

    public static void firstPerson(PoseStack poseStack, MultiBufferSource buffers, AbstractClientPlayer player,
                                   ModelPart arm, boolean right) {
        FirstPersonPose pose = FirstPersonPoses.playing();
        if (right || pose == null || !FIRST_PERSON.equals(pose.id())
                || player != Minecraft.getInstance().player) {
            return;
        }
        float time = player.tickCount + Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        Quaternionf upright = new Quaternionf(Minecraft.getInstance().gameRenderer.getMainCamera().rotation()).conjugate();
        draw(poseStack, buffers, arm, new Quaternionf(), upright, time);
    }

    private static void draw(PoseStack poseStack, MultiBufferSource buffers, ModelPart arm, Quaternionf facing,
                             Quaternionf upright, float time) {
        poseStack.pushPose();
        arm.translateAndRotate(poseStack);
        Vector3f hand = poseStack.last().pose().transformPosition(new Vector3f(HAND_X, HAND_Y, 0F));
        Vector3f fist = poseStack.last().pose().transformPosition(new Vector3f(HAND_X, FIST_Y, 0F));
        poseStack.popPose();

        int tick = (int) time;
        float swell = AURA_SIZE + AURA_SWELL * Mth.sin(time * 0.35F);
        sprite(buffers, AURA[(tick / AURA_FRAME_TICKS) % AURA.length], hand, facing, swell, 0xFFFFFFFF);

        for (int i = 0; i < FLAMES; i++) {
            float phase = ((time + i * (FLAME_TICKS / (float) FLAMES)) % FLAME_TICKS) / FLAME_TICKS;
            float size = FLAME_SIZE + FLAME_GROWTH * phase;
            float swayX = FLAME_SWAY * Mth.sin(time * 0.3F + i * 1.7F);
            float swayZ = FLAME_SWAY * Mth.cos(time * 0.27F + i * 2.3F);
            float around = time * 0.08F + i * ((float) Math.PI * 2F / FLAMES);
            Vector3f lift = new Vector3f(swayX + FLAME_RING * Mth.cos(around), FLAME_RISE * phase + size * FLAME_BASE,
                    swayZ + FLAME_RING * Mth.sin(around));
            upright.transform(lift);
            float fade = phase < 0.2F ? phase / 0.2F : 1F - (phase - 0.2F) / 0.8F;
            ResourceLocation frame = FLAME[Math.min(FLAME.length - 1, (int) (phase * FLAME.length))];
            for (int p = 0; p < FLAME_PLANES; p++) {
                Quaternionf plane = new Quaternionf(upright).rotateY((float) Math.PI * p / FLAME_PLANES + i * 0.7F);
                sprite(buffers, frame, new Vector3f(fist).add(lift), plane, size, hamon(fade));
            }
        }

        if (tick % SPARK_EVERY < SPARK_LASTS) {
            sprite(buffers, SPARK[(tick / SPARK_FRAME_TICKS) % SPARK.length], hand, facing, SPARK_SIZE, 0xFFFFFFFF);
        }
    }

    private static int hamon(float strength) {
        float s = Mth.clamp(strength, 0F, 1F);
        int r = Math.round(255F * HamonFx.RED * s);
        int g = Math.round(255F * HamonFx.GREEN * s);
        int b = Math.round(255F * HamonFx.BLUE * s);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    private static void sprite(MultiBufferSource buffers, ResourceLocation texture, Vector3f at, Quaternionf facing,
                               float size, int colour) {
        VertexConsumer out = buffers.getBuffer(RenderType.eyes(texture));
        PoseStack stack = new PoseStack();
        stack.translate(at.x, at.y, at.z);
        stack.mulPose(facing);
        Matrix4f pose = stack.last().pose();
        PoseStack.Pose normal = stack.last();
        float h = size / 2F;
        vertex(out, pose, normal, -h, -h, 0F, 1F, colour);
        vertex(out, pose, normal, h, -h, 1F, 1F, colour);
        vertex(out, pose, normal, h, h, 1F, 0F, colour);
        vertex(out, pose, normal, -h, h, 0F, 0F, colour);
        vertex(out, pose, normal, -h, h, 0F, 0F, colour);
        vertex(out, pose, normal, h, h, 1F, 0F, colour);
        vertex(out, pose, normal, h, -h, 1F, 1F, colour);
        vertex(out, pose, normal, -h, -h, 0F, 1F, colour);
    }

    private static void vertex(VertexConsumer out, Matrix4f pose, PoseStack.Pose normal, float x, float y, float u,
                               float v, int colour) {
        out.addVertex(pose, x, y, 0F).setColor(colour).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT).setNormal(normal, 0F, 0F, 1F);
    }
}
