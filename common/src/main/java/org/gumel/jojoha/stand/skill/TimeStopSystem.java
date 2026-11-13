package org.gumel.jojoha.stand.skill;

import dev.architectury.event.events.common.TickEvent;
import net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.accessory.Relics;
import org.gumel.jojoha.config.JojohaServerConfig;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.heaven.Heaven;
import org.gumel.jojoha.stand.skill.moves.TimeStopSkill;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

public final class TimeStopSystem {
    public static final double RADIUS = 45.0;

    private static final Map<UUID, Session> ACTIVE = new HashMap<>();

    public static final int TIME_STOPPER_MOVE_TICKS = 40;

    private static boolean payingOut;

    private TimeStopSystem() {
    }

    public static void init() {
        TickEvent.SERVER_POST.register(server -> tick());
    }

    public static boolean isStopped(UUID playerId) {
        return ACTIVE.containsKey(playerId);
    }

    public static void begin(Player user, int durationTicks, int quietTicks, int introTicks) {
        release(user.getUUID());

        ServerLevel level = (ServerLevel) user.level();

        Vec3 centre = user.position().add(0, user.getEyeHeight() * 0.5, 0);

        double radius = Heaven.is(PlayerDataAccess.get(user)) ? GLOBAL_RADIUS : RADIUS;
        AABB area = new AABB(centre, centre).inflate(Math.min(radius, RADIUS));
        List<Frozen> caught = new ArrayList<>();

        for (Mob mob : around(level, Mob.class, area, radius,
                mob -> mob.isAlive() && withinStop(mob, centre, radius))) {
            caught.add(new Frozen(mob, mob.isNoAi()));
            mob.setNoAi(true);
            mob.setDeltaMovement(Vec3.ZERO);
        }

        Session session = new Session(caught, new ArrayList<>(), new HashMap<>(),
                level.getGameTime() + durationTicks, level, area, centre, user.getUUID(),
                new ArrayList<>(), new HashSet<>(), new HashMap<>(), radius, introTicks);
        catchPlayers(session, durationTicks);
        holdProjectiles(session);
        ACTIVE.put(user.getUUID(), session);

        NetworkHandler.broadcastTimeStop(level, centre, durationTicks, true, session.frozenPlayers, radius, quietTicks, introTicks);
    }

    public static boolean freezes(Entity entity) {
        if (ACTIVE.isEmpty()) {
            return false;
        }
        for (Session session : ACTIVE.values()) {
            if (session.level() != entity.level() || session.userId().equals(entity.getUUID())) {
                continue;
            }
            if (entity instanceof StandEntity stand) {
                Player owner = stand.getOwner();
                if (owner != null && (session.userId().equals(owner.getUUID())
                        || (owner instanceof ServerPlayer serverOwner
                                && !PlayerDataAccess.get(serverOwner).isTimeStopFrozen()))) {
                    continue;
                }
            }
            if (withinStop(entity, session.centre(), session.radius())) {
                return true;
            }
        }
        return false;
    }

    public static Boolean noAiBeforeStop(Mob mob) {
        for (Session session : ACTIVE.values()) {
            for (Frozen frozen : session.frozen) {
                if (frozen.mob == mob) {
                    return frozen.wasNoAi;
                }
            }
        }
        return null;
    }

    private static boolean heldByStand(Mob mob) {
        return mob.getVehicle() instanceof StandEntity stand
                && stand.heldEntity() == mob;
    }

    public static boolean freezesPosition(Level level, Vec3 position) {
        return freezesPosition(level, position.x, position.y, position.z);
    }

    public static boolean freezesPosition(Level level,
                                          double x, double y, double z) {
        if (ACTIVE.isEmpty()) {
            return false;
        }
        for (Session session : ACTIVE.values()) {
            if (session.level() != level) {
                continue;
            }
            Vec3 centre = session.centre();
            double dx = x - centre.x;
            double dy = y - centre.y;
            double dz = z - centre.z;
            if (dx * dx + dy * dy + dz * dz <= session.radius() * session.radius()) {
                return true;
            }
        }
        return false;
    }

    public static boolean isHeldProjectile(Projectile projectile) {
        if (ACTIVE.isEmpty()) {
            return false;
        }

        for (Session session : ACTIVE.values()) {
            if (session.holdsProjectile(projectile)) {
                return true;
            }
        }
        return false;
    }

    public static boolean holdIfInside(Projectile projectile) {
        if (ACTIVE.isEmpty() || !projectile.isAlive()) {
            return false;
        }

        for (Session session : ACTIVE.values()) {
            if (session.holdsProjectile(projectile)) {
                return true;
            }
            if (projectile.level() != session.level) {
                continue;
            }

            Vec3 next = projectile.position().add(projectile.getDeltaMovement());
            if (!withinStop(projectile, session.centre, session.radius)
                    && !withinRadius(next, session.centre, session.radius)) {
                continue;
            }

            session.projectiles.add(new Held(projectile, projectile.getDeltaMovement(),
                    projectile.isNoGravity(), projectile.getYRot(), projectile.getXRot(),
                    projectile.position()));
            projectile.setDeltaMovement(Vec3.ZERO);
            projectile.setNoGravity(true);
            return true;
        }

        return false;
    }

    public static final double GLOBAL_RADIUS = 3.0E7;

    public static boolean global(double radius) {
        return radius >= GLOBAL_RADIUS;
    }

    private static <T extends Entity> List<T> around(ServerLevel level, Class<T> kind,
                                                                               AABB area, double radius,
                                                                               Predicate<T> test) {
        if (!global(radius)) {
            return level.getEntitiesOfClass(kind, area, test);
        }
        List<T> found = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (kind.isInstance(entity) && test.test(kind.cast(entity))) {
                found.add(kind.cast(entity));
            }
        }
        return found;
    }

    private static boolean withinRadius(Vec3 point, Vec3 centre, double radius) {
        return global(radius) || point.distanceToSqr(centre) <= radius * radius;
    }

    private static boolean withinStop(Entity entity, Vec3 centre, double radius) {
        return global(radius) || entity.position().add(0, entity.getBbHeight() * 0.5, 0)
                .distanceToSqr(centre) <= radius * radius;
    }

    private static boolean catchPlayers(Session session, int remainingTicks) {
        if (remainingTicks <= 0) {
            return false;
        }
        boolean caughtSomeoneNew = false;
        for (Player other : around(session.level, Player.class, session.area, session.radius,
                p -> p.isAlive() && p instanceof ServerPlayer && !p.getUUID().equals(session.userId)
                        && withinStop(p, session.centre, session.radius))) {

            ServerPlayer caught = (ServerPlayer) other;
            JojohaPlayerData data = PlayerDataAccess.get(caught);

            if (Heaven.is(data)) {
                continue;
            }
            if (TimeStopSkill.INSTANCE.isUnlocked(data)
                    && session.moving.merge(caught.getUUID(), 1, Integer::sum)
                            <= JojohaServerConfig.TIME_STOPPER_MOVES.whole()) {
                continue;
            }
            boolean firstTime = session.seen.add(caught.getUUID());

            data.timeStopFrozenTicks = Math.max(data.timeStopFrozenTicks, remainingTicks);
            if (firstTime) {
                data.timeStopExposures++;
                session.frozenPlayers.add(caught.getId());
                caughtSomeoneNew = true;
            }
            PlayerDataAccess.set(caught, data);
            PlayerDataAccess.sync(caught);
        }
        return caughtSomeoneNew;
    }

    private static void tick() {
        if (ACTIVE.isEmpty()) {
            return;
        }

        List<UUID> expired = null;

        for (Map.Entry<UUID, Session> entry : ACTIVE.entrySet()) {
            Session session = entry.getValue();
            if (session.level.getGameTime() < session.expiryTick) {
                holdProjectiles(session);
                pinHeldProjectiles(session);
                int remaining = (int) (session.expiryTick - session.level.getGameTime());
                if (catchPlayers(session, remaining)) {
                    NetworkHandler.broadcastTimeStop(session.level, session.centre, remaining, true,
                            session.frozenPlayers, session.radius, 0, session.introTicks);
                }
                continue;
            }

            if (expired == null) {
                expired = new ArrayList<>();
            }
            expired.add(entry.getKey());
        }

        if (expired == null) {
            return;
        }

        for (UUID id : expired) {
            Session session = ACTIVE.remove(id);
            if (session != null) {
                thaw(session);
            }
        }
    }

    public static void release(UUID playerId) {
        Session session = ACTIVE.remove(playerId);
        if (session != null) {
            thaw(session);
        }
    }

    private static void holdProjectiles(Session session) {
        for (Projectile projectile : around(session.level, Projectile.class, session.area, session.radius,
                projectile -> projectile.isAlive() && withinStop(projectile, session.centre, session.radius))) {
            if (session.holdsProjectile(projectile)) {
                continue;
            }

            session.projectiles.add(new Held(projectile, projectile.getDeltaMovement(),
                    projectile.isNoGravity(), projectile.getYRot(), projectile.getXRot(),
                    projectile.position()));
            projectile.setDeltaMovement(Vec3.ZERO);
            projectile.setNoGravity(true);
        }
    }

    private static void pinHeldProjectiles(Session session) {
        for (Held held : session.projectiles) {
            Projectile projectile = held.projectile;
            if (!projectile.isAlive()) {
                continue;
            }

            projectile.setDeltaMovement(Vec3.ZERO);
            projectile.setPos(held.at.x, held.at.y, held.at.z);
            projectile.xOld = held.at.x;
            projectile.yOld = held.at.y;
            projectile.zOld = held.at.z;

            projectile.setYRot(held.yRot);
            projectile.setXRot(held.xRot);
            projectile.yRotO = held.yRot;
            projectile.xRotO = held.xRot;
        }
    }

    public static boolean holdDamage(LivingEntity victim, DamageSource source, float amount) {
        if (payingOut || ACTIVE.isEmpty() || amount <= 0F) {
            return false;
        }

        for (Session session : ACTIVE.values()) {
            if (!session.holds(victim)) {
                continue;
            }

            session.pending.computeIfAbsent(victim.getUUID(), id -> new ArrayList<>())
                    .add(new Pending(source, amount));

            session.level.getChunkSource().broadcastAndSend(victim,
                    new ClientboundHurtAnimationPacket(victim));
            return true;
        }

        return false;
    }

    private static void thaw(Session session) {
        for (Frozen frozen : session.frozen) {
            if (frozen.mob.isAlive() && !heldByStand(frozen.mob)) {
                frozen.mob.setNoAi(frozen.wasNoAi);
            }
        }

        for (Held held : session.projectiles) {
            if (!held.projectile.isAlive()) {
                continue;
            }

            held.projectile.setNoGravity(held.wasNoGravity);
            held.projectile.setDeltaMovement(held.motion);
            held.projectile.setYRot(held.yRot);
            held.projectile.setXRot(held.xRot);
            held.projectile.hasImpulse = true;
            held.projectile.hurtMarked = true;
        }

        payOut(session);

        NetworkHandler.broadcastTimeStop(session.level, session.centre, 0, false, List.of(), session.radius);
    }

    private static void payOut(Session session) {
        if (session.pending.isEmpty()) {
            return;
        }

        int kills = 0;
        payingOut = true;
        try {
            for (Map.Entry<UUID, List<Pending>> entry : session.pending.entrySet()) {
                LivingEntity victim = session.find(entry.getKey());
                if (victim == null || !victim.isAlive()) {
                    continue;
                }

                boolean theirs = false;
                for (Pending blow : entry.getValue()) {
                    if (!victim.isAlive()) {
                        break;
                    }
                    theirs |= blow.source.getEntity() != null && session.userId.equals(blow.source.getEntity().getUUID());
                    victim.invulnerableTime = 0;
                    victim.hurt(blow.source, blow.amount);
                }

                if (theirs && !victim.isAlive() && victim instanceof Mob) {
                    kills++;
                }
                victim.invulnerableTime = 0;
            }
        } finally {
            payingOut = false;
        }
        if (session.level.getPlayerByUUID(session.userId) instanceof ServerPlayer user) {
            Relics.timeStopKills(user, kills);
        }

        session.pending.clear();
    }

    private record Frozen(Mob mob, boolean wasNoAi) {
    }

    private record Held(Projectile projectile, Vec3 motion, boolean wasNoGravity,
                        float yRot, float xRot, Vec3 at) {
    }

    private record Pending(DamageSource source, float amount) {
    }

    private record Session(List<Frozen> frozen, List<Held> projectiles, Map<UUID, List<Pending>> pending,
                           long expiryTick, ServerLevel level, AABB area, Vec3 centre, UUID userId,
                           List<Integer> frozenPlayers, Set<UUID> seen, Map<UUID, Integer> moving,
                           double radius, int introTicks) {
        boolean holdsProjectile(Projectile projectile) {
            for (Held held : projectiles) {
                if (held.projectile == projectile) {
                    return true;
                }
            }
            return false;
        }

        boolean holds(LivingEntity entity) {
            if (entity instanceof Player player) {
                return PlayerDataAccess.get(player).timeStopFrozenTicks > 0;
            }

            for (Frozen frozen : this.frozen) {
                if (frozen.mob == entity) {
                    return true;
                }
            }
            return false;
        }

        LivingEntity find(UUID id) {
            for (Frozen frozen : this.frozen) {
                if (frozen.mob.getUUID().equals(id)) {
                    return frozen.mob;
                }
            }
            return level.getPlayerByUUID(id);
        }
    }
}
