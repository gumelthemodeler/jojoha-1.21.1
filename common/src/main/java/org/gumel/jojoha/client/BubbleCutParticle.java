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
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class BubbleCutParticle extends TextureSheetParticle {
    private static final int WIPE_TICKS = 2;
    private static final int HOLD_TICKS = 1;
    private static final int FADE_TICKS = 4;
    private static final float THICKNESS = 0.25F;

    private final int delay;
    private final float length;
    private final float slant;

    private BubbleCutParticle(ClientLevel level, double x, double y, double z, double slant, double length,
                              double delay, SpriteSet sprites) {
        super(level, x, y, z, 0.0, 0.0, 0.0);
        this.xd = 0.0;
        this.yd = 0.0;
        this.zd = 0.0;
        this.gravity = 0F;
        this.hasPhysics = false;
        this.slant = (float) slant;
        this.length = (float) length;
        this.delay = Math.max(0, (int) Math.round(delay));
        this.lifetime = this.delay + WIPE_TICKS + HOLD_TICKS + FADE_TICKS;
        this.pickSprite(sprites);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
        }
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        float shown = this.age + partialTick - this.delay;
        if (shown <= 0F) {
            return;
        }
        float wipe = Mth.clamp(shown / WIPE_TICKS, 0F, 1F);
        float fading = Mth.clamp((shown - WIPE_TICKS - HOLD_TICKS) / FADE_TICKS, 0F, 1F);
        float alpha = 1F - fading;
        float half = this.length * 0.5F;
        float thick = this.length * THICKNESS * 0.5F * (1F - 0.6F * fading);
        float from = -half;
        float to = -half + this.length * wipe;
        float u0 = getU0();
        float u1 = Mth.lerp(wipe, getU0(), getU1());

        Vec3 eye = camera.getPosition();
        float cx = (float) (this.x - eye.x);
        float cy = (float) (this.y - eye.y);
        float cz = (float) (this.z - eye.z);
        Quaternionf facing = new Quaternionf(camera.rotation()).rotateZ(this.slant);
        float[][] corners = {{to, -thick}, {to, thick}, {from, thick}, {from, -thick}};
        float[][] uvs = {{u1, getV1()}, {u1, getV0()}, {u0, getV0()}, {u0, getV1()}};
        for (int k = 0; k < 4; k++) {
            Vector3f at = new Vector3f(corners[k][0], corners[k][1], 0F).rotate(facing).add(cx, cy, cz);
            buffer.addVertex(at.x(), at.y(), at.z()).setUv(uvs[k][0], uvs[k][1])
                    .setColor(1F, 1F, 1F, alpha).setLight(0xF000F0);
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
                                       double slant, double length, double delay) {
            return new BubbleCutParticle(level, x, y, z, slant, length, delay, sprites);
        }
    }
}
