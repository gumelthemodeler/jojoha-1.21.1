package org.gumel.jojoha.level.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.loot.LootTable;
import org.gumel.jojoha.registry.ModRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class OgreStreetStructure extends Structure {
    public static final MapCodec<OgreStreetStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            settingsCodec(instance),
            ResourceLocation.CODEC.listOf().fieldOf("homes").forGetter(structure -> structure.homes),
            ResourceLocation.CODEC.listOf().fieldOf("shops").forGetter(structure -> structure.shops),
            ResourceLocation.CODEC.listOf().fieldOf("squares").forGetter(structure -> structure.squares),
            ResourceLocation.CODEC.fieldOf("lamp").forGetter(structure -> structure.lamp),
            Codec.intRange(1, 40).optionalFieldOf("min_buildings", 22).forGetter(structure -> structure.minBuildings),
            Codec.intRange(1, 40).optionalFieldOf("max_buildings", 36).forGetter(structure -> structure.maxBuildings),
            ResourceKey.codec(Registries.LOOT_TABLE).optionalFieldOf("chest_loot", SurfaceTemplatePiece.CHEST_LOOT)
                    .forGetter(structure -> structure.chest),
            ResourceKey.codec(Registries.LOOT_TABLE).optionalFieldOf("barrel_loot", SurfaceTemplatePiece.BARREL_LOOT)
                    .forGetter(structure -> structure.barrel),
            StructureAvoid.CODEC.listOf().optionalFieldOf("avoid", List.of()).forGetter(structure -> structure.avoid)
    ).apply(instance, OgreStreetStructure::new));

    private static final int[][] DIRS = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}};
    private static final int HALF_ROAD = 2;
    private static final int RUNS_MIN = 4;
    private static final int RUNS_MAX = 5;
    private static final int RUN_MIN = 30;
    private static final int RUN_MAX = 40;
    private static final int BRANCH_RUNS = 2;
    private static final int BRANCH_MIN = 28;
    private static final int BRANCH_MAX = 44;
    private static final int BRANCH_MARGIN = 12;
    private static final int BRANCH_CLEAR = 16;
    private static final int BRANCH_KEEP = 20;
    private static final int BRANCH_TRIES = 5;
    private static final int BRANCHES_MAX = 10;
    private static final int BRANCH_DEPTH = 2;
    private static final float BRANCH_CHANCE = 0.9F;
    private static final float SUB_BRANCH_CHANCE = 0.6F;
    private static final int FITS = 4;
    private static final int PLOT_MIN = 6;
    private static final int PLOT_MAX = 10;
    private static final int PLOT_DEPTH = 7;
    private static final int PLOTS_MAX = 5;
    private static final float PLOT_CHANCE = 0.6F;
    private static final float JOG_CHANCE = 0.4F;
    private static final int JOG_MIN = 2;
    private static final int JOG_MAX = 4;
    private static final int REACH = 112;
    private static final int FLAT = 5;
    private static final int RUN_RISE = 3;
    private static final int TRIM_SHARE = 10;
    private static final int WET_SHARE = 10;
    private static final int SIDE_EVERY = 4;
    private static final int[] SIDE_REACH = {HALF_ROAD + 3, HALF_ROAD + 8};
    private static final int BUILD_FIT = 3;
    private static final int MAX_CUT = 6;
    private static final int MIN_PATH = 40;
    private static final int PLAZA_MARGIN = 2;
    private static final int LAMP_EVERY = 16;
    private static final float SHOP_CHANCE = 0.4F;
    private static final float ALLEY_CHANCE = 0.1F;
    private static final int CORE = HALF_ROAD * HALF_ROAD + HALF_ROAD;
    private static final int FRAY = 2;
    private static final float STRAY = 0.08F;
    private static final double WOBBLE_SCALE = 0.11;
    private static final double PLAZA_WOBBLE = 1.5;
    private static final long WOBBLE_SALT = 0x3C6EF372FE94F82BL;
    private static final int RESIDENTS = 1;
    private static final int ATTEMPTS = 4;

    private final List<ResourceLocation> homes;
    private final List<ResourceLocation> shops;
    private final List<ResourceLocation> squares;
    private final ResourceLocation lamp;
    private final int minBuildings;
    private final int maxBuildings;
    private final ResourceKey<LootTable> chest;
    private final ResourceKey<LootTable> barrel;
    private final List<StructureAvoid> avoid;

    public OgreStreetStructure(StructureSettings settings, List<ResourceLocation> homes, List<ResourceLocation> shops,
                               List<ResourceLocation> squares, ResourceLocation lamp, int minBuildings,
                               int maxBuildings, ResourceKey<LootTable> chest, ResourceKey<LootTable> barrel,
                               List<StructureAvoid> avoid) {
        super(settings);
        this.homes = homes;
        this.shops = shops;
        this.squares = squares;
        this.lamp = lamp;
        this.minBuildings = minBuildings;
        this.maxBuildings = maxBuildings;
        this.chest = chest;
        this.barrel = barrel;
        this.avoid = avoid;
    }

    private record Run(int from, int to, int dir, boolean lined) {
    }

    private static final class Street {
        private final List<int[]> path = new ArrayList<>();
        private final List<Integer> dirs = new ArrayList<>();
        private final List<Run> runs = new ArrayList<>();
        private final List<Integer> bends = new ArrayList<>();
        private int[] h = new int[0];
        private int n;
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        if (StructureAvoid.near(context, avoid)) {
            return Optional.empty();
        }
        ChunkPos chunk = context.chunkPos();
        int middleX = chunk.getMiddleBlockX();
        int middleZ = chunk.getMiddleBlockZ();
        Ground ground = new Ground(context);
        int middle = ground.surface(middleX, middleZ);
        if (!context.validBiome().test(context.biomeSource().getNoiseBiome(QuartPos.fromBlock(middleX),
                QuartPos.fromBlock(middle), QuartPos.fromBlock(middleZ), context.randomState().sampler()))) {
            return Optional.empty();
        }
        for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
            Optional<GenerationStub> street = layout(context, ground);
            if (street.isPresent()) {
                return street;
            }
        }
        return Optional.empty();
    }

    private Optional<GenerationStub> layout(GenerationContext context, Ground land) {
        RandomSource random = context.random();
        ChunkPos chunk = context.chunkPos();
        int startX = chunk.getMiddleBlockX();
        int startZ = chunk.getMiddleBlockZ();

        Street trunk = new Street();
        walk(random, trunk, startX, startZ, startX, startZ, random.nextInt(4),
                RUNS_MIN + random.nextInt(RUNS_MAX - RUNS_MIN + 1), RUN_MIN, RUN_MAX, true);
        settle(land, trunk, Integer.MIN_VALUE);
        if (trunk.n < MIN_PATH) {
            return Optional.empty();
        }
        int[] h = trunk.h;
        int n = trunk.n;

        SimplexNoise wobble = new SimplexNoise(new LegacyRandomSource(context.seed() ^ WOBBLE_SALT));
        Map<Long, Integer> road = new LinkedHashMap<>();
        Map<Long, Integer> kinds = new HashMap<>();
        pave(road, trunk);

        StructureTemplateManager templates = context.structureTemplateManager();
        List<StructurePiece> pieces = new ArrayList<>();
        List<int[]> taken = new ArrayList<>();
        SurfaceTemplatePiece.Stock open = new SurfaceTemplatePiece.Stock(chest, barrel, List.of(), 0);
        SurfaceTemplatePiece.Stock lived = new SurfaceTemplatePiece.Stock(chest, barrel, List.of(), RESIDENTS);

        int squareTurn = random.nextInt(Math.max(1, squares.size()));
        List<Integer> plazas = new ArrayList<>();
        for (int bend : trunk.bends) {
            if (bend < n) {
                plazas.add(bend);
            }
        }
        if (plazas.isEmpty()) {
            plazas.add(n - 1);
        }
        for (int at : plazas) {
            if (squares.isEmpty()) {
                break;
            }
            ResourceLocation square = squares.get(squareTurn++ % squares.size());
            Vec3i size = templates.getOrCreate(square).getSize();
            int half = Math.max(size.getX(), size.getZ()) / 2 + PLAZA_MARGIN + 1;
            int px = trunk.path.get(at)[0];
            int pz = trunk.path.get(at)[1];
            int span = half + (int) Math.ceil(PLAZA_WOBBLE);
            for (int dx = -span; dx <= span; dx++) {
                for (int dz = -span; dz <= span; dz++) {
                    double rim = half + wobble.getValue((px + dx) * WOBBLE_SCALE, (pz + dz) * WOBBLE_SCALE) * PLAZA_WOBBLE;
                    double reach = Math.sqrt(dx * dx + dz * dz);
                    if (reach > rim) {
                        continue;
                    }
                    long key = GroundFit.key(px + dx, pz + dz);
                    if (!road.containsKey(key) && reach > rim - 1) {
                        kinds.put(key, OgreRoadPiece.EDGE);
                    }
                    road.put(key, h[at]);
                }
            }
            Rotation rotation = Rotation.getRandom(random);
            Vec3i turned = size.getX() == size.getZ() || rotation == Rotation.NONE || rotation == Rotation.CLOCKWISE_180
                    ? size : new Vec3i(size.getZ(), size.getY(), size.getX());
            int minX = px - turned.getX() / 2;
            int minZ = pz - turned.getZ() / 2;
            pieces.add(piece(templates, square, rotation, minX, minZ, h[at], open));
        }

        List<Street> streets = new ArrayList<>();
        streets.add(trunk);
        sprout(random, land, road, streets, trunk, 0, startX, startZ);

        List<ResourceLocation> shopsLeft = new ArrayList<>(shops);
        int built = 0;
        for (Street street : streets) {
            for (Run run : street.runs) {
                if (!run.lined || run.from >= street.n || built >= maxBuildings) {
                    continue;
                }
                int end = Math.min(run.to, street.n - 1) - HALF_ROAD - 1;
                for (int side : new int[]{1, -1}) {
                    int[] out = DIRS[Math.floorMod(run.dir + side, 4)];
                    Rotation rotation = facing(out);
                    int cursor = run.from + HALF_ROAD + 1;
                    int sinceLamp = random.nextInt(LAMP_EVERY / 2);
                    while (cursor < end && built < maxBuildings) {
                        boolean lit = sinceLamp >= LAMP_EVERY;
                        boolean placed = false;
                        for (int fit = 0; fit < (lit ? 1 : FITS) && !placed; fit++) {
                            ResourceLocation pick;
                            boolean shop = false;
                            if (lit) {
                                pick = lamp;
                            } else if (fit == 0 && !shopsLeft.isEmpty() && random.nextFloat() < SHOP_CHANCE) {
                                pick = shopsLeft.get(random.nextInt(shopsLeft.size()));
                                shop = true;
                            } else {
                                pick = homes.get(random.nextInt(homes.size()));
                            }
                            Vec3i size = templates.getOrCreate(pick).getSize();
                            int width = size.getX();
                            if (cursor + width - 1 > end) {
                                continue;
                            }
                            int[] box = lot(street, cursor, width, size.getZ(), out);
                            int y = street.h[cursor + width / 2];
                            if (!free(box, taken, road, startX, startZ) || misfit(land, box, y)) {
                                continue;
                            }
                            pieces.add(piece(templates, pick, rotation, box[0], box[1], y, lit ? open : lived));
                            taken.add(box);
                            placed = true;
                            cursor += width;
                            if (lit) {
                                sinceLamp = 0;
                            } else {
                                sinceLamp += width;
                                built++;
                                if (shop) {
                                    shopsLeft.remove(pick);
                                }
                                if (random.nextFloat() < ALLEY_CHANCE) {
                                    cursor += 1 + random.nextInt(2);
                                }
                            }
                        }
                        if (!placed) {
                            cursor++;
                            sinceLamp++;
                        }
                    }
                }
            }
        }
        if (built < minBuildings) {
            return Optional.empty();
        }

        int plots = 0;
        for (Street street : streets) {
            for (Run run : street.runs) {
                if (!run.lined || run.from >= street.n) {
                    continue;
                }
                int end = Math.min(run.to, street.n - 1) - HALF_ROAD - 1;
                for (int side : new int[]{1, -1}) {
                    int[] out = DIRS[Math.floorMod(run.dir + side, 4)];
                    for (int cursor = run.from + HALF_ROAD + 1; cursor < end && plots < PLOTS_MAX; cursor++) {
                        for (int width = PLOT_MAX; width >= PLOT_MIN; width--) {
                            if (cursor + width - 1 > end) {
                                continue;
                            }
                            int[] box = lot(street, cursor, width, PLOT_DEPTH, out);
                            int y = street.h[cursor + width / 2];
                            if (!free(box, taken, road, startX, startZ) || misfit(land, box, y)) {
                                continue;
                            }
                            if (random.nextFloat() < PLOT_CHANCE) {
                                plot(road, kinds, street, cursor, width, out, y,
                                        OgreRoadPiece.CROP + random.nextInt(OgreRoadPiece.CROPS));
                                taken.add(box);
                                plots++;
                            }
                            cursor += width;
                            break;
                        }
                    }
                }
            }
        }

        for (Street street : streets) {
            for (int i = 0; i < street.n; i++) {
                int[] at = street.path.get(i);
                int[] across = DIRS[Math.floorMod(street.dirs.get(i) + 1, 4)];
                for (int side : new int[]{1, -1}) {
                    int x = at[0] + across[0] * side * (HALF_ROAD + 1);
                    int z = at[1] + across[1] * side * (HALF_ROAD + 1);
                    double width = (wobble.getValue(x * WOBBLE_SCALE, z * WOBBLE_SCALE) + 1) * 0.5 * (FRAY + 1);
                    for (int step = 1; step <= FRAY && step <= width; step++) {
                        edge(road, kinds, taken, at, across, side * (HALF_ROAD + step), street.h[i],
                                step == 1 ? OgreRoadPiece.EDGE : OgreRoadPiece.FRINGE);
                    }
                    if (random.nextFloat() < STRAY) {
                        edge(road, kinds, taken, at, across, side * (HALF_ROAD + FRAY + 1), street.h[i],
                                OgreRoadPiece.FRINGE);
                    }
                }
            }
        }

        int[] xs = new int[road.size()];
        int[] ys = new int[road.size()];
        int[] zs = new int[road.size()];
        int[] ks = new int[road.size()];
        int i = 0;
        for (Map.Entry<Long, Integer> cell : road.entrySet()) {
            xs[i] = (int) (cell.getKey() >> 32);
            zs[i] = (int) (long) cell.getKey();
            ys[i] = cell.getValue();
            ks[i] = kinds.getOrDefault(cell.getKey(), OgreRoadPiece.ROAD);
            i++;
        }
        pieces.add(0, new OgreRoadPiece(xs, ys, zs, ks));
        BlockPos start = new BlockPos(startX, h[0], startZ);
        return Optional.of(new GenerationStub(start, builder -> pieces.forEach(builder::addPiece)));
    }

    private static void sprout(RandomSource random, Ground land, Map<Long, Integer> road, List<Street> streets,
                               Street parent, int depth, int originX, int originZ) {
        for (Run run : parent.runs) {
            int to = Math.min(run.to, parent.n - 1);
            if (!run.lined || run.from >= parent.n || to - run.from < 2 * BRANCH_MARGIN) {
                continue;
            }
            for (int side : new int[]{1, -1}) {
                if (streets.size() > BRANCHES_MAX
                        || random.nextFloat() > (depth == 0 ? BRANCH_CHANCE : SUB_BRANCH_CHANCE)) {
                    continue;
                }
                for (int attempt = 0; attempt < BRANCH_TRIES; attempt++) {
                    int at = run.from + BRANCH_MARGIN + random.nextInt(to - run.from - 2 * BRANCH_MARGIN + 1);
                    int[] fork = parent.path.get(at);
                    Street branch = new Street();
                    walk(random, branch, fork[0], fork[1], originX, originZ, Math.floorMod(run.dir + side, 4),
                            1 + random.nextInt(BRANCH_RUNS), BRANCH_MIN, BRANCH_MAX, false);
                    int keep = clear(branch, road);
                    if (keep < BRANCH_KEEP) {
                        continue;
                    }
                    branch.path.subList(keep, branch.path.size()).clear();
                    branch.dirs.subList(keep, branch.dirs.size()).clear();
                    settle(land, branch, parent.h[at]);
                    if (branch.n < BRANCH_KEEP) {
                        continue;
                    }
                    pave(road, branch);
                    streets.add(branch);
                    if (depth < BRANCH_DEPTH) {
                        sprout(random, land, road, streets, branch, depth + 1, originX, originZ);
                    }
                    break;
                }
            }
        }
    }

    private static int clear(Street branch, Map<Long, Integer> road) {
        int first = BRANCH_CLEAR + HALF_ROAD + 1;
        for (int j = first; j < branch.path.size(); j++) {
            int[] at = branch.path.get(j);
            int dir = branch.dirs.get(j);
            boolean whole = j == first || dir != branch.dirs.get(j - 1);
            for (int a = -BRANCH_CLEAR; a <= BRANCH_CLEAR; a++) {
                for (int b = whole ? -BRANCH_CLEAR : BRANCH_CLEAR; b <= BRANCH_CLEAR; b++) {
                    int x = at[0] + DIRS[dir][0] * b - DIRS[dir][1] * a;
                    int z = at[1] + DIRS[dir][1] * b + DIRS[dir][0] * a;
                    if (road.containsKey(GroundFit.key(x, z))) {
                        return j;
                    }
                }
            }
        }
        return branch.path.size();
    }

    private static void pave(Map<Long, Integer> road, Street street) {
        for (int i = 0; i < street.n; i++) {
            int[] at = street.path.get(i);
            for (int dx = -HALF_ROAD; dx <= HALF_ROAD; dx++) {
                for (int dz = -HALF_ROAD; dz <= HALF_ROAD; dz++) {
                    if (dx * dx + dz * dz <= CORE) {
                        road.putIfAbsent(GroundFit.key(at[0] + dx, at[1] + dz), street.h[i]);
                    }
                }
            }
        }
    }

    private static int[] lot(Street street, int cursor, int width, int depth, int[] out) {
        int[] a = street.path.get(cursor);
        int[] b = street.path.get(cursor + width - 1);
        int ax = a[0] + out[0] * (HALF_ROAD + 1);
        int az = a[1] + out[1] * (HALF_ROAD + 1);
        int bx = b[0] + out[0] * (HALF_ROAD + depth);
        int bz = b[1] + out[1] * (HALF_ROAD + depth);
        return new int[]{Math.min(ax, bx), Math.min(az, bz), Math.max(ax, bx), Math.max(az, bz)};
    }

    private static void plot(Map<Long, Integer> road, Map<Long, Integer> kinds, Street street, int cursor, int width,
                             int[] out, int y, int crop) {
        int channel = (PLOT_DEPTH + 1) / 2;
        for (int a = 0; a < width; a++) {
            int[] at = street.path.get(cursor + a);
            for (int k = 1; k <= PLOT_DEPTH; k++) {
                long key = GroundFit.key(at[0] + out[0] * (HALF_ROAD + k), at[1] + out[1] * (HALF_ROAD + k));
                boolean rim = a == 0 || a == width - 1 || k == 1 || k == PLOT_DEPTH;
                road.put(key, y);
                kinds.put(key, rim ? OgreRoadPiece.ROAD : k == channel ? OgreRoadPiece.WATER : crop);
            }
        }
    }

    private static void walk(RandomSource random, Street street, int x, int z, int originX, int originZ, int dir,
                             int count, int runMin, int runMax, boolean jogs) {
        int lastTurn = random.nextBoolean() ? 1 : 3;
        street.path.add(new int[]{x, z});
        street.dirs.add(dir);
        for (int r = 0; r < count; r++) {
            int from = street.path.size() - 1;
            int length = runMin + random.nextInt(runMax - runMin + 1);
            boolean whole = step(street, originX, originZ, dir, length);
            street.runs.add(new Run(from, street.path.size() - 1, dir, true));
            if (!whole || r == count - 1) {
                return;
            }
            if (jogs && random.nextFloat() < JOG_CHANCE) {
                int jogDir = Math.floorMod(dir + (random.nextBoolean() ? 1 : 3), 4);
                int jogFrom = street.path.size() - 1;
                if (!step(street, originX, originZ, jogDir, JOG_MIN + random.nextInt(JOG_MAX - JOG_MIN + 1))) {
                    return;
                }
                street.runs.add(new Run(jogFrom, street.path.size() - 1, jogDir, false));
            } else {
                street.bends.add(street.path.size() - 1);
                lastTurn = lastTurn == 1 ? 3 : 1;
                dir = Math.floorMod(dir + lastTurn, 4);
            }
        }
    }

    private static boolean step(Street street, int originX, int originZ, int dir, int length) {
        for (int i = 0; i < length; i++) {
            int[] last = street.path.get(street.path.size() - 1);
            int x = last[0] + DIRS[dir][0];
            int z = last[1] + DIRS[dir][1];
            if (Math.abs(x - originX) > REACH - HALF_ROAD || Math.abs(z - originZ) > REACH - HALF_ROAD) {
                return false;
            }
            street.path.add(new int[]{x, z});
            street.dirs.add(dir);
        }
        return true;
    }

    private static void settle(Ground land, Street street, int start) {
        int n = street.path.size();
        int[] ground = new int[n];
        for (int i = 0; i < n; i++) {
            int dry = land.dry(street.path.get(i)[0], street.path.get(i)[1]);
            if (dry == Ground.WET) {
                n = i;
                break;
            }
            ground[i] = dry;
        }
        int[] h = new int[street.path.size()];
        n = terrace(land, street, ground, h, n, start);
        for (int i = start == Integer.MIN_VALUE ? 0 : HALF_ROAD + 2; i < n; i++) {
            if (Math.abs(h[i] - ground[i]) > MAX_CUT) {
                n = i;
                break;
            }
        }
        street.h = h;
        street.n = n;
    }

    private static int terrace(Ground land, Street street, int[] ground, int[] h, int n, int start) {
        int level = start;
        int reached = 0;
        for (Run run : street.runs) {
            if (run.from >= n) {
                break;
            }
            int to = Math.min(run.to, n - 1);
            List<Integer> samples = new ArrayList<>();
            int wet = 0;
            for (int i = run.from; i <= to; i++) {
                samples.add(ground[i]);
                if ((i - run.from) % SIDE_EVERY != 0) {
                    continue;
                }
                int[] across = DIRS[Math.floorMod(street.dirs.get(i) + 1, 4)];
                for (int reach : SIDE_REACH) {
                    for (int side : new int[]{1, -1}) {
                        int x = street.path.get(i)[0] + across[0] * reach * side;
                        int z = street.path.get(i)[1] + across[1] * reach * side;
                        int dry = land.dry(x, z);
                        if (dry == Ground.WET) {
                            wet++;
                        } else {
                            samples.add(dry);
                        }
                    }
                }
            }
            samples.sort(null);
            int trim = samples.size() / TRIM_SHARE;
            int spread = samples.get(samples.size() - 1 - trim) - samples.get(trim);
            int median = samples.get(samples.size() / 2);
            if (spread > FLAT || wet * WET_SHARE > samples.size()
                    || level != Integer.MIN_VALUE && Math.abs(median - level) > RUN_RISE) {
                break;
            }
            level = median;
            for (int i = run.from; i <= to; i++) {
                h[i] = level;
            }
            reached = to + 1;
        }
        if (start != Integer.MIN_VALUE) {
            for (int i = 0; i < Math.min(reached, HALF_ROAD + 2); i++) {
                h[i] = start;
            }
        }
        for (int i = 1; i < reached; i++) {
            h[i] = Math.max(h[i - 1] - 1, Math.min(h[i - 1] + 1, h[i]));
        }
        return reached;
    }

    private static Rotation facing(int[] out) {
        if (out[0] == 1) {
            return Rotation.COUNTERCLOCKWISE_90;
        }
        if (out[0] == -1) {
            return Rotation.CLOCKWISE_90;
        }
        return out[1] == 1 ? Rotation.NONE : Rotation.CLOCKWISE_180;
    }

    private static boolean free(int[] box, List<int[]> taken, Map<Long, Integer> road, int startX, int startZ) {
        if (box[0] < startX - REACH || box[2] > startX + REACH || box[1] < startZ - REACH || box[3] > startZ + REACH) {
            return false;
        }
        for (int[] other : taken) {
            if (box[0] <= other[2] && box[2] >= other[0] && box[1] <= other[3] && box[3] >= other[1]) {
                return false;
            }
        }
        for (int x = box[0]; x <= box[2]; x++) {
            for (int z = box[1]; z <= box[3]; z++) {
                if (road.containsKey(GroundFit.key(x, z))) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean inside(int x, int z, List<int[]> taken) {
        for (int[] box : taken) {
            if (x >= box[0] && x <= box[2] && z >= box[1] && z <= box[3]) {
                return true;
            }
        }
        return false;
    }

    private static void edge(Map<Long, Integer> road, Map<Long, Integer> kinds, List<int[]> taken, int[] at,
                             int[] across, int reach, int y, int kind) {
        int x = at[0] + across[0] * reach;
        int z = at[1] + across[1] * reach;
        long key = GroundFit.key(x, z);
        if (!inside(x, z, taken) && !road.containsKey(key)) {
            road.put(key, y);
            kinds.put(key, kind);
        }
    }

    private static boolean misfit(Ground land, int[] box, int y) {
        int midX = (box[0] + box[2]) / 2;
        int midZ = (box[1] + box[3]) / 2;
        int[][] spots = {{midX, midZ}, {box[0], box[1]}, {box[2], box[1]}, {box[0], box[3]}, {box[2], box[3]}};
        for (int[] spot : spots) {
            int dry = land.dry(spot[0], spot[1]);
            if (dry == Ground.WET || Math.abs(dry - y) > BUILD_FIT) {
                return true;
            }
        }
        return false;
    }

    private static final class Ground {
        private static final int WET = Integer.MIN_VALUE;

        private final GenerationContext context;
        private final Map<Long, Integer> known = new HashMap<>();

        private Ground(GenerationContext context) {
            this.context = context;
        }

        private int dry(int x, int z) {
            return known.computeIfAbsent(GroundFit.key(x, z), key -> {
                int floor = base(x, z, Heightmap.Types.OCEAN_FLOOR_WG);
                return floor == surface(x, z) ? floor - 1 : WET;
            });
        }

        private int surface(int x, int z) {
            return base(x, z, Heightmap.Types.WORLD_SURFACE_WG);
        }

        private int base(int x, int z, Heightmap.Types type) {
            return context.chunkGenerator().getBaseHeight(x, z, type, context.heightAccessor(), context.randomState());
        }
    }

    private static SurfaceTemplatePiece piece(StructureTemplateManager templates, ResourceLocation template,
                                              Rotation rotation, int minX, int minZ, int y,
                                              SurfaceTemplatePiece.Stock stock) {
        Vec3i size = templates.getOrCreate(template).getSize();
        BlockPos pivot = new BlockPos(size.getX() / 2, 0, size.getZ() / 2);
        BlockPos low = BlockPos.ZERO.subtract(pivot).rotate(rotation);
        BlockPos high = new BlockPos(size.getX() - 1, 0, size.getZ() - 1).subtract(pivot).rotate(rotation);
        int offsetX = Math.min(low.getX(), high.getX());
        int offsetZ = Math.min(low.getZ(), high.getZ());
        BlockPos middle = new BlockPos(minX - offsetX, y, minZ - offsetZ);
        return new SurfaceTemplatePiece(templates, template, middle, rotation, 0, stock, false);
    }

    @Override
    public StructureType<?> type() {
        return ModRegistries.OGRE_STREET_STRUCTURE.get();
    }
}
