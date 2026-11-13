package org.gumel.jojoha.client;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public final class RainbowHamon {
    private static final float HUE_PER_TICK = 0.02F;

    private static final float HUE_SPREAD = 0.35F;

    private static final float SATURATION = 0.7F;

    private RainbowHamon() {
    }

    public static ParticleProvider<SimpleParticleType> wrap(ParticleProvider<SimpleParticleType> base) {
        return (type, level, x, y, z, dx, dy, dz) -> {
            Particle particle = base.createParticle(type, level, x, y, z, dx, dy, dz);
            if (particle != null) {
                float hue = (level.getGameTime() * HUE_PER_TICK + level.random.nextFloat() * HUE_SPREAD) % 1F;
                int rgb = Mth.hsvToRgb(hue, SATURATION, 1F);
                particle.setColor(((rgb >> 16) & 0xFF) / 255F, ((rgb >> 8) & 0xFF) / 255F, (rgb & 0xFF) / 255F);
            }
            return particle;
        };
    }
}
