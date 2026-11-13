package org.gumel.jojoha.level.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.storage.loot.LootTable;
import org.gumel.jojoha.registry.ModRegistries;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

public class SurfaceTemplateStructure extends Structure {
    public static final MapCodec<SurfaceTemplateStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            settingsCodec(instance),
            ResourceLocation.CODEC.fieldOf("template").forGetter(structure -> structure.template),
            Codec.intRange(0, 8).optionalFieldOf("flatness", 3).forGetter(structure -> structure.flatness),
            Codec.intRange(0, 4).optionalFieldOf("sink", 1).forGetter(structure -> structure.sink),
            ResourceKey.codec(Registries.LOOT_TABLE).optionalFieldOf("chest_loot", SurfaceTemplatePiece.CHEST_LOOT)
                    .forGetter(structure -> structure.chest),
            ResourceKey.codec(Registries.LOOT_TABLE).optionalFieldOf("barrel_loot", SurfaceTemplatePiece.BARREL_LOOT)
                    .forGetter(structure -> structure.barrel),
            SpawnerMobsProcessor.Band.CODEC.listOf().optionalFieldOf("spawners", List.of())
                    .forGetter(structure -> structure.spawners),
            StructureAvoid.CODEC.listOf().optionalFieldOf("avoid", List.of()).forGetter(structure -> structure.avoid)
    ).apply(instance, SurfaceTemplateStructure::new));

    private final ResourceLocation template;
    private final int flatness;
    private final int sink;
    private final ResourceKey<LootTable> chest;
    private final ResourceKey<LootTable> barrel;
    private final List<SpawnerMobsProcessor.Band> spawners;
    private final List<StructureAvoid> avoid;

    public SurfaceTemplateStructure(StructureSettings settings, ResourceLocation template, int flatness, int sink,
                                    ResourceKey<LootTable> chest, ResourceKey<LootTable> barrel,
                                    List<SpawnerMobsProcessor.Band> spawners, List<StructureAvoid> avoid) {
        super(settings);
        this.template = template;
        this.flatness = flatness;
        this.sink = sink;
        this.chest = chest;
        this.barrel = barrel;
        this.spawners = spawners;
        this.avoid = avoid;
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        if (StructureAvoid.near(context, avoid)) {
            return Optional.empty();
        }
        ChunkPos chunk = context.chunkPos();
        Rotation rotation = Rotation.getRandom(context.random());
        Vec3i turned = context.structureTemplateManager().getOrCreate(template).getSize(rotation);
        int x = chunk.getMiddleBlockX();
        int z = chunk.getMiddleBlockZ();
        OptionalInt ground = GroundFit.site(context, x, z, turned, flatness);
        if (ground.isEmpty()) {
            return Optional.empty();
        }

        BlockPos middle = new BlockPos(x, ground.getAsInt() - 1, z);
        return Optional.of(new GenerationStub(middle, pieces -> pieces.addPiece(
                new SurfaceTemplatePiece(context.structureTemplateManager(), template, middle, rotation, sink,
                        new SurfaceTemplatePiece.Stock(chest, barrel, spawners, 0)))));
    }

    @Override
    public StructureType<?> type() {
        return ModRegistries.SURFACE_TEMPLATE_STRUCTURE.get();
    }
}
