package org.gumel.jojoha.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public final class GuardBreakParticle extends TextureSheetParticle {
    private static final int CRACK = 0;
    private static final int SHATTER = 1;
    private static final int LAST_FRAME = 1;

    private static final double PANE_SPEED_EPSILON = 0.02;

    private static final float PANE_SHATTER_AT = 0.4F;

    private static final float PANE_SPREAD = 0.45F;

    private final SpriteSet sprites;
    private final boolean pane;
    private final float baseSize;

    private final float spin;

    private GuardBreakParticle(ClientLevel level, double x, double y, double z,
                               double xd, double yd, double zd, SpriteSet sprites) {
        super(level, x, y, z, 0.0, 0.0, 0.0);
        this.sprites = sprites;
        this.pane = xd * xd + yd * yd + zd * zd < PANE_SPEED_EPSILON * PANE_SPEED_EPSILON;

        if (pane) {
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
            this.lifetime = 10;
            this.setSprite(sprites.get(CRACK, LAST_FRAME));
        } else {
            this.xd = xd;
            this.yd = yd;
            this.zd = zd;
            this.gravity = 0.9F;
            this.friction = 0.96F;
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
            this.quadSize = baseSize * (1F + PANE_SPREAD * progress);
            this.alpha = progress < 0.35F ? 1F : 1F - (progress - 0.35F) / 0.65F;
            return;
        }

        this.oRoll = this.roll;
        this.roll += spin;
        this.alpha = progress < 0.6F ? 1F : 1F - (progress - 0.6F) / 0.4F;
    }

    @Override
    public int getLightColor(float partialTick) {
        return pane ? 240 : super.getLightColor(partialTick);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return pane ? EmissiveParticleRenderType.INSTANCE : ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double xd, double yd, double zd) {
            return new GuardBreakParticle(level, x, y, z, xd, yd, zd, sprites);
        }
    }
}
