package org.gumel.jojoha.stand.passive;

import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;

public final class SecondSightPassive implements StandPassive {
    public static final SecondSightPassive INSTANCE = new SecondSightPassive();

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "second_sight");

    private static final float DURATION_SCALE = 1.75F;
    private static final float COOLDOWN_SCALE = 0.75F;

    private SecondSightPassive() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "passive.jojoha.second_sight";
    }

    public static int durationFor(JojohaPlayerData data, int base) {
        return StandPassives.grants(data, ID) ? Math.round(base * DURATION_SCALE) : base;
    }

    public static int cooldownFor(JojohaPlayerData data, int base) {
        return StandPassives.grants(data, ID) ? Math.round(base * COOLDOWN_SCALE) : base;
    }
}
