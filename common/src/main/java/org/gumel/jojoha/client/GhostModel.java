package org.gumel.jojoha.client;

import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.entity.CatacombGhost;
import software.bernie.geckolib.model.GeoModel;

public final class GhostModel extends GeoModel<CatacombGhost> {

    private static final ResourceLocation MODEL = asset("geo/ghost.geo.json");
    private static final ResourceLocation TEXTURE = asset("textures/entity/mob/ghost.png");
    private static final ResourceLocation ANIMATION = asset("animations/ghost.animation.json");

    private static ResourceLocation asset(String path) {
        return ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, path);
    }

    @Override
    public ResourceLocation getModelResource(CatacombGhost animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(CatacombGhost animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(CatacombGhost animatable) {
        return ANIMATION;
    }
}
