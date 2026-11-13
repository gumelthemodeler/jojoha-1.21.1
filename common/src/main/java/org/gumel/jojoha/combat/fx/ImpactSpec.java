package org.gumel.jojoha.combat.fx;

public record ImpactSpec(int sparks, float spread, float ring, int debris, int rings, float largeBias,
                         float shake, float frame, float pitch, float volume, ImpactTier tier) {
    public static final ImpactSpec LIGHT =
            new ImpactSpec(4, 22F, 0.9F, 2, 2, 0.45F, 0.06F, 0F, 1.35F, 0.55F, ImpactTier.TAP);

    public static final ImpactSpec FLURRY =
            new ImpactSpec(5, 24F, 1.0F, 2, 3, 0.55F, 0.03F, 0F, 1.4F, 0.5F, ImpactTier.TAP);

    public static final ImpactSpec MEDIUM =
            new ImpactSpec(8, 30F, 1.2F, 4, 2, 0.55F, 0.14F, 0F, 1.1F, 0.75F, ImpactTier.SOLID);

    public static final ImpactSpec THUD =
            new ImpactSpec(10, 32F, 1.35F, 5, 2, 0.65F, 0.2F, 0F, 0.85F, 0.85F, ImpactTier.THUD);

    public static final ImpactSpec HEAVY =
            new ImpactSpec(16, 38F, 1.7F, 8, 3, 0.75F, 0.32F, 0F, 0.85F, 0.95F, ImpactTier.HEAVY);

    public static final ImpactSpec FINISHER =
            new ImpactSpec(28, 46F, 2.4F, 14, 4, 0.9F, 0.6F, 0.8F, 0.7F, 1.0F, ImpactTier.FINISHER);

    public static final ImpactSpec GUARD_BREAK =
            new ImpactSpec(10, 70F, 2.0F, 6, 3, 0.7F, 0.22F, 0F, 0.6F, 0.8F, ImpactTier.GUARD_BREAK);

    public static final ImpactSpec PARRY =
            new ImpactSpec(12, 60F, 1.6F, 4, 3, 0.7F, 0.25F, 0F, 1.3F, 0.9F, ImpactTier.PARRY);

    public static final ImpactSpec PERFECT_PARRY =
            new ImpactSpec(24, 70F, 2.3F, 6, 3, 0.8F, 0.45F, 0.6F, 1.0F, 1.0F, ImpactTier.PARRY);

    public static final ImpactSpec FIST_PARRY =
            new ImpactSpec(4, 60F, 0F, 0, 0, 0F, 0.12F, 0F, 1.3F, 0.9F, ImpactTier.PARRY);

    public static final ImpactSpec PERFECT_FIST_PARRY =
            new ImpactSpec(7, 60F, 0F, 0, 0, 0F, 0.45F, 0.6F, 1.0F, 1.0F, ImpactTier.PARRY);

    public static final ImpactSpec STRUGGLE =
            new ImpactSpec(6, 30F, 1.0F, 2, 1, 0.5F, 0F, 0F, 1.1F, 0.6F, ImpactTier.TAP);

    public static final ImpactSpec CLASH =
            new ImpactSpec(20, 90F, 2.2F, 8, 4, 0.85F, 0.4F, 0F, 0.8F, 1.0F, ImpactTier.PARRY);

    public static boolean perfectParry(ImpactSpec spec) {
        return spec == PERFECT_PARRY || spec == PERFECT_FIST_PARRY;
    }

    public static ImpactSpec parry(boolean weapon, boolean perfect) {
        if (weapon) {
            return perfect ? PERFECT_PARRY : PARRY;
        }
        return perfect ? PERFECT_FIST_PARRY : FIST_PARRY;
    }

    public ImpactSpec silent() {
        return new ImpactSpec(sparks, spread, ring, debris, rings, largeBias, shake, frame, pitch, 0F, tier);
    }

    public ImpactSpec scaledBy(float strength) {
        float s = Math.max(0F, Math.min(1F, strength));
        return new ImpactSpec(
                Math.max(1, Math.round(sparks * s)),
                spread,
                ring * (0.65F + 0.35F * s),
                Math.max(0, Math.round(debris * s)),
                Math.max(1, Math.round(rings * s)),
                largeBias * s,
                shake * s,
                frame * s,
                pitch,
                volume * (0.6F + 0.4F * s),
                tier);
    }
}
