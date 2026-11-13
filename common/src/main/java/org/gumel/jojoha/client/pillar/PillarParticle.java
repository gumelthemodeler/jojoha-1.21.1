package org.gumel.jojoha.client.pillar;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public class PillarParticle extends TextureSheetParticle {
    public enum Kind {
        CHUNK,
        GUST,
        SAND,
        STEAM,
        BUBBLE,
        GLINT
    }

    private static final float STEAM_ALPHA = 0.55F;
    private static final float GUST_ALPHA = 0.7F;

    private final SpriteSet sprites;
    private final Kind kind;
    private final float baseSize;

    protected PillarParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz,
                             SpriteSet sprites, Kind kind) {
        super(level, x, y, z, 0.0, 0.0, 0.0);
        this.sprites = sprites;
        this.kind = kind;
        this.xd = dx;
        this.yd = dy;
        this.zd = dz;
        this.roll = 0F;
        this.oRoll = 0F;
        switch (kind) {
            case CHUNK -> {
                this.gravity = 0.9F;
                this.friction = 0.96F;
                this.hasPhysics = true;
                this.lifetime = 24 + random.nextInt(14);
                this.quadSize = 0.11F + random.nextFloat() * 0.07F;
                pickSprite(sprites);
            }
            case GUST -> {
                this.gravity = 0F;
                this.friction = 0.9F;
                this.hasPhysics = false;
                this.lifetime = 10 + random.nextInt(4);
                this.quadSize = 0.35F + random.nextFloat() * 0.3F;
                this.roll = random.nextInt(4) * Mth.HALF_PI;
                this.oRoll = this.roll;
                setSpriteFromAge(sprites);
            }
            case SAND -> {
                this.gravity = 0.04F;
                this.friction = 0.94F;
                this.hasPhysics = true;
                this.lifetime = 18 + random.nextInt(20);
                this.quadSize = 0.07F + random.nextFloat() * 0.04F;
                float shade = 0.85F + random.nextFloat() * 0.15F;
                setColor(0.85F * shade, 0.78F * shade, 0.62F * shade);
                pickSprite(sprites);
            }
            case STEAM -> {
                this.gravity = -0.02F;
                this.friction = 0.92F;
                this.hasPhysics = false;
                this.lifetime = 16 + random.nextInt(8);
                this.quadSize = 0.22F + random.nextFloat() * 0.12F;
                setSpriteFromAge(sprites);
            }
            case BUBBLE -> {
                this.gravity = -0.01F;
                this.friction = 0.8F;
                this.hasPhysics = false;
                this.lifetime = 8 + random.nextInt(5);
                this.quadSize = 0.1F + random.nextFloat() * 0.06F;
                setSpriteFromAge(sprites);
            }
            case GLINT -> {
                this.gravity = 0F;
                this.friction = 0.85F;
                this.hasPhysics = false;
                this.lifetime = 6 + random.nextInt(5);
                this.quadSize = 0.08F + random.nextFloat() * 0.08F;
                setColor(1F, 0.96F, 0.8F);
                setSpriteFromAge(sprites);
            }
        }
        this.baseSize = this.quadSize;
        this.alpha = startAlpha();
    }

    private boolean smoky() {
        return kind == Kind.STEAM || kind == Kind.GUST;
    }

    private float startAlpha() {
        return kind == Kind.STEAM ? STEAM_ALPHA : kind == Kind.GUST ? GUST_ALPHA : 1F;
    }

    @Override
    public void tick() {
        super.tick();
        if (removed) {
            return;
        }
        float life = age / (float) lifetime;
        if (smoky()) {
            alpha = startAlpha() * Mth.clamp((1F - life) * 2F, 0F, 1F);
        }
        switch (kind) {
            case CHUNK -> {
                if (onGround) {
                    xd *= 0.6;
                    zd *= 0.6;
                }
                if (life > 0.75F) {
                    quadSize = baseSize * Mth.clamp((1F - life) * 4F, 0F, 1F);
                }
            }
            case GUST, BUBBLE, GLINT -> setSpriteFromAge(sprites);
            case STEAM -> {
                setSpriteFromAge(sprites);
                quadSize = baseSize * (1F + life * 0.8F);
            }
            case SAND -> {
                if (life > 0.8F) {
                    quadSize = baseSize * Mth.clamp((1F - life) * 5F, 0F, 1F);
                }
            }
        }
    }

    @Override
    public int getLightColor(float partialTick) {
        return kind == Kind.BUBBLE || kind == Kind.GLINT ? 0xF000F0 : super.getLightColor(partialTick);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return smoky() ? ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT : ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        private final Kind kind;

        private Provider(SpriteSet sprites, Kind kind) {
            this.sprites = sprites;
            this.kind = kind;
        }

        public static Provider chunk(SpriteSet sprites) {
            return new Provider(sprites, Kind.CHUNK);
        }

        public static Provider gust(SpriteSet sprites) {
            return new Provider(sprites, Kind.GUST);
        }

        public static Provider sand(SpriteSet sprites) {
            return new Provider(sprites, Kind.SAND);
        }

        public static Provider steam(SpriteSet sprites) {
            return new Provider(sprites, Kind.STEAM);
        }

        public static Provider bubble(SpriteSet sprites) {
            return new Provider(sprites, Kind.BUBBLE);
        }

        public static Provider glint(SpriteSet sprites) {
            return new Provider(sprites, Kind.GLINT);
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double dx, double dy, double dz) {
            return new PillarParticle(level, x, y, z, dx, dy, dz, sprites, kind);
        }
    }
}
