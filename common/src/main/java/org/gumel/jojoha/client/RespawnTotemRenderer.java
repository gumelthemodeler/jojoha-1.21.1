package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.block.RespawnTotemBlock;
import org.gumel.jojoha.block.RespawnTotemBlockEntity;
import org.gumel.jojoha.data.ClientPlayerDataCache;
import org.gumel.jojoha.data.TotemBinding;

public final class RespawnTotemRenderer implements BlockEntityRenderer<RespawnTotemBlockEntity> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/block/respawn_totem.png");
    private static final ResourceLocation OVERLAY =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/block/respawn_totem_overlay.png");
    private static final ResourceLocation RING =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/fx/respawn_totem_ring.png");

    private static final int[] LIFE_COLOURS = {0xF5D83A, 0xF28A1E, 0xE8322A};

    private static final float JAW_DROP = 5.0F;

    private static final float BLOOM = 0.8F;
    private static final float[][] HALO_SHELLS = {{1.06F, 0.45F}, {1.14F, 0.25F}, {1.24F, 0.12F}};
    private static final float[] HEAD_CENTRE = {-8F, -27F, 8F};
    private static final float GLOW_BREATH = 0.25F;
    private static final float GLOW_BREATH_SPEED = (float) (2 * Math.PI / 80);

    private static final float RING_RADIUS = 1.5F;
    private static final float RING_LIFT = 0.02F;
    private static final float RING_DEGREES_PER_TICK = 360F / 160F;
    private static final float RING_ALPHA = 0.9F;

    private final ModelPart root;
    private final ModelPart jaws;
    private final float jawsShut;

    public RespawnTotemRenderer(BlockEntityRendererProvider.Context context) {
        this.root = context.bakeLayer(RespawnTotemModel.LAYER);
        this.jaws = root.getChild("bone").getChild("bone2").getChild("bone3");
        this.jawsShut = jaws.y;
    }

    @Override
    public void render(RespawnTotemBlockEntity totem, float partialTick, PoseStack pose, MultiBufferSource buffers,
                       int light, int overlay) {
        Level level = totem.getLevel();
        if (level == null) {
            return;
        }
        int lives = livesFor(totem);
        long now = level.getGameTime();
        float open = totem.opening(lives, now, partialTick);
        int colour = colourOf(lives > 0 ? lives : totem.shownLives());

        pose.pushPose();
        pose.translate(0.5F, 0F, 0.5F);
        pose.mulPose(Axis.YP.rotationDegrees(180F - totem.getBlockState().getValue(RespawnTotemBlock.FACING).toYRot()));
        pose.pushPose();
        pose.translate(0F, 1.5F, 0F);
        pose.scale(-1F, -1F, 1F);

        jaws.y = jawsShut + JAW_DROP * open;
        root.render(pose, buffers.getBuffer(RenderType.entityCutout(TEXTURE)), light, overlay);
        if (open > 0F) {
            glow(pose, buffers, colour, open, now + partialTick);
        }
        jaws.y = jawsShut;
        pose.popPose();

        if (open > 0F) {
            ring(pose, buffers, withAlpha(colour, open * RING_ALPHA), (now + partialTick) * RING_DEGREES_PER_TICK);
        }
        pose.popPose();
    }

    private void glow(PoseStack pose, MultiBufferSource buffers, int colour, float open, float ticks) {
        root.render(pose, buffers.getBuffer(RenderType.entityTranslucentEmissive(OVERLAY)),
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, withAlpha(colour, open));

        float breath = 1F - GLOW_BREATH * (0.5F + 0.5F * (float) Math.sin(ticks * GLOW_BREATH_SPEED));
        VertexConsumer added = buffers.getBuffer(RenderType.eyes(OVERLAY));
        root.render(pose, added, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                dimmed(colour, BLOOM * open * breath));

        ModelPart body = root.getChild("bone");
        ModelPart faces = body.getChild("bone2");
        for (float[] shell : HALO_SHELLS) {
            pose.pushPose();
            body.translateAndRotate(pose);
            pose.translate(HEAD_CENTRE[0] / 16F, HEAD_CENTRE[1] / 16F, HEAD_CENTRE[2] / 16F);
            pose.scale(shell[0], shell[0], shell[0]);
            pose.translate(-HEAD_CENTRE[0] / 16F, -HEAD_CENTRE[1] / 16F, -HEAD_CENTRE[2] / 16F);
            faces.render(pose, added, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                    dimmed(colour, shell[1] * open * breath));
            pose.popPose();
        }
    }

    private static int dimmed(int rgb, float strength) {
        float k = Math.max(0F, Math.min(strength, 1F));
        int r = (int) ((rgb >> 16 & 255) * k), g = (int) ((rgb >> 8 & 255) * k), b = (int) ((rgb & 255) * k);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    private static void ring(PoseStack pose, MultiBufferSource buffers, int colour, float degrees) {
        pose.pushPose();
        pose.translate(0F, RING_LIFT, 0F);
        pose.mulPose(Axis.YP.rotationDegrees(degrees));
        PoseStack.Pose at = pose.last();
        VertexConsumer out = buffers.getBuffer(RenderType.entityTranslucentEmissive(RING));
        float r = RING_RADIUS;
        corner(out, at, -r, -r, 0F, 0F, colour);
        corner(out, at, -r, r, 0F, 1F, colour);
        corner(out, at, r, r, 1F, 1F, colour);
        corner(out, at, r, -r, 1F, 0F, colour);
        pose.popPose();
    }

    private static void corner(VertexConsumer out, PoseStack.Pose at, float x, float z, float u, float v, int colour) {
        out.addVertex(at, x, 0F, z).setColor(colour).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT).setNormal(at, 0F, 1F, 0F);
    }

    private static int livesFor(RespawnTotemBlockEntity totem) {
        if (Minecraft.getInstance().player == null || totem.getLevel() == null) {
            return 0;
        }
        TotemBinding binding = ClientPlayerDataCache.data.totem;
        return binding.at(totem.getLevel().dimension(), totem.getBlockPos()) ? binding.lives() : 0;
    }

    private static int colourOf(int lives) {
        return LIFE_COLOURS[Math.max(1, Math.min(lives, LIFE_COLOURS.length)) - 1];
    }

    private static int withAlpha(int rgb, float alpha) {
        return ((int) (Math.max(0F, Math.min(alpha, 1F)) * 255F) << 24) | (rgb & 0xFFFFFF);
    }

    @Override
    public boolean shouldRenderOffScreen(RespawnTotemBlockEntity totem) {
        return true;
    }

    public AABB getRenderBoundingBox(RespawnTotemBlockEntity totem) {
        return new AABB(totem.getBlockPos()).inflate(RING_RADIUS, 0.0, RING_RADIUS).expandTowards(0.0, 1.2, 0.0);
    }
}
