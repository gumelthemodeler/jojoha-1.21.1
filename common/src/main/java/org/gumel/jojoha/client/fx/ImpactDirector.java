package org.gumel.jojoha.client.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.client.BarrageFx;
import org.gumel.jojoha.client.CombatShake;
import org.gumel.jojoha.client.ComboCounter;
import org.gumel.jojoha.client.Gore;
import org.gumel.jojoha.client.ImpactFrame;
import org.gumel.jojoha.client.ImpactWords;
import org.gumel.jojoha.client.JojohaClientConfig;
import org.gumel.jojoha.client.PaletteSwap;
import org.gumel.jojoha.client.ScreenLanguage;
import org.gumel.jojoha.combat.fx.ImpactTier;
import org.gumel.jojoha.combat.fx.MovementFx;
import org.gumel.jojoha.combat.fx.Palette;
import org.gumel.jojoha.network.packet.ImpactCuePacket;
import org.gumel.jojoha.network.packet.MovementCuePacket;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ImpactDirector {
    public static final long FRAME_MS = 50L;

    private static final int MAX_BURSTS = 64;

    private static final double DRAW_RANGE = 64.0;

    private static final double FULL_FEEL = 6.0;
    private static final double FEEL_RANGE = 24.0;

    private static final double PUNCH_RANGE = 10.0;

    private static final float RUMBLE_SHARE = 0.5F;

    private static final long KICK_IN_MS = 35L;
    private static final long KICK_OUT_MS = 260L;
    private static final float KICK_CEILING = 7F;

    private static final long PUNCH_IN_MS = 30L;
    private static final long PUNCH_OUT_MS = 220L;

    private static final float MIN_FOV_SCALE = 0.88F;

    private static final int WHITE = 0xFFFFFFFF;

    private static final double LIFT_TOWARD_VIEWER = 0.6;

    private static final ArrayDeque<Burst> BURSTS = new ArrayDeque<>();
    private static final List<Kick> KICKS = new ArrayList<>();
    private static final List<Kick> PUNCHES = new ArrayList<>();

    private record Burst(ImpactCuePacket cue, ImpactTier tier, long start, Vec3 at, Vec3 dir) {
    }

    private record Kick(Vec3 dir, float amount, long start) {
    }

    private ImpactDirector() {
    }

    public static void cue(ImpactCuePacket cue) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        ImpactTier tier = ImpactTier.byId(cue.tier());
        Vec3 at = new Vec3(cue.x(), cue.y(), cue.z());
        Vec3 dir = new Vec3(cue.dx(), cue.dy(), cue.dz());
        long now = Util.getMillis();

        BURSTS.addLast(new Burst(cue, tier, now, at, dir));
        if (tier == ImpactTier.FINISHER) {
            BarrageFx.hush(minecraft.level.getGameTime());
        }
        ComboCounter.landed(tier, cue.strikerId());
        ImpactWords.landed(tier, at, cue.colour(), cue.seed());
        while (BURSTS.size() > MAX_BURSTS) {
            BURSTS.removeFirst();
        }

        if (cue.targetId() >= 0 && tier.hitStopMs() > 0) {
            Entity target = minecraft.level.getEntity(cue.targetId());
            if (target != null) {
                HitStop.begin(target, tier.hitStopMs(), dir);
            }
        }
        if (tier == ImpactTier.HEAVY || tier == ImpactTier.FINISHER || tier == ImpactTier.GUARD_BREAK) {
            Entity striker = cue.strikerId() >= 0 ? minecraft.level.getEntity(cue.strikerId()) : null;
            Entity target = cue.targetId() >= 0 ? minecraft.level.getEntity(cue.targetId()) : null;
            if (striker != null || target != null) {
                HitStop.strike(striker, target, tier.hitStopMs());
            }
        }

        double distance = Math.sqrt(minecraft.player.distanceToSqr(at));
        if (JojohaClientConfig.cameraMotion()) {
            CombatShake.add(cue.x(), cue.y(), cue.z(),
                    cue.shake() * (tier == ImpactTier.PARRY ? 1F : RUMBLE_SHARE));

            float felt = felt(distance);
            if (felt > 0F && tier.kickDegrees() > 0F) {
                KICKS.add(new Kick(dir, tier.kickDegrees() * felt, now));
            }
            if (distance <= PUNCH_RANGE && tier.fovPunch() > 0F) {
                PUNCHES.add(new Kick(dir, tier.fovPunch() * felt, now));
            }
        }

        boolean struck = cue.targetId() == minecraft.player.getId();
        if (JojohaClientConfig.impactFrames()) {
            ScreenLanguage.landed(tier, cue.colour(),
                    struck || cue.strikerId() == minecraft.player.getId(), distance);
        }
        if (tier == ImpactTier.FINISHER && struck) {
            PaletteSwap.beat(Palette.DREAD);
        } else if (tier == ImpactTier.FINISHER && cue.frame() > 0F) {
            PaletteSwap.beat(Palette.IMPACT);
        } else if (tier == ImpactTier.GUARD_BREAK && (struck || distance <= PUNCH_RANGE)) {
            PaletteSwap.beat(Palette.BREAK);
        }
        Gore.landed(cue, tier, at, dir);

        if (cue.frame() > 0F && JojohaClientConfig.impactFrames()) {
            ImpactFrame.begin(cue.frame());
        }
    }

    public static void punch(float amount) {
        if (JojohaClientConfig.cameraMotion()) {
            PUNCHES.add(new Kick(Vec3.ZERO, amount, Util.getMillis()));
        }
    }

    private static float felt(double distance) {
        double near = 1.0 - Math.max(0.0, distance - FULL_FEEL) / (FEEL_RANGE - FULL_FEEL);
        return (float) Mth.clamp(near, 0.0, 1.0);
    }

    private static float envelope(long elapsed, long in, long out) {
        if (elapsed < in) {
            return (float) elapsed / in;
        }
        float left = 1F - (float) (elapsed - in) / out;
        return left <= 0F ? 0F : left * left;
    }

    public static float[] cameraKick() {
        if (KICKS.isEmpty()) {
            return null;
        }
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vector3f look = camera.getLookVector();
        Vector3f left = camera.getLeftVector();
        long now = Util.getMillis();

        float yaw = 0F;
        float pitch = 0F;
        Iterator<Kick> it = KICKS.iterator();
        while (it.hasNext()) {
            Kick kick = it.next();
            long elapsed = now - kick.start;
            if (elapsed < 0L || elapsed > KICK_IN_MS + KICK_OUT_MS) {
                it.remove();
                continue;
            }
            float amount = kick.amount * envelope(elapsed, KICK_IN_MS, KICK_OUT_MS);
            double side = -(kick.dir.x * left.x() + kick.dir.y * left.y() + kick.dir.z * left.z());
            double forward = kick.dir.x * look.x() + kick.dir.y * look.y() + kick.dir.z * look.z();
            yaw += (float) side * amount;
            pitch += (float) (-kick.dir.y * 0.7 + forward * 0.45) * amount;
        }
        return new float[] {squash(yaw), squash(pitch)};
    }

    private static float squash(float degrees) {
        return KICK_CEILING * (float) Math.tanh(degrees / KICK_CEILING);
    }

    public static float fovScale() {
        if (PUNCHES.isEmpty()) {
            return 1F;
        }
        long now = Util.getMillis();
        float punch = 0F;
        Iterator<Kick> it = PUNCHES.iterator();
        while (it.hasNext()) {
            Kick kick = it.next();
            long elapsed = now - kick.start;
            if (elapsed < 0L || elapsed > PUNCH_IN_MS + PUNCH_OUT_MS) {
                it.remove();
                continue;
            }
            punch += kick.amount * envelope(elapsed, PUNCH_IN_MS, PUNCH_OUT_MS);
        }
        return Math.max(MIN_FOV_SCALE, 1F - punch);
    }

    private static int frameOf(Burst burst, long now) {
        long hold = Math.max(burst.tier.hitStopMs(), FRAME_MS);
        long elapsed = now - burst.start;
        return elapsed < hold ? 0 : 1 + (int) ((elapsed - hold) / FRAME_MS);
    }

    public static void render(PoseStack poseStack, MultiBufferSource buffers, Camera camera, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            BURSTS.clear();
            MOVES.clear();
            SPARKS.clear();
            KICKS.clear();
            PUNCHES.clear();
            HitStop.clear();
            return;
        }
        HitStop.prune();
        if (BURSTS.isEmpty() && MOVES.isEmpty()) {
            return;
        }

        long now = Util.getMillis();
        Vec3 eye = camera.getPosition();
        Vector3f leftF = camera.getLeftVector();
        Vector3f upF = camera.getUpVector();
        View view = new View(poseStack.last().pose(), buffers, eye,
                new Vec3(-leftF.x(), -leftF.y(), -leftF.z()), new Vec3(upF.x(), upF.y(), upF.z()),
                new LinkedHashSet<>());

        Iterator<Burst> it = BURSTS.iterator();
        while (it.hasNext()) {
            Burst burst = it.next();
            int frame = frameOf(burst, now);
            if (frame > lastFrame(burst.tier)) {
                it.remove();
                continue;
            }
            if (burst.at.distanceToSqr(eye) > DRAW_RANGE * DRAW_RANGE) {
                continue;
            }
            draw(view, burst, frame);
        }
        if (!SPARKS.isEmpty()) {
            SPARKS.keySet().removeIf(burst -> !BURSTS.contains(burst));
        }
        drawMoves(view, now, partialTick, minecraft);

        for (Burst burst : BURSTS) {
            if (burst.at.distanceToSqr(eye) <= DRAW_RANGE * DRAW_RANGE) {
                hitmarker(view, burst, frameOf(burst, now));
            }
        }

        if (buffers instanceof MultiBufferSource.BufferSource source) {
            for (RenderType type : view.used) {
                source.endBatch(type);
            }
        }
    }

    private static final float MOVE_TEXEL = 1F / 28F;

    private static final int MAX_MOVES = 32;
    private static final int TAKE_OFF_FRAMES = 9;
    private static final int WAKE_MARK_FRAMES = 5;

    private static final ArrayDeque<Move> MOVES = new ArrayDeque<>();

    private static final class Move {
        final MovementCuePacket cue;
        final MovementFx.Kind kind;
        final long start;
        final Vec3 at;
        final Vec3 dir;
        final List<Vec3> wake = new ArrayList<>();
        final List<Long> wakeAt = new ArrayList<>();

        Move(MovementCuePacket cue, long start) {
            this.cue = cue;
            MovementFx.Kind[] kinds = MovementFx.Kind.values();
            this.kind = kinds[Mth.clamp(cue.kind(), 0, kinds.length - 1)];
            this.start = start;
            this.at = new Vec3(cue.x(), cue.y(), cue.z());
            Vec3 d = new Vec3(cue.dx(), cue.dy(), cue.dz());
            this.dir = d.lengthSqr() < 1.0E-6 ? new Vec3(0, 1, 0) : d.normalize();
        }

        long lastsMs() {
            return Math.max(TAKE_OFF_FRAMES * FRAME_MS, (cue.wakeTicks() + WAKE_MARK_FRAMES) * FRAME_MS);
        }
    }

    public static void movement(MovementCuePacket cue) {
        if (Minecraft.getInstance().level == null) {
            return;
        }
        MOVES.addLast(new Move(cue, Util.getMillis()));
        while (MOVES.size() > MAX_MOVES) {
            MOVES.removeFirst();
        }
    }

    private static void drawMoves(View view, long now, float partialTick, Minecraft minecraft) {
        Iterator<Move> it = MOVES.iterator();
        while (it.hasNext()) {
            Move move = it.next();
            long elapsed = now - move.start;
            if (elapsed > move.lastsMs()) {
                it.remove();
                continue;
            }
            if (move.at.distanceToSqr(view.eye) > DRAW_RANGE * DRAW_RANGE) {
                continue;
            }
            if (elapsed < move.cue.wakeTicks() * FRAME_MS && minecraft.level != null
                    && (move.wakeAt.isEmpty() || now - move.wakeAt.get(move.wakeAt.size() - 1) >= FRAME_MS)) {
                Entity mover = minecraft.level.getEntity(move.cue.entityId());
                if (mover != null) {
                    move.wake.add(mover.getPosition(partialTick));
                    move.wakeAt.add(now);
                }
            }
            drawTakeOff(view, move, (int) (elapsed / FRAME_MS));
            drawWake(view, move, now);
        }
    }

    private static void drawTakeOff(View view, Move move, int frame) {
        int colour = 0xFF000000 | move.cue.colour();
        int light = mix(colour, 0.5F);
        int dust = mix(0xFFCFCFCF, colour, 0.15F);
        float px = MOVE_TEXEL;
        Vec3 feet = move.at.add(0, 0.06, 0);
        Vec3 chest = move.at.add(0, 0.9, 0);
        boolean grounded = move.cue.grounded();
        RandomSource rng = RandomSource.create(move.cue.seed() * 31L + 7L);

        switch (move.kind) {
            case DASH -> {
                if (frame < BURST.frames) {
                    billboard(view, BURST, frame, chest.subtract(move.dir.scale(0.4)), 0, 16 * px, 16 * px, light);
                }
                moveStreaks(view, chest, move.dir, frame, 4, rng, light);
                if (grounded) {
                    flatRing(view, RING_SMALL, frame, feet, 0.45F, light);
                    dustBurst(view, feet, move.dir.reverse(), 4, frame, rng, dust);
                }
            }
            case ROLL -> {
                if (grounded) {
                    flatRing(view, RING_SMALL, frame, feet, 0.5F, light);
                    flatRing(view, RING_MEDIUM, frame, feet.add(move.dir.scale(0.6)), 0.6F, mix(light, 0.35F));
                }
                moveStreaks(view, feet.add(0, 0.18, 0), move.dir, frame, 3, rng, light);
            }
            case LEAP -> {
                flatRing(view, RING, frame, feet, 1.3F, colour);
                if (frame < BURST.frames) {
                    billboard(view, BURST, frame, feet.add(0, 0.3, 0), 0, 24 * px, 24 * px, light);
                }
                if (grounded) {
                    dustBurst(view, feet, null, 10, frame, rng, dust);
                }
                moveStreaks(view, chest, move.dir, frame, 5, rng, light);
            }
            case DOUBLE_JUMP, RISE -> {
                flatRing(view, RING_MEDIUM, frame, feet, 0.8F, light);
                if (frame < BURST.frames) {
                    billboard(view, BURST, frame, feet.add(0, -0.15, 0), 0, 16 * px, 16 * px, light);
                }
                moveStreaks(view, feet, new Vec3(0, -1, 0), frame, 4, rng, light);
                if (grounded) {
                    dustBurst(view, feet, null, 6, frame, rng, dust);
                }
            }
        }
    }

    private static void drawWake(View view, Move move, long now) {
        int light = mix(0xFF000000 | move.cue.colour(), 0.5F);
        int dust = mix(0xFFCFCFCF, 0xFF000000 | move.cue.colour(), 0.15F);
        float px = MOVE_TEXEL;
        for (int i = 0; i < move.wake.size(); i++) {
            int age = (int) ((now - move.wakeAt.get(i)) / FRAME_MS);
            if (age >= WAKE_MARK_FRAMES) {
                continue;
            }
            Vec3 at = move.wake.get(i);
            switch (move.kind) {
                case DASH -> {
                    if (i == 0 || age >= STREAK.frames) {
                        continue;
                    }
                    Vec3 from = move.wake.get(i - 1).add(0, 0.9, 0);
                    Vec3 to = at.add(0, 0.9, 0);
                    Vec3 along = to.subtract(from);
                    if (along.lengthSqr() < 1.0E-4) {
                        continue;
                    }
                    Vec3 centre = from.lerp(to, 0.5);
                    Vec3 across = along.cross(view.eye.subtract(centre));
                    if (across.lengthSqr() < 1.0E-6) {
                        continue;
                    }
                    sprite(view, STREAK, age, centre, across.normalize().scale(4 * px), along.scale(0.5), light);
                }
                case ROLL -> flatRing(view, RING_SMALL, age, at.add(0, 0.05, 0), 0.4F, dust);
                default -> {
                    if (age < STREAK.frames) {
                        sprite(view, STREAK, age, at.add(0, -0.2 - 0.1 * age, 0),
                                view.right.scale(4 * px), new Vec3(0, -12 * px, 0), light);
                    }
                }
            }
        }
    }

    private static void flatRing(View view, Flipbook book, int frame, Vec3 centre, float half, int argb) {
        if (frame >= book.frames) {
            return;
        }
        sprite(view, book, frame, centre, new Vec3(half, 0, 0), new Vec3(0, 0, half), argb);
    }

    private static void moveStreaks(View view, Vec3 from, Vec3 dir, int frame, int count, RandomSource rng, int argb) {
        if (frame < 1 || frame > STREAK.frames) {
            return;
        }
        Vec3[] plane = plane(dir);
        for (int i = 0; i < count; i++) {
            float angle = rng.nextFloat() * Mth.TWO_PI;
            float out = 0.2F + rng.nextFloat() * 0.35F;
            Vec3 centre = from.add(plane[0].scale(Mth.cos(angle) * out)).add(plane[1].scale(Mth.sin(angle) * out))
                    .subtract(dir.scale(0.3 + 0.35 * frame));
            Vec3 across = dir.cross(view.eye.subtract(centre));
            if (across.lengthSqr() < 1.0E-6) {
                continue;
            }
            sprite(view, STREAK, frame - 1, centre, across.normalize().scale(4 * MOVE_TEXEL), dir.scale(16 * MOVE_TEXEL), argb);
        }
    }

    private static void dustBurst(View view, Vec3 centre, Vec3 heading, int count, int frame, RandomSource rng, int argb) {
        if (frame >= 7) {
            return;
        }
        for (int i = 0; i < count; i++) {
            double angle;
            if (heading == null) {
                angle = (Mth.TWO_PI * i) / count + rng.nextFloat() * 0.5;
            } else {
                angle = Math.atan2(heading.z, heading.x) + (rng.nextFloat() - 0.5F) * 1.6F;
            }
            double speed = 0.12 + rng.nextFloat() * 0.1;
            float px = MOVE_TEXEL;
            Vec3 at = centre.add(Math.cos(angle) * (0.25 + speed * frame), 0.05 + 0.035 * frame,
                    Math.sin(angle) * (0.25 + speed * frame));
            billboard(view, DUST, Math.min(DUST.frames - 1, frame * DUST.frames / 7), at, 0, 8 * px, 8 * px, argb);
        }
    }

    private static int lastFrame(ImpactTier tier) {
        if (tier == ImpactTier.PARRY) {
            return (int) (PARRY_MS / FRAME_MS);
        }
        return marks(tier) ? Math.max(tier.frames(), HITMARKER_FRAMES - 1) : tier.frames();
    }

    private static boolean marks(ImpactTier tier) {
        return tier != ImpactTier.GUARD_BREAK && tier != ImpactTier.PARRY;
    }

    private static void hitmarker(View view, Burst burst, int frame) {
        if (!marks(burst.tier) || frame < 0 || frame >= HITMARKER_FRAMES) {
            return;
        }
        float px = texel(Math.max(0.6F, burst.cue.size()));
        sprite(view, SPARK, frame, burst.at, view.right.scale(16 * px), view.up.scale(16 * px), WHITE, true);
    }

    private record View(Matrix4f pose, MultiBufferSource buffers, Vec3 eye, Vec3 right, Vec3 up,
                        Set<RenderType> used) {
    }

    private record Flipbook(ResourceLocation[] textures, int frames) {
        static Flipbook strip(String name, int frames) {
            return new Flipbook(new ResourceLocation[] {
                    ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/fx/" + name + ".png")}, frames);
        }

        static Flipbook frames(String prefix, int count) {
            ResourceLocation[] textures = new ResourceLocation[count];
            for (int i = 0; i < count; i++) {
                textures[i] = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID,
                        "textures/fx/" + prefix + (i + 1) + ".png");
            }
            return new Flipbook(textures, count);
        }

        ResourceLocation texture(int frame) {
            return textures.length == 1 ? textures[0] : textures[Mth.clamp(frame, 0, frames - 1)];
        }

        float u0(int frame) {
            return textures.length == 1 ? (float) Mth.clamp(frame, 0, frames - 1) / frames : 0F;
        }

        float u1(int frame) {
            return textures.length == 1 ? (float) (Mth.clamp(frame, 0, frames - 1) + 1) / frames : 1F;
        }
    }

    private static final Flipbook STAR = Flipbook.strip("impact_star", 4);

    private static final Flipbook LINES = Flipbook.strip("impact_lines", 3);

    private static final Flipbook STREAK = Flipbook.strip("speed_streak", 3);

    private static final Flipbook SPARK = Flipbook.frames("hitmarker", 8);

    private static final int HITMARKER_FRAMES = 8;

    private static final Flipbook RING_SMALL = Flipbook.frames("circle_small_", 5);
    private static final Flipbook RING_MEDIUM = Flipbook.frames("circle_medium_", 5);

    private static final Flipbook RING = Flipbook.frames("circle_large_", 5);

    private static final Flipbook BURST = Flipbook.frames("wind_particle", 9);

    private static final Flipbook DUST = Flipbook.frames("smoke", 5);

    private static float texel(float size) {
        return size / 26F;
    }

    private static float ringTexel(float size) {
        return size / 14F;
    }

    private static void draw(View view, Burst burst, int frame) {
        if (burst.tier == ImpactTier.PARRY) {
            parry(view, burst);
            return;
        }
        ImpactCuePacket cue = burst.cue;
        ImpactTier tier = burst.tier;
        int colour = 0xFF000000 | cue.colour();
        int light = mix(colour, 0.5F);
        float size = Math.max(0.6F, cue.size());
        float px = texel(size);
        int frames = tier.frames();

        Vec3 toViewer = view.eye.subtract(burst.at);
        Vec3 anchor = toViewer.lengthSqr() < 1.0E-6
                ? burst.at
                : burst.at.add(toViewer.normalize().scale(Math.min(LIFT_TOWARD_VIEWER, toViewer.length() * 0.5)));

        if (tier == ImpactTier.TAP) {
            taps(view, burst, frame, anchor, px, colour);
        } else if (frame <= 3) {
            boolean hollow = tier == ImpactTier.GUARD_BREAK;
            int starFrame = hollow ? Math.min(3, frame + 1) : frame;
            billboard(view, STAR, starFrame, anchor, 0, 16 * px, 16 * px, colour);
        }

        if (tier != ImpactTier.TAP && frame <= 2) {
            double bx = burst.dir.dot(view.right);
            double by = burst.dir.dot(view.up);
            int quarter = bx * bx + by * by < 0.04 ? 0
                    : Math.floorMod(Math.round((float) Math.atan2(by, bx) / Mth.HALF_PI), 4);
            billboard(view, LINES, frame, lift(view, anchor, 2), quarter, 16 * px, 16 * px, light);
        }

        if (tier.shockwave() && frame >= 1 && frame < frames) {
            float progress = (frame - 1F) / Math.max(1F, frames - 2F);
            int ringFrame = 1 + Math.round(progress * 3F);
            float half = 16 * ringTexel(size);
            if (tier == ImpactTier.GUARD_BREAK || tier == ImpactTier.PARRY) {
                billboard(view, RING, ringFrame, anchor, 0, half, half, colour);
            } else {
                Vec3[] plane = plane(burst.dir);
                sprite(view, RING, ringFrame, burst.at, plane[0].scale(half), plane[1].scale(half), colour);
            }
        }

        if (tier == ImpactTier.FINISHER && frame >= 1) {
            int burstFrame = Math.min(BURST.frames - 1, (frame - 1) * BURST.frames / Math.max(1, frames));
            float half = 16 * ringTexel(size) * 1.15F;
            billboard(view, BURST, burstFrame, anchor, 0, half, half, light);
        }

        if (tier.streaks() && frame >= 1 && frame <= STREAK.frames) {
            streaks(view, burst, frame, size, px, light);
        }

        if (tier.shards() && frame >= 2 && frame <= frames) {
            debris(view, burst, frame, size, px, colour);
        }
    }

    private static void taps(View view, Burst burst, int frame, Vec3 anchor, float px, int colour) {
        RandomSource rng = RandomSource.create(burst.cue.seed() * 17L + 11L);
        float reach = Math.max(0.6F, burst.cue.size()) * 0.5F;
        Vec3 near = anchor.add(view.right.scale((rng.nextFloat() - 0.5F) * reach))
                .add(view.up.scale((rng.nextFloat() - 0.5F) * reach));
        Vec3 far = anchor.add(view.right.scale((rng.nextFloat() - 0.5F) * reach * 2F))
                .add(view.up.scale((rng.nextFloat() - 0.5F) * reach * 2F));

        if (frame > burst.tier.frames()) {
            return;
        }
        billboard(view, RING_SMALL, Math.min(RING_SMALL.frames - 1, 1 + frame), near, 0, 16 * px, 16 * px, colour);
        if (frame >= 1) {
            billboard(view, RING_MEDIUM, Math.min(RING_MEDIUM.frames - 1, frame), far, 0, 32 * px, 32 * px, mix(colour, 0.35F));
        }
    }

    private static void streaks(View view, Burst burst, int frame, float size, float px, int argb) {
        RandomSource rng = RandomSource.create(burst.cue.seed() * 7L + 3L);
        int count = burst.tier == ImpactTier.FINISHER ? 6 : 4;
        Vec3[] plane = plane(burst.dir);

        for (int i = 0; i < count; i++) {
            float angle = rng.nextFloat() * Mth.TWO_PI;
            float out = size * (0.15F + rng.nextFloat() * 0.35F);
            Vec3 lateral = plane[0].scale(Mth.cos(angle) * out).add(plane[1].scale(Mth.sin(angle) * out));
            Vec3 centre = burst.at.add(lateral).add(burst.dir.scale(size * (0.5F + 0.3F * frame)));

            Vec3 across = burst.dir.cross(view.eye.subtract(centre));
            if (across.lengthSqr() < 1.0E-6) {
                continue;
            }
            sprite(view, STREAK, frame - 1, centre, across.normalize().scale(4 * px),
                    burst.dir.scale(16 * px), argb);
        }
    }

    private static void debris(View view, Burst burst, int frame, float size, float px, int colour) {
        RandomSource rng = RandomSource.create(burst.cue.seed() * 13L + 5L);
        int count = switch (burst.tier) {
            case FINISHER -> 7;
            case GUARD_BREAK, PARRY -> 6;
            default -> 4;
        };
        boolean allRound = burst.tier == ImpactTier.GUARD_BREAK || burst.tier == ImpactTier.PARRY;
        float spread = allRound ? Mth.PI : burst.cue.spread() * 1.3F * Mth.DEG_TO_RAD;
        Vec3[] plane = plane(burst.dir);
        int dust = mix(0xFFB8B8B8, colour, 0.25F);
        int frames = burst.tier.frames();

        for (int i = 0; i < count; i++) {
            float tilt = rng.nextFloat() * spread;
            float round = rng.nextFloat() * Mth.TWO_PI;
            Vec3 way = burst.dir.scale(Mth.cos(tilt))
                    .add(plane[0].scale(Mth.sin(tilt) * Mth.cos(round)))
                    .add(plane[1].scale(Mth.sin(tilt) * Mth.sin(round)));
            float speed = size * (0.18F + rng.nextFloat() * 0.22F);
            Vec3 at = burst.at.add(way.scale(size * 0.45F + speed * (frame - 1))).add(0, -0.03 * frame * frame, 0);

            if (i % 3 == 2) {
                if (frame <= 4) {
                    billboard(view, SPARK, 2 + frame, at, 0, 8 * px, 8 * px, colour);
                }
            } else {
                int dustFrame = Math.min(DUST.frames - 1, (frame - 1) * DUST.frames / Math.max(1, frames));
                billboard(view, DUST, dustFrame, at, 0, 8 * px, 8 * px, dust);
            }
        }
    }

    private static final Flipbook PARRY_STAR = Flipbook.strip("parry_star", 8);

    private static final Flipbook PARRY_CORE = Flipbook.strip("parry_core", 8);

    private static final Flipbook PARRY_MOTE = Flipbook.strip("parry_mote", 3);

    private static final long PARRY_MS = 1500L;

    private static final long PARRY_STAR_HOLD_MS = 300L;
    private static final long PARRY_STAR_MS = 650L;
    private static final long PARRY_CORE_MS = 450L;

    private static final float PARRY_SPIN_FRAMES = 16F;
    private static final float PARRY_SPIN_SECONDS = 0.3F;

    private static final int PARRY_MOTES = 12;
    private static final float MOTE_DRAG_SECONDS = 0.12F;

    private static final float MOTE_SINK = 0.06F;

    private static final int PARRY_SPARKS = 18;
    private static final float SPARK_SPEED_MIN = 12F;
    private static final float SPARK_SPEED_MAX = 17F;
    private static final float SPARK_DRAG_SECONDS = 0.6F;

    private static final float SPARK_GRAVITY = 8F;

    private static final float SPARK_TOWARD_VIEWER = 0.4F;
    private static final float SPARK_AWAY_FROM_VIEWER = 0.85F;

    private static final float SPARK_TURN = 2.3999631F;

    private static final float SPARK_STRAIGHT_SECONDS = 0.45F;

    private static final float FIST_SPARK_SPEED = 0.3F;
    private static final float FIST_SPARK_LIFE_MIN = 0.2F;
    private static final float FIST_SPARK_LIFE_MAX = 0.4F;

    private static boolean weaponParry(ImpactCuePacket cue) {
        return cue.size() > 0F;
    }

    private static final float SPARK_ROD_SPACING = 0.08F;
    private static final float SPARK_ROD_SCALE = 0.25F;
    private static final int SPARK_ROD_TICKS = 9;

    private static final float SPARK_LIFE_MIN = 1.1F;
    private static final float SPARK_LIFE_MAX = 1.4F;

    private static final class Sparks {
        final Vec3[] velocity;
        final float[] life;
        final float straight;
        float laid;

        Sparks(Vec3[] velocity, float[] life, float straight) {
            this.velocity = velocity;
            this.life = life;
            this.straight = straight;
        }
    }

    private static final Map<Burst, Sparks> SPARKS = new IdentityHashMap<>();

    private static final float PARRY_PIXEL_SCALE = 0.75F;

    private static void parry(View view, Burst burst) {
        long elapsed = Util.getMillis() - burst.start;
        if (elapsed < 0L || elapsed > PARRY_MS) {
            return;
        }
        int step = (int) (elapsed / FRAME_MS);
        float seconds = step * FRAME_MS / 1000F;
        long stepped = step * FRAME_MS;

        int colour = 0xFF000000 | burst.cue.colour();
        int light = mix(colour, 0.5F);
        float size = Math.max(0.6F, burst.cue.size());
        float px = texel(size) * PARRY_PIXEL_SCALE;
        Vec3 at = burst.at;

        sparks(view, burst, elapsed / 1000F);

        if (!weaponParry(burst.cue)) {
            return;
        }

        if (stepped < PARRY_STAR_MS) {
            int turned = Math.round(PARRY_SPIN_FRAMES * (1F - (float) Math.exp(-seconds / PARRY_SPIN_SECONDS)));
            int spin = Math.floorMod(turned, PARRY_STAR.frames);
            float starAlpha = fade(stepped, PARRY_STAR_HOLD_MS, PARRY_STAR_MS);
            billboard(view, PARRY_STAR, spin, lift(view, at, 2), 0, 16 * px, 16 * px, alpha(colour, starAlpha));
            if (stepped < PARRY_CORE_MS) {
                float coreAlpha = fade(stepped, PARRY_STAR_HOLD_MS / 2, PARRY_CORE_MS);
                billboard(view, PARRY_CORE, spin, lift(view, at, 3), 0, 8 * px, 8 * px, alpha(WHITE, coreAlpha));
            }
        }

        RandomSource rng = RandomSource.create(burst.cue.seed() * 29L + 13L);
        float drag = 1F - (float) Math.exp(-seconds / MOTE_DRAG_SECONDS);
        for (int i = 0; i < PARRY_MOTES; i++) {
            float angle = Mth.TWO_PI * i / PARRY_MOTES + (rng.nextFloat() - 0.5F) * 0.5F;
            float speed = size * (1.6F + rng.nextFloat() * 1.8F);
            float life = 0.8F + rng.nextFloat() * 0.6F;
            float sway = rng.nextFloat() * Mth.TWO_PI;
            int tint = i % 3 == 0 ? WHITE : mix(colour, 0.35F);
            if (seconds >= life) {
                continue;
            }
            float out = size * 0.15F + speed * MOTE_DRAG_SECONDS * drag;
            float across = Mth.cos(angle) * out + 0.03F * Mth.sin(sway + seconds * 4F) * drag;
            float up = Mth.sin(angle) * out - MOTE_SINK * seconds;
            Vec3 where = at.add(view.right.scale(across)).add(view.up.scale(up));

            float share = seconds / life;
            int frame = share < 0.55F ? 0 : share < 0.8F ? (step + i) % 2 : 2;
            float moteAlpha = share < 0.7F ? 1F : 1F - (share - 0.7F) / 0.3F;
            billboard(view, PARRY_MOTE, frame, lift(view, where, 4), 0, 3.5F * px, 3.5F * px, alpha(tint, moteAlpha));
        }
    }

    private static Sparks fire(View view, Burst burst) {
        RandomSource rng = RandomSource.create(burst.cue.seed() * 41L + 7L);
        Vec3 toViewer = view.eye.subtract(burst.at).normalize();
        boolean weapon = weaponParry(burst.cue);
        int asked = Math.max(1, burst.cue.sparks());
        int count = weapon ? Math.max(PARRY_SPARKS, asked) : (asked + 1) / 2 + rng.nextInt(asked / 2 + 1);
        Vec3[] velocity = new Vec3[count];
        float[] life = new float[count];
        if (!weapon) {
            for (int i = 0; i < count; i++) {
                float angle = rng.nextFloat() * Mth.TWO_PI;
                Vec3 way = view.right.scale(Mth.cos(angle)).add(view.up.scale(Mth.sin(angle)))
                        .add(toViewer.scale(-rng.nextFloat() * 0.3F)).normalize();
                velocity[i] = way.scale(FIST_SPARK_SPEED
                        * (SPARK_SPEED_MIN + rng.nextFloat() * (SPARK_SPEED_MAX - SPARK_SPEED_MIN)));
                life[i] = FIST_SPARK_LIFE_MIN + rng.nextFloat() * (FIST_SPARK_LIFE_MAX - FIST_SPARK_LIFE_MIN);
            }
            return new Sparks(velocity, life, 0F);
        }
        float spin = rng.nextFloat() * Mth.TWO_PI;
        for (int i = 0; i < count; i++) {
            float depth = Mth.lerp((i + rng.nextFloat()) / count, -SPARK_AWAY_FROM_VIEWER, SPARK_TOWARD_VIEWER);
            float across = Mth.sqrt(1F - depth * depth);
            float angle = spin + i * SPARK_TURN;
            Vec3 way = view.right.scale(Mth.cos(angle) * across).add(view.up.scale(Mth.sin(angle) * across))
                    .add(toViewer.scale(depth)).normalize();
            velocity[i] = way.scale(SPARK_SPEED_MIN + rng.nextFloat() * (SPARK_SPEED_MAX - SPARK_SPEED_MIN));
            life[i] = SPARK_LIFE_MIN + rng.nextFloat() * (SPARK_LIFE_MAX - SPARK_LIFE_MIN);
        }
        return new Sparks(velocity, life, SPARK_STRAIGHT_SECONDS);
    }

    private static void sparks(View view, Burst burst, float seconds) {
        Sparks sparks = SPARKS.computeIfAbsent(burst, fired -> fire(view, fired));
        Minecraft minecraft = Minecraft.getInstance();
        ParticleStatus setting = minecraft.options.particles().get();
        if (setting == ParticleStatus.MINIMAL) {
            sparks.laid = seconds;
            return;
        }
        float spacing = setting == ParticleStatus.DECREASED ? SPARK_ROD_SPACING * 2F : SPARK_ROD_SPACING;

        for (int i = 0; i < sparks.velocity.length; i++) {
            float from = Math.min(sparks.laid, sparks.life[i]);
            float to = Math.min(seconds, sparks.life[i]);
            if (to <= from) {
                continue;
            }
            Vec3 velocity = sparks.velocity[i];
            int count = Math.max(1, (int) Math.ceil(
                    sparkAt(burst.at, velocity, from, sparks.straight).distanceTo(sparkAt(burst.at, velocity, to, sparks.straight)) / spacing));
            for (int k = 1; k <= count; k++) {
                float t = Mth.lerp((float) k / count, from, to);
                Vec3 where = sparkAt(burst.at, velocity, t, sparks.straight);
                Particle rod = minecraft.particleEngine.createParticle(ParticleTypes.END_ROD,
                        where.x, where.y, where.z, 0.0, 0.0, 0.0);
                if (rod != null) {
                    ((NearFade.Marked) rod).jojoha$fadeNear();
                    float spent = t / sparks.life[i];
                    rod.scale(SPARK_ROD_SCALE * (1F - 0.3F * spent));
                    rod.setLifetime(Math.max(3, Math.round(SPARK_ROD_TICKS * (1F - 0.4F * spent))));
                }
            }
        }
        sparks.laid = seconds;
    }

    private static Vec3 sparkAt(Vec3 at, Vec3 velocity, float t, float straight) {
        float slowed = 1F - (float) Math.exp(-t / SPARK_DRAG_SECONDS);
        float falling = Math.max(0F, t - straight);
        float fallSlowed = 1F - (float) Math.exp(-falling / SPARK_DRAG_SECONDS);
        float dropped = SPARK_GRAVITY * SPARK_DRAG_SECONDS * (falling - SPARK_DRAG_SECONDS * fallSlowed);
        return at.add(velocity.scale(SPARK_DRAG_SECONDS * slowed)).add(0, -dropped, 0);
    }

    private static float fade(long elapsed, long hold, long end) {
        if (elapsed <= hold) {
            return 1F;
        }
        return Mth.clamp(1F - (float) (elapsed - hold) / (end - hold), 0F, 1F);
    }

    private static int alpha(int argb, float share) {
        return Math.round(Mth.clamp(share, 0F, 1F) * 255F) << 24 | (argb & 0xFFFFFF);
    }

    private static void billboard(View view, Flipbook book, int frame, Vec3 centre, int quarter,
                                  float halfW, float halfH, int argb) {
        Vec3 right = view.right;
        Vec3 up = view.up;
        for (int q = 0; q < quarter; q++) {
            Vec3 turned = up;
            up = right.scale(-1);
            right = turned;
        }
        sprite(view, book, frame, centre, right.scale(halfW), up.scale(halfH), argb);
    }

    private static void sprite(View view, Flipbook book, int frame, Vec3 centre, Vec3 right, Vec3 up, int argb) {
        sprite(view, book, frame, centre, right, up, argb, false);
    }

    private static void sprite(View view, Flipbook book, int frame, Vec3 centre, Vec3 right, Vec3 up, int argb,
                               boolean onTop) {
        argb = NearFade.apply(argb, view.eye, centre);
        RenderType type = onTop ? RenderType.textSeeThrough(book.texture(frame)) : RenderType.text(book.texture(frame));
        view.used.add(type);
        VertexConsumer out = view.buffers.getBuffer(type);
        float u0 = book.u0(frame);
        float u1 = book.u1(frame);

        Vec3 bl = centre.subtract(right).subtract(up);
        Vec3 br = centre.add(right).subtract(up);
        Vec3 tr = centre.add(right).add(up);
        Vec3 tl = centre.subtract(right).add(up);

        vertex(view, out, bl, argb, u0, 1F);
        vertex(view, out, br, argb, u1, 1F);
        vertex(view, out, tr, argb, u1, 0F);
        vertex(view, out, tl, argb, u0, 0F);

        vertex(view, out, tl, argb, u0, 0F);
        vertex(view, out, tr, argb, u1, 0F);
        vertex(view, out, br, argb, u1, 1F);
        vertex(view, out, bl, argb, u0, 1F);
    }

    private static void vertex(View view, VertexConsumer out, Vec3 at, int argb, float u, float v) {
        out.addVertex(view.pose, (float) (at.x - view.eye.x), (float) (at.y - view.eye.y), (float) (at.z - view.eye.z))
                .setColor(argb)
                .setUv(u, v)
                .setLight(LightTexture.FULL_BRIGHT);
    }

    private static Vec3[] plane(Vec3 dir) {
        Vec3 helper = Math.abs(dir.y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 u = dir.cross(helper).normalize();
        return new Vec3[] {u, dir.cross(u).normalize()};
    }

    private static Vec3 lift(View view, Vec3 at, int layer) {
        Vec3 toward = view.eye.subtract(at);
        double distance = toward.length();
        if (distance < 1.0E-4) {
            return at;
        }
        return at.add(toward.scale(0.004 * layer * Math.max(1.0, distance) / distance));
    }

    private static int mix(int argb, float white) {
        return mix(argb, WHITE, white);
    }

    private static int mix(int from, int to, float share) {
        int r = Math.round(((from >> 16) & 0xFF) * (1F - share) + ((to >> 16) & 0xFF) * share);
        int g = Math.round(((from >> 8) & 0xFF) * (1F - share) + ((to >> 8) & 0xFF) * share);
        int b = Math.round((from & 0xFF) * (1F - share) + (to & 0xFF) * share);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }
}
