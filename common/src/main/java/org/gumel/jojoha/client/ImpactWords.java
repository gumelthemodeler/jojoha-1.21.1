package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.combat.fx.ImpactTier;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayDeque;

public final class ImpactWords {
    private static final ResourceLocation SHEET =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/fx/impact_words.png");

    private static final int ROWS = 4;
    private static final int ROW_HEAVY = 0;
    private static final int ROW_FINISHER = 1;
    private static final int ROW_GUARD_BREAK = 2;
    private static final int ROW_PARRY = 3;

    private static final int LIFE_TICKS = 24;
    private static final float FADE_TICKS = 7F;
    private static final float POP_TICKS = 3F;
    private static final float OVERSHOOT = 1.7F;
    private static final float WIDTH = 1.5F;
    private static final float FINISHER_WIDTH = 1.9F;
    private static final float ASPECT = 0.25F;
    private static final float TILT_DEGREES = 14F;
    private static final float WHITEN = 0.45F;
    private static final double ABOVE = 0.7;
    private static final double SIDE = 0.55;
    private static final double RISE = 0.018;
    private static final int ALIVE = 5;
    private static final int GAP_TICKS = 6;
    private static final double GAP_BLOCKS = 3.0;
    private static final double RANGE = 48.0;

    private record Word(int row, Vec3 at, float side, float tilt, float width, int colour, long born) {
    }

    private static final ArrayDeque<Word> WORDS = new ArrayDeque<>();

    private ImpactWords() {
    }

    public static void clear() {
        WORDS.clear();
    }

    public static void landed(ImpactTier tier, Vec3 at, int colour, int seed) {
        ClientLevel level = Minecraft.getInstance().level;
        int row = rowOf(tier);
        if (level == null || row < 0 || !JojohaClientConfig.IMPACT_WORDS.on()) {
            return;
        }
        long now = level.getGameTime();
        for (Word word : WORDS) {
            if (now - word.born() < GAP_TICKS && word.at().distanceToSqr(at) < GAP_BLOCKS * GAP_BLOCKS) {
                return;
            }
        }
        RandomSource random = RandomSource.create(seed * 31L + row);
        float side = random.nextBoolean() ? 1F : -1F;
        float tilt = (random.nextFloat() * 2F - 1F) * TILT_DEGREES * Mth.DEG_TO_RAD;
        WORDS.addLast(new Word(row, at.add(0.0, ABOVE + random.nextDouble() * 0.3, 0.0), side, tilt,
                tier == ImpactTier.FINISHER ? FINISHER_WIDTH : WIDTH, whiten(colour), now));
        while (WORDS.size() > ALIVE) {
            WORDS.removeFirst();
        }
    }

    private static int rowOf(ImpactTier tier) {
        return switch (tier) {
            case HEAVY -> ROW_HEAVY;
            case FINISHER -> ROW_FINISHER;
            case GUARD_BREAK -> ROW_GUARD_BREAK;
            case PARRY -> ROW_PARRY;
            default -> -1;
        };
    }

    private static int whiten(int colour) {
        int red = colour >> 16 & 0xFF;
        int green = colour >> 8 & 0xFF;
        int blue = colour & 0xFF;
        red += Math.round((255 - red) * WHITEN);
        green += Math.round((255 - green) * WHITEN);
        blue += Math.round((255 - blue) * WHITEN);
        return red << 16 | green << 8 | blue;
    }

    public static void render(PoseStack poseStack, MultiBufferSource buffers, Camera camera, float partialTick) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || WORDS.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        WORDS.removeIf(word -> now - word.born() > LIFE_TICKS || now < word.born());
        if (WORDS.isEmpty()) {
            return;
        }
        Vec3 eye = camera.getPosition();
        Vector3f leftF = camera.getLeftVector();
        Vector3f upF = camera.getUpVector();
        Vec3 right = new Vec3(-leftF.x(), -leftF.y(), -leftF.z());
        Vec3 up = new Vec3(upF.x(), upF.y(), upF.z());
        Matrix4f pose = poseStack.last().pose();
        VertexConsumer out = buffers.getBuffer(RenderType.text(SHEET));
        for (Word word : WORDS) {
            float age = now - word.born() + partialTick;
            float fade = Mth.clamp((LIFE_TICKS - age) / FADE_TICKS, 0F, 1F);
            float size = pop(age);
            if (age < 0F || fade <= 0.01F || size <= 0.01F || word.at().distanceToSqr(eye) > RANGE * RANGE) {
                continue;
            }
            Vec3 centre = word.at().add(right.scale(word.side() * SIDE)).add(0.0, Math.floor(age) * RISE, 0.0);
            double halfWide = word.width() * size * 0.5;
            double halfTall = halfWide * ASPECT;
            double cos = Math.cos(word.tilt());
            double sin = Math.sin(word.tilt());
            Vec3 wide = right.scale(cos).add(up.scale(sin)).scale(halfWide);
            Vec3 tall = up.scale(cos).subtract(right.scale(sin)).scale(halfTall);
            int argb = Math.round(fade * 255F) << 24 | word.colour();
            float v0 = word.row() / (float) ROWS;
            float v1 = (word.row() + 1) / (float) ROWS;
            Vec3 bl = centre.subtract(wide).subtract(tall).subtract(eye);
            Vec3 br = centre.add(wide).subtract(tall).subtract(eye);
            Vec3 tr = centre.add(wide).add(tall).subtract(eye);
            Vec3 tl = centre.subtract(wide).add(tall).subtract(eye);
            vertex(out, pose, bl, argb, 0F, v1);
            vertex(out, pose, br, argb, 1F, v1);
            vertex(out, pose, tr, argb, 1F, v0);
            vertex(out, pose, tl, argb, 0F, v0);
            vertex(out, pose, tl, argb, 0F, v0);
            vertex(out, pose, tr, argb, 1F, v0);
            vertex(out, pose, br, argb, 1F, v1);
            vertex(out, pose, bl, argb, 0F, v1);
        }
    }

    private static float pop(float age) {
        if (age >= POP_TICKS) {
            return 1F;
        }
        float t = age / POP_TICKS - 1F;
        return 1F + (OVERSHOOT + 1F) * t * t * t + OVERSHOOT * t * t;
    }

    private static void vertex(VertexConsumer out, Matrix4f pose, Vec3 at, int argb, float u, float v) {
        out.addVertex(pose, (float) at.x, (float) at.y, (float) at.z)
                .setColor(argb)
                .setUv(u, v)
                .setLight(LightTexture.FULL_BRIGHT);
    }
}
