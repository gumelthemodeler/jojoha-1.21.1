package org.gumel.jojoha.stand.passive;

import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

public final class BossPresencePassive implements StandPassive {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "boss_presence");
    public static final BossPresencePassive INSTANCE = new BossPresencePassive();

    private BossPresencePassive() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "passive.jojoha.boss_presence";
    }
}
