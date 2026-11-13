package org.gumel.jojoha.stand.skill;

import dev.architectury.event.events.common.TickEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.gumel.jojoha.combat.ComboLink;
import org.gumel.jojoha.stand.StandAim;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;

public final class StandBeat {
    private static final List<Pending> WAITING = new ArrayList<>();

    private static final List<Pending> QUEUED = new ArrayList<>();

    private StandBeat() {
    }

    private static final List<Timed> TIMED = new ArrayList<>();

    private static final List<Timed> STARTING = new ArrayList<>();

    public static void init() {
        TickEvent.SERVER_POST.register(server -> {
            WAITING.addAll(QUEUED);
            QUEUED.clear();
            WAITING.removeIf(Pending::advance);
            TIMED.addAll(STARTING);
            STARTING.clear();
            TIMED.removeIf(Timed::tick);
        });
    }

    public static void after(int ticks, ServerPlayer attacker, LivingEntity target,
                             BiConsumer<ServerPlayer, LivingEntity> blow) {
        QUEUED.add(new Pending(attacker, target, Math.max(1, ComboLink.paced(attacker, ticks)), blow));
    }

    public static void sweep(int ticks, ServerPlayer attacker, double reach,
                             BiConsumer<ServerPlayer, LivingEntity> blow, Runnable miss) {
        QUEUED.add(new Pending(attacker, reach, Math.max(1, ComboLink.paced(attacker, ticks)), blow, miss));
    }

    public static void sweep(int ticks, ServerPlayer attacker, double reach,
                             BiConsumer<ServerPlayer, LivingEntity> blow) {
        sweep(ticks, attacker, reach, blow, () -> {
        });
    }

    public static void at(int ticks, ServerPlayer player, Runnable action) {
        during(ticks, ticks, player, tick -> action.run());
    }

    public static void during(int from, int to, ServerPlayer player, IntConsumer step) {
        int start = ComboLink.paced(player, from);
        int end = ComboLink.paced(player, to);
        STARTING.add(new Timed(player, Math.max(1, start), Math.max(Math.max(1, start), end), step));
    }

    private static final class Timed {
        private final ServerPlayer player;
        private final int from;
        private final int to;
        private final IntConsumer step;
        private int elapsed;

        private Timed(ServerPlayer player, int from, int to, IntConsumer step) {
            this.player = player;
            this.from = from;
            this.to = to;
            this.step = step;
        }

        private boolean tick() {
            if (!player.isAlive() || player.isRemoved()) {
                return true;
            }
            elapsed++;
            if (elapsed >= from) {
                step.accept(elapsed);
            }
            return elapsed >= to;
        }
    }

    private static final class Pending {
        private final ServerPlayer attacker;

        private final LivingEntity target;

        private final double reach;

        private final BiConsumer<ServerPlayer, LivingEntity> blow;
        private final Runnable miss;
        private int left;

        private Pending(ServerPlayer attacker, LivingEntity target, int left,
                        BiConsumer<ServerPlayer, LivingEntity> blow) {
            this(attacker, target, 0.0, left, blow, () -> {
            });
        }

        private Pending(ServerPlayer attacker, double reach, int left,
                        BiConsumer<ServerPlayer, LivingEntity> blow, Runnable miss) {
            this(attacker, null, reach, left, blow, miss);
        }

        private Pending(ServerPlayer attacker, LivingEntity target, double reach, int left,
                        BiConsumer<ServerPlayer, LivingEntity> blow, Runnable miss) {
            this.attacker = attacker;
            this.target = target;
            this.reach = reach;
            this.left = left;
            this.blow = blow;
            this.miss = miss;
        }

        private boolean advance() {
            if (!attacker.isAlive()) {
                return true;
            }
            if (target != null && (!target.isAlive() || attacker.level() != target.level())) {
                return true;
            }

            if (--left > 0) {
                return false;
            }

            LivingEntity hit = target != null
                    ? target
                    : StandAim.lookTarget(attacker, reach);
            if (hit == null) {
                miss.run();
                return true;
            }

            blow.accept(attacker, hit);
            return true;
        }
    }
}
