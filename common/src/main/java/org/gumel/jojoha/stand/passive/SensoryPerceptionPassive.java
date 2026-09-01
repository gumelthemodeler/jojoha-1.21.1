package org.gumel.jojoha.stand.passive;

import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

/**
 * Star Platinum notices what is worth opening.
 *
 * <p>Containers near its user are outlined, so a chest behind a wall or buried in a floor announces
 * itself the way a Stand's own perception would.
 *
 * <h2>Why there is no behaviour here</h2>
 *
 * <p>Every other passive does something to the world and belongs on the server. This one does
 * nothing to the world at all - it changes what its user can see, and the client already knows
 * where every loaded block is. Implementing it server-side would mean scanning chunks, packing the
 * results into a packet and sending them at some refresh rate, all to tell a client something it
 * could have looked up itself in a fraction of the time. It would also be a strictly worse feature:
 * the outline would lag the player by however long the round trip took.
 *
 * <p>So this class is a name and an id, and {@code LootSense} on the client does the work by asking
 * whether the local player's Stand grants this passive. The registration still matters - it is what
 * puts the passive in the Stand's list, which is what the client tests against, and what any future
 * interface listing a Stand's passives will read.
 */
public final class SensoryPerceptionPassive implements StandPassive {
    public static final SensoryPerceptionPassive INSTANCE = new SensoryPerceptionPassive();

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "sensory_perception");

    private SensoryPerceptionPassive() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "passive.jojoha.sensory_perception";
    }
}
