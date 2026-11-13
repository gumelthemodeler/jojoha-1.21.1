package org.gumel.jojoha.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.data.ClientPlayerDataCache;
import org.gumel.jojoha.data.StandData;
import org.gumel.jojoha.stand.StandShadowForm;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class AwakeningLight {
    private static final float TAU = Mth.TWO_PI;

    private static final float CHEST_HEIGHT = 0.6F;
    private static final float SUN_START = 0.25F;
    private static final int SUN_SLOTS = 30;
    private static final int SUN_MIN_LIFE = 7;
    private static final int SUN_MAX_LIFE = 16;
    private static final float SUN_SHORT = 1.8F;
    private static final float SUN_LONG = 5.0F;
    private static final float SUN_BASE = 0.06F;
    private static final float SUN_TIP_MIN = 0.35F;
    private static final float SUN_TIP_MAX = 0.95F;
    private static final float SUN_LIFT = 0.25F;

    private static final int HERO_RAYS = 6;
    private static final float HERO_LENGTH = 6.0F;
    private static final float HERO_TIP = 1.1F;
    private static final float HERO_TURN = 0.02F;
    private static final float HERO_RISE_TICKS = 5F;

    private static final float CORE_WIDTH = 0.4F;
    private static final float CORE_LENGTH = 0.7F;

    private static final int MASK_SLOTS = 18;
    private static final int MASK_MIN_LIFE = 6;
    private static final int MASK_MAX_LIFE = 13;
    private static final float MASK_DEPTH = -4.8F;
    private static final float MASK_CENTRE_Y = -4.5F;
    private static final float MASK_RADIUS_X = 4.8F;
    private static final float MASK_RADIUS_Y = 5.2F;
    private static final float MASK_FORWARD_MIN = 0.35F;
    private static final float MASK_FORWARD_MAX = 0.9F;
    private static final float RIM_SHORT = 30F;
    private static final float RIM_LONG = 100F;
    private static final float RIM_BASE = 0.05F;
    private static final float RIM_TIP_MIN = 0.3F;
    private static final float RIM_TIP_MAX = 0.65F;
    private static final float EYE_X = 2F;
    private static final float EYE_Y = -4F;
    private static final float EYE_SPREAD = 0.15F;
    private static final float EYE_LENGTH = 120F;
    private static final float EYE_BASE = 0.08F;
    private static final float EYE_TIP = 0.7F;

    private static final float SHOOT_SHARE = 0.3F;
    private static final float APPEAR_SHARE = 0.08F;
    private static final float SWAY = 0.12F;
    private static final float RIM_ALPHA = 0.8F;
    private static final int ENVELOPE_IN_TICKS = 2;
    private static final int FADE_OUT_TICKS = 12;

    private static final float DARKEN = 0.6F;
    private static final float DARK_NEAR = 16F;
    private static final float DARK_FAR = 40F;
    private static final double FLASH_RANGE = 24.0;

    private static final int FALLBACK_RIM = 0xFFE9A8;
    private static final int WHITE = 0xFFFFFF;

    private static final Map<UUID, Vector3f> CHESTS = new HashMap<>();
    private static final Map<UUID, Matrix4f> FACES = new HashMap<>();

    private AwakeningLight() {
    }

    public static void chest(Player player, PoseStack poseStack, float clientTimeTicks) {
        UUID id = player.getUUID();
        if (StandAwakeningRays.raying(id, clientTimeTicks) < 0F || StandAwakeningRays.masked(id)) {
            return;
        }
        CHESTS.put(id, poseStack.last().pose().transformPosition(0F, player.getBbHeight() * CHEST_HEIGHT, 0F,
                new Vector3f()));
    }

    public static void face(Player player, PoseStack poseStack, float clientTimeTicks) {
        UUID id = player.getUUID();
        if (StandAwakeningRays.raying(id, clientTimeTicks) < 0F || !StandAwakeningRays.masked(id)) {
            return;
        }
        FACES.put(id, new Matrix4f(poseStack.last().pose()));
    }

    public static void burst(UUID playerId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        Player player = minecraft.level.getPlayerByUUID(playerId);
        if (player != null && player.distanceToSqr(minecraft.player) < FLASH_RANGE * FLASH_RANGE) {
            ImpactFrame.begin(1F);
        }
    }

    public static void clear() {
        CHESTS.clear();
        FACES.clear();
    }

    public static void render(Matrix4f modelView, Matrix4f projection, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (CHESTS.isEmpty() && FACES.isEmpty() || minecraft.level == null) {
            clear();
            return;
        }
        float now = minecraft.level.getGameTime() + partialTick;

        float dark = 0F;
        for (Map.Entry<UUID, Vector3f> entry : CHESTS.entrySet()) {
            dark = Math.max(dark, darkness(StandAwakeningRays.raying(entry.getKey(), now), entry.getValue().length()));
        }
        for (Map.Entry<UUID, Matrix4f> entry : FACES.entrySet()) {
            Vector3f at = entry.getValue().transformPosition(0F, MASK_CENTRE_Y / 16F, 0F, new Vector3f());
            dark = Math.max(dark, darkness(StandAwakeningRays.raying(entry.getKey(), now), at.length()));
        }

        minecraft.getMainRenderTarget().bindWrite(false);
        Matrix4fStack stack = RenderSystem.getModelViewStack();
        RenderSystem.backupProjectionMatrix();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        if (dark > 0F) {
            RenderSystem.disableDepthTest();
            RenderSystem.setProjectionMatrix(new Matrix4f(), VertexSorting.ORTHOGRAPHIC_Z);
            stack.pushMatrix();
            stack.identity();
            RenderSystem.applyModelViewMatrix();
            BufferBuilder shade = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,
                    DefaultVertexFormat.POSITION_COLOR);
            int black = Mth.clamp(Math.round(dark * 255F), 0, 255) << 24;
            shade.addVertex(-1F, -1F, 0F).setColor(black);
            shade.addVertex(1F, -1F, 0F).setColor(black);
            shade.addVertex(1F, 1F, 0F).setColor(black);
            shade.addVertex(-1F, 1F, 0F).setColor(black);
            BufferUploader.drawWithShader(shade.buildOrThrow());
            stack.popMatrix();
        }

        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.setProjectionMatrix(new Matrix4f(projection), VertexSorting.DISTANCE_TO_ORIGIN);
        stack.pushMatrix();
        stack.mul(modelView);
        RenderSystem.applyModelViewMatrix();

        BufferBuilder rays = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (Map.Entry<UUID, Vector3f> entry : CHESTS.entrySet()) {
            sunburst(rays, entry.getKey(), new Vec3(entry.getValue()), StandAwakeningRays.raying(entry.getKey(), now));
        }
        for (Map.Entry<UUID, Matrix4f> entry : FACES.entrySet()) {
            mask(rays, entry.getKey(), entry.getValue(), StandAwakeningRays.raying(entry.getKey(), now));
        }
        MeshData mesh = rays.build();
        if (mesh != null) {
            BufferUploader.drawWithShader(mesh);
        }

        stack.popMatrix();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.restoreProjectionMatrix();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        clear();
    }

    private static void sunburst(BufferBuilder out, UUID id, Vec3 chest, float elapsed) {
        float envelope = envelope(elapsed);
        if (envelope <= 0F) {
            return;
        }
        int seed = id.hashCode();
        int rim = rim(id);
        for (int slot = 0; slot < SUN_SLOTS; slot++) {
            float[] life = life(seed, slot, elapsed, SUN_MIN_LIFE, SUN_MAX_LIFE);
            float phase = life[0];
            int cycle = (int) life[1];
            Vec3 heading = sphere(seed, slot, cycle);
            float reach = Mth.lerp(unit(seed, slot, cycle * 3 + 1), SUN_SHORT, SUN_LONG);
            float tip = Mth.lerp(unit(seed, slot, cycle * 3 + 2), SUN_TIP_MIN, SUN_TIP_MAX);
            float length = reach * shoot(phase) * (1F + SWAY * phase);
            light(out, chest.add(heading.scale(SUN_START)), heading, length, SUN_BASE, tip, rim,
                    envelope * glow(phase));
        }
        for (int hero = 0; hero < HERO_RAYS; hero++) {
            float yaw = unit(seed, SUN_SLOTS + hero, 0) * TAU + elapsed * HERO_TURN * (hero % 2 == 0 ? 1F : -1F);
            float pitch = (unit(seed, SUN_SLOTS + hero, 1) - 0.4F) * 1.4F;
            Vec3 heading = new Vec3(Mth.cos(yaw) * Mth.cos(pitch), Mth.sin(pitch), Mth.sin(yaw) * Mth.cos(pitch));
            float breath = 1F + SWAY * Mth.sin(elapsed * 0.9F + hero * 1.7F);
            float length = HERO_LENGTH * shoot(elapsed / HERO_RISE_TICKS) * breath;
            light(out, chest.add(heading.scale(SUN_START)), heading, length, SUN_BASE, HERO_TIP * breath, rim,
                    envelope);
        }
    }

    private static void mask(BufferBuilder out, UUID id, Matrix4f head, float elapsed) {
        float envelope = envelope(elapsed);
        if (envelope <= 0F) {
            return;
        }
        int seed = id.hashCode();
        int rim = packed(StandAwakeningRays.glowTint(id));
        for (int slot = 0; slot < MASK_SLOTS; slot++) {
            float[] life = life(seed, slot, elapsed, MASK_MIN_LIFE, MASK_MAX_LIFE);
            float phase = life[0];
            int cycle = (int) life[1];
            float around = (slot + unit(seed, slot, cycle * 4)) * TAU / MASK_SLOTS;
            float cos = Mth.cos(around);
            float sin = Mth.sin(around);
            float forward = Mth.lerp(unit(seed, slot, cycle * 4 + 1), MASK_FORWARD_MIN, MASK_FORWARD_MAX);
            Vector3f base = new Vector3f(MASK_RADIUS_X * cos, MASK_CENTRE_Y + MASK_RADIUS_Y * sin, MASK_DEPTH);
            Vector3f heading = new Vector3f(cos, sin, -forward).normalize();
            float reach = Mth.lerp(unit(seed, slot, cycle * 4 + 2), RIM_SHORT, RIM_LONG);
            float tip = Mth.lerp(unit(seed, slot, cycle * 4 + 3), RIM_TIP_MIN, RIM_TIP_MAX);
            beam(out, head, base, heading, reach * shoot(phase) * (1F + SWAY * phase), RIM_BASE, tip, rim,
                    envelope * glow(phase));
        }
        for (int s = -1; s <= 1; s += 2) {
            Vector3f base = new Vector3f(s * EYE_X, EYE_Y, MASK_DEPTH);
            Vector3f heading = new Vector3f(s * EYE_SPREAD, 0F, -1F).normalize();
            float breath = 1F + SWAY * Mth.sin(elapsed * 1.3F + s);
            beam(out, head, base, heading, EYE_LENGTH * shoot(elapsed / HERO_RISE_TICKS) * breath, EYE_BASE,
                    EYE_TIP * breath, rim, envelope);
        }
    }

    private static void beam(BufferBuilder out, Matrix4f head, Vector3f base, Vector3f heading, float length,
                             float baseWidth, float tipWidth, int rim, float strength) {
        Vec3 from = at(head, base);
        Vec3 to = at(head, new Vector3f(heading).mul(length).add(base));
        Vec3 along = to.subtract(from);
        if (along.lengthSqr() < 1.0E-6) {
            return;
        }
        light(out, from, along.normalize(), (float) along.length(), baseWidth, tipWidth, rim, strength);
    }

    private static void light(BufferBuilder out, Vec3 from, Vec3 heading, float length, float baseWidth,
                              float tipWidth, int rim, float strength) {
        if (strength <= 0.01F || length <= 0.01F) {
            return;
        }
        shaft(out, from, from.add(heading.scale(length)), baseWidth, tipWidth, rim, strength * RIM_ALPHA);
        shaft(out, from, from.add(heading.scale(length * CORE_LENGTH)), baseWidth * CORE_WIDTH,
                tipWidth * CORE_WIDTH, WHITE, strength);
    }

    private static Vec3 at(Matrix4f head, Vector3f pixels) {
        return new Vec3(head.transformPosition(pixels.x / 16F, pixels.y / 16F, pixels.z / 16F, new Vector3f()));
    }

    private static void shaft(BufferBuilder out, Vec3 from, Vec3 to, float baseWidth, float tipWidth, int rgb,
                              float alpha) {
        Vec3 side = to.subtract(from).cross(from.add(to).scale(-0.5));
        if (side.lengthSqr() < 1.0E-8) {
            return;
        }
        side = side.normalize();
        int base = Mth.clamp(Math.round(alpha * 255F), 0, 255) << 24 | rgb;
        vertex(out, from.add(side.scale(baseWidth / 2F)), base);
        vertex(out, from.subtract(side.scale(baseWidth / 2F)), base);
        vertex(out, to.subtract(side.scale(tipWidth / 2F)), rgb);
        vertex(out, to.add(side.scale(tipWidth / 2F)), rgb);
    }

    private static void vertex(BufferBuilder out, Vec3 at, int argb) {
        out.addVertex((float) at.x, (float) at.y, (float) at.z).setColor(argb);
    }

    private static float[] life(int seed, int slot, float elapsed, int shortest, int longest) {
        float start = 0F;
        for (int cycle = 0; ; cycle++) {
            float span = Mth.lerp(unit(seed, slot + 101, cycle), shortest, longest);
            if (elapsed < start + span) {
                return new float[] {(elapsed - start) / span, cycle};
            }
            start += span;
        }
    }

    private static Vec3 sphere(int seed, int slot, int cycle) {
        float y = Mth.clamp(unit(seed, slot + 211, cycle) * 2F - 1F + SUN_LIFT, -1F, 1F);
        float turn = unit(seed, slot + 307, cycle) * TAU;
        float flat = Mth.sqrt(1F - y * y);
        return new Vec3(flat * Mth.cos(turn), y, flat * Mth.sin(turn));
    }

    private static float shoot(float phase) {
        float t = Mth.clamp(phase / SHOOT_SHARE, 0F, 1F);
        float left = 1F - t;
        return 1F - left * left * left;
    }

    private static float glow(float phase) {
        float appear = Mth.clamp(phase / APPEAR_SHARE, 0F, 1F);
        float after = Mth.clamp((phase - SHOOT_SHARE) / (1F - SHOOT_SHARE), 0F, 1F);
        return appear * (1F - after * after);
    }

    private static float envelope(float elapsed) {
        if (elapsed < 0F) {
            return 0F;
        }
        float in = Mth.clamp(elapsed / ENVELOPE_IN_TICKS, 0F, 1F);
        float out = Mth.clamp((StandAwakeningRays.DURATION_TICKS - elapsed) / FADE_OUT_TICKS, 0F, 1F);
        return in * out * out * (3F - 2F * out);
    }

    private static float darkness(float elapsed, float distance) {
        float near = 1F - Mth.clamp((distance - DARK_NEAR) / (DARK_FAR - DARK_NEAR), 0F, 1F);
        return DARKEN * envelope(elapsed) * near;
    }

    private static float unit(int seed, int ray, int cycle) {
        return (Mth.murmurHash3Mixer(seed * 31 + ray * 7919 + cycle * 104729) & 0xFFFF) / 65535F;
    }

    private static int rim(UUID id) {
        Minecraft minecraft = Minecraft.getInstance();
        StandData stand = ClientPlayerDataCache.data.stand;
        if (minecraft.player != null && id.equals(minecraft.player.getUUID()) && stand.isPresent()) {
            return StandShadowForm.auraColourFor(stand) & 0xFFFFFF;
        }
        return FALLBACK_RIM;
    }

    private static int packed(float[] rgb) {
        int r = Mth.clamp(Math.round(rgb[0] * 255F), 0, 255);
        int g = Mth.clamp(Math.round(rgb[1] * 255F), 0, 255);
        int b = Mth.clamp(Math.round(rgb[2] * 255F), 0, 255);
        return (r << 16) | (g << 8) | b;
    }
}
