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
 * The motes that rise off someone the mask is turning.
 *
 * <p>Slow, sparse and upward, which is the opposite of the burst that starts the transformation.
 * The burst is the instant; these are the minute after it, and they exist so a long transformation
 * has something happening throughout rather than one bang followed by waiting.
 *
 * <p>Drawn additively and fullbright. Blood-red at full saturation over a dark body would simply
 * read as black specks; adding light instead means they glow against whatever is behind them, which
 * is what makes them read as something coming out of the player rather than dirt in the air.
 */
public class BloodMoteParticle extends TextureSheetParticle {
    // Drawn as authored. There used to be a setColor of (1.0, 0.1, 0.1) here, dyeing the sprite
    // red - which was necessary only because the particle was pointed at ambient1 through ambient5,
    // a set of plain white motes. The blood frames were sitting unused in the same folder the whole
    // time. They are already the colour they are meant to be, so the tint is gone with them.

    /** How fast they drift up, and how much of that is random per mote. */
    private static final double RISE_SPEED = 0.018;
    private static final double RISE_SPREAD = 0.022;

    /**
     * The line between a mote that was released and one that was thrown.
     *
     * <p>This particle has two jobs and they want opposite physics. Around a stone mask it is a
     * haze - slow, weightless, drifting upward, hanging where it was put. Coming out of a head that
     * has just burst it is a spray, and a spray has to leave fast and then fall.
     *
     * <p>The speed it is handed is the only thing that tells the two apart, so that is what decides
     * it. Anything under a tenth of a block a tick was almost certainly meant to hang; anything over
     * was aimed somewhere.
     */
    private static final double THROWN = 0.1;
    private static final double THROWN_SQR = THROWN * THROWN;

    /** What a thrown drop is subject to, once it is out. */
    private static final float SPURT_GRAVITY = 0.72F;
    private static final float SPURT_FRICTION = 0.93F;

    /** Blood, kept off pure red so the falloff stays readable when several overlap. */
    private final SpriteSet sprites;

    protected BloodMoteParticle(ClientLevel level, double x, double y, double z,
                                double dx, double dy, double dz, SpriteSet sprites) {
        super(level, x, y, z, 0.0, 0.0, 0.0);
        this.sprites = sprites;

        boolean thrown = dx * dx + dy * dy + dz * dz > THROWN_SQR;
        if (thrown) {
            // Kept, rather than thrown away. The velocity handed to this particle used to be
            // discarded outright - the provider ignored its three speed arguments and the
            // constructor wrote a gentle rise over the top of them - so no caller could make blood
            // do anything but float, however hard it threw it. That is the whole of why a burst out
            // of somebody's head hung in the air instead of leaving it.
            this.xd = dx;
            this.yd = dy;
            this.zd = dz;

            // And it falls, which is the other half. A spray with no weight on it is a cloud that
            // happens to be moving; the arc downward is what makes it read as liquid.
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

        // Large for a mote, and it has to be.
        //
        // The sheet these come off is pixel art in the strict sense: each frame paints between one
        // and four pixels of a sixteen by sixteen canvas and leaves the rest clear, so the droplet
        // is somewhere around a tenth of the quad it is drawn on. At the tenth-of-a-block size this
        // used to run at, the visible part came out near a hundredth of a block - present in the
        // buffer and invisible on screen.
        //
        // Sized so the painted dot lands at a few centimetres instead, which is a droplet. The
        // quad being much bigger than the mark costs nothing: the rest of it is transparent.
        this.quadSize = 0.5F + this.random.nextFloat() * 0.3F;
        // Thrown drops are short-lived: they are meant to leave, land and be done, not to still be
        // in the air two seconds after the head they came out of.
        this.lifetime = thrown ? 14 + this.random.nextInt(10) : 30 + this.random.nextInt(24);
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.removed) {
            this.setSpriteFromAge(sprites);

            // Faded out over the back half rather than the whole life, so they hold their colour
            // while they are worth looking at and only thin as they leave.
            float progress = Mth.clamp(this.age / (float) this.lifetime, 0F, 1F);
            this.alpha = progress < 0.5F ? 1F : 1F - (progress - 0.5F) * 2F;
        }
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
            return new BloodMoteParticle(level, x, y, z, dx, dy, dz, sprites);
        }
    }
}
