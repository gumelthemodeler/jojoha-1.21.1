package org.gumel.jojoha.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.gumel.jojoha.entity.VampireRat;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class VampireRatRenderer extends GeoEntityRenderer<VampireRat> {

    public VampireRatRenderer(EntityRendererProvider.Context context) {
        super(context, new VampireRatModel());
        this.shadowRadius = 0.25F;
    }
}
