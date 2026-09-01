package org.gumel.jojoha.stand.skill;

import dev.architectury.event.events.common.TickEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.registry.ModSounds;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.StandSummonHandler;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The wind-up before time stops.
 *
 * <p>Stopping time used to be instantaneous, which made the single most powerful thing in the game
 * cost nothing but a keypress and gave everyone else no idea it was happening until it already had.
 * A cast that takes a couple of seconds and can be knocked out of you turns it into the moment it
 * ought to be: a gold shell rises around the caster while it charges, so anyone watching can see it
 * coming and has a window to stop it. Nothing folds until the freeze actually lands.
 *
 * <p>Interrupting refunds nothing. The energy is spent when the cast begins, so being hit out of it
 * is a genuine loss rather than a free attempt - otherwise there would be no reason not to spam it
 * and see whether it lands.
 */
public final class TimeStopCast {
    /**
     * How long the wind-up runs.
     *
     * <p>Deliberately short. This is a Stand throwing its hand out, not a spell being channelled -
     * at over two seconds it read as charging up, and the whole moment sagged. Just over a second
     * still leaves a window to be hit out of it, which is the only thing the wind-up has to buy.
     */
    /**
     * The wind-up, in ticks.
     *
     * <p>Landed on the Stand's throw rather than at the end of its animation. Read out of the
     * animation itself rather than guessed: the time-stop pose is completely still until tick 18 and
     * then does all of its movement between 19 and 27.5, so the throw is the back third and anything
     * that fires after it has missed the moment it belongs to. At 48 the Stand finished posing and
     * stood there for a second first; at 26 it landed on the last frames of the follow-through.
     * Twenty puts it on the front of the throw, with the sphere opening through the rest of it.
     */
    public static final int CAST_TICKS = 20;

    private static final List<Cast> ACTIVE = new ArrayList<>();

    private TimeStopCast() {
    }

    public static void init() {
        TickEvent.SERVER_POST.register(server -> tick());
    }

    /** Begins the wind-up. The freeze itself happens when it completes. */
    public static void begin(ServerPlayer player, int stopDurationTicks) {
        // Re-casting replaces rather than stacking, so a second press cannot run two winds-up.
        ACTIVE.removeIf(cast -> cast.player == player);
        ACTIVE.add(new Cast(player, stopDurationTicks));

        JojohaPlayerData data = PlayerDataAccess.get(player);
        data.timeStopCastTicks = CAST_TICKS;
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);

        StandEntity stand = StandSummonHandler.findStand(player, data);
        if (stand != null) {
            stand.triggerTimeStopPose();
        }

    }

    /**
     * Knocks a player out of their wind-up.
     *
     * <p>Called from the damage hook. Silent when nothing is being cast, since almost every call
     * will be an ordinary hit on somebody who is not casting anything.
     */
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

        // Pitched down and quiet - the same cue collapsing rather than completing.
        player.serverLevel().playSound(null, player.blockPosition(), ModSounds.TIME_RESUME.get(),
                SoundSource.PLAYERS, 0.7F, 0.6F);
        player.displayClientMessage(
                Component.translatable("message.jojoha.skill.time_stop_broken").withStyle(ChatFormatting.RED), true);
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
        private int ticksLeft = CAST_TICKS;

        private Cast(ServerPlayer player, int stopDurationTicks) {
            this.player = player;
            this.stopDurationTicks = stopDurationTicks;
        }

        /** @return true when this cast is finished with, whether it landed or not. */
        private boolean step() {
            if (!player.isAlive()) {
                return true;
            }

            JojohaPlayerData data = PlayerDataAccess.get(player);
            data.timeStopCastTicks = --ticksLeft;

            if (ticksLeft > 0) {
                PlayerDataAccess.set(player, data);
                // Synced every tick for the length of the wind-up. The counter was being written
                // server-side but only pushed to the client every twenty ticks, so over a 45-tick
                // cast the bubble was handed about three values and grew in visible jumps. Forty-odd
                // packets, once per cast, is a cheap price for a smooth swell.
                PlayerDataAccess.sync(player);
                return false;
            }

            // Landed. The freeze starts here, not at the keypress - and so does the sound, which
            // used to fire on the keypress and so announced a stop that had not happened for another
            // second and a half. It belongs on the instant the world actually changes.
            player.serverLevel().playSound(null, player.blockPosition(), ModSounds.SP_TIMESTOP.get(),
                    SoundSource.PLAYERS, 1.2F, 1.0F);

            TimeStopSystem.begin(player, stopDurationTicks);
            data.timeStopHeldTicks = stopDurationTicks;
            PlayerDataAccess.set(player, data);
            PlayerDataAccess.sync(player);

            player.displayClientMessage(
                    Component.translatable("message.jojoha.skill.time_stopped").withStyle(ChatFormatting.AQUA), true);
            return true;
        }
    }
}
