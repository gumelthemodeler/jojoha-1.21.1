package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.hamon.BubbleFlight;
import org.joml.Vector3f;

public final class HamonBubbleParticle extends TextureSheetParticle {
    private final float tint;

    private final float fullSize;

    private static final float BIRTH_SCALE = 0.35F;
    private static final float OPEN_TICKS = 3F;

    private static final int TRAIL_EVERY = 2;

    private static final int DISC_SPRITE = 3;
    private static final float DISC_SPIN = 0.9F;
    private static final float DISC_TILT_MIN = 25F;
    private static final float DISC_TILT_SPREAD = 20F;

    private final boolean disc;
    private final Vector3f heading = new Vector3f(0F, 0F, 1F);
    private final Vector3f face = new Vector3f(1F, 0F, 0F);
    private float spin;
    private float oSpin;

    private HamonBubbleParticle(ClientLevel level, double x, double y, double z,
                                double dx, double dy, double dz, SpriteSet sprites,
                                BubbleFlight.Kind kind, float size, boolean disc) {
        super(level, x, y, z, 0.0, 0.0, 0.0);
        this.disc = disc;

        this.xd = dx;
        this.yd = dy;
        this.zd = dz;

        this.gravity = 0F;
        this.friction = (float) kind.drag();
        this.hasPhysics = true;
        this.lifetime = BubbleFlight.lifetime(Math.sqrt(dx * dx + dy * dy + dz * dz), kind);

        this.tint = this.random.nextFloat();
        this.fullSize = size * (0.75F + this.random.nextFloat() * 0.5F);
        this.quadSize = this.fullSize;
        this.roll = this.random.nextFloat() * Mth.TWO_PI;
        this.oRoll = this.roll;
        this.alpha = 0.92F;

        if (disc) {
            this.setSprite(sprites.get(DISC_SPRITE, DISC_SPRITE));
            aim(dx, dy, dz);
        } else {
            this.pickSprite(sprites);
        }
        this.shimmer();
    }

    private void aim(double dx, double dy, double dz) {
        Vector3f forward = new Vector3f((float) dx, (float) dy, (float) dz);
        if (forward.lengthSquared() > 1.0E-6F) {
            heading.set(forward.normalize());
        }
        Vector3f side = new Vector3f(heading).cross(0F, 1F, 0F);
        if (side.lengthSquared() < 1.0E-4F) {
            side.set(1F, 0F, 0F);
        }
        side.normalize();
        Vector3f lift = new Vector3f(side).cross(heading).normalize();
        float tilt = (DISC_TILT_MIN + this.random.nextFloat() * DISC_TILT_SPREAD) * Mth.DEG_TO_RAD
                * (this.random.nextBoolean() ? 1F : -1F);
        face.set(side).mul(Mth.cos(tilt)).add(lift.mul(Mth.sin(tilt)));
        this.spin = this.random.nextFloat() * Mth.TWO_PI;
        this.oSpin = this.spin;
    }

    @Override
    public void tick() {
        double wantX = this.xd;
        double wantY = this.yd;
        double wantZ = this.zd;

        this.oSpin = this.spin;
        this.spin += DISC_SPIN;
        super.tick();
        if (this.removed) {
            pop();
            return;
        }

        double wanted = wantX * wantX + wantY * wantY + wantZ * wantZ;
        double moved = (this.x - this.xo) * (this.x - this.xo)
                + (this.y - this.yo) * (this.y - this.yo)
                + (this.z - this.zo) * (this.z - this.zo);
        if (wanted > 1.0E-4 && moved < wanted * 0.25) {
            pop();
            this.remove();
            return;
        }

        this.shimmer();

        if (this.age % TRAIL_EVERY == 0) {
            float through = (float) this.age / this.lifetime;
            this.level.addParticle(film(this.tint, through, this.fullSize * 2.2F),
                    this.xo, this.yo, this.zo, 0.0, 0.0, 0.0);
        }
    }

    private void shimmer() {
        float through = Mth.clamp((float) this.age / this.lifetime, 0F, 1F);
        float at = Mth.lerp(through, this.tint, 1F - this.tint);
        this.setColor(
                BubbleFlight.mix(BubbleFlight.PINK_R, BubbleFlight.CYAN_R, at),
                BubbleFlight.mix(BubbleFlight.PINK_G, BubbleFlight.CYAN_G, at),
                BubbleFlight.mix(BubbleFlight.PINK_B, BubbleFlight.CYAN_B, at));
    }

    private void pop() {
        this.level.addParticle(ParticleTypes.BUBBLE_POP, this.x, this.y, this.z, 0.0, 0.0, 0.0);
        for (int i = 0; i < 5; i++) {
            double vx = (this.random.nextDouble() - 0.5) * 0.16;
            double vy = (this.random.nextDouble() - 0.5) * 0.16;
            double vz = (this.random.nextDouble() - 0.5) * 0.16;
            this.level.addParticle(film(this.tint, this.random.nextFloat(), this.fullSize * 2.6F),
                    this.x, this.y, this.z, vx, vy, vz);
        }
    }

    static DustColorTransitionOptions film(float tint, float through, float size) {
        float at = Mth.lerp(through, tint, 1F - tint);
        Vector3f from = new Vector3f(
                BubbleFlight.mix(BubbleFlight.PINK_R, BubbleFlight.CYAN_R, at),
                BubbleFlight.mix(BubbleFlight.PINK_G, BubbleFlight.CYAN_G, at),
                BubbleFlight.mix(BubbleFlight.PINK_B, BubbleFlight.CYAN_B, at));
        Vector3f to = new Vector3f(
                BubbleFlight.mix(BubbleFlight.PINK_R, BubbleFlight.CYAN_R, 1F - at),
                BubbleFlight.mix(BubbleFlight.PINK_G, BubbleFlight.CYAN_G, 1F - at),
                BubbleFlight.mix(BubbleFlight.PINK_B, BubbleFlight.CYAN_B, 1F - at));
        return new DustColorTransitionOptions(from, to, Mth.clamp(size, 0.3F, 1.6F));
    }

    @Override
    public float getQuadSize(float partialTick) {
        float open = Mth.clamp((this.age + partialTick) / OPEN_TICKS, BIRTH_SCALE, 1F);
        return this.fullSize * open;
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        if (!disc) {
            super.render(buffer, camera, partialTick);
            return;
        }
        Vec3 eye = camera.getPosition();
        float cx = (float) (Mth.lerp(partialTick, this.xo, this.x) - eye.x);
        float cy = (float) (Mth.lerp(partialTick, this.yo, this.y) - eye.y);
        float cz = (float) (Mth.lerp(partialTick, this.zo, this.z) - eye.z);
        float radius = getQuadSize(partialTick) * Mth.SQRT_OF_TWO;
        float turn = Mth.lerp(partialTick, this.oSpin, this.spin);
        float[][] uvs = {{getU0(), getV0()}, {getU1(), getV0()}, {getU1(), getV1()}, {getU0(), getV1()}};
        Vector3f[] corners = new Vector3f[4];
        for (int k = 0; k < 4; k++) {
            float angle = turn + Mth.HALF_PI * 0.5F + k * Mth.HALF_PI;
            corners[k] = new Vector3f(heading).mul(Mth.cos(angle) * radius)
                    .add(new Vector3f(face).mul(Mth.sin(angle) * radius)).add(cx, cy, cz);
        }
        int light = getLightColor(partialTick);
        for (int k = 0; k < 4; k++) {
            discVertex(buffer, corners[k], uvs[k], light);
        }
        for (int k = 3; k >= 0; k--) {
            discVertex(buffer, corners[k], uvs[k], light);
        }
    }

    private void discVertex(VertexConsumer buffer, Vector3f at, float[] uv, int light) {
        buffer.addVertex(at.x(), at.y(), at.z()).setUv(uv[0], uv[1])
                .setColor(this.rCol, this.gCol, this.bCol, this.alpha).setLight(light);
    }

    @Override
    public int getLightColor(float partialTick) {
        return 240;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return EmissiveParticleRenderType.INSTANCE;
    }

    public static final class DriftProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public DriftProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double dx, double dy, double dz) {
            return new HamonBubbleParticle(level, x, y, z, dx, dy, dz, sprites,
                    BubbleFlight.Kind.DRIFT, 0.24F, true);
        }
    }

    public static final class ShotProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public ShotProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double dx, double dy, double dz) {
            return new HamonBubbleParticle(level, x, y, z, dx, dy, dz, sprites,
                    BubbleFlight.Kind.SHOT, 0.34F, false);
        }
    }
}
