package org.gumel.jojoha.level.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import org.gumel.jojoha.registry.ModRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class SpacedGridPlacement extends PlacementModifier {

    public static final MapCodec<SpacedGridPlacement> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    Codec.intRange(1, 4096).fieldOf("spacing").forGetter(p -> p.spacing),
                    Codec.intRange(0, 4096).fieldOf("min_distance").forGetter(p -> p.minDistance),
                    Codec.floatRange(0.0F, 1.0F).optionalFieldOf("density", 1.0F)
                            .forGetter(p -> p.density),
                    Codec.intRange(1, 64).optionalFieldOf("clump", 6).forGetter(p -> p.clump)
            ).apply(instance, SpacedGridPlacement::new));

    private final int spacing;
    private final int minDistance;
    private final float density;
    private final int clump;

    private final int reach;

    private final long minDistanceSq;

    public SpacedGridPlacement(int spacing, int minDistance, float density, int clump) {
        this.spacing = spacing;
        this.minDistance = minDistance;
        this.density = density;
        this.clump = clump;
        this.reach = Math.max(1, (int) Math.ceil((double) minDistance / spacing));
        this.minDistanceSq = (long) minDistance * minDistance;
    }

    @Override
    public Stream<BlockPos> getPositions(PlacementContext context, RandomSource random, BlockPos origin) {
        long seed = context.getLevel().getSeed();
        int minX = SectionPos.sectionToBlockCoord(SectionPos.blockToSectionCoord(origin.getX()));
        int minZ = SectionPos.sectionToBlockCoord(SectionPos.blockToSectionCoord(origin.getZ()));

        int firstCellX = Math.floorDiv(minX - spacing, spacing);
        int lastCellX = Math.floorDiv(minX + 15, spacing);
        int firstCellZ = Math.floorDiv(minZ - spacing, spacing);
        int lastCellZ = Math.floorDiv(minZ + 15, spacing);

        List<BlockPos> out = new ArrayList<>();
        for (int cellX = firstCellX; cellX <= lastCellX; cellX++) {
            for (int cellZ = firstCellZ; cellZ <= lastCellZ; cellZ++) {
                int x = candidateX(seed, cellX, cellZ);
                int z = candidateZ(seed, cellX, cellZ);
                if (x < minX || x > minX + 15 || z < minZ || z > minZ + 15) continue;
                if (crowded(seed, cellX, cellZ, x, z)) continue;
                if (density < 1.0F && noise(seed, x, z) > density) continue;

                out.add(new BlockPos(x, origin.getY(), z));
            }
        }
        return out.stream();
    }

    private boolean crowded(long seed, int cellX, int cellZ, int x, int z) {
        for (int ox = -reach; ox <= reach; ox++) {
            for (int oz = -reach; oz <= reach; oz++) {
                if (ox == 0 && oz == 0) continue;
                int nx = cellX + ox;
                int nz = cellZ + oz;
                if (!(nz < cellZ || (nz == cellZ && nx < cellX))) continue;

                long dx = candidateX(seed, nx, nz) - x;
                long dz = candidateZ(seed, nx, nz) - z;
                if (dx * dx + dz * dz < minDistanceSq) return true;
            }
        }
        return false;
    }

    private int candidateX(long seed, int cellX, int cellZ) {
        return cellX * spacing + (int) Math.floorMod(hash(seed, cellX, cellZ), spacing);
    }

    private int candidateZ(long seed, int cellX, int cellZ) {
        return cellZ * spacing + (int) Math.floorMod(hash(seed, cellX, cellZ) >> 21, spacing);
    }

    private float noise(long seed, int x, int z) {
        int size = spacing * clump;
        int gx = Math.floorDiv(x, size);
        int gz = Math.floorDiv(z, size);
        float fx = smooth((x - gx * size) / (float) size);
        float fz = smooth((z - gz * size) / (float) size);
        long s = seed ^ 0x5DEECE66DL;

        return lerp(fz,
                lerp(fx, unit(hash(s, gx, gz)), unit(hash(s, gx + 1, gz))),
                lerp(fx, unit(hash(s, gx, gz + 1)), unit(hash(s, gx + 1, gz + 1))));
    }

    private static float smooth(float t) {
        return t * t * (3.0F - 2.0F * t);
    }

    private static float lerp(float t, float a, float b) {
        return a + t * (b - a);
    }

    private static float unit(long h) {
        return (h >>> 11) / (float) (1L << 53);
    }

    private static long hash(long seed, int x, int z) {
        long h = seed ^ (x * 0x9E3779B97F4A7C15L) ^ (z * 0xC2B2AE3D27D4EB4FL);
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 32;
        h *= 0x94D049BB133111EBL;
        h ^= h >>> 29;
        return h;
    }

    @Override
    public PlacementModifierType<?> type() {
        return ModRegistries.SPACED_GRID.get();
    }
}
