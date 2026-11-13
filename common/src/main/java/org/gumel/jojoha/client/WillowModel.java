package org.gumel.jojoha.client;

import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.entity.WillowEntity;
import software.bernie.geckolib.model.GeoModel;

public final class WillowModel extends GeoModel<WillowEntity> {

    private static final ResourceLocation MODEL = asset("geo/willow.geo.json");
    private static final ResourceLocation TEXTURE = asset("textures/entity/mob/willow_trader.png");
    private static final ResourceLocation ANIMATION = asset("animations/willow.animation.json");

    private static ResourceLocation asset(String path) {
        return ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, path);
    }

    @Override
    public ResourceLocation getModelResource(WillowEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(WillowEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(WillowEntity animatable) {
        return ANIMATION;
    }
}
