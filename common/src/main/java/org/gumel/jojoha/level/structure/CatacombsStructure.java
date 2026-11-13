package org.gumel.jojoha.level.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.pools.DimensionPadding;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import org.gumel.jojoha.registry.ModRegistries;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class CatacombsStructure extends Structure {
    public static final MapCodec<CatacombsStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            settingsCodec(instance),
            StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(s -> s.startPool),
            Codec.intRange(0, 20).fieldOf("size").forGetter(s -> s.size),
            Codec.intRange(1, 128).fieldOf("max_distance_from_center").forGetter(s -> s.maxDistance)
    ).apply(instance, CatacombsStructure::new));

    private static final String GATE = "catacombs/entrance/gate";
    private static final String DESCENT = "catacombs/entrance/descent";
    private static final String VAULT = "catacombs/vaults/";
    private static final String BOSS = "catacombs/rooms/boss";
    private static final String ROOM = "catacombs/rooms/";
    private static final int MIN_ROOMS = 6;
    private static final int MIN_KINDS = 4;
    private static final int KIND_BONUS = 12;
    private static final int ARENA_DEPTH = 26;
    private static final int SURVEY_STEP = 16;
    private static final int SURVEY_RADIUS = 4;
    private static final int RELIEF = 14;
    private static final int RELIEF_RADIUS = 40;
    private static final int RELIEF_POINTS = 8;
    private static final int SHIFT_STEP = 16;
    private static final int SHIFT_RADIUS = 2;
    private static final int SURVEY_SLACK = 4;
    private static final int CHECKED = 4;
    private static final int FLATNESS = 6;
    private static final int SINK = 4;
    private static final int DESCENT_COVER = 1;
    private static final int BOSS_COVER = 1;
    private static final int COVER = 4;
    private static final int GRID = 5;
    private static final int COARSE = 16;
    private static final int COARSE_MARGIN = 10;
    private static final int MIN_VAULTS = 2;
    private static final int LARGE = 36;
    private static final int LAYOUTS = 6;
    private static final int RESEED_TRIES = 48;
    private static final long LAYOUT_STEP = 0xBF58476D1CE4E5B9L;
    private static final int FOUNDATION = 24;
    private static final int BLEND = 10;
    private static final int SPARED = 4;
    private static final int FRAY = 6;
    private static final double FRAY_SCALE = 0.17;
    private static final double FRAY_JITTER = 0.45;
    private static final double FRAYED = 0.5;
    private static final long FRAY_SALT = 0x6A09E667F3BCC909L;
    private static final int TREE_REACH = 48;
    private static final int SEEDS = 4;
    private static final long SEED_STEP = 0x9E3779B97F4A7C15L;

    private final Holder<StructureTemplatePool> startPool;
    private final int size;
    private final int maxDistance;
    private final Map<Rotation, Entrance> entrances = new ConcurrentHashMap<>();

    public CatacombsStructure(StructureSettings settings, Holder<StructureTemplatePool> startPool, int size, int maxDistance) {
        super(settings);
        this.startPool = startPool;
        this.size = size;
        this.maxDistance = maxDistance;
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        Heights heights = new Heights(context);
        int middleX = chunk.getMiddleBlockX(), middleZ = chunk.getMiddleBlockZ();
        int middle = heights.top(middleX, middleZ);
        if (!context.validBiome().test(context.biomeSource().getNoiseBiome(QuartPos.fromBlock(middleX), QuartPos.fromBlock(middle),
                QuartPos.fromBlock(middleZ), context.randomState().sampler()))) {
            return Optional.empty();
        }
        int low = middle, high = middle;
        for (int i = 0; i < RELIEF_POINTS; i++) {
            double angle = Math.PI * 2 * i / RELIEF_POINTS;
            int h = heights.top(middleX + (int) (Math.cos(angle) * RELIEF_RADIUS), middleZ + (int) (Math.sin(angle) * RELIEF_RADIUS));
            low = Math.min(low, h);
            high = Math.max(high, h);
        }
        if (high - low < RELIEF) {
            return Optional.empty();
        }
        Survey survey = new Survey(heights, middleX, middleZ);

        List<Option> options = new ArrayList<>();
        Set<Rotation> tried = EnumSet.noneOf(Rotation.class);
        for (int seed = 0; seed < SEEDS; seed++) {
            long attempt = context.seed() + seed * SEED_STEP;
            Rotation rotation = Rotation.getRandom(reseed(context, attempt).random());
            if (!tried.add(rotation)) {
                continue;
            }
            Entrance shape = entrances.get(rotation);
            if (shape == null) {
                shape = entrance(context, attempt, new BlockPos(middleX, middle - ARENA_DEPTH, middleZ));
                if (shape == null) {
                    continue;
                }
                entrances.put(rotation, shape);
            }
            for (int sx = -SHIFT_RADIUS; sx <= SHIFT_RADIUS; sx++) {
                for (int sz = -SHIFT_RADIUS; sz <= SHIFT_RADIUS; sz++) {
                    BlockPos start = new BlockPos(middleX + sx * SHIFT_STEP, middle - ARENA_DEPTH, middleZ + sz * SHIFT_STEP);
                    Placed placed = shape.at(start);
                    int roof = placed.descent.maxY() - placed.gate.minY() + DESCENT_COVER;
                    int rise = survey.lowest(placed.descent) - survey.at(placed.front());
                    if (rise + SINK + SURVEY_SLACK >= roof) {
                        options.add(new Option(attempt, start, placed, roof - rise));
                    }
                }
            }
        }
        options.sort(Comparator.comparingInt(Option::shortfall));

        List<PoolElementStructurePiece> best = null;
        int bestVaults = 0;
        boolean bestBoss = false;
        int bestScore = -1;
        int checked = 0;
        search:
        for (Option option : options) {
            if (checked++ >= CHECKED) {
                break;
            }
            Integer shift = settle(heights, option.placed);
            if (shift == null) {
                continue;
            }
            Rotation rotation = Rotation.getRandom(reseed(context, option.attempt).random());
            for (long attempt : layoutSeeds(context, option.attempt, rotation)) {
                List<PoolElementStructurePiece> all = layout(reseed(context, attempt), option.start, size);
                PoolElementStructurePiece gate = find(all, GATE);
                PoolElementStructurePiece descent = find(all, DESCENT);
                if (gate == null || descent == null || !gate.getBoundingBox().equals(option.placed.gate)) {
                    continue;
                }
                for (PoolElementStructurePiece piece : all) {
                    piece.move(0, shift, 0);
                }
                List<PoolElementStructurePiece> kept = keep(heights, all, gate, descent);
                int vaults = count(kept, VAULT);
                if (vaults == 0) {
                    continue;
                }
                if (count(kept, BOSS) == 0) {
                    continue;
                }
                int kinds = kinds(kept);
                if (count(kept, ROOM) + vaults < MIN_ROOMS || kinds < MIN_KINDS) {
                    continue;
                }
                boolean boss = count(kept, BOSS) > 0;
                int score = kept.size() + kinds * KIND_BONUS + Math.min(vaults, MIN_VAULTS) * LARGE + (boss ? LARGE * 2 : 0);
                if (score > bestScore) {
                    best = kept;
                    bestVaults = vaults;
                    bestBoss = boss;
                    bestScore = score;
                }
                if (bestBoss && bestVaults >= MIN_VAULTS && best.size() >= LARGE) {
                    break search;
                }
            }
        }
        if (best == null) {
            return Optional.empty();
        }
        List<PoolElementStructurePiece> chosen = best;
        BoundingBox gateBox = find(chosen, GATE).getBoundingBox();
        BlockPos at = new BlockPos(gateBox.getCenter().getX(), gateBox.minY() + 1, gateBox.getCenter().getZ());
        return Optional.of(new GenerationStub(at, builder -> chosen.forEach(builder::addPiece)));
    }

    private Entrance entrance(GenerationContext context, long attempt, BlockPos start) {
        List<PoolElementStructurePiece> probe = layout(reseed(context, attempt), start, 2);
        PoolElementStructurePiece gate = find(probe, GATE);
        PoolElementStructurePiece descent = find(probe, DESCENT);
        if (gate == null || descent == null) {
            return null;
        }
        int x = -start.getX(), y = -start.getY(), z = -start.getZ();
        return new Entrance(gate.getBoundingBox().moved(x, y, z), descent.getBoundingBox().moved(x, y, z),
                probe.get(0).getBoundingBox().moved(x, y, z));
    }

    private record Entrance(BoundingBox gate, BoundingBox descent, BoundingBox arena) {
        private Placed at(BlockPos start) {
            return new Placed(gate.moved(start.getX(), start.getY(), start.getZ()),
                    descent.moved(start.getX(), start.getY(), start.getZ()),
                    arena.moved(start.getX(), start.getY(), start.getZ()));
        }
    }

    private record Placed(BoundingBox gate, BoundingBox descent, BoundingBox arena) {
        private boolean alongX() {
            return Math.abs(gate.getCenter().getX() - descent.getCenter().getX()) > Math.abs(gate.getCenter().getZ() - descent.getCenter().getZ());
        }

        private int sign() {
            int d = alongX() ? gate.getCenter().getX() - descent.getCenter().getX() : gate.getCenter().getZ() - descent.getCenter().getZ();
            return d >= 0 ? 1 : -1;
        }

        private int[] front() {
            return frontPoint(4, 1, 2);
        }

        private int[] frontPoint(int depthOver, int acrossNumerator, int acrossOver) {
            boolean alongX = alongX();
            int sign = sign();
            int span = alongX ? gate.getXSpan() : gate.getZSpan();
            int width = alongX ? gate.getZSpan() : gate.getXSpan();
            int edge = alongX ? (sign > 0 ? gate.maxX() : gate.minX()) : (sign > 0 ? gate.maxZ() : gate.minZ());
            int side = alongX ? gate.minZ() : gate.minX();
            int a = edge - sign * span / depthOver;
            int b = side + width * acrossNumerator / acrossOver;
            return alongX ? new int[]{a, b} : new int[]{b, a};
        }
    }

    private record Option(long attempt, BlockPos start, Placed placed, int shortfall) {
    }

    private static List<Long> layoutSeeds(GenerationContext context, long first, Rotation rotation) {
        List<Long> seeds = new ArrayList<>();
        seeds.add(first);
        for (int i = 1; i <= RESEED_TRIES && seeds.size() < LAYOUTS; i++) {
            long attempt = first + i * LAYOUT_STEP;
            if (Rotation.getRandom(reseed(context, attempt).random()) == rotation) {
                seeds.add(attempt);
            }
        }
        return seeds;
    }

    private List<PoolElementStructurePiece> layout(GenerationContext context, BlockPos start, int depth) {
        List<PoolElementStructurePiece> pieces = new ArrayList<>();
        JigsawPlacement.addPieces(context, startPool, Optional.empty(), depth, start, false, Optional.empty(), maxDistance,
                        PoolAliasLookup.EMPTY, DimensionPadding.ZERO, LiquidSettings.IGNORE_WATERLOGGING)
                .flatMap(stub -> stub.generator().left())
                .ifPresent(generator -> generator.accept(new StructurePiecesBuilder() {
                    @Override
                    public void addPiece(StructurePiece piece) {
                        if (piece instanceof PoolElementStructurePiece pooled) {
                            pieces.add(pooled);
                        }
                    }
                }));
        return pieces;
    }

    private static GenerationContext reseed(GenerationContext context, long seed) {
        return new GenerationContext(context.registryAccess(), context.chunkGenerator(), context.biomeSource(), context.randomState(),
                context.structureTemplateManager(), seed, context.chunkPos(), context.heightAccessor(), context.validBiome());
    }

    private static PoolElementStructurePiece find(List<? extends StructurePiece> pieces, String name) {
        for (StructurePiece piece : pieces) {
            if (piece instanceof PoolElementStructurePiece pooled && pooled.getElement().toString().contains(name)) {
                return pooled;
            }
        }
        return null;
    }

    private static int count(List<PoolElementStructurePiece> pieces, String name) {
        int n = 0;
        for (PoolElementStructurePiece piece : pieces) {
            if (piece.getElement().toString().contains(name)) {
                n++;
            }
        }
        return n;
    }

    private static int kinds(List<PoolElementStructurePiece> pieces) {
        Set<String> kinds = new HashSet<>();
        for (PoolElementStructurePiece piece : pieces) {
            String name = piece.getElement().toString();
            if (name.contains(ROOM) || name.contains(VAULT)) {
                kinds.add(name);
            }
        }
        return kinds.size();
    }

    private static Integer settle(Heights heights, Placed placed) {
        int[] front = new int[6];
        int n = 0;
        for (int depthOver : new int[]{6, 3}) {
            for (int across : new int[]{1, 3, 5}) {
                int[] at = placed.frontPoint(depthOver, across, 6);
                int top = heights.top(at[0], at[1]);
                if (top != heights.surface(at[0], at[1])) {
                    return null;
                }
                front[n++] = top;
            }
        }
        Arrays.sort(front);
        if (front[front.length - 1] - front[0] > FLATNESS) {
            return null;
        }
        int level = front[front.length / 2] - 1;
        int roofAbove = placed.descent.maxY() - placed.gate.minY() + DESCENT_COVER;
        int arenaAbove = placed.arena.maxY() - placed.gate.minY() + 1;
        int ground = Math.min(level, Math.min(heights.lowest(placed.descent, GRID) - 1 - roofAbove,
                heights.lowest(placed.arena, GRID) - 1 - arenaAbove));
        if (level - ground > SINK) {
            return null;
        }
        return ground - placed.gate.minY();
    }

    private static List<PoolElementStructurePiece> keep(Heights heights, List<PoolElementStructurePiece> all,
                                                       PoolElementStructurePiece gate, PoolElementStructurePiece descent) {
        PoolElementStructurePiece arena = all.get(0);
        int floor = heights.context.heightAccessor().getMinBuildHeight() + 2;
        List<PoolElementStructurePiece> buried = new ArrayList<>();
        for (PoolElementStructurePiece piece : all) {
            if (piece == arena || piece == gate || piece == descent) {
                buried.add(piece);
                continue;
            }
            BoundingBox box = piece.getBoundingBox();
            if (box.minY() < floor) {
                continue;
            }
            int need = box.maxY() + (piece.getElement().toString().contains(BOSS) ? BOSS_COVER : COVER) + 1;
            if (heights.lowest(box, COARSE) >= need + COARSE_MARGIN || heights.lowest(box, GRID) >= need) {
                buried.add(piece);
            }
        }

        Set<PoolElementStructurePiece> reached = new HashSet<>();
        Set<PoolElementStructurePiece> spare = new HashSet<>();
        ArrayDeque<PoolElementStructurePiece> queue = new ArrayDeque<>();
        reached.add(arena);
        queue.add(arena);
        boolean boss = false;
        while (!queue.isEmpty()) {
            BoundingBox near = queue.poll().getBoundingBox().inflatedBy(1);
            for (PoolElementStructurePiece other : buried) {
                if (reached.contains(other) || spare.contains(other) || !near.intersects(other.getBoundingBox())) {
                    continue;
                }
                if (other.getElement().toString().contains(BOSS)) {
                    if (boss) {
                        spare.add(other);
                        continue;
                    }
                    boss = true;
                }
                reached.add(other);
                queue.add(other);
            }
        }
        List<PoolElementStructurePiece> kept = new ArrayList<>();
        for (PoolElementStructurePiece piece : buried) {
            if (reached.contains(piece)) {
                kept.add(piece);
            }
        }
        return kept;
    }

    @Override
    public BoundingBox adjustBoundingBox(BoundingBox box) {
        return box.inflatedBy(BLEND + 1, 0, BLEND + 1);
    }

    @Override
    public void afterPlace(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random,
                           BoundingBox chunkBox, ChunkPos chunkPos, PiecesContainer pieces) {
        PoolElementStructurePiece gate = find(pieces.pieces(), GATE);
        if (gate == null) {
            return;
        }
        BoundingBox box = gate.getBoundingBox();
        int ground = box.minY();
        PoolElementStructurePiece descent = find(pieces.pieces(), DESCENT);
        BoundingBox spared = descent == null ? null : descent.getBoundingBox().inflatedBy(SPARED, 0, SPARED);
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        SimplexNoise noise = new SimplexNoise(new LegacyRandomSource(level.getSeed() ^ FRAY_SALT));
        for (int x = Math.max(box.minX() - BLEND, chunkBox.minX()); x <= Math.min(box.maxX() + BLEND, chunkBox.maxX()); x++) {
            for (int z = Math.max(box.minZ() - BLEND, chunkBox.minZ()); z <= Math.min(box.maxZ() + BLEND, chunkBox.maxZ()); z++) {
                double reach = distance(box, x, z);
                if (reach > BLEND) {
                    continue;
                }
                boolean kept = spared != null && x >= spared.minX() && x <= spared.maxX() && z >= spared.minZ() && z <= spared.maxZ();
                if (reach == 0) {
                    underpin(level, at, x, z, ground);
                    if (!kept) {
                        fray(level, at, box, x, z, ground, noise);
                    }
                    continue;
                }
                if (kept) {
                    continue;
                }
                blend(level, at, x, z, ground, reach);
            }
        }
    }

    private static double distance(BoundingBox box, int x, int z) {
        int ox = Math.max(0, Math.max(box.minX() - x, x - box.maxX()));
        int oz = Math.max(0, Math.max(box.minZ() - z, z - box.maxZ()));
        return Math.sqrt(ox * ox + oz * oz);
    }

    private static void underpin(WorldGenLevel level, BlockPos.MutableBlockPos at, int x, int z, int ground) {
        if (level.getBlockState(at.set(x, ground, z)).isAir()) {
            return;
        }
        int bottom = ground - 1;
        while (bottom > ground - FOUNDATION && level.getBlockState(at.set(x, bottom, z)).canBeReplaced()) {
            bottom--;
        }
        BlockState found = level.getBlockState(at.set(x, bottom, z));
        if (found.canBeReplaced()) {
            return;
        }
        BlockState fill = soil(found)[1];
        for (int y = bottom + 1; y < ground; y++) {
            level.setBlock(at.set(x, y, z), fill, 2);
        }
    }

    private static void blend(WorldGenLevel level, BlockPos.MutableBlockPos at, int x, int z, int ground, double reach) {
        int surface = Integer.MIN_VALUE;
        for (int y = ground + TREE_REACH; y > ground - FOUNDATION; y--) {
            BlockState state = level.getBlockState(at.set(x, y, z));
            if (!state.canBeReplaced() && !state.is(BlockTags.LEAVES) && !state.is(BlockTags.LOGS)) {
                surface = y;
                break;
            }
        }
        if (surface == Integer.MIN_VALUE || !level.getFluidState(at.set(x, surface + 1, z)).isEmpty()) {
            return;
        }
        double t = reach / BLEND;
        int target = (int) Math.round(Mth.lerp(t * t * (3 - 2 * t), ground, surface));
        if (target <= surface) {
            return;
        }

        BlockState[] soil = soil(level, at, x, z, surface);
        if (soil[0] != soil[1] && level.getBlockState(at.set(x, surface, z)) == soil[0]) {
            level.setBlock(at, soil[1], 2);
        }
        for (int y = surface + 1; y < target; y++) {
            level.setBlock(at.set(x, y, z), soil[1], 2);
        }
        level.setBlock(at.set(x, target, z), soil[0], 2);
        if (level.getBlockState(at.set(x, target + 1, z)).canBeReplaced()) {
            level.setBlock(at, Blocks.AIR.defaultBlockState(), 2);
        }
    }

    private static BlockState[] soil(WorldGenLevel level, BlockPos.MutableBlockPos at, int x, int z, int surface) {
        BlockState top = level.getBlockState(at.set(x, surface, z));
        if (top.is(BlockTags.DIRT)) {
            BlockState under = level.getBlockState(at.set(x, surface - 1, z));
            return new BlockState[]{top, under.is(BlockTags.DIRT) && under != top ? under : Blocks.DIRT.defaultBlockState()};
        }
        return soil(top);
    }

    private static boolean loose(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.GRAVEL) || state.is(Blocks.MOSS_BLOCK)
                || state.is(Blocks.MOSSY_COBBLESTONE) || state.is(BlockTags.DIRT);
    }

    private static void fray(WorldGenLevel level, BlockPos.MutableBlockPos at, BoundingBox box, int x, int z, int ground,
                             SimplexNoise noise) {
        int west = x - box.minX(), east = box.maxX() - x, north = z - box.minZ(), south = box.maxZ() - z;
        int inset = Math.min(Math.min(west, east), Math.min(north, south));
        if (inset >= FRAY) {
            return;
        }
        double edge = 1 - (inset + 0.5) / FRAY;
        if (edge + noise.getValue(x * FRAY_SCALE, z * FRAY_SCALE) * FRAY_JITTER < FRAYED) {
            return;
        }
        if (!loose(level.getBlockState(at.set(x, ground, z))) || !level.getBlockState(at.set(x, ground + 1, z)).canBeReplaced()) {
            return;
        }
        int ox = inset == west ? box.minX() - 2 : inset == east ? box.maxX() + 2 : x;
        int oz = inset == west || inset == east ? z : inset == north ? box.minZ() - 2 : box.maxZ() + 2;
        int outside = Integer.MIN_VALUE;
        for (int y = ground + TREE_REACH; y > ground - FOUNDATION; y--) {
            BlockState state = level.getBlockState(at.set(ox, y, oz));
            if (!state.canBeReplaced() && !state.is(BlockTags.LEAVES) && !state.is(BlockTags.LOGS)) {
                outside = y;
                break;
            }
        }
        if (outside == Integer.MIN_VALUE) {
            return;
        }
        if (level.getFluidState(at.set(ox, outside + 1, oz)).isEmpty()) {
            level.setBlock(at.set(x, ground, z), soil(level, at, ox, oz, outside)[0], 2);
            return;
        }
        int water = outside + 1;
        while (water < ground + TREE_REACH && !level.getFluidState(at.set(ox, water + 1, oz)).isEmpty()) {
            water++;
        }
        BlockState fluid = level.getBlockState(at.set(ox, water, oz)).getFluidState().createLegacyBlock();
        for (int y = ground + 1; y > outside; y--) {
            level.setBlock(at.set(x, y, z), y > water ? Blocks.AIR.defaultBlockState() : fluid, 2);
        }
    }

    private static BlockState[] soil(BlockState found) {
        if (found.is(BlockTags.SAND)) {
            BlockState sand = found.getBlock().defaultBlockState();
            return new BlockState[]{sand, sand};
        }
        if (found.is(BlockTags.BASE_STONE_OVERWORLD) || found.is(BlockTags.TERRACOTTA) || found.is(Blocks.SNOW_BLOCK)
                || found.is(Blocks.GRAVEL)) {
            BlockState stone = found.getBlock().defaultBlockState();
            return new BlockState[]{stone, stone};
        }
        return new BlockState[]{Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.DIRT.defaultBlockState()};
    }

    private static final class Survey {
        private final int x0, z0;
        private final int[][] grid;

        private Survey(Heights heights, int middleX, int middleZ) {
            int cells = SURVEY_RADIUS * 2 + 1;
            x0 = middleX - SURVEY_RADIUS * SURVEY_STEP - SHIFT_RADIUS * SHIFT_STEP;
            z0 = middleZ - SURVEY_RADIUS * SURVEY_STEP - SHIFT_RADIUS * SHIFT_STEP;
            int span = cells + SHIFT_RADIUS * 2 * SHIFT_STEP / SURVEY_STEP;
            grid = new int[span][span];
            for (int i = 0; i < span; i++) {
                for (int j = 0; j < span; j++) {
                    grid[i][j] = heights.top(x0 + i * SURVEY_STEP, z0 + j * SURVEY_STEP);
                }
            }
        }

        private int at(int[] point) {
            return at(point[0], point[1]);
        }

        private int at(int x, int z) {
            double fx = Mth.clamp((x - x0) / (double) SURVEY_STEP, 0, grid.length - 1.001);
            double fz = Mth.clamp((z - z0) / (double) SURVEY_STEP, 0, grid.length - 1.001);
            int i = (int) fx, j = (int) fz;
            double u = fx - i, v = fz - j;
            return (int) Math.round(Mth.lerp2(u, v, grid[i][j], grid[i + 1][j], grid[i][j + 1], grid[i + 1][j + 1]));
        }

        private int lowest(BoundingBox box) {
            int low = at(box.getCenter().getX(), box.getCenter().getZ());
            low = Math.min(low, at(box.minX(), box.minZ()));
            low = Math.min(low, at(box.maxX(), box.minZ()));
            low = Math.min(low, at(box.minX(), box.maxZ()));
            return Math.min(low, at(box.maxX(), box.maxZ()));
        }
    }

    private static final class Heights {
        private final GenerationContext context;
        private final Map<Long, Integer> tops = new HashMap<>();
        private final Map<Long, Integer> surfaces = new HashMap<>();

        private Heights(GenerationContext context) {
            this.context = context;
        }

        private int lowest(BoundingBox box, int step) {
            int low = Integer.MAX_VALUE;
            for (int x = Math.floorDiv(box.minX(), step) * step; x <= box.maxX() + step; x += step) {
                for (int z = Math.floorDiv(box.minZ(), step) * step; z <= box.maxZ() + step; z += step) {
                    low = Math.min(low, top(x, z));
                }
            }
            return low;
        }

        private int top(int x, int z) {
            return tops.computeIfAbsent(BlockPos.asLong(x, 0, z), key -> base(x, z, Heightmap.Types.OCEAN_FLOOR_WG));
        }

        private int surface(int x, int z) {
            return surfaces.computeIfAbsent(BlockPos.asLong(x, 0, z), key -> base(x, z, Heightmap.Types.WORLD_SURFACE_WG));
        }

        private int base(int x, int z, Heightmap.Types type) {
            return context.chunkGenerator().getBaseHeight(x, z, type, context.heightAccessor(), context.randomState());
        }
    }

    @Override
    public StructureType<?> type() {
        return ModRegistries.CATACOMBS_STRUCTURE.get();
    }
}
