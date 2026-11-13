package org.gumel.jojoha.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import org.gumel.jojoha.hamon.HamonFx;

public final class HamonParticle extends TextureSheetParticle {

    private final SpriteSet sprites;

    private HamonParticle(ClientLevel level, double x, double y, double z,
                          double dx, double dy, double dz,
                          SpriteSet sprites, float size, int lifetime, float friction) {
        super(level, x, y, z, 0.0, 0.0, 0.0);
        this.sprites = sprites;

        this.xd = dx;
        this.yd = dy;
        this.zd = dz;

        this.gravity = 0F;
        this.hasPhysics = false;
        this.friction = friction;

        this.quadSize = size;
        this.lifetime = lifetime;
        this.roll = this.random.nextFloat() * Mth.TWO_PI;
        this.oRoll = this.roll;

        this.setColor(HamonFx.RED, HamonFx.GREEN, HamonFx.BLUE);
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.removed) {
            this.setSpriteFromAge(sprites);
        }
    }

    @Override
    public int getLightColor(float partialTick) {
        return 240;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return EmissiveParticleRenderType.INSTANCE;
    }

    public static final class RippleProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public RippleProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double dx, double dy, double dz) {
            return new HamonParticle(level, x, y, z, dx, dy, dz, sprites,
                    0.55F + level.random.nextFloat() * 0.2F, 18, 0.86F);
        }
    }

    public static final class SparkProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public SparkProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double dx, double dy, double dz) {
            return new HamonParticle(level, x, y, z, dx, dy, dz, sprites,
                    0.32F + level.random.nextFloat() * 0.12F, 8, 0.94F);
        }
    }

    public static final class FineSparkProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public FineSparkProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double dx, double dy, double dz) {
            return new HamonParticle(level, x, y, z, dx, dy, dz, sprites,
                    0.14F + level.random.nextFloat() * 0.06F, 8, 0.94F);
        }
    }

    public static final class FineMoteProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public FineMoteProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double dx, double dy, double dz) {
            return new HamonParticle(level, x, y, z, dx, dy * 0.5 + 0.01, dz, sprites,
                    0.10F + level.random.nextFloat() * 0.05F, 21, 0.9F);
        }
    }

    public static final class MoteProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public MoteProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double dx, double dy, double dz) {
            return new HamonParticle(level, x, y, z, dx, dy * 0.5 + 0.01, dz, sprites,
                    0.2F + level.random.nextFloat() * 0.1F, 21, 0.9F);
        }
    }
}
