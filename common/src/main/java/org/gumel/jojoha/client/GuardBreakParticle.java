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
 * The guard coming apart: one sheet of cracks where the block was, and the pieces of it falling.
 *
 * <p>Two shapes out of one particle type, told apart by whether they were given any velocity. A
 * pane is spawned standing still and the shards are spawned moving, and the server guarantees every
 * shard a speed well above {@link #PANE_SPEED_EPSILON} - so this is a decision, not a coincidence
 * that a slow enough shard could break. The alternative was a second registered type carrying no
 * information the velocity did not already carry.
 *
 * <dl>
 *   <dt>The pane</dt>
 *   <dd>One large sheet hanging in the air at the guard, holding station and spreading as it goes.
 *   This is the part that reads as a guard breaking rather than as something being hit: a flat
 *   plane of cracks appearing exactly where the block was, in the moment it stops working.</dd>
 *
 *   <dt>The shards</dt>
 *   <dd>What is left of it, thrown outward and falling under gravity, tumbling as they go. They
 *   collide with the world, so the pieces land at the user's feet instead of sinking through the
 *   floor - the guard leaves wreckage on the ground for a moment after it fails.</dd>
 * </dl>
 *
 * <p>The two textures are used as a progression rather than a flip-book: {@code break} is the
 * fracture running through, {@code broken} is what it leaves. The pane crosses from one to the
 * other partway through its life and the shards are only ever the second, since a shard is by
 * definition the after. <b>The two files are byte-identical at the time of writing</b>, so nothing
 * of that is visible yet - it is wired this way so that drawing them apart is all the change it
 * takes, with no code to revisit.
 */
public final class GuardBreakParticle extends TextureSheetParticle {
    /** Sheet order - see guard_break.json. */
    private static final int CRACK = 0;
    private static final int SHATTER = 1;
    private static final int LAST_FRAME = 1;

    /**
     * Below this much speed a spawn is the pane rather than a shard.
     *
     * <p>Well under the slowest shard the server will throw, and well over the exact zero a pane is
     * given, so there is nothing in between for the test to get wrong.
     */
    private static final double PANE_SPEED_EPSILON = 0.02;

    /** How far through its life the pane stops cracking and is simply broken. */
    private static final float PANE_SHATTER_AT = 0.4F;

    /** How much wider the pane gets across its life - the fracture running outward. */
    private static final float PANE_SPREAD = 0.45F;

    private final SpriteSet sprites;
    private final boolean pane;
    private final float baseSize;

    /** How fast this shard tumbles, in radians per tick. Panes do not turn. */
    private final float spin;

    private GuardBreakParticle(ClientLevel level, double x, double y, double z,
                               double xd, double yd, double zd, SpriteSet sprites) {
        super(level, x, y, z, 0.0, 0.0, 0.0);
        this.sprites = sprites;
        this.pane = xd * xd + yd * yd + zd * zd < PANE_SPEED_EPSILON * PANE_SPEED_EPSILON;

        if (pane) {
            // Holds exactly where the guard was. A pane that drifted would stop reading as the
            // surface that just failed and start reading as smoke.
            this.xd = 0;
            this.yd = 0;
            this.zd = 0;
            this.gravity = 0F;
            this.friction = 1F;
            this.hasPhysics = false;
            this.spin = 0F;
            this.roll = 0F;
            this.oRoll = 0F;
            this.baseSize = 1.15F;
            // Long enough to register as its own beat and short enough not to outlive the sound.
            this.lifetime = 10;
            this.setSprite(sprites.get(CRACK, LAST_FRAME));
        } else {
            this.xd = xd;
            this.yd = yd;
            this.zd = zd;
            this.gravity = 0.9F;
            this.friction = 0.96F;
            // Collides, so the wreckage lands rather than falling through the floor.
            this.hasPhysics = true;
            this.spin = (this.random.nextFloat() - 0.5F) * 0.5F;
            this.roll = this.random.nextFloat() * Mth.TWO_PI;
            this.oRoll = this.roll;
            this.baseSize = 0.16F + this.random.nextFloat() * 0.12F;
            this.lifetime = 14 + this.random.nextInt(9);
            this.setSprite(sprites.get(SHATTER, LAST_FRAME));
        }

        this.quadSize = baseSize;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.removed) {
            return;
        }

        float progress = Mth.clamp(this.age / (float) this.lifetime, 0F, 1F);

        if (pane) {
            if (progress >= PANE_SHATTER_AT) {
                this.setSprite(sprites.get(SHATTER, LAST_FRAME));
            }
            // Spreads as it goes, so the break travels outward instead of appearing whole.
            this.quadSize = baseSize * (1F + PANE_SPREAD * progress);
            // Full for the first beat, then straight out - a break is a flash, not a fog.
            this.alpha = progress < 0.35F ? 1F : 1F - (progress - 0.35F) / 0.65F;
            return;
        }

        this.oRoll = this.roll;
        this.roll += spin;
        // Only the tail of a shard's life fades: they are debris, and debris that starts
        // disappearing the moment it leaves never looks like it had any weight.
        this.alpha = progress < 0.6F ? 1F : 1F - (progress - 0.6F) / 0.4F;
    }

    /**
     * Fullbright for the pane only.
     *
     * <p>The pane is the flash and should carry in a dark room; the shards are wreckage, and
     * wreckage that glows in a cave reads as embers rather than as pieces of a broken guard.
     */
    @Override
    public int getLightColor(float partialTick) {
        return pane ? 240 : super.getLightColor(partialTick);
    }

    /**
     * Additive for the pane, ordinary blending for the shards - for the same reason as the light.
     *
     * <p>The two end up in different draw passes, which is exactly right: the engine batches by
     * render type, and these are two different kinds of thing that happen to share a texture.
     */
    @Override
    public ParticleRenderType getRenderType() {
        return pane ? EmissiveParticleRenderType.INSTANCE : ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        /** Velocity is velocity here, and it also decides which of the two shapes this is. */
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double xd, double yd, double zd) {
            return new GuardBreakParticle(level, x, y, z, xd, yd, zd, sprites);
        }
    }
}
