package org.gumel.jojoha.stand.passive;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.registry.ModRegistries;

public final class ThornbindPassive implements StandPassive {
    public static final ThornbindPassive INSTANCE = new ThornbindPassive();

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "thornbind");

    private static final int BIND_TICKS = 100;
    private static final int BIND_LEVEL = 0;

    private static final float MINIMUM_BITE = 2.0F;

    private ThornbindPassive() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "passive.jojoha.thornbind";
    }

    @Override
    public float onOutgoingDamage(ServerPlayer player, JojohaPlayerData data, LivingEntity target,
                                  float amount) {
        if (amount < MINIMUM_BITE) {
            return amount;
        }

        target.addEffect(new MobEffectInstance(
                MobEffects.WITHER, BIND_TICKS, BIND_LEVEL));

        player.serverLevel().sendParticles(ModRegistries.BLOOD_MOTE.get(),
                target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(),
                8, 0.25, 0.3, 0.25, 0.01);
        return amount;
    }
}
