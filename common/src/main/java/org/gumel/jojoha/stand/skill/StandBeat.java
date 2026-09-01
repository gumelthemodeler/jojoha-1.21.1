package org.gumel.jojoha.stand.skill;

import dev.architectury.event.events.common.TickEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * A blow that lands a moment after the swing that threw it.
 *
 * <h2>Why a move would want to wait</h2>
 *
 * <p>An attack that resolves on the tick the key is pressed is finished before the Stand has moved.
 * The animation then plays over an outcome that has already happened: the target is hurt, launched
 * and falling while the fist is still travelling toward where they used to be. Testers read that
 * exactly as it looks - the damage arriving before the punch.
 *
 * <p>Holding the payload for a few ticks costs nothing and buys the obvious thing: the blow lands
 * when the blow lands. It is also the window in which a target can be healed, shielded or pulled
 * clear by somebody else, which is worth having for its own sake - a hit with no window is a hit
 * with no answer.
 *
 * <h2>Deliberately not a general scheduler</h2>
 *
 * <p>It takes the two parties and re-checks both when it fires, because the gap it exists to create
 * is long enough for either to die, be dismissed, change dimension or unload. A plain
 * {@code Runnable} queue would have let a move resolve against a corpse or across a world boundary,
 * and every caller would have had to remember to guard against it separately.
 */
public final class StandBeat {
    /** Blows in the air, checked once a tick. */
    private static final List<Pending> WAITING = new ArrayList<>();

    private StandBeat() {
    }

    public static void init() {
        TickEvent.SERVER_POST.register(server -> WAITING.removeIf(Pending::advance));
    }

    /**
     * Runs {@code blow} after the given number of ticks, if both parties are still there for it.
     *
     * @param ticks how long the swing takes to arrive
     */
    public static void after(int ticks, ServerPlayer attacker, LivingEntity target,
                             BiConsumer<ServerPlayer, LivingEntity> blow) {
        WAITING.add(new Pending(attacker, target, Math.max(1, ticks), blow));
    }

    private static final class Pending {
        private final ServerPlayer attacker;
        private final LivingEntity target;
        private final BiConsumer<ServerPlayer, LivingEntity> blow;
        private int left;

        private Pending(ServerPlayer attacker, LivingEntity target, int left,
                        BiConsumer<ServerPlayer, LivingEntity> blow) {
            this.attacker = attacker;
            this.target = target;
            this.left = left;
            this.blow = blow;
        }

        /** True once this is finished with, whether it landed or was abandoned. */
        private boolean advance() {
            // Abandoned rather than fired. Either party gone, or the two no longer in the same
            // world, and there is no sensible thing left to resolve.
            if (!attacker.isAlive() || !target.isAlive()
                    || attacker.level() != target.level()) {
                return true;
            }

            if (--left > 0) {
                return false;
            }

            blow.accept(attacker, target);
            return true;
        }
    }
}
