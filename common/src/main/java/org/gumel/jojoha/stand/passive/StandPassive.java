package org.gumel.jojoha.stand.passive;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.data.JojohaPlayerData;

public interface StandPassive {
    ResourceLocation id();

    default ResourceLocation replaces() {
        return null;
    }

    String translationKey();

    default void tick(ServerPlayer player, JojohaPlayerData data) {
    }

    default float onOutgoingDamage(ServerPlayer player, JojohaPlayerData data, LivingEntity target,
                                   float amount) {
        return amount;
    }

    default float onRangedHit(ServerPlayer player, JojohaPlayerData data, LivingEntity target,
                              Vec3 from, float amount) {
        return amount;
    }
}
