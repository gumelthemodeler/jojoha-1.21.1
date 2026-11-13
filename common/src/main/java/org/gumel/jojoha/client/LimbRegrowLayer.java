package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.body.Body;
import org.gumel.jojoha.body.Limbs;
import org.gumel.jojoha.registry.ModSounds;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class LimbRegrowLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public static final ResourceLocation FLESH =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/entity/limb/regrow_flesh.png");
    public static final ResourceLocation BONE =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/entity/limb/regrow_bone.png");
    private static final float PX = 1F / 16F;
    private static final float SHEET = 16F;
    private static final float SIDE_LENGTH = 12F;
    private static final float FLESH_STRIP = 4F;
    private static final float BONE_STRIP = 2F;
    private static final float BONE_UNTIL = 0.35F;
    private static final float MUSCLE_FROM = 0.25F, MUSCLE_UNTIL = 0.6F;
    private static final float SKIN_FROM = 0.5F;
    private static final float PATCH_POP = 0.22F;
    private static final float SLEEVE_LAG = 0.06F;
    private static final float PATCH_ALONG = 0.6F;
    private static final float PATCH_START_SIZE = 0.3F;
    private static final float PATCH_LIFT = 0.6F;
    private static final float TILE = 4F;
    private static final float SKIN_SHEET = 64F;
    private static final float SLEEVE_GROW = 0.25F;
    private static final int[][] SKIN_UV = {{40, 16, 40, 32}, {32, 48, 48, 48}, {0, 16, 0, 32}, {16, 48, 0, 48}};
    private static final PlayerModelPart[] OUTER = {PlayerModelPart.RIGHT_SLEEVE, PlayerModelPart.LEFT_SLEEVE,
            PlayerModelPart.RIGHT_PANTS_LEG, PlayerModelPart.LEFT_PANTS_LEG};
    private static final int[][] FACES = {{5, 4, 0, 1}, {2, 3, 7, 6}, {0, 4, 7, 3}, {1, 0, 3, 2}, {5, 1, 2, 6},
            {4, 5, 6, 7}};
    private static final float[][] NORMALS = {{0, -1, 0}, {0, 1, 0}, {-1, 0, 0}, {0, 0, -1}, {1, 0, 0}, {0, 0, 1}};
    private static final float[] REGEN_AT = {0F, MUSCLE_FROM, SKIN_FROM};
    private static final float[] REGEN_PITCH = {0.9F, 1.0F, 1.1F};
    private static final int HOLD_TICKS = 40;
    private static final int DRIP_EVERY = 5;
    private static final float BONE_OUT = 2F;
    private static final float INSET = 0.02F;

    private static final RandomSource RANDOM = RandomSource.create();
    private static final Map<Integer, Growth> GROWING = new HashMap<>();

    private static final class Growth {
        private final int limb;
        private final long born;
        private int cues;

        private Growth(int limb, long born) {
            this.limb = limb;
            this.born = born;
        }
    }

    public LimbRegrowLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    public static void start(int entityId, int limb) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            GROWING.put(entityId, new Growth(limb, level.getGameTime()));
        }
    }

    public static void cut(int entityId) {
        GROWING.remove(entityId);
    }

    public static void clear() {
        GROWING.clear();
    }

    public static boolean growing(Player player, int limb) {
        Growth growth = GROWING.get(player.getId());
        return growth != null && growth.limb == limb;
    }

    public static void tick() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || GROWING.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        Iterator<Map.Entry<Integer, Growth>> it = GROWING.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Growth> entry = it.next();
            Growth growth = entry.getValue();
            Entity entity = level.getEntity(entry.getKey());
            long age = now - growth.born;
            if (!(entity instanceof Player player) || age > Limbs.GROW_TICKS + HOLD_TICKS
                    || age > Limbs.GROW_TICKS && !Body.missing(player, growth.limb)) {
                it.remove();
                continue;
            }
            float t = (float) age / Limbs.GROW_TICKS;
            double x = entity.getX(), y = entity.getY() + entity.getBbHeight() * 0.6, z = entity.getZ();
            if (growth.cues < REGEN_AT.length && t >= REGEN_AT[growth.cues]) {
                level.playLocalSound(x, y, z, ModSounds.LIMB_REGEN.get(), SoundSource.PLAYERS, 0.9F,
                        REGEN_PITCH[growth.cues] + (RANDOM.nextFloat() - 0.5F) * 0.1F, false);
                growth.cues++;
            }
            if (t >= MUSCLE_FROM && t < MUSCLE_UNTIL && age % DRIP_EVERY == 0) {
                Gore.drip(entity.getId(), 1);
            }
        }
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (player.isInvisible()) {
            return;
        }
        PlayerModel<AbstractClientPlayer> model = getParentModel();
        Growth growth = GROWING.get(player.getId());
        if (growth == null) {
            return;
        }
        ModelPart part = switch (growth.limb) {
            case Limbs.RIGHT_ARM -> model.rightArm;
            case Limbs.LEFT_ARM -> model.leftArm;
            case Limbs.RIGHT_LEG -> model.rightLeg;
            default -> model.leftLeg;
        };
        grow(poseStack, buffers, light, LivingEntityRenderer.getOverlayCoords(player, 0F), player, part,
                partialTick);
    }

    public static void grow(PoseStack poseStack, MultiBufferSource buffers, int light, int overlay,
                            AbstractClientPlayer player, ModelPart part, float partialTick) {
        Growth growth = GROWING.get(player.getId());
        ClientLevel level = Minecraft.getInstance().level;
        if (growth == null || level == null || !Body.missing(player, growth.limb)) {
            return;
        }
        float t = Mth.clamp((level.getGameTime() - growth.born + partialTick) / Limbs.GROW_TICKS, 0F, 1F);
        if (JojohaClientConfig.GORE.get() != GoreLevel.OFF) {
            ModelPart.Cube cube = part.getRandomCube(RANDOM);
            poseStack.pushPose();
            part.translateAndRotate(poseStack);
            float length = cube.maxY - cube.minY;
            float bone = (length - 1F) * ease(t / BONE_UNTIL);
            float swell = ease((t - MUSCLE_FROM) / (MUSCLE_UNTIL - MUSCLE_FROM));
            if (bone > 0F) {
                bone(buffers.getBuffer(RenderType.entityCutoutNoCull(BONE)), poseStack.last(), cube,
                        cube.minY, cube.minY + bone, light, overlay);
            }
            if (swell > 0F) {
                float x = (cube.minX + cube.maxX) / 2F, z = (cube.minZ + cube.maxZ) / 2F;
                float halfX = Mth.lerp(swell, BONE_STRIP / 2F + 0.25F, (cube.maxX - cube.minX) / 2F - INSET);
                float halfZ = Mth.lerp(swell, BONE_STRIP / 2F + 0.25F, (cube.maxZ - cube.minZ) / 2F - INSET);
                box(buffers.getBuffer(RenderType.entityCutoutNoCull(FLESH)), poseStack.last(),
                        x - halfX, cube.minY, z - halfZ, x + halfX, cube.minY + length * swell, z + halfZ,
                        FLESH_STRIP, light, overlay);
            }
            poseStack.popPose();
        }
        if (t > SKIN_FROM) {
            int[] uv = SKIN_UV[slot(growth.limb)];
            ResourceLocation texture = player.getSkin().texture();
            poseStack.pushPose();
            part.translateAndRotate(poseStack);
            patches(buffers.getBuffer(RenderType.entityCutoutNoCull(texture)), poseStack.last(),
                    part.getRandomCube(RANDOM), 0F, uv[0], uv[1], t, 0F, growth.born, light, overlay);
            if (player.isModelPartShown(OUTER[slot(growth.limb)])) {
                patches(buffers.getBuffer(RenderType.entityTranslucent(texture)), poseStack.last(),
                        part.getRandomCube(RANDOM), SLEEVE_GROW, uv[2], uv[3], t, SLEEVE_LAG, growth.born + 1,
                        light, overlay);
            }
            poseStack.popPose();
        }
    }

    private static int slot(int limb) {
        return switch (limb) {
            case Limbs.RIGHT_ARM -> 0;
            case Limbs.LEFT_ARM -> 1;
            case Limbs.RIGHT_LEG -> 2;
            default -> 3;
        };
    }

    private static void patches(VertexConsumer out, PoseStack.Pose pose, ModelPart.Cube cube, float grow,
                                int texU, int texV, float t, float lag, long seed, int light, int overlay) {
        float x0 = cube.minX - grow, y0 = cube.minY - grow, z0 = cube.minZ - grow;
        float x1 = cube.maxX + grow, y1 = cube.maxY + grow, z1 = cube.maxZ + grow;
        float[][] corners = {{x0, y0, z0}, {x1, y0, z0}, {x1, y1, z0}, {x0, y1, z0}, {x0, y0, z1}, {x1, y0, z1},
                {x1, y1, z1}, {x0, y1, z1}};
        float w = Math.round(cube.maxX - cube.minX), h = Math.round(cube.maxY - cube.minY);
        float d = Math.round(cube.maxZ - cube.minZ);
        float u0 = texU, u1 = texU + d, u2 = u1 + w, u3 = u2 + w, u4 = u2 + d, u5 = u4 + w;
        float v0 = texV, v1 = texV + d, v2 = v1 + h;
        float[][] uvs = {{u1, v0, u2, v1}, {u2, v1, u3, v0}, {u0, v1, u1, v2}, {u1, v1, u2, v2}, {u2, v1, u4, v2},
                {u4, v1, u5, v2}};
        float start = SKIN_FROM + lag;
        float span = 1F - start - PATCH_POP;
        Matrix4f m = pose.pose();
        for (int face = 0; face < FACES.length; face++) {
            float[] a = corners[FACES[face][0]], b = corners[FACES[face][1]], c = corners[FACES[face][2]];
            float[] n = NORMALS[face], uv = uvs[face];
            int across = Math.max(1, Mth.ceil(Math.abs(uv[2] - uv[0]) / TILE));
            int down = Math.max(1, Mth.ceil(Math.abs(uv[3] - uv[1]) / TILE));
            for (int i = 0; i < across; i++) {
                for (int j = 0; j < down; j++) {
                    float sa = (float) i / across, sb = (float) (i + 1) / across;
                    float ra = (float) j / down, rb = (float) (j + 1) / down;
                    float sm = (sa + sb) / 2F, rm = (ra + rb) / 2F;
                    float midY = b[1] + sm * (a[1] - b[1]) + rm * (c[1] - b[1]);
                    float along = Mth.clamp((midY - y0) / (y1 - y0), 0F, 1F);
                    float due = start + span * (PATCH_ALONG * along + (1F - PATCH_ALONG) * hash(seed, face, i, j));
                    float shown = ease((t - due) / PATCH_POP);
                    if (shown <= 0F) {
                        continue;
                    }
                    float size = Mth.lerp(shown, PATCH_START_SIZE, 1F);
                    float lift = (1F - shown) * PATCH_LIFT;
                    float[][] at = {{sb, ra}, {sa, ra}, {sa, rb}, {sb, rb}};
                    for (float[] st : at) {
                        float s = sm + (st[0] - sm) * size, r = rm + (st[1] - rm) * size;
                        float x = b[0] + s * (a[0] - b[0]) + r * (c[0] - b[0]) + n[0] * lift;
                        float y = b[1] + s * (a[1] - b[1]) + r * (c[1] - b[1]) + n[1] * lift;
                        float z = b[2] + s * (a[2] - b[2]) + r * (c[2] - b[2]) + n[2] * lift;
                        float u = (uv[0] + st[0] * (uv[2] - uv[0])) / SKIN_SHEET;
                        float v = (uv[1] + st[1] * (uv[3] - uv[1])) / SKIN_SHEET;
                        corner(out, m, pose, x, y, z, u, v, n[0], n[1], n[2], light, overlay);
                    }
                }
            }
        }
    }

    private static float hash(long seed, int face, int i, int j) {
        int mixed = (int) seed * 31 + face;
        mixed = mixed * 31 + i;
        mixed = mixed * 31 + j;
        mixed ^= mixed >>> 16;
        mixed *= 0x45D9F3B;
        mixed ^= mixed >>> 16;
        return (mixed & 1023) / 1023F;
    }

    public static void cutEnd(PoseStack poseStack, MultiBufferSource buffers, ModelPart part, int light) {
        if (JojohaClientConfig.GORE.get() == GoreLevel.OFF) {
            return;
        }
        ModelPart.Cube cube = part.getRandomCube(RANDOM);
        if (cube.maxX - cube.minX <= 2F) {
            return;
        }
        float face = cube.minY - INSET;
        box(buffers.getBuffer(RenderType.entityCutoutNoCull(FLESH)), poseStack.last(),
                cube.minX, face, cube.minZ, cube.maxX, face, cube.maxZ, FLESH_STRIP, light,
                OverlayTexture.NO_OVERLAY);
        bone(buffers.getBuffer(RenderType.entityCutoutNoCull(BONE)), poseStack.last(), cube,
                cube.minY - BONE_OUT, cube.minY, light, OverlayTexture.NO_OVERLAY);
    }

    private static void bone(VertexConsumer out, PoseStack.Pose pose, ModelPart.Cube cube, float from, float to,
                             int light, int overlay) {
        float wide = Math.round(cube.maxX - cube.minX) % 2 == 0 ? BONE_STRIP : 1F;
        float deep = Math.round(cube.maxZ - cube.minZ) % 2 == 0 ? BONE_STRIP : 1F;
        float x = (cube.minX + cube.maxX) / 2F, z = (cube.minZ + cube.maxZ) / 2F;
        box(out, pose, x - wide / 2F, from, z - deep / 2F, x + wide / 2F, to, z + deep / 2F, BONE_STRIP,
                light, overlay);
    }

    private static void box(VertexConsumer out, PoseStack.Pose pose, float x0, float y0, float z0,
                            float x1, float y1, float z1, float strip, int light, int overlay) {
        float length = Math.min(y1 - y0, SIDE_LENGTH);
        float sideV = length / SHEET;
        float capU = strip / SHEET, capV0 = SIDE_LENGTH / SHEET, capV1 = (SIDE_LENGTH + strip) / SHEET;
        Matrix4f m = pose.pose();
        if (x1 > x0 && z1 > z0) {
            quad(out, m, pose, 0, -1, 0, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1,
                    0F, capV0, capU, capV1, light, overlay);
            quad(out, m, pose, 0, 1, 0, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1,
                    0F, capV0, capU, capV1, light, overlay);
        }
        if (y1 <= y0) {
            return;
        }
        float[][] sides = {{x0, z0, x1, z0, 0, -1}, {x1, z0, x1, z1, 1, 0}, {x1, z1, x0, z1, 0, 1},
                {x0, z1, x0, z0, -1, 0}};
        for (int i = 0; i < sides.length; i++) {
            float[] s = sides[i];
            float u0 = i * strip / SHEET, u1 = (i + 1) * strip / SHEET;
            quad(out, m, pose, s[4], 0, s[5], s[0], y0, s[1], s[2], y0, s[3], s[2], y1, s[3], s[0], y1, s[1],
                    u0, 0F, u1, sideV, light, overlay);
        }
    }

    private static void quad(VertexConsumer out, Matrix4f m, PoseStack.Pose pose, float nx, float ny, float nz,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz,
                             float u0, float v0, float u1, float v1, int light, int overlay) {
        corner(out, m, pose, ax, ay, az, u0, v0, nx, ny, nz, light, overlay);
        corner(out, m, pose, bx, by, bz, u1, v0, nx, ny, nz, light, overlay);
        corner(out, m, pose, cx, cy, cz, u1, v1, nx, ny, nz, light, overlay);
        corner(out, m, pose, dx, dy, dz, u0, v1, nx, ny, nz, light, overlay);
    }

    private static void corner(VertexConsumer out, Matrix4f m, PoseStack.Pose pose, float x, float y, float z,
                               float u, float v, float nx, float ny, float nz, int light, int overlay) {
        out.addVertex(m, x * PX, y * PX, z * PX)
                .setColor(0xFFFFFFFF)
                .setUv(u, v)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
    }

    private static float ease(float t) {
        float clamped = Mth.clamp(t, 0F, 1F);
        return 1F - (1F - clamped) * (1F - clamped);
    }
}
