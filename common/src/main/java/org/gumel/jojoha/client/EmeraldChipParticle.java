package org.gumel.jojoha.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import org.gumel.jojoha.stand.hierophant.EmeraldChip;

public final class EmeraldChipParticle extends TextureSheetParticle {
    private static final float ALPHA = 0.7F;

    private final float spin;

    private EmeraldChipParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz,
                                SpriteSet sprites, int colour) {
        super(level, x, y, z, dx, dy, dz);
        this.xd = dx;
        this.yd = dy;
        this.zd = dz;
        this.gravity = 1.0F;
        this.friction = 0.98F;
        this.lifetime = 8 + this.random.nextInt(10);
        this.quadSize = 0.08F + this.random.nextFloat() * 0.05F;
        this.rCol = ((colour >> 16) & 0xFF) / 255F;
        this.gCol = ((colour >> 8) & 0xFF) / 255F;
        this.bCol = (colour & 0xFF) / 255F;
        this.alpha = ALPHA;
        this.roll = this.oRoll = this.random.nextFloat() * (float) Math.PI * 2F;
        this.spin = (this.random.nextFloat() - 0.5F) * 0.6F;
        this.pickSprite(sprites);
    }

    @Override
    public void tick() {
        this.oRoll = this.roll;
        super.tick();
        if (!this.onGround) {
            this.roll += this.spin;
        }
    }

    @Override
    public int getLightColor(float partialTick) {
        return 240;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static final class Provider implements ParticleProvider<EmeraldChip> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(EmeraldChip options, ClientLevel level, double x, double y, double z,
                                       double dx, double dy, double dz) {
            return new EmeraldChipParticle(level, x, y, z, dx, dy, dz, sprites, options.colour());
        }
    }
}
