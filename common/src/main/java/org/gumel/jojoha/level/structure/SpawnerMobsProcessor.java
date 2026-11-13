package org.gumel.jojoha.level.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.gumel.jojoha.registry.ModBlocks;
import org.gumel.jojoha.registry.ModRegistries;

import java.util.List;

public final class SpawnerMobsProcessor extends StructureProcessor {
    public record Pick(ResourceLocation type, int weight) {
        public static final Codec<Pick> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("type").forGetter(Pick::type),
                ExtraCodecs.POSITIVE_INT.optionalFieldOf("weight", 1).forGetter(Pick::weight)
        ).apply(instance, Pick::new));
    }

    public record Band(int minY, int maxY, List<Pick> mobs) {
        public static final Codec<Band> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.optionalFieldOf("min_y", Integer.MIN_VALUE).forGetter(Band::minY),
                Codec.INT.optionalFieldOf("max_y", Integer.MAX_VALUE).forGetter(Band::maxY),
                Pick.CODEC.listOf().fieldOf("mobs").forGetter(Band::mobs)
        ).apply(instance, Band::new));

        boolean holds(int y) {
            return y >= minY && y <= maxY;
        }
    }

    public static final MapCodec<SpawnerMobsProcessor> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Band.CODEC.listOf().fieldOf("bands").forGetter(p -> p.bands)
    ).apply(instance, SpawnerMobsProcessor::new));

    private final List<Band> bands;

    public SpawnerMobsProcessor(List<Band> bands) {
        this.bands = bands;
    }

    @Override
    public StructureTemplate.StructureBlockInfo processBlock(LevelReader level, BlockPos offset, BlockPos pos,
                                                             StructureTemplate.StructureBlockInfo original,
                                                             StructureTemplate.StructureBlockInfo placed,
                                                             StructurePlaceSettings settings) {
        boolean vanilla = placed.state().is(Blocks.SPAWNER);
        if (!vanilla && !placed.state().is(ModBlocks.DUNGEON_SPAWNER.get())) {
            return placed;
        }
        Band band = bandAt(original.pos().getY());
        if (band == null || band.mobs().isEmpty()) {
            return placed;
        }
        CompoundTag nbt = placed.nbt() == null ? new CompoundTag() : placed.nbt().copy();
        if (vanilla) {
            vanilla(nbt, band.mobs());
        } else {
            nbt.put("Roster", roster(band.mobs()));
        }
        return new StructureTemplate.StructureBlockInfo(placed.pos(), placed.state(), nbt);
    }

    private Band bandAt(int y) {
        for (Band band : bands) {
            if (band.holds(y)) {
                return band;
            }
        }
        return null;
    }

    private static CompoundTag entity(ResourceLocation type) {
        CompoundTag entity = new CompoundTag();
        entity.putString("id", type.toString());
        CompoundTag data = new CompoundTag();
        data.put("entity", entity);
        return data;
    }

    private static void vanilla(CompoundTag nbt, List<Pick> mobs) {
        ListTag potentials = new ListTag();
        for (Pick pick : mobs) {
            CompoundTag entry = new CompoundTag();
            entry.putInt("weight", pick.weight());
            entry.put("data", entity(pick.type()));
            potentials.add(entry);
        }
        nbt.put("SpawnPotentials", potentials);
        nbt.put("SpawnData", entity(mobs.get(0).type()));
    }

    private static ListTag roster(List<Pick> mobs) {
        ListTag roster = new ListTag();
        for (Pick pick : mobs) {
            CompoundTag entry = new CompoundTag();
            entry.putString("Id", pick.type().toString());
            entry.putInt("Weight", pick.weight());
            roster.add(entry);
        }
        return roster;
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return ModRegistries.SPAWNER_MOBS_PROCESSOR.get();
    }
}
