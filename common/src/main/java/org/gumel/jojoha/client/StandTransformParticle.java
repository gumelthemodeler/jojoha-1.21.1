package org.gumel.jojoha.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

/**
 * The energy winding off a player as their Stand awakens - a mote climbing a tight upward spiral.
 *
 * <p>Each one keeps its own angle and radius and advances them itself rather than coasting on a
 * velocity, because a spiral can't be expressed as a starting velocity: the direction of travel
 * has to keep turning. The radius also draws inward as it climbs, so the column tapers to a point
 * above the player instead of rising as a straight cylinder.
 *
 * <p>Sprite frames are stepped across the particle's lifetime rather than by age in ticks, so the
 * full twelve-frame animation always plays through exactly once however long the mote lives.
 */
public final class StandTransformParticle extends TextureSheetParticle {
    private static final double SPIN_PER_TICK = 0.30;
    private static final double CLIMB_PER_TICK = 0.085;
    /** Under 1, so the helix narrows as it rises. */
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

        // The spiral is described around a fixed ground point, so the whole column stays coherent
        // rather than each mote wandering off on its own path.
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

        // Walked across the sheet by life fraction so all twelve frames are always seen.
        int frame = Mth.clamp((int) ((this.age / (float) this.lifetime) * 12), 0, 11);
        this.setSprite(sprites.get(frame, 11));
    }

    /** Fades out over the back half so the column dissolves at its tip. */
    @Override
    public float getQuadSize(float partialTick) {
        float progress = Mth.clamp((this.age + partialTick) / this.lifetime, 0F, 1F);
        return this.quadSize * (1F - progress * progress);
    }

    /** Additive, matching the rest of the Stand's effects - these are light, not decals. */
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

        /**
         * The spiral's starting angle and radius ride in on the velocity fields, which this
         * particle has no other use for - it steers itself rather than coasting.
         */
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                        double angle, double unusedY, double radius) {
            return new StandTransformParticle(level, x, y, z, angle, radius, sprites);
        }
    }
}
