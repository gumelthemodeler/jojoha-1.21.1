package org.gumel.jojoha.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public final class AnubisSliceParticle extends TextureSheetParticle {
    private static final int COLOUR = 0xB14CFF;
    private static final int LAST_FRAME = 3;

    private static final float SWELL = 0.25F;

    private final SpriteSet sprites;
    private final float baseSize;

    private AnubisSliceParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z, 0.0, 0.0, 0.0);
        this.sprites = sprites;
        this.xd = 0;
        this.yd = 0;
        this.zd = 0;
        this.gravity = 0F;
        this.hasPhysics = false;
        this.lifetime = 8;
        this.baseSize = 0.75F + this.random.nextFloat() * 0.3F;
        this.quadSize = baseSize;
        this.roll = this.random.nextFloat() * Mth.TWO_PI;
        this.oRoll = this.roll;
        this.rCol = ((COLOUR >> 16) & 0xFF) / 255F;
        this.gCol = ((COLOUR >> 8) & 0xFF) / 255F;
        this.bCol = (COLOUR & 0xFF) / 255F;
        pickFrame();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.removed) {
            return;
        }
        float life = Mth.clamp(this.age / (float) this.lifetime, 0F, 1F);
        this.quadSize = baseSize * (1F + SWELL * life);
        this.alpha = life < 0.6F ? 1F : 1F - (life - 0.6F) / 0.4F;
        pickFrame();
    }

    private void pickFrame() {
        this.setSprite(this.sprites.get(Math.min(this.age, LAST_FRAME), LAST_FRAME));
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

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double dx, double dy, double dz) {
            return new AnubisSliceParticle(level, x, y, z, sprites);
        }
    }
}
