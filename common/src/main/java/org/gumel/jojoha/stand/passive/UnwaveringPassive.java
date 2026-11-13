package org.gumel.jojoha.stand.passive;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;

public final class UnwaveringPassive implements StandPassive {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "unwavering");
    public static final UnwaveringPassive INSTANCE = new UnwaveringPassive();

    private static final ResourceLocation MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "unwavering_knockback");

    private static final double BASE_RESISTANCE = 0.5;
    private static final double WOUNDED_RESISTANCE = 0.9;
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

        knockback.removeModifier(MODIFIER_ID);
        knockback.addTransientModifier(new AttributeModifier(
                MODIFIER_ID, resistance, AttributeModifier.Operation.ADD_VALUE));
        clearStagger(player);
    }

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
