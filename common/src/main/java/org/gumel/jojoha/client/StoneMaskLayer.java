package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
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

public final class StoneMaskLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation WHITE = ResourceLocation.withDefaultNamespace("textures/misc/white.png");

    private static final float EYE_GLOW = 0.75F;

    private final StoneMaskModel mask;

    public StoneMaskLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent,
                          ModelPart root) {
        super(parent);
        this.mask = new StoneMaskModel(root);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                       AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {

        if (player.isInvisible() || player.isSpectator()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        float now = minecraft.level == null ? 0F : (float) minecraft.level.getGameTime() + partialTick;

        if (!StoneMaskState.wornBy(player)) {
            return;
        }

        float flash = StandAwakeningRays.burn(player, now);

        float turn = StoneMaskState.turnProgress(player, now);

        poseStack.pushPose();
        getParentModel().head.translateAndRotate(poseStack);
        AwakeningLight.face(player, poseStack, now);

        boolean aja = StoneMaskState.ajaOn(player);
        mask.setAja(aja);

        mask.renderToBuffer(poseStack, bufferSource.getBuffer(maskType(StoneMaskModel.dormant(aja))),
                packedLight, OverlayTexture.NO_OVERLAY, -1);

        if (flash > 0F) {
            float[] tint = StandAwakeningRays.glowTint(player.getUUID());
            mask.renderToBuffer(poseStack, bufferSource.getBuffer(RenderType.eyes(WHITE)),
                    LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                    tinted(tint, flash * MASK_BURN));
        }

        if (turn > 0F) {
            mask.renderToBuffer(poseStack, bufferSource.getBuffer(RenderType.eyes(StoneMaskModel.glow(aja))),
                    LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, argb(EYE_GLOW * turn));
        }

        poseStack.popPose();
    }

    private static RenderType maskType(ResourceLocation sheet) {
        return RenderType.entityTranslucent(sheet);
    }

    private static final float MASK_BURN = 0.85F;

    private static int tinted(float[] tint, float strength) {
        int r = Mth.clamp(Math.round(tint[0] * strength * 255F), 0, 255);
        int g = Mth.clamp(Math.round(tint[1] * strength * 255F), 0, 255);
        int b = Mth.clamp(Math.round(tint[2] * strength * 255F), 0, 255);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static int argb(float strength) {
        int c = Mth.clamp(Math.round(strength * 255F), 0, 255);
        return 0xFF000000 | (c << 16) | (c << 8) | c;
    }
}
