package org.gumel.jojoha.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.gumel.jojoha.stand.BuildMode;
import org.gumel.jojoha.stand.StandMode;

import java.util.List;

public record StandSession(boolean summoned, int modeOrdinal, boolean piloting, int timeStopExposures,
                           int timeStopFrozenTicks, int timeStopHeldTicks, int timeStopCastTicks,
                           int timeStopCasts, int buildModeOrdinal, int timeStopUsesThisFight,
                           int killProgress, List<String> equipped,
                           List<String> utilityEquipped, int standPoints,
                           StatProgress statProgress, BarState barState) {
    public static final StandSession DEFAULT =
            new StandSession(false, StandMode.ANALOG.ordinal(), false, 0, 0, 0, 0, 0,
                    BuildMode.SINGLE.ordinal(), 0, 0,
                    List.of(), List.of(), 0, StatProgress.DEFAULT,
                    BarState.DEFAULT);

    public static final Codec<StandSession> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("summoned", false).forGetter(StandSession::summoned),
            Codec.INT.optionalFieldOf("mode", StandMode.ANALOG.ordinal()).forGetter(StandSession::modeOrdinal),
            Codec.BOOL.optionalFieldOf("piloting", false).forGetter(StandSession::piloting),
            Codec.INT.optionalFieldOf("time_stop_exposures", 0).forGetter(StandSession::timeStopExposures),
            Codec.INT.optionalFieldOf("time_stop_frozen", 0).forGetter(StandSession::timeStopFrozenTicks),
            Codec.INT.optionalFieldOf("time_stop_held", 0).forGetter(StandSession::timeStopHeldTicks),
            Codec.INT.optionalFieldOf("time_stop_cast", 0).forGetter(StandSession::timeStopCastTicks),
            Codec.INT.optionalFieldOf("time_stop_casts", 0).forGetter(StandSession::timeStopCasts),
            Codec.INT.optionalFieldOf("build_mode", BuildMode.SINGLE.ordinal())
                    .forGetter(StandSession::buildModeOrdinal),
            Codec.INT.optionalFieldOf("time_stop_uses_this_fight", 0)
                    .forGetter(StandSession::timeStopUsesThisFight),
            Codec.INT.optionalFieldOf("kill_progress", 0).forGetter(StandSession::killProgress),
            Codec.STRING.listOf().optionalFieldOf("equipped", List.of())
                    .forGetter(StandSession::equipped),
            Codec.STRING.listOf().optionalFieldOf("utility_equipped", List.of())
                    .forGetter(StandSession::utilityEquipped),
            Codec.INT.optionalFieldOf("stand_points", 0).forGetter(StandSession::standPoints),
            StatProgress.CODEC.optionalFieldOf("stat_progress", StatProgress.DEFAULT)
                    .forGetter(StandSession::statProgress),
            BarState.CODEC.optionalFieldOf("bar_state", BarState.DEFAULT)
                    .forGetter(StandSession::barState)
    ).apply(instance, StandSession::new));
}
