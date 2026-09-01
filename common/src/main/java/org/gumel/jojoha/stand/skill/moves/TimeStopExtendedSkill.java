package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.StandSkill;
import org.gumel.jojoha.stand.skill.TimeStopCast;

/**
 * Time Stop held longer, at the price of waiting far longer for it back.
 *
 * <p>A separate move rather than an upgrade applied to the old one. The distinction matters on the
 * bar: this and Time Stop occupy the same role, so holding both would be paying two slots for one
 * ability - which the loadout refuses. See {@link #replaces}.
 *
 * <p>It reuses Time Stop's own charge and duration maths rather than restating it, so everything
 * that moves the ordinary stop - Endurance, the practice bonus, the vampire ceiling - moves this one
 * by the same amount before the extension is applied on top.
 */
public final class TimeStopExtendedSkill implements StandSkill {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "time_stop_extended");
    public static final TimeStopExtendedSkill INSTANCE = new TimeStopExtendedSkill();

    /** Half again as long, and getting on for twice the wait. */
    private static final float EXTENSION = 1.5F;
    private static final int COOLDOWN_TICKS = 700;
    private static final float ENERGY_COST = EnergyWeight.ULTIMATE.cost();

    private TimeStopExtendedSkill() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "skill.jojoha.time_stop_extended";
    }

    /** The stop it is a longer version of. One role, one slot. */
    @Override
    public ResourceLocation replaces() {
        return TimeStopSkill.ID;
    }

    /**
     * Both gates, exactly as the ordinary stop has them.
     *
     * <p>The node has to be taken and the exposures still have to have happened - a longer Time Stop
     * is still a Time Stop, and nothing about buying the longer one teaches you the thing itself.
     */
    @Override
    public boolean isUnlocked(JojohaPlayerData data) {
        return TimeStopSkill.hasLearned(data)
                && org.gumel.jojoha.skilltree.SkillTrees.skillUnlocked(data, ID);
    }

    @Override
    public int cooldownTicks() {
        return COOLDOWN_TICKS;
    }

    @Override
    public int chargeMaxTicks() {
        return TimeStopSkill.INSTANCE.chargeMaxTicks();
    }

    @Override
    public float energyCost() {
        return ENERGY_COST;
    }

    @Override
    public boolean activate(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        return activate(player, data, stand, chargeMaxTicks());
    }

    @Override
    public boolean activate(ServerPlayer player, JojohaPlayerData data, StandEntity stand,
                            int chargeTicks) {
        // Counted the same way the ordinary stop counts, so practice earned here is practice with
        // Time Stop rather than a second, separate tally that never adds up to anything.
        data.timeStopCasts++;
        data.timeStopUsesThisFight++;

        int ticks = Math.round(TimeStopSkill.durationTicks(data, chargeTicks) * EXTENSION);
        TimeStopCast.begin(player, Math.max(1, ticks));
        return true;
    }
}
