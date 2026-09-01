package org.gumel.jojoha.stand.passive;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.StatProgression;

/**
 * This Stand does not get better by winning fights, because it does not win fights.
 *
 * <p>Every other Stand grows on kills, and for Hermit Purple that is a scoring system it cannot
 * play. Its whole kit is rope and photographs - a player using it correctly spends the afternoon
 * swinging across a ravine and breaking cameras, and under the ordinary rule they would finish that
 * afternoon exactly as strong as they started it.
 *
 * <p>So it earns from being out. Keeping the Stand up is the thing being rewarded, which is also the
 * thing the Stand is for, and the two finally agree.
 *
 * <h2>The rate</h2>
 *
 * <p>Deliberately slow. This is not a faster way to level - it is the only way this Stand levels at
 * all, and it has to sit somewhere near what a player fighting with a different Stand would make in
 * the same time. Earning while simply standing still with the vines out is the failure mode to
 * avoid, and the answer to it is that the rate is low enough that idling is a poor use of an
 * afternoon compared with going somewhere.
 */
public final class UnorthodoxMethodPassive implements StandPassive {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "unorthodox_method");

    public static final UnorthodoxMethodPassive INSTANCE = new UnorthodoxMethodPassive();

    /** How often the Stand being out is worth something, in ticks, and how much. */
    private static final int EVERY = 100;
    private static final int WORTH = 1;

    private UnorthodoxMethodPassive() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "passive.jojoha.unorthodox_method";
    }

    @Override
    public void tick(ServerPlayer player, JojohaPlayerData data) {
        if (!data.standSummoned || player.tickCount % EVERY != 0) {
            return;
        }

        StatProgression.awardUse(player, WORTH);
    }
}
