package org.gumel.jojoha.skill;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.gumel.jojoha.combat.MovementCharges;
import org.gumel.jojoha.data.Discipline;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.VampireStage;
import org.gumel.jojoha.hamon.HamonMove;
import org.gumel.jojoha.hamon.HamonMoves;
import org.gumel.jojoha.hamon.HamonPaths;
import org.gumel.jojoha.skilltree.SkillTrees;
import org.gumel.jojoha.stand.skill.StandReinforced;
import org.gumel.jojoha.stand.skill.StandSkill;
import org.gumel.jojoha.stand.skill.StandSkills;
import org.gumel.jojoha.vampire.Headless;
import org.gumel.jojoha.vampire.VampireMove;
import org.gumel.jojoha.vampire.VampireMoves;

import java.util.Objects;

public final class Moves {

    private Moves() {
    }

    public static String benched(JojohaPlayerData data, ResourceLocation id) {
        if (id == null) {
            return null;
        }
        StandSkill stand = StandSkills.byId(id);
        if (data.inUtilityStance()) {
            boolean allowed = stand != null && (stand.passiveOnly() || StandSkills.utilityTool(data, stand)
                    || MovementCharges.kindOf(
                            StandReinforced.baseOf(id)) != null);
            return allowed ? null : "message.jojoha.skill.utility_stance";
        }
        if (stand == null) {
            return null;
        }
        if (StandSkills.utilityOnly(data, stand)) {
            return "message.jojoha.skill.utility_only";
        }
        return stand.requiresStand() && !data.standOut() ? "message.jojoha.skill.no_stand" : null;
    }

    public static int scaledCooldown(JojohaPlayerData data, ResourceLocation id) {
        StandSkill stand = StandSkills.byId(id);
        if (stand != null) {
            return StandSkills.scaledCooldown(stand, data);
        }

        HamonMove hamon = HamonMoves.byId(id);
        if (hamon != null) {
            return hamon.cooldownTicks();
        }

        VampireMove vampire = VampireMoves.byId(id);
        return vampire == null ? 0 : vampire.cooldownTicks(data);
    }

    public static boolean canEquip(JojohaPlayerData data, ResourceLocation id) {
        if (id == null) {
            return false;
        }

        StandSkill stand = StandSkills.byId(id);
        if (stand != null) {
            return StandSkills.canEquip(data, stand);
        }

        HamonMove hamon = HamonMoves.byId(id);
        if (hamon != null) {
            return canUseHamon(data, id);
        }

        VampireMove vampire = VampireMoves.byId(id);
        if (vampire != null) {
            return canUseVampire(data, vampire);
        }

        return false;
    }

    public static boolean canUseHamon(JojohaPlayerData data, ResourceLocation id) {
        if (data.discipline != Discipline.HAMON) {
            return false;
        }

        boolean taught = data.getUnlockedHamonPaths().stream()
                .map(HamonPaths::byId)
                .filter(Objects::nonNull)
                .anyMatch(path -> path.moveIds().contains(id));

        return taught && SkillTrees.skillUnlocked(data, id);
    }

    public static boolean canUseVampire(JojohaPlayerData data, VampireMove move) {
        if (data.vampireStage == VampireStage.NONE) {
            return false;
        }
        return VampireMoves.unlocked(data, move) && SkillTrees.skillUnlocked(data, move.id());
    }

    public static boolean dispatch(ServerPlayer player, ResourceLocation id) {
        return dispatch(player, id, 0);
    }

    public static boolean dispatch(ServerPlayer player, ResourceLocation id, int charge) {
        if (id == null) {
            return false;
        }
        if (VampireMoves.byId(id) != null) {
            VampireMoves.handleUseRequest(player, id, charge);
            return true;
        }
        if (HamonMoves.byId(id) != null) {
            if (Headless.is(player)) {
                return true;
            }
            HamonMoves.handleUseRequest(player, id, charge);
            return true;
        }
        return false;
    }

    public static boolean isSustained(ResourceLocation id) {
        if (id == null) {
            return false;
        }

        StandSkill stand = StandSkills.byId(id);
        if (stand != null) {
            return stand.isSustained();
        }

        HamonMove hamon = HamonMoves.byId(id);
        if (hamon != null) {
            return hamon.isSustained();
        }

        VampireMove vampire = VampireMoves.byId(id);
        return vampire != null && vampire.isSustained();
    }
}
