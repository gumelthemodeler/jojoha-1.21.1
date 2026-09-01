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
 * A thorn caught on something, for as long as it stays caught.
 *
 * <p>This replaced a set of real vine segments driven into the model. Solid geometry poking out of a
 * mob reads as clipping rather than as an effect - the eye knows what a model looks like when it is
 * broken, and seven stubs sticking through a zombie looked exactly like that. A mote does not have to
 * belong to the silhouette to be believed.
 *
 * <p>Short and small on purpose. This has to be readable across a fight without becoming the thing
 * being looked at, and the effect it marks only lasts a couple of seconds - a particle that outlives
 * a good fraction of that would still be hanging in the air after the mob was free.
 *
 * <h2>It stays where it is put</h2>
 *
 * <p>No velocity, no gravity, no drift. The aura mote this first borrowed climbs and tapers like
 * flame, which is right for something burning off a body and wrong for something stuck in one - and
 * it also binds itself to the nearest player, so on a zombie ten blocks away it found no owner and
 * deleted itself on its first tick. Nothing here follows anybody: the spawn point is the whole
 * statement, and a mob that walks out of its own thorns leaves them behind, which is what happens.
 */
public final class TangledThornParticle extends TextureSheetParticle {
    /** The colour of the effect it marks, kept in step with ModEffects.TANGLED_COLOUR. */
    private static final float RED = 0x8A / 255F;
    private static final float GREEN = 0x4F / 255F;
    private static final float BLUE = 0xBF / 255F;

    /** How long the fade in and the fade out each take, as a share of the lifetime. */
    private static final float FADE_IN = 0.2F;
    private static final float FADE_OUT = 0.5F;

    private final SpriteSet sprites;

    private TangledThornParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z, 0.0, 0.0, 0.0);
        this.sprites = sprites;

        this.gravity = 0F;
        this.hasPhysics = false;
        this.friction = 1F;
        this.xd = 0.0;
        this.yd = 0.0;
        this.zd = 0.0;

        // Small. A thorn is a detail on a body, not a shape next to one.
        this.quadSize = 0.11F + this.random.nextFloat() * 0.06F;

        // Brief, and varied so a burst does not blink out all at once.
        this.lifetime = 7 + this.random.nextInt(5);

        this.setColor(RED, GREEN, BLUE);
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
     * <p>Same reasoning as the stun mark's: a status indicator is something you read, and thorns
     * glowing in a dark room would make a caught mob easier to see than an uncaught one, which is a
     * gameplay change dressed as a visual one.
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
            return new TangledThornParticle(level, x, y, z, sprites);
        }
    }
}
