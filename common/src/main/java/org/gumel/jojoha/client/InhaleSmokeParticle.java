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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class InhaleSmokeParticle extends TextureSheetParticle {
    private static final double OWNER_SEARCH_RADIUS = 12.0;

    private static final double PULL_ACCELERATION = 0.055;
    private static final double BLOW_ACCELERATION = 0.035;
    private static final double MAX_SPEED = 0.85;
    private static final double SWALLOW_DISTANCE = 0.7;

    private static final float MAX_ALPHA = 0.42F;
    private static final float FADE_IN = 0.2F;
    private static final float FADE_OUT = 0.45F;

    private final SpriteSet sprites;
    private final Player owner;
    private final boolean inhaling;

    private InhaleSmokeParticle(ClientLevel level, double x, double y, double z,
                                boolean inhaling, SpriteSet sprites) {
        super(level, x, y, z, 0.0, 0.0, 0.0);
        this.sprites = sprites;
        this.inhaling = inhaling;
        this.gravity = 0F;
        this.hasPhysics = false;
        this.friction = 0.96F;

        this.owner = level.getNearestPlayer(x, y, z, OWNER_SEARCH_RADIUS, false);

        this.quadSize = 0.7F + this.random.nextFloat() * 0.4F;
        this.lifetime = 16 + this.random.nextInt(10);
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        if (owner == null) {
            super.tick();
            return;
        }

        Vec3 chest = new Vec3(owner.getX(), owner.getY() + owner.getBbHeight() * 0.6, owner.getZ());
        Vec3 toOwner = chest.subtract(this.x, this.y, this.z);
        double distance = toOwner.length();

        if (inhaling && distance < SWALLOW_DISTANCE) {
            this.remove();
            return;
        }

        if (distance > 1.0E-4) {
            Vec3 pull = toOwner.scale(1.0 / distance)
                    .scale(inhaling ? PULL_ACCELERATION : -BLOW_ACCELERATION);
            this.xd = Mth.clamp(this.xd + pull.x, -MAX_SPEED, MAX_SPEED);
            this.yd = Mth.clamp(this.yd + pull.y, -MAX_SPEED, MAX_SPEED);
            this.zd = Mth.clamp(this.zd + pull.z, -MAX_SPEED, MAX_SPEED);
        }

        super.tick();

        if (!this.removed) {
            int frame = Mth.clamp((int) ((this.age / (float) this.lifetime) * 5), 0, 4);
            this.setSprite(sprites.get(frame, 4));
        }
    }

    @Override
    public float getQuadSize(float partialTick) {
        float progress = Mth.clamp((this.age + partialTick) / this.lifetime, 0F, 1F);
        return inhaling ? this.quadSize * (1F - progress * 0.7F) : this.quadSize * (1F + progress);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void render(VertexConsumer buffer,
                       Camera camera, float partialTick) {
        float progress = Mth.clamp((this.age + partialTick) / this.lifetime, 0F, 1F);
        float fade = Math.min(progress / FADE_IN, (1F - progress) / FADE_OUT);
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
                                        double dx, double inhaleFlag, double dz) {
            return new InhaleSmokeParticle(level, x, y, z, inhaleFlag >= 0, sprites);
        }
    }
}
