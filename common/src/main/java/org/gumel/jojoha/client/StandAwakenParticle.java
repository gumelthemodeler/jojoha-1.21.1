package org.gumel.jojoha.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

public final class StandAwakenParticle extends TextureSheetParticle {
    private final SpriteSet sprites;

    private StandAwakenParticle(ClientLevel level, double x, double y, double z,
                                 double dx, double dy, double dz, SpriteSet sprites) {
        super(level, x, y, z, 0.0, 0.0, 0.0);
        this.sprites = sprites;
        this.gravity = 0F;
        this.hasPhysics = false;
        this.friction = 0.92F;

        this.xd = dx;
        this.yd = dy;
        this.zd = dz;

        this.quadSize = 0.18F + this.random.nextFloat() * 0.14F;
        this.lifetime = 18 + this.random.nextInt(10);
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        this.setSpriteFromAge(sprites);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return EmissiveParticleRenderType.INSTANCE;
    }

    @Override
    public int getLightColor(float partialTick) {
        return 240;
    }

    public static final class RedProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public RedProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                        double dx, double dy, double dz) {
            StandAwakenParticle particle = new StandAwakenParticle(level, x, y, z, dx, dy, dz, sprites);
            particle.setColor(1.0F, 0.12F, 0.08F);
            return particle;
        }
    }

    public static final class BlueProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public BlueProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                        double dx, double dy, double dz) {
            StandAwakenParticle particle = new StandAwakenParticle(level, x, y, z, dx, dy, dz, sprites);
            particle.setColor(0.32F, 0.58F, 1.0F);
            return particle;
        }
    }

    public static final class PinkProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public PinkProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                        double dx, double dy, double dz) {
            StandAwakenParticle particle = new StandAwakenParticle(level, x, y, z, dx, dy, dz, sprites);
            particle.setColor(1.0F, 0.42F, 0.86F);
            return particle;
        }
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                        double dx, double dy, double dz) {
            return new StandAwakenParticle(level, x, y, z, dx, dy, dz, sprites);
        }
    }
}
