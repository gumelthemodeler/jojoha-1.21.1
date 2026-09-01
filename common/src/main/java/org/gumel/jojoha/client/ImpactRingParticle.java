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
 * The ring of force that snaps outward where a blow connects.
 *
 * <p>The sheet holds three sets of five frames - small, medium and large circles. Each ring picks
 * one set at spawn and animates only within it, rather than sweeping the whole sheet, so a burst of
 * these is a genuine mixture of circle sizes rather than every ring marching through the same
 * growth. Animating within a set keeps the artwork's own line weight instead of scaling one sprite
 * up into a blurry one.
 *
 * <p>Drawn additively, because an impact ring is a flash rather than an object: over a dark cave
 * it should light the wall behind it, and against daylight it should wash out.
 */
public final class ImpactRingParticle extends TextureSheetParticle {
    /** Frames per size band, and how many bands the sheet holds - see impact_ring.json. */
    private static final int FRAMES_PER_BAND = 5;
    private static final int BAND_COUNT = 3;
    private static final int FRAME_COUNT = FRAMES_PER_BAND * BAND_COUNT;

    private final SpriteSet sprites;
    /** Index of the first frame in this ring's chosen size band. */
    private final int bandStart;

    private ImpactRingParticle(ClientLevel level, double x, double y, double z,
                               double scale, int band, SpriteSet sprites) {
        super(level, x, y, z, 0.0, 0.0, 0.0);
        this.sprites = sprites;
        this.gravity = 0F;
        this.hasPhysics = false;
        // Holds station: a ring marks where the hit happened, so drifting would smear the record
        // of it across the fight.
        this.friction = 1F;
        this.xd = 0;
        this.yd = 0;
        this.zd = 0;

        // A band beyond the sheet means "surprise me", which is what a barrage asks for - it wants
        // a scatter of sizes, not one repeated.
        int chosen = band >= 0 && band < BAND_COUNT ? band : this.random.nextInt(BAND_COUNT);
        this.bandStart = chosen * FRAMES_PER_BAND;

        // The artwork already differs in size between bands; this scales on top of it so the larger
        // circles also read as physically bigger rather than just thicker.
        float bandScale = 0.75F + chosen * 0.35F;
        this.quadSize = (float) (0.35 * (scale <= 0 ? 1.0 : scale)) * bandScale;
        // Short - a barrage lands one of these every few ticks, and anything longer would stack
        // into a solid wall of rings rather than reading as separate blows.
        this.lifetime = 6 + this.random.nextInt(3);
        this.roll = this.random.nextFloat() * Mth.TWO_PI;
        this.oRoll = this.roll;
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.removed) {
            int within = Mth.clamp((int) ((this.age / (float) this.lifetime) * FRAMES_PER_BAND),
                    0, FRAMES_PER_BAND - 1);
            this.setSprite(sprites.get(bandStart + within, FRAME_COUNT - 1));

            // Written straight to the field: Particle exposes alpha as protected state with no
            // getter to override, so a fade has to be pushed rather than pulled.
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

        /**
         * The velocity slots carry data, not motion: the first is an overall size multiplier and
         * the second selects the size band, with anything out of range meaning "pick one at random".
         */
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                        double scale, double band, double unusedZ) {
            return new ImpactRingParticle(level, x, y, z, scale, (int) band, sprites);
        }
    }
}
