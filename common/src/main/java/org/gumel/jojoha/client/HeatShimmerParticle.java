package org.gumel.jojoha.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public class HeatShimmerParticle extends TextureSheetParticle {
    private static final double RISE = 0.03;
    private static final double SWAY = 0.012;
    private static final float PEAK_ALPHA = 0.35F;
    private static final float SIZE = 0.25F;

    private final SpriteSet sprites;
    private final float phase;

    protected HeatShimmerParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z, 0.0, 0.0, 0.0);
        this.sprites = sprites;
        this.gravity = 0F;
        this.hasPhysics = false;
        this.friction = 1F;
        this.xd = 0.0;
        this.yd = RISE + this.random.nextDouble() * RISE * 0.5;
        this.zd = 0.0;
        this.quadSize = SIZE;
        this.lifetime = 20 + this.random.nextInt(12);
        this.phase = this.random.nextFloat() * Mth.TWO_PI;
        this.alpha = 0F;
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.removed) {
            return;
        }
        this.setSpriteFromAge(sprites);
        this.xd = Mth.sin(phase + this.age * 0.45F) * SWAY;
        float progress = Mth.clamp(this.age / (float) this.lifetime, 0F, 1F);
        this.alpha = PEAK_ALPHA * (progress < 0.3F ? progress / 0.3F : 1F - (progress - 0.3F) / 0.7F);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double dx, double dy, double dz) {
            return new HeatShimmerParticle(level, x, y, z, sprites);
        }
    }
}
