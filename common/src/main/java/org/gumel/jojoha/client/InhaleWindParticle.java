package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public final class InhaleWindParticle extends TextureSheetParticle {
    private final SpriteSet sprites;

    private static final float FADE_FRACTION = 0.25F;

    private static final float MAX_ALPHA = 0.34F;

    private InhaleWindParticle(ClientLevel level, double x, double y, double z,
                               double dx, double dy, double dz, SpriteSet sprites) {
        super(level, x, y, z, 0.0, 0.0, 0.0);
        this.sprites = sprites;
        this.gravity = 0F;
        this.hasPhysics = false;
        this.friction = 1F;

        this.xd = dx;
        this.yd = dy;
        this.zd = dz;

        this.quadSize = 1.0F + this.random.nextFloat() * 0.5F;
        this.lifetime = 10 + this.random.nextInt(6);
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
            int frame = Mth.clamp((int) ((this.age / (float) this.lifetime) * 9), 0, 8);
            this.setSprite(sprites.get(frame, 8));
        }
    }

    @Override
    public float getQuadSize(float partialTick) {
        float progress = Mth.clamp((this.age + partialTick) / this.lifetime, 0F, 1F);
        return this.quadSize * (1F + progress * 0.35F);
    }

    @Override
    public int getLightColor(float partialTick) {
        return super.getLightColor(partialTick);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void render(VertexConsumer buffer,
                       Camera camera, float partialTick) {
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
