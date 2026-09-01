package org.gumel.jojoha.client;

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

/**
 * Smoke caught in the breath - dragged toward the user and swallowed, or blasted away from them.
 *
 * <p>The pull is written as an acceleration toward the user rather than a fixed velocity, because
 * being sucked in is defined by getting faster the closer you get. A constant drift inward reads as
 * smoke that happens to be moving; acceleration reads as suction. Puffs also shrink as they close,
 * so they visibly disappear into the user rather than piling up inside them.
 *
 * <p>The user is found by proximity at spawn, the same way the aura motes do it: a
 * {@code SimpleParticleType} carries no payload, so there is no way to be told who to converge on.
 */
public final class InhaleSmokeParticle extends TextureSheetParticle {
    /** Far enough to catch the caster from anywhere a puff spawns, close enough not to grab a bystander. */
    private static final double OWNER_SEARCH_RADIUS = 12.0;

    private static final double PULL_ACCELERATION = 0.055;
    private static final double BLOW_ACCELERATION = 0.035;
    private static final double MAX_SPEED = 0.85;
    /** Inside this, a pulled puff has arrived and is consumed. */
    private static final double SWALLOW_DISTANCE = 0.7;

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

        // Enlarged alongside the wind, and for the same reason - see InhaleWindParticle.
        this.quadSize = 0.7F + this.random.nextFloat() * 0.4F;
        this.lifetime = 16 + this.random.nextInt(10);
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        if (owner == null) {
            // Nothing to converge on - let it behave as ordinary drifting smoke rather than vanish,
            // which would leave a visible hole in the effect.
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

    /** Pulled smoke tapers as it is swallowed; blown smoke expands as it disperses. */
    @Override
    public float getQuadSize(float partialTick) {
        float progress = Mth.clamp((this.age + partialTick) / this.lifetime, 0F, 1F);
        return inhaling ? this.quadSize * (1F - progress * 0.7F) : this.quadSize * (1F + progress);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        /**
         * The vertical velocity slot carries the direction of the breath rather than a speed - the
         * puff works out its own motion, and a SimpleParticleType has nowhere else to put a flag.
         */
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                        double dx, double inhaleFlag, double dz) {
            return new InhaleSmokeParticle(level, x, y, z, inhaleFlag >= 0, sprites);
        }
    }
}
