package org.gumel.jojoha.client;

import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEventListener;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.ClientPlayerDataCache;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.fortune.Cursed;
import org.gumel.jojoha.fortune.Fortune;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class BlindSense {
    private static final int SCENT_RADIUS = 10;
    private static final double MOVING_NOSE = 2.0;
    private static final int SNIFF_TICKS = 24;
    private static final int SCAN_EVERY = 5;
    private static final int SNIFF_SOUND_EVERY = 30;
    private static final int SCENT_MEMORY = 100;
    private static final int SCENT_COLOUR = 0xE0703A;
    private static final double MOB_NOSE = 2.4;
    private static final double PLAYER_NOSE = 4.8;
    private static final double MOB_LEAST = 6.0;
    private static final double PLAYER_LEAST = 12.0;
    private static final int EMIT_EVERY = 4;
    private static final int BODY_EVERY = 2;
    private static final int BODY_LIFE = 16;
    private static final float BODY_PUFF = 0.32F;
    private static final int PLAYER_TRAIL_TICKS = 200;
    private static final int MOB_TRAIL_TICKS = 80;
    private static final float TRAIL_PUFF = 0.2F;
    private static final int PUFFS_MOST = 1400;
    private static final int BURST_LIFE = 18;
    private static final float BURST_PUFF = 0.24F;
    private static final float BURST_SPEED = 0.05F;
    private static final double HEARING_RANGE = 24.0;
    private static final int HEARING_MEMORY = 60;
    private static final double WAVE_SPEED = 0.8;
    private static final int ECHO_RADIUS = 3;
    private static final int ECHO_LEAST = 2;
    private static final float LOUDEST = 2.5F;
    private static final int HEARTBEAT_EVERY = 24;
    private static final double STILL = 1.0E-4;
    private static final int MARK_TICKS = 60;
    private static final double MARK_RANGE = 48.0;
    private static final int MARK_COLOUR = 0xFF2A2A;
    private static final float MARK_FLARE = 1.6F;
    private static final double AIM_COS = 0.995;
    private static final float AIM_FLARE = 1.35F;
    private static final double LIP = 0.002;
    private static final int STRIDE = 7;
    private static final float PIXEL = 1F / 16F;
    private static final int DOT_GAP = 2;
    private static final float DOT_ALPHA = 0.9F;
    private static final double SPARSE_FROM = 18.0;
    private static final float SPARSEST = 0.65F;
    private static final float CREST_TICKS = 3F;
    private static final double TOWARD_EYE = 0.03;
    private static final int GRAIN = 140;
    private static final float HURT_BELOW = 0.4F;
    private static final int BEAT_FAST = 12;
    private static final int BEAT_SLOW = 28;
    private static final int BEAT_EDGE = 24;

    private static final ResourceLocation[] SMOKE = frames("fx/smoke", 1, 5);

    private record Edge(int x, int y, int z) {
    }

    private static final class Seen {
        final float x0;
        final float y0;
        final float z0;
        final float x1;
        final float y1;
        final float z1;
        final int seed;
        int colour;
        float strength;
        int memory;
        boolean scent;
        long at;
        long before = Long.MIN_VALUE;

        Seen(float[] edges, int i, int seed) {
            x0 = edges[i];
            y0 = edges[i + 1];
            z0 = edges[i + 2];
            x1 = edges[i + 3];
            y1 = edges[i + 4];
            z1 = edges[i + 5];
            this.seed = seed;
        }
    }

    private record Puff(Vec3 at, Vec3 drift, long born, int life, int colour, float size, float peak, int seed,
                        int owner) {
    }

    private static final Map<Edge, Seen> SEEN = new HashMap<>();
    private static final List<Puff> PUFFS = new ArrayList<>();
    private static final SoundEventListener EAR = (sound, events, range) -> heard(sound);
    private static double nose = MOVING_NOSE;
    private static long clock;
    private static int sniffing;
    private static int lastSense;
    private static boolean listening;
    private static int beat = BEAT_SLOW;
    private static long lastBeat;
    private static int lastHurt;
    private static int marked = -1;
    private static long markedUntil;
    private static int aimed = -1;

    private BlindSense() {
    }

    private static ResourceLocation[] frames(String prefix, int first, int count) {
        ResourceLocation[] textures = new ResourceLocation[count];
        for (int i = 0; i < count; i++) {
            textures[i] = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID,
                    "textures/" + prefix + (first + i) + ".png");
        }
        return textures;
    }

    public static int sense() {
        JojohaPlayerData data = ClientPlayerDataCache.data;
        if (!Fortune.has(data, Fortune.CURSED) || (data.curses & Cursed.BLINDED) == 0 || Cursed.ghost(data)) {
            return 0;
        }
        return data.curses & (Cursed.SCENT | Cursed.HEARING);
    }

    public static boolean active() {
        return lastSense != 0;
    }

    public static void clear() {
        SEEN.clear();
        PUFFS.clear();
        nose = MOVING_NOSE;
        sniffing = 0;
        marked = -1;
        aimed = -1;
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (!listening) {
            minecraft.getSoundManager().addListener(EAR);
            listening = true;
        }
        int sense = level == null || minecraft.player == null ? 0 : sense();
        if (sense != lastSense) {
            lastSense = sense;
            clear();
        }
        if (sense == 0) {
            return;
        }
        LocalPlayer player = minecraft.player;
        clock++;
        SEEN.values().removeIf(seen -> seen.at <= clock && clock - seen.at > seen.memory);
        PUFFS.removeIf(puff -> clock - puff.born > puff.life);
        heartbeat(level, player);
        mark(level, player);
        if (sense == Cursed.SCENT) {
            sniff(level, player);
        }
        double mobs = sense == Cursed.SCENT ? Math.max(MOB_LEAST, nose * MOB_NOSE) : HEARING_RANGE;
        double people = sense == Cursed.SCENT ? Math.max(PLAYER_LEAST, nose * PLAYER_NOSE) : HEARING_RANGE;
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1F);
        double sharpest = AIM_COS;
        aimed = -1;
        for (LivingEntity body : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(people),
                entity -> entity != player && entity.isAlive() && !entity.isSpectator())) {
            double reach = body instanceof Player ? people : mobs;
            if (body.distanceToSqr(player) > reach * reach) {
                continue;
            }
            double facing = body.getBoundingBox().getCenter().subtract(eye).normalize().dot(look);
            if (facing > sharpest) {
                sharpest = facing;
                aimed = body.getId();
            }
            if (body.getId() == marked) {
                continue;
            }
            if (sense == Cursed.SCENT) {
                smell(level.random, body);
            } else {
                listen(level.random, body);
            }
        }
        while (PUFFS.size() > PUFFS_MOST) {
            PUFFS.remove(0);
        }
    }

    private static void mark(ClientLevel level, LocalPlayer player) {
        if (player.hurtTime > lastHurt) {
            DamageSource source = player.getLastDamageSource();
            if (source != null && source.getEntity() instanceof LivingEntity attacker && attacker != player) {
                marked = attacker.getId();
                markedUntil = clock + MARK_TICKS;
                burst(level.random, attacker.getBoundingBox().getCenter(), 10, 1.6F, MARK_COLOUR, marked);
            }
        }
        lastHurt = player.hurtTime;
        if (marked == -1) {
            return;
        }
        if (clock > markedUntil || !(level.getEntity(marked) instanceof LivingEntity body) || !body.isAlive()
                || body.distanceToSqr(player) > MARK_RANGE * MARK_RANGE) {
            marked = -1;
            return;
        }
        if (clock % BODY_EVERY == 0) {
            cloud(level.random, body, MARK_COLOUR);
        }
    }

    private static void sniff(ClientLevel level, LocalPlayer player) {
        double dx = player.getX() - player.xo;
        double dz = player.getZ() - player.zo;
        boolean still = player.isShiftKeyDown() || dx * dx + dz * dz < STILL;
        if (still) {
            nose = Math.min(SCENT_RADIUS, nose + (double) SCENT_RADIUS / SNIFF_TICKS);
            if (sniffing++ % SNIFF_SOUND_EVERY == 0) {
                level.playLocalSound(player.getX(), player.getEyeY(), player.getZ(), SoundEvents.FOX_SNIFF,
                        SoundSource.PLAYERS, 0.35F, 1.3F, false);
            }
        } else {
            nose = Math.max(MOVING_NOSE, nose - 1.0);
            sniffing = 0;
        }
        if (clock % SCAN_EVERY != 0) {
            return;
        }
        Vec3 feet = player.position();
        float[] edges = edges(level, player.blockPosition(), (int) Math.ceil(nose), null);
        for (int i = 0; i < edges.length; i += STRIDE) {
            double d = Math.sqrt(midDistanceSqr(edges, i, feet));
            if (d <= nose) {
                remember(edges, i, clock, (float) (1.0 - d / (SCENT_RADIUS + 2.0)), SCENT_COLOUR, SCENT_MEMORY, true);
            }
        }
    }

    private static double midDistanceSqr(float[] edges, int i, Vec3 to) {
        double mx = (edges[i] + edges[i + 3]) * 0.5 - to.x;
        double my = (edges[i + 1] + edges[i + 4]) * 0.5 - to.y;
        double mz = (edges[i + 2] + edges[i + 5]) * 0.5 - to.z;
        return mx * mx + my * my + mz * mz;
    }

    private static void remember(float[] edges, int i, long at, float strength, int colour, int memory,
                                 boolean scent) {
        Edge key = new Edge(Math.round((edges[i] + edges[i + 3]) * 8F), Math.round((edges[i + 1] + edges[i + 4]) * 8F),
                Math.round((edges[i + 2] + edges[i + 5]) * 8F));
        Seen seen = SEEN.get(key);
        if (seen == null) {
            seen = new Seen(edges, i, key.hashCode() * 31 + 7);
            SEEN.put(key, seen);
        } else if (seen.at <= clock) {
            seen.before = seen.at;
        }
        seen.at = at;
        seen.strength = strength;
        seen.colour = colour;
        seen.memory = memory;
        seen.scent = scent;
    }

    private static void smell(RandomSource random, LivingEntity body) {
        if ((clock + body.getId()) % BODY_EVERY == 0) {
            cloud(random, body, scentOf(body));
        }
        if ((clock + body.getId()) % EMIT_EVERY != 0) {
            return;
        }
        boolean person = body instanceof Player;
        double width = body.getBbWidth();
        Vec3 at = body.position().add((random.nextDouble() - 0.5) * width, 0.1 + random.nextDouble() * 0.5,
                (random.nextDouble() - 0.5) * width);
        PUFFS.add(new Puff(at, new Vec3(0, 0.002, 0), clock, person ? PLAYER_TRAIL_TICKS : MOB_TRAIL_TICKS,
                scentOf(body), TRAIL_PUFF, 0.55F, random.nextInt(), -1));
    }

    private static void cloud(RandomSource random, LivingEntity body, int colour) {
        AABB box = body.getBoundingBox();
        float size = BODY_PUFF * Mth.clamp(body.getBbWidth() / 0.6F, 0.6F, 2F);
        for (int i = 0; i < 2; i++) {
            Vec3 at = new Vec3(Mth.lerp(0.15 + random.nextDouble() * 0.7, box.minX, box.maxX),
                    Mth.lerp(random.nextDouble(), box.minY, box.maxY),
                    Mth.lerp(0.15 + random.nextDouble() * 0.7, box.minZ, box.maxZ));
            Vec3 drift = new Vec3((random.nextFloat() - 0.5F) * 0.012F, 0.006F + random.nextFloat() * 0.01F,
                    (random.nextFloat() - 0.5F) * 0.012F);
            PUFFS.add(new Puff(at, drift, clock, BODY_LIFE, colour, size, 0.9F, random.nextInt(), body.getId()));
        }
    }

    private static void burst(RandomSource random, Vec3 at, int count, float loud, int colour, int owner) {
        for (int i = 0; i < count; i++) {
            float yaw = random.nextFloat() * Mth.TWO_PI;
            float speed = BURST_SPEED * loud * (0.5F + random.nextFloat() * 0.5F);
            Vec3 drift = new Vec3(Mth.cos(yaw) * speed, (0.2F + random.nextFloat() * 0.5F) * speed,
                    Mth.sin(yaw) * speed);
            PUFFS.add(new Puff(at, drift, clock, BURST_LIFE, colour, BURST_PUFF * Mth.sqrt(loud), 0.85F,
                    random.nextInt(), owner));
        }
    }

    private static void listen(RandomSource random, LivingEntity body) {
        int colour = colourOf(body.getSoundSource());
        if ((clock + body.getId() * 7L) % HEARTBEAT_EVERY == 0) {
            burst(random, body.position().add(0, body.getBbHeight() * 0.6, 0), 4, 0.6F, colour, body.getId());
        }
        double dx = body.getX() - body.xo;
        double dz = body.getZ() - body.zo;
        if ((clock + body.getId()) % EMIT_EVERY == 0 && dx * dx + dz * dz > STILL && body.onGround()
                && !body.isSteppingCarefully() && !body.isShiftKeyDown()) {
            burst(random, body.position().add(0, 0.1, 0), 3, 0.8F, colour, body.getId());
        }
    }

    private static int scentOf(LivingEntity body) {
        if (body instanceof Player person) {
            float hue = (person.getUUID().hashCode() & 0xFFFF) / 65536F;
            return Mth.hsvToRgb(hue, 0.55F, 1F);
        }
        if (body instanceof Enemy) {
            return 0xD94A3A;
        }
        return body instanceof Animal ? 0x8FD06A : 0xD9B48A;
    }

    private static void heard(SoundInstance sound) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || lastSense != Cursed.HEARING
                || sound.isRelative() || sound.getAttenuation() == SoundInstance.Attenuation.NONE) {
            return;
        }
        SoundSource source = sound.getSource();
        if (source == SoundSource.MUSIC || source == SoundSource.AMBIENT || source == SoundSource.WEATHER) {
            return;
        }
        Vec3 at = new Vec3(sound.getX(), sound.getY(), sound.getZ());
        if (at.distanceToSqr(minecraft.player.position()) > HEARING_RANGE * HEARING_RANGE) {
            return;
        }
        float loud = Mth.clamp(sound.getVolume(), 0.3F, LOUDEST);
        int radius = Math.max(ECHO_LEAST, Math.round(ECHO_RADIUS * loud));
        int colour = colourOf(source);
        float[] edges = edges(minecraft.level, BlockPos.containing(at), radius, at);
        for (int i = 0; i < edges.length; i += STRIDE) {
            double d = Math.sqrt(midDistanceSqr(edges, i, at));
            remember(edges, i, clock + Math.round(d / WAVE_SPEED), edges[i + 6], colour, HEARING_MEMORY, false);
        }
        burst(minecraft.level.random, at, 3 + Math.round(3F * loud), loud, colour, -1);
    }

    private static int colourOf(SoundSource source) {
        return switch (source) {
            case HOSTILE -> 0xFF5A4A;
            case PLAYERS -> 0xFFD36A;
            case NEUTRAL -> 0x9BE38A;
            default -> 0x9EC8FF;
        };
    }

    private static void heartbeat(ClientLevel level, LocalPlayer player) {
        float health = player.getHealth() / Math.max(1F, player.getMaxHealth());
        if (health >= HURT_BELOW) {
            beat = BEAT_SLOW;
            return;
        }
        beat = Math.round(Mth.lerp(health / HURT_BELOW, BEAT_FAST, BEAT_SLOW));
        if (clock - lastBeat >= beat) {
            lastBeat = clock;
            level.playLocalSound(player.getX(), player.getEyeY(), player.getZ(), SoundEvents.WARDEN_HEARTBEAT,
                    SoundSource.PLAYERS, 0.5F, 1.2F, false);
        }
    }

    private static boolean full(ClientLevel level, BlockPos pos) {
        return level.getBlockState(pos).isCollisionShapeFullBlock(level, pos);
    }

    private static float[] edges(ClientLevel level, BlockPos centre, int radius, Vec3 source) {
        List<Float> out = new ArrayList<>();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos near = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dy * dy + dz * dz > radius * radius) {
                        continue;
                    }
                    pos.set(centre.getX() + dx, centre.getY() + dy, centre.getZ() + dz);
                    BlockState state = level.getBlockState(pos);
                    if (state.isAir()) {
                        continue;
                    }
                    double weight = source == null ? 1.0
                            : 1.0 - Math.sqrt(dx * dx + dy * dy + dz * dz) / (radius + 1.0);
                    if (state.isCollisionShapeFullBlock(level, pos)) {
                        cube(level, pos, near, out, (float) weight);
                    } else {
                        VoxelShape shape = state.getCollisionShape(level, pos);
                        if (!shape.isEmpty()) {
                            int x = pos.getX();
                            int y = pos.getY();
                            int z = pos.getZ();
                            shape.forAllEdges((x0, y0, z0, x1, y1, z1) -> add(out, x + x0, y + y0, z + z0,
                                    x + x1, y + y1, z + z1, (float) weight));
                        }
                    }
                }
            }
        }
        float[] packed = new float[out.size()];
        for (int i = 0; i < packed.length; i++) {
            packed[i] = out.get(i);
        }
        return packed;
    }

    private static void cube(ClientLevel level, BlockPos pos, BlockPos.MutableBlockPos near, List<Float> out,
                             float weight) {
        for (Direction face : Direction.values()) {
            if (full(level, near.setWithOffset(pos, face))) {
                continue;
            }
            for (Direction side : Direction.values()) {
                if (side.getAxis() == face.getAxis()) {
                    continue;
                }
                boolean flat = full(level, near.setWithOffset(pos, side))
                        && !full(level, near.move(face));
                if (flat) {
                    continue;
                }
                double cx = pos.getX() + 0.5 + 0.5 * face.getStepX() + 0.5 * side.getStepX();
                double cy = pos.getY() + 0.5 + 0.5 * face.getStepY() + 0.5 * side.getStepY();
                double cz = pos.getZ() + 0.5 + 0.5 * face.getStepZ() + 0.5 * side.getStepZ();
                int ax = face.getStepY() * side.getStepZ() - face.getStepZ() * side.getStepY();
                int ay = face.getStepZ() * side.getStepX() - face.getStepX() * side.getStepZ();
                int az = face.getStepX() * side.getStepY() - face.getStepY() * side.getStepX();
                double lx = cx + LIP * (face.getStepX() + side.getStepX());
                double ly = cy + LIP * (face.getStepY() + side.getStepY());
                double lz = cz + LIP * (face.getStepZ() + side.getStepZ());
                add(out, lx - 0.5 * ax, ly - 0.5 * ay, lz - 0.5 * az, lx + 0.5 * ax, ly + 0.5 * ay,
                        lz + 0.5 * az, weight);
            }
        }
    }

    private static void add(List<Float> out, double x0, double y0, double z0, double x1, double y1, double z1,
                            float weight) {
        out.add((float) x0);
        out.add((float) y0);
        out.add((float) z0);
        out.add((float) x1);
        out.add((float) y1);
        out.add((float) z1);
        out.add(weight);
    }

    public static void post(Matrix4f modelView, Matrix4f projection, Camera camera, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (lastSense == 0 || minecraft.player == null) {
            return;
        }
        minecraft.getMainRenderTarget().bindWrite(false);
        RenderSystem.clearColor(0F, 0F, 0F, 1F);
        RenderSystem.clear(GlConst.GL_COLOR_BUFFER_BIT, Minecraft.ON_OSX);

        Matrix4fStack stack = RenderSystem.getModelViewStack();
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(new Matrix4f(projection), VertexSorting.DISTANCE_TO_ORIGIN);
        stack.pushMatrix();
        stack.mul(modelView);
        RenderSystem.applyModelViewMatrix();
        float fogStart = RenderSystem.getShaderFogStart();
        FogRenderer.setupNoFog();

        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        draw(new PoseStack().last().pose(), buffers, camera, minecraft.player.getPosition(partialTick), partialTick);
        buffers.endBatch();

        RenderSystem.setShaderFogStart(fogStart);
        stack.popMatrix();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.restoreProjectionMatrix();
    }

    private static void draw(Matrix4f pose, MultiBufferSource buffers, Camera camera, Vec3 feet, float partialTick) {
        Vec3 eye = camera.getPosition();
        Vector3f leftF = camera.getLeftVector();
        Vector3f upF = camera.getUpVector();
        Vec3 right = new Vec3(-leftF.x(), -leftF.y(), -leftF.z());
        Vec3 up = new Vec3(upF.x(), upF.y(), upF.z());
        float now = clock + partialTick;

        VertexConsumer dots = buffers.getBuffer(RenderType.debugQuads());
        Vec3 dotRight = right.scale(PIXEL * 0.5);
        Vec3 dotUp = up.scale(PIXEL * 0.5);
        for (Seen seen : SEEN.values()) {
            float age = now - seen.at;
            if (age < 0F) {
                if (seen.before == Long.MIN_VALUE) {
                    continue;
                }
                age = now - seen.before;
            }
            if (age >= seen.memory) {
                continue;
            }
            float fade = 1F - age / seen.memory;
            fade *= Mth.sqrt(fade);
            float crest = age < CREST_TICKS ? 1F + 0.8F * (1F - age / CREST_TICKS) : 1F;
            float alpha = DOT_ALPHA * seen.strength * fade * crest;
            if (alpha <= 0.02F) {
                continue;
            }
            dotted(dots, pose, seen, eye, feet, now, age, fade, alpha, dotRight, dotUp);
        }

        for (Puff puff : PUFFS) {
            float age = now - puff.born;
            float t = age / puff.life;
            if (age < 0F || t >= 1F) {
                continue;
            }
            float flare = puff.owner == -1 ? 1F : puff.owner == marked ? MARK_FLARE
                    : puff.owner == aimed ? AIM_FLARE : 1F;
            float travel = age * (1F - t * 0.5F);
            Vec3 at = puff.at.add(puff.drift.scale(travel)).add(Mth.sin(puff.seed + age * 0.2F) * 0.03, 0,
                    Mth.cos(puff.seed * 1.3F + age * 0.17F) * 0.03);
            float size = puff.size * (0.6F + 0.9F * t) * (0.8F + 0.2F * flare);
            float left = 1F - t;
            float alpha = Mth.clamp(puff.peak * flare * Math.min(1F, 0.3F + age / 2F) * left * Mth.sqrt(left),
                    0F, 1F);
            int colour = flare > 1F && puff.owner == aimed ? whiter(puff.colour, 0.45F) : puff.colour;
            int frame = Math.min(SMOKE.length - 1, (int) (t * SMOKE.length));
            Vec3 r = right.scale(size);
            Vec3 u = up.scale(size);
            switch (puff.seed & 3) {
                case 1 -> {
                    Vec3 turned = u;
                    u = r.scale(-1);
                    r = turned;
                }
                case 2 -> {
                    r = r.scale(-1);
                    u = u.scale(-1);
                }
                case 3 -> {
                    Vec3 turned = u.scale(-1);
                    u = r;
                    r = turned;
                }
                default -> {
                }
            }
            sprite(buffers, pose, SMOKE[frame], at.subtract(eye), r, u, (int) (alpha * 255F) << 24 | colour);
        }
    }

    private static int whiter(int colour, float by) {
        int r = colour >> 16 & 0xFF;
        int g = colour >> 8 & 0xFF;
        int b = colour & 0xFF;
        return (int) Mth.lerp(by, r, 255) << 16 | (int) Mth.lerp(by, g, 255) << 8 | (int) Mth.lerp(by, b, 255);
    }

    private static void dotted(VertexConsumer out, Matrix4f pose, Seen seen, Vec3 eye, Vec3 feet, float now,
                               float age, float fade, float alpha, Vec3 dotRight, Vec3 dotUp) {
        float dx = seen.x1 - seen.x0;
        float dy = seen.y1 - seen.y0;
        float dz = seen.z1 - seen.z0;
        int pixels = Math.max(1, Math.round(Mth.sqrt(dx * dx + dy * dy + dz * dz) / PIXEL));
        double mx = (seen.x0 + seen.x1) * 0.5 - feet.x;
        double my = (seen.y0 + seen.y1) * 0.5 - feet.y;
        double mz = (seen.z0 + seen.z1) * 0.5 - feet.z;
        float far = (float) Mth.clamp(Math.sqrt(mx * mx + my * my + mz * mz) / SPARSE_FROM, 0.0, SPARSEST);
        float keep = 1F - far - (1F - fade) * 0.4F;
        int argb = (int) (Mth.clamp(alpha, 0F, 1F) * 255F) << 24 | seen.colour;
        for (int p = seen.seed & 1; p < pixels; p += DOT_GAP) {
            int hash = Mth.murmurHash3Mixer(seen.seed + p * 0x9E3779B9);
            if ((hash & 0xFF) / 255F > keep) {
                continue;
            }
            float t = (p + 0.5F) / pixels;
            double x = seen.x0 + dx * t;
            double y = seen.y0 + dy * t;
            double z = seen.z0 + dz * t;
            float spin = (hash >>> 8 & 0xFF) / 255F * Mth.TWO_PI;
            if (seen.scent) {
                x += Mth.cos(now * 0.12F + spin) * PIXEL * 0.6F;
                y += age * 0.002F + Mth.sin(now * 0.15F + spin) * PIXEL * 0.6F;
                z += Mth.sin(now * 0.1F + spin) * PIXEL * 0.6F;
            } else if (age < CREST_TICKS * 2F) {
                float shake = (1F - age / (CREST_TICKS * 2F)) * PIXEL;
                x += Mth.cos(spin) * shake;
                y += Mth.sin(spin * 1.7F) * shake;
                z += Mth.sin(spin) * shake;
            }
            Vec3 at = new Vec3(x - eye.x, y - eye.y, z - eye.z);
            at = at.subtract(at.normalize().scale(TOWARD_EYE));
            Vec3 bl = at.subtract(dotRight).subtract(dotUp);
            Vec3 br = at.add(dotRight).subtract(dotUp);
            Vec3 tr = at.add(dotRight).add(dotUp);
            Vec3 tl = at.subtract(dotRight).add(dotUp);
            out.addVertex(pose, (float) bl.x, (float) bl.y, (float) bl.z).setColor(argb);
            out.addVertex(pose, (float) br.x, (float) br.y, (float) br.z).setColor(argb);
            out.addVertex(pose, (float) tr.x, (float) tr.y, (float) tr.z).setColor(argb);
            out.addVertex(pose, (float) tl.x, (float) tl.y, (float) tl.z).setColor(argb);
        }
    }

    private static void sprite(MultiBufferSource buffers, Matrix4f pose, ResourceLocation texture, Vec3 centre,
                               Vec3 right, Vec3 up, int argb) {
        VertexConsumer out = buffers.getBuffer(RenderType.text(texture));
        Vec3 bl = centre.subtract(right).subtract(up);
        Vec3 br = centre.add(right).subtract(up);
        Vec3 tr = centre.add(right).add(up);
        Vec3 tl = centre.subtract(right).add(up);
        corner(out, pose, bl, argb, 0F, 1F);
        corner(out, pose, br, argb, 1F, 1F);
        corner(out, pose, tr, argb, 1F, 0F);
        corner(out, pose, tl, argb, 0F, 0F);
        corner(out, pose, tl, argb, 0F, 0F);
        corner(out, pose, tr, argb, 1F, 0F);
        corner(out, pose, br, argb, 1F, 1F);
        corner(out, pose, bl, argb, 0F, 1F);
    }

    private static void corner(VertexConsumer out, Matrix4f pose, Vec3 at, int argb, float u, float v) {
        out.addVertex(pose, (float) at.x, (float) at.y, (float) at.z).setColor(argb).setUv(u, v)
                .setLight(LightTexture.FULL_BRIGHT);
    }

    public static void hud(GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (lastSense == 0 || minecraft.player == null) {
            return;
        }
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        RandomSource grain = RandomSource.create(clock * 31L + System.currentTimeMillis() / 40L);
        for (int i = 0; i < GRAIN; i++) {
            int x = grain.nextInt(width);
            int y = grain.nextInt(height);
            int shade = 0x30 + grain.nextInt(0x30);
            graphics.fill(x, y, x + 1, y + 1, (0x10 + grain.nextInt(0x18)) << 24 | shade << 16 | shade << 8 | shade);
        }
        float health = minecraft.player.getHealth() / Math.max(1F, minecraft.player.getMaxHealth());
        if (health >= HURT_BELOW) {
            return;
        }
        float phase = (clock - lastBeat + minecraft.getTimer().getGameTimeDeltaPartialTick(false)) / beat;
        float pulse = phase < 0.15F ? 1F - phase / 0.15F
                : phase > 0.25F && phase < 0.4F ? 0.6F * (1F - (phase - 0.25F) / 0.15F) : 0F;
        if (pulse <= 0F) {
            return;
        }
        float strength = pulse * (0.35F + 0.65F * (1F - health / HURT_BELOW));
        for (int i = 0; i < BEAT_EDGE; i++) {
            int alpha = (int) (0x70 * strength * (1F - (float) i / BEAT_EDGE));
            if (alpha <= 0) {
                continue;
            }
            int colour = alpha << 24 | 0x8A0F0F;
            graphics.fill(i, i, width - i, i + 1, colour);
            graphics.fill(i, height - i - 1, width - i, height - i, colour);
            graphics.fill(i, i + 1, i + 1, height - i - 1, colour);
            graphics.fill(width - i - 1, i + 1, width - i, height - i - 1, colour);
        }
    }
}
