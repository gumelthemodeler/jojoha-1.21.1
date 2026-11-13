package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.entity.npc.HamonStudent;
import org.gumel.jojoha.entity.npc.JojoNpc;
import org.gumel.jojoha.entity.npc.Quests;
import org.joml.Matrix4f;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class NpcGeoRenderer extends GeoEntityRenderer<JojoNpc> {
    private static final ResourceLocation AVAILABLE = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/fx/quest_indicator_available.png");
    private static final ResourceLocation HANDIN = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/fx/quest_indicator_handin.png");
    private static final float SHADOW = 0.5F;
    private static final float MARK_SIZE = 0.6F;
    private static final float MARK_HEIGHT = 0.7F;
    private static final float MARK_ABOVE_NAME = 1.0F;
    private static final float MARK_BOB = 0.06F;
    private static final float MARK_BOB_SPEED = 0.12F;

    public NpcGeoRenderer(EntityRendererProvider.Context context) {
        super(context, new NpcGeoModel());
        this.shadowRadius = SHADOW;
    }

    @Override
    public void render(JojoNpc npc, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        super.render(npc, yaw, partialTick, pose, buffers, light);
        if (npc instanceof HamonStudent) {
            ResourceLocation marker = ClientQuests.anyReady() ? HANDIN
                    : ClientQuests.offering(Quests.HAMON) ? AVAILABLE : null;
            if (marker != null) {
                mark(npc, marker, partialTick, pose, buffers);
            }
        }
    }

    private void mark(JojoNpc npc, ResourceLocation marker, float partialTick, PoseStack pose, MultiBufferSource buffers) {
        float bob = Mth.sin((npc.tickCount + partialTick) * MARK_BOB_SPEED) * MARK_BOB;
        float lift = npc.getBbHeight() + (npc.isCustomNameVisible() ? MARK_ABOVE_NAME : MARK_HEIGHT);
        pose.pushPose();
        pose.translate(0F, lift + bob, 0F);
        pose.mulPose(entityRenderDispatcher.cameraOrientation());
        pose.scale(-MARK_SIZE, -MARK_SIZE, MARK_SIZE);
        Matrix4f matrix = pose.last().pose();
        VertexConsumer quad = buffers.getBuffer(RenderType.text(marker));
        quad.addVertex(matrix, -0.5F, -0.5F, 0F).setColor(-1).setUv(0F, 0F).setLight(LightTexture.FULL_BRIGHT);
        quad.addVertex(matrix, -0.5F, 0.5F, 0F).setColor(-1).setUv(0F, 1F).setLight(LightTexture.FULL_BRIGHT);
        quad.addVertex(matrix, 0.5F, 0.5F, 0F).setColor(-1).setUv(1F, 1F).setLight(LightTexture.FULL_BRIGHT);
        quad.addVertex(matrix, 0.5F, -0.5F, 0F).setColor(-1).setUv(1F, 0F).setLight(LightTexture.FULL_BRIGHT);
        pose.popPose();
    }
}
