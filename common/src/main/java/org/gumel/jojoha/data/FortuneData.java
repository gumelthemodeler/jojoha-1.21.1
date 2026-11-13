package org.gumel.jojoha.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record FortuneData(String fortune, int phobia, float honor, int curses, int ghostTicks, long lastConflict,
                          int limbs, int limbRegrow, int tally, long until) {
    public static final FortuneData NONE = new FortuneData("", 0, 50F, 0, 0, 0L, 0, 0, 0, 0L);

    public static final Codec<FortuneData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("fortune", "").forGetter(FortuneData::fortune),
            Codec.INT.optionalFieldOf("phobia", 0).forGetter(FortuneData::phobia),
            Codec.FLOAT.optionalFieldOf("honor", 50F).forGetter(FortuneData::honor),
            Codec.INT.optionalFieldOf("curses", 0).forGetter(FortuneData::curses),
            Codec.INT.optionalFieldOf("ghost_ticks", 0).forGetter(FortuneData::ghostTicks),
            Codec.LONG.optionalFieldOf("last_conflict", 0L).forGetter(FortuneData::lastConflict),
            Codec.INT.optionalFieldOf("limbs", 0).forGetter(FortuneData::limbs),
            Codec.INT.optionalFieldOf("limb_regrow", 0).forGetter(FortuneData::limbRegrow),
            Codec.INT.optionalFieldOf("tally", 0).forGetter(FortuneData::tally),
            Codec.LONG.optionalFieldOf("until", 0L).forGetter(FortuneData::until)
    ).apply(instance, FortuneData::new));
}
