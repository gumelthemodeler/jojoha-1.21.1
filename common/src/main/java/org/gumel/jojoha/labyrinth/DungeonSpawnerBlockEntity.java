package org.gumel.jojoha.labyrinth;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.config.JojohaServerConfig;
import org.gumel.jojoha.registry.ModBlocks;
import org.gumel.jojoha.registry.ModRegistries;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class DungeonSpawnerBlockEntity extends BlockEntity {
    public static final int WAVE_SIZE = 3;
    public static final int WAVES = 3;
    public static final int WAVE_DELAY_SECONDS = 8;
    public static final int RANGE = 12;
    public static final int CHECK_EVERY = 10;
    public static final int SPREAD = 4;
    public static final int TRIES = 16;
    public static final int DROP = 6;
    public static final int ENCLOSURE = 6;
    public static final int OUTSIDE_REACH = 10;
    public static final int FLOOD_LIMIT = 600;
    public static final int CRAMPED = 24;
    public static final int SIGHT_STEPS = 4;

    private record Roster(Supplier<? extends EntityType<?>> type, int weight) {
    }

    private static final List<Roster> ROSTER = List.of(
            new Roster(ModRegistries.VAMPIRE, 2),
            new Roster(ModRegistries.RISEN_ZOMBIE, 3),
            new Roster(() -> EntityType.SKELETON, 2),
            new Roster(ModRegistries.GHOST, 2));

    private final List<UUID> wave = new ArrayList<>();
    private ListTag roster = new ListTag();
    private int wavesSpawned;
    private int cooldown;
    private boolean wasActive = true;
    private boolean revived;

    public DungeonSpawnerBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistries.DUNGEON_SPAWNER_BLOCK_ENTITY.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, DungeonSpawnerBlockEntity spawner) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        if (spawner.cooldown > 0) {
            spawner.cooldown--;
        }
        if (server.getGameTime() % CHECK_EVERY != 0 || state.getValue(DungeonSpawnerBlock.SPENT)) {
            return;
        }
        boolean active = state.getValue(DungeonSpawnerBlock.ACTIVE);
        if (active != spawner.wasActive) {
            spawner.wasActive = active;
            spawner.changed(server, pos, active);
        }
        if (spawner.prune(server)) {
            spawner.cooldown = JojohaServerConfig.SPAWNER_WAVE_DELAY.whole() * 20;
            spawner.setChanged();
        }
        if (spawner.wave.isEmpty() && spawner.wavesSpawned >= JojohaServerConfig.SPAWNER_WAVES.whole()) {
            spawner.spend(server, pos, state);
            return;
        }
        if (!active) {
            return;
        }
        if (!spawner.wave.isEmpty() || spawner.cooldown > 0) {
            return;
        }
        int range = JojohaServerConfig.SPAWNER_RANGE.whole();
        if (server.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, range,
                EntitySelector.NO_CREATIVE_OR_SPECTATOR) == null) {
            return;
        }
        spawner.spawnWave(server, pos);
    }

    private void changed(ServerLevel level, BlockPos pos, boolean active) {
        Vec3 at = Vec3.atCenterOf(pos);
        if (active) {
            level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.8F, 0.6F);
            level.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 16, 0.4, 0.4, 0.4, 0.02);
        } else {
            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 0.8F);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y, at.z, 20, 0.4, 0.4, 0.4, 0.02);
        }
        setChanged();
    }

    private void spend(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state.setValue(DungeonSpawnerBlock.SPENT, true), 3);
        Vec3 at = Vec3.atCenterOf(pos);
        level.playSound(null, pos, SoundEvents.TRIAL_SPAWNER_CLOSE_SHUTTER, SoundSource.BLOCKS, 1.0F, 0.7F);
        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.8F, 0.6F);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y, at.z, 24, 0.4, 0.4, 0.4, 0.02);
        level.sendParticles(ParticleTypes.SOUL, at.x, at.y + 0.4, at.z, 6, 0.3, 0.3, 0.3, 0.03);
        CatacombSeal.spent(level, pos);
        setChanged();
    }

    public static boolean revivable(BlockState state, ServerLevel level, BlockPos pos) {
        return state.is(ModBlocks.DUNGEON_SPAWNER.get()) && state.getValue(DungeonSpawnerBlock.SPENT)
                && level.getBlockEntity(pos) instanceof DungeonSpawnerBlockEntity spawner && !spawner.revived;
    }

    public boolean revive(ServerLevel level, BlockPos pos, BlockState state) {
        if (!revivable(state, level, pos)) {
            return false;
        }
        revived = true;
        wave.clear();
        cooldown = 0;
        wavesSpawned = Math.max(0, JojohaServerConfig.SPAWNER_WAVES.whole() - 1);
        level.setBlock(pos, state.setValue(DungeonSpawnerBlock.SPENT, false), 3);
        Vec3 at = Vec3.atCenterOf(pos);
        level.playSound(null, pos, SoundEvents.TRIAL_SPAWNER_OPEN_SHUTTER, SoundSource.BLOCKS, 1.0F, 0.6F);
        level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.8F, 0.5F);
        level.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 20, 0.4, 0.4, 0.4, 0.03);
        CatacombSeal.revived(level, pos);
        setChanged();
        return true;
    }

    private boolean prune(ServerLevel level) {
        if (wave.isEmpty()) {
            return false;
        }
        wave.removeIf(id -> {
            Entity entity = level.getEntity(id);
            return entity == null || !entity.isAlive();
        });
        return wave.isEmpty();
    }

    private void spawnWave(ServerLevel level, BlockPos pos) {
        int size = JojohaServerConfig.SPAWNER_WAVE_SIZE.whole();
        List<BlockPos> outside = outsideSpots(level, pos);
        for (int i = 0; i < size; i++) {
            Mob mob = outside.isEmpty() ? spawnOne(level, pos) : spawnOutside(level, outside);
            if (mob != null) {
                wave.add(mob.getUUID());
            }
        }
        if (!wave.isEmpty()) {
            wavesSpawned++;
            level.levelEvent(2004, pos, 0);
            level.playSound(null, pos, SoundEvents.TRIAL_SPAWNER_SPAWN_MOB, SoundSource.BLOCKS, 1.0F, 0.8F);
        }
        setChanged();
    }

    private EntityType<?> pick(ServerLevel level) {
        List<Roster> own = own();
        List<Roster> list = own.isEmpty() ? ROSTER : own;
        int total = 0;
        for (Roster entry : list) {
            total += entry.weight();
        }
        int roll = level.random.nextInt(total);
        for (Roster entry : list) {
            roll -= entry.weight();
            if (roll < 0) {
                return entry.type().get();
            }
        }
        return list.get(0).type().get();
    }

    private List<Roster> own() {
        List<Roster> own = new ArrayList<>();
        for (int i = 0; i < roster.size(); i++) {
            CompoundTag entry = roster.getCompound(i);
            ResourceLocation id = ResourceLocation.tryParse(entry.getString("Id"));
            int weight = entry.getInt("Weight");
            if (id == null || weight <= 0) {
                continue;
            }
            BuiltInRegistries.ENTITY_TYPE.getOptional(id).ifPresent(type -> own.add(new Roster(() -> type, weight)));
        }
        return own;
    }

    private static BlockPos floorBelow(ServerLevel level, BlockPos column) {
        for (int down = 0; down <= DROP + 1; down++) {
            BlockPos at = column.below(down);
            double floor = level.getBlockFloorHeight(at);
            if (floor >= 0.0 && floor <= 0.5) {
                return at;
            }
            if (floor > 0.5) {
                return null;
            }
        }
        return null;
    }

    private static boolean hatch(BlockState state) {
        return state.getBlock() instanceof DoorBlock || state.getBlock() instanceof TrapDoorBlock;
    }

    private static boolean seen(ServerLevel level, Vec3 from, Vec3 to, BlockPos spawner) {
        Vec3 path = to.subtract(from);
        int steps = Math.max(1, (int) Math.ceil(path.length() * SIGHT_STEPS));
        for (int i = 1; i < steps; i++) {
            Vec3 point = from.add(path.scale(i / (double) steps));
            BlockPos at = BlockPos.containing(point);
            if (at.equals(spawner)) {
                continue;
            }
            BlockState state = level.getBlockState(at);
            if (hatch(state)) {
                continue;
            }
            for (AABB box : state.getCollisionShape(level, at).toAabbs()) {
                if (box.move(at).contains(point)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean open(ServerLevel level, BlockPos at) {
        BlockState state = level.getBlockState(at);
        return !hatch(state) && state.getCollisionShape(level, at).isEmpty()
                && state.getFluidState().isEmpty();
    }

    private static boolean standable(ServerLevel level, BlockPos at) {
        BlockPos below = at.below();
        return open(level, at) && open(level, at.above())
                && !level.getBlockState(below).getCollisionShape(level, below).isEmpty();
    }

    private static BlockPos step(ServerLevel level, BlockPos from, Direction direction) {
        BlockPos ahead = from.relative(direction);
        if (standable(level, ahead)) {
            return ahead;
        }
        if (standable(level, ahead.above()) && open(level, from.above(2))) {
            return ahead.above();
        }
        for (int down = 1; down <= 3; down++) {
            if (!open(level, ahead.below(down - 1))) {
                return null;
            }
            if (standable(level, ahead.below(down))) {
                return ahead.below(down);
            }
        }
        return null;
    }

    private static Set<BlockPos> walk(ServerLevel level, List<BlockPos> seeds, BlockPos centre, int reach,
                                      Set<BlockPos> skip) {
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        for (BlockPos seed : seeds) {
            if (!skip.contains(seed) && standable(level, seed) && seen.add(seed)) {
                queue.add(seed);
            }
        }
        while (!queue.isEmpty() && seen.size() < FLOOD_LIMIT) {
            BlockPos cell = queue.poll();
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos next = step(level, cell, direction);
                if (next != null && next.distManhattan(centre) <= reach && !skip.contains(next) && seen.add(next)) {
                    queue.add(next);
                }
            }
        }
        return seen;
    }

    private static List<BlockPos> outsideSpots(ServerLevel level, BlockPos pos) {
        List<BlockPos> around = new ArrayList<>();
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            for (int dy = -1; dy <= 1; dy++) {
                around.add(pos.relative(direction).above(dy));
            }
        }
        Set<BlockPos> inner = walk(level, around, pos, ENCLOSURE, Set.of());
        if (inner.isEmpty() || inner.size() > CRAMPED) {
            return List.of();
        }
        for (BlockPos cell : inner) {
            if (cell.distManhattan(pos) >= ENCLOSURE) {
                return List.of();
            }
        }
        List<BlockPos> beyond = new ArrayList<>();
        for (BlockPos cell : inner) {
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos door = cell.relative(direction);
                if (hatch(level.getBlockState(door))) {
                    BlockPos past = door.relative(direction);
                    beyond.add(past);
                    beyond.add(past.below());
                }
            }
        }
        if (beyond.isEmpty()) {
            return List.of();
        }
        return List.copyOf(walk(level, beyond, pos, ENCLOSURE + OUTSIDE_REACH, inner));
    }

    private Mob spawnOutside(ServerLevel level, List<BlockPos> spots) {
        EntityType<?> type = pick(level);
        for (int attempt = 0; attempt < TRIES; attempt++) {
            BlockPos at = spots.get(level.random.nextInt(spots.size()));
            double x = at.getX() + 0.5, y = at.getY(), z = at.getZ() + 0.5;
            if (!level.noCollision(type.getSpawnAABB(x, y, z))) {
                continue;
            }
            return place(level, type, at, x, y, z);
        }
        return null;
    }

    private Mob place(ServerLevel level, EntityType<?> type, BlockPos at, double x, double y, double z) {
        Entity entity = type.create(level);
        if (!(entity instanceof Mob mob)) {
            return null;
        }
        mob.moveTo(x, y, z, level.random.nextFloat() * 360F, 0F);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.SPAWNER, null);
        mob.setPersistenceRequired();
        level.addFreshEntityWithPassengers(mob);
        mob.spawnAnim();
        return mob;
    }

    private Mob spawnOne(ServerLevel level, BlockPos pos) {
        EntityType<?> type = pick(level);
        Vec3 centre = Vec3.atCenterOf(pos);
        for (int attempt = 0; attempt < TRIES; attempt++) {
            BlockPos at = floorBelow(level, pos.offset(level.random.nextInt(SPREAD * 2 + 1) - SPREAD,
                    1, level.random.nextInt(SPREAD * 2 + 1) - SPREAD));
            if (at == null || at.equals(pos)) {
                continue;
            }
            double x = at.getX() + 0.5, y = at.getY() + level.getBlockFloorHeight(at), z = at.getZ() + 0.5;
            if (!level.noCollision(type.getSpawnAABB(x, y, z))) {
                continue;
            }
            Vec3 target = new Vec3(x, y + 0.5, z);
            if (!seen(level, target, centre, pos)) {
                continue;
            }
            return place(level, type, at, x, y, z);
        }
        return null;
    }

    public int wavesSpawned() {
        return wavesSpawned;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag ids = new ListTag();
        for (UUID id : wave) {
            ids.add(NbtUtils.createUUID(id));
        }
        tag.put("Wave", ids);
        tag.putInt("WavesSpawned", wavesSpawned);
        tag.putInt("Cooldown", cooldown);
        tag.putBoolean("WasActive", wasActive);
        tag.putBoolean("Revived", revived);
        if (!roster.isEmpty()) {
            tag.put("Roster", roster);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        wave.clear();
        for (Tag id : tag.getList("Wave", Tag.TAG_INT_ARRAY)) {
            wave.add(NbtUtils.loadUUID(id));
        }
        wavesSpawned = tag.getInt("WavesSpawned");
        cooldown = tag.getInt("Cooldown");
        wasActive = !tag.contains("WasActive") || tag.getBoolean("WasActive");
        revived = tag.getBoolean("Revived");
        roster = tag.getList("Roster", Tag.TAG_COMPOUND);
    }
}
