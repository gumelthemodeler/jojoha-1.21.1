package org.gumel.jojoha.stand.passive;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.stand.heaven.Rewrites;

public final class RealityIsMinePassive implements StandPassive {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "reality_is_mine");
    public static final RealityIsMinePassive INSTANCE = new RealityIsMinePassive();

    public static final float CHANCE = 0.25F;

    private RealityIsMinePassive() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "passive.jojoha.reality_is_mine";
    }

    @Override
    public float onOutgoingDamage(ServerPlayer player, JojohaPlayerData data, LivingEntity target,
                                  float amount) {
        if (player.getRandom().nextFloat() < CHANCE) {
            Rewrites.strike(player, target);
        }
        return amount;
    }
}
