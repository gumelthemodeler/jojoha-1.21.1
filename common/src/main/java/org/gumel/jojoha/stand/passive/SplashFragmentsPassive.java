package org.gumel.jojoha.stand.passive;

import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

public final class SplashFragmentsPassive implements StandPassive {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "splash_fragments");

    public static final SplashFragmentsPassive INSTANCE = new SplashFragmentsPassive();

    public static final float CHANCE = 0.25F;

    public static final int FRAGMENTS = 4;
    public static final float SHARE = 0.4F;

    public static final float SPREAD = 18F;

    private SplashFragmentsPassive() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "passive.jojoha.splash_fragments";
    }
}
