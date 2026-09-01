package org.gumel.jojoha.stand.passive;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;

/**
 * Joseph has been in worse spots than this one.
 *
 * <p>Everything harmful runs down faster. Not weaker - the poison still poisons and the blindness
 * still blinds - simply shorter, which is the right shape for a Stand whose whole answer to trouble
 * is to get through it rather than to shrug it off.
 *
 * <h2>Ticking them down rather than intercepting them</h2>
 *
 * <p>The tidier-looking version catches effects as they are applied and shortens them there, and it
 * needs a mixin on the entity to do it - which then has to decide what to do about effects already
 * running when the Stand is summoned, and about effects reapplied by something that refreshes them.
 *
 * <p>Spending the duration faster sidesteps all of that. Anything harmful, however it arrived and
 * whenever it started, simply runs out sooner. Beneficial effects are left entirely alone, which a
 * blanket "resist effects" would not have managed.
 */
public final class MadeForThisPassive implements StandPassive {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "made_for_this");

    public static final MadeForThisPassive INSTANCE = new MadeForThisPassive();

    /**
     * How often an extra tick is taken off, and how many.
     *
     * <p>One extra tick every three works out at a third off the length of everything harmful.
     * Checked on a cadence rather than every tick because the whole point is to be a fraction, and a
     * fraction needs a denominator.
     */
    private static final int EVERY = 3;
    private static final int EXTRA = 1;

    /** Below this an effect is about to end anyway, and hurrying it risks ending it early. */
    private static final int FLOOR = 2;

    private MadeForThisPassive() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "passive.jojoha.made_for_this";
    }

    @Override
    public void tick(ServerPlayer player, JojohaPlayerData data) {
        if (player.tickCount % EVERY != 0) {
            return;
        }

        for (MobEffectInstance instance : player.getActiveEffects()) {
            if (instance.getEffect().value().isBeneficial() || instance.isInfiniteDuration()) {
                continue;
            }

            if (instance.getDuration() > FLOOR) {
                instance.mapDuration(duration -> Math.max(FLOOR, duration - EXTRA));
            }
        }
    }
}
