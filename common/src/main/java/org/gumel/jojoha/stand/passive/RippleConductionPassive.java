package org.gumel.jojoha.stand.passive;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.Fate;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.hamon.HamonCombat;
import org.gumel.jojoha.hamon.HamonFx;

public final class RippleConductionPassive implements StandPassive {
    public static final RippleConductionPassive INSTANCE = new RippleConductionPassive();

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "ripple_conduction");

    private static final float UNDEAD_BONUS = 6.0F;

    private RippleConductionPassive() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "passive.jojoha.ripple_conduction";
    }

    @Override
    public float onOutgoingDamage(ServerPlayer player, JojohaPlayerData data, LivingEntity target,
                                  float amount) {
        if (!Fate.breathesHamon(data) || !HamonCombat.isUndead(target)) {
            return amount;
        }

        float charged = amount + HamonCombat.scaled(player, UNDEAD_BONUS);

        HamonFx.spark(player.serverLevel(), target.getX(),
                target.getY() + target.getBbHeight() * 0.5, target.getZ(), 14, 0.35, 0.04);
        HamonFx.ring(player.serverLevel(), target.getX(),
                target.getY() + target.getBbHeight() * 0.5, target.getZ(), 0.9, 1);
        return charged;
    }
}
