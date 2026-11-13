package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.entity.CatacombGhost;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.util.Color;

public final class GhostRenderer extends GeoEntityRenderer<CatacombGhost> {

    private static final int ALPHA = 0xC8;

    public GhostRenderer(EntityRendererProvider.Context context) {
        super(context, new GhostModel());
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(CatacombGhost ghost, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
                       int packedLight) {
        super.render(ghost, yaw, partialTick, poseStack, buffers, LightTexture.FULL_BRIGHT);
    }

    @Override
    public RenderType getRenderType(CatacombGhost ghost, ResourceLocation texture, MultiBufferSource buffers,
                                    float partialTick) {
        return RenderType.entityTranslucentEmissive(texture);
    }

    @Override
    public Color getRenderColor(CatacombGhost ghost, float partialTick, int packedLight) {
        return new Color(ALPHA << 24 | ghost.shade());
    }
}
