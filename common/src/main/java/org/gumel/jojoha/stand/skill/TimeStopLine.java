package org.gumel.jojoha.stand.skill;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.registry.ModSounds;
import org.gumel.jojoha.stand.StandTypes;
import org.gumel.jojoha.stand.StandVoices;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Supplier;

public final class TimeStopLine {
    public static final int CHARGE_TICKS = 70;

    public static final float CHARGE_CYCLE_TICKS = 0.8167F * 20F;

    private static final double HEARD_RANGE = 48.0;

    private record Timing(int leadTicks, @Nullable Supplier<SoundEvent> impact, int quietTicks) {
    }

    private static final Timing IMPACT_ONLY = new Timing(0, null, 22);

    private static final Map<ResourceLocation, Timing> TIMINGS = Map.of(
            ModSounds.SP_TIMESTOP.getId(), new Timing(0, ModSounds.TIME_STOP_CAST, 22),
            ModSounds.TW_TIME_STOP_CALL.getId(), new Timing(0, ModSounds.TW_TIME_STOP_IMPACT, 24),
            ModSounds.SECRET_TW_TIME_STOP.getId(), new Timing(0, ModSounds.TIME_STOP_CAST, 145),
            ModSounds.TW_SHADOW_TIME_STOP.getId(), new Timing(28, ModSounds.TIME_STOP_CAST, 22));

    private TimeStopLine() {
    }

    public static SoundEvent sound(JojohaPlayerData data) {
        return StandVoices.timeStopCast(StandTypes.byId(data.stand.standId()), StandVoices.shadowed(data.stand));
    }

    private static Timing timing(JojohaPlayerData data) {
        return TIMINGS.getOrDefault(BuiltInRegistries.SOUND_EVENT.getKey(sound(data)), IMPACT_ONLY);
    }

    public static int leadTicks(JojohaPlayerData data) {
        return timing(data).leadTicks();
    }

    public static int chargeTicks(JojohaPlayerData data) {
        return Math.max(CHARGE_TICKS, leadTicks(data));
    }

    public static int quietTicks(JojohaPlayerData data) {
        return timing(data).quietTicks();
    }

    public static boolean spoken(JojohaPlayerData data) {
        return leadTicks(data) > 0;
    }

    public static void speak(ServerPlayer player, JojohaPlayerData data) {
        player.serverLevel().playSound(null, player.blockPosition(), sound(data), SoundSource.PLAYERS, 1.2F, 1.0F);
    }

    public static void impact(ServerPlayer player, JojohaPlayerData data) {
        player.serverLevel().playSound(null, player.blockPosition(), ModSounds.TIMESTOP_BEGINNING.get(), SoundSource.PLAYERS, 1.2F, 1.0F);
        Supplier<SoundEvent> impact = timing(data).impact();
        if (impact != null) {
            player.serverLevel().playSound(null, player.blockPosition(), impact.get(), SoundSource.PLAYERS, 1.3F, 1.0F);
        }
        if (!spoken(data)) {
            speak(player, data);
        }
    }

    public static void silence(ServerPlayer player, JojohaPlayerData data) {
        ClientboundStopSoundPacket stop = new ClientboundStopSoundPacket(
                BuiltInRegistries.SOUND_EVENT.getKey(sound(data)), SoundSource.PLAYERS);
        double reach = HEARD_RANGE * HEARD_RANGE;
        for (ServerPlayer listener : player.serverLevel().players()) {
            if (listener.distanceToSqr(player) <= reach) {
                listener.connection.send(stop);
            }
        }
    }
}
