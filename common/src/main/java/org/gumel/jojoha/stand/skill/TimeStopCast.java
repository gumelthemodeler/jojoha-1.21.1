package org.gumel.jojoha.stand.skill;

import dev.architectury.event.events.common.TickEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.registry.ModSounds;
import org.gumel.jojoha.speech.Speech;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.StandSummonHandler;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class TimeStopCast {
    public static final int CAST_TICKS = 4;

    private static final List<Cast> ACTIVE = new ArrayList<>();

    private TimeStopCast() {
    }

    public static void init() {
        TickEvent.SERVER_POST.register(server -> tick());
    }

    public static void begin(ServerPlayer player, int stopDurationTicks) {
        ACTIVE.removeIf(cast -> cast.player == player);
        JojohaPlayerData data = PlayerDataAccess.get(player);
        freeze(player, data, stopDurationTicks, TimeStopSettle.introTicks(data));
    }

    public static void tap(ServerPlayer player, int stopDurationTicks) {
        ACTIVE.removeIf(cast -> cast.player == player);
        JojohaPlayerData data = PlayerDataAccess.get(player);
        StandEntity stand = StandSummonHandler.findStand(player, data);
        if (stand != null) {
            stand.triggerTimeStopPose();
        }
        freeze(player, data, stopDurationTicks, 0);
    }

    public static int speakThenBegin(ServerPlayer player, int stopDurationTicks) {
        ACTIVE.removeIf(cast -> cast.player == player);
        JojohaPlayerData data = PlayerDataAccess.get(player);
        int castTicks = Math.max(CAST_TICKS, TimeStopLine.leadTicks(data));
        StandEntity stand = StandSummonHandler.findStand(player, data);
        int poseAt = stand == null ? -1 : Math.max(0, castTicks - 1 - stand.chooseTimeStopPose());
        ACTIVE.add(new Cast(player, stopDurationTicks, castTicks, poseAt));
        NetworkHandler.broadcastTension(player);
        data.timeStopCastTicks = castTicks;
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
        if (TimeStopLine.spoken(data)) {
            TimeStopLine.speak(player, data);
        }
        return castTicks;
    }

    private static void freeze(ServerPlayer player, JojohaPlayerData data, int stopDurationTicks, int intro) {
        TimeStopCharging.struck(player, data);
        NetworkHandler.broadcastTension(player);
        TimeStopLine.impact(player, data);
        Speech.timeStopped(player);
        data.timeStopCastTicks = 0;
        int total = intro + stopDurationTicks;
        TimeStopSystem.begin(player, total, intro > 0 ? Math.max(intro, TimeStopLine.quietTicks(data)) : 0, intro);
        TimeStopSettle.begin(player, intro, total);
        data.timeStopHeldTicks = total;
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
    }

    public static void interrupt(ServerPlayer player) {
        if (!ACTIVE.removeIf(cast -> cast.player == player)) {
            return;
        }

        JojohaPlayerData data = PlayerDataAccess.get(player);
        data.timeStopCastTicks = 0;
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);

        StandEntity stand = StandSummonHandler.findStand(player, data);
        if (stand != null) {
            stand.stopTimeStopPose();
        }

        TimeStopLine.silence(player, data);
        player.serverLevel().playSound(null, player.blockPosition(), ModSounds.TIME_RESUME.get(),
                SoundSource.PLAYERS, 0.7F, 0.6F);
        Speech.timeStopBroken(player);
    }

    public static boolean isCasting(ServerPlayer player) {
        return ACTIVE.stream().anyMatch(cast -> cast.player == player);
    }

    private static void tick() {
        if (ACTIVE.isEmpty()) {
            return;
        }

        Iterator<Cast> iterator = ACTIVE.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().step()) {
                iterator.remove();
            }
        }
    }

    private static final class Cast {
        private final ServerPlayer player;
        private final int stopDurationTicks;
        private final int castTicks;
        private final int poseAt;
        private int ticksLeft;

        private Cast(ServerPlayer player, int stopDurationTicks, int castTicks, int poseAt) {
            this.player = player;
            this.stopDurationTicks = stopDurationTicks;
            this.castTicks = castTicks;
            this.ticksLeft = castTicks;
            this.poseAt = poseAt;
        }

        private boolean step() {
            if (!player.isAlive()) {
                return true;
            }

            JojohaPlayerData data = PlayerDataAccess.get(player);
            data.timeStopCastTicks = --ticksLeft;

            if (castTicks - ticksLeft - 1 == poseAt) {
                StandEntity stand = StandSummonHandler.findStand(player, data);
                if (stand != null) {
                    stand.triggerTimeStopPose();
                }
            }

            if (ticksLeft > 0) {
                PlayerDataAccess.set(player, data);
                PlayerDataAccess.sync(player);
                return false;
            }

            freeze(player, data, stopDurationTicks, TimeStopSettle.introTicks(data));
            return true;
        }
    }
}
