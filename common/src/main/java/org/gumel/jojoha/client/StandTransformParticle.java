package org.gumel.jojoha.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public final class StandTransformParticle extends TextureSheetParticle {
    private static final double SPIN_PER_TICK = 0.30;
    private static final double CLIMB_PER_TICK = 0.085;
    private static final double RADIUS_TAPER = 0.965;

    private final SpriteSet sprites;
    private final double originX;
    private final double originZ;

    private double angle;
    private double radius;
    private double height;

    private StandTransformParticle(ClientLevel level, double x, double y, double z,
                                    double angle, double radius, SpriteSet sprites) {
        super(level, x, y, z, 0.0, 0.0, 0.0);
        this.sprites = sprites;
        this.gravity = 0F;
        this.hasPhysics = false;

        this.originX = x;
        this.originZ = z;
        this.height = y;
        this.angle = angle;
        this.radius = radius;

        this.quadSize = 0.22F + this.random.nextFloat() * 0.12F;
        this.lifetime = 26 + this.random.nextInt(14);
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        angle += SPIN_PER_TICK;
        height += CLIMB_PER_TICK;
        radius *= RADIUS_TAPER;

        setPos(originX + Math.cos(angle) * radius, height, originZ + Math.sin(angle) * radius);

        int frame = Mth.clamp((int) ((this.age / (float) this.lifetime) * 12), 0, 11);
        this.setSprite(sprites.get(frame, 11));
    }

    @Override
    public float getQuadSize(float partialTick) {
        float progress = Mth.clamp((this.age + partialTick) / this.lifetime, 0F, 1F);
        return this.quadSize * (1F - progress * progress);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return EmissiveParticleRenderType.INSTANCE;
    }

    @Override
    public int getLightColor(float partialTick) {
        return 240;
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                        double angle, double unusedY, double radius) {
            return new StandTransformParticle(level, x, y, z, angle, radius, sprites);
        }
    }
}
