package org.gumel.jojoha.client;

import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.block.CameraBlockEntity;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

/**
 * Draws the placed camera, and turns it to face the way it was set down.
 *
 * <p>The turning is GeckoLib's own: it reads {@code HorizontalDirectionalBlock.FACING} off the state
 * and rotates the model to match, which is the whole reason CameraBlock uses vanilla's property
 * rather than declaring one of its own.
 */
public final class CameraBlockRenderer extends GeoBlockRenderer<CameraBlockEntity> {
    public CameraBlockRenderer() {
        super(new Model());
    }

    /** Where the three files live. One camera, so they are constants rather than lookups. */
    private static final class Model extends GeoModel<CameraBlockEntity> {
        private static final ResourceLocation GEO =
                ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "geo/camera_placeable.geo.json");
        private static final ResourceLocation TEXTURE =
                ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/block/camera_model.png");
        private static final ResourceLocation ANIMATION =
                ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "animations/camera.animation.json");

        @Override
        public ResourceLocation getModelResource(CameraBlockEntity camera) {
            return GEO;
        }

        @Override
        public ResourceLocation getTextureResource(CameraBlockEntity camera) {
            return TEXTURE;
        }

        @Override
        public ResourceLocation getAnimationResource(CameraBlockEntity camera) {
            return ANIMATION;
        }
    }
}
