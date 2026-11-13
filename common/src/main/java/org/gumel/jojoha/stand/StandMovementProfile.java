package org.gumel.jojoha.stand;

import net.minecraft.resources.ResourceLocation;

import java.util.Map;

public record StandMovementProfile(double followDistance, double sideOffset, double verticalOffset,
                                   double springStiffness, double springDamping,
                                   float rotationSpeed, float combatRotationSpeed,
                                   double crouchDrop, double sprintLean, double fallCatchUp,
                                   double attackForward, double attackSide, double barragePush) {
    public static final StandMovementProfile SHARP = new StandMovementProfile(
            1.0, 0.9, 0.6, 0.15, 0.56, 22F, 30F, 0.35, 0.35, 0.55,
            2.15, 1.1, 0.9);

    public static final StandMovementProfile STEADY = new StandMovementProfile(
            1.0, 0.9, 0.6, 0.08, 0.43, 14F, 20F, 0.35, 0.25, 0.6,
            2.15, 1.0, 0.8);

    public static final StandMovementProfile FLOWING = new StandMovementProfile(
            1.15, 1.0, 0.6, 0.05, 0.35, 9F, 14F, 0.3, 0.15, 0.7,
            2.0, 1.35, 0.7);

    private static final Map<ResourceLocation, StandMovementProfile> BY_STAND = Map.of(
            StandTypes.STAR_PLATINUM_ID, SHARP,
            StandTypes.HERMIT_PURPLE_ID, FLOWING);

    public static StandMovementProfile forStand(StandType type) {
        if (type == null) {
            return STEADY;
        }
        StandMovementProfile named = BY_STAND.get(type.id());
        if (named != null) {
            return named;
        }
        return type.range() == StandRange.CLOSE ? SHARP : STEADY;
    }
}
