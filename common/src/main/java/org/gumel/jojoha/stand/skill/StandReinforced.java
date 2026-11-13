package org.gumel.jojoha.stand.skill;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.stand.skill.moves.BarrageSkill;
import org.gumel.jojoha.stand.skill.moves.BurstSkill;
import org.gumel.jojoha.stand.skill.moves.DodgeSkill;
import org.gumel.jojoha.stand.skill.moves.StandDashSkill;
import org.gumel.jojoha.stand.skill.moves.StandLeapSkill;
import org.gumel.jojoha.stand.skill.moves.SwordFlurrySkill;
import org.gumel.jojoha.stand.skill.moves.TimeSkipSkill;

import java.util.Map;
import java.util.Set;

public final class StandReinforced {
    private static final Map<ResourceLocation, ResourceLocation> UPGRADES = Map.of(
            DodgeSkill.ID, StandDashSkill.ID,
            BurstSkill.ID, StandLeapSkill.ID);

    private static final Map<ResourceLocation, ResourceLocation> BASES = Map.of(
            StandDashSkill.ID, DodgeSkill.ID,
            TimeSkipSkill.ID, DodgeSkill.ID,
            StandLeapSkill.ID, BurstSkill.ID,
            SwordFlurrySkill.ID, BarrageSkill.ID);

    private static final Set<ResourceLocation> READY_WHEN_PARTIAL = Set.of(
            BarrageSkill.ID, SwordFlurrySkill.ID, StandDashSkill.ID);

    private StandReinforced() {
    }

    public static ResourceLocation resolve(JojohaPlayerData data, ResourceLocation id) {
        ResourceLocation upgraded = upgradeFor(data, id);
        return upgraded == null ? id : upgraded;
    }

    public static ResourceLocation upgradeFor(JojohaPlayerData data, ResourceLocation id) {
        if (id == null || !data.standOut()) {
            return null;
        }

        ResourceLocation upgrade = UPGRADES.get(id);
        if (upgrade == null) {
            return null;
        }

        if (StandDashSkill.ID.equals(upgrade) && data.timeSkipDash && ready(data, TimeSkipSkill.ID)) {
            return TimeSkipSkill.ID;
        }
        return ready(data, upgrade) ? upgrade : null;
    }

    public static boolean isReinforced(JojohaPlayerData data, ResourceLocation id) {
        return upgradeFor(data, id) != null;
    }

    public static boolean rides(ResourceLocation id) {
        return id != null && BASES.containsKey(id);
    }

    public static ResourceLocation baseOf(ResourceLocation id) {
        return id == null ? null : BASES.getOrDefault(id, id);
    }

    public static boolean readyWhenPartial(ResourceLocation id) {
        return READY_WHEN_PARTIAL.contains(id);
    }

    public static boolean owns(JojohaPlayerData data, ResourceLocation id) {
        StandSkill skill = StandSkills.byId(id);
        return skill != null && skill.isUnlocked(data) && StandSkills.inMoveset(data, skill);
    }

    private static boolean ready(JojohaPlayerData data, ResourceLocation id) {
        return owns(data, id) && data.stand.trust().level()
                >= StandSkills.requiredTrust(StandSkills.byId(id)).level();
    }

    public static boolean dashChoice(JojohaPlayerData data) {
        return owns(data, TimeSkipSkill.ID) && owns(data, StandDashSkill.ID);
    }

    public static boolean dashes(JojohaPlayerData data, ResourceLocation form) {
        return TimeSkipSkill.ID.equals(form) == data.timeSkipDash;
    }

    public static void chooseDash(ServerPlayer player, boolean timeSkip) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (data.timeSkipDash == timeSkip || timeSkip && !dashChoice(data)) {
            return;
        }
        data.timeSkipDash = timeSkip;
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
    }
}
