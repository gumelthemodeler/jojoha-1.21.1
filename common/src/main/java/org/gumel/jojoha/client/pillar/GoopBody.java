package org.gumel.jojoha.client.pillar;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.client.Gore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class GoopBody {
    private static final float PX = PixelDraw.PIXEL;
    private static final int HEAD_PX = 7;
    private static final int LIMBS = 4;
    private static final double SNAP_DISTANCE = 4.0;
    private static final double DAMPING = 0.62;
    private static final int DRIP_EVERY = 3;
    private static final int DRIP_TICKS = 40;
    private static final int SQUISH_EVERY = 7;
    private static final int SINK_BITS = 26;
    private static final int TENDRIL_SEGMENTS = 14;

    private static final int[] HEAD = {8, 8, 8, 8};
    private static final int[] TORSO = {20, 20, 8, 12};
    private static final int[] ARM = {44, 20, 4, 12};
    private static final int[] LEG = {4, 20, 4, 12};

    private static final Map<Integer, Blob> BLOBS = new HashMap<>();
    private static final List<Bit> BITS = new ArrayList<>();
    private static final List<Tendril> TENDRILS = new ArrayList<>();
    private static final Map<Integer, Inhabit> INHABITED = new HashMap<>();

    private GoopBody() {
    }

    public static boolean hides(Entity entity) {
        return BLOBS.containsKey(entity.getId()) || Gore.reforming(entity);
    }

    public static void clear() {
        BLOBS.clear();
        BITS.clear();
        TENDRILS.clear();
        INHABITED.clear();
    }

    private static ResourceLocation skinOf(Entity entity) {
        return entity instanceof AbstractClientPlayer player ? player.getSkin().texture()
                : ResourceLocation.withDefaultNamespace("textures/entity/player/wide/steve.png");
    }

    private static float[] patch(RandomSource random, int[] region, int size) {
        int w = Math.min(size, region[2]);
        int h = Math.min(size, region[3]);
        int u = region[0] + random.nextInt(region[2] - w + 1);
        int v = region[1] + random.nextInt(region[3] - h + 1);
        return PixelDraw.skin(u, v, w, h);
    }

    public static void reachIn(ClientLevel level, int playerId, int hostId, int ticks) {
        Entity player = level.getEntity(playerId);
        if (player == null) {
            return;
        }
        TENDRILS.add(new Tendril(playerId, hostId, Math.max(2, ticks), skinOf(player), level.random));
    }

    public static void sink(ClientLevel level, int playerId, int hostId, Vec3 at, float yaw, int inside) {
        Entity player = level.getEntity(playerId);
        Entity host = level.getEntity(hostId);
        if (player == null || host == null) {
            return;
        }
        ResourceLocation skin = skinOf(player);
        RandomSource random = level.random;
        for (int i = 0; i < SINK_BITS; i++) {
            Vec3 from = at.add(standing(random, yaw, i));
            Bit bit = Bit.flow(skin, patchFor(random, i), from, hostId, Vec3.ZERO, i / 3, 6 + random.nextInt(4),
                    (2 + random.nextInt(3)) * PX, true);
            BITS.add(bit);
        }
        INHABITED.put(hostId, new Inhabit(playerId, skin, inside));
        level.playLocalSound(at.x, at.y, at.z, SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 1.0F, 0.6F, false);
        level.playLocalSound(at.x, at.y, at.z, SoundEvents.HONEY_BLOCK_SLIDE, SoundSource.PLAYERS, 1.0F, 0.5F, false);
    }

    public static void burst(ClientLevel level, int playerId, int hostId) {
        INHABITED.remove(hostId);
        Entity player = level.getEntity(playerId);
        Entity host = level.getEntity(hostId);
        if (player == null || host == null) {
            return;
        }
        ResourceLocation skin = skinOf(player);
        RandomSource random = level.random;
        Vec3 from = host.getBoundingBox().getCenter();
        for (int i = 0; i < SINK_BITS; i++) {
            Vec3 to = standing(random, player.getYRot(), i);
            Bit bit = Bit.flow(skin, patchFor(random, i), from, playerId, to, random.nextInt(3), 4 + random.nextInt(3),
                    (2 + random.nextInt(3)) * PX, false);
            BITS.add(bit);
        }
    }

    private static int[] regionFor(int index) {
        return switch (index % 6) {
            case 0 -> HEAD;
            case 1, 2 -> TORSO;
            case 3 -> ARM;
            default -> LEG;
        };
    }

    private static float[] patchFor(RandomSource random, int index) {
        return patch(random, regionFor(index), 3);
    }

    private static Vec3 standing(RandomSource random, float yaw, int index) {
        double x;
        double y;
        double z = (random.nextDouble() - 0.5) * 0.25;
        switch (index % 6) {
            case 0 -> {
                x = (random.nextDouble() - 0.5) * 0.45;
                y = 1.5 + random.nextDouble() * 0.4;
            }
            case 1, 2 -> {
                x = (random.nextDouble() - 0.5) * 0.45;
                y = 0.8 + random.nextDouble() * 0.7;
            }
            case 3 -> {
                x = (random.nextBoolean() ? 1 : -1) * (0.3 + random.nextDouble() * 0.1);
                y = 0.8 + random.nextDouble() * 0.65;
            }
            default -> {
                x = (random.nextBoolean() ? 1 : -1) * (0.05 + random.nextDouble() * 0.12);
                y = random.nextDouble() * 0.75;
            }
        }
        return rotate(new Vec3(x, y, z), yaw);
    }

    private static Vec3 rotate(Vec3 local, float yaw) {
        double r = Math.toRadians(yaw);
        double cos = Math.cos(r);
        double sin = Math.sin(r);
        return new Vec3(local.x * cos - local.z * sin, local.y, local.x * sin + local.z * cos);
    }

    public static void tick(ClientLevel level) {
        Iterator<Map.Entry<Integer, Blob>> blobs = BLOBS.entrySet().iterator();
        while (blobs.hasNext()) {
            Map.Entry<Integer, Blob> entry = blobs.next();
            Entity entity = level.getEntity(entry.getKey());
            if (!(entity instanceof Player player) || !PillarClient.on(player.getId(), PillarClient.CONTORT)
                    || !player.isAlive()) {
                blobs.remove();
            }
        }
        for (Player player : level.players()) {
            if (PillarClient.on(player.getId(), PillarClient.CONTORT) && player.isAlive() && !player.isSpectator()) {
                BLOBS.computeIfAbsent(player.getId(), id -> new Blob(player)).update(level, player);
            }
        }

        BITS.removeIf(bit -> !bit.tick(level));
        TENDRILS.removeIf(tendril -> ++tendril.age > tendril.ticks + 2);

        Iterator<Map.Entry<Integer, Inhabit>> hosts = INHABITED.entrySet().iterator();
        while (hosts.hasNext()) {
            Map.Entry<Integer, Inhabit> entry = hosts.next();
            Inhabit inhabit = entry.getValue();
            Entity host = level.getEntity(entry.getKey());
            if (host == null || !host.isAlive() || ++inhabit.age > inhabit.ticks + 20) {
                hosts.remove();
                continue;
            }
            if (level.random.nextFloat() < 0.45F) {
                AABB box = host.getBoundingBox();
                RandomSource random = level.random;
                double angle = random.nextDouble() * Math.PI * 2;
                double rx = box.getXsize() * 0.5;
                double rz = box.getZsize() * 0.5;
                Vec3 offset = new Vec3(Math.cos(angle) * rx, box.getYsize() * (0.25 + random.nextDouble() * 0.6),
                        Math.sin(angle) * rz);
                BITS.add(Bit.bulge(inhabit.skin, patch(random, TORSO, 3), entry.getKey(), offset,
                        (2 + random.nextInt(3)) * PX));
            }
            if (inhabit.age % 12 == 0) {
                level.playLocalSound(host.getX(), host.getY(), host.getZ(), SoundEvents.SLIME_SQUISH_SMALL,
                        SoundSource.PLAYERS, 0.5F, 0.5F + level.random.nextFloat() * 0.2F, false);
            }
        }
    }

    public static void render(PoseStack poseStack, MultiBufferSource buffers, Camera camera, float partialTick,
                              ClientLevel level) {
        if (BLOBS.isEmpty() && BITS.isEmpty() && TENDRILS.isEmpty()) {
            return;
        }
        Vec3 eye = camera.getPosition();
        for (Map.Entry<Integer, Blob> entry : BLOBS.entrySet()) {
            Entity entity = level.getEntity(entry.getKey());
            if (entity == null || entity == camera.getEntity() && !camera.isDetached()) {
                continue;
            }
            entry.getValue().render(poseStack, buffers, eye, partialTick, level, entity);
        }
        for (Bit bit : BITS) {
            bit.render(poseStack, buffers, eye, partialTick, level);
        }
        for (Tendril tendril : TENDRILS) {
            tendril.render(poseStack, buffers, eye, partialTick, level);
        }
    }

    private static int light(ClientLevel level, Vec3 at) {
        return LevelRenderer.getLightColor(level, BlockPos.containing(at));
    }

    private static void cube(PoseStack poseStack, VertexConsumer out, Vec3 at, float yaw, float pitch, float sx,
                             float sy, float sz, float[] uv, int light) {
        poseStack.pushPose();
        poseStack.translate(at.x, at.y, at.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        if (pitch != 0F) {
            poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        }
        PixelDraw.box(out, poseStack.last(), sx, sy, sz, uv, 0xFFFFFFFF, light);
        poseStack.popPose();
    }

    private static Vec3 lerp(Vec3 from, Vec3 to, float t) {
        return new Vec3(Mth.lerp(t, from.x, to.x), Mth.lerp(t, from.y, to.y), Mth.lerp(t, from.z, to.z));
    }

    private static final class Gob {
        final Vec3 rest;
        final float size;
        final float[] uv;
        final float phase;
        final double stiffness;
        final int limb;
        Vec3 pos = Vec3.ZERO;
        Vec3 prev = Vec3.ZERO;
        Vec3 velocity = Vec3.ZERO;
        float swing;
        float swingO;

        Gob(Vec3 rest, float size, float[] uv, float phase, double stiffness, int limb) {
            this.rest = rest;
            this.size = size;
            this.uv = uv;
            this.phase = phase;
            this.stiffness = stiffness;
            this.limb = limb;
        }
    }

    private static final class Blob {
        final List<Gob> gobs = new ArrayList<>();
        final Gob head;
        final ResourceLocation skin;
        final float[][] headFaces;
        Vec3 lastBase;
        double stretch;
        double squeeze;
        double low;
        double flat;
        double walk;
        boolean wasOnGround = true;
        int age;
        float yaw;
        float yawO;

        Blob(Player player) {
            this.skin = skinOf(player);
            RandomSource random = RandomSource.create(player.getUUID().getLeastSignificantBits());
            for (int i = 0; i < 12; i++) {
                double angle = random.nextDouble() * Math.PI * 2;
                double r = Math.sqrt(random.nextDouble());
                gobs.add(new Gob(new Vec3(Math.cos(angle) * r * 0.42, 0.1 + random.nextDouble() * 0.08,
                        Math.sin(angle) * r * 0.55), (4 + random.nextInt(3)) * PX,
                        patch(random, i % 3 == 0 ? LEG : TORSO, 4), random.nextFloat() * 6.28F,
                        0.22 + random.nextDouble() * 0.12, -1));
            }
            for (int i = 0; i < 8; i++) {
                double angle = random.nextDouble() * Math.PI * 2;
                double r = Math.sqrt(random.nextDouble());
                gobs.add(new Gob(new Vec3(Math.cos(angle) * r * 0.3, 0.24 + random.nextDouble() * 0.1,
                        Math.sin(angle) * r * 0.4 - 0.05), (3 + random.nextInt(3)) * PX,
                        patch(random, i % 2 == 0 ? TORSO : ARM, 4), random.nextFloat() * 6.28F,
                        0.18 + random.nextDouble() * 0.1, -1));
            }
            for (int i = 0; i < LIMBS; i++) {
                boolean left = i % 2 == 0;
                boolean front = i < 2;
                gobs.add(new Gob(new Vec3(left ? 0.36 : -0.36, 0.14, front ? 0.22 : -0.22), 2 * PX,
                        patch(random, front ? ARM : LEG, 2), i * 1.57F, 0.4, i));
            }
            this.head = new Gob(new Vec3(0, 0.36, 0.28), HEAD_PX * PX, PixelDraw.skin(8, 8, 8, 8), 0F, 0.3, -1);
            gobs.add(head);
            this.headFaces = new float[][]{
                    PixelDraw.skin(8, 8, 8, 8), PixelDraw.skin(24, 8, 8, 8), PixelDraw.skin(0, 8, 8, 8),
                    PixelDraw.skin(16, 8, 8, 8), PixelDraw.skin(8, 0, 8, 8), PixelDraw.skin(16, 0, 8, 8)};
            float[] front = headFaces[0];
            headFaces[0] = new float[]{front[2], front[1], front[0], front[3]};
        }

        void update(ClientLevel level, Player player) {
            age++;
            Vec3 base = player.position();
            boolean snap = lastBase == null || lastBase.distanceToSqr(base) > SNAP_DISTANCE * SNAP_DISTANCE;
            Vec3 moved = snap ? Vec3.ZERO : base.subtract(lastBase);
            lastBase = base;
            double speed = Math.sqrt(moved.x * moved.x + moved.z * moved.z);
            yawO = yaw;
            yaw = speed > 0.01 ? (float) Math.toDegrees(Math.atan2(-moved.x, moved.z)) : player.yBodyRot;
            if (snap) {
                yawO = yaw;
            } else {
                yaw = yawO + Mth.wrapDegrees(yaw - yawO) * 0.4F;
            }

            stretch += (Math.min(speed * 5.0, 1.2) - stretch) * 0.3;
            squeeze += ((narrow(level, player) ? 1.0 : 0.0) - squeeze) * 0.35;
            low += ((ceilingLow(level, player) ? 1.0 : 0.0) - low) * 0.3;
            flat += ((underSlab(level, player) ? 1.0 : 0.0) - flat) * 0.4;
            walk += speed * 9.0;

            boolean landed = player.onGround() && !wasOnGround;
            wasOnGround = player.onGround();

            double along = 1 + stretch * 0.9 + squeeze * 0.45;
            double lateral = (1 / (1 + stretch * 0.5)) * (1 - squeeze * 0.6) * (1 + flat * 0.3);
            double height = (1 / (1 + stretch * 0.4)) * (1 - low * 0.3) * (1 + squeeze * 0.2) * (1 - flat * 0.45);
            double stride = Math.min(speed * 12.0, 1.0);

            for (Gob gob : gobs) {
                Vec3 local = new Vec3(gob.rest.x * lateral, gob.rest.y * height, gob.rest.z * along);
                local = local.add(0, Math.sin((age + gob.phase) * 0.35) * 0.012, -stretch * 0.08 * (1 - gob.stiffness));
                gob.swingO = gob.swing;
                if (gob.limb >= 0) {
                    gob.swing = (float) (Math.sin(walk + gob.limb * Math.PI * 0.5) * 35 * stride);
                    local = local.add(0, Math.max(0, Math.sin(walk + gob.limb * Math.PI * 0.5)) * 0.06 * stride, 0);
                }
                Vec3 target = base.add(rotate(local, yaw));
                if (snap) {
                    gob.pos = target;
                    gob.prev = target;
                    gob.velocity = Vec3.ZERO;
                    continue;
                }
                gob.prev = gob.pos;
                gob.velocity = gob.velocity.scale(DAMPING).add(target.subtract(gob.pos).scale(gob.stiffness));
                if (landed) {
                    Vec3 out = new Vec3(gob.pos.x - base.x, 0, gob.pos.z - base.z);
                    gob.velocity = gob.velocity.add(out.scale(0.25)).add(0, -0.05 * level.random.nextDouble(), 0);
                }
                gob.pos = gob.pos.add(gob.velocity);
            }

            if (speed > 0.02 && age % DRIP_EVERY == 0) {
                Gob tail = gobs.get(level.random.nextInt(12));
                Vec3 at = new Vec3(tail.pos.x, base.y + PX, tail.pos.z);
                BITS.add(Bit.drip(skin, tail.uv, at, (1 + level.random.nextInt(2)) * PX));
            }
            if (speed > 0.03 && age % SQUISH_EVERY == 0) {
                level.playLocalSound(base.x, base.y, base.z, SoundEvents.SLIME_SQUISH_SMALL, SoundSource.PLAYERS,
                        0.3F, 0.6F + level.random.nextFloat() * 0.2F, false);
            }
            if ((squeeze > 0.5 || flat > 0.5) && age % 9 == 0) {
                level.playLocalSound(base.x, base.y, base.z, SoundEvents.BONE_BLOCK_HIT, SoundSource.PLAYERS,
                        0.5F, 1.3F + level.random.nextFloat() * 0.3F, false);
            }
        }

        void render(PoseStack poseStack, MultiBufferSource buffers, Vec3 eye, float partialTick, ClientLevel level,
                    Entity entity) {
            VertexConsumer out = buffers.getBuffer(RenderType.entityCutoutNoCull(skin));
            int light = light(level, entity.position().add(0, 0.3, 0));
            float shownYaw = Mth.rotLerp(partialTick, yawO, yaw);
            float time = age + partialTick;
            for (Gob gob : gobs) {
                Vec3 at = lerp(gob.prev, gob.pos, partialTick).subtract(eye);
                float pulse = 1F + 0.09F * Mth.sin(time * 0.5F + gob.phase);
                if (gob == head) {
                    poseStack.pushPose();
                    poseStack.translate(at.x, at.y, at.z);
                    poseStack.mulPose(Axis.YP.rotationDegrees(-shownYaw));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.sin(time * 0.2F) * 25F + 160F));
                    float s = gob.size;
                    PixelDraw.box(out, poseStack.last(), s, s * pulse * 0.9F, s, headFaces[0], headFaces[1],
                            headFaces[2], headFaces[3], headFaces[4], headFaces[5], 0xFFFFFFFF, light);
                    poseStack.popPose();
                } else if (gob.limb >= 0) {
                    float swing = Mth.lerp(partialTick, gob.swingO, gob.swing);
                    boolean left = gob.limb % 2 == 0;
                    poseStack.pushPose();
                    poseStack.translate(at.x, at.y, at.z);
                    poseStack.mulPose(Axis.YP.rotationDegrees(-shownYaw + (left ? 70F : -70F) + swing));
                    poseStack.mulPose(Axis.XP.rotationDegrees(-25F));
                    PixelDraw.box(out, poseStack.last(), gob.size, gob.size, 6 * PX, gob.uv, 0xFFFFFFFF, light);
                    poseStack.popPose();
                } else {
                    float s = gob.size * pulse;
                    cube(poseStack, out, at, shownYaw, 0F, s, gob.size * (2F - pulse), s, gob.uv, light);
                }
            }
        }

        private static boolean narrow(ClientLevel level, Player player) {
            AABB box = player.getBoundingBox().inflate(0.12, 0.0, 0.12);
            for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY, box.minZ),
                    BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {
                BlockState state = level.getBlockState(pos);
                if (state.getBlock() instanceof IronBarsBlock || state.getBlock() instanceof FenceBlock) {
                    return true;
                }
            }
            float yaw = player.yBodyRot * Mth.DEG_TO_RAD;
            Vec3 side = new Vec3(Math.cos(yaw), 0, Math.sin(yaw)).scale(0.8);
            Vec3 feet = player.position().add(0, 0.2, 0);
            return solid(level, feet.add(side)) && solid(level, feet.subtract(side));
        }

        private static boolean underSlab(ClientLevel level, Player player) {
            AABB box = player.getBoundingBox();
            AABB above = new AABB(box.minX, box.minY + 0.46, box.minZ, box.maxX, box.minY + 0.95, box.maxZ);
            return level.getBlockCollisions(player, above).iterator().hasNext();
        }

        private static boolean ceilingLow(ClientLevel level, Player player) {
            return solid(level, player.position().add(0, 1.1, 0));
        }

        private static boolean solid(ClientLevel level, Vec3 at) {
            BlockPos pos = BlockPos.containing(at);
            return !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
        }
    }

    private static final class Bit {
        enum Mode { DRIP, FLOW, BULGE }

        final Mode mode;
        final ResourceLocation skin;
        final float[] uv;
        final float size;
        final int life;
        final int delay;
        final int entity;
        final Vec3 offset;
        final boolean into;
        final float spin;
        Vec3 from;
        int age;

        private Bit(Mode mode, ResourceLocation skin, float[] uv, float size, int life, int delay, int entity,
                    Vec3 from, Vec3 offset, boolean into, float spin) {
            this.mode = mode;
            this.skin = skin;
            this.uv = uv;
            this.size = size;
            this.life = life;
            this.delay = delay;
            this.entity = entity;
            this.from = from;
            this.offset = offset;
            this.into = into;
            this.spin = spin;
        }

        static Bit drip(ResourceLocation skin, float[] uv, Vec3 at, float size) {
            return new Bit(Mode.DRIP, skin, uv, size, DRIP_TICKS, 0, -1, at, Vec3.ZERO, false, 0F);
        }

        static Bit flow(ResourceLocation skin, float[] uv, Vec3 from, int entity, Vec3 offset, int delay, int ticks,
                        float size, boolean into) {
            return new Bit(Mode.FLOW, skin, uv, size, ticks, delay, entity, from, offset, into,
                    (float) (Math.random() * 360));
        }

        static Bit bulge(ResourceLocation skin, float[] uv, int entity, Vec3 offset, float size) {
            return new Bit(Mode.BULGE, skin, uv, size, 8, 0, entity, Vec3.ZERO, offset, false, 0F);
        }

        boolean tick(ClientLevel level) {
            age++;
            if (mode != Mode.DRIP && level.getEntity(entity) == null) {
                return false;
            }
            return age <= delay + life;
        }

        void render(PoseStack poseStack, MultiBufferSource buffers, Vec3 eye, float partialTick, ClientLevel level) {
            float t = age - delay + partialTick;
            if (t < 0) {
                return;
            }
            float progress = Mth.clamp(t / life, 0F, 1F);
            Vec3 at;
            float scale;
            switch (mode) {
                case DRIP -> {
                    at = from;
                    scale = progress < 0.5F ? 1F : (1F - progress) * 2F;
                }
                case BULGE -> {
                    Entity host = level.getEntity(entity);
                    if (host == null) {
                        return;
                    }
                    Vec3 base = host.getPosition(partialTick);
                    at = base.add(offset);
                    scale = Mth.sin(progress * Mth.PI);
                }
                default -> {
                    Entity target = level.getEntity(entity);
                    if (target == null) {
                        return;
                    }
                    Vec3 end = into ? target.getBoundingBox().getCenter() : target.getPosition(partialTick).add(offset);
                    float eased = progress * progress * (3 - 2 * progress);
                    at = lerp(from, end, eased).add(0, Mth.sin(progress * Mth.PI) * 0.35, 0);
                    scale = into ? 1F - progress * 0.8F : (progress < 0.85F ? 1F : (1F - progress) / 0.15F);
                }
            }
            if (scale <= 0.02F) {
                return;
            }
            VertexConsumer out = buffers.getBuffer(RenderType.entityCutoutNoCull(skin));
            float s = size * scale;
            float squash = mode == Mode.DRIP ? 0.4F : 1F;
            Vec3 shown = at.subtract(eye).add(0, mode == Mode.DRIP ? s * squash * 0.5 : 0, 0);
            cube(poseStack, out, shown, spin + t * (mode == Mode.FLOW ? 30F : 0F), 0F, s, s * squash, s, uv,
                    light(level, at));
        }
    }

    private static final class Tendril {
        final int from;
        final int to;
        final int ticks;
        final ResourceLocation skin;
        final float[][] patches = new float[TENDRIL_SEGMENTS][];
        int age;

        Tendril(int from, int to, int ticks, ResourceLocation skin, RandomSource random) {
            this.from = from;
            this.to = to;
            this.ticks = ticks;
            this.skin = skin;
            for (int i = 0; i < TENDRIL_SEGMENTS; i++) {
                patches[i] = patch(random, ARM, 3);
            }
        }

        void render(PoseStack poseStack, MultiBufferSource buffers, Vec3 eye, float partialTick, ClientLevel level) {
            Entity source = level.getEntity(from);
            Entity target = level.getEntity(to);
            if (source == null || target == null) {
                return;
            }
            Vec3 hand = PillarClient.hand(source, partialTick);
            Vec3 end = target.getBoundingBox().getCenter();
            float reach = Mth.clamp((age + partialTick) / ticks, 0F, 1F);
            Vec3[] basis = PixelDraw.basis(end.subtract(hand));
            VertexConsumer out = buffers.getBuffer(RenderType.entityCutoutNoCull(skin));
            int light = light(level, hand);
            int shown = Math.max(1, Math.round(TENDRIL_SEGMENTS * reach));
            float time = age + partialTick;
            for (int i = 0; i < shown; i++) {
                float along = i / (float) (TENDRIL_SEGMENTS - 1);
                Vec3 at = lerp(hand, end, along * reach);
                double wave = Math.sin(time * 0.9 + i * 0.8) * 0.06 * Math.sin(along * Math.PI);
                at = at.add(basis[1].scale(wave)).add(basis[2].scale(Math.cos(time * 0.7 + i) * 0.04));
                float s = (3.5F - along * 1.5F) * PX;
                cube(poseStack, out, at.subtract(eye), time * 12F + i * 40F, 0F, s, s, s, patches[i], light);
            }
        }
    }

    private static final class Inhabit {
        final int player;
        final ResourceLocation skin;
        final int ticks;
        int age;

        Inhabit(int player, ResourceLocation skin, int ticks) {
            this.player = player;
            this.skin = skin;
            this.ticks = ticks;
        }
    }
}
