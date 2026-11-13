package org.gumel.jojoha.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import org.gumel.jojoha.vampire.VampireFx;

public class BloodMoteParticle extends TextureSheetParticle {
    private static final double RISE_SPEED = 0.018;
    private static final double RISE_SPREAD = 0.022;

    private static final double THROWN = 0.1;
    private static final double THROWN_SQR = THROWN * THROWN;

    private static final float SPURT_GRAVITY = 0.72F;
    private static final float SPURT_FRICTION = 0.93F;

    private final SpriteSet sprites;

    protected BloodMoteParticle(ClientLevel level, double x, double y, double z,
                                double dx, double dy, double dz, SpriteSet sprites) {
        super(level, x, y, z, 0.0, 0.0, 0.0);
        this.sprites = sprites;

        boolean thrown = dx * dx + dy * dy + dz * dz > THROWN_SQR;
        if (thrown) {
            this.xd = dx;
            this.yd = dy;
            this.zd = dz;

            this.gravity = SPURT_GRAVITY;
            this.friction = SPURT_FRICTION;
            this.hasPhysics = true;
        } else {
            this.gravity = 0F;
            this.hasPhysics = false;
            this.friction = 1F;

            this.xd = (this.random.nextDouble() - 0.5) * 0.01;
            this.yd = RISE_SPEED + this.random.nextDouble() * RISE_SPREAD;
            this.zd = (this.random.nextDouble() - 0.5) * 0.01;
        }

        this.quadSize = 0.5F + this.random.nextFloat() * 0.3F;
        this.lifetime = thrown ? 14 + this.random.nextInt(10) : 30 + this.random.nextInt(24);
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.removed) {
            this.setSpriteFromAge(sprites);

            float progress = Mth.clamp(this.age / (float) this.lifetime, 0F, 1F);
            this.alpha = progress < 0.5F ? 1F : 1F - (progress - 0.5F) * 2F;
        }
    }

    private boolean lit;

    @Override
    public int getLightColor(float partialTick) {
        return lit ? 240 : super.getLightColor(partialTick);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return lit ? EmissiveParticleRenderType.INSTANCE
                : ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static final class RipperProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public RipperProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double dx, double dy, double dz) {
            BloodMoteParticle mote = new BloodMoteParticle(level, x, y, z, dx, dy, dz, sprites);
            mote.setColor(VampireFx.RIPPER_RED,
                    VampireFx.RIPPER_GREEN,
                    VampireFx.RIPPER_BLUE);
            mote.quadSize *= MIST_SCALE;
            mote.lit = true;
            return mote;
        }
    }

    private static final float MIST_SCALE = 0.45F;

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double dx, double dy, double dz) {
            return new BloodMoteParticle(level, x, y, z, dx, dy, dz, sprites);
        }
    }
}
