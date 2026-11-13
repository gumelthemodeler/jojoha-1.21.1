package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.VampireStage;
import org.gumel.jojoha.skilltree.SkillTrees;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.TrustTier;
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.StandSkill;

public final class TimeStopUpgradeSkill implements StandSkill {
    public static final ResourceLocation EXTENDED_ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "time_stop_extended");
    public static final ResourceLocation PERFECT_ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "time_stop_perfect");

    public static final TimeStopUpgradeSkill EXTENDED = new TimeStopUpgradeSkill(EXTENDED_ID, 120, 700, false);
    public static final TimeStopUpgradeSkill PERFECT = new TimeStopUpgradeSkill(PERFECT_ID, 160, 800, true);

    private static final float ENERGY_COST = EnergyWeight.ULTIMATE.cost();

    private final ResourceLocation id;
    private final float extension;
    private final int cooldownTicks;
    private final boolean vampireOnly;

    private TimeStopUpgradeSkill(ResourceLocation id, int stopTicks, int cooldownTicks, boolean vampireOnly) {
        this.id = id;
        this.extension = stopTicks / (float) TimeStopSkill.FULL_STOP_TICKS;
        this.cooldownTicks = cooldownTicks;
        this.vampireOnly = vampireOnly;
    }

    @Override
    public ResourceLocation id() {
        return id;
    }

    @Override
    public String translationKey() {
        return "skill.jojoha." + id.getPath();
    }

    @Override
    public ResourceLocation replaces() {
        return TimeStopSkill.ID;
    }

    @Override
    public boolean isUnlocked(JojohaPlayerData data) {
        return TimeStopSkill.hasLearned(data)
                && (!vampireOnly || data.vampireStage != VampireStage.NONE)
                && SkillTrees.skillUnlocked(data, id);
    }

    @Override
    public int cooldownTicks() {
        return cooldownTicks;
    }

    @Override
    public int chargeMaxTicks() {
        return TimeStopSkill.INSTANCE.chargeMaxTicks();
    }

    @Override
    public int chargeMaxTicks(JojohaPlayerData data) {
        return TimeStopSkill.INSTANCE.chargeMaxTicks(data);
    }

    @Override
    public boolean chargesTime() {
        return true;
    }

    @Override
    public int timeStopTicks(JojohaPlayerData data) {
        return TimeStopSkill.stopTicks(data, extension);
    }

    @Override
    public boolean chargeBreaksOnDamage() {
        return true;
    }

    @Override
    public float energyCost() {
        return ENERGY_COST;
    }

    @Override
    public TrustTier minimumTrust() {
        return TimeStopSkill.INSTANCE.minimumTrust();
    }

    @Override
    public int lockoutTicks(JojohaPlayerData data) {
        return TimeStopSkill.INSTANCE.lockoutTicks(data);
    }

    @Override
    public boolean isRunning(JojohaPlayerData data) {
        return TimeStopSkill.INSTANCE.isRunning(data);
    }

    @Override
    public boolean canBeCalledOff(JojohaPlayerData data) {
        return TimeStopSkill.INSTANCE.canBeCalledOff(data);
    }

    @Override
    public void callOff(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        TimeStopSkill.INSTANCE.callOff(player, data, stand);
    }

    @Override
    public boolean activate(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        return activate(player, data, stand, chargeMaxTicks(data));
    }

    @Override
    public boolean activate(ServerPlayer player, JojohaPlayerData data, StandEntity stand,
                            int chargeTicks) {
        return TimeStopSkill.cast(player, data, chargeTicks, TimeStopSkill.stopTicks(data, extension));
    }
}
