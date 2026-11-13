package org.gumel.jojoha.stand.ripple;

import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.stand.StandType;
import org.gumel.jojoha.stand.StandTypes;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public final class RippleProfiles {

    private RippleProfiles() {
    }

    public static final RippleProfile HIEROPHANT_GREEN = new RippleProfile(
            0.5F, 3.3F, 0.8F,
            2.0F, 1.0F,
            2.4F, 0.45F, 0.35F,
            "_green_overlay");

    private static final Map<ResourceLocation, RippleProfile> PROFILES = Map.of(
            StandTypes.HIEROPHANT_GREEN_ID, HIEROPHANT_GREEN);

    @Nullable
    public static RippleProfile of(@Nullable StandType type) {
        return type == null ? null : of(type.id());
    }

    @Nullable
    public static RippleProfile of(@Nullable ResourceLocation standId) {
        RippleProfile profile = standId == null ? null : PROFILES.get(standId);
        return profile == null ? null : profile.tuned(standId);
    }

    public static boolean has(@Nullable ResourceLocation standId) {
        return standId != null && PROFILES.containsKey(standId);
    }
}
