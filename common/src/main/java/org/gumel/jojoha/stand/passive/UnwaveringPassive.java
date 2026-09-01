package org.gumel.jojoha.stand.passive;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;

/**
 * The user does not get moved, and does not get interrupted.
 *
 * <p>Knockback resistance is applied as a real attribute modifier rather than by cancelling
 * knockback after the fact. Everything that pushes a player - attacks, explosions, pistons - already
 * consults that attribute, so one modifier covers all of them, and anything that respects it in
 * future is covered for free.
 *
 * <p>The modifier is replaced rather than stacked on each application, and it is removed the moment
 * the Stand is gone. A modifier added every tick and never cleaned up is how a player ends up
 * permanently immovable with no way to work out why.
 *
 * <p>Interruption is handled by clearing the hurt timer: that field is what drives the stagger
 * animation and the pause it imposes, so zeroing it lets the user keep swinging through a hit.
 */
public final class UnwaveringPassive implements StandPassive {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "unwavering");
    public static final UnwaveringPassive INSTANCE = new UnwaveringPassive();

    private static final ResourceLocation MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "unwavering_knockback");

    /** Knockback resistance runs 0-1, so these are straight fractions of "immovable". */
    private static final double BASE_RESISTANCE = 0.5;
    private static final double WOUNDED_RESISTANCE = 0.9;
    /** Below this share of max health the Stand digs in - the doc's "more powerful below 50% HP". */
    private static final float WOUNDED_HEALTH_FRACTION = 0.5F;

    private UnwaveringPassive() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "passive.jojoha.unwavering";
    }

    @Override
    public void tick(ServerPlayer player, JojohaPlayerData data) {
        AttributeInstance knockback = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (knockback == null) {
            return;
        }

        boolean wounded = player.getHealth() <= player.getMaxHealth() * WOUNDED_HEALTH_FRACTION;
        double resistance = wounded ? WOUNDED_RESISTANCE : BASE_RESISTANCE;

        AttributeModifier existing = knockback.getModifier(MODIFIER_ID);
        if (existing != null && existing.amount() == resistance) {
            clearStagger(player);
            return;
        }

        // Removed first: addPermanentModifier on an id that is already present is rejected, so the
        // value would silently never change between the wounded and healthy states.
        knockback.removeModifier(MODIFIER_ID);
        knockback.addTransientModifier(new AttributeModifier(
                MODIFIER_ID, resistance, AttributeModifier.Operation.ADD_VALUE));
        clearStagger(player);
    }

    /** Called when the Stand goes away, so the resistance goes with it. */
    public static void clear(ServerPlayer player) {
        AttributeInstance knockback = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (knockback != null) {
            knockback.removeModifier(MODIFIER_ID);
        }
    }

    private static void clearStagger(ServerPlayer player) {
        player.hurtTime = 0;
        player.hurtDuration = 0;
    }
}
