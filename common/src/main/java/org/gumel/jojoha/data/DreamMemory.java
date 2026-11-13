package org.gumel.jojoha.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record DreamMemory(int visits, long lastDay, String lastWish, String lastOutcome,
                          String lastOffering, String lastStand, int lore) {
    public static final String GRANTED = "granted";
    public static final String WOKE = "woke";
    public static final String IGNORED = "ignored";

    public static final DreamMemory NONE = new DreamMemory(0, -1L, "", "", "", "", 0);

    public static final Codec<DreamMemory> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("visits", 0).forGetter(DreamMemory::visits),
            Codec.LONG.optionalFieldOf("last_day", -1L).forGetter(DreamMemory::lastDay),
            Codec.STRING.optionalFieldOf("last_wish", "").forGetter(DreamMemory::lastWish),
            Codec.STRING.optionalFieldOf("last_outcome", "").forGetter(DreamMemory::lastOutcome),
            Codec.STRING.optionalFieldOf("last_offering", "").forGetter(DreamMemory::lastOffering),
            Codec.STRING.optionalFieldOf("last_stand", "").forGetter(DreamMemory::lastStand),
            Codec.INT.optionalFieldOf("lore", 0).forGetter(DreamMemory::lore)
    ).apply(instance, DreamMemory::new));

    public boolean stranger() {
        return visits <= 0;
    }

    public DreamMemory arrived(long day, String stand) {
        return new DreamMemory(visits + 1, day, lastWish, lastOutcome, lastOffering, stand, lore);
    }

    public DreamMemory ended(String outcome, String wish, String offering, String stand) {
        return new DreamMemory(visits, lastDay, wish.isEmpty() ? lastWish : wish, outcome,
                offering.isEmpty() ? lastOffering : offering, stand, lore);
    }

    public DreamMemory toldMore() {
        return new DreamMemory(visits, lastDay, lastWish, lastOutcome, lastOffering, lastStand, lore + 1);
    }
}
