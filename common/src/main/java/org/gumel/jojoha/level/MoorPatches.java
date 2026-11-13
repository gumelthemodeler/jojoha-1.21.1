package org.gumel.jojoha.level;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

public final class MoorPatches {
    public static final double REGION_SCALE = 320.0;
    public static final double FIELD_SCALE = 240.0;
    public static final double GLADE_SCALE = 72.0;
    public static final double BOG_BELOW = 0.34;
    public static final double HOLLOW_ABOVE = 0.66;
    public static final double FIELD_ABOVE = 0.70;
    public static final double GLADE_ABOVE = 0.66;

    private static final long REGION_SALT = 0x2545F4914F6CDD1DL;
    private static final long FIELD_SALT = 0x77L;
    private static final long GLADE_SALT = 0x99L;
    private static final long DETAIL_SALT = 0x5BD1E995L;
    private static final double DETAIL_DIVIDE = 2.3;
    private static final double BASE_SHARE = 0.7;

    private MoorPatches() {
    }

    public static ResourceKey<Biome> at(int blockX, int blockZ, long salt) {
        double region = layered(blockX, blockZ, REGION_SCALE, salt ^ REGION_SALT);
        if (region < BOG_BELOW) {
            return ModBiomes.PHANTOM_BOG;
        }
        if (region > HOLLOW_ABOVE) {
            return ModBiomes.MISTY_HOLLOW;
        }
        if (layered(blockX, blockZ, FIELD_SCALE, salt ^ FIELD_SALT) > FIELD_ABOVE
                || layered(blockX, blockZ, GLADE_SCALE, salt ^ GLADE_SALT) > GLADE_ABOVE) {
            return ModBiomes.MOOR_CLEARING;
        }
        return ModBiomes.PHANTOM_MOORS;
    }

    public static double layered(int x, int z, double scale, long salt) {
        return noise(x, z, scale, salt) * BASE_SHARE
                + noise(x, z, scale / DETAIL_DIVIDE, salt ^ DETAIL_SALT) * (1.0 - BASE_SHARE);
    }

    private static double noise(int x, int z, double scale, long salt) {
        double fx = x / scale;
        double fz = z / scale;
        int ix = (int) Math.floor(fx);
        int iz = (int) Math.floor(fz);
        double tx = smooth(fx - ix);
        double tz = smooth(fz - iz);
        double top = lerp(tx, lattice(ix, iz, salt), lattice(ix + 1, iz, salt));
        double bottom = lerp(tx, lattice(ix, iz + 1, salt), lattice(ix + 1, iz + 1, salt));
        return lerp(tz, top, bottom);
    }

    private static double lattice(int ix, int iz, long salt) {
        return Continents.unit(Continents.mix(salt ^ (ix * 0x632BE59BD9B4E019L) ^ (iz * 0x9E3779B97F4A7C15L)));
    }

    private static double smooth(double t) {
        return t * t * (3.0 - 2.0 * t);
    }

    private static double lerp(double t, double a, double b) {
        return a + (b - a) * t;
    }
}
