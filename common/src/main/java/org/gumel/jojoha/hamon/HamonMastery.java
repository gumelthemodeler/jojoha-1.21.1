package org.gumel.jojoha.hamon;

import net.minecraft.server.level.ServerPlayer;
import org.gumel.jojoha.config.JojohaServerConfig;
import org.gumel.jojoha.data.Discipline;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.VampireStage;
import org.gumel.jojoha.item.StandArrowRitual;
import org.gumel.jojoha.stand.StandTypes;
import org.gumel.jojoha.stand.skill.StandBeat;

public final class HamonMastery {
    public static final float FULL_BARS = 50F;

    private HamonMastery() {
    }

    public static float needed() {
        return JojohaServerConfig.HERMIT_MASTERY.value() * JojohaPlayerData.MAX_SPEC_ENERGY;
    }

    public static void spent(ServerPlayer player, JojohaPlayerData data, float amount) {
        if (amount > 0F) {
            HamonCombat.owed(player);
        }
        float needed = needed();
        if (amount <= 0F || needed <= 0F || data.discipline != Discipline.HAMON
                || data.vampireStage != VampireStage.NONE || data.stand.isPresent()
                || data.standArrowRitualTicks > 0) {
            return;
        }
        data.hamonSpent += amount;
        if (data.hamonSpent < needed) {
            return;
        }
        data.hamonSpent = 0F;
        StandBeat.at(1, player, () -> StandArrowRitual.grant(player,
                StandTypes.HERMIT_PURPLE_ID, "message.jojoha.hermit_mastery"));
    }
}
