package org.gumel.jojoha.stand.skill;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;

/**
 * What goes on the bar, and the only thing allowed to decide.
 *
 * <p>Server side. Every path in here starts from a request a client sent and ends by writing the
 * player's own record, so all of it is the same job: work out whether the request was legitimate
 * and, if it was, do the smallest version of it.
 */
public final class SkillLoadout {
    private SkillLoadout() {
    }

    /**
     * Puts a move on the bar.
     *
     * <p>A move can only be in one slot. Equipping one that is already somewhere moves it rather
     * than copying it - two slots casting the same thing is not a loadout, it is a mistake nobody
     * would make on purpose and everybody would make by accident.
     *
     * @param slot where to put it, or {@code -1} to use the first free slot
     * @return whether anything changed
     */
    public static boolean equip(ServerPlayer player, ResourceLocation skillId, int slot,
                               boolean utility) {
        JojohaPlayerData data = PlayerDataAccess.get(player);

        StandSkill skill = StandSkills.byId(skillId);
        if (!StandSkills.canEquip(data, skill)) {
            return false;
        }

        ResourceLocation[] bar = prepare(data, utility);

        // A move and the move it replaces answer the same question, so the bar holds one or the
        // other. Refused rather than silently swapped: taking something off the bar is the player's
        // decision, and a request that quietly removes a move they did not name is worse than one
        // that does nothing.
        int clash = conflicting(bar, skill, slot);
        if (clash >= 0) {
            return false;
        }

        int target = slot >= 0 ? slot : firstFree(bar);
        if (target < 0 || target >= bar.length) {
            return false;
        }

        // Taken off wherever it was first, so a move asked for twice ends up in one place.
        int existing = slotOf(bar, skillId);
        if (existing >= 0) {
            bar[existing] = null;
        }

        bar[target] = skillId;
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
        return true;
    }

    /** Takes a move off the bar, wherever it is. */
    public static boolean unequip(ServerPlayer player, ResourceLocation skillId, boolean utility) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        ResourceLocation[] bar = prepare(data, utility);

        int slot = slotOf(bar, skillId);
        if (slot < 0) {
            return false;
        }

        bar[slot] = null;
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
        return true;
    }

    /** Empties one slot by position, for a client that pointed at the bar rather than at a move. */
    public static boolean clear(ServerPlayer player, int slot, boolean utility) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        ResourceLocation[] bar = prepare(data, utility);

        if (slot < 0 || slot >= bar.length || bar[slot] == null) {
            return false;
        }

        bar[slot] = null;
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
        return true;
    }

    /**
     * The bar being edited, made ready to be edited.
     *
     * <p>The Utility bar is empty until somebody arranges it, and while it is empty the stance shows
     * the stock tools. Writing a single slot into an otherwise empty array would therefore not add a
     * tool - it would silently delete the other four, because the stored bar becomes the whole answer
     * the moment it stops being empty. So the first edit copies the stock tools in first, and the
     * change lands on top of them.
     */
    private static ResourceLocation[] prepare(JojohaPlayerData data, boolean utility) {
        ResourceLocation[] bar = data.loadout(utility);
        if (!utility || !data.utilityLoadoutUntouched()) {
            return bar;
        }

        for (int slot = 0; slot < StandSkills.UTILITY_TOOLS.size() && slot < bar.length; slot++) {
            bar[slot] = StandSkills.UTILITY_TOOLS.get(slot).id();
        }
        return bar;
    }

    /**
     * Where an alternative to this move already sits on the bar, or -1.
     *
     * <p>The slot being written to is excluded: replacing a move with its own variant in the very
     * slot it occupies is the one case where holding both is not what is being asked for.
     */
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
