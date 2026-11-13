package org.gumel.jojoha.combat;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.gumel.jojoha.stand.StandEntity;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Supplier;

public final class ComboDecay {
    public static final int FULL_HITS = 4;
    public static final float STEP = 0.85F;
    public static final float FLOOR = 0.5F;
    public static final int RESET_TICKS = 30;

    private static final Map<Entity, Chain> CHAINS = new WeakHashMap<>();

    private static int uncounted;

    private ComboDecay() {
    }

    public static float apply(Entity attacker, Entity target, float amount) {
        Entity from = source(attacker);
        Entity body = target instanceof LivingEntity living ? StandHits.bodyOf(living) : target;
        if (!(body instanceof Player victim) || from == null || from == victim || victim.level().isClientSide()) {
            return amount;
        }
        long now = victim.level().getGameTime();
        Chain theirs = CHAINS.get(from);
        if (theirs != null && theirs.attacker == victim) {
            CHAINS.remove(from);
        }
        Chain chain = CHAINS.get(victim);
        if (chain == null || chain.attacker != from || now - chain.last > RESET_TICKS) {
            chain = new Chain(from);
            CHAINS.put(victim, chain);
        }
        float kept = kept(chain.hits);
        if (uncounted == 0 && chain.counted != now) {
            chain.hits++;
            chain.counted = now;
        }
        chain.last = now;
        return amount * kept;
    }

    public static void reset(Entity victim) {
        CHAINS.remove(victim);
    }

    public static float uncounted(Supplier<Float> hit) {
        uncounted++;
        try {
            return hit.get();
        } finally {
            uncounted--;
        }
    }

    public static void uncounted(Runnable hit) {
        uncounted++;
        try {
            hit.run();
        } finally {
            uncounted--;
        }
    }

    public static float kept(int hits) {
        return hits < FULL_HITS ? 1F : Math.max(FLOOR, (float) Math.pow(STEP, hits - FULL_HITS + 1));
    }

    private static Entity source(Entity attacker) {
        if (attacker instanceof StandEntity stand && stand.getOwner() != null) {
            return stand.getOwner();
        }
        return attacker instanceof LivingEntity ? attacker : null;
    }

    private static final class Chain {
        private final Entity attacker;
        private int hits;
        private long last;
        private long counted = Long.MIN_VALUE;

        private Chain(Entity attacker) {
            this.attacker = attacker;
        }
    }
}
