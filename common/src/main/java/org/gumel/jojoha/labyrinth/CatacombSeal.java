package org.gumel.jojoha.labyrinth;

import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.entity.VampireRat;
import org.gumel.jojoha.mixin.PoolTemplateAccessor;
import org.gumel.jojoha.registry.ModBlocks;
import org.gumel.jojoha.registry.ModEffects;
import org.gumel.jojoha.registry.ModRegistries;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public final class CatacombSeal {
    public static final ResourceKey<Structure> CATACOMBS = ResourceKey.create(Registries.STRUCTURE,
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "catacombs"));
    private static final String GATE = "catacombs/entrance/gate";
    private static final String DESCENT = "catacombs/entrance/descent";
    private static final String ARENA = "catacombs/arena";
    private static final String VAULT = "catacombs/vaults/";
    private static final String EXIT_POOL = "jojoha:catacombs/descent";
    private static final int CHECK_EVERY = 20;
    private static final int FATIGUE_TICKS = 80;
    private static final int FATIGUE_LEVELS = 3;
    private static final int DOOR_WIDTH = 4;
    private static final int DOOR_HEIGHT = 8;
    private static final double WARN_DISTANCE = 4.0;
    private static final double TELL_DISTANCE = 96.0;
    private static final int RAT_ODDS = 75;
    private static final int RAT_LIMIT = 2;
    private static final int RAT_REACH = 24;
    private static final int RAT_NEAR = 5;
    private static final int RAT_FAR = 12;
    private static final int RAT_TRIES = 24;

    private static final class Catacomb {
        final long key;
        final List<BlockPos> spawners;
        final List<Vault> vaults;
        final BlockPos exit;
        final Direction out;
        List<BlockPos> door;

        Catacomb(long key, List<BlockPos> spawners, List<Vault> vaults, BlockPos exit, Direction out) {
            this.key = key;
            this.spawners = spawners;
            this.vaults = vaults;
            this.exit = exit;
            this.out = out;
        }
    }

    private record Vault(List<BlockPos> doors, List<BlockPos> spawners) {
    }

    private static final Map<ResourceKey<Level>, Map<Long, Catacomb>> KNOWN = new HashMap<>();

    private CatacombSeal() {
    }

    public static void init() {
        TickEvent.PLAYER_POST.register(player -> {
            if (player instanceof ServerPlayer server && (server.tickCount + server.getId()) % CHECK_EVERY == 0) {
                watch(server);
            }
        });
        LifecycleEvent.SERVER_STOPPED.register(server -> KNOWN.clear());
    }

    private static Structure catacombs(ServerLevel level) {
        return level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(CATACOMBS);
    }

    private static void watch(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Structure structure = catacombs(level);
        if (structure == null) {
            return;
        }
        BlockPos at = player.blockPosition();
        StructureStart start = level.structureManager().getStructureWithPieceAt(at, structure);
        if (!start.isValid()) {
            return;
        }
        StructurePiece here = pieceAt(start, at);
        if (here == null || named(here, GATE)) {
            return;
        }
        Catacomb catacomb = catacomb(level, start);
        Record record = Record.of(level);
        if (record.opened(catacomb.key)) {
            lift(level, catacomb);
            return;
        }
        int left = remaining(level, catacomb, record);
        if (left == 0) {
            open(level, start, catacomb, record);
            return;
        }
        if (!player.isCreative() && !player.isSpectator()) {
            fatigue(player, catacomb, left);
        }
        seal(level, catacomb);
        warn(player, catacomb, left);
        if (!player.isCreative() && !player.isSpectator()) {
            rats(player, level, start, catacomb, record);
        }
    }

    private static void rats(ServerPlayer player, ServerLevel level, StructureStart start, Catacomb catacomb,
                             Record record) {
        if (level.random.nextInt(RAT_ODDS) != 0) {
            return;
        }
        if (level.getEntitiesOfClass(VampireRat.class, player.getBoundingBox().inflate(RAT_REACH * 2)).size() >= RAT_LIMIT) {
            return;
        }
        BlockPos spawner = spentNear(level, catacomb, record, player.blockPosition(), RAT_REACH);
        if (spawner == null) {
            return;
        }
        BlockPos origin = player.blockPosition();
        for (int tries = 0; tries < RAT_TRIES; tries++) {
            int dx = level.random.nextInt(RAT_FAR * 2 + 1) - RAT_FAR;
            int dz = level.random.nextInt(RAT_FAR * 2 + 1) - RAT_FAR;
            if (Math.abs(dx) + Math.abs(dz) < RAT_NEAR) {
                continue;
            }
            BlockPos spot = origin.offset(dx, level.random.nextInt(5) - 2, dz);
            if (!level.isLoaded(spot) || !within(start, spot) || !level.getBlockState(spot).getCollisionShape(level, spot).isEmpty()
                    || !level.getBlockState(spot.below()).isFaceSturdy(level, spot.below(), Direction.UP)) {
                continue;
            }
            for (Direction side : Direction.Plane.HORIZONTAL.shuffledCopy(level.random)) {
                BlockPos wall = spot.relative(side);
                BlockState stone = level.getBlockState(wall);
                if (stone.isCollisionShapeFullBlock(level, wall) && !stone.is(ModBlocks.DUNGEON_SPAWNER.get())) {
                    emerge(level, spot, side, stone, spawner);
                    return;
                }
            }
        }
    }

    private static void emerge(ServerLevel level, BlockPos spot, Direction wall, BlockState stone, BlockPos spawner) {
        VampireRat rat = ModRegistries.VAMPIRE_RAT.get().create(level);
        if (rat == null) {
            return;
        }
        Vec3 at = Vec3.atBottomCenterOf(spot).add(wall.getStepX() * 0.3, 0.0, wall.getStepZ() * 0.3);
        rat.moveTo(at.x, at.y, at.z, wall.getOpposite().toYRot(), 0.0F);
        rat.finalizeSpawn(level, level.getCurrentDifficultyAt(spot), MobSpawnType.EVENT, null);
        rat.aim(spawner);
        level.addFreshEntity(rat);
        Vec3 face = Vec3.atCenterOf(spot).add(wall.getStepX() * 0.5, -0.3, wall.getStepZ() * 0.5);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, stone), face.x, face.y, face.z, 18, 0.15, 0.2, 0.15, 0.05);
        level.playSound(null, spot, stone.getSoundType().getBreakSound(), SoundSource.BLOCKS, 0.8F, 0.8F);
        level.playSound(null, spot, SoundEvents.SILVERFISH_AMBIENT, SoundSource.HOSTILE, 1.0F, 1.2F);
    }

    public static BlockPos spentNear(ServerLevel level, BlockPos pos, int radius) {
        Structure structure = catacombs(level);
        if (structure == null) {
            return null;
        }
        StructureStart start = level.structureManager().getStructureWithPieceAt(pos, structure);
        if (!start.isValid()) {
            return null;
        }
        Record record = Record.of(level);
        Catacomb catacomb = catacomb(level, start);
        if (record.opened(catacomb.key)) {
            return null;
        }
        return spentNear(level, catacomb, record, pos, radius);
    }

    private static BlockPos spentNear(ServerLevel level, Catacomb catacomb, Record record, BlockPos pos, int radius) {
        BlockPos best = null;
        double nearest = (double) radius * radius;
        for (BlockPos spawner : counted(level, catacomb, record)) {
            double distance = spawner.distSqr(pos);
            if (distance > nearest || !level.isLoaded(spawner)
                    || !DungeonSpawnerBlockEntity.revivable(level.getBlockState(spawner), level, spawner)) {
                continue;
            }
            nearest = distance;
            best = spawner;
        }
        return best;
    }

    public static void revived(ServerLevel level, BlockPos pos) {
        Structure structure = catacombs(level);
        if (structure == null) {
            return;
        }
        StructureStart start = level.structureManager().getStructureWithPieceAt(pos, structure);
        if (start.isValid()) {
            Record.of(level).unclear(start.getChunkPos().toLong(), pos);
        }
    }

    private static void fatigue(ServerPlayer player, Catacomb catacomb, int left) {
        int total = Math.max(1, counted(player.serverLevel(), catacomb, Record.of(player.serverLevel())).size());
        int amplifier = Math.max(0, (int) Math.ceil((double) FATIGUE_LEVELS * left / total) - 1);
        MobEffectInstance current = player.getEffect(ModEffects.graveFatigue());
        if (current != null && current.getAmplifier() > amplifier) {
            player.removeEffect(ModEffects.graveFatigue());
        }
        player.addEffect(new MobEffectInstance(ModEffects.graveFatigue(), FATIGUE_TICKS, amplifier, true, true, true));
    }

    public static boolean holds(ServerPlayer player, double x, double y, double z) {
        if (player.isCreative() || player.isSpectator() || player.isRemoved()) {
            return false;
        }
        ServerLevel level = player.serverLevel();
        Structure structure = catacombs(level);
        if (structure == null) {
            return false;
        }
        BlockPos at = player.blockPosition();
        StructureStart start = level.structureManager().getStructureWithPieceAt(at, structure);
        if (!start.isValid() || !within(start, at)) {
            return false;
        }
        Catacomb catacomb = catacomb(level, start);
        Record record = Record.of(level);
        if (catacomb.door == null || record.opened(catacomb.key) || remaining(level, catacomb, record) == 0) {
            return false;
        }
        if (within(start, BlockPos.containing(x, y, z))) {
            return false;
        }
        player.displayClientMessage(Component.translatable("message.jojoha.catacombs.held").withStyle(ChatFormatting.GRAY), true);
        level.playSound(null, at, SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 1.0F, 0.6F);
        return true;
    }

    private static boolean within(StructureStart start, BlockPos pos) {
        StructurePiece piece = pieceAt(start, pos);
        return piece != null && !named(piece, GATE) && !named(piece, DESCENT);
    }

    public static void spent(ServerLevel level, BlockPos pos) {
        Structure structure = catacombs(level);
        if (structure == null) {
            return;
        }
        StructureStart start = level.structureManager().getStructureWithPieceAt(pos, structure);
        if (start.isValid()) {
            Record.of(level).clear(start.getChunkPos().toLong(), pos);
        }
    }

    private static StructurePiece pieceAt(StructureStart start, BlockPos pos) {
        for (StructurePiece piece : start.getPieces()) {
            if (piece.getBoundingBox().isInside(pos)) {
                return piece;
            }
        }
        return null;
    }

    private static boolean named(StructurePiece piece, String name) {
        return piece instanceof PoolElementStructurePiece pooled && pooled.getElement().toString().contains(name);
    }

    private static Catacomb catacomb(ServerLevel level, StructureStart start) {
        long key = start.getChunkPos().toLong();
        Map<Long, Catacomb> known = KNOWN.computeIfAbsent(level.dimension(), dimension -> new HashMap<>());
        Catacomb found = known.get(key);
        if (found != null) {
            return found;
        }
        StructureTemplateManager templates = level.getServer().getStructureManager();
        List<BlockPos> spawners = new ArrayList<>();
        List<Vault> vaults = new ArrayList<>();
        BlockPos exit = null;
        Direction out = null;
        for (StructurePiece piece : start.getPieces()) {
            if (!(piece instanceof PoolElementStructurePiece pooled) || !(pooled.getElement() instanceof SinglePoolElement single)) {
                continue;
            }
            if (named(piece, ARENA)) {
                for (StructureTemplate.StructureBlockInfo jigsaw : single.getShuffledJigsawBlocks(templates,
                        pooled.getPosition(), pooled.getRotation(), RandomSource.create(0L))) {
                    if (jigsaw.nbt() != null && EXIT_POOL.equals(jigsaw.nbt().getString("pool"))) {
                        exit = jigsaw.pos();
                        out = JigsawBlock.getFrontFacing(jigsaw.state());
                    }
                }
            }
            if (named(piece, GATE) || named(piece, DESCENT)) {
                continue;
            }
            StructureTemplate template = ((PoolTemplateAccessor) single).jojoha$template()
                    .map(templates::getOrCreate, Function.identity());
            StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(pooled.getRotation());
            List<BlockPos> inside = new ArrayList<>();
            for (StructureTemplate.StructureBlockInfo info : template.filterBlocks(pooled.getPosition(), settings,
                    ModBlocks.DUNGEON_SPAWNER.get())) {
                inside.add(info.pos());
            }
            if (!named(piece, VAULT)) {
                spawners.addAll(inside);
                continue;
            }
            List<BlockPos> doors = new ArrayList<>();
            for (StructureTemplate.StructureBlockInfo info : template.filterBlocks(pooled.getPosition(), settings,
                    ModBlocks.LABYRINTH_DOOR.get())) {
                doors.add(info.pos());
            }
            if (!inside.isEmpty()) {
                vaults.add(new Vault(List.copyOf(doors), List.copyOf(inside)));
            }
        }
        Catacomb made = new Catacomb(key, List.copyOf(spawners), List.copyOf(vaults), exit, out);
        known.put(key, made);
        return made;
    }

    private static List<BlockPos> counted(ServerLevel level, Catacomb catacomb, Record record) {
        List<BlockPos> counted = new ArrayList<>(catacomb.spawners);
        for (Vault vault : catacomb.vaults) {
            if (unlocked(level, catacomb, record, vault)) {
                counted.addAll(vault.spawners());
            }
        }
        return counted;
    }

    private static boolean unlocked(ServerLevel level, Catacomb catacomb, Record record, Vault vault) {
        if (vault.doors().isEmpty()) {
            return true;
        }
        BlockPos mark = vault.doors().get(0);
        if (record.unlocked(catacomb.key, mark)) {
            return true;
        }
        for (BlockPos door : vault.doors()) {
            if (level.isLoaded(door) && !level.getBlockState(door).is(ModBlocks.LABYRINTH_DOOR.get())) {
                record.unlock(catacomb.key, mark);
                return true;
            }
        }
        return false;
    }

    private static int remaining(ServerLevel level, Catacomb catacomb, Record record) {
        int left = 0;
        for (BlockPos pos : counted(level, catacomb, record)) {
            if (record.cleared(catacomb.key, pos)) {
                continue;
            }
            if (level.isLoaded(pos)) {
                BlockState state = level.getBlockState(pos);
                if (!state.is(ModBlocks.DUNGEON_SPAWNER.get()) || state.getValue(DungeonSpawnerBlock.SPENT)) {
                    record.clear(catacomb.key, pos);
                    continue;
                }
            }
            left++;
        }
        return left;
    }

    private static List<BlockPos> door(ServerLevel level, Catacomb catacomb) {
        if (catacomb.door != null) {
            return catacomb.door;
        }
        if (catacomb.exit == null || catacomb.out.getAxis().isVertical() || !level.isLoaded(catacomb.exit)) {
            return List.of();
        }
        Direction side = catacomb.out.getClockWise();
        BlockPos base = catacomb.exit.above();
        List<BlockPos> cells = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(base);
        seen.add(base);
        while (!queue.isEmpty()) {
            BlockPos cell = queue.poll();
            BlockState state = level.getBlockState(cell);
            if (!state.isAir() && !state.is(ModRegistries.CATACOMB_FOG.get())) {
                continue;
            }
            cells.add(cell);
            for (BlockPos next : List.of(cell.above(), cell.below(), cell.relative(side), cell.relative(side.getOpposite()))) {
                int across = Math.abs((next.getX() - base.getX()) * side.getStepX() + (next.getZ() - base.getZ()) * side.getStepZ());
                int up = next.getY() - base.getY();
                if (across <= DOOR_WIDTH && up >= 0 && up <= DOOR_HEIGHT && seen.add(next)) {
                    queue.add(next);
                }
            }
        }
        if (!cells.isEmpty()) {
            catacomb.door = List.copyOf(cells);
        }
        return cells;
    }

    private static void seal(ServerLevel level, Catacomb catacomb) {
        BlockState fog = ModRegistries.CATACOMB_FOG.get().defaultBlockState().setValue(CatacombFogBlock.FACING, catacomb.out);
        boolean placed = false;
        for (BlockPos cell : door(level, catacomb)) {
            if (level.getBlockState(cell).isAir()) {
                level.setBlock(cell, fog, 3);
                placed = true;
            }
        }
        if (placed) {
            BlockPos middle = catacomb.exit.above(2);
            level.playSound(null, middle, SoundEvents.SOUL_ESCAPE.value(), SoundSource.BLOCKS, 1.6F, 0.6F);
            level.playSound(null, middle, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.8F, 0.5F);
        }
    }

    private static void lift(ServerLevel level, Catacomb catacomb) {
        boolean lifted = false;
        for (BlockPos cell : door(level, catacomb)) {
            if (level.getBlockState(cell).is(ModRegistries.CATACOMB_FOG.get())) {
                level.setBlock(cell, Blocks.AIR.defaultBlockState(), 3);
                Vec3 at = Vec3.atCenterOf(cell);
                level.sendParticles(ParticleTypes.WHITE_ASH, at.x, at.y, at.z, 20, 0.4, 0.4, 0.4, 0.02);
                level.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 3, 0.3, 0.3, 0.3, 0.02);
                lifted = true;
            }
        }
        if (lifted) {
            BlockPos middle = catacomb.exit.above(2);
            level.playSound(null, middle, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1.2F, 0.6F);
            level.playSound(null, middle, SoundEvents.SOUL_ESCAPE.value(), SoundSource.BLOCKS, 1.6F, 1.2F);
        }
    }

    private static void open(ServerLevel level, StructureStart start, Catacomb catacomb, Record record) {
        record.open(catacomb.key);
        lift(level, catacomb);
        Component line = Component.translatable("message.jojoha.catacombs.cleared").withStyle(ChatFormatting.GRAY);
        Vec3 centre = start.getBoundingBox().getCenter().getCenter();
        for (ServerPlayer player : level.players()) {
            if (player.position().distanceTo(centre) < TELL_DISTANCE
                    && level.structureManager().getStructureWithPieceAt(player.blockPosition(), catacombs(level)).isValid()) {
                player.displayClientMessage(line, true);
                player.removeEffect(ModEffects.graveFatigue());
                level.playSound(null, player.blockPosition(), SoundEvents.BELL_RESONATE, SoundSource.BLOCKS, 0.8F, 0.6F);
            }
        }
    }

    private static void warn(ServerPlayer player, Catacomb catacomb, int left) {
        if (catacomb.door == null || catacomb.door.isEmpty()) {
            return;
        }
        Vec3 middle = Vec3.atCenterOf(catacomb.exit.above(2));
        Vec3 from = player.position().subtract(middle);
        if (from.length() < WARN_DISTANCE && from.x * catacomb.out.getStepX() + from.z * catacomb.out.getStepZ() < 0) {
            player.displayClientMessage((left == 1 ? Component.translatable("message.jojoha.catacombs.sealed.one")
                    : Component.translatable("message.jojoha.catacombs.sealed", left))
                    .withStyle(ChatFormatting.GRAY), true);
        }
    }

    private static final class Record extends SavedData {
        private static final String NAME = "jojoha_catacombs";
        private static final Factory<Record> FACTORY = new Factory<>(Record::new, Record::load, null);

        private final Map<Long, Set<Long>> cleared = new HashMap<>();
        private final Set<Long> opened = new HashSet<>();
        private final Map<Long, Set<Long>> unlocked = new HashMap<>();

        static Record of(ServerLevel level) {
            return level.getDataStorage().computeIfAbsent(FACTORY, NAME);
        }

        private static Record load(CompoundTag tag, HolderLookup.Provider registries) {
            Record record = new Record();
            CompoundTag cleared = tag.getCompound("Cleared");
            for (String key : cleared.getAllKeys()) {
                Set<Long> positions = new HashSet<>();
                for (long pos : cleared.getLongArray(key)) {
                    positions.add(pos);
                }
                record.cleared.put(Long.parseLong(key), positions);
            }
            CompoundTag unlocked = tag.getCompound("Unlocked");
            for (String key : unlocked.getAllKeys()) {
                Set<Long> doors = new HashSet<>();
                for (long pos : unlocked.getLongArray(key)) {
                    doors.add(pos);
                }
                record.unlocked.put(Long.parseLong(key), doors);
            }
            for (long key : tag.getLongArray("Opened")) {
                record.opened.add(key);
            }
            return record;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            CompoundTag cleared = new CompoundTag();
            this.cleared.forEach((key, positions) -> cleared.putLongArray(Long.toString(key),
                    positions.stream().mapToLong(Long::longValue).toArray()));
            tag.put("Cleared", cleared);
            CompoundTag unlocked = new CompoundTag();
            this.unlocked.forEach((key, doors) -> unlocked.putLongArray(Long.toString(key),
                    doors.stream().mapToLong(Long::longValue).toArray()));
            tag.put("Unlocked", unlocked);
            tag.putLongArray("Opened", opened.stream().mapToLong(Long::longValue).toArray());
            return tag;
        }

        boolean cleared(long key, BlockPos pos) {
            Set<Long> positions = cleared.get(key);
            return positions != null && positions.contains(pos.asLong());
        }

        void unclear(long key, BlockPos pos) {
            Set<Long> positions = cleared.get(key);
            if (positions != null && positions.remove(pos.asLong())) {
                setDirty();
            }
        }

        void clear(long key, BlockPos pos) {
            if (cleared.computeIfAbsent(key, k -> new HashSet<>()).add(pos.asLong())) {
                setDirty();
            }
        }

        boolean unlocked(long key, BlockPos door) {
            Set<Long> doors = unlocked.get(key);
            return doors != null && doors.contains(door.asLong());
        }

        void unlock(long key, BlockPos door) {
            if (unlocked.computeIfAbsent(key, k -> new HashSet<>()).add(door.asLong())) {
                setDirty();
            }
        }

        boolean opened(long key) {
            return opened.contains(key);
        }

        void open(long key) {
            if (opened.add(key)) {
                setDirty();
            }
        }
    }
}
