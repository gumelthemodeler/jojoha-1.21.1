package org.gumel.jojoha.level;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class StructureSeek {
    private record Candidate(Holder<Structure> structure, ChunkPos chunk, long distance) {
    }

    private StructureSeek() {
    }

    public static BlockPos nearest(ServerLevel level, TagKey<Structure> tag, BlockPos from, int radiusChunks) {
        ServerChunkCache chunks = level.getChunkSource();
        ChunkGeneratorStructureState state = chunks.getGeneratorState();
        ChunkPos origin = new ChunkPos(from);
        List<Candidate> candidates = new ArrayList<>();
        for (Holder<Structure> structure : level.registryAccess().registryOrThrow(Registries.STRUCTURE).getTagOrEmpty(tag)) {
            for (StructurePlacement placement : state.getPlacementsForStructure(structure)) {
                if (placement instanceof RandomSpreadStructurePlacement spread) {
                    spread(state, spread, structure, origin, radiusChunks, candidates);
                } else if (placement instanceof ConcentricRingsStructurePlacement rings) {
                    for (ChunkPos chunk : state.getRingPositionsFor(rings)) {
                        add(structure, chunk, origin, radiusChunks, candidates);
                    }
                }
            }
        }
        candidates.sort(Comparator.comparingLong(Candidate::distance));
        ChunkGenerator generator = chunks.getGenerator();
        RandomState random = chunks.randomState();
        for (Candidate candidate : candidates) {
            BlockPos at = candidate.chunk().getMiddleBlockPosition(0);
            int y = generator.getBaseHeight(at.getX(), at.getZ(), Heightmap.Types.WORLD_SURFACE_WG, level, random);
            if (candidate.structure().value().biomes().contains(generator.getBiomeSource().getNoiseBiome(
                    QuartPos.fromBlock(at.getX()), QuartPos.fromBlock(y), QuartPos.fromBlock(at.getZ()), random.sampler()))) {
                return at.atY(y);
            }
        }
        return null;
    }

    private static void spread(ChunkGeneratorStructureState state, RandomSpreadStructurePlacement spread,
                               Holder<Structure> structure, ChunkPos origin, int radiusChunks, List<Candidate> candidates) {
        int spacing = spread.spacing();
        int cells = radiusChunks / spacing + 1;
        for (int dx = -cells; dx <= cells; dx++) {
            for (int dz = -cells; dz <= cells; dz++) {
                ChunkPos chunk = spread.getPotentialStructureChunk(state.getLevelSeed(),
                        origin.x + dx * spacing, origin.z + dz * spacing);
                if (spread.isStructureChunk(state, chunk.x, chunk.z)) {
                    add(structure, chunk, origin, radiusChunks, candidates);
                }
            }
        }
    }

    private static void add(Holder<Structure> structure, ChunkPos chunk, ChunkPos origin, int radiusChunks,
                            List<Candidate> candidates) {
        long dx = chunk.x - origin.x, dz = chunk.z - origin.z;
        if (Math.abs(dx) <= radiusChunks && Math.abs(dz) <= radiusChunks) {
            candidates.add(new Candidate(structure, chunk, dx * dx + dz * dz));
        }
    }
}
