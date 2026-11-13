package org.gumel.jojoha.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public class StunParticle extends TextureSheetParticle {
    private static final double BOB = 0.045;

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

        this.y = this.yo + Mth.sin(progress * Mth.PI) * BOB;

        if (progress < FADE_IN) {
            this.alpha = progress / FADE_IN;
        } else if (progress > 1F - FADE_OUT) {
            this.alpha = (1F - progress) / FADE_OUT;
        } else {
            this.alpha = 1F;
        }
    }

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
