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
 * The mark that says somebody is not going to be doing anything for a moment.
 *
 * <p>Spawned in a ring over the head by {@code ModEffects.StunEffect}, one point of the ring at a
 * time, so the ring itself is the server walking round the circle rather than anything this class
 * knows about. That keeps the particle dumb: it appears where it is put, hangs there, and goes.
 *
 * <h2>Why it barely moves</h2>
 *
 * <p>Stars over a stunned head is a cartoon shorthand that only works if the shape stays legible,
 * and a shape made of particles that drift is legible for about four ticks. This one holds its
 * position and only bobs, so at any instant there is a readable ring above the mob and not a cloud
 * of specks near one.
 */
public class StunParticle extends TextureSheetParticle {
    /** How far it rides up and down over its life, in blocks. */
    private static final double BOB = 0.045;

    /** How long the fade in and the fade out each take, as a share of the lifetime. */
    private static final float FADE_IN = 0.15F;
    private static final float FADE_OUT = 0.45F;

    private final SpriteSet sprites;

    protected StunParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z, 0.0, 0.0, 0.0);
        this.sprites = sprites;

        this.gravity = 0F;
        this.hasPhysics = false;
        this.friction = 1F;

        this.xd = 0.0;
        this.yd = 0.0;
        this.zd = 0.0;

        this.quadSize = 0.13F + this.random.nextFloat() * 0.05F;

        // Short, because the ring is being redrawn constantly. A long-lived particle here would
        // stack several rings on top of each other and turn the shape back into a cloud.
        this.lifetime = 14 + this.random.nextInt(6);

        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.removed) {
            return;
        }

        this.setSpriteFromAge(sprites);

        float progress = Mth.clamp(this.age / (float) this.lifetime, 0F, 1F);

        // A single half-cycle rather than a repeating wobble - it rises and settles once, which
        // reads as the thing being light rather than as it being animated.
        this.y = this.yo + Mth.sin(progress * Mth.PI) * BOB;

        if (progress < FADE_IN) {
            this.alpha = progress / FADE_IN;
        } else if (progress > 1F - FADE_OUT) {
            this.alpha = (1F - progress) / FADE_OUT;
        } else {
            this.alpha = 1F;
        }
    }

    /**
     * Lit by the world, not by itself.
     *
     * <p>Inherited rather than overridden, which is the whole change - it used to return full
     * brightness and draw on the emissive sheet, so a stunned mob in a dark room had a ring of
     * lamps over its head. A status indicator is a thing you read, not a light source, and it should
     * sit in the same shade as the mob it belongs to.
     */
    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            return new StunParticle(level, x, y, z, sprites);
        }
    }
}
