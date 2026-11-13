package org.gumel.jojoha.level.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public record StructureAvoid(ResourceKey<StructureSet> set, int chunks) {
    public static final Codec<StructureAvoid> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceKey.codec(Registries.STRUCTURE_SET).fieldOf("set").forGetter(StructureAvoid::set),
            Codec.intRange(1, 16).fieldOf("chunks").forGetter(StructureAvoid::chunks)
    ).apply(instance, StructureAvoid::new));

    private static final int REMEMBERED = 4096;
    private static final Map<String, Boolean> BUILT = new ConcurrentHashMap<>();
    private static final ThreadLocal<Boolean> ASKING = ThreadLocal.withInitial(() -> false);

    public static boolean near(Structure.GenerationContext context, List<StructureAvoid> rules) {
        if (rules.isEmpty() || ASKING.get()) {
            return false;
        }
        Registry<StructureSet> sets = context.registryAccess().registryOrThrow(Registries.STRUCTURE_SET);
        for (StructureAvoid rule : rules) {
            StructureSet set = sets.get(rule.set);
            if (set != null && set.placement() instanceof RandomSpreadStructurePlacement spread
                    && rule.near(context, set, spread)) {
                return true;
            }
        }
        return false;
    }

    private boolean near(Structure.GenerationContext context, StructureSet set, RandomSpreadStructurePlacement spread) {
        ChunkPos here = context.chunkPos();
        int spacing = spread.spacing();
        for (int rx = Math.floorDiv(here.x - chunks, spacing); rx <= Math.floorDiv(here.x + chunks, spacing); rx++) {
            for (int rz = Math.floorDiv(here.z - chunks, spacing); rz <= Math.floorDiv(here.z + chunks, spacing); rz++) {
                ChunkPos at = spread.getPotentialStructureChunk(context.seed(), rx * spacing, rz * spacing);
                if (Math.abs(at.x - here.x) <= chunks && Math.abs(at.z - here.z) <= chunks && built(context, set, at)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean built(Structure.GenerationContext context, StructureSet set, ChunkPos at) {
        String key = set.hashCode() + "/" + context.seed() + "/" + at.toLong();
        Boolean known = BUILT.get(key);
        if (known != null) {
            return known;
        }
        boolean found = false;
        ASKING.set(true);
        try {
            for (StructureSet.StructureSelectionEntry entry : set.structures()) {
                Holder<Structure> structure = entry.structure();
                Structure.GenerationContext there = new Structure.GenerationContext(context.registryAccess(),
                        context.chunkGenerator(), context.biomeSource(), context.randomState(),
                        context.structureTemplateManager(), context.seed(), at, context.heightAccessor(),
                        structure.value().biomes()::contains);
                if (structure.value().findValidGenerationPoint(there).isPresent()) {
                    found = true;
                    break;
                }
            }
        } finally {
            ASKING.set(false);
        }
        if (BUILT.size() > REMEMBERED) {
            BUILT.clear();
        }
        BUILT.put(key, found);
        return found;
    }
}
