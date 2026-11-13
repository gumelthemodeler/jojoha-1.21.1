package org.gumel.jojoha.stand.passive;

import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

public final class ReboundPassive implements StandPassive {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "rebound");

    public static final ReboundPassive INSTANCE = new ReboundPassive();

    public static final float DAMAGE_KEPT = 0.8F;
    public static final double SPEED_KEPT = 0.85;

    private ReboundPassive() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "passive.jojoha.rebound";
    }
}
