package org.gumel.jojoha.stand.combat;

import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.data.StandTuning;
import org.gumel.jojoha.stand.StandTypes;

import java.util.Locale;
import java.util.Map;

public final class StandCombatProfiles {

    private static final Map<ResourceLocation, StandCombatProfile> AUTHORED = Map.of(
            StandTypes.STAR_PLATINUM_ID, StandCombatProfile.STAR_PLATINUM,
            StandTypes.THE_WORLD_ID, StandCombatProfile.THE_WORLD,
            StandTypes.SECRET_THE_WORLD_ID, StandCombatProfile.THE_WORLD);

    private StandCombatProfiles() {
    }

    public static StandCombatProfile of(ResourceLocation standId) {
        StandCombatProfile authored = standId == null ? StandCombatProfile.NEUTRAL
                : AUTHORED.getOrDefault(standId, StandCombatProfile.NEUTRAL);
        if (standId == null) {
            return authored;
        }
        String stand = "profile_" + standId.getPath() + "_";
        PunchType[] types = PunchType.values();
        double[] weights = new double[types.length];
        for (PunchType type : types) {
            weights[type.ordinal()] = StandTuning.value(stand + "weight_" + type.name().toLowerCase(Locale.ROOT),
                    (float) authored.weight(type));
        }
        return new StandCombatProfile(
                StandTuning.value(stand + "spread", (float) authored.spread()),
                StandTuning.value(stand + "lead", (float) authored.lead()),
                StandTuning.value(stand + "momentum_gain", (float) authored.momentumGain()),
                StandTuning.ticks(stand + "extra_afterimages", authored.extraAfterimages()),
                StandTuning.value(stand + "clash_strength", (float) authored.clashStrength()),
                StandTuning.value(stand + "finisher_scale", authored.finisherScale()),
                weights).bounded();
    }
}
