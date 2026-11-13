package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.network.packet.HeavenFxPacket;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.heaven.Ascension;
import org.gumel.jojoha.stand.heaven.Heaven;
import org.gumel.jojoha.stand.heaven.HeavenShow;
import org.gumel.jojoha.stand.heaven.HeavenTimeStop;
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.StandBeat;
import org.gumel.jojoha.stand.skill.StandSkill;
import org.gumel.jojoha.stand.skill.TimeStopCast;
import org.gumel.jojoha.stand.skill.TimeStopSystem;

public final class HeavenTimeStopSkill implements StandSkill {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "heaven_time_stop");
    public static final HeavenTimeStopSkill INSTANCE = new HeavenTimeStopSkill();

    public static final int STOP_SECONDS = 15;
    private static final int COOLDOWN_TICKS = 1200;
    private static final float ENERGY_COST = EnergyWeight.ULTIMATE.cost();

    private HeavenTimeStopSkill() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "skill.jojoha.heaven_time_stop";
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
        return TimeStopSkill.INSTANCE.isRunning(data);
    }

    @Override
    public boolean canBeCalledOff(JojohaPlayerData data) {
        return TimeStopSkill.INSTANCE.canBeCalledOff(data);
    }

    @Override
    public void callOff(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        TimeStopSkill.INSTANCE.callOff(player, data, stand);
        HeavenTimeStop.release(player.getUUID());
    }

    @Override
    public boolean activate(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        int ticks = Math.max(20, STOP_SECONDS * 20);
        data.timeStopCasts++;
        Heaven.telegraph(player);
        Heaven.announce(player, "announce.jojoha.heaven_time_stop");
        int castTicks = TimeStopCast.speakThenBegin(player, ticks);
        HeavenTimeStop.begin(player, castTicks, ticks);
        StandBeat.at(castTicks, player, () ->
                HeavenShow.at(player.serverLevel(),
                        HeavenFxPacket.GOLD_STOP, player.position(),
                        (float) TimeStopSystem.RADIUS, ticks));
        Ascension.add(player, Ascension.PER_TIME_STOP);
        return true;
    }
}
