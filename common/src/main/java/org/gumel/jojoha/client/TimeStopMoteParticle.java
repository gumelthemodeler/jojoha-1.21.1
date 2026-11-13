package org.gumel.jojoha.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.gumel.jojoha.stand.StandEntity;

public final class TimeStopMoteParticle extends TextureSheetParticle {
    private static final float HUE_STEP = 0.31F;

    private static final double BIND_RADIUS = 4.0;

    private static final double ORBIT_SPEED = 0.10;
    private static final double ORBIT_SPEED_CHARGED = 0.52;

    private static final double DISPEL_SPEED = 0.42;
    private static final int DISPEL_LIFETIME = 5;
    private static final double ORBIT_RADIUS = 0.95;
    private static final double RADIUS_SPREAD = 0.55;
    private static final double CLIMB_PER_TICK = 0.018;

    private static float nextHue;

    private final SpriteSet sprites;
    private final StandEntity around;

    private double angle;
    private double orbitRadius;
    private double height;
    private boolean thrown;

    private TimeStopMoteParticle(ClientLevel level, double x, double y, double z,
                                 double dx, double dy, double dz, SpriteSet sprites) {
        super(level, x, y, z, 0.0, 0.0, 0.0);
        this.sprites = sprites;
        this.gravity = 0F;
        this.hasPhysics = false;
        this.xd = 0.0;
        this.yd = 0.0;
        this.zd = 0.0;
        this.quadSize = 0.16F + this.random.nextFloat() * 0.14F;
        this.lifetime = 26 + this.random.nextInt(14);

        this.around = nearestStand(level, x, y, z);
        if (this.around != null) {
            this.angle = Math.atan2(z - around.getZ(), x - around.getX());
            this.height = y - around.getY();
        }
        this.orbitRadius = ORBIT_RADIUS + this.random.nextDouble() * RADIUS_SPREAD;

        nextHue += HUE_STEP;
        if (nextHue >= 1F) {
            nextHue -= 1F;
        }
        setHue(nextHue);
        this.setSpriteFromAge(sprites);
    }

    private void setHue(float turns) {
        float h = (turns - Mth.floor(turns)) * 6F;
        float f = h - Mth.floor(h);

        switch ((int) h) {
            case 0 -> this.setColor(1F, f, 0F);
            case 1 -> this.setColor(1F - f, 1F, 0F);
            case 2 -> this.setColor(0F, 1F, f);
            case 3 -> this.setColor(0F, 1F - f, 1F);
            case 4 -> this.setColor(f, 0F, 1F);
            default -> this.setColor(1F, 0F, 1F - f);
        }
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.around == null || !this.around.isAlive() || this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        if (TimeStopCharge.dispelling() && !this.thrown) {
            this.thrown = true;
            this.lifetime = this.age + DISPEL_LIFETIME;
            this.xd = Math.cos(this.angle) * DISPEL_SPEED;
            this.yd = CLIMB_PER_TICK * 4;
            this.zd = Math.sin(this.angle) * DISPEL_SPEED;
        }

        if (this.thrown) {
            this.move(this.xd, this.yd, this.zd);
            this.setSpriteFromAge(this.sprites);
            return;
        }

        this.angle += ORBIT_SPEED
                + (ORBIT_SPEED_CHARGED - ORBIT_SPEED) * TimeStopCharge.charge();
        this.height += CLIMB_PER_TICK;

        setPos(this.around.getX() + Math.cos(this.angle) * this.orbitRadius,
                this.around.getY() + this.height,
                this.around.getZ() + Math.sin(this.angle) * this.orbitRadius);

        this.setSpriteFromAge(this.sprites);
    }

    private static StandEntity nearestStand(ClientLevel level, double x, double y, double z) {
        StandEntity best = null;
        double bestDistance = BIND_RADIUS * BIND_RADIUS;

        for (StandEntity stand : level.getEntitiesOfClass(StandEntity.class,
                new AABB(x - BIND_RADIUS, y - BIND_RADIUS, z - BIND_RADIUS,
                        x + BIND_RADIUS, y + BIND_RADIUS, z + BIND_RADIUS))) {
            double distance = stand.distanceToSqr(x, y, z);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = stand;
            }
        }

        return best;
    }

    @Override
    public float getQuadSize(float partialTick) {
        float progress = Mth.clamp((this.age + partialTick) / this.lifetime, 0F, 1F);
        return this.quadSize * (1F - progress * progress) * 1.3F;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return EmissiveParticleRenderType.INSTANCE;
    }

    @Override
    public int getLightColor(float partialTick) {
        return 240;
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double dx, double dy, double dz) {
            return new TimeStopMoteParticle(level, x, y, z, dx, dy, dz, sprites);
        }
    }
}
