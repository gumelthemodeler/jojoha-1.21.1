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
 * One link in the stream of air Inhale moves.
 *
 * <p>These are spawned as a row along the direction of the move rather than as a puff, so the
 * effect reads as a current with a direction to it. Each one holds the heading it was given for its
 * whole life and simply travels, which is what keeps the row parallel - anything that let them
 * wander individually would turn the line back into a cloud within a few ticks.
 *
 * <p>Not emissive, unlike the Stand's own effects: this is moving air, not something the Stand is
 * radiating, and lighting it from within would make it read as energy.
 */
public final class InhaleWindParticle extends TextureSheetParticle {
    private final SpriteSet sprites;

    /** Faded at both ends so links appear and leave along the row instead of popping. */
    private static final float FADE_FRACTION = 0.25F;

    /**
     * How solid a link ever gets.
     *
     * <p>They used to reach full opacity, which is most of why the stream was hard to see past -
     * a row of solid sprites crossing the middle of the view hides whatever the move is dragging
     * toward you. Held well below that, the air reads as air: present, moving, and something you
     * can watch a fight through.
     */
    private static final float MAX_ALPHA = 0.34F;

    private InhaleWindParticle(ClientLevel level, double x, double y, double z,
                               double dx, double dy, double dz, SpriteSet sprites) {
        super(level, x, y, z, 0.0, 0.0, 0.0);
        this.sprites = sprites;
        this.gravity = 0F;
        this.hasPhysics = false;
        // No friction: a gust that slows down as it crosses the gap stops looking like a current.
        this.friction = 1F;

        this.xd = dx;
        this.yd = dy;
        this.zd = dz;

        // Roughly doubled. Fewer, larger shapes cover the same air with far less edge in it, and
        // edges are what the eye catches - this is the same trade as the halved spawn counts.
        this.quadSize = 1.0F + this.random.nextFloat() * 0.5F;
        this.lifetime = 10 + this.random.nextInt(6);
        // A slow spin, different per link, so a row of identical sprites doesn't read as a decal.
        this.roll = this.random.nextFloat() * Mth.TWO_PI;
        this.oRoll = this.roll;
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        this.oRoll = this.roll;
        this.roll += 0.06F;

        super.tick();

        if (!this.removed) {
            // Stepped by life fraction rather than by tick, so all nine frames always play through
            // whatever lifetime this link happened to roll.
            int frame = Mth.clamp((int) ((this.age / (float) this.lifetime) * 9), 0, 8);
            this.setSprite(sprites.get(frame, 8));
        }
    }

    @Override
    public float getQuadSize(float partialTick) {
        // Grows slightly as it travels, the way a gust spreads.
        float progress = Mth.clamp((this.age + partialTick) / this.lifetime, 0F, 1F);
        return this.quadSize * (1F + progress * 0.35F);
    }

    /**
     * Lit by the world, not from within.
     *
     * <p>It was fullbright, which is what made the stream glare in a dark cave - and moving air has
     * no business being the brightest thing in the room. Taking the scene's light also settles it
     * into the shot instead of sitting on top of it.
     */
    @Override
    public int getLightColor(float partialTick) {
        return super.getLightColor(partialTick);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void render(com.mojang.blaze3d.vertex.VertexConsumer buffer,
                       net.minecraft.client.Camera camera, float partialTick) {
        float progress = Mth.clamp((this.age + partialTick) / this.lifetime, 0F, 1F);
        float fade = progress < FADE_FRACTION
                ? progress / FADE_FRACTION
                : Math.min(1F, (1F - progress) / FADE_FRACTION);
        this.alpha = Mth.clamp(fade, 0F, 1F) * MAX_ALPHA;
        super.render(buffer, camera, partialTick);
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                        double dx, double dy, double dz) {
            return new InhaleWindParticle(level, x, y, z, dx, dy, dz, sprites);
        }
    }
}
