package org.gumel.jojoha.stand.passive;

import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

public final class CombatAdaptationPassive implements StandPassive {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "combat_adaptation");

    public static final CombatAdaptationPassive INSTANCE = new CombatAdaptationPassive();

    private CombatAdaptationPassive() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "passive.jojoha.combat_adaptation";
    }
}
