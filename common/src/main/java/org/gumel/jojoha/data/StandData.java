package org.gumel.jojoha.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.stand.TrustTier;

import java.util.Optional;

/**
 * A player's Stand progress. Structure only for this pass — matches the PDF's Stand Stats
 * (POW/SPD/END/PRC/POT) and Trust Tiers (0 Dormant - 3 Bonded); obtainment, summoning and
 * ability logic are future work.
 *
 * @param skin which of its type's skins this Stand wears, as an index into
 *             {@code StandType.skins}. Held on the Stand rather than on the player because it
 *             belongs to the Stand: it survives being dismissed and re-summoned, and a player who
 *             somehow ended up with a different Stand would not be carrying this one's wardrobe
 *             across to it.
 */
public record StandData(
        ResourceLocation standId,
        int trustTier,
        int power,
        int speed,
        int endurance,
        int protection,
        int potential,
        int usageCount,
        int skin
) {
    /**
     * The multiplier this Stand's POWER puts on a blow.
     *
     * <p>1.2 at the starting five and 2.0 at the cap - see {@link StatEffects}, which owns the curve
     * along with every other. Everything that hits goes through here: the stat used to be priced by
     * hand at two call sites and absent from the rest, so Star Finger and the uppercut grew with it
     * while the punch, the barrage and the whole combo did not.
     */
    public float powerScale() {
        return StatEffects.powerScale(power);
    }

    /** Sentinel value for "no stand obtained yet". */
    public static final StandData NONE = new StandData((ResourceLocation) null, -1, 0, 0, 0, 0, 0, 0, 0);

    // Secondary constructor used only by the codec below, so RecordCodecBuilder's group
    // combinator never has to handle a bare null in the standId slot - see the comment there.
    public StandData(Optional<ResourceLocation> standId, int trustTier, int power, int speed,
                      int endurance, int protection, int potential, int usageCount, int skin) {
        this(standId.orElse(null), trustTier, power, speed, endurance, protection, potential,
                usageCount, skin);
    }

    /**
     * The eight-argument form every existing caller was written against, defaulting to the stock
     * look. Kept so that granting a Stand stays a statement about its stats.
     */
    public StandData(ResourceLocation standId, int trustTier, int power, int speed,
                      int endurance, int protection, int potential, int usageCount) {
        this(standId, trustTier, power, speed, endurance, protection, potential, usageCount, 0);
    }

    public static final Codec<StandData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            // Codec.optionalFieldOf(key, null) is broken for reference-typed defaults (NPEs
            // internally via Optional.of(null)) - but RecordCodecBuilder's group-combination
            // internals also choke on a bare Java null flowing through any field slot at all,
            // regardless of which codec produced it. So this stays Optional<ResourceLocation>
            // all the way to the (secondary) constructor above, which unwraps it outside the
            // codec's own value flow.
            ResourceLocation.CODEC.optionalFieldOf("stand_id").forGetter(d -> Optional.ofNullable(d.standId())),
            Codec.INT.fieldOf("trust_tier").forGetter(StandData::trustTier),
            Codec.INT.fieldOf("power").forGetter(StandData::power),
            Codec.INT.fieldOf("speed").forGetter(StandData::speed),
            Codec.INT.fieldOf("endurance").forGetter(StandData::endurance),
            Codec.INT.fieldOf("protection").forGetter(StandData::protection),
            Codec.INT.fieldOf("potential").forGetter(StandData::potential),
            Codec.INT.fieldOf("usage_count").forGetter(StandData::usageCount),
            // Optional with a default, unlike its neighbours: every Stand saved before skins
            // existed has no such field, and a required one would fail to parse them all rather
            // than reading them as wearing the default look, which is exactly what they are.
            Codec.INT.optionalFieldOf("skin", 0).forGetter(StandData::skin)
    ).apply(instance, StandData::new));

    public boolean isPresent() {
        return standId != null;
    }

    /** {@link #trustTier()} as the behaviour-carrying enum - see {@link TrustTier} for what each gates. */
    public TrustTier trust() {
        return TrustTier.fromLevel(trustTier);
    }

    /** Returns a copy at the given tier, leaving everything else untouched. */
    public StandData withTrust(TrustTier tier) {
        return new StandData(standId, tier.level(), power, speed, endurance, protection, potential,
                usageCount, skin);
    }

    /** Returns a copy wearing a different skin, leaving everything else untouched. */
    /**
     * The same Stand with one of its five raised by one.
     *
     * <p>A record cannot be edited, so every one of them is restated. That is tedious and it is the
     * right shape: a Stand's stats are one value together, and rebuilding the whole thing means
     * there is no path that changes power while leaving something else stale.
     *
     * @param stat which of the five, in the order the interface lists them
     * @return the raised Stand, or this one unchanged if the index means nothing
     */
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

    /** The five as the interface lists them, for anything that wants to show them in a row. */
    public int[] stats() {
        return new int[]{power, speed, endurance, protection, potential};
    }

    public StandData withSkin(int newSkin) {
        return new StandData(standId, trustTier, power, speed, endurance, protection, potential,
                usageCount, newSkin);
    }
}
