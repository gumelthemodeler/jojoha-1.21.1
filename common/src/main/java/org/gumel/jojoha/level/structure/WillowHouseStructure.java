package org.gumel.jojoha.level.structure;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.registry.ModRegistries;

import java.util.Optional;
import java.util.OptionalInt;

public class WillowHouseStructure extends Structure {
    public static final MapCodec<WillowHouseStructure> CODEC = simpleCodec(WillowHouseStructure::new);

    public static final ResourceLocation TEMPLATE =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "willow_house");

    private static final int FLATNESS = 3;

    public WillowHouseStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        Rotation rotation = Rotation.getRandom(context.random());
        Vec3i turned = context.structureTemplateManager().getOrCreate(TEMPLATE).getSize(rotation);
        int x = chunk.getMiddleBlockX();
        int z = chunk.getMiddleBlockZ();
        OptionalInt ground = GroundFit.site(context, x, z, turned, FLATNESS);
        if (ground.isEmpty()) {
            return Optional.empty();
        }

        BlockPos middle = new BlockPos(x, ground.getAsInt() - 1, z);
        return Optional.of(new GenerationStub(middle, pieces -> pieces.addPiece(
                new WillowHousePiece(context.structureTemplateManager(), middle, rotation))));
    }

    @Override
    public StructureType<?> type() {
        return ModRegistries.WILLOW_HOUSE_STRUCTURE.get();
    }
}
