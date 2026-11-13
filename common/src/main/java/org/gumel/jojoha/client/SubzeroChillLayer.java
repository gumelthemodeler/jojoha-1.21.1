package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class SubzeroChillLayer
        extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation WHITE =
            ResourceLocation.withDefaultNamespace("textures/misc/white.png");

    private static final float PEAK_ALPHA = 0.55F;

    private static final float RED = 0.34F;
    private static final float GREEN = 0.66F;
    private static final float BLUE = 0.95F;

    public SubzeroChillLayer(
            RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                       AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        float now = (float) minecraft.level.getGameTime() + partialTick;
        float chill = SubzeroChill.strength(player.getUUID(), now);
        if (chill <= 0.004F) {
            return;
        }

        PlayerModel<AbstractClientPlayer> model = getParentModel();

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(WHITE));
        model.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY,
                argb(chill * PEAK_ALPHA));
    }

    private static int argb(float alpha) {
        return (Mth.clamp(Math.round(alpha * 255F), 0, 255) << 24)
                | (Math.round(RED * 255F) << 16)
                | (Math.round(GREEN * 255F) << 8)
                | Math.round(BLUE * 255F);
    }
}
