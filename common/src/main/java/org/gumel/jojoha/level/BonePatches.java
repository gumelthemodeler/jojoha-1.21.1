package org.gumel.jojoha.level;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

import java.util.Set;

public final class BonePatches {
    public static final double REGION_SCALE = 360.0;
    public static final double FLATS_SCALE = 260.0;
    public static final double ASH_BELOW = 0.33;
    public static final double CANYON_ABOVE = 0.67;
    public static final double FLATS_ABOVE = 0.68;

    private static final long REGION_SALT = 0x6A09E667F3BCC908L;
    private static final long FLATS_SALT = 0x3CL;

    private static final Set<ResourceKey<Biome>> MOUNTAINS = Set.of(
            Biomes.JAGGED_PEAKS, Biomes.FROZEN_PEAKS, Biomes.STONY_PEAKS, Biomes.SNOWY_SLOPES, Biomes.GROVE,
            Biomes.MEADOW, Biomes.CHERRY_GROVE, Biomes.WINDSWEPT_HILLS, Biomes.WINDSWEPT_GRAVELLY_HILLS,
            Biomes.WINDSWEPT_FOREST);

    private BonePatches() {
    }

    public static ResourceKey<Biome> at(int blockX, int blockZ, long salt, ResourceKey<Biome> vanilla) {
        if (vanilla != null && MOUNTAINS.contains(vanilla)) {
            return ModBiomes.BONE_PEAKS;
        }
        double region = MoorPatches.layered(blockX, blockZ, REGION_SCALE, salt ^ REGION_SALT);
        if (region < ASH_BELOW) {
            return ModBiomes.ASH_WASTES;
        }
        if (region > CANYON_ABOVE) {
            return ModBiomes.RIBCAGE_CANYON;
        }
        if (MoorPatches.layered(blockX, blockZ, FLATS_SCALE, salt ^ FLATS_SALT) > FLATS_ABOVE) {
            return ModBiomes.OSSUARY_FLATS;
        }
        return ModBiomes.SKELETON_HEEL_BONELANDS;
    }
}
