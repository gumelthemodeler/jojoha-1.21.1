package org.gumel.jojoha.stand.heaven;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.BlockEvent;
import dev.architectury.event.events.common.ExplosionEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Clearable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class WorldPatch {
    public static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final String STORE = "jojoha_world_patches";
    private static final int MOST_LIFT = 96;

    private static final List<Job> JOBS = new ArrayList<>();
    private static final Map<ResourceKey<Level>, Set<Long>> HELD = new HashMap<>();

    public interface Selector {
        boolean wants(ServerLevel level, BlockPos pos, BlockState state);
    }

    public interface Plan {
        BlockState to(ServerLevel level, BlockPos pos, BlockState now);
    }

    private enum Stage { SCANNING, WAITING, APPLYING, HOLDING, RESTORING, DONE }

    private record Done(BlockPos pos, BlockState from, CompoundTag entity) {
    }

    public static final class Job {
        private final ServerLevel level;
        private final BlockPos centre;
        private final int radius;
        private final Selector selector;
        private final Plan plan;
        private final int layersPerTick;
        private final long applyAt;
        private final int applyTicks;
        private final int holdTicks;
        private final int restoreTicks;
        private final boolean restoreInward;
        private final List<BlockPos> found = new ArrayList<>();
        private final List<Done> done = new ArrayList<>();
        private Stage stage = Stage.SCANNING;
        private int nextLayer;
        private int cursor;
        private int perTick;
        private long restoreAt;

        private Job(ServerLevel level, BlockPos centre, int radius, int scanTicks, long applyAt, int applyTicks,
                    int holdTicks, int restoreTicks, boolean restoreInward, Selector selector, Plan plan) {
            this.level = level;
            this.centre = centre;
            this.radius = radius;
            this.selector = selector;
            this.plan = plan;
            this.layersPerTick = Math.max(1, (int) Math.ceil((radius * 2 + 1) / (double) Math.max(1, scanTicks)));
            this.applyAt = applyAt;
            this.applyTicks = Math.max(1, applyTicks);
            this.holdTicks = holdTicks;
            this.restoreTicks = Math.max(1, restoreTicks);
            this.restoreInward = restoreInward;
            this.nextLayer = -radius;
        }

        private void tick() {
            long now = level.getGameTime();
            switch (stage) {
                case SCANNING -> scan();
                case WAITING -> {
                    if (now >= applyAt) {
                        stage = Stage.APPLYING;
                        cursor = 0;
                        perTick = Math.max(1, (int) Math.ceil(found.size() / (double) applyTicks));
                    }
                }
                case APPLYING -> apply();
                case HOLDING -> {
                    if (now >= restoreAt) {
                        stage = Stage.RESTORING;
                        cursor = 0;
                        perTick = Math.max(1, (int) Math.ceil(done.size() / (double) restoreTicks));
                    }
                }
                case RESTORING -> restore();
                default -> {
                }
            }
        }

        private void scan() {
            Set<Long> held = held(level);
            BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
            int r2 = radius * radius;
            for (int layer = 0; layer < layersPerTick && nextLayer <= radius; layer++, nextLayer++) {
                int dy = nextLayer;
                int y = centre.getY() + dy;
                if (y < level.getMinBuildHeight() || y >= level.getMaxBuildHeight()) {
                    continue;
                }
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        if (dx * dx + dy * dy + dz * dz > r2) {
                            continue;
                        }
                        at.set(centre.getX() + dx, y, centre.getZ() + dz);
                        if (held.contains(at.asLong()) || !level.isLoaded(at)) {
                            continue;
                        }
                        BlockState state = level.getBlockState(at);
                        if (selector.wants(level, at, state)) {
                            found.add(at.immutable());
                        }
                    }
                }
            }
            if (nextLayer > radius) {
                found.sort((a, b) -> Double.compare(a.distSqr(centre), b.distSqr(centre)));
                stage = Stage.WAITING;
            }
        }

        private void apply() {
            Set<Long> held = held(level);
            int end = Math.min(found.size(), cursor + perTick);
            for (; cursor < end; cursor++) {
                BlockPos pos = found.get(cursor);
                if (held.contains(pos.asLong())) {
                    continue;
                }
                BlockState now = level.getBlockState(pos);
                BlockState to = plan.to(level, pos, now);
                if (to == null || to == now) {
                    continue;
                }
                CompoundTag entity = null;
                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity != null) {
                    entity = blockEntity.saveWithFullMetadata(level.registryAccess());
                    Clearable.tryClear(blockEntity);
                }
                level.setBlock(pos, to, FLAGS);
                done.add(new Done(pos, now, entity));
                held.add(pos.asLong());
            }
            store(level).setDirty();
            if (cursor >= found.size()) {
                found.clear();
                stage = Stage.HOLDING;
                restoreAt = level.getGameTime() + holdTicks;
            }
        }

        private void restore() {
            Set<Long> held = held(level);
            int end = Math.min(done.size(), cursor + perTick);
            for (; cursor < end; cursor++) {
                Done one = done.get(restoreInward ? done.size() - 1 - cursor : cursor);
                put(level, one);
                held.remove(one.pos.asLong());
            }
            store(level).setDirty();
            lift(level, centre, radius);
            if (cursor >= done.size()) {
                done.clear();
                stage = Stage.DONE;
            }
        }

        private void restoreAll() {
            Set<Long> held = held(level);
            boolean restoring = stage == Stage.RESTORING;
            for (int i = restoring ? cursor : 0; i < done.size(); i++) {
                Done one = done.get(restoring && restoreInward ? done.size() - 1 - i : i);
                put(level, one);
                held.remove(one.pos.asLong());
            }
            done.clear();
            stage = Stage.DONE;
        }
    }

    private WorldPatch() {
    }

    public static Job begin(ServerLevel level, BlockPos centre, int radius, int scanTicks, int applyAfter,
                            int applyTicks, int holdTicks, int restoreTicks, boolean restoreInward,
                            Selector selector, Plan plan) {
        Job job = new Job(level, centre, radius, scanTicks, level.getGameTime() + applyAfter, applyTicks, holdTicks,
                restoreTicks, restoreInward, selector, plan);
        JOBS.add(job);
        return job;
    }

    public static boolean held(Level level, BlockPos pos) {
        Set<Long> held = HELD.get(level.dimension());
        return held != null && held.contains(pos.asLong());
    }

    private static Set<Long> held(ServerLevel level) {
        return HELD.computeIfAbsent(level.dimension(), key -> new HashSet<>());
    }

    private static void put(ServerLevel level, Done one) {
        level.setBlock(one.pos, one.from, FLAGS);
        if (one.entity != null) {
            BlockEntity blockEntity = level.getBlockEntity(one.pos);
            if (blockEntity != null) {
                blockEntity.loadWithComponents(one.entity, level.registryAccess());
                blockEntity.setChanged();
            }
        }
    }

    private static void lift(ServerLevel level, BlockPos centre, int radius) {
        AABB area = new AABB(centre).inflate(radius + 2);
        for (Entity entity : level.getEntitiesOfClass(Entity.class, area,
                entity -> entity.isAlive() && !(entity instanceof HangingEntity))) {
            unstick(entity);
        }
    }

    private static void unstick(Entity entity) {
        Level level = entity.level();
        if (level.noCollision(entity)) {
            return;
        }
        for (int up = 1; up <= MOST_LIFT; up++) {
            AABB moved = entity.getBoundingBox().move(0.0, up, 0.0);
            if (level.noCollision(entity, moved)) {
                entity.teleportTo(entity.getX(), entity.getY() + up, entity.getZ());
                if (entity instanceof LivingEntity living) {
                    living.fallDistance = 0F;
                }
                return;
            }
        }
    }

    public static void init() {
        TickEvent.SERVER_POST.register(WorldPatch::tick);
        BlockEvent.BREAK.register((level, pos, state, player, xp) ->
                held(level, pos) ? EventResult.interruptFalse() : EventResult.pass());
        BlockEvent.PLACE.register((level, pos, state, placer) ->
                held(level, pos) ? EventResult.interruptFalse() : EventResult.pass());
        ExplosionEvent.DETONATE.register((level, explosion, entities) ->
                explosion.getToBlow().removeIf(pos -> held(level, pos)));
        LifecycleEvent.SERVER_LEVEL_LOAD.register(WorldPatch::recover);
        PlayerEvent.PLAYER_JOIN.register(player -> unstick(player));
        LifecycleEvent.SERVER_STOPPING.register(server -> {
            for (Job job : JOBS) {
                job.restoreAll();
                store(job.level).setDirty();
            }
            JOBS.clear();
            HELD.clear();
        });
    }

    private static void tick(MinecraftServer server) {
        if (JOBS.isEmpty()) {
            return;
        }
        Iterator<Job> iterator = JOBS.iterator();
        while (iterator.hasNext()) {
            Job job = iterator.next();
            job.tick();
            if (job.stage == Stage.DONE) {
                iterator.remove();
            }
        }
    }

    private static void recover(ServerLevel level) {
        Store store = store(level);
        if (store.leftovers.isEmpty()) {
            return;
        }
        HolderLookup.Provider registries = level.registryAccess();
        for (Tag entry : store.leftovers) {
            CompoundTag tag = (CompoundTag) entry;
            BlockPos pos = BlockPos.of(tag.getLong("pos"));
            BlockState from = NbtUtils.readBlockState(registries.lookupOrThrow(Registries.BLOCK), tag.getCompound("from"));
            put(level, new Done(pos, from, tag.contains("entity") ? tag.getCompound("entity") : null));
        }
        store.leftovers = new ListTag();
        store.setDirty();
    }

    private static Store store(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(() -> new Store(level.dimension()),
                        (tag, registries) -> Store.load(level.dimension(), tag), null), STORE);
    }

    private static final class Store extends SavedData {
        private final ResourceKey<Level> dimension;
        private ListTag leftovers = new ListTag();

        private Store(ResourceKey<Level> dimension) {
            this.dimension = dimension;
        }

        private static Store load(ResourceKey<Level> dimension, CompoundTag tag) {
            Store store = new Store(dimension);
            store.leftovers = tag.getList("done", Tag.TAG_COMPOUND);
            return store;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            ListTag list = leftovers.copy();
            for (Job job : JOBS) {
                if (job.level.dimension() != dimension) {
                    continue;
                }
                int from = job.stage == Stage.RESTORING ? job.cursor : 0;
                for (int i = from; i < job.done.size(); i++) {
                    Done one = job.done.get(job.restoreInward && job.stage == Stage.RESTORING ? job.done.size() - 1 - i : i);
                    CompoundTag entry = new CompoundTag();
                    entry.putLong("pos", one.pos.asLong());
                    entry.put("from", NbtUtils.writeBlockState(one.from));
                    if (one.entity != null) {
                        entry.put("entity", one.entity);
                    }
                    list.add(entry);
                }
            }
            tag.put("done", list);
            return tag;
        }
    }
}
