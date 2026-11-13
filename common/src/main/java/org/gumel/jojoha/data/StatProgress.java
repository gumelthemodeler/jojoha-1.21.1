package org.gumel.jojoha.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.List;

public record StatProgress(List<Integer> player, List<Integer> stand, List<String> deeds) {

    public static final StatProgress DEFAULT =
            new StatProgress(zeroes(), zeroes(), List.of());

    public static final Codec<StatProgress> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.listOf().optionalFieldOf("player", zeroes()).forGetter(StatProgress::player),
            Codec.INT.listOf().optionalFieldOf("stand", zeroes()).forGetter(StatProgress::stand),
            Codec.STRING.listOf().optionalFieldOf("deeds", List.of()).forGetter(StatProgress::deeds)
    ).apply(instance, StatProgress::new));

    private static List<Integer> zeroes() {
        List<Integer> list = new ArrayList<>(StatPoints.COUNT);
        for (int i = 0; i < StatPoints.COUNT; i++) {
            list.add(0);
        }
        return list;
    }

    public static int[] toArray(List<Integer> stored) {
        int[] out = new int[StatPoints.COUNT];
        for (int i = 0; i < StatPoints.COUNT && i < stored.size(); i++) {
            Integer value = stored.get(i);
            out[i] = value == null ? 0 : value;
        }
        return out;
    }

    public static List<Integer> fromArray(int[] values) {
        List<Integer> list = new ArrayList<>(values.length);
        for (int value : values) {
            list.add(value);
        }
        return list;
    }
}
