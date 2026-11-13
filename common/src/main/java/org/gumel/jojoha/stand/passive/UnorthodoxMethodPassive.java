package org.gumel.jojoha.stand.passive;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.StatGrowth;
import org.gumel.jojoha.data.StatPoints;

public final class UnorthodoxMethodPassive implements StandPassive {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "unorthodox_method");

    public static final UnorthodoxMethodPassive INSTANCE = new UnorthodoxMethodPassive();

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

        StatGrowth.award(player,
                StatPoints.STRENGTH, WORTH);
    }
}
