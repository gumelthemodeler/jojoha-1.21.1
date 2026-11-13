package org.gumel.jojoha.stand.passive;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;

public final class MadeForThisPassive implements StandPassive {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "made_for_this");

    public static final MadeForThisPassive INSTANCE = new MadeForThisPassive();

    private static final int EVERY = 3;
    private static final int EXTRA = 1;

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
