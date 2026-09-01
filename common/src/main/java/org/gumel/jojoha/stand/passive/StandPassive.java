package org.gumel.jojoha.stand.passive;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.gumel.jojoha.data.JojohaPlayerData;

/**
 * Something a Stand does for its user simply by being there.
 *
 * <p>Separate from {@code StandSkill} because the two are opposites: a skill is pressed and
 * resolves, a passive is a standing condition with no input and no cooldown. Folding them together
 * would mean every passive carrying a slot, a key and an energy cost it has no use for.
 *
 * <p>Both hooks default to doing nothing, so a passive implements only the one it cares about.
 */
public interface StandPassive {
    ResourceLocation id();

    String translationKey();

    /** Runs every tick while the Stand is manifested. */
    default void tick(ServerPlayer player, JojohaPlayerData data) {
    }

    /**
     * Adjusts incoming damage before it is applied.
     *
     * @return the damage to actually deal - return the amount unchanged to stay out of the way
     */
    default float onIncomingDamage(ServerPlayer player, JojohaPlayerData data, float amount) {
        return amount;
    }

    /**
     * Adjusts damage this player's Stand is about to deal.
     *
     * <p>Given the target, because the interesting passives are about the relationship between the
     * two rather than about the number - how far apart they are, what the target is, what it was
     * doing. A passive that only wanted to scale damage could have been a stat.
     *
     * @return the damage to actually deal - return the amount unchanged to stay out of the way
     */
    default float onOutgoingDamage(ServerPlayer player, JojohaPlayerData data, LivingEntity target,
                                   float amount) {
        return amount;
    }
}
