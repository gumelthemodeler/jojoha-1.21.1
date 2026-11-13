package org.gumel.jojoha.client;

import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.entity.VampireRat;
import software.bernie.geckolib.model.GeoModel;

public final class VampireRatModel extends GeoModel<VampireRat> {

    private static final ResourceLocation MODEL = asset("geo/vampire_rat.geo.json");
    private static final ResourceLocation TEXTURE = asset("textures/entity/mob/vampire_rat.png");
    private static final ResourceLocation ANIMATION = asset("animations/rat.animation.json");

    private static ResourceLocation asset(String path) {
        return ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, path);
    }

    @Override
    public ResourceLocation getModelResource(VampireRat animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(VampireRat animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(VampireRat animatable) {
        return ANIMATION;
    }
}
