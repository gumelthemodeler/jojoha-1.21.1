package org.gumel.jojoha.stand.passive;

import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;

public final class SensoryPerceptionPassive implements StandPassive {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "sensory_perception");

    public static final double RANGE = 12.0;

    public static final SensoryPerceptionPassive INSTANCE = new SensoryPerceptionPassive();

    private SensoryPerceptionPassive() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "passive." + ID.getNamespace() + "." + ID.getPath();
    }

    public double range() {
        return RANGE;
    }

    public static double rangeFor(JojohaPlayerData data) {
        return StandPassives.grants(data, ID) ? RANGE : 0;
    }
}
