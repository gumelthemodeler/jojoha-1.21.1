package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.stand.StandEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StandGlowLayer extends AutoGlowingGeoLayer<StandEntity> {
    private static final Map<ResourceLocation, Boolean> HAS_MASK =
            new ConcurrentHashMap<>();

    public StandGlowLayer(GeoRenderer<StandEntity> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, StandEntity animatable, BakedGeoModel model,
                       RenderType renderType, MultiBufferSource buffers, VertexConsumer consumer,
                       float partialTick, int light, int overlay) {
        if (!hasMask(getRenderer().getTextureLocation(animatable))) {
            return;
        }

        super.render(poseStack, animatable, model, renderType, buffers, consumer,
                partialTick, light, overlay);
    }

    static boolean hasMask(ResourceLocation texture) {
        return HAS_MASK.computeIfAbsent(texture, path -> {
            String withMask = path.getPath().replace(".png", "_glowmask.png");
            return Minecraft.getInstance().getResourceManager()
                    .getResource(ResourceLocation.fromNamespaceAndPath(path.getNamespace(), withMask))
                    .isPresent();
        });
    }

    public static void forget() {
        HAS_MASK.clear();
    }
}
