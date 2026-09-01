package org.gumel.jojoha.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import org.gumel.jojoha.stand.StandEntity;

/**
 * The motes that gather while a time stop is being wound up.
 *
 * <p>Each takes a colour of its own rather than the Stand's, which is the whole point of them: the
 * Stand's own aura is one colour and says whose it is, and these say that something is being done to
 * the world instead. Hues are spread around the wheel rather than picked at random, so a handful of
 * motes on screen at once are reliably different from each other - true random would clump, and a
 * clump of three near-identical motes reads as a single colour that happens to be wrong.
 *
 * <p>They circle the Stand rather than drifting. A mote with its own velocity leaves, and a mote
 * that leaves says the wind-up is dispersing; motes held in orbit say it is being gathered and kept.
 * The orbit is recomputed from the Stand's position every tick rather than integrated from a
 * velocity, so the ring follows the Stand about instead of being left behind wherever it started.
 *
 * <p>Bound to the Stand by proximity, because a SimpleParticleType carries no payload to name one
 * in. Motes are spawned in a tight shell around it, so the nearest Stand is the right one in every
 * case that matters.
 */
public final class TimeStopMoteParticle extends TextureSheetParticle {
    /** How far round the wheel each successive mote is placed. */
    private static final float HUE_STEP = 0.31F;

    /** How far a mote may be from a Stand and still be counted as circling it. */
    private static final double BIND_RADIUS = 4.0;

    /**
     * Turns per tick at no charge and at full, how far out the ring sits, and the climb.
     *
     * <p>Read live from the shared charge every tick rather than fixed when the mote is born, so a
     * ring already in flight winds up along with the one being spawned into it. Set at birth, the
     * ring would be a record of how charged the move was when each mote happened to appear, which
     * looks like several rings at different speeds rather than one gathering.
     */
    private static final double ORBIT_SPEED = 0.10;
    private static final double ORBIT_SPEED_CHARGED = 0.52;

    /** How hard a mote is flung outward when the charge lets go, and how fast it dies doing it. */
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

        // Let go: the ring stops holding them and they carry on the way they were going, outward.
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

        // Set outright rather than moved by a velocity: the ring is a position around the Stand, and
        // integrating toward it would let a moving Stand drag the motes into a comet tail.
        setPos(this.around.getX() + Math.cos(this.angle) * this.orbitRadius,
                this.around.getY() + this.height,
                this.around.getZ() + Math.sin(this.angle) * this.orbitRadius);

        this.setSpriteFromAge(this.sprites);
    }

    private static StandEntity nearestStand(ClientLevel level, double x, double y, double z) {
        StandEntity best = null;
        double bestDistance = BIND_RADIUS * BIND_RADIUS;

        for (StandEntity stand : level.getEntitiesOfClass(StandEntity.class,
                new net.minecraft.world.phys.AABB(x - BIND_RADIUS, y - BIND_RADIUS, z - BIND_RADIUS,
                        x + BIND_RADIUS, y + BIND_RADIUS, z + BIND_RADIUS))) {
            double distance = stand.distanceToSqr(x, y, z);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = stand;
            }
        }

        return best;
    }

    /** Swells then shrinks away, so motes dissipate rather than blinking out. */
    @Override
    public float getQuadSize(float partialTick) {
        float progress = Mth.clamp((this.age + partialTick) / this.lifetime, 0F, 1F);
        return this.quadSize * (1F - progress * progress) * 1.3F;
    }

    /** Additive, so overlapping motes build light instead of stacking into flat colour. */
    @Override
    public ParticleRenderType getRenderType() {
        return EmissiveParticleRenderType.INSTANCE;
    }

    /** Full brightness regardless of world lighting, as the aura motes are. */
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
