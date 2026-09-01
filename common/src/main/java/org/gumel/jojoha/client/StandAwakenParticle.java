package org.gumel.jojoha.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * The burst thrown off when a Stand awakens - fired outward in a sphere and left to coast.
 *
 * <p>Unlike the drifting aura particle this shares a sprite sheet with, these keep whatever
 * velocity the server hands them and bleed it off through friction, so the shape of the explosion
 * comes from the spawn pattern rather than from anything the particle decides for itself.
 */
public final class StandAwakenParticle extends TextureSheetParticle {
    private final SpriteSet sprites;

    private StandAwakenParticle(ClientLevel level, double x, double y, double z,
                                 double dx, double dy, double dz, SpriteSet sprites) {
        super(level, x, y, z, 0.0, 0.0, 0.0);
        this.sprites = sprites;
        this.gravity = 0F;
        this.hasPhysics = false;
        // Coasts outward and slows, rather than vanilla's default of shedding almost all speed
        // immediately - that decay is what turns a burst into a puff.
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

    /** Additive, so the burst reads as a flash of light rather than a cloud of decals. */
    @Override
    public ParticleRenderType getRenderType() {
        return EmissiveParticleRenderType.INSTANCE;
    }

    /**
     * Full brightness regardless of world lighting - the same fixed value (240) vanilla's own
     * SoulParticle.EmissiveProvider uses, rather than the default which samples block/sky light
     * at the particle's position and washes it out under normal lighting.
     */
    @Override
    public int getLightColor(float partialTick) {
        return 240;
    }

    /**
     * The same burst in the colour of blood, for the Stone Mask.
     *
     * <p>A tint on the shared sprite rather than a second sheet: the shapes are identical and only
     * the colour of the light differs, so painting a red copy of the art would be two files to keep
     * in step for no visual gain. Green and blue are pulled down rather than red pushed up, because
     * the sprite is already near white and there is no headroom above it.
     */
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

    /**
     * Blue and pink, the pair a Stand comes apart in when its skin is being changed.
     *
     * <p>Two tints rather than one because the burst wants to read as unstable - a single colour
     * looks like an effect, two interleaved colours look like something coming apart at a seam.
     * Same sprite and same class as the red, for the reason given there.
     */
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
