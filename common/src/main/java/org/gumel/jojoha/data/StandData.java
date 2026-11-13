package org.gumel.jojoha.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.stand.TrustTier;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public record StandData(
        ResourceLocation standId,
        int trustTier,
        int power,
        int speed,
        int endurance,
        int protection,
        int potential,
        int usageCount,
        int skin,

        Set<ResourceLocation> equippedPassives
) {
    public float powerScale() {
        return StatEffects.powerScale(power);
    }

    public static final StandData NONE = new StandData((ResourceLocation) null, -1, 0, 0, 0, 0, 0, 0, 0);

    public StandData(ResourceLocation standId, int trustTier, int power, int speed,
                     int endurance, int protection, int potential, int usageCount, int skin) {
        this(standId, trustTier, power, speed, endurance, protection, potential, usageCount, skin,
                Set.of());
    }

    public StandData(Optional<ResourceLocation> standId, int trustTier, int power, int speed,
                      int endurance, int protection, int potential, int usageCount, int skin,
                      Set<ResourceLocation> equippedPassives) {
        this(standId.orElse(null), trustTier, power, speed, endurance, protection, potential,
                usageCount, skin, equippedPassives);
    }

    public StandData(ResourceLocation standId, int trustTier, int power, int speed,
                      int endurance, int protection, int potential, int usageCount) {
        this(standId, trustTier, power, speed, endurance, protection, potential, usageCount, 0);
    }

    public static final Codec<StandData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.optionalFieldOf("stand_id").forGetter(d -> Optional.ofNullable(d.standId())),
            Codec.INT.fieldOf("trust_tier").forGetter(StandData::trustTier),
            Codec.INT.fieldOf("power").forGetter(StandData::power),
            Codec.INT.fieldOf("speed").forGetter(StandData::speed),
            Codec.INT.fieldOf("endurance").forGetter(StandData::endurance),
            Codec.INT.fieldOf("protection").forGetter(StandData::protection),
            Codec.INT.fieldOf("potential").forGetter(StandData::potential),
            Codec.INT.fieldOf("usage_count").forGetter(StandData::usageCount),
            Codec.INT.optionalFieldOf("skin", 0).forGetter(StandData::skin),
            ResourceLocation.CODEC.listOf()
                    .xmap(list -> Collections.unmodifiableSet(new LinkedHashSet<>(list)),
                            List::copyOf)
                    .optionalFieldOf("equipped_passives", Set.of())
                    .forGetter(StandData::equippedPassives)
    ).apply(instance, StandData::new));

    public StandData withEndurance(int value) {
        return new StandData(standId, trustTier, power, speed, value, protection, potential,
                usageCount, skin, equippedPassives);
    }

    public StandData withPassives(Set<ResourceLocation> passives) {
        return new StandData(standId, trustTier, power, speed, endurance, protection, potential,
                usageCount, skin, passives);
    }

    public boolean isPresent() {
        return standId != null;
    }

    public TrustTier trust() {
        return TrustTier.fromLevel(trustTier);
    }

    public StandData withTrust(TrustTier tier) {
        return new StandData(standId, tier.level(), power, speed, endurance, protection, potential,
                usageCount, skin);
    }

    public StandData plusStat(int stat) {
        return switch (stat) {
            case 0 -> new StandData(standId, trustTier, power + 1, speed, endurance, protection,
                    potential, usageCount, skin);
            case 1 -> new StandData(standId, trustTier, power, speed + 1, endurance, protection,
                    potential, usageCount, skin);
            case 2 -> new StandData(standId, trustTier, power, speed, endurance + 1, protection,
                    potential, usageCount, skin);
            case 3 -> new StandData(standId, trustTier, power, speed, endurance, protection + 1,
                    potential, usageCount, skin);
            case 4 -> new StandData(standId, trustTier, power, speed, endurance, protection,
                    potential + 1, usageCount, skin);
            default -> this;
        };
    }

    public int[] stats() {
        return new int[]{power, speed, endurance, protection, potential};
    }

    public StandData withSkin(int newSkin) {
        return new StandData(standId, trustTier, power, speed, endurance, protection, potential,
                usageCount, newSkin);
    }
}
