package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.combat.fx.MovementFx;
import org.gumel.jojoha.hamon.BubbleFlight;
import org.gumel.jojoha.hamon.BubbleSheet;
import org.gumel.jojoha.hamon.moves.BubbleLauncherMove;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

public final class BubbleSheetParticle extends Particle {
    private static final int ACROSS = 8;
    private static final int DOWN = 4;
    private static final float OPEN_SHARE = 0.8F;
    private static final float FADE_IN_TICKS = 2F;
    private static final float FILM_ALPHA = 0.85F;
    private static final double WOBBLE = 0.04;
    private static final int WOBBLE_STEP_TICKS = 2;
    private static final float BUBBLE_SIZE = 0.09F;
    private static final float BUBBLE_GROW = 0.18F;
    private static final double BUBBLE_LIFT = 0.04;
    private static final int BURST_MOTES = 40;
    private static final int BURST_POPS = 10;
    private static final int LIGHT = 0xF000F0;

    private final int casterId;
    private final TextureAtlasSprite film;
    private final TextureAtlasSprite[] bubbles;
    private final List<BubbleSheet.Slot> slots;
    private final float[] sizes;
    private final float[] tints;
    private final int[] looks;

    private BubbleSheetParticle(ClientLevel level, double x, double y, double z, int casterId, long seed,
                                SpriteSet sprites) {
        super(level, x, y, z);
        this.casterId = casterId;
        this.gravity = 0F;
        this.hasPhysics = false;
        this.lifetime = MovementFx.BUBBLE_LAUNCH_TICKS;
        this.film = sprites.get(0, 3);
        this.bubbles = new TextureAtlasSprite[]{sprites.get(1, 3), sprites.get(2, 3), sprites.get(3, 3)};
        this.slots = BubbleSheet.slots(seed, BubbleLauncherMove.BUBBLES, BubbleLauncherMove.ARC_DEGREES);

        RandomSource dress = RandomSource.create(seed * 31L + 7L);
        this.sizes = new float[slots.size()];
        this.tints = new float[slots.size()];
        this.looks = new int[slots.size()];
        for (int i = 0; i < slots.size(); i++) {
            sizes[i] = BUBBLE_SIZE * (0.7F + dress.nextFloat() * 0.7F);
            tints[i] = dress.nextFloat();
            looks[i] = dress.nextInt(bubbles.length);
        }
        this.setSize(6F, 4F);
    }

    private static BubbleSheet.Frame frame(Entity caster, float partialTick) {
        return BubbleSheet.Frame.of(caster.getEyePosition(partialTick), caster.getViewVector(partialTick),
                caster.getViewYRot(partialTick) * Mth.DEG_TO_RAD);
    }

    private double open(float ticks) {
        float through = Mth.clamp(ticks / (this.lifetime * OPEN_SHARE), 0F, 1F);
        return 1.0 - Math.pow(1.0 - through, 3.0);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        Entity caster = this.level.getEntity(casterId);
        if (caster == null) {
            this.remove();
            return;
        }
        BubbleSheet.Frame frame = frame(caster, 1F);
        Vec3 centre = frame.point(0.0, 0.0, open(this.age), 0.0);
        this.setPos(centre.x, centre.y, centre.z);
        if (++this.age >= this.lifetime) {
            burst(frame);
            this.remove();
        }
    }

    private void burst(BubbleSheet.Frame frame) {
        for (int i = 0; i < BURST_MOTES; i++) {
            double u = this.random.nextDouble() * 2.0 - 1.0;
            double v = this.random.nextDouble() * 2.0 - 1.0;
            Vec3 at = frame.point(u, v, 1.0, 0.0);
            Vec3 out = at.subtract(frame.origin()).normalize().scale(0.04 + this.random.nextDouble() * 0.1);
            this.level.addParticle(HamonBubbleParticle.film(this.random.nextFloat(), this.random.nextFloat(), 0.9F),
                    at.x, at.y, at.z, out.x, out.y + 0.02, out.z);
        }
        for (int i = 0; i < BURST_POPS; i++) {
            double u = this.random.nextBoolean() ? 1.0 : -1.0;
            double v = this.random.nextDouble() * 2.0 - 1.0;
            Vec3 at = this.random.nextBoolean() ? frame.point(u, v, 1.0, 0.0) : frame.point(v, u, 1.0, 0.0);
            this.level.addParticle(ParticleTypes.BUBBLE_POP, at.x, at.y, at.z, 0.0, 0.0, 0.0);
        }
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        Entity caster = this.level.getEntity(casterId);
        if (caster == null) {
            return;
        }
        float ticks = this.age + partialTick;
        double open = open(ticks);
        float fade = Mth.clamp(ticks / FADE_IN_TICKS, 0F, 1F);
        float arc = BubbleLauncherMove.ARC_DEGREES;
        BubbleSheet.Frame frame = frame(caster, partialTick);
        Vec3 camera0 = camera.getPosition();

        Vec3[][] grid = new Vec3[ACROSS + 1][DOWN + 1];
        float beat = (float) (Math.floor(ticks / WOBBLE_STEP_TICKS) * WOBBLE_STEP_TICKS);
        for (int i = 0; i <= ACROSS; i++) {
            for (int j = 0; j <= DOWN; j++) {
                double u = -1.0 + 2.0 * i / ACROSS;
                double v = -1.0 + 2.0 * j / DOWN;
                grid[i][j] = frame.point(u, v, open, wobble(beat, open, u, v)).subtract(camera0);
            }
        }

        for (int i = 0; i < ACROSS; i++) {
            for (int j = 0; j < DOWN; j++) {
                int[][] corners = {{i, j}, {i + 1, j}, {i + 1, j + 1}, {i, j + 1}};
                for (int k = 0; k < 4; k++) {
                    filmVertex(buffer, grid, corners[k][0], corners[k][1], open, fade);
                }
                for (int k = 3; k >= 0; k--) {
                    filmVertex(buffer, grid, corners[k][0], corners[k][1], open, fade);
                }
            }
        }

        Quaternionf rotation = camera.rotation();
        for (int s = 0; s < slots.size(); s++) {
            BubbleSheet.Slot slot = slots.get(s);
            float grow = Mth.clamp((float) (open - slot.appear()) / BUBBLE_GROW, 0F, 1F);
            if (grow <= 0F) {
                continue;
            }
            double u = slot.u(arc);
            Vec3 at = frame.point(u, slot.v(), open, wobble(beat, open, u, slot.v()) + BUBBLE_LIFT).subtract(camera0);
            float size = sizes[s] * (grow < 0.5F ? 0.5F : 1F);
            float tint = tints[s];
            float r = Mth.lerp(0.5F, 1F, BubbleFlight.mix(BubbleFlight.PINK_R, BubbleFlight.CYAN_R, tint));
            float g = Mth.lerp(0.5F, 1F, BubbleFlight.mix(BubbleFlight.PINK_G, BubbleFlight.CYAN_G, tint));
            float b = Mth.lerp(0.5F, 1F, BubbleFlight.mix(BubbleFlight.PINK_B, BubbleFlight.CYAN_B, tint));
            billboard(buffer, rotation, at, size, bubbles[looks[s]], r, g, b, 0.9F * fade);
        }
    }

    private static double wobble(float beat, double open, double u, double v) {
        return WOBBLE * open * Math.sin(beat * 0.45 + u * 4.0 + v * 2.5);
    }

    private void filmVertex(VertexConsumer buffer, Vec3[][] grid, int i, int j, double open, float fade) {
        Vec3 at = grid[i][j];
        float alpha = FILM_ALPHA * fade * (0.35F + 0.65F * (float) open);
        buffer.addVertex((float) at.x, (float) at.y, (float) at.z)
                .setUv(film.getU((float) i / ACROSS), film.getV(1F - (float) j / DOWN))
                .setColor(1F, 1F, 1F, alpha)
                .setLight(LIGHT);
    }

    private static void billboard(VertexConsumer buffer, Quaternionf rotation, Vec3 at, float size,
                                  TextureAtlasSprite sprite, float r, float g, float b, float alpha) {
        float[][] corners = {{1F, -1F}, {1F, 1F}, {-1F, 1F}, {-1F, -1F}};
        float[][] uvs = {{sprite.getU1(), sprite.getV1()}, {sprite.getU1(), sprite.getV0()},
                {sprite.getU0(), sprite.getV0()}, {sprite.getU0(), sprite.getV1()}};
        for (int k = 0; k < 4; k++) {
            Vector3f corner = new Vector3f(corners[k][0], corners[k][1], 0F).rotate(rotation).mul(size)
                    .add((float) at.x, (float) at.y, (float) at.z);
            buffer.addVertex(corner.x(), corner.y(), corner.z())
                    .setUv(uvs[k][0], uvs[k][1])
                    .setColor(r, g, b, alpha)
                    .setLight(LIGHT);
        }
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
                                       double caster, double seed, double unused) {
            return new BubbleSheetParticle(level, x, y, z, (int) Math.round(caster), Math.round(seed), sprites);
        }
    }
}
