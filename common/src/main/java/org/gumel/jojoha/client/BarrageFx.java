package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.registry.ModRegistries;
import org.gumel.jojoha.registry.ModSounds;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.StandShadowForm;
import org.gumel.jojoha.stand.StandTypes;
import org.gumel.jojoha.stand.combat.StandFist;
import org.gumel.jojoha.stand.workshop.BarrageShout;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public final class BarrageFx {
    private static final double RANGE = 48.0;

    private static final double HIT_SLACK = 0.45;

    private static final float SPARK_SCALE = 0.22F;
    private static final float FOCUS_SPARK_SCALE = 0.5F;

    private static final float SHUDDER = 1F;
    private static final float FOCUS_SHUDDER = 2.2F;

    private static final int DUST_EVERY = 3;

    private static final float HIT_VOLUME = 0.3F;
    private static final float FOCUS_HIT_VOLUME = 0.6F;
    private static final float HIT_PITCH = 1.25F;
    private static final float HIT_PITCH_SPREAD = 0.4F;
    private static final float FOCUS_HIT_PITCH = 0.72F;
    private static final float THUD_VOLUME = 0.5F;
    private static final float THUD_PITCH = 0.55F;
    private static final int HUSH_TICKS = 10;

    private static long hushedUntil = Long.MIN_VALUE;

    private static final double WALL_PAST = 0.35;
    private static final int WALL_CHIPS = 3;
    private static final double WALL_CHIP_SPEED = 0.12;
    private static final float WALL_VOLUME = 0.25F;

    private static final ResourceLocation WORD_SHEET =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/fx/barrage_words.png");
    private static final int WORD_ROWS = 12;
    private static final int WORD_VARIANTS = 2;
    private static final int KICK_WORD = 5;
    private static final int WORD_GAP_TICKS = 8;
    private static final int WORD_LIFE_TICKS = 30;
    private static final int WORDS_ALIVE = 6;
    private static final float WORD_WIDTH = 1.15F;
    private static final float WORD_ASPECT = 0.25F;
    private static final float WORD_POP_TICKS = 3F;
    private static final float WORD_OVERSHOOT = 1.7F;
    private static final float WORD_FADE_TICKS = 8F;
    private static final double WORD_RISE = 0.022;
    private static final double WORD_DRIFT = 0.02;
    private static final double WORD_SIDE_NEAR = 0.6;
    private static final double WORD_SIDE_FAR = 1.4;
    private static final double WORD_LOW = 1.0;
    private static final double WORD_HIGH = 2.4;
    private static final double WORD_AHEAD = 0.3;
    private static final float WORD_TILT_DEGREES = 12F;
    private static final float WORD_WHITEN = 0.45F;
    private static final long WORD_SALT = 0x2545F4914F6CDD1DL;

    private static final int SMEAR_POINTS = 6;
    private static final double SMEAR_STEP = 0.3;
    private static final double SMEAR_FROM = 0.15;
    private static final float SMEAR_WIDTH = 0.2F;
    private static final float SMEAR_ALPHA = 0.22F;
    private static final float PHANTOM_TRAIL_ALPHA = 0.4F;
    private static final float FAR_TRAIL_ALPHA = 0.55F;
    private static final float FAR_TRAIL_GROW = 1.2F;

    private static final float FOV_PUSH = 0.035F;
    private static final float FOV_PULSE = 0.008F;
    private static final float PULSE_TICKS = 3F;
    private static final float PUSH_RAMP = 0.2F;
    private static final float SHAKE_DEGREES = 0.06F;
    private static final float FOCUS_SHAKE = 1.6F;

    private static final Map<ResourceLocation, Integer> WORD_OF = Map.of(
            StandTypes.STAR_PLATINUM_ID, 0,
            StandTypes.THE_WORLD_ID, 1,
            StandTypes.SECRET_THE_WORLD_ID, 1,
            StandTypes.SILVER_CHARIOT_ID, 2,
            StandTypes.HIEROPHANT_GREEN_ID, 3,
            StandTypes.MAGICIANS_RED_ID, 4);

    private record Word(int row, Vec3 at, Vec3 drift, float tilt, int colour, long born) {
    }

    private static final ArrayDeque<Word> WORDS = new ArrayDeque<>();
    private static final Map<Integer, Integer> HITS = new HashMap<>();

    private static int ownStand = -1;
    private static float push;
    private static float pushO;
    private static float pulseAt = Float.NEGATIVE_INFINITY;

    private BarrageFx() {
    }

    public static void hush(long now) {
        hushedUntil = now + HUSH_TICKS;
    }

    public static void clear() {
        hushedUntil = Long.MIN_VALUE;
        WORDS.clear();
        HITS.clear();
        BarrageLoop.clear();
        TargetShudder.clear();
        ownStand = -1;
        push = 0F;
        pushO = 0F;
        pulseAt = Float.NEGATIVE_INFINITY;
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null) {
            clear();
            return;
        }
        if (minecraft.isPaused()) {
            return;
        }
        long now = level.getGameTime();
        Set<Integer> seen = new HashSet<>();
        StandEntity mine = null;
        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof StandEntity stand) || !stand.isBarraging()) {
                continue;
            }
            seen.add(stand.getId());
            if (stand.getOwner() == minecraft.player) {
                mine = stand;
            }
            if (stand.distanceToSqr(minecraft.player) > RANGE * RANGE) {
                continue;
            }
            if (BarrageLoop.runs(stand)) {
                BarrageLoop.track(stand);
                arrive(level, stand, now, stand == mine);
            }
            if (now % WORD_GAP_TICKS == 0 && JojohaClientConfig.BARRAGE_WORDS.on()) {
                shout(stand, now);
            }
        }
        BarrageLoop.forget(seen);
        BarrageGhosts.forget(seen);
        HITS.keySet().retainAll(seen);
        WORDS.removeIf(word -> now - word.born() > WORD_LIFE_TICKS || now < word.born());
        TargetShudder.prune();

        ownStand = mine == null ? -1 : mine.getId();
        pushO = push;
        push = Mth.approach(push, mine != null ? 1F : 0F, PUSH_RAMP);
    }

    private static void arrive(ClientLevel level, StandEntity stand, long now, boolean mine) {
        BarrageLoop.Punch punch = BarrageLoop.startedAt(stand, now - 1, 0F);
        if (punch == null) {
            return;
        }
        Vec3 aim = punch.fist().aim();
        RandomSource random = RandomSource.create(BarrageLoop.seed(stand.getId(), punch.index()) ^ WORD_SALT);
        LivingEntity target = BarrageLoop.target(stand);
        if (target != null) {
            if (target.getBoundingBox().inflate(HIT_SLACK).contains(aim)) {
                hit(level, stand, target, aim, punch.focus(), random);
                if (mine && punch.focus()) {
                    pulseAt = now;
                }
            }
            return;
        }
        wall(level, stand, aim, punch.focus(), random);
    }

    private static void hit(ClientLevel level, StandEntity stand, LivingEntity target, Vec3 aim, boolean focus,
                            RandomSource random) {
        level.addParticle(ModRegistries.IMPACT_RING.get(), aim.x, aim.y, aim.z,
                focus ? FOCUS_SPARK_SCALE : SPARK_SCALE, 0.0, 0.0);
        TargetShudder.kick(target, focus ? FOCUS_SHUDDER : SHUDDER);
        if (JojohaClientConfig.BARRAGE_HIT_SOUNDS.on() && level.getGameTime() >= hushedUntil) {
            level.playLocalSound(aim.x, aim.y, aim.z, ModSounds.HIT_TAP.get(), SoundSource.PLAYERS,
                    focus ? FOCUS_HIT_VOLUME : HIT_VOLUME,
                    focus ? FOCUS_HIT_PITCH : HIT_PITCH + random.nextFloat() * HIT_PITCH_SPREAD, false);
            if (focus) {
                level.playLocalSound(aim.x, aim.y, aim.z, SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS,
                        THUD_VOLUME, THUD_PITCH, false);
            }
        }
        int hits = HITS.merge(stand.getId(), 1, Integer::sum);
        if ((focus || hits % DUST_EVERY == 0) && JojohaClientConfig.BARRAGE_AIR.on()) {
            Vec3 heading = aim.subtract(BarrageLoop.chest(stand, 0F));
            BarrageAir.dust(level, target.position().add(0.0, 0.1, 0.0),
                    heading.lengthSqr() < 1.0E-6 ? Vec3.ZERO : heading.normalize(), random);
        }
    }

    private static void wall(ClientLevel level, StandEntity stand, Vec3 aim, boolean focus, RandomSource random) {
        if (!JojohaClientConfig.BARRAGE_AIR.on()) {
            return;
        }
        Vec3 chest = BarrageLoop.chest(stand, 0F);
        Vec3 heading = aim.subtract(chest);
        if (heading.lengthSqr() < 1.0E-6) {
            return;
        }
        Vec3 end = aim.add(heading.normalize().scale(WALL_PAST));
        BlockHitResult struck = level.clip(new ClipContext(chest, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, stand));
        if (struck.getType() != HitResult.Type.BLOCK) {
            return;
        }
        BlockState state = level.getBlockState(struck.getBlockPos());
        if (state.isAir()) {
            return;
        }
        Vec3 at = struck.getLocation();
        Vec3 out = Vec3.atLowerCornerOf(struck.getDirection().getNormal());
        BlockParticleOption chip = new BlockParticleOption(ParticleTypes.BLOCK, state);
        for (int i = 0; i < WALL_CHIPS; i++) {
            level.addParticle(chip, at.x, at.y, at.z,
                    out.x * WALL_CHIP_SPEED + (random.nextDouble() - 0.5) * WALL_CHIP_SPEED,
                    out.y * WALL_CHIP_SPEED + random.nextDouble() * WALL_CHIP_SPEED,
                    out.z * WALL_CHIP_SPEED + (random.nextDouble() - 0.5) * WALL_CHIP_SPEED);
        }
        if (focus) {
            level.addParticle(ModRegistries.IMPACT_RING.get(), at.x, at.y, at.z, SPARK_SCALE, 0.0, 0.0);
        }
        if ((focus || random.nextInt(3) == 0) && JojohaClientConfig.BARRAGE_HIT_SOUNDS.on()) {
            level.playLocalSound(at.x, at.y, at.z, state.getSoundType().getHitSound(), SoundSource.BLOCKS,
                    WALL_VOLUME, HIT_PITCH + random.nextFloat() * HIT_PITCH_SPREAD, false);
        }
    }

    private static void shout(StandEntity stand, long now) {
        BarrageShout cry = stand.barrageShout();
        ResourceLocation voice = cry == BarrageShout.SILENT ? null
                : cry.voices().isEmpty() ? stand.getStandType().id() : cry.voice((int) (now / WORD_GAP_TICKS));
        Integer word = voice == null ? null : cry.kick() ? Integer.valueOf(KICK_WORD) : WORD_OF.get(voice);
        if (word == null) {
            return;
        }
        RandomSource random = RandomSource.create(BarrageLoop.seed(stand.getId(), now) ^ WORD_SALT);
        float yaw = stand.yBodyRot;
        Vec3 forward = StandFist.toWorld(Vec3.ZERO, yaw, new Vec3(0, 0, -16));
        Vec3 right = StandFist.right(yaw);
        double side = (random.nextBoolean() ? 1.0 : -1.0) * Mth.lerp(random.nextDouble(), WORD_SIDE_NEAR, WORD_SIDE_FAR);
        Vec3 at = stand.position()
                .add(forward.scale(WORD_AHEAD + random.nextDouble() * 0.5))
                .add(right.scale(side))
                .add(0.0, Mth.lerp(random.nextDouble(), WORD_LOW, WORD_HIGH), 0.0);
        Vec3 drift = right.scale(Math.signum(side) * WORD_DRIFT).add(0.0, WORD_RISE, 0.0);
        float tilt = (random.nextFloat() * 2F - 1F) * WORD_TILT_DEGREES * Mth.DEG_TO_RAD;
        int row = word * WORD_VARIANTS + random.nextInt(WORD_VARIANTS);
        int colour = whiter(StandShadowForm.auraColourFor(stand));
        WORDS.addLast(new Word(row, at, drift, tilt, colour, now));
        while (WORDS.size() > WORDS_ALIVE) {
            WORDS.removeFirst();
        }
    }

    private static int whiter(int rgb) {
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;
        red += Math.round((255 - red) * WORD_WHITEN);
        green += Math.round((255 - green) * WORD_WHITEN);
        blue += Math.round((255 - blue) * WORD_WHITEN);
        return (red << 16) | (green << 8) | blue;
    }

    public static void render(PoseStack poseStack, MultiBufferSource buffers, Camera camera, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }
        Vec3 eye = camera.getPosition();
        Matrix4f pose = poseStack.last().pose();
        boolean smeared = false;
        float smears = JojohaClientConfig.BARRAGE_SMEARS.value();
        if (smears > 0F) {
            for (Entity entity : level.entitiesForRendering()) {
                if (!(entity instanceof StandEntity stand) || !stand.isBarraging()
                        || stand.distanceToSqr(eye) > RANGE * RANGE) {
                    continue;
                }
                float alpha = SMEAR_ALPHA * smears * StandViewAlpha.of(stand) * BarrageGlow.glow(stand, partialTick);
                if (alpha <= 0.01F) {
                    continue;
                }
                int colour = StandShadowForm.auraColourFor(stand);
                VertexConsumer out = buffers.getBuffer(RenderType.lightning());
                if (BarrageLoop.runs(stand)) {
                    for (BarrageLoop.Punch punch : BarrageLoop.punches(stand, partialTick)) {
                        if (punch != null) {
                            smear(out, pose, eye, stand, punch.fist(), StandFist.shoulderModel(punch.fist().slot()),
                                    punch.fistAge(), punch.focus() ? BarrageLoop.FOCUS_SCALE : 1F, colour, alpha,
                                    partialTick);
                        }
                    }
                    for (BarrageLoop.Phantom phantom : BarrageLoop.phantoms(stand, partialTick)) {
                        smear(out, pose, eye, stand, phantom.fist(), phantom.shoulder(), phantom.age(),
                                phantom.far() ? FAR_TRAIL_GROW : 1F, colour,
                                alpha * PHANTOM_TRAIL_ALPHA * (phantom.far() ? FAR_TRAIL_ALPHA : 1F), partialTick);
                    }
                }
                for (int slot = StandFist.BODY_SLOTS; slot < StandFist.SLOTS; slot++) {
                    StandFist fist = StandFistView.live(stand, slot, partialTick);
                    if (fist != null) {
                        smear(out, pose, eye, stand, fist, StandFist.shoulderModel(fist.slot()),
                                StandFistView.age(fist, partialTick), 1F, colour, alpha, partialTick);
                    }
                }
                smeared = true;
            }
        }
        boolean worded = drawWords(buffers, pose, eye, camera, level.getGameTime(), partialTick);

        if (buffers instanceof MultiBufferSource.BufferSource source) {
            if (smeared) {
                source.endBatch(RenderType.lightning());
            }
            if (worded) {
                source.endBatch(RenderType.text(WORD_SHEET));
            }
        }
    }

    private static void smear(VertexConsumer out, Matrix4f pose, Vec3 eye, StandEntity stand, StandFist fist,
                              Vec3 from, double age, float grow, int colour, float alpha, float partialTick) {
        if (age <= SMEAR_FROM || age > StandFist.OUT_TICKS) {
            return;
        }
        float yaw = Mth.rotLerp(partialTick, stand.yBodyRotO, stand.yBodyRot);
        Vec3 origin = stand.getPosition(partialTick).add(BarrageFlow.bodyOffset(stand, partialTick));
        Vec3 right = StandFist.right(yaw);
        Vec3 shoulder = StandFist.toWorld(origin, yaw, from);
        Vec3[] points = new Vec3[SMEAR_POINTS];
        for (int i = 0; i < SMEAR_POINTS; i++) {
            points[i] = fist.at(shoulder, right, Math.max(0.0, age - i * SMEAR_STEP));
        }
        float r = ((colour >> 16) & 0xFF) / 255F;
        float g = ((colour >> 8) & 0xFF) / 255F;
        float b = (colour & 0xFF) / 255F;
        int last = SMEAR_POINTS - 1;
        for (int i = 0; i < last; i++) {
            Vec3 head = points[i];
            Vec3 tail = points[i + 1];
            Vec3 along = head.subtract(tail);
            if (along.lengthSqr() < 1.0E-6) {
                continue;
            }
            Vec3 headSide = across(along, eye.subtract(head), SMEAR_WIDTH * grow * (1F - i / (float) last));
            Vec3 tailSide = across(along, eye.subtract(tail), SMEAR_WIDTH * grow * (1F - (i + 1) / (float) last));
            if (headSide == null || tailSide == null) {
                continue;
            }
            float headAlpha = alpha * (1F - i / (float) last);
            float tailAlpha = alpha * (1F - (i + 1) / (float) last);
            Vec3 a = head.subtract(headSide).subtract(eye);
            Vec3 bb = head.add(headSide).subtract(eye);
            Vec3 c = tail.add(tailSide).subtract(eye);
            Vec3 d = tail.subtract(tailSide).subtract(eye);
            vertex(out, pose, a, r, g, b, headAlpha);
            vertex(out, pose, bb, r, g, b, headAlpha);
            vertex(out, pose, c, r, g, b, tailAlpha);
            vertex(out, pose, d, r, g, b, tailAlpha);
            vertex(out, pose, d, r, g, b, tailAlpha);
            vertex(out, pose, c, r, g, b, tailAlpha);
            vertex(out, pose, bb, r, g, b, headAlpha);
            vertex(out, pose, a, r, g, b, headAlpha);
        }
    }

    private static Vec3 across(Vec3 along, Vec3 toEye, float width) {
        Vec3 side = along.cross(toEye);
        if (side.lengthSqr() < 1.0E-8) {
            return null;
        }
        return side.normalize().scale(width * 0.5);
    }

    private static void vertex(VertexConsumer out, Matrix4f pose, Vec3 at, float r, float g, float b, float a) {
        out.addVertex(pose, (float) at.x, (float) at.y, (float) at.z).setColor(r, g, b, a);
    }

    private static boolean drawWords(MultiBufferSource buffers, Matrix4f pose, Vec3 eye, Camera camera, long now,
                                     float partialTick) {
        if (WORDS.isEmpty() || !JojohaClientConfig.BARRAGE_WORDS.on()) {
            return false;
        }
        Vector3f leftF = camera.getLeftVector();
        Vector3f upF = camera.getUpVector();
        Vec3 right = new Vec3(-leftF.x(), -leftF.y(), -leftF.z());
        Vec3 up = new Vec3(upF.x(), upF.y(), upF.z());
        VertexConsumer out = buffers.getBuffer(RenderType.text(WORD_SHEET));
        Iterator<Word> it = WORDS.iterator();
        while (it.hasNext()) {
            Word word = it.next();
            float age = now - word.born() + partialTick;
            if (age < 0F || age > WORD_LIFE_TICKS) {
                continue;
            }
            float size = pop(age);
            float fade = Mth.clamp((WORD_LIFE_TICKS - age) / WORD_FADE_TICKS, 0F, 1F);
            if (size <= 0.01F || fade <= 0.01F) {
                continue;
            }
            Vec3 centre = word.at().add(word.drift().scale(age));
            double halfWide = WORD_WIDTH * size * 0.5;
            double halfTall = halfWide * WORD_ASPECT;
            double cos = Math.cos(word.tilt());
            double sin = Math.sin(word.tilt());
            Vec3 wide = right.scale(cos).add(up.scale(sin)).scale(halfWide);
            Vec3 tall = up.scale(cos).subtract(right.scale(sin)).scale(halfTall);
            int argb = (Math.round(fade * 255F) << 24) | (word.colour() & 0xFFFFFF);
            float v0 = word.row() / (float) WORD_ROWS;
            float v1 = (word.row() + 1) / (float) WORD_ROWS;
            Vec3 bl = centre.subtract(wide).subtract(tall).subtract(eye);
            Vec3 br = centre.add(wide).subtract(tall).subtract(eye);
            Vec3 tr = centre.add(wide).add(tall).subtract(eye);
            Vec3 tl = centre.subtract(wide).add(tall).subtract(eye);
            textVertex(out, pose, bl, argb, 0F, v1);
            textVertex(out, pose, br, argb, 1F, v1);
            textVertex(out, pose, tr, argb, 1F, v0);
            textVertex(out, pose, tl, argb, 0F, v0);
            textVertex(out, pose, tl, argb, 0F, v0);
            textVertex(out, pose, tr, argb, 1F, v0);
            textVertex(out, pose, br, argb, 1F, v1);
            textVertex(out, pose, bl, argb, 0F, v1);
        }
        return true;
    }

    private static float pop(float age) {
        if (age >= WORD_POP_TICKS) {
            return 1F;
        }
        float t = age / WORD_POP_TICKS - 1F;
        return 1F + (WORD_OVERSHOOT + 1F) * t * t * t + WORD_OVERSHOOT * t * t;
    }

    private static void textVertex(VertexConsumer out, Matrix4f pose, Vec3 at, int argb, float u, float v) {
        out.addVertex(pose, (float) at.x, (float) at.y, (float) at.z)
                .setColor(argb)
                .setUv(u, v)
                .setLight(LightTexture.FULL_BRIGHT);
    }

    public static float fovScale() {
        if (!JojohaClientConfig.cameraMotion()) {
            return 1F;
        }
        float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        float amount = Mth.lerp(partialTick, pushO, push);
        float recoil = JojohaClientConfig.BARRAGE_RECOIL.value();
        if (amount <= 0F || recoil <= 0F) {
            return 1F;
        }
        float pulse = 0F;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            float since = minecraft.level.getGameTime() + partialTick - pulseAt;
            if (since >= 0F && since < PULSE_TICKS) {
                pulse = FOV_PULSE * (1F - since / PULSE_TICKS);
            }
        }
        return 1F - recoil * (FOV_PUSH * amount + pulse);
    }

    public static float[] cameraShake() {
        if (!JojohaClientConfig.cameraMotion() || push <= 0F && pushO <= 0F) {
            return null;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return null;
        }
        float partialTick = minecraft.getTimer().getGameTimeDeltaPartialTick(false);
        float recoil = JojohaClientConfig.BARRAGE_RECOIL.value();
        float amount = Mth.lerp(partialTick, pushO, push) * recoil * SHAKE_DEGREES;
        if (amount <= 0F) {
            return null;
        }
        if (minecraft.level.getEntity(ownStand) instanceof StandEntity stand && BarrageLoop.runs(stand)) {
            for (BarrageLoop.Punch punch : BarrageLoop.punches(stand, partialTick)) {
                if (punch != null && punch.focus()
                        && punch.age() < BarrageLoop.OUT_TICKS + BarrageLoop.FOCUS_HOLD_TICKS) {
                    amount *= FOCUS_SHAKE;
                    break;
                }
            }
        }
        double t = (minecraft.level.getGameTime() % 100000L) + partialTick;
        return new float[] {(float) (Math.sin(t * 2.9) * Math.cos(t * 1.3)) * amount,
                (float) (Math.cos(t * 3.7) * Math.sin(t * 0.9 + 1.0)) * amount};
    }
}
