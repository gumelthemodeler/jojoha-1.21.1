package org.gumel.jojoha.client;

import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.entity.Amalgamation;
import software.bernie.geckolib.model.GeoModel;

public final class AmalgamationModel extends GeoModel<Amalgamation> {

    private static final ResourceLocation MODEL = asset("geo/amalgamation.geo.json");
    private static final ResourceLocation TEXTURE = asset("textures/entity/mob/amalgamation.png");
    private static final ResourceLocation ANIMATION = asset("animations/amalgamation.animation.json");

    private static ResourceLocation asset(String path) {
        return ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, path);
    }

    @Override
    public ResourceLocation getModelResource(Amalgamation animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(Amalgamation animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(Amalgamation animatable) {
        return ANIMATION;
    }
}
