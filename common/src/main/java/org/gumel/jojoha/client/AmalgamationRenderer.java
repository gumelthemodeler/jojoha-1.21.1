package org.gumel.jojoha.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.gumel.jojoha.entity.Amalgamation;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class AmalgamationRenderer extends GeoEntityRenderer<Amalgamation> {

    public AmalgamationRenderer(EntityRendererProvider.Context context) {
        super(context, new AmalgamationModel());
        this.shadowRadius = 0.9F;
    }

    @Override
    protected float getDeathMaxRotation(Amalgamation animatable) {
        return 0.0F;
    }
}
