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

public final class VineHaul {
    private static final double SPEED = 0.9;
    private static final double ARRIVE = 2.1;

    private static final double LIFT = 0.08;

    private static final int TRAIL_MOTES = 3;
    private static final double TRAIL_SPEED = 0.45;

    private static final List<Haul> ACTIVE = new ArrayList<>();

    private VineHaul() {
    }

    public static void init() {
        TickEvent.SERVER_POST.register(server -> ACTIVE.removeIf(Haul::advance));
    }

    public record Beats(int reach, int haul, int wind,
                        BiConsumer<ServerPlayer, LivingEntity> took,
                        BiConsumer<ServerPlayer, LivingEntity> wound,
                        BiConsumer<ServerPlayer, LivingEntity> landed,
                        BiConsumer<ServerPlayer, LivingEntity> lost) {
        public static Beats drag(int ticks, BiConsumer<ServerPlayer, LivingEntity> landed) {
            return new Beats(0, ticks, 0, null, null, landed, null);
        }
    }

    public static void begin(ServerPlayer holder, LivingEntity target, int ticks,
                             BiConsumer<ServerPlayer, LivingEntity> arrival) {
        begin(holder, target, Beats.drag(ticks, arrival));
    }

    public static void begin(ServerPlayer holder, LivingEntity target, Beats beats) {
        ACTIVE.removeIf(haul -> haul.target == target || haul.holder == holder);
        ACTIVE.add(new Haul(holder, target, beats));
    }

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

    private static final class Haul {
        private final ServerPlayer holder;
        private final LivingEntity target;
        private final Beats beats;

        private int remaining;
        private Beat beat;

        private enum Beat { REACHING, HAULING, WINDING }

        private Haul(ServerPlayer holder, LivingEntity target, Beats beats) {
            this.holder = holder;
            this.target = target;
            this.beats = beats;
            this.beat = beats.reach() > 0 ? Beat.REACHING : Beat.HAULING;
            this.remaining = beats.reach() > 0 ? beats.reach() : beats.haul();
        }

        private void fire(BiConsumer<ServerPlayer, LivingEntity> at) {
            if (at != null) {
                at.accept(holder, target);
            }
        }

        private boolean advance() {
            if (!holder.isAlive() || !target.isAlive() || holder.level() != target.level()) {
                if (holder.isAlive()) {
                    fire(beats.lost());
                }
                return true;
            }

            if (beat == Beat.REACHING) {
                if (--remaining > 0) {
                    return false;
                }
                fire(beats.took());
                beat = Beat.HAULING;
                remaining = beats.haul();
                return false;
            }

            if (beat == Beat.WINDING) {
                if (--remaining > 0) {
                    return false;
                }
                fire(beats.landed());
                return true;
            }

            Vec3 toHolder = holder.position().subtract(target.position());
            double gap = toHolder.length();

            if (gap <= ARRIVE || --remaining <= 0) {
                if (beats.wind() > 0) {
                    fire(beats.wound());
                    beat = Beat.WINDING;
                    remaining = beats.wind();
                    return false;
                }
                fire(beats.landed());
                return true;
            }

            Vec3 heading = toHolder.scale(1.0 / gap);
            target.setDeltaMovement(heading.x * SPEED, heading.y * SPEED * 0.5 + LIFT,
                    heading.z * SPEED);
            target.hurtMarked = true;

            if (holder.level() instanceof ServerLevel level) {
                Vec3 at = target.position().add(0, target.getBbHeight() * 0.6, 0);
                for (int i = 0; i < TRAIL_MOTES; i++) {
                    Vec3 along = at.add(heading.scale(i * (gap / (TRAIL_MOTES + 1))));
                    level.sendParticles(ModRegistries.STAND_AURA.get(), along.x, along.y, along.z, 0,
                            heading.x, heading.y, heading.z, TRAIL_SPEED);
                }
            }
            return false;
        }
    }
}
