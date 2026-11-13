package org.gumel.jojoha.level.feature;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.gumel.jojoha.level.Continents;
import org.gumel.jojoha.level.MoorPatches;
import org.gumel.jojoha.level.SandPatches;
import org.gumel.jojoha.registry.ModBlocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DuneFeature extends Feature<NoneFeatureConfiguration> {
    public static final int HEIGHT = 12;
    public static final double WAVELENGTH = 56.0;
    public static final double CREST = 0.7;

    private static final double WARP_SCALE = 90.0;
    private static final double WARP = 1.1;
    private static final double SWELL_SCALE = 70.0;
    private static final double SWELL_LOW = 0.45;
    private static final int SHORE_RISE = 6;
    private static final int CLEARANCE = 16;
    private static final int PLACE_FLAGS = Block.UPDATE_CLIENTS;

    private static final long WARP_SALT = 0x5BE0CD19L;
    private static final long SWELL_SALT = 0x9B05688CL;

    public DuneFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        long salt = Continents.salt(level.getLevel().getChunkSource().randomState().sampler());
        ChunkPos chunk = new ChunkPos(context.origin());
        List<ChunkPos> built = structuresAround(level, chunk);
        double wind = SandPatches.wind(salt);
        double cos = Math.cos(wind);
        double sin = Math.sin(wind);
        BlockState sand = ModBlocks.CRUSADER_SAND.get().defaultBlockState();
        int sea = level.getSeaLevel();
        boolean placed = false;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = chunk.getMinBlockX() + dx;
                int z = chunk.getMinBlockZ() + dz;
                double weight = SandPatches.dunes(x, z, salt) * clearance(built, x, z);
                if (weight <= 0.0) {
                    continue;
                }
                int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                cursor.set(x, top, z);
                if (!level.getBlockState(cursor).is(BlockTags.SAND)
                        || !level.getFluidState(cursor.above()).isEmpty()) {
                    continue;
                }
                double shore = Mth.clamp((top - sea) / (double) SHORE_RISE, 0.0, 1.0);
                int pile = (int) Math.round(HEIGHT * weight * shore * profile(x, z, cos, sin, salt));
                for (int y = 1; y <= pile; y++) {
                    cursor.set(x, top + y, z);
                    if (!level.getBlockState(cursor).isAir()) {
                        break;
                    }
                    level.setBlock(cursor, sand, PLACE_FLAGS);
                    placed = true;
                }
            }
        }
        return placed;
    }

    private static double profile(int x, int z, double cos, double sin, long salt) {
        double along = x * cos + z * sin;
        double across = -x * sin + z * cos;
        double warp = (MoorPatches.layered(x, z, WARP_SCALE, salt ^ WARP_SALT) - 0.5) * 2.0 * WARP;
        double phase = along / WAVELENGTH + warp;
        double f = phase - Math.floor(phase);
        double rise = f < CREST ? f / CREST : (1.0 - f) / (1.0 - CREST);
        double swell = Mth.lerp(MoorPatches.layered((int) across, (int) along, SWELL_SCALE, salt ^ SWELL_SALT),
                SWELL_LOW, 1.0);
        return rise * rise * (3.0 - 2.0 * rise) * swell;
    }

    private static List<ChunkPos> structuresAround(WorldGenLevel level, ChunkPos centre) {
        List<ChunkPos> found = new ArrayList<>();
        for (int ox = -1; ox <= 1; ox++) {
            for (int oz = -1; oz <= 1; oz++) {
                ChunkAccess chunk = level.getChunk(centre.x + ox, centre.z + oz);
                for (Map.Entry<Structure, LongSet> entry : chunk.getAllReferences().entrySet()) {
                    if (entry.getKey().step() == GenerationStep.Decoration.SURFACE_STRUCTURES
                            && !entry.getValue().isEmpty()) {
                        found.add(chunk.getPos());
                        break;
                    }
                }
            }
        }
        return found;
    }

    private static double clearance(List<ChunkPos> built, int x, int z) {
        double nearest = CLEARANCE;
        for (ChunkPos chunk : built) {
            int gapX = Math.max(0, Math.max(chunk.getMinBlockX() - x, x - chunk.getMaxBlockX()));
            int gapZ = Math.max(0, Math.max(chunk.getMinBlockZ() - z, z - chunk.getMaxBlockZ()));
            nearest = Math.min(nearest, Math.max(gapX, gapZ));
        }
        return nearest / CLEARANCE;
    }
}
