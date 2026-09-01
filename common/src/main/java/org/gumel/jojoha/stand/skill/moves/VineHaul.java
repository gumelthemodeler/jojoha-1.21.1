package org.gumel.jojoha.stand.skill.moves;

import dev.architectury.event.events.common.TickEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.registry.ModRegistries;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Dragging something toward you on a vine, and doing something to it when it arrives.
 *
 * <p>Both of this Stand's combat moves are this and then a payload - the whip hurls them out again,
 * the gut punch meets them with a fist - and the dragging half was written twice before it was
 * written once. The half that differs is a callback; everything else is here.
 *
 * <h2>Why the pull is ticked rather than applied</h2>
 *
 * <p>Setting the victim's position outright would be a teleport with a sound on it. The gap has to
 * be visibly crossed, both because that is the whole read of the move and because the crossing is
 * the window in which anything can be done about it - blocked for, healed through, interrupted by
 * somebody else. A move that resolves on the tick it is pressed has no answer.
 *
 * <p>Velocity is set rather than added. Adding lets whatever the target was already doing carry them
 * off the line, so a mob that happened to be sprinting away arrives late or not at all, and the
 * payload fires at nothing.
 */
public final class VineHaul {
    /** How fast something is dragged, and how close counts as arrived. */
    private static final double SPEED = 0.9;
    private static final double ARRIVE = 2.1;

    /** A little lift, so they are pulled over the ground rather than ploughed along it. */
    private static final double LIFT = 0.08;

    private static final List<Haul> ACTIVE = new ArrayList<>();

    private VineHaul() {
    }

    public static void init() {
        TickEvent.SERVER_POST.register(server -> ACTIVE.removeIf(Haul::advance));
    }

    /**
     * Starts a haul, replacing any this thrower or this target was already part of.
     *
     * @param ticks   how long the drag may run before the payload fires anyway
     * @param arrival what to do once they are here - or once the time is up
     */
    public static void begin(ServerPlayer holder, LivingEntity target, int ticks,
                             BiConsumer<ServerPlayer, LivingEntity> arrival) {
        // Nobody is hauled twice at once, and nobody hauls two things: either would fire two
        // payloads off one press.
        ACTIVE.removeIf(haul -> haul.target == target || haul.holder == holder);
        ACTIVE.add(new Haul(holder, target, ticks, arrival));
    }

    /**
     * The nearest living thing along a player's line of sight.
     *
     * <p>Scored by how far it sits off the aim line rather than by distance, so the one being
     * pointed at wins over a nearer one off to the side. Shared for the same reason the drag is:
     * two moves that aim differently would be two moves that feel differently to aim.
     */
    public static LivingEntity look(ServerPlayer player, double reach, double forgiveness) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        AABB search = new AABB(eye, eye.add(look.scale(reach))).inflate(forgiveness);

        LivingEntity best = null;
        double bestOffset = Double.MAX_VALUE;

        for (Entity entity : player.serverLevel().getEntities(player, search)) {
            if (!(entity instanceof LivingEntity living) || entity == player || !living.isAlive()) {
                continue;
            }

            Vec3 toward = entity.position().add(0, entity.getBbHeight() * 0.5, 0).subtract(eye);
            double along = toward.dot(look);
            if (along <= 0 || along > reach) {
                continue;
            }

            double offset = toward.subtract(look.scale(along)).length();
            if (offset < bestOffset && offset <= forgiveness) {
                bestOffset = offset;
                best = living;
            }
        }
        return best;
    }

    /** One haul in progress. */
    private static final class Haul {
        private final ServerPlayer holder;
        private final LivingEntity target;
        private final BiConsumer<ServerPlayer, LivingEntity> arrival;
        private int remaining;

        private Haul(ServerPlayer holder, LivingEntity target, int ticks,
                     BiConsumer<ServerPlayer, LivingEntity> arrival) {
            this.holder = holder;
            this.target = target;
            this.arrival = arrival;
            this.remaining = ticks;
        }

        /** True once this haul is finished with, one way or another. */
        private boolean advance() {
            if (!holder.isAlive() || !target.isAlive() || holder.level() != target.level()) {
                return true;
            }

            Vec3 toHolder = holder.position().subtract(target.position());
            double gap = toHolder.length();

            // Arrived, or out of time. The payload fires either way - a haul that ran its clock
            // against a wall has still earned whatever it was going to do.
            if (gap <= ARRIVE || --remaining <= 0) {
                arrival.accept(holder, target);
                return true;
            }

            Vec3 heading = toHolder.scale(1.0 / gap);
            target.setDeltaMovement(heading.x * SPEED, heading.y * SPEED * 0.5 + LIFT,
                    heading.z * SPEED);
            target.hurtMarked = true;

            // Motes coming off them toward the thrower, so the pull has a direction on screen.
            if (holder.level() instanceof ServerLevel level) {
                Vec3 at = target.position().add(0, target.getBbHeight() * 0.6, 0);
                level.sendParticles(ModRegistries.STAND_AURA.get(), at.x, at.y, at.z, 2,
                        0.22, 0.22, 0.22, 0.02);
            }
            return false;
        }
    }
}
