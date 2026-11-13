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
import org.gumel.jojoha.vampire.VampireFx;

public final class ImpactRingParticle extends TextureSheetParticle {
    private static final int FRAMES_PER_BAND = 5;
    private static final int BAND_COUNT = 3;
    private static final int FRAME_COUNT = FRAMES_PER_BAND * BAND_COUNT;

    private final SpriteSet sprites;
    private final int bandStart;

    private ImpactRingParticle(ClientLevel level, double x, double y, double z,
                               double scale, int band, SpriteSet sprites, float[] tint) {
        super(level, x, y, z, 0.0, 0.0, 0.0);
        this.sprites = sprites;
        this.gravity = 0F;
        this.hasPhysics = false;
        this.friction = 1F;
        this.xd = 0;
        this.yd = 0;
        this.zd = 0;

        int chosen = band >= 0 && band < BAND_COUNT ? band : this.random.nextInt(BAND_COUNT);
        this.bandStart = chosen * FRAMES_PER_BAND;

        float bandScale = 0.95F + chosen * 0.33F;
        this.quadSize = (float) (0.44 * (scale <= 0 ? 1.0 : scale)) * bandScale;
        this.lifetime = 6 + this.random.nextInt(3);
        this.roll = this.random.nextFloat() * Mth.TWO_PI;
        this.oRoll = this.roll;
        if (tint != null) {
            this.setColor(tint[0], tint[1], tint[2]);
        }
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.removed) {
            int within = Mth.clamp((int) ((this.age / (float) this.lifetime) * FRAMES_PER_BAND),
                    0, FRAMES_PER_BAND - 1);
            this.setSprite(sprites.get(bandStart + within, FRAME_COUNT - 1));

            float progress = Mth.clamp(this.age / (float) this.lifetime, 0F, 1F);
            this.alpha = progress < 0.5F ? 1F : 1F - (progress - 0.5F) * 2F;
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

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                        double scale, double band, double unusedZ) {
            return new ImpactRingParticle(level, x, y, z, scale, (int) band, sprites, null);
        }
    }

    public static final class BloodProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public BloodProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double scale, double band, double unusedZ) {
            return new ImpactRingParticle(level, x, y, z, scale, (int) band, sprites,
                    new float[]{VampireFx.BLOOD_RED,
                                VampireFx.BLOOD_GREEN,
                                VampireFx.BLOOD_BLUE});
        }
    }

    public static final class RipperProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public RipperProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double scale, double band, double unusedZ) {
            return new ImpactRingParticle(level, x, y, z, scale, (int) band, sprites,
                    new float[]{VampireFx.RIPPER_RED,
                                VampireFx.RIPPER_GREEN,
                                VampireFx.RIPPER_BLUE});
        }
    }

    public static final class HamonProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public HamonProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                        double scale, double band, double unusedZ) {
            return new ImpactRingParticle(level, x, y, z, scale, (int) band, sprites,
                    new float[]{HamonFx.RED,
                                HamonFx.GREEN,
                                HamonFx.BLUE});
        }
    }
}
