package org.gumel.jojoha.stand;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.StandData;
import org.gumel.jojoha.skilltree.SkillTrees;
import org.gumel.jojoha.stand.anubis.Anubis;

import java.util.List;
import java.util.Set;

public final class StandRelease {

    private StandRelease() {
    }

    public static boolean release(ServerPlayer player, JojohaPlayerData data) {
        if (!data.stand.isPresent()) {
            return false;
        }

        if (data.standSummoned) {
            StandSummonHandler.dismissImmediately(player, data);
        }
        if (Anubis.inStandSlot(data)) {
            Anubis.forgetHolder(player);
        }

        Set<ResourceLocation> standNodes = SkillTrees.allStandNodeIds();
        data.getUnlockedNodes().removeAll(standNodes);
        data.setChosenPassives(List.of());
        Spirit.released(player, data.stand.standId());
        data.stand = StandData.NONE;
        data.dropUnequippable();
        return true;
    }
}
