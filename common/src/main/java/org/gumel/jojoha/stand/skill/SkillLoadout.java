package org.gumel.jojoha.stand.skill;

import dev.architectury.event.events.common.TickEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.skill.Moves;
import org.gumel.jojoha.skilltree.SkillTrees;

public final class SkillLoadout {
    private static final int CHECK_EVERY = 20;

    private SkillLoadout() {
    }

    public static void init() {
        TickEvent.PLAYER_POST.register(player -> {
            if (player instanceof ServerPlayer server && server.tickCount % CHECK_EVERY == 0) {
                tidy(server);
            }
        });
    }

    public static boolean tidy(ServerPlayer player) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        boolean relocked = SkillTrees.relock(data);
        if (!data.dropUnequippable() && !relocked) {
            return false;
        }
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
        return true;
    }

    public static boolean equip(ServerPlayer player, ResourceLocation skillId, int slot) {
        JojohaPlayerData data = PlayerDataAccess.get(player);

        if (!Moves.canEquip(data, skillId)) {
            return false;
        }

        boolean utility = slot >= StandSkills.UTILITY_SLOT
                || slot < 0 && !StandSkills.fitsCombat(data, skillId);
        if (utility ? !StandSkills.fitsUtility(data, skillId) : !StandSkills.fitsCombat(data, skillId)) {
            return false;
        }

        StandSkill skill = StandSkills.byId(skillId);

        ResourceLocation[] bar = utility ? data.utilitySkills : data.equippedSkills;
        int asked = slot >= StandSkills.UTILITY_SLOT ? slot - StandSkills.UTILITY_SLOT : slot;

        int clash = conflicting(bar, skill, asked);
        if (clash >= 0) {
            return false;
        }

        int target = asked >= 0 ? asked : firstFree(bar);
        if (target < 0 || target >= bar.length) {
            return false;
        }

        int existing = slotOf(bar, skillId);
        if (existing >= 0) {
            bar[existing] = null;
        }

        bar[target] = skillId;
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
        return true;
    }

    public static boolean unequip(ServerPlayer player, ResourceLocation skillId) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        ResourceLocation[] bar = data.equippedSkills;

        int slot = slotOf(bar, skillId);
        if (slot < 0) {
            bar = data.utilitySkills;
            slot = slotOf(bar, skillId);
        }
        if (slot < 0) {
            return false;
        }

        bar[slot] = null;
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
        return true;
    }

    public static boolean clear(ServerPlayer player, int slot) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        boolean utility = slot >= StandSkills.UTILITY_SLOT;
        ResourceLocation[] bar = utility ? data.utilitySkills : data.equippedSkills;
        int at = utility ? slot - StandSkills.UTILITY_SLOT : slot;

        if (at < 0 || at >= bar.length || bar[at] == null) {
            return false;
        }

        bar[at] = null;
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
        return true;
    }

    private static int conflicting(ResourceLocation[] bar, StandSkill skill, int slot) {
        for (int i = 0; i < bar.length; i++) {
            if (i == slot || bar[i] == null) {
                continue;
            }
            if (StandSkills.conflict(skill, StandSkills.byId(bar[i]))) {
                return i;
            }
        }
        return -1;
    }

    private static int slotOf(ResourceLocation[] bar, ResourceLocation skillId) {
        for (int slot = 0; slot < bar.length; slot++) {
            if (skillId.equals(bar[slot])) {
                return slot;
            }
        }
        return -1;
    }

    private static int firstFree(ResourceLocation[] bar) {
        for (int slot = 0; slot < bar.length; slot++) {
            if (bar[slot] == null) {
                return slot;
            }
        }
        return -1;
    }
}
