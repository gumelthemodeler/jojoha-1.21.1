package org.gumel.jojoha.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;
import org.gumel.jojoha.stand.hierophant.EmeraldSparkle;

public final class EmeraldSparkleParticle extends TextureSheetParticle {
    private final SpriteSet sprites;

    private EmeraldSparkleParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz,
                                   SpriteSet sprites, int colour) {
        super(level, x, y, z, dx, dy, dz);
        this.sprites = sprites;
        this.xd = dx;
        this.yd = dy;
        this.zd = dz;
        this.friction = 0.86F;
        this.gravity = 0.02F;
        this.hasPhysics = false;
        this.lifetime = 10 + this.random.nextInt(10);
        this.quadSize = 0.18F + this.random.nextFloat() * 0.14F;
        this.rCol = ((colour >> 16) & 0xFF) / 255F;
        this.gCol = ((colour >> 8) & 0xFF) / 255F;
        this.bCol = (colour & 0xFF) / 255F;
        this.roll = this.oRoll = 0F;
        pickFrame();
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.removed) {
            pickFrame();
        }
    }

    private void pickFrame() {
        float life = Mth.clamp(this.age / (float) this.lifetime, 0F, 1F);
        float peak = 1F - Math.abs(life * 2F - 1F);
        this.setSprite(this.sprites.get(Math.round(peak * 2F), 2));
    }

    @Override
    public int getLightColor(float partialTick) {
        return 240;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return EmissiveParticleRenderType.INSTANCE;
    }

    public static final class Provider implements ParticleProvider<EmeraldSparkle> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(EmeraldSparkle options, ClientLevel level, double x, double y, double z,
                                       double dx, double dy, double dz) {
            return new EmeraldSparkleParticle(level, x, y, z, dx, dy, dz, sprites, options.colour());
        }
    }
}
