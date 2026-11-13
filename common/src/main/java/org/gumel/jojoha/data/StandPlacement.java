package org.gumel.jojoha.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;

public record StandPlacement(float side, float height, float ahead, float turn) {
    public static final StandPlacement NONE = new StandPlacement(0F, 0F, 0F, 0F);

    public static final float TURN_LIMIT = 180F;

    public static final Codec<StandPlacement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.optionalFieldOf("side", 0F).forGetter(StandPlacement::side),
            Codec.FLOAT.optionalFieldOf("height", 0F).forGetter(StandPlacement::height),
            Codec.FLOAT.optionalFieldOf("ahead", 0F).forGetter(StandPlacement::ahead),
            Codec.FLOAT.optionalFieldOf("turn", 0F).forGetter(StandPlacement::turn)
    ).apply(instance, StandPlacement::new));

    public StandPlacement clamped(float range) {
        return new StandPlacement(clamp(side, range), clamp(height, range), clamp(ahead, range),
                Float.isFinite(turn) ? Mth.clamp(turn, -TURN_LIMIT, TURN_LIMIT) : 0F);
    }

    private static float clamp(float value, float range) {
        return Float.isFinite(value) ? Mth.clamp(value, -range, range) : 0F;
    }

    public boolean isNone() {
        return side == 0F && height == 0F && ahead == 0F && turn == 0F;
    }
}
