package org.gumel.jojoha.stand;

import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.stand.passive.BossPresencePassive;
import org.gumel.jojoha.stand.passive.DeadeyePassive;
import org.gumel.jojoha.stand.passive.EnhancedReflexesPassive;
import org.gumel.jojoha.stand.passive.HeavenTierPassive;
import org.gumel.jojoha.stand.passive.MadeForThisPassive;
import org.gumel.jojoha.stand.passive.OpportunistPassive;
import org.gumel.jojoha.stand.passive.UnorthodoxMethodPassive;
import org.gumel.jojoha.stand.passive.UnwaveringPassive;

import java.util.List;
import java.util.Locale;

public enum StandArchetype {
    BRAWLER(List.of(EnhancedReflexesPassive.ID, UnwaveringPassive.ID)),

    RANGED(List.of(DeadeyePassive.ID,
            OpportunistPassive.ID)),

    CONTROL(List.of(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "territorial"),
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "encroachment"))),

    SPECIALIST(List.of(MadeForThisPassive.ID,
            UnorthodoxMethodPassive.ID)),

    HEAVEN(List.of(BossPresencePassive.ID,
            HeavenTierPassive.ASCENDED_ID,
            HeavenTierPassive.TRANSCENDENT_ID,
            HeavenTierPassive.HEAVEN_ID));

    private final List<ResourceLocation> passives;

    StandArchetype(List<ResourceLocation> passives) {
        this.passives = passives;
    }

    public List<ResourceLocation> passives() {
        return passives;
    }

    public String translationKey() {
        return "archetype.jojoha." + name().toLowerCase(Locale.ROOT);
    }

    public String descriptionKey() {
        return translationKey() + ".desc";
    }
}
