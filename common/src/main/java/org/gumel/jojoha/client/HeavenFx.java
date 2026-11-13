package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.client.fx.HeavenHold;
import org.gumel.jojoha.network.packet.GroundBreakerFxPacket;
import org.gumel.jojoha.network.packet.HeavenFxPacket;
import org.gumel.jojoha.stand.StandEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class HeavenFx {
    private static final long FADE_IN_MS = 150L;
    private static final long FADE_OUT_MS = 400L;
    private static final long FLASH_HOLD_MS = 350L;
    private static final long SEPIA_IN_MS = 150L;
    private static final long SEPIA_OUT_MS = 400L;
    private static final long GOLD_IN_MS = 200L;
    private static final long GOLD_OUT_MS = 400L;
    private static final long PULSE_MS = 600L;
    public static final float FIRST_PERSON_FLASH = 0.45F;
    private static final long REWRITE_IN_MS = 400L;
    private static final long REWRITE_OUT_MS = 700L;
    private static final long SHOCK_MS = 900L;
    private static final float REWRITE_PEAK = 0.85F;
    private static final long REWRITE_SETTLE_MS = 1500L;
    private static final float REWRITE_HOLD_SHARE = 0.5F;
    private static final double REWRITE_SEEN = 48.0;
    private static final float GOLD_STOP_PEAK = 0.55F;
    private static final int GOLD_STOP_DOME_TICKS = 60;

    private static final int RUSH_TRAIL_BASE = 6;
    private static final int RUSH_MARK_BASE = 900000;
    private static final int SLAM_MARK_BASE = 950000;
    private static final float SLAM_SWELL_CAP = 12F;

    private static final int[] GOLD = HeavenWorldFx.GOLDS;
    private static final int[] VIOLET = HeavenWorldFx.VIOLETS;
    private static final int[] WHITE = HeavenWorldFx.WHITES;

    private static long erasureStart = Long.MIN_VALUE;
    private static long erasureLength;
    private static long erasureIn = 150L;
    private static long flashStart = Long.MIN_VALUE;
    private static long flashLength;
    private static long sepiaStart = Long.MIN_VALUE;
    private static long sepiaLength;
    private static long goldStart = Long.MIN_VALUE;
    private static long goldLength;
    private static Vec3 goldCentre = Vec3.ZERO;
    private static float goldRadius;
    private static long pulseStart = Long.MIN_VALUE;
    private static long realmStart = Long.MIN_VALUE;
    private static long realmWaveMs;
    private static long realmHoldMs;
    private static float realmRadius;
    private static Vec3 realmCentre = Vec3.ZERO;
    private static final long REALM_CLOSE_MS = 3000L;

    private static long rewriteStart = Long.MIN_VALUE;
    private static long rewriteLength;
    private static long rewriteIn = 400L;
    private static long shockStart = Long.MIN_VALUE;

    private static final List<Pending> PENDING = new ArrayList<>();
    private static final Map<Integer, Vec3> STEPPED_FROM = new HashMap<>();

    private record Pending(int[] left, Runnable action) {
    }

    private HeavenFx() {
    }

    public static void heard(HeavenFxPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null) {
            return;
        }
        Vec3 at = new Vec3(packet.x(), packet.y(), packet.z());
        long length = packet.ticks() * 50L;
        switch (packet.kind()) {
            case HeavenFxPacket.ERASURE -> {
                erasureStart = now();
                erasureLength = length;
                erasureIn = FADE_IN_MS;
            }
            case HeavenFxPacket.REALM -> {
                realmStart = now();
                realmCentre = at;
                realmRadius = packet.size();
                realmWaveMs = Math.max(1, packet.other()) * 50L;
                realmHoldMs = Math.max(20, packet.ticks()) * 50L;
            }
            case HeavenFxPacket.ERASE_WORLD -> {
                erasureStart = now();
                erasureLength = length;
                erasureIn = Math.max(FADE_IN_MS, packet.other() * 50L);
            }
            case HeavenFxPacket.FLASH -> {
                flashStart = now();
                flashLength = length;
            }
            case HeavenFxPacket.REWIND -> {
                sepiaStart = now();
                sepiaLength = length;
            }
            case HeavenFxPacket.SUMMON -> summon(packet, at);
            case HeavenFxPacket.TIER -> tier(level, packet, at);
            case HeavenFxPacket.HALO -> HeavenWorldFx.halo(packet.entity(), packet.ticks());
            case HeavenFxPacket.RUSH -> HeavenGhostLayer.rush(packet.entity(), packet.ticks() + RUSH_TRAIL_BASE);
            case HeavenFxPacket.RUSH_LEG -> rushLeg(packet, at);
            case HeavenFxPacket.ROAR -> roar(level, packet, at);
            case HeavenFxPacket.HIGH_HIT -> highHit(at);
            case HeavenFxPacket.GOLD_STOP -> goldStop(packet, at);
            case HeavenFxPacket.PULSE -> pulse(packet, at);
            case HeavenFxPacket.TETHER -> HeavenWorldFx.beam(packet.entity(), packet.other(), at,
                    HeavenWorldFx.GOLD, 0.15F, packet.ticks(), true);
            case HeavenFxPacket.SERVANT -> HeavenWorldFx.servant(packet.entity(), packet.ticks());
            case HeavenFxPacket.BEAM -> beam(level, packet, at);
            case HeavenFxPacket.PILLAR -> pillar(packet, at);
            case HeavenFxPacket.DOME -> dome(packet, at);
            case HeavenFxPacket.ERASED -> {
                HeavenWorldFx.erased(packet.entity(), packet.ticks());
                HeavenWorldFx.whiten(packet.entity(), packet.ticks());
            }
            case HeavenFxPacket.PILLARS -> pillars(packet, at);
            case HeavenFxPacket.LIFT -> lift(packet);
            case HeavenFxPacket.SLAM -> slam(packet, at);
            case HeavenFxPacket.RISE -> rise(level, packet, at);
            case HeavenFxPacket.HIGH -> HeavenWorldFx.high(packet.entity(), packet.ticks());
            case HeavenFxPacket.RUSH_MARK -> rushMark(packet, at);
            case HeavenFxPacket.CHARGE -> HeavenWorldFx.windup(packet.entity(), Math.max(2, packet.ticks()));
            case HeavenFxPacket.JUDGE -> judge(packet, at);
            case HeavenFxPacket.STEP -> step(level, packet, at);
            case HeavenFxPacket.UPPER -> upper(packet, at);
            case HeavenFxPacket.KNIVES -> knives(packet, at);
            case HeavenFxPacket.CRACKS -> HeavenWorldFx.cracks(at, packet.size(), Math.max(10, packet.ticks()));
            case HeavenFxPacket.UNMADE -> unmade(level, packet, at);
            case HeavenFxPacket.STAND_TIER -> HeavenWorldFx.standTier(packet.entity(), Math.round(packet.size()), packet.ticks());
            case HeavenFxPacket.SKY -> HeavenSky.cue(Math.round(packet.size()), packet.ticks());
            case HeavenFxPacket.TELEGRAPH -> HeavenWorldFx.telegraph(packet.entity(), packet.ticks());
            case HeavenFxPacket.REWRITE -> {
                rewriteStart = now();
                rewriteLength = Math.max(20, packet.ticks()) * 50L;
                rewriteIn = Math.max(REWRITE_IN_MS, Math.round(packet.size()) * 50L);
            }
            default -> {
            }
        }
    }

    public static void tick() {
        if (PENDING.isEmpty()) {
            return;
        }
        List<Runnable> due = new ArrayList<>();
        Iterator<Pending> iterator = PENDING.iterator();
        while (iterator.hasNext()) {
            Pending pending = iterator.next();
            if (--pending.left[0] <= 0) {
                due.add(pending.action);
                iterator.remove();
            }
        }
        due.forEach(Runnable::run);
    }

    public static void clear() {
        PENDING.clear();
        STEPPED_FROM.clear();
        erasureStart = Long.MIN_VALUE;
        flashStart = Long.MIN_VALUE;
        sepiaStart = Long.MIN_VALUE;
        goldStart = Long.MIN_VALUE;
        pulseStart = Long.MIN_VALUE;
        rewriteStart = Long.MIN_VALUE;
        shockStart = Long.MIN_VALUE;
    }

    private static void later(int ticks, Runnable action) {
        if (ticks <= 0) {
            action.run();
            return;
        }
        PENDING.add(new Pending(new int[]{ticks}, action));
    }

    private static void summon(HeavenFxPacket packet, Vec3 at) {
        HeavenWorldFx.pillar(at, 1.4F, 42F, HeavenWorldFx.GOLD, 1F, 34, HeavenWorldFx.DESCEND);
        later(9, () -> {
            HeavenWorldFx.ring(at, 0.5F, 9F, 1.5F, HeavenWorldFx.GOLD, 1F, 22);
            HeavenWorldFx.ring(at, 0.2F, 5F, 0.6F, HeavenWorldFx.WHITE, 1F, 14);
            pop(at.add(0.0, 1.0, 0.0), 4.5);
            radial(at.add(0.0, 0.2, 0.0), 40, 0.45, GOLD);
            fountain(at, 24, 0.5, WHITE);
            shake(at, 0.8F);
            if (mine(packet.entity())) {
                frame(0.55F);
            }
        });
    }

    private static void tier(ClientLevel level, HeavenFxPacket packet, Vec3 at) {
        int tier = Mth.clamp(Math.round(packet.size()), 1, 3);
        Entity user = level.getEntity(packet.entity());
        HeavenHold.hold(user, 90 + 50 * tier);
        if (user instanceof Player player) {
            HeavenHold.hold(StandEntity.findFor(player), 90 + 50 * tier);
        }
        for (int i = 0; i <= tier; i++) {
            int wave = i;
            later(i * 3, () -> HeavenWorldFx.ring(at, 0.3F, 4F + 2F * tier + wave, 0.9F,
                    wave % 2 == 0 ? HeavenWorldFx.GOLD : HeavenWorldFx.WHITE, 1F, 16));
        }
        Vec3 chest = at.add(0.0, 1.1, 0.0);
        pop(chest, 3.0 + tier);
        radial(chest, 15 * tier, 0.4, GOLD);
        fountain(at, 10 * tier, 0.45, WHITE);
        shake(at, 0.35F + 0.25F * tier);
        if (tier == 3) {
            HeavenWorldFx.pillar(at, 1.0F, 30F, HeavenWorldFx.GOLD, 1F, 30, HeavenWorldFx.RISE);
        }
        if (mine(packet.entity())) {
            pulseStart = now();
            if (tier >= 2) {
                frame(0.35F + 0.2F * tier);
            }
        }
    }

    private static void rushMark(HeavenFxPacket packet, Vec3 at) {
        Vec3 previous = HeavenGhostLayer.mark(packet.entity(), packet.other(), at, packet.size(),
                Math.max(10, packet.ticks()));
        Vec3 chest = at.add(0.0, 1.1, 0.0);
        if (previous != null) {
            HeavenWorldFx.line(previous.add(0.0, 1.1, 0.0), chest, HeavenWorldFx.GOLD, 0.12F,
                    Math.max(10, packet.ticks()));
        }
        HeavenWorldFx.glint(chest, 0.8F, HeavenWorldFx.GOLD, 8);
        HeavenWorldFx.flare(chest, 2.2F, HeavenWorldFx.GOLD, 10);
        HeavenWorldFx.burst(chest, 10, 0.12, GOLD, 0.07F, 12, 0.0, false);
    }

    private static void rushLeg(HeavenFxPacket packet, Vec3 at) {
        boolean last = packet.other() == 1;
        int index = Math.round(packet.size());
        Vec3 shard = HeavenGhostLayer.shatter(packet.entity(), index);
        if (shard != null) {
            Vec3 chest = shard.add(0.0, 1.1, 0.0);
            HeavenWorldFx.burst(chest, 22, 0.32, WHITE, 0.08F, 10, 0.01, true);
            HeavenWorldFx.burst(chest, 14, 0.2, GOLD, 0.08F, 14, 0.0, false);
            HeavenWorldFx.glint(chest, 1.2F, HeavenWorldFx.WHITE, 8);
        }
        HeavenWorldFx.ring(at, 0.3F, last ? 7F : 3.5F, last ? 1.3F : 0.7F, HeavenWorldFx.GOLD, 1F, last ? 18 : 10);
        pop(at.add(0.0, 1.0, 0.0), last ? 4.0 : 2.4);
        GroundBreakerFx.begin(-(RUSH_MARK_BASE + Math.floorMod(packet.entity(), 4096) * 8 + index),
                at.x, at.y, at.z, last ? 5.5F : 3.5F, GroundBreakerFxPacket.SLAM, last ? 12 : 8);
        radial(at.add(0.0, 0.3, 0.0), last ? 24 : 8, 0.35, GOLD);
        shake(at, last ? 1.1F : 0.45F);
        if (last && near(at, 20.0)) {
            frame(0.85F);
        }
    }

    private static void roar(ClientLevel level, HeavenFxPacket packet, Vec3 at) {
        for (int i = 0; i < 3; i++) {
            int wave = i;
            later(i * 4, () -> HeavenWorldFx.ring(at, 0.5F, 10F + wave * 3F, 1.4F, HeavenWorldFx.VIOLET, 1F, 20));
        }
        HeavenWorldFx.pillar(at, 1.4F, 30F, HeavenWorldFx.VIOLET, 1F, 26, HeavenWorldFx.RISE);
        radial(at.add(0.0, 1.0, 0.0), 60, 0.5, VIOLET);
        shake(at, 1.3F);
        if (near(at, 14.0)) {
            frame(0.9F);
        }
        if (mine(packet.entity())) {
            HeavenHold.hold(level.getEntity(packet.entity()), 180);
        }
    }

    private static void highHit(Vec3 at) {
        HeavenWorldFx.ring(at, 0.2F, 1.8F, 0.45F, HeavenWorldFx.VIOLET, 1F, 8);
        HeavenWorldFx.burst(at, 8, 0.25, VIOLET, 0.07F, 8, 0.01, true);
        HeavenWorldFx.glint(at, 0.6F, HeavenWorldFx.VIOLET, 6);
    }

    private static void goldStop(HeavenFxPacket packet, Vec3 at) {
        float radius = packet.size();
        int life = Math.max(40, Math.min(400, packet.ticks()));
        HeavenWorldFx.sphere(at, radius, HeavenWorldFx.GOLD, 1F, 20, Math.min(life, GOLD_STOP_DOME_TICKS), false);
        HeavenWorldFx.ring(at, 0.5F, radius, 1.6F, HeavenWorldFx.GOLD, 1F, 24);
        later(4, () -> HeavenWorldFx.ring(at, 0.5F, radius * 0.8F, 0.7F, HeavenWorldFx.WHITE, 1F, 22));
        goldCentre = at;
        goldRadius = radius;
        goldStart = now();
        goldLength = packet.ticks() * 50L;
        shake(at, 0.6F);
    }

    private static void pulse(HeavenFxPacket packet, Vec3 at) {
        float radius = packet.size();
        HeavenWorldFx.ring(at, 0.5F, radius, 1.8F, HeavenWorldFx.GOLD, 1F, 18);
        later(4, () -> HeavenWorldFx.ring(at, 0.5F, radius * 0.85F, 0.6F, HeavenWorldFx.WHITE, 1F, 16));
        fountain(at, 30, 0.5, GOLD);
        shake(at, 0.35F);
    }

    private static void beam(ClientLevel level, HeavenFxPacket packet, Vec3 at) {
        HeavenWorldFx.beam(packet.entity(), packet.other(), at, HeavenWorldFx.GOLD, 0.35F, packet.ticks(), false);
        Entity target = packet.other() < 0 ? null : level.getEntity(packet.other());
        Vec3 heart = target == null ? at : target.position().add(0.0, target.getBbHeight() * 0.6, 0.0);
        pop(heart, 2.5);
        RandomSource random = level.getRandom();
        for (int i = 0; i < 20; i++) {
            Vec3 from = heart.add((random.nextDouble() - 0.5) * 1.2, -0.6 + random.nextDouble(),
                    (random.nextDouble() - 0.5) * 1.2);
            HeavenWorldFx.spark(from, new Vec3(0, 0.06 + random.nextDouble() * 0.05, 0), GOLD[random.nextInt(GOLD.length)],
                    0.07F, 14, -0.002, false);
        }
    }

    private static void pillar(HeavenFxPacket packet, Vec3 at) {
        HeavenWorldFx.pillar(at, 1.1F, Math.max(6F, packet.size()), HeavenWorldFx.GOLD, 1F,
                Math.max(10, packet.ticks()), HeavenWorldFx.RISE);
        HeavenWorldFx.ring(at, 0.3F, 3F, 0.8F, HeavenWorldFx.WHITE, 1F, 14);
        fountain(at, 24, 0.35, WHITE);
        if (packet.other() >= 0) {
            HeavenWorldFx.whiten(packet.other(), 30);
        }
    }

    private static void dome(HeavenFxPacket packet, Vec3 at) {
        float radius = packet.size();
        int grow = Math.max(4, packet.ticks());
        HeavenWorldFx.sphere(at, radius, HeavenWorldFx.BLACK, 1F, grow, grow + 6, true);
        HeavenWorldFx.ring(at, 0.5F, radius, 1.2F, HeavenWorldFx.VIOLET, 1F, grow);
        HeavenWorldFx.cracks(at, radius, grow + 20);
        later(grow, () -> {
            HeavenWorldFx.ring(at, radius * 0.2F, radius, 2.2F, HeavenWorldFx.WHITE, 1F, 14);
            shake(at, 0.9F);
            if (near(at, radius)) {
                frame(0.7F);
            }
        });
    }

    private static void pillars(HeavenFxPacket packet, Vec3 at) {
        float radius = packet.size();
        int ticks = Math.max(20, packet.ticks());
        if (near(at, REWRITE_SEEN)) {
            rewriteStart = now();
            rewriteLength = ticks * 50L;
            rewriteIn = REWRITE_IN_MS;
        }
        for (int k = 0; k < 8; k++) {
            double angle = k / 8.0 * Math.PI * 2.0;
            Vec3 spot = at.add(Math.cos(angle) * radius, 0.0, Math.sin(angle) * radius);
            int delay = k * 2;
            later(delay, () -> HeavenWorldFx.pillar(spot, 0.9F, 48F, HeavenWorldFx.GOLD, 1F, ticks - delay,
                    HeavenWorldFx.RISE));
        }
        HeavenWorldFx.ring(at, radius, radius, 0.6F, HeavenWorldFx.GOLD, 1F, ticks);
        HeavenWorldFx.pillar(at, 0.7F, 60F, HeavenWorldFx.WHITE, 1F, ticks, HeavenWorldFx.RISE);
    }

    private static void lift(HeavenFxPacket packet) {
        HeavenWorldFx.ringOn(packet.entity(), 0.9F, 0.5F, HeavenWorldFx.GOLD, 1F, packet.ticks());
        HeavenWorldFx.ringOn(packet.entity(), 0.55F, 0.25F, HeavenWorldFx.WHITE, 1F, packet.ticks());
    }

    private static void slam(HeavenFxPacket packet, Vec3 at) {
        float radius = packet.size();
        if (near(at, REWRITE_SEEN)) {
            shockStart = now();
        }
        HeavenWorldFx.ring(at, 1F, radius, 3F, HeavenWorldFx.WHITE, 1F, 24);
        later(3, () -> HeavenWorldFx.ring(at, 0.5F, radius * 0.8F, 2F, HeavenWorldFx.GOLD, 1F, 26));
        later(6, () -> HeavenWorldFx.ring(at, 0.5F, radius * 0.55F, 1.4F, HeavenWorldFx.GOLD, 1F, 26));
        GroundBreakerFx.begin(-(SLAM_MARK_BASE + Math.floorMod(packet.entity(), 4096)), at.x, at.y, at.z,
                Math.min(SLAM_SWELL_CAP, radius * 0.4F), GroundBreakerFxPacket.SLAM, 16);
        pop(at.add(0.0, 1.0, 0.0), 6.0);
        radial(at.add(0.0, 0.4, 0.0), 80, 0.8, GOLD);
        shake(at, 2.0F);
        if (near(at, radius)) {
            frame(1F);
        }
    }

    private static void rise(ClientLevel level, HeavenFxPacket packet, Vec3 at) {
        HeavenWorldFx.pillar(at, 1.3F, 30F, HeavenWorldFx.GOLD, 1F, 30, HeavenWorldFx.RISE);
        HeavenWorldFx.ring(at, 0.3F, 8F, 1.4F, HeavenWorldFx.GOLD, 1F, 20);
        later(3, () -> HeavenWorldFx.ring(at, 0.3F, 5F, 0.6F, HeavenWorldFx.WHITE, 1F, 16));
        radial(at.add(0.0, 1.0, 0.0), 50, 0.5, GOLD);
        fountain(at, 30, 0.55, WHITE);
        shake(at, 0.9F);
        if (mine(packet.entity())) {
            frame(0.6F);
            HeavenHold.hold(level.getEntity(packet.entity()), 150);
        }
    }

    private static void judge(HeavenFxPacket packet, Vec3 end) {
        float width = Math.max(0.2F, packet.size());
        HeavenWorldFx.beam(packet.entity(), -1, end, HeavenWorldFx.GOLD, width, 12, false);
        HeavenWorldFx.glint(end, 1.8F, HeavenWorldFx.WHITE, 10);
        HeavenWorldFx.flare(end, 3.5F, HeavenWorldFx.GOLD, 12);
        HeavenWorldFx.burst(end, 26, 0.4, GOLD, 0.09F, 10, 0.01, true);
        HeavenWorldFx.burst(end, 14, 0.3, WHITE, 0.07F, 8, 0.0, true);
        shake(end, 0.6F + width);
        if (near(end, 16.0) && width >= 0.45F) {
            frame(0.6F);
        }
    }

    private static void step(ClientLevel level, HeavenFxPacket packet, Vec3 at) {
        Vec3 chest = at.add(0.0, 1.0, 0.0);
        if (packet.size() < 0.5F) {
            STEPPED_FROM.put(packet.entity(), chest);
            HeavenWorldFx.burst(chest, 14, 0.2, GOLD, 0.07F, 8, 0.0, true);
            HeavenWorldFx.flare(chest, 2F, HeavenWorldFx.GOLD, 8);
            return;
        }
        Vec3 from = STEPPED_FROM.remove(packet.entity());
        if (from != null) {
            HeavenWorldFx.line(from, chest, HeavenWorldFx.WHITE, 0.1F, 6);
            for (int i = 1; i < 6; i++) {
                HeavenWorldFx.spark(from.lerp(chest, i / 6.0), Vec3.ZERO, GOLD[i % GOLD.length], 0.12F, 6 + i, 0.0, false);
            }
        }
        HeavenWorldFx.glint(chest, 1.1F, HeavenWorldFx.WHITE, 8);
        HeavenWorldFx.burst(chest, 10, 0.25, WHITE, 0.07F, 8, 0.0, true);
        shake(at, 0.4F);
    }

    private static void upper(HeavenFxPacket packet, Vec3 at) {
        float height = Math.max(4F, packet.size());
        HeavenWorldFx.pillar(at, 0.9F, height, HeavenWorldFx.GOLD, 1F, 18, HeavenWorldFx.RISE);
        HeavenWorldFx.ring(at, 0.3F, 3.5F, 1.0F, HeavenWorldFx.GOLD, 1F, 12);
        HeavenWorldFx.glint(at.add(0.0, 1.2, 0.0), 1.5F, HeavenWorldFx.WHITE, 10);
        HeavenWorldFx.spray(at.add(0.0, 0.6, 0.0), new Vec3(0, 1, 0), 0.35, 26, 0.55, GOLD, 0.08F, 12, 0.03, true);
        HeavenWorldFx.spray(at.add(0.0, 0.6, 0.0), new Vec3(0, 1, 0), 0.2, 12, 0.7, WHITE, 0.1F, 10, 0.03, true);
        if (packet.other() >= 0) {
            HeavenWorldFx.whiten(packet.other(), 10);
        }
        shake(at, 0.9F);
        if (near(at, 12.0)) {
            frame(0.55F);
        }
    }

    private static void knives(HeavenFxPacket packet, Vec3 at) {
        float radius = Math.abs(packet.size());
        if (packet.size() >= 0F) {
            HeavenWorldFx.flare(at, radius * 2.4F, HeavenWorldFx.GOLD, 14);
            HeavenWorldFx.gather(at, radius * 1.4, 18, GOLD, 0.07F, 8);
            return;
        }
        HeavenWorldFx.flare(at, radius * 3F, HeavenWorldFx.WHITE, 8);
        HeavenWorldFx.glint(at, 1.4F, HeavenWorldFx.WHITE, 8);
        shake(at, 0.5F);
    }

    private static void unmade(ClientLevel level, HeavenFxPacket packet, Vec3 at) {
        RandomSource random = level.getRandom();
        float height = Math.max(0.5F, packet.size());
        for (int i = 0; i < 40; i++) {
            Vec3 from = at.add((random.nextDouble() - 0.5) * 0.9, random.nextDouble() * height,
                    (random.nextDouble() - 0.5) * 0.9);
            HeavenWorldFx.spark(from, new Vec3((random.nextDouble() - 0.5) * 0.04, 0.05 + random.nextDouble() * 0.08,
                    (random.nextDouble() - 0.5) * 0.04), i % 3 == 0 ? VIOLET[0] : WHITE[i % WHITE.length],
                    0.07F + random.nextFloat() * 0.05F, 14 + random.nextInt(10), -0.003, i % 2 == 0);
        }
        HeavenWorldFx.glint(at.add(0.0, height * 0.6, 0.0), 1.4F, HeavenWorldFx.WHITE, 10);
        HeavenWorldFx.flare(at.add(0.0, height * 0.5, 0.0), height * 1.6F, HeavenWorldFx.VIOLET, 12);
        HeavenWorldFx.pillar(at, 0.6F, height + 2F, HeavenWorldFx.WHITE, 1F, 12, HeavenWorldFx.RISE);
    }

    private static void pop(Vec3 at, double scale) {
        HeavenWorldFx.glint(at, (float) Math.min(2.4, 0.3 * scale), HeavenWorldFx.WHITE, 8);
        HeavenWorldFx.flare(at, (float) Math.min(6.0, scale * 0.9), HeavenWorldFx.GOLD, 10);
    }

    private static void radial(Vec3 at, int count, double speed, int[] colours) {
        RandomSource random = Minecraft.getInstance().level.getRandom();
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double pace = speed * (0.6 + random.nextDouble() * 0.6);
            HeavenWorldFx.spark(at, new Vec3(Math.cos(angle) * pace, (random.nextDouble() - 0.3) * speed * 0.3,
                            Math.sin(angle) * pace), colours[random.nextInt(colours.length)],
                    0.07F + random.nextFloat() * 0.05F, 8 + random.nextInt(6), 0.01, true);
        }
    }

    private static void fountain(Vec3 at, int count, double speed, int[] colours) {
        HeavenWorldFx.spray(at.add(0.0, 0.2, 0.0), new Vec3(0, 1, 0), 0.45, count, speed, colours, 0.08F, 12, 0.025, true);
    }

    private static void frame(float strength) {
        ImpactFrame.begin(firstPerson() ? strength * FIRST_PERSON_FLASH : strength);
    }

    public static boolean firstPerson() {
        return Minecraft.getInstance().options.getCameraType().isFirstPerson();
    }

    private static void shake(Vec3 at, float strength) {
        if (JojohaClientConfig.cameraMotion()) {
            CombatShake.add(at.x, at.y, at.z, strength);
        }
    }

    private static boolean mine(int entityId) {
        Player player = Minecraft.getInstance().player;
        return player != null && player.getId() == entityId;
    }

    private static boolean near(Vec3 at, double range) {
        Player player = Minecraft.getInstance().player;
        return player != null && player.distanceToSqr(at) <= range * range;
    }

    public static float erasure() {
        if (erasureStart == Long.MIN_VALUE || Minecraft.getInstance().level == null) {
            return 0F;
        }
        long elapsed = now() - erasureStart;
        if (elapsed < 0L || elapsed >= erasureLength + FADE_OUT_MS) {
            erasureStart = Long.MIN_VALUE;
            return 0F;
        }
        return envelope(elapsed, erasureLength, erasureIn, FADE_OUT_MS);
    }

    public static float flash() {
        if (flashStart == Long.MIN_VALUE || Minecraft.getInstance().level == null) {
            return 0F;
        }
        long elapsed = now() - flashStart;
        if (elapsed < 0L || elapsed >= flashLength) {
            flashStart = Long.MIN_VALUE;
            return 0F;
        }
        if (elapsed < FLASH_HOLD_MS) {
            return 1F;
        }
        return smooth(1F - (elapsed - FLASH_HOLD_MS) / (float) Math.max(1L, flashLength - FLASH_HOLD_MS));
    }

    public static float sepia() {
        if (sepiaStart == Long.MIN_VALUE || Minecraft.getInstance().level == null) {
            return 0F;
        }
        long elapsed = now() - sepiaStart;
        if (elapsed < 0L || elapsed >= sepiaLength + SEPIA_OUT_MS) {
            sepiaStart = Long.MIN_VALUE;
            return 0F;
        }
        return envelope(elapsed, sepiaLength, SEPIA_IN_MS, SEPIA_OUT_MS);
    }

    public static float gold() {
        Minecraft minecraft = Minecraft.getInstance();
        if (goldStart == Long.MIN_VALUE || minecraft.level == null || minecraft.player == null) {
            return 0F;
        }
        long elapsed = now() - goldStart;
        if (elapsed < 0L || elapsed >= goldLength + GOLD_OUT_MS) {
            goldStart = Long.MIN_VALUE;
            return 0F;
        }
        if (minecraft.player.distanceToSqr(goldCentre) > goldRadius * goldRadius) {
            return 0F;
        }
        return GOLD_STOP_PEAK * envelope(elapsed, goldLength, GOLD_IN_MS, GOLD_OUT_MS);
    }

    public static float realm() {
        if (realmStart == Long.MIN_VALUE || Minecraft.getInstance().level == null) {
            return 0F;
        }
        long elapsed = now() - realmStart;
        if (elapsed < 0L || elapsed >= realmWaveMs + realmHoldMs + REALM_CLOSE_MS) {
            realmStart = Long.MIN_VALUE;
            return 0F;
        }
        return 1F;
    }

    public static float realmFront() {
        if (realmStart == Long.MIN_VALUE) {
            return 0F;
        }
        long elapsed = now() - realmStart;
        if (elapsed < realmWaveMs) {
            return realmRadius * smooth(elapsed / (float) realmWaveMs);
        }
        long closing = elapsed - realmWaveMs - realmHoldMs;
        if (closing > 0L) {
            return realmRadius * smooth(1F - closing / (float) REALM_CLOSE_MS);
        }
        return realmRadius;
    }

    public static Vec3 realmCentre() {
        return realmCentre;
    }

    public static float rewrite() {
        if (rewriteStart == Long.MIN_VALUE || Minecraft.getInstance().level == null) {
            return 0F;
        }
        long elapsed = now() - rewriteStart;
        if (elapsed < 0L || elapsed >= rewriteLength + REWRITE_OUT_MS) {
            rewriteStart = Long.MIN_VALUE;
            return 0F;
        }
        float settle = elapsed < rewriteIn ? 1F
                : Mth.lerp(smooth((elapsed - rewriteIn) / (float) REWRITE_SETTLE_MS), 1F, REWRITE_HOLD_SHARE);
        return REWRITE_PEAK * settle * envelope(elapsed, rewriteLength, rewriteIn, REWRITE_OUT_MS);
    }

    public static float shock() {
        if (shockStart == Long.MIN_VALUE) {
            return 1F;
        }
        long elapsed = now() - shockStart;
        if (elapsed < 0L || elapsed >= SHOCK_MS) {
            shockStart = Long.MIN_VALUE;
            return 1F;
        }
        return elapsed / (float) SHOCK_MS;
    }

    public static float pulse() {
        if (pulseStart == Long.MIN_VALUE) {
            return 0F;
        }
        long elapsed = now() - pulseStart;
        if (elapsed < 0L || elapsed >= PULSE_MS) {
            pulseStart = Long.MIN_VALUE;
            return 0F;
        }
        return smooth(1F - elapsed / (float) PULSE_MS);
    }

    private static float smooth(float value) {
        float t = Mth.clamp(value, 0F, 1F);
        return t * t * (3F - 2F * t);
    }

    private static float envelope(long elapsed, long length, long in, long out) {
        if (elapsed < in) {
            return smooth(elapsed / (float) in);
        }
        if (elapsed < length) {
            return 1F;
        }
        return smooth(1F - (elapsed - length) / (float) out);
    }

    private static long now() {
        return System.nanoTime() / 1_000_000L;
    }
}
