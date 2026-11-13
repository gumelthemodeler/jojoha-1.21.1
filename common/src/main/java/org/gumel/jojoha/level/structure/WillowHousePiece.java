package org.gumel.jojoha.level.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.gumel.jojoha.entity.WillowEntity;
import org.gumel.jojoha.registry.ModRegistries;

public class WillowHousePiece extends TemplateStructurePiece {
    private static final int GROUND_COURSE = 1;

    public static final BlockPos WILLOW_AT = new BlockPos(13, 3, 9);

    public static final int WILLOW_ROAM = 6;

    public WillowHousePiece(StructureTemplateManager templates, BlockPos middle, Rotation rotation) {
        super(ModRegistries.WILLOW_HOUSE_PIECE.get(), 0, templates, WillowHouseStructure.TEMPLATE,
                WillowHouseStructure.TEMPLATE.toString(), settings(templates, rotation),
                middle.subtract(pivot(templates)).below(GROUND_COURSE));
    }

    public WillowHousePiece(StructureTemplateManager templates, CompoundTag tag) {
        super(ModRegistries.WILLOW_HOUSE_PIECE.get(), tag, templates,
                location -> settings(templates, Rotation.valueOf(tag.getString("Rot"))));
    }

    private static BlockPos pivot(StructureTemplateManager templates) {
        Vec3i size = templates.getOrCreate(WillowHouseStructure.TEMPLATE).getSize();
        return new BlockPos(size.getX() / 2, 0, size.getZ() / 2);
    }

    private static StructurePlaceSettings settings(StructureTemplateManager templates, Rotation rotation) {
        return new StructurePlaceSettings()
                .setRotation(rotation)
                .setMirror(Mirror.NONE)
                .setRotationPivot(pivot(templates))
                .addProcessor(BlockIgnoreProcessor.STRUCTURE_AND_AIR)
                .addProcessor(new GroundTrimProcessor(GroundFit.footprint(
                        templates.getOrCreate(WillowHouseStructure.TEMPLATE), GROUND_COURSE), GROUND_COURSE))
                .addProcessor(new ContainerLootProcessor(SurfaceTemplatePiece.CHEST_LOOT,
                        SurfaceTemplatePiece.BARREL_LOOT))
                .setLiquidSettings(LiquidSettings.IGNORE_WATERLOGGING);
    }

    @Override
    public BoundingBox getBoundingBox() {
        return super.getBoundingBox().inflatedBy(GroundFit.MARGIN, 0, GroundFit.MARGIN);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        super.addAdditionalSaveData(context, tag);
        tag.putString("Rot", this.placeSettings.getRotation().name());
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator,
                            RandomSource random, BoundingBox chunk, ChunkPos chunkPos, BlockPos pivot) {
        super.postProcess(level, structures, generator, random, chunk, chunkPos, pivot);
        GroundFit.settle(level, chunk, this.template, this.placeSettings, this.templatePosition, GROUND_COURSE, true);

        int groundY = this.templatePosition.getY() + GROUND_COURSE;
        for (StructureTemplate.StructureBlockInfo air
                : this.template.filterBlocks(this.templatePosition, this.placeSettings, Blocks.AIR)) {
            BlockPos at = air.pos();
            if (at.getY() > groundY && chunk.isInside(at) && !level.getBlockState(at).isAir()) {
                level.setBlock(at, Blocks.AIR.defaultBlockState(), 2);
            }
        }

        BlockPos home = this.templatePosition.offset(
                StructureTemplate.calculateRelativePosition(this.placeSettings, WILLOW_AT));
        if (chunk.isInside(home)) {
            WillowEntity willow = ModRegistries.WILLOW.get().create(level.getLevel());
            if (willow != null) {
                willow.moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, random.nextFloat() * 360F, 0F);
                willow.finalizeSpawn(level, level.getCurrentDifficultyAt(home), MobSpawnType.STRUCTURE, null);
                willow.setPersistenceRequired();
                willow.moveIn(home, WILLOW_ROAM);
                level.addFreshEntityWithPassengers(willow);
            }
        }
    }

    @Override
    protected void handleDataMarker(String name, BlockPos pos, ServerLevelAccessor level, RandomSource random,
                                    BoundingBox box) {
    }
}
