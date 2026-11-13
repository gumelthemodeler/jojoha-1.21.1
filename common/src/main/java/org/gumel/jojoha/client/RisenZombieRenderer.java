package org.gumel.jojoha.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.gumel.jojoha.entity.RisenZombie;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class RisenZombieRenderer extends GeoEntityRenderer<RisenZombie> {

    public RisenZombieRenderer(EntityRendererProvider.Context context) {
        super(context, new RisenZombieModel());
        this.shadowRadius = 0.5F;
    }
}
