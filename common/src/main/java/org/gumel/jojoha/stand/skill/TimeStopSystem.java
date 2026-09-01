package org.gumel.jojoha.stand.skill;

import dev.architectury.event.events.common.TickEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Holds the world still around whoever stopped time, and puts it back afterwards.
 *
 * <p>Freezing is done with each mob's own {@code NoAi} flag rather than by intercepting the tick
 * loop. A NoAI mob in vanilla does not act, does not path and does not even fall, which is exactly
 * the behaviour wanted, and it costs no mixin on one of the hottest methods in the game.
 *
 * <p>The catch that shape brings is that the flag is shared state: a mob spawned with NoAI already
 * set, or frozen by something else, must not be woken up when time resumes. So the previous value
 * is recorded per mob and restored rather than being cleared, and the release runs off the stored
 * list rather than re-scanning the area - by then the mobs may no longer be anywhere near where
 * they were caught.
 */
public final class TimeStopSystem {
    /** Everything this far from the user is caught. */
    /**
     * How far the stop reaches, in blocks. The one number, and the shader reads it too.
     *
     * <p>It was 24 here and 20 in the shader, which is the smaller half of the problem. The larger
     * half is that it was applied as an inflated bounding box - a 48-block cube - while what is
     * drawn is a sphere. A cube's corner is over 41 blocks from its centre, so a mob could be
     * frozen at twice the radius of any bubble the player could see, in a direction the effect
     * never reached. Anything caught now has to be inside the same ball that gets drawn.
     *
     * <p>The box has not gone away - it is still what the level is queried with, because that is
     * the only shape an entity lookup takes - but it is now only a first pass, and everything it
     * returns is checked against {@link #withinStop} before it is touched.
     */
    public static final double RADIUS = 45.0;

    private static final Map<UUID, Session> ACTIVE = new HashMap<>();

    /**
     * Set while held damage is being paid out.
     *
     * <p>A session is taken out of the active map before it is thawed, so in the ordinary case the
     * payout cannot find itself to hold its own blows. This guards the case that ordering does not
     * cover: one stop paying out and killing something, and that death ending a second stop which is
     * still listed and would happily swallow the damage from the first.
     */
    private static boolean payingOut;

    private TimeStopSystem() {
    }

    public static void init() {
        TickEvent.SERVER_POST.register(server -> tick());
    }

    /** True if this player is currently holding time. */
    public static boolean isStopped(UUID playerId) {
        return ACTIVE.containsKey(playerId);
    }

    public static void begin(Player user, int durationTicks) {
        // A second cast simply replaces the first rather than nesting, so the frozen set and its
        // saved flags can never be captured twice and restored to the wrong values.
        release(user.getUUID());

        ServerLevel level = (ServerLevel) user.level();

        // Fixed at the cast. The stop is an area, not something carried about - and this is also the
        // point every client measures the sphere from, so it has to be the same one for all of them.
        // Taken before anything is caught, because it is what "caught" is measured from.
        Vec3 centre = user.position().add(0, user.getEyeHeight() * 0.5, 0);

        AABB area = new AABB(centre, centre).inflate(RADIUS);
        List<Frozen> caught = new ArrayList<>();

        for (Mob mob : level.getEntitiesOfClass(Mob.class, area,
                mob -> mob.isAlive() && withinStop(mob, centre))) {
            caught.add(new Frozen(mob, mob.isNoAi()));
            mob.setNoAi(true);
            mob.setDeltaMovement(Vec3.ZERO);
        }

        catchPlayers(user, level, area, centre, durationTicks);

        Session session = new Session(caught, new ArrayList<>(), new HashMap<>(),
                level.getGameTime() + durationTicks, level, area, centre, user.getUUID());
        holdProjectiles(session);
        ACTIVE.put(user.getUUID(), session);

        NetworkHandler.broadcastTimeStop(level, centre, durationTicks, true);
    }

    /**
     * Whether this projectile is being held by any running stop.
     *
     * <p>Called for every projectile in the level, every tick, so the empty case has to be free -
     * hence the map check first. See ProjectileFreezeMixin for what it is protecting against.
     */
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

    /**
     * Catches a projectile at the head of its own tick, before it has moved or hit anything.
     *
     * <p>The per-tick sweep is not enough on its own for something arriving from outside. It runs
     * after the level has ticked, so an arrow fired in from beyond the bubble gets to move, run its
     * hit test and resolve against a frozen mob before anything asks whether it should have been
     * stopped - and a projectile that resolves against a target inside held time is destroyed
     * against it (see ProjectileFreezeMixin for why). By the time the sweep looked, there was
     * nothing left to catch.
     *
     * <p>So this is asked first, for every projectile, at the moment its tick begins. Anything
     * standing in the bubble is frozen there and its tick cancelled.
     *
     * <p>Where it is <em>going</em> counts as well as where it is. A tick of travel is several
     * blocks for an arrow and far more for some things, so a projectile can be outside at the top of
     * a tick and through the far wall by the end of it, never once ticking inside. Testing the
     * destination too means it is stopped at the threshold rather than waved through.
     */
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
            if (!withinStop(projectile, session.centre) && !withinRadius(next, session.centre)) {
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

    /** Whether a bare point is inside the ball - the destination test, which has no entity yet. */
    private static boolean withinRadius(Vec3 point, Vec3 centre) {
        return point.distanceToSqr(centre) <= RADIUS * RADIUS;
    }

    /** Whether something is actually inside the ball the effect draws, not merely inside its box. */
    private static boolean withinStop(net.minecraft.world.entity.Entity entity, Vec3 centre) {
        return entity.position().add(0, entity.getBbHeight() * 0.5, 0)
                .distanceToSqr(centre) <= RADIUS * RADIUS;
    }

    /**
     * Catches other players in the stop, and credits them for having been in it.
     *
     * <p>The exposure count is the whole reason this matters beyond the moment: Star Platinum's own
     * time stop is learned by being caught in someone else's, so surviving one is progress. It is
     * incremented once per stop rather than once per tick - the ability is earned by living through
     * separate occasions, not by standing in one for a long time.
     *
     * <p>Players are frozen by a tick counter they carry themselves rather than by NoAi, which does
     * nothing to a player: movement is client-authoritative, so stopping one means telling their own
     * client to stop answering the keyboard. See KeyboardInputMixin.
     */
    private static void catchPlayers(Player user, ServerLevel level, AABB area, Vec3 centre,
                                     int durationTicks) {
        for (Player other : level.getEntitiesOfClass(Player.class, area,
                p -> p.isAlive() && p != user && p instanceof ServerPlayer && withinStop(p, centre))) {

            ServerPlayer caught = (ServerPlayer) other;
            JojohaPlayerData data = PlayerDataAccess.get(caught);

            // Somebody who can stop time themselves is not stopped by it.
            //
            // The rule the fiction has always had, and the one that makes a stop into a duel rather
            // than an execution: the only thing that moves in held time is something that already
            // knows how to hold it. They still get the exposure counted - being in one is how the
            // ability is learned - but the freeze does not land on them.
            if (org.gumel.jojoha.stand.skill.moves.TimeStopSkill.INSTANCE.isUnlocked(data)) {
                data.timeStopExposures++;
                PlayerDataAccess.set(caught, data);
                PlayerDataAccess.sync(caught);
                continue;
            }
            data.timeStopFrozenTicks = Math.max(data.timeStopFrozenTicks, durationTicks);
            data.timeStopExposures++;
            PlayerDataAccess.set(caught, data);
            PlayerDataAccess.sync(caught);
        }
    }

    private static void tick() {
        if (ACTIVE.isEmpty()) {
            return;
        }

        List<UUID> expired = null;

        for (Map.Entry<UUID, Session> entry : ACTIVE.entrySet()) {
            Session session = entry.getValue();
            if (session.level.getGameTime() < session.expiryTick) {
                // Anything loosed while time is stopped is caught the same tick it appears, which is
                // the point of rescanning rather than taking the world once at the start: an arrow
                // fired during a stop should hang exactly where it was aimed until the world starts
                // again, and then go.
                holdProjectiles(session);
                pinHeldProjectiles(session);
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

        // Removed first, thawed second, and deliberately not inside the loop above. Thawing pays out
        // every blow the stop held, and dealing damage can kill things, and killing things can run
        // anything at all - including something that ends another time stop. Handing out damage
        // while iterating the map those stops live in is a crash waiting for the right death.
        for (UUID id : expired) {
            Session session = ACTIVE.remove(id);
            if (session != null) {
                thaw(session);
            }
        }
    }

    /** Ends a player's time stop early - used when they log out or their Stand goes away. */
    public static void release(UUID playerId) {
        Session session = ACTIVE.remove(playerId);
        if (session != null) {
            thaw(session);
        }
    }

    /**
     * Catches any projectile in the area that is not already being held.
     *
     * <p>Its velocity is taken away and remembered rather than scaled down, and gravity with it, so
     * an arrow keeps the exact line it was on. Restored on the other side, it carries on as though
     * the intervening seconds had not happened - which is the whole idea.
     */
    private static void holdProjectiles(Session session) {
        for (Projectile projectile : session.level.getEntitiesOfClass(Projectile.class, session.area,
                projectile -> projectile.isAlive() && withinStop(projectile, session.centre))) {

            // Everything is caught, the caster's own shots included. Loosing an arrow into stopped
            // time and watching it hang there is the whole image, and it costs nothing: it lands
            // when time does. Exempting the caster was a wrong turn - it stopped their arrows
            // freezing but left them flying into targets that were still refusing damage, which is
            // the same deflection by another route.
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

    /**
     * Pins every held projectile exactly where it was caught.
     *
     * <p>Position, heading and velocity are all written back every tick, not just the velocity. A
     * projectile whose motion is zeroed still runs its own tick: it applies drag, it counts down its
     * life, it re-derives its facing from a velocity that is now nothing - which is why arrows hung
     * sideways - and an arrow that clips a block on the way to a standstill can set itself as landed
     * and then drop out of the air when the world starts again. Rewriting the whole of its state
     * each tick means none of that can accumulate, and it resumes from exactly where it stopped.
     *
     * <p>The previous-frame rotation and position go back too. Without them the client interpolates
     * from wherever the projectile was a moment ago, so a pinned arrow jitters between two poses
     * instead of standing still.
     */
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

    /**
     * Records a blow instead of letting it land.
     *
     * @return true if the hit was taken and should not be applied now
     */
    public static boolean holdDamage(LivingEntity victim, DamageSource source, float amount) {
        if (payingOut || ACTIVE.isEmpty() || amount <= 0F) {
            return false;
        }

        for (Session session : ACTIVE.values()) {
            if (!session.holds(victim)) {
                continue;
            }

            // Summed, and the first blow's source is the one kept. Summing is what makes a barrage
            // land as a barrage: every punch thrown into held time arrives at once the instant it
            // starts again, which is the only reading of a time stop that makes it worth having.
            session.pending.merge(victim.getUUID(), new Pending(source, amount), Pending::plus);

            // The blow is held, not refused, and the difference has to be visible or it reads as
            // one that missed. Vanilla's own hurt event - the red wash and the grunt - is broadcast
            // without any of the damage behind it, so the target flashes exactly as it would have
            // and then goes back to standing perfectly still. Which is the image: something was hit
            // and has not yet found out.
            //
            // broadcastAndSend rather than broadcast, so a player hit inside the stop sees their own
            // screen shake too - the sending entity is excluded from a plain broadcast.
            session.level.getChunkSource().broadcastAndSend(victim,
                    new ClientboundHurtAnimationPacket(victim));
            return true;
        }

        return false;
    }

    private static void thaw(Session session) {
        for (Frozen frozen : session.frozen) {
            if (frozen.mob.isAlive()) {
                frozen.mob.setNoAi(frozen.wasNoAi);
            }
        }

        for (Held held : session.projectiles) {
            if (!held.projectile.isAlive()) {
                continue;
            }

            // Put back exactly as it was caught, so it carries on along the line it was already on
            // rather than falling out of the sky from a standstill.
            held.projectile.setNoGravity(held.wasNoGravity);
            held.projectile.setDeltaMovement(held.motion);
            held.projectile.setYRot(held.yRot);
            held.projectile.setXRot(held.xRot);
            held.projectile.hasImpulse = true;
            // And told to the client, which is what makes carrying the momentum a guarantee rather
            // than a coincidence. hasImpulse alone broadcasts nothing; only hurtMarked sends a
            // motion packet. It happened to look right because the client was never told about the
            // zeroing either, so its copy still held the original velocity - but that is two
            // omissions cancelling out, and the first one going away would have broken the second.
            held.projectile.hurtMarked = true;
        }

        payOut(session);

        NetworkHandler.broadcastTimeStop(session.level, session.centre, 0, false);
    }

    /** Everything that was held back, landing at once. */
    private static void payOut(Session session) {
        if (session.pending.isEmpty()) {
            return;
        }

        payingOut = true;
        try {
            for (Map.Entry<UUID, Pending> entry : session.pending.entrySet()) {
                LivingEntity victim = session.find(entry.getKey());
                if (victim == null || !victim.isAlive()) {
                    continue;
                }

                // Cleared so the whole total lands. Left alone, the usual half-second of mercy after
                // a hit would swallow everything but the first blow, and a stop full of punches would
                // be worth exactly one punch.
                victim.invulnerableTime = 0;
                victim.hurt(entry.getValue().source, entry.getValue().amount);

                // And cleared again afterwards. Everything the stop held lands on one tick, which
                // leaves the target with a full half-second of invulnerability starting at exactly
                // the moment the projectiles it was also holding are let go. An arrow a block away
                // arrives inside that window, is refused, and vanilla answers a refusal by turning
                // it round - so the blows landing would have been what knocked the arrows off.
                victim.invulnerableTime = 0;
            }
        } finally {
            payingOut = false;
        }

        session.pending.clear();
    }

    private record Frozen(Mob mob, boolean wasNoAi) {
    }

    private record Held(Projectile projectile, Vec3 motion, boolean wasNoGravity,
                        float yRot, float xRot, Vec3 at) {
    }

    /** A blow held back, and who to credit when it finally lands. */
    private record Pending(DamageSource source, float amount) {
        Pending plus(Pending later) {
            return new Pending(this.source, this.amount + later.amount);
        }
    }

    private record Session(List<Frozen> frozen, List<Held> projectiles, Map<UUID, Pending> pending,
                           long expiryTick, ServerLevel level, AABB area, Vec3 centre, UUID userId) {
        boolean holdsProjectile(Projectile projectile) {
            for (Held held : projectiles) {
                if (held.projectile == projectile) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Whether this stop is holding the given body.
         *
         * <p>Mobs by identity, since those are the ones whose flags were taken; players by the
         * counter they carry, since they are frozen by that rather than by anything stored here.
         */
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
