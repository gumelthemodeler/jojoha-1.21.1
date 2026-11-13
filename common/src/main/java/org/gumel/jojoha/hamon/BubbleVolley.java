package org.gumel.jojoha.hamon;

import dev.architectury.event.events.common.TickEvent;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.accessory.Accessories;
import org.gumel.jojoha.combat.DamageScale;
import org.gumel.jojoha.combat.HitWindow;
import org.gumel.jojoha.combat.fx.ImpactSpec;
import org.gumel.jojoha.combat.fx.Impacts;
import org.gumel.jojoha.registry.ModItems;
import org.gumel.jojoha.registry.ModRegistries;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class BubbleVolley {
    private static final double HIT_RADIUS = 0.45;

    private static final int SLASHES_MIN = 2;
    private static final int SLASHES_MAX = 4;
    private static final int SLASH_STAGGER = 1;
    private static final int SLASH_SOUND_EVERY = 4;

    private static final class Bubble {
        Vec3 at;
        Vec3 velocity;
        Vec3 start;
        double travelled;

        final Set<Integer> through = new HashSet<>();

        Bubble(Vec3 at, Vec3 velocity) {
            this.at = at;
            this.velocity = velocity;
        }
    }

    private static final class Volley {
        final UUID caster;
        final ServerLevel level;
        final BubbleFlight.Kind kind;
        final float damage;
        final boolean pellets;
        final boolean slashes;
        final List<Bubble> bubbles;

        final Set<Integer> cut = new HashSet<>();

        Volley(ServerPlayer caster, BubbleFlight.Kind kind, float damage, boolean pellets, boolean slashes,
               List<Bubble> bubbles) {
            this.caster = caster.getUUID();
            this.level = caster.serverLevel();
            this.kind = kind;
            this.damage = damage;
            this.pellets = pellets;
            this.slashes = slashes;
            this.bubbles = bubbles;
        }
    }

    private static final List<Volley> IN_FLIGHT = new ArrayList<>();

    private BubbleVolley() {
    }

    public static void init() {
        TickEvent.SERVER_POST.register(server -> tick());
    }

    public static boolean gloved(ServerPlayer player) {
        if (Accessories.wearing(player,
                ModItems.BUBBLE_GLOVES.get())) {
            return true;
        }
        player.displayClientMessage(
                Component.translatable("message.jojoha.hamon.no_bubble_gloves"), true);
        return false;
    }

    public static void launch(ServerPlayer player, BubbleFlight.Kind kind, int count, double speed,
                              float coneDegrees, float damage, boolean pellets, int muzzle, boolean slashes) {
        RandomSource random = player.serverLevel().getRandom();
        Vec3 look = player.getLookAngle();

        List<Vec3> velocities = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            velocities.add(inCone(look, coneDegrees, random)
                    .scale(speed * (0.9 + random.nextDouble() * 0.2)));
        }
        fire(player, kind, velocities, null, damage, pellets, muzzle, slashes);
    }

    public static void launchArc(ServerPlayer player, BubbleFlight.Kind kind, int count,
                                 double speed, float arcDegrees, float thicknessDegrees,
                                 float tiltDegrees, float damage, boolean pellets, long seed) {
        RandomSource random = player.serverLevel().getRandom();
        float yaw = player.getYRot() * ((float) Math.PI / 180F);
        BubbleSheet.Frame frame = BubbleSheet.Frame.of(player.getEyePosition(), player.getLookAngle(), yaw);

        List<Vec3> velocities = new ArrayList<>(count);
        List<Vec3> starts = new ArrayList<>(count);
        for (BubbleSheet.Slot slot : BubbleSheet.slots(seed, count, arcDegrees)) {
            double theta = Math.toRadians(slot.theta());
            double phi = Math.toRadians(-tiltDegrees
                    + (random.nextDouble() - 0.5) * 2.0 * thicknessDegrees);

            Vec3 flat = frame.look().scale(Math.cos(theta)).add(frame.across().scale(Math.sin(theta)));
            Vec3 way = flat.scale(Math.cos(phi)).add(frame.up().scale(Math.sin(phi))).normalize();
            velocities.add(way.scale(speed * (0.96 + random.nextDouble() * 0.08)));
            starts.add(frame.point(slot.u(arcDegrees), slot.v(), 1.0, 0.0));
        }
        fire(player, kind, velocities, starts, damage, pellets, 0, false);
    }

    private static void fire(ServerPlayer player, BubbleFlight.Kind kind, List<Vec3> velocities,
                             List<Vec3> starts, float damage, boolean pellets, int muzzle, boolean slashes) {
        ServerLevel level = player.serverLevel();
        Vec3 from = HamonFx.mainHand(player);
        SimpleParticleType type = kind == BubbleFlight.Kind.SHOT
                ? ModRegistries.BUBBLE_SHOT.get()
                : ModRegistries.BUBBLE_DRIFT.get();

        List<Bubble> bubbles = new ArrayList<>(velocities.size());
        for (int i = 0; i < velocities.size(); i++) {
            Vec3 velocity = velocities.get(i);
            Bubble bubble = new Bubble(from, velocity);
            Vec3 shown = from;
            if (starts != null) {
                HitResult blocked = level.clip(new ClipContext(from, starts.get(i), ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE, player));
                bubble.start = blocked.getType() == HitResult.Type.MISS ? starts.get(i) : blocked.getLocation();
                shown = bubble.start;
            }
            bubbles.add(bubble);

            level.sendParticles(type, shown.x, shown.y, shown.z, 0,
                    velocity.x, velocity.y, velocity.z, 1.0);
        }

        if (muzzle > 0) {
            level.sendParticles(film(0.2F, 1.2F), from.x, from.y, from.z, muzzle, 0.3, 0.3, 0.3, 0.02);
            level.sendParticles(film(0.8F, 1.2F), from.x, from.y, from.z, muzzle, 0.3, 0.3, 0.3, 0.02);
            level.sendParticles(ParticleTypes.BUBBLE_POP, from.x, from.y, from.z,
                    Math.max(1, muzzle / 2), 0.25, 0.25, 0.25, 0.0);
        }

        IN_FLIGHT.add(new Volley(player, kind, damage, pellets, slashes, bubbles));
    }

    private static Vec3 inCone(Vec3 axis, float degrees, RandomSource random) {
        Vec3 forward = axis.normalize();
        double cosMax = Math.cos(Math.toRadians(degrees));
        double cos = 1.0 - random.nextDouble() * (1.0 - cosMax);
        double sin = Math.sqrt(Math.max(0.0, 1.0 - cos * cos));
        double spin = random.nextDouble() * Math.PI * 2.0;

        Vec3 side = Math.abs(forward.y) > 0.99 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 right = forward.cross(side).normalize();
        Vec3 up = right.cross(forward).normalize();

        return forward.scale(cos)
                .add(right.scale(Math.cos(spin) * sin))
                .add(up.scale(Math.sin(spin) * sin));
    }

    private static void tick() {
        for (int v = IN_FLIGHT.size() - 1; v >= 0; v--) {
            Volley volley = IN_FLIGHT.get(v);
            ServerPlayer caster = volley.level.getServer().getPlayerList().getPlayer(volley.caster);

            if (caster == null) {
                IN_FLIGHT.remove(v);
                continue;
            }

            advance(volley, caster);
            if (volley.bubbles.isEmpty()) {
                IN_FLIGHT.remove(v);
            }
        }
    }

    private static void advance(Volley volley, ServerPlayer caster) {
        ServerLevel level = volley.level;

        Map<LivingEntity, Integer> struck = new HashMap<>();

        for (int i = volley.bubbles.size() - 1; i >= 0; i--) {
            Bubble bubble = volley.bubbles.get(i);
            Vec3 was = bubble.at;
            Vec3 next = (bubble.start != null ? bubble.start : was).add(bubble.velocity);
            bubble.start = null;

            HitResult hit = level.clip(new ClipContext(was, next, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, caster));
            if (hit.getType() != HitResult.Type.MISS) {
                next = hit.getLocation();
                touch(level, volley, caster, bubble, was, next, struck);
                volley.bubbles.remove(i);
                continue;
            }

            touch(level, volley, caster, bubble, was, next, struck);

            bubble.travelled += bubble.velocity.length();
            bubble.at = next;
            bubble.velocity = bubble.velocity.scale(volley.kind.drag());

            if (bubble.travelled >= BubbleFlight.MAX_TRAVEL
                    || bubble.velocity.lengthSqr() < BubbleFlight.MIN_SPEED_SQ) {
                volley.bubbles.remove(i);
            }
        }

        for (Map.Entry<LivingEntity, Integer> entry : struck.entrySet()) {
            LivingEntity target = entry.getKey();
            int bubbles = entry.getValue();
            float damage = volley.pellets ? volley.damage * bubbles : volley.damage;

            if (volley.pellets) {
                HitWindow.open(target);
            }
            target.hurt(level.damageSources().playerAttack(caster),
                    DamageScale.hamon(caster, target, HamonCombat.scaled(caster, damage)));
            Impacts.landed(level, target, target.position().subtract(caster.position()),
                    ImpactSpec.LIGHT.silent(), Impacts.HAMON);

            burst(level, target.position().add(0.0, target.getBbHeight() * 0.5, 0.0), bubbles);
            if (volley.slashes) {
                slash(level, caster, target, bubbles);
            }
        }
    }

    private static void touch(ServerLevel level, Volley volley, ServerPlayer caster, Bubble bubble,
                              Vec3 was, Vec3 next, Map<LivingEntity, Integer> struck) {
        AABB sweep = new AABB(was, next).inflate(HIT_RADIUS + 1.0);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, sweep,
                entity -> entity != caster && entity.isAlive())) {
            AABB body = target.getBoundingBox().inflate(HIT_RADIUS);
            if (!body.contains(was) && body.clip(was, next).isEmpty()) {
                continue;
            }

            if (volley.pellets) {
                if (!bubble.through.add(target.getId())) {
                    continue;
                }
            } else if (!volley.cut.add(target.getId())) {
                continue;
            }
            struck.merge(target, 1, Integer::sum);
        }
    }

    private static void slash(ServerLevel level, ServerPlayer caster, LivingEntity target, int bubbles) {
        RandomSource random = level.getRandom();
        Vec3 toward = caster.position().subtract(target.position()).multiply(1.0, 0.0, 1.0);
        toward = toward.lengthSqr() < 1.0E-4 ? caster.getLookAngle().reverse().multiply(1.0, 0.0, 1.0) : toward;
        toward = toward.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : toward.normalize();
        Vec3 side = new Vec3(-toward.z, 0.0, toward.x);
        double width = target.getBbWidth();
        double height = target.getBbHeight();
        double size = Mth.clamp((width + height) / 2.6, 0.45, 1.4);
        int cuts = Math.min(SLASHES_MAX, SLASHES_MIN + bubbles);
        for (int k = 0; k < cuts; k++) {
            Vec3 at = target.position().add(toward.scale(width * 0.5 + 0.05))
                    .add(side.scale((random.nextDouble() - 0.5) * width * 0.8))
                    .add(0.0, height * (0.25 + random.nextDouble() * 0.55), 0.0);
            double slant = (random.nextBoolean() ? 1.0 : -1.0) * Math.toRadians(20.0 + random.nextDouble() * 50.0);
            double length = size * (0.8 + random.nextDouble() * 0.4);
            level.sendParticles(ModRegistries.BUBBLE_CUT.get(), at.x, at.y, at.z, 0,
                    slant, length, k * SLASH_STAGGER, 1.0);
        }
        if (level.getGameTime() % SLASH_SOUND_EVERY < 2) {
            level.playSound(null, target.getX(), target.getY(0.5), target.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP,
                    SoundSource.PLAYERS, 0.35F, 1.6F + random.nextFloat() * 0.3F);
        }
    }

    private static void burst(ServerLevel level, Vec3 at, int bubbles) {
        int motes = Math.min(30, 6 + bubbles * 2);
        level.sendParticles(film(0.3F, 1.3F), at.x, at.y, at.z, motes, 0.3, 0.4, 0.3, 0.06);
        level.sendParticles(film(0.7F, 1.3F), at.x, at.y, at.z, motes, 0.3, 0.4, 0.3, 0.06);
        level.sendParticles(ParticleTypes.BUBBLE_POP, at.x, at.y, at.z,
                Math.min(12, 3 + bubbles), 0.3, 0.4, 0.3, 0.0);
    }

    private static DustColorTransitionOptions film(float at, float size) {
        return new DustColorTransitionOptions(
                new Vector3f(
                        BubbleFlight.mix(BubbleFlight.PINK_R, BubbleFlight.CYAN_R, at),
                        BubbleFlight.mix(BubbleFlight.PINK_G, BubbleFlight.CYAN_G, at),
                        BubbleFlight.mix(BubbleFlight.PINK_B, BubbleFlight.CYAN_B, at)),
                new Vector3f(
                        BubbleFlight.mix(BubbleFlight.PINK_R, BubbleFlight.CYAN_R, 1F - at),
                        BubbleFlight.mix(BubbleFlight.PINK_G, BubbleFlight.CYAN_G, 1F - at),
                        BubbleFlight.mix(BubbleFlight.PINK_B, BubbleFlight.CYAN_B, 1F - at)),
                size);
    }
}
