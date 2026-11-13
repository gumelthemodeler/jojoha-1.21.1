package org.gumel.jojoha.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.gumel.jojoha.entity.VampireEntity;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class VampireRenderer extends GeoEntityRenderer<VampireEntity> {

    public VampireRenderer(EntityRendererProvider.Context context) {
        super(context, new VampireModel());
        this.shadowRadius = 0.5F;
    }
}
