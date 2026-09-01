package org.gumel.jojoha.level.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import org.gumel.jojoha.registry.ModRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Emits positions with a guaranteed minimum distance between them, so whatever is placed at them
 * cannot crowd itself.
 *
 * <h2>Why this had to be written rather than configured</h2>
 *
 * <p>Trees do not check whether they are standing in another tree, and no configuration makes them.
 * The tree feature's clearance test bottoms out in {@code TrunkPlacer.isFree}, which is
 * {@code validTreePos(level, pos) || level.isStateAtPosition(pos, s -> s.is(BlockTags.LOGS))}.
 * Leaves are free because they are a valid tree position; logs are free because the second half of
 * that expression says so outright. An existing tree is invisible to a new one in both of the
 * materials it is made of, so widening the clearance radius cannot separate two trees - it only
 * makes them refuse to grow near terrain, which on a hilly biome means bare hillsides.
 *
 * <p>Vanilla lives with this and its own forests do interpenetrate; its oaks are just small enough
 * that it does not read as a fault. Ours are not. So the spacing has to be imposed before the
 * feature runs, which is what a placement modifier is for.
 *
 * <h2>How the distance is guaranteed</h2>
 *
 * <p>The world is cut into cells {@code spacing} blocks across, and each cell offers one candidate
 * placed anywhere within it. The offset comes from hashing the cell's coordinates rather than from
 * the placement random, so a cell answers identically no matter which chunk is being generated, or
 * in what order - which is what makes the guarantee hold across chunk borders, where a per-chunk
 * scatter would not.
 *
 * <p>A candidate is then dropped if it lands within {@code minDistance} of a neighbouring cell's
 * candidate. The tie has to be broken without knowing which neighbours survived - otherwise a
 * chunk's answer would depend on the order chunks generate in - so a cell yields to any neighbour
 * earlier in a fixed global order, whether or not that neighbour was itself kept. That discards a
 * few placements it could have allowed, and buys a rule that is purely local and identical
 * everywhere.
 *
 * <p>The alternative, confining each candidate to the middle of its cell, makes the distance fall
 * out arithmetically with no rejection at all, and it is denser. It was measured and rejected: at a
 * useful density the jitter band is only two or three blocks wide, so every trunk lands on one of a
 * handful of residues and the result is a visible lattice in open ground. This way every offset in
 * the cell gets used.
 *
 * <h2>Clumping</h2>
 *
 * <p>Even spacing everywhere would be an orchard. A coarse value noise thins the candidates in
 * bands so they gather into woods with clearings between, and because thinning only ever removes
 * candidates it can never violate the minimum distance.
 */
public class SpacedGridPlacement extends PlacementModifier {

    public static final MapCodec<SpacedGridPlacement> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    Codec.intRange(1, 64).fieldOf("spacing").forGetter(p -> p.spacing),
                    Codec.intRange(0, 64).fieldOf("min_distance").forGetter(p -> p.minDistance),
                    Codec.floatRange(0.0F, 1.0F).optionalFieldOf("density", 1.0F)
                            .forGetter(p -> p.density),
                    Codec.intRange(1, 64).optionalFieldOf("clump", 6).forGetter(p -> p.clump)
            ).apply(instance, SpacedGridPlacement::new));

    private final int spacing;
    private final int minDistance;
    private final float density;
    private final int clump;

    /**
     * How many cells out to look when enforcing the distance.
     *
     * <p>Usually one. It is derived rather than assumed because the guarantee quietly depends on it:
     * two candidates {@code n} cells apart are at least {@code (n - 1) * spacing + 1} blocks apart,
     * so checking only immediate neighbours is sound exactly while {@code minDistance} does not
     * exceed {@code spacing + 1}. Deriving the radius means the modifier stays correct if those
     * numbers are ever retuned, instead of silently letting distant pairs through.
     */
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
        int minX = origin.getX();
        int minZ = origin.getZ();

        // Every cell that could put its candidate inside this chunk, and no others - a candidate is
        // emitted by exactly the one chunk containing it, so nothing is placed twice or missed.
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

    /** True when an earlier neighbour has already claimed the space this candidate wants. */
    private boolean crowded(long seed, int cellX, int cellZ, int x, int z) {
        for (int ox = -reach; ox <= reach; ox++) {
            for (int oz = -reach; oz <= reach; oz++) {
                if (ox == 0 && oz == 0) continue;
                int nx = cellX + ox;
                int nz = cellZ + oz;
                // The fixed global order: smaller z first, then smaller x.
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

    /**
     * Smoothed value noise in {@code [0, 1)}, coarse enough to make woods rather than speckle.
     *
     * <p>Four corner values of a large cell, smoothstepped and bilinearly blended. This is not
     * vanilla's noise and does not need to be - nothing else consults it, and all that is asked of
     * it is to vary slowly and to answer the same way every time.
     */
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

    /** A mix strong enough that neighbouring cells do not correlate, which a plain multiply is not. */
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
