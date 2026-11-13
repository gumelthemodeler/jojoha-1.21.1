package org.gumel.jojoha.level;

import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;

public final class SandPatches {
    public static final double REGION_SCALE = 360.0;
    public static final double METEOR_SCALE = 420.0;
    public static final double OASIS_SCALE = 56.0;
    public static final double DUNE_BELOW = 0.36;
    public static final double METEOR_ABOVE = 0.84;
    public static final double OASIS_ABOVE = 0.80;

    private static final double DUNE_FADE = 0.07;
    private static final double EDGE_FADE = 96.0;

    private static final long REGION_SALT = 0xBB67AE8584CAA73BL;
    private static final long METEOR_SALT = 0xA54FF53A5F1D36F1L;
    private static final long OASIS_SALT = 0x510E527FL;
    private static final long WIND_SALT = 0x1F83D9ABFB41BD6BL;

    private SandPatches() {
    }

    public static ResourceKey<Biome> at(int blockX, int blockZ, long salt) {
        if (MoorPatches.layered(blockX, blockZ, METEOR_SCALE, salt ^ METEOR_SALT) > METEOR_ABOVE) {
            return ModBiomes.METEOR_FIELD;
        }
        double region = MoorPatches.layered(blockX, blockZ, REGION_SCALE, salt ^ REGION_SALT);
        if (region < DUNE_BELOW) {
            return ModBiomes.DUNE_SEA;
        }
        if (MoorPatches.layered(blockX, blockZ, OASIS_SCALE, salt ^ OASIS_SALT) > OASIS_ABOVE) {
            return ModBiomes.OASIS;
        }
        return ModBiomes.STARDUST_SANDS;
    }

    public static double wind(long salt) {
        return Continents.unit(Continents.mix(salt ^ WIND_SALT)) * Mth.TWO_PI;
    }

    public static double dunes(int blockX, int blockZ, long salt) {
        double inside = Continents.inset(blockX, blockZ, salt, Continents.Continent.STARDUST_SANDS);
        if (inside <= 0.0) {
            return 0.0;
        }
        double region = MoorPatches.layered(blockX, blockZ, REGION_SCALE, salt ^ REGION_SALT);
        double meteor = MoorPatches.layered(blockX, blockZ, METEOR_SCALE, salt ^ METEOR_SALT);
        return Mth.clamp((DUNE_BELOW - region) / DUNE_FADE, 0.0, 1.0)
                * Mth.clamp((METEOR_ABOVE - meteor) / DUNE_FADE, 0.0, 1.0)
                * Mth.clamp(inside / EDGE_FADE, 0.0, 1.0);
    }
}
