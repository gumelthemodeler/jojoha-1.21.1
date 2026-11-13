package org.gumel.jojoha.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public record WorkshopData(int summon, int shout, int rest, List<WorkshopLook> looks, List<String> log,
                           String idle, List<StandPose> poses) {
    public static final int IDLE_KEY_CHARS = 256;
    public static final int LOOK_SLOTS = 3;

    public static final WorkshopData NONE = new WorkshopData(0, 0, 0, List.of(), List.of(), "", List.of());

    public static final Codec<WorkshopData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("summon", 0).forGetter(WorkshopData::summon),
            Codec.INT.optionalFieldOf("shout", 0).forGetter(WorkshopData::shout),
            Codec.INT.optionalFieldOf("rest", 0).forGetter(WorkshopData::rest),
            WorkshopLook.CODEC.listOf().optionalFieldOf("looks", List.of()).forGetter(WorkshopData::looks),
            Codec.STRING.listOf().optionalFieldOf("log", List.of()).forGetter(WorkshopData::log),
            Codec.STRING.optionalFieldOf("idle", "").forGetter(WorkshopData::idle),
            StandPose.CODEC.listOf().optionalFieldOf("poses", List.of()).forGetter(WorkshopData::poses)
    ).apply(instance, WorkshopData::new));
}
