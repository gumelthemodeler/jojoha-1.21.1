package org.gumel.jojoha.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public record WorkshopLook(Optional<ResourceLocation> stand, int skin, StandPlacement placement, int rest, int shout,
                           int summon, String idle) {
    public static final WorkshopLook EMPTY =
            new WorkshopLook(Optional.empty(), 0, StandPlacement.NONE, 0, 0, 0, "");

    public static final Codec<WorkshopLook> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.optionalFieldOf("stand").forGetter(WorkshopLook::stand),
            Codec.INT.optionalFieldOf("skin", 0).forGetter(WorkshopLook::skin),
            StandPlacement.CODEC.optionalFieldOf("placement", StandPlacement.NONE).forGetter(WorkshopLook::placement),
            Codec.INT.optionalFieldOf("rest", 0).forGetter(WorkshopLook::rest),
            Codec.INT.optionalFieldOf("shout", 0).forGetter(WorkshopLook::shout),
            Codec.INT.optionalFieldOf("summon", 0).forGetter(WorkshopLook::summon),
            Codec.STRING.optionalFieldOf("idle", "").forGetter(WorkshopLook::idle)
    ).apply(instance, WorkshopLook::new));

    public boolean saved() {
        return stand.isPresent();
    }
}
