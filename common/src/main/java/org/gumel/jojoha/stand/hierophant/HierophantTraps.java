package org.gumel.jojoha.stand.hierophant;

import dev.architectury.event.events.common.TickEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.gumel.jojoha.combat.DamageScale;
import org.gumel.jojoha.combat.HitWindow;
import org.gumel.jojoha.combat.Stun;
import org.gumel.jojoha.registry.ModEffects;
import org.gumel.jojoha.stand.StandEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class HierophantTraps {
    private static final int RESEND_TICKS = 40;

    public static final int ENTANGLE_TICKS = 50;
    private static final float WEB_DAMAGE = 3F;
    private static final float WIRE_DAMAGE = 4F;
    private static final int PINS = 5;
    private static final double PIN_REACH = 2.2;

    private static final int WEB_RECATCH_TICKS = 100;

    private static final int WEB_STRINGS = 14;
    private static final int FIELD_STRINGS = 44;
    private static final double FIELD_SHORTEST = 0.7;

    private static final int MOST_WEBS = 1;
    private static final int MOST_WIRES = 3;
    private static final int MOST_FIELDS = 1;
    private static final int MOST_ALARMS = 1;

    private static final int ALARM_STRINGS = 10;
    private static final double ALARM_HEIGHT = 0.5;
    private static final double ALARM_REACH_UP = 2.0;
    private static final int ALARM_EVERY = 10;
    private static final int ALARM_REST = 100;
    private static final int ALARM_GLOW = 60;

    private static final int FIELD_VOLLEY = 4;
    private static final float FIELD_DAMAGE = 3F;
    private static final int FIELD_TARGET_REST = 25;
    private static final float FIELD_SPEED = 2.2F;
    private static final double FIELD_NEAREST_SHOT = 3.0;

    private static final List<Trap> TRAPS = new ArrayList<>();

    private HierophantTraps() {
    }

    public static void init() {
        TickEvent.SERVER_LEVEL_POST.register(level -> {
            if (level instanceof ServerLevel serverLevel) {
                tick(serverLevel);
            }
        });
    }

    public static void web(ServerLevel level, ServerPlayer owner, Vec3 centre, double radius, int ticks) {
        add(new Web(owner.getUUID(), Tendrils.tintOf(owner), level, centre, radius, level.getGameTime() + ticks),
                MOST_WEBS);
    }

    public static void wire(ServerLevel level, ServerPlayer owner, Vec3 from, Vec3 to, int ticks) {
        add(new Wire(owner.getUUID(), Tendrils.tintOf(owner), level, from, to, level.getGameTime() + ticks),
                MOST_WIRES);
    }

    public static void field(ServerLevel level, ServerPlayer owner, Vec3 centre, double radius, int ticks) {
        add(new Field(owner.getUUID(), Tendrils.tintOf(owner), level, centre, radius, level.getGameTime() + ticks),
                MOST_FIELDS);
    }

    public static void alarm(ServerLevel level, ServerPlayer owner, Vec3 centre, double radius, int ticks) {
        add(new Alarm(owner.getUUID(), Tendrils.tintOf(owner), level, centre, radius, level.getGameTime() + ticks),
                MOST_ALARMS);
    }

    public static boolean hasField(Player owner) {
        return TRAPS.stream().anyMatch(trap -> trap instanceof Field && trap.owner.equals(owner.getUUID()));
    }

    private static void add(Trap trap, int most) {
        List<Trap> same = new ArrayList<>();
        for (Trap other : TRAPS) {
            if (other.getClass() == trap.getClass() && other.owner.equals(trap.owner)) {
                same.add(other);
            }
        }
        while (same.size() >= most) {
            Trap oldest = same.remove(0);
            oldest.clear();
            TRAPS.remove(oldest);
        }
        TRAPS.add(trap);
        trap.draw();
    }

    private static void tick(ServerLevel level) {
        if (TRAPS.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        for (Iterator<Trap> it = TRAPS.iterator(); it.hasNext(); ) {
            Trap trap = it.next();
            if (trap.level != level) {
                continue;
            }
            if (now >= trap.expiresAt || trap.tick(now)) {
                trap.clear();
                it.remove();
            } else if ((now - trap.laidAt) % RESEND_TICKS == 0) {
                trap.draw();
            }
        }
    }

    static boolean prey(Entity entity, UUID owner) {
        if (!(entity instanceof LivingEntity living) || !living.isAlive() || entity instanceof ArmorStand
                || entity instanceof StandEntity) {
            return false;
        }
        if (entity.getUUID().equals(owner) || entity.isSpectator()) {
            return false;
        }
        return !(entity instanceof TamableAnimal pet && owner.equals(pet.getOwnerUUID()));
    }

    static void entangle(ServerLevel level, LivingEntity body, Vec3 from, float damage, UUID owner, int tint) {
        body.addEffect(new MobEffectInstance(ModEffects.stun(), ENTANGLE_TICKS,
                Stun.TIER_TWO, false, false, true));
        body.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ENTANGLE_TICKS, 3, false, false, true));
        body.setDeltaMovement(0.0, Math.min(0.0, body.getDeltaMovement().y), 0.0);
        body.hurtMarked = true;
        Player user = level.getPlayerByUUID(owner);
        HitWindow.open(body);
        body.hurt(user != null ? level.damageSources().playerAttack(user) : level.damageSources().magic(), DamageScale.stand(user, body, damage));

        Vec3 at = body.position().add(0.0, body.getBbHeight() * 0.5, 0.0);
        Tendrils.line(level, Tendrils.newKey(), from, at, Tendrils.THIN, ENTANGLE_TICKS, tint);
        RandomSource random = level.getRandom();
        for (int i = 0; i < PINS; i++) {
            double a = random.nextDouble() * Math.PI * 2.0;
            double reach = PIN_REACH * (0.6 + random.nextDouble() * 0.4);
            Vec3 pin = new Vec3(body.getX() + Math.cos(a) * reach, body.getY() + 0.02, body.getZ() + Math.sin(a) * reach);
            Vec3 on = body.position().add(0.0, body.getBbHeight() * (0.2 + random.nextDouble() * 0.6), 0.0);
            Tendrils.line(level, Tendrils.newKey(), pin, on, Tendrils.THIN, ENTANGLE_TICKS, tint);
        }
        level.playSound(null, body.blockPosition(), SoundEvents.VINE_STEP, SoundSource.PLAYERS, 1.0F, 0.6F);
    }

    static Vec3 nearestOn(Vec3 a, Vec3 b, Vec3 to) {
        Vec3 ab = b.subtract(a);
        double lengthSqr = ab.lengthSqr();
        double t = lengthSqr < 1.0E-8 ? 0.0 : Math.max(0.0, Math.min(1.0, to.subtract(a).dot(ab) / lengthSqr));
        return a.add(ab.scale(t));
    }

    private abstract static class Trap {
        final UUID owner;
        final int tint;
        final ServerLevel level;
        final long expiresAt;
        final long laidAt;
        final List<Integer> keys = new ArrayList<>();

        Trap(UUID owner, int tint, ServerLevel level, long expiresAt) {
            this.owner = owner;
            this.tint = tint;
            this.level = level;
            this.expiresAt = expiresAt;
            this.laidAt = level.getGameTime();
        }

        abstract boolean tick(long now);

        abstract void draw();

        abstract Vec3 near();

        int left() {
            return (int) Math.max(1, expiresAt - level.getGameTime());
        }

        void clear() {
            for (int key : keys) {
                Tendrils.clear(level, key, near());
            }
        }

        int key(int i) {
            while (keys.size() <= i) {
                keys.add(Tendrils.newKey());
            }
            return keys.get(i);
        }

        void drawStrings(List<Vec3[]> strings, float width) {
            int left = left();
            for (int i = 0; i < strings.size(); i++) {
                Tendrils.line(level, key(i), strings.get(i)[0], strings.get(i)[1], width, left, tint);
            }
        }
    }

    private static final class Web extends Trap {
        private final Vec3 centre;
        private final double radius;
        private final List<Vec3[]> strings = new ArrayList<>();
        private final Map<UUID, Long> caught = new HashMap<>();

        Web(UUID owner, int tint, ServerLevel level, Vec3 centre, double radius, long expiresAt) {
            super(owner, tint, level, expiresAt);
            this.centre = centre;
            this.radius = radius;
            RandomSource random = RandomSource.create(level.getGameTime() ^ owner.getLeastSignificantBits());
            Vec3 floor = centre.add(0.0, 0.04, 0.0);
            for (int i = 0; i < WEB_STRINGS; i++) {
                double a = random.nextDouble() * Math.PI * 2.0;
                double b = a + Math.PI * (0.55 + random.nextDouble() * 0.9);
                double ra = radius * (0.75 + random.nextDouble() * 0.25);
                double rb = radius * (0.75 + random.nextDouble() * 0.25);
                strings.add(new Vec3[]{floor.add(Math.cos(a) * ra, random.nextDouble() * 0.05, Math.sin(a) * ra),
                        floor.add(Math.cos(b) * rb, random.nextDouble() * 0.05, Math.sin(b) * rb)});
            }
        }

        @Override
        boolean tick(long now) {
            AABB area = new AABB(centre, centre).inflate(radius, 1.0, radius);
            for (LivingEntity body : level.getEntitiesOfClass(LivingEntity.class, area, e -> prey(e, owner))) {
                double dx = body.getX() - centre.x;
                double dz = body.getZ() - centre.z;
                Long free = caught.get(body.getUUID());
                if (dx * dx + dz * dz > radius * radius || (free != null && now < free)) {
                    continue;
                }
                caught.put(body.getUUID(), now + WEB_RECATCH_TICKS);
                Vec3[] nearest = strings.get(0);
                double best = Double.MAX_VALUE;
                for (Vec3[] string : strings) {
                    double d = nearestOn(string[0], string[1], body.position()).distanceToSqr(body.position());
                    if (d < best) {
                        best = d;
                        nearest = string;
                    }
                }
                entangle(level, body, nearestOn(nearest[0], nearest[1], body.position()), WEB_DAMAGE, owner, tint);
            }
            return false;
        }

        @Override
        void draw() {
            drawStrings(strings, Tendrils.THIN);
        }

        @Override
        Vec3 near() {
            return centre;
        }
    }

    private static final class Wire extends Trap {
        private final Vec3 from;
        private final Vec3 to;

        Wire(UUID owner, int tint, ServerLevel level, Vec3 from, Vec3 to, long expiresAt) {
            super(owner, tint, level, expiresAt);
            this.from = from;
            this.to = to;
        }

        @Override
        boolean tick(long now) {
            AABB span = new AABB(from, to).inflate(0.3);
            for (LivingEntity body : level.getEntitiesOfClass(LivingEntity.class, span, e -> prey(e, owner))) {
                if (body.getBoundingBox().inflate(0.1).clip(from, to).isPresent()
                        || body.getBoundingBox().contains(from) || body.getBoundingBox().contains(to)) {
                    entangle(level, body, from, WIRE_DAMAGE, owner, tint);
                    Vec3 at = body.position().add(0.0, body.getBbHeight() * 0.5, 0.0);
                    Tendrils.line(level, Tendrils.newKey(), to, at, Tendrils.THIN, ENTANGLE_TICKS, tint);
                    return true;
                }
            }
            return false;
        }

        @Override
        void draw() {
            Tendrils.line(level, key(0), from, to, Tendrils.THIN, left(), tint);
        }

        @Override
        Vec3 near() {
            return from;
        }
    }

    private static final class Alarm extends Trap {
        private final Vec3 centre;
        private final double radius;
        private final List<Vec3[]> strings = new ArrayList<>();
        private final Map<UUID, Long> rested = new HashMap<>();

        Alarm(UUID owner, int tint, ServerLevel level, Vec3 centre, double radius, long expiresAt) {
            super(owner, tint, level, expiresAt);
            this.centre = centre;
            this.radius = radius;
            Vec3 ring = centre.add(0.0, ALARM_HEIGHT, 0.0);
            for (int i = 0; i < ALARM_STRINGS; i++) {
                double a = Math.PI * 2.0 * i / ALARM_STRINGS;
                double b = Math.PI * 2.0 * (i + 1) / ALARM_STRINGS;
                strings.add(new Vec3[]{ring.add(Math.cos(a) * radius, 0.0, Math.sin(a) * radius),
                        ring.add(Math.cos(b) * radius, 0.0, Math.sin(b) * radius)});
            }
        }

        @Override
        boolean tick(long now) {
            if (now % ALARM_EVERY != 0) {
                return false;
            }
            ServerPlayer user = level.getServer().getPlayerList().getPlayer(owner);
            if (user == null) {
                return false;
            }
            AABB area = new AABB(centre, centre).inflate(radius, ALARM_REACH_UP, radius);
            for (LivingEntity body : level.getEntitiesOfClass(LivingEntity.class, area,
                    e -> prey(e, owner) && (e instanceof Enemy
                            || e instanceof Player))) {
                double dx = body.getX() - centre.x;
                double dz = body.getZ() - centre.z;
                Long rest = rested.get(body.getUUID());
                if (dx * dx + dz * dz > radius * radius || (rest != null && now < rest)) {
                    continue;
                }
                rested.put(body.getUUID(), now + ALARM_REST);
                body.addEffect(new MobEffectInstance(MobEffects.GLOWING, ALARM_GLOW, 0, false, false, false));
                user.playNotifySound(SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 1.0F, 1.4F);
                int away = user.level() == level ? (int) Math.round(user.position().distanceTo(body.position())) : -1;
                user.displayClientMessage(away < 0
                        ? Component.translatable("message.jojoha.tendril_work.alarm_far",
                        body.getDisplayName())
                        : Component.translatable("message.jojoha.tendril_work.alarm",
                        body.getDisplayName(), away), true);
            }
            return false;
        }

        @Override
        void draw() {
            drawStrings(strings, Tendrils.THIN);
        }

        @Override
        Vec3 near() {
            return centre;
        }
    }

    private static final class Field extends Trap {
        private final Vec3 centre;
        private final double radius;
        private final List<Vec3[]> strings = new ArrayList<>();
        private final Map<UUID, Long> rested = new HashMap<>();

        Field(UUID owner, int tint, ServerLevel level, Vec3 centre, double radius, long expiresAt) {
            super(owner, tint, level, expiresAt);
            this.centre = centre;
            this.radius = radius;
            RandomSource random = RandomSource.create(level.getGameTime() ^ owner.getMostSignificantBits());
            int tries = 0;
            while (strings.size() < FIELD_STRINGS && tries++ < FIELD_STRINGS * 20) {
                Vec3 a = anchor(random);
                Vec3 b = anchor(random);
                if (a.distanceTo(b) >= radius * FIELD_SHORTEST) {
                    strings.add(new Vec3[]{a, b});
                }
            }
        }

        private Vec3 anchor(RandomSource random) {
            Vec3 direction = new Vec3(random.nextGaussian(), random.nextGaussian() * 0.7, random.nextGaussian());
            if (direction.lengthSqr() < 1.0E-4) {
                direction = new Vec3(1.0, 0.0, 0.0);
            }
            Vec3 edge = centre.add(direction.normalize().scale(radius * (0.8 + random.nextDouble() * 0.2)));
            BlockHitResult hit = level.clip(new ClipContext(centre, edge, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, CollisionContext.empty()));
            return hit.getType() == HitResult.Type.MISS ? edge : hit.getLocation();
        }

        @Override
        boolean tick(long now) {
            if (now % 5 != 0) {
                return false;
            }
            Player user = level.getPlayerByUUID(owner);
            if (user == null) {
                return false;
            }
            AABB area = new AABB(centre, centre).inflate(radius);
            for (LivingEntity body : level.getEntitiesOfClass(LivingEntity.class, area, e -> prey(e, owner))) {
                Vec3 at = body.position().add(0.0, body.getBbHeight() * 0.5, 0.0);
                Long rest = rested.get(body.getUUID());
                if (at.distanceToSqr(centre) > radius * radius || (rest != null && now < rest)) {
                    continue;
                }
                rested.put(body.getUUID(), now + FIELD_TARGET_REST);
                erupt(user, at);
            }
            return false;
        }

        private void erupt(Player user, Vec3 at) {
            RandomSource random = level.getRandom();
            int fired = 0;
            for (int tries = 0; tries < FIELD_VOLLEY * 4 && fired < FIELD_VOLLEY; tries++) {
                Vec3[] string = strings.get(random.nextInt(strings.size()));
                Vec3 from = nearestOn(string[0], string[1], at);
                if (from.distanceTo(at) < FIELD_NEAREST_SHOT) {
                    from = string[0].lerp(string[1], random.nextDouble());
                    if (from.distanceTo(at) < FIELD_NEAREST_SHOT) {
                        continue;
                    }
                }
                EmeraldSplash.fire(level, user, from, at.subtract(from).normalize(), FIELD_SPEED, 1.0F,
                        FIELD_DAMAGE, false);
                fired++;
            }
            level.playSound(null, BlockPos.containing(at), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS,
                    1.0F, 1.4F);
        }

        @Override
        void draw() {
            drawStrings(strings, Tendrils.THIN);
        }

        @Override
        Vec3 near() {
            return centre;
        }
    }
}
