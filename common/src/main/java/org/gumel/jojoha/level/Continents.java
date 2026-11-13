package org.gumel.jojoha.level;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;

import java.util.Set;

public final class Continents {
    public enum Continent {
        NONE(null, 1F),
        PHANTOM_MOORS(ModBiomes.PHANTOM_MOORS, 1F),
        BONELANDS(ModBiomes.SKELETON_HEEL_BONELANDS, 1F),
        STARDUST_SANDS(ModBiomes.STARDUST_SANDS, 3F);

        public final ResourceKey<Biome> biome;
        public final float standUsers;

        Continent(ResourceKey<Biome> biome, float standUsers) {
            this.biome = biome;
            this.standUsers = standUsers;
        }
    }

    public static final int CELL = 3072;

    private static final double RADIUS = 0.34;
    private static final double JITTER = 0.08;
    private static final double WOBBLE = 0.07;
    private static final double RIPPLE = 0.03;
    private static final float VANILLA_SHARE = 0.3F;

    private static final Continent[] RING = {
            Continent.PHANTOM_MOORS, Continent.NONE, Continent.STARDUST_SANDS, Continent.BONELANDS,
            Continent.PHANTOM_MOORS, Continent.STARDUST_SANDS, Continent.NONE, Continent.BONELANDS};

    private static final Continent[] LANDS = {
            Continent.PHANTOM_MOORS, Continent.BONELANDS, Continent.STARDUST_SANDS};

    private static final Set<ResourceKey<Biome>> KEPT = Set.of(
            Biomes.OCEAN, Biomes.DEEP_OCEAN, Biomes.COLD_OCEAN, Biomes.DEEP_COLD_OCEAN,
            Biomes.FROZEN_OCEAN, Biomes.DEEP_FROZEN_OCEAN, Biomes.LUKEWARM_OCEAN, Biomes.DEEP_LUKEWARM_OCEAN,
            Biomes.WARM_OCEAN, Biomes.RIVER, Biomes.FROZEN_RIVER, Biomes.BEACH, Biomes.SNOWY_BEACH,
            Biomes.STONY_SHORE, Biomes.MUSHROOM_FIELDS, Biomes.LUSH_CAVES, Biomes.DRIPSTONE_CAVES,
            Biomes.DEEP_DARK);

    private static volatile Object[] lastSalt = {null, 0L};

    private Continents() {
    }

    public static long salt(Climate.Sampler sampler) {
        Object[] last = lastSalt;
        if (last[0] == sampler) {
            return (Long) last[1];
        }
        Climate.TargetPoint probe = sampler.sample(4099, 0, -7919);
        long salt = mix(probe.temperature() * 31 + probe.humidity() * 17 + probe.weirdness());
        lastSalt = new Object[]{sampler, salt};
        return salt;
    }

    public static boolean remaps(Holder<Biome> vanilla) {
        return vanilla.unwrapKey()
                .map(key -> "minecraft".equals(key.location().getNamespace()) && !KEPT.contains(key))
                .orElse(false);
    }

    public static Continent at(int blockX, int blockZ, long salt) {
        int cellX = Math.floorDiv(blockX, CELL);
        int cellZ = Math.floorDiv(blockZ, CELL);
        Continent continent = kind(cellX, cellZ, salt);
        if (continent == Continent.NONE) {
            return Continent.NONE;
        }
        return edgeDistance(blockX, blockZ, cellX, cellZ, salt) >= 0.0 ? continent : Continent.NONE;
    }

    public static double inset(int blockX, int blockZ, long salt, Continent wanted) {
        int cellX = Math.floorDiv(blockX, CELL);
        int cellZ = Math.floorDiv(blockZ, CELL);
        if (wanted == Continent.NONE || kind(cellX, cellZ, salt) != wanted) {
            return -1.0;
        }
        return edgeDistance(blockX, blockZ, cellX, cellZ, salt);
    }

    private static double edgeDistance(int blockX, int blockZ, int cellX, int cellZ, long salt) {
        long hash = mix(salt ^ (cellX * 0x632BE59BD9B4E019L) ^ (cellZ * 0x9E3779B97F4A7C15L));
        double centreX = (cellX + 0.5 + JITTER * signed(hash)) * CELL;
        double centreZ = (cellZ + 0.5 + JITTER * signed(hash >>> 21)) * CELL;
        double dx = blockX - centreX;
        double dz = blockZ - centreZ;
        double angle = Mth.atan2(dz, dx);
        double edge = CELL * (RADIUS
                + WOBBLE * Math.sin(3.0 * angle + unit(hash >>> 7) * Mth.TWO_PI)
                + RIPPLE * Math.sin(7.0 * angle + unit(hash >>> 13) * Mth.TWO_PI));
        return edge - Math.sqrt(dx * dx + dz * dz);
    }

    public static final int SEARCH_CELLS = 4;

    public static BlockPos nearest(ServerLevel level, BlockPos from, Continent wanted) {
        if (level.dimension() != Level.OVERWORLD || wanted == Continent.NONE) {
            return null;
        }
        long salt = salt(level.getChunkSource().randomState().sampler());
        int homeX = Math.floorDiv(from.getX(), CELL);
        int homeZ = Math.floorDiv(from.getZ(), CELL);
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int cellX = homeX - SEARCH_CELLS; cellX <= homeX + SEARCH_CELLS; cellX++) {
            for (int cellZ = homeZ - SEARCH_CELLS; cellZ <= homeZ + SEARCH_CELLS; cellZ++) {
                if (kind(cellX, cellZ, salt) != wanted) {
                    continue;
                }
                long hash = mix(salt ^ (cellX * 0x632BE59BD9B4E019L) ^ (cellZ * 0x9E3779B97F4A7C15L));
                int x = (int) ((cellX + 0.5 + JITTER * signed(hash)) * CELL);
                int z = (int) ((cellZ + 0.5 + JITTER * signed(hash >>> 21)) * CELL);
                double distance = from.distToCenterSqr(x, from.getY(), z);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = new BlockPos(x, from.getY(), z);
                }
            }
        }
        return best;
    }

    public static Continent kind(int cellX, int cellZ, long salt) {
        if (cellX == 0 && cellZ == 0) {
            return Continent.NONE;
        }
        if (Math.abs(cellX) <= 1 && Math.abs(cellZ) <= 1) {
            int index = ringIndex(cellX, cellZ);
            return RING[Math.floorMod(index + (int) (salt & 7), RING.length)];
        }
        long hash = mix(salt + cellX * 0x5DEECE66DL + cellZ * 0x27BB2EE687B0B0FDL);
        if (unit(hash) < VANILLA_SHARE) {
            return Continent.NONE;
        }
        return LANDS[(int) Math.floorMod(hash >>> 32, LANDS.length)];
    }

    public static ResourceKey<Biome> biomeAt(int blockX, int blockZ, long salt, ResourceKey<Biome> vanilla) {
        return switch (at(blockX, blockZ, salt)) {
            case PHANTOM_MOORS -> MoorPatches.at(blockX, blockZ, salt);
            case BONELANDS -> BonePatches.at(blockX, blockZ, salt, vanilla);
            case STARDUST_SANDS -> SandPatches.at(blockX, blockZ, salt);
            case NONE -> null;
        };
    }

    public static Continent at(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server) || server.dimension() != Level.OVERWORLD) {
            return Continent.NONE;
        }
        return at(pos.getX(), pos.getZ(), salt(server.getChunkSource().randomState().sampler()));
    }

    public static Continent of(ResourceKey<Biome> biome) {
        for (Continent continent : LANDS) {
            if (continent.biome == biome) {
                return continent;
            }
        }
        if (ModBiomes.MOORS.contains(biome)) {
            return Continent.PHANTOM_MOORS;
        }
        if (ModBiomes.BONELANDS.contains(biome)) {
            return Continent.BONELANDS;
        }
        if (ModBiomes.SANDS.contains(biome)) {
            return Continent.STARDUST_SANDS;
        }
        return Continent.NONE;
    }

    private static int ringIndex(int cellX, int cellZ) {
        int[][] order = {{-1, -1}, {0, -1}, {1, -1}, {1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}};
        for (int i = 0; i < order.length; i++) {
            if (order[i][0] == cellX && order[i][1] == cellZ) {
                return i;
            }
        }
        return 0;
    }

    static long mix(long seed) {
        seed ^= seed >>> 33;
        seed *= 0xFF51AFD7ED558CCDL;
        seed ^= seed >>> 33;
        seed *= 0xC4CEB9FE1A85EC53L;
        seed ^= seed >>> 33;
        return seed;
    }

    static double unit(long hash) {
        return (hash & 0xFFFFFFL) / (double) 0x1000000L;
    }

    private static double signed(long hash) {
        return unit(hash) * 2.0 - 1.0;
    }
}
