package org.gumel.jojoha.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.gumel.jojoha.entity.WillowEntity;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class WillowRenderer extends GeoEntityRenderer<WillowEntity> {

    public WillowRenderer(EntityRendererProvider.Context context) {
        super(context, new WillowModel());
    }
}
