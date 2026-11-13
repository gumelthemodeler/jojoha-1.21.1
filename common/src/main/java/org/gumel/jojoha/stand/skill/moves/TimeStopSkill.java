package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.StandTuning;
import org.gumel.jojoha.skilltree.SkillTrees;
import org.gumel.jojoha.speech.Speech;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.StandTypes;
import org.gumel.jojoha.stand.StandVoices;
import org.gumel.jojoha.stand.TrustTier;
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.StandSkill;
import org.gumel.jojoha.stand.skill.TimeStopCast;
import org.gumel.jojoha.stand.skill.TimeStopLine;
import org.gumel.jojoha.stand.skill.TimeStopSettle;
import org.gumel.jojoha.stand.skill.TimeStopSystem;

public final class TimeStopSkill implements StandSkill {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "time_stop");
    public static final TimeStopSkill INSTANCE = new TimeStopSkill();

    private static final int COOLDOWN_TICKS = 400;

    private static final int USES_PER_FIGHT = 3;
    private static final int EXHAUSTION_LOCKOUT_TICKS = 1200;
    private static final float ENERGY_COST = EnergyWeight.ULTIMATE.cost();

    public static final int FULL_STOP_TICKS = 100;

    public static final int HARD_CAP_TICKS = 180;

    public static final int TAP_TICKS = 5;

    public static final int TAP_STOP_TICKS = 50;

    public static final int BROKEN_COOLDOWN_TICKS = 100;

    private TimeStopSkill() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "skill.jojoha.time_stop";
    }

    @Override
    public int cooldownTicks() {
        return COOLDOWN_TICKS;
    }

    @Override
    public float energyCost() {
        return ENERGY_COST;
    }

    @Override
    public boolean isRunning(JojohaPlayerData data) {
        return data.timeStopCastTicks > 0 || data.timeStopHeldTicks > 0;
    }

    private static int usesPerFight() {
        return StandTuning.ticks("time_stop_uses_per_fight", USES_PER_FIGHT);
    }

    private static int lockoutTicks() {
        return StandTuning.ticks("time_stop_lockout_ticks",
                EXHAUSTION_LOCKOUT_TICKS);
    }

    private static int fullStopTicks() {
        return Math.min(HARD_CAP_TICKS, StandTuning.ticks("time_stop_ticks", FULL_STOP_TICKS));
    }

    @Override
    public int lockoutTicks(JojohaPlayerData data) {
        return data.timeStopUsesThisFight >= usesPerFight() ? lockoutTicks() : 0;
    }

    public static int usesLeftThisFight(JojohaPlayerData data) {
        return Math.max(0, usesPerFight() - data.timeStopUsesThisFight);
    }

    public static boolean shouldClearFightUses(JojohaPlayerData data, long gameTime) {
        if (data.timeStopUsesThisFight <= 0) {
            return false;
        }

        return data.combatTicks <= 0
                || (data.timeStopUsesThisFight >= usesPerFight()
                        && !data.isMoveOnCooldown(ID, gameTime));
    }

    @Override
    public int chargeMaxTicks() {
        return TimeStopLine.CHARGE_TICKS;
    }

    @Override
    public int chargeMaxTicks(JojohaPlayerData data) {
        return TimeStopLine.chargeTicks(data);
    }

    @Override
    public boolean chargesTime() {
        return true;
    }

    public static boolean charged(JojohaPlayerData data, int chargeTicks) {
        return chargeTicks >= TimeStopLine.chargeTicks(data);
    }

    public static boolean tapped(int chargeTicks) {
        return chargeTicks > 0 && chargeTicks <= TAP_TICKS;
    }

    public static int stopTicks(JojohaPlayerData data, float extension) {
        return Mth.clamp(Math.round(ceiling(data) * extension), 1, HARD_CAP_TICKS);
    }

    @Override
    public int timeStopTicks(JojohaPlayerData data) {
        return stopTicks(data, 1F);
    }

    @Override
    public boolean chargeBreaksOnDamage() {
        return true;
    }

    @Override
    public TrustTier minimumTrust() {
        return TrustTier.BONDED;
    }

    public static final int REQUIRED_EXPOSURES = 3;

    public static boolean hasLearned(JojohaPlayerData data) {
        return data.timeStopExposures >= REQUIRED_EXPOSURES || bornWithIt(data);
    }

    public static boolean bornWithIt(JojohaPlayerData data) {
        return data.stand.isPresent()
                && StandTypes.THE_WORLD_ID.equals(data.stand.standId());
    }

    @Override
    public boolean isUnlocked(JojohaPlayerData data) {
        return hasLearned(data)
                && SkillTrees.skillUnlocked(data, id());
    }

    @Override
    public boolean canBeCalledOff(JojohaPlayerData data) {
        return data.timeStopHeldTicks > 0;
    }

    @Override
    public void callOff(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        TimeStopSystem.release(player.getUUID());
        TimeStopSettle.end(player);
        data.timeStopHeldTicks = 0;
        TimeStopLine.silence(player, data);

        if (stand != null) {
            stand.stopTimeStopPose();
        }

        player.serverLevel().playSound(null, player.blockPosition(),
                StandVoices.timeResume(
                        StandTypes.byId(data.stand.standId())), SoundSource.PLAYERS, 1.1F, 1.0F);
        Speech.timeResumed(player);
    }

    @Override
    public boolean activate(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        return activate(player, data, stand, chargeMaxTicks(data));
    }

    @Override
    public boolean activate(ServerPlayer player, JojohaPlayerData data, StandEntity stand,
                            int chargeTicks) {
        return cast(player, data, chargeTicks, stopTicks(data, 1F));
    }

    public static boolean cast(ServerPlayer player, JojohaPlayerData data, int chargeTicks, int stopTicks) {
        boolean full = charged(data, chargeTicks);
        if (!full && !tapped(chargeTicks)) {
            return false;
        }
        data.timeStopCasts++;
        data.timeStopUsesThisFight++;

        if (full) {
            TimeStopCast.begin(player, stopTicks);
        } else {
            TimeStopCast.tap(player, Math.min(TAP_STOP_TICKS, stopTicks));
        }
        return true;
    }

    public static int ceiling(JojohaPlayerData data) {
        return fullStopTicks();
    }
}
