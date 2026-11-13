package org.gumel.jojoha.level.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.loot.LootTable;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.mixin.StructureTemplateAccessor;
import org.gumel.jojoha.registry.ModRegistries;

import java.util.ArrayList;
import java.util.List;

public class SurfaceTemplatePiece extends TemplateStructurePiece {
    public static final ResourceKey<LootTable> CHEST_LOOT = ResourceKey.create(Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "chests/ancient_ruins"));
    public static final ResourceKey<LootTable> BARREL_LOOT = ResourceKey.create(Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "chests/moor_barrel"));

    private static List<Block> fragile;

    public record Stock(ResourceKey<LootTable> chest, ResourceKey<LootTable> barrel,
                        List<SpawnerMobsProcessor.Band> spawners, int residents) {
        public static final Stock PLAIN = new Stock(CHEST_LOOT, BARREL_LOOT, List.of(), 0);

        static Stock load(CompoundTag tag) {
            if (!tag.contains("Chest")) {
                return PLAIN;
            }
            List<SpawnerMobsProcessor.Band> spawners = SpawnerMobsProcessor.Band.CODEC.listOf()
                    .parse(NbtOps.INSTANCE, tag.get("Spawners")).result().orElse(List.of());
            return new Stock(table(tag.getString("Chest")), table(tag.getString("Barrel")), spawners,
                    tag.getInt("Residents"));
        }

        private static ResourceKey<LootTable> table(String id) {
            return ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.parse(id));
        }

        void save(CompoundTag tag) {
            tag.putString("Chest", chest.location().toString());
            tag.putString("Barrel", barrel.location().toString());
            SpawnerMobsProcessor.Band.CODEC.listOf().encodeStart(NbtOps.INSTANCE, spawners).result()
                    .ifPresent(bands -> tag.put("Spawners", bands));
            if (residents > 0) {
                tag.putInt("Residents", residents);
            }
        }
    }

    private final int sink;
    private final Stock stock;
    private final boolean apron;

    public SurfaceTemplatePiece(StructureTemplateManager templates, ResourceLocation template, BlockPos middle,
                                Rotation rotation, int sink, Stock stock) {
        this(templates, template, middle, rotation, sink, stock, true);
    }

    public SurfaceTemplatePiece(StructureTemplateManager templates, ResourceLocation template, BlockPos middle,
                                Rotation rotation, int sink, Stock stock, boolean apron) {
        super(ModRegistries.SURFACE_TEMPLATE_PIECE.get(), 0, templates, template, template.toString(),
                settings(templates, template, rotation, sink, stock), middle.subtract(pivot(templates, template)).below(sink));
        this.sink = sink;
        this.stock = stock;
        this.apron = apron;
    }

    public SurfaceTemplatePiece(StructureTemplateManager templates, CompoundTag tag) {
        super(ModRegistries.SURFACE_TEMPLATE_PIECE.get(), tag, templates,
                location -> settings(templates, location, Rotation.valueOf(tag.getString("Rot")), tag.getInt("Sink"),
                        Stock.load(tag)));
        this.sink = tag.getInt("Sink");
        this.stock = Stock.load(tag);
        this.apron = !tag.getBoolean("NoApron");
    }

    private static BlockPos pivot(StructureTemplateManager templates, ResourceLocation template) {
        Vec3i size = templates.getOrCreate(template).getSize();
        return new BlockPos(size.getX() / 2, 0, size.getZ() / 2);
    }

    private static StructurePlaceSettings settings(StructureTemplateManager templates, ResourceLocation template,
                                                   Rotation rotation, int sink, Stock stock) {
        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setRotation(rotation)
                .setMirror(Mirror.NONE)
                .setRotationPivot(pivot(templates, template))
                .addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK)
                .addProcessor(new GroundTrimProcessor(GroundFit.footprint(templates.getOrCreate(template), sink), sink))
                .addProcessor(new ContainerLootProcessor(stock.chest(), stock.barrel()))
                .setLiquidSettings(LiquidSettings.IGNORE_WATERLOGGING);
        if (!stock.spawners().isEmpty()) {
            settings.addProcessor(new SpawnerMobsProcessor(stock.spawners()));
        }
        return settings;
    }

    @Override
    public BoundingBox getBoundingBox() {
        return super.getBoundingBox().inflatedBy(GroundFit.MARGIN, 0, GroundFit.MARGIN);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        super.addAdditionalSaveData(context, tag);
        tag.putString("Rot", this.placeSettings.getRotation().name());
        tag.putInt("Sink", sink);
        stock.save(tag);
        if (!apron) {
            tag.putBoolean("NoApron", true);
        }
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator,
                            RandomSource random, BoundingBox chunk, ChunkPos chunkPos, BlockPos pivot) {
        super.postProcess(level, structures, generator, random, chunk, chunkPos, pivot);
        GroundFit.settle(level, chunk, this.template, this.placeSettings, this.templatePosition, sink, apron);
        replant(level, chunk);
        residents(level, chunk, random);
    }

    private static List<Block> fragile() {
        if (fragile == null) {
            fragile = BuiltInRegistries.BLOCK.stream()
                    .filter(block -> block instanceof BushBlock || block instanceof FarmBlock)
                    .toList();
        }
        return fragile;
    }

    private void replant(WorldGenLevel level, BoundingBox chunk) {
        for (Block block : fragile()) {
            for (StructureTemplate.StructureBlockInfo info
                    : this.template.filterBlocks(this.templatePosition, this.placeSettings, block)) {
                BlockPos at = info.pos();
                if (chunk.isInside(at) && level.getBlockState(at) != info.state()) {
                    level.setBlock(at, info.state(), 2);
                }
            }
        }
    }

    private void residents(WorldGenLevel level, BoundingBox chunk, RandomSource random) {
        List<StructureTemplate.Palette> palettes = ((StructureTemplateAccessor) this.template).jojoha$palettes();
        if (stock.residents() <= 0 || palettes.isEmpty()) {
            return;
        }
        List<BlockPos> doors = new ArrayList<>();
        int lowest = Integer.MAX_VALUE;
        for (StructureTemplate.StructureBlockInfo info : palettes.get(0).blocks()) {
            if (info.state().getBlock() instanceof DoorBlock
                    && info.state().getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER) {
                if (info.pos().getY() < lowest) {
                    lowest = info.pos().getY();
                    doors.clear();
                }
                if (info.pos().getY() == lowest) {
                    doors.add(info.pos());
                }
            }
        }
        int left = stock.residents();
        for (BlockPos door : doors) {
            if (left <= 0) {
                return;
            }
            for (BlockPos local : List.of(door.south(), door.north())) {
                BlockPos at = this.templatePosition.offset(
                        StructureTemplate.calculateRelativePosition(this.placeSettings, local));
                if (chunk.isInside(at) && level.getBlockState(at).isAir() && level.getBlockState(at.above()).isAir()) {
                    Villager villager = EntityType.VILLAGER.create(level.getLevel());
                    if (villager == null) {
                        return;
                    }
                    villager.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, random.nextFloat() * 360F, 0F);
                    villager.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.STRUCTURE, null);
                    villager.setPersistenceRequired();
                    level.addFreshEntityWithPassengers(villager);
                    left--;
                    break;
                }
            }
        }
    }

    @Override
    protected void handleDataMarker(String name, BlockPos pos, ServerLevelAccessor level, RandomSource random,
                                    BoundingBox box) {
    }
}
