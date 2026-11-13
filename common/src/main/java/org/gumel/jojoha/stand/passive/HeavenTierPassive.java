package org.gumel.jojoha.stand.passive;

import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.stand.heaven.Ascension;

import java.util.List;

public final class HeavenTierPassive implements StandPassive {
    public static final ResourceLocation ASCENDED_ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "ascended");
    public static final ResourceLocation TRANSCENDENT_ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "transcendent");
    public static final ResourceLocation HEAVEN_ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "heaven");

    public static final HeavenTierPassive ASCENDED = new HeavenTierPassive(ASCENDED_ID, Ascension.ASCENDED);
    public static final HeavenTierPassive TRANSCENDENT = new HeavenTierPassive(TRANSCENDENT_ID, Ascension.TRANSCENDENT);
    public static final HeavenTierPassive HEAVEN = new HeavenTierPassive(HEAVEN_ID, Ascension.HEAVEN);

    private static final List<HeavenTierPassive> TIERS = List.of(HEAVEN, TRANSCENDENT, ASCENDED);

    private final ResourceLocation id;
    private final float threshold;

    private HeavenTierPassive(ResourceLocation id, float threshold) {
        this.id = id;
        this.threshold = threshold;
    }

    public static HeavenTierPassive byId(ResourceLocation id) {
        for (HeavenTierPassive tier : TIERS) {
            if (tier.id.equals(id)) {
                return tier;
            }
        }
        return null;
    }

    public static HeavenTierPassive highest(float ascension) {
        for (HeavenTierPassive tier : TIERS) {
            if (ascension >= tier.threshold - 0.001F) {
                return tier;
            }
        }
        return null;
    }

    public float threshold() {
        return threshold;
    }

    @Override
    public ResourceLocation id() {
        return id;
    }

    @Override
    public String translationKey() {
        return "passive.jojoha." + id.getPath();
    }
}
