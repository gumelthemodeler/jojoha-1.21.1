package org.gumel.jojoha.stand.heaven;

import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.network.packet.HeavenFxPacket;
import org.gumel.jojoha.registry.ModSounds;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.skill.moves.RealityBendSkill;
import org.joml.Vector3f;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class RealityBend {
    public static final int WINDOW_SECONDS = 10;
    public static final float RADIUS = 32F;
    public static final int FRAME_EVERY = 2;
    public static final int PLAYBACK_TICKS = 40;
    public static final int COOLDOWN_SECONDS = 300;
    public static final int HEALTH_BOOST_TICKS = 1200;
    public static final int HEALTH_BOOST_LEVEL = 1;
    public static final int MAX_CHANGES = 20000;

    private static final Vector3f REWIND_GOLD = new Vector3f(1.0F, 0.83F, 0.37F);

    private static final Map<UUID, Recorder> RECORDERS = new HashMap<>();
    private static final Map<UUID, Playback> PLAYING = new HashMap<>();
    private static boolean replaying;

    public static final int MOST_REVIVES = 3;
    private static final Map<UUID, Integer> REVIVES = new HashMap<>();

    private RealityBend() {
    }

    static void init() {
        TickEvent.SERVER_POST.register(RealityBend::tick);
        PlayerEvent.PLAYER_QUIT.register(player -> RECORDERS.remove(player.getUUID()));
        PlayerEvent.PLAYER_RESPAWN.register((player, conqueredEnd, reason) -> {
            if (!conqueredEnd) {
                REVIVES.remove(player.getUUID());
            }
        });
    }

    public static int windowTicks() {
        return Math.max(FRAME_EVERY * 2, WINDOW_SECONDS * 20);
    }

    public static double radius() {
        return RADIUS;
    }

    public static boolean rewinding(Entity entity) {
        return !PLAYING.isEmpty() && PLAYING.containsKey(entity.getUUID());
    }

    public static boolean watching(Level level, BlockPos pos) {
        if (replaying || RECORDERS.isEmpty() || level.isClientSide()) {
            return false;
        }
        double radius = radius();
        for (Recorder recorder : RECORDERS.values()) {
            if (recorder.dimension == level.dimension()
                    && recorder.centre.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= radius * radius) {
                return true;
            }
        }
        return false;
    }

    public static void noteBlock(Level level, BlockPos pos, BlockState incoming) {
        BlockState old = level.getBlockState(pos);
        if (old == incoming) {
            return;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        CompoundTag saved = blockEntity == null ? null : blockEntity.saveWithFullMetadata(level.registryAccess());
        long now = level.getGameTime();
        double radius = radius();
        for (Recorder recorder : RECORDERS.values()) {
            if (recorder.dimension == level.dimension()
                    && recorder.centre.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= radius * radius
                    && recorder.changes.size() < MAX_CHANGES) {
                recorder.changes.addLast(new Change(now, pos.immutable(), old, saved));
            }
        }
    }

    static void fell(ServerLevel level, LivingEntity entity, CompoundTag tag) {
        if (RECORDERS.isEmpty()) {
            return;
        }
        double radius = radius();
        for (Recorder recorder : RECORDERS.values()) {
            if (recorder.dimension == level.dimension() && recorder.centre.distanceToSqr(entity.position()) <= radius * radius) {
                recorder.fell.addLast(new Fell(level.getGameTime(), entity.getUUID(), tag));
            }
        }
    }

    public static boolean save(LivingEntity victim, DamageSource source) {
        if (!(victim instanceof ServerPlayer player) || PLAYING.containsKey(player.getUUID())) {
            return false;
        }
        JojohaPlayerData data = PlayerDataAccess.get(player);
        long now = player.serverLevel().getGameTime();
        if (!Heaven.is(data) || !RealityBendSkill.INSTANCE.isUnlocked(data)
                || data.isMoveOnCooldown(RealityBendSkill.ID, now)
                || REVIVES.getOrDefault(player.getUUID(), 0) >= MOST_REVIVES) {
            return false;
        }
        Recorder recorder = RECORDERS.remove(player.getUUID());
        if (recorder == null || recorder.frames.size() < 2) {
            return false;
        }
        player.setHealth(1F);
        REVIVES.merge(player.getUUID(), 1, Integer::sum);
        data.setMoveCooldown(RealityBendSkill.ID, now, COOLDOWN_SECONDS * 20);
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);

        List<Change> newestFirst = new ArrayList<>(recorder.changes);
        Collections.reverse(newestFirst);
        PLAYING.put(player.getUUID(), new Playback(player.getUUID(), recorder.dimension,
                new ArrayList<>(recorder.frames), newestFirst, new ArrayList<>(recorder.fell), new int[]{0, 0}));

        ServerLevel level = player.serverLevel();
        HeavenShow.screen(player, HeavenFxPacket.REWIND, PLAYBACK_TICKS);
        level.playSound(null, player.blockPosition(), ModSounds.TIME_RESUME.get(), SoundSource.PLAYERS, 1.6F, 0.5F);
        player.displayClientMessage(Component.translatable("message.jojoha.heaven.reality_bend")
                .withStyle(ChatFormatting.LIGHT_PURPLE), true);
        return true;
    }

    private static void tick(MinecraftServer server) {
        if (!PLAYING.isEmpty()) {
            Iterator<Map.Entry<UUID, Playback>> iterator = PLAYING.entrySet().iterator();
            while (iterator.hasNext()) {
                Playback playback = iterator.next().getValue();
                if (playback.step(server)) {
                    iterator.remove();
                }
            }
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID id = player.getUUID();
            if (!player.isAlive() || PLAYING.containsKey(id) || !Heaven.is(PlayerDataAccess.get(player))) {
                RECORDERS.remove(id);
                continue;
            }
            ServerLevel level = player.serverLevel();
            Recorder recorder = RECORDERS.get(id);
            if (recorder == null || recorder.dimension != level.dimension()) {
                recorder = new Recorder(level.dimension());
                RECORDERS.put(id, recorder);
            }
            recorder.centre = player.position();
            long now = level.getGameTime();
            if (now % FRAME_EVERY == 0) {
                recorder.capture(player, level, now);
            }
            recorder.prune(now - windowTicks());
        }
    }

    private static boolean tracked(Entity entity) {
        if (entity instanceof StandEntity || entity.isSpectator()) {
            return false;
        }
        return entity instanceof LivingEntity || entity instanceof ItemEntity || entity instanceof ExperienceOrb;
    }

    private record Snap(double x, double y, double z, float yRot, float xRot, float health) {
    }

    private record Frame(long time, Map<UUID, Snap> snaps) {
    }

    private record Change(long time, BlockPos pos, BlockState old, CompoundTag blockEntity) {
    }

    private record Fell(long time, UUID id, CompoundTag tag) {
    }

    private static final class Recorder {
        private final ResourceKey<Level> dimension;
        private final Deque<Frame> frames = new ArrayDeque<>();
        private final Deque<Change> changes = new ArrayDeque<>();
        private final Deque<Fell> fell = new ArrayDeque<>();
        private Vec3 centre = Vec3.ZERO;

        private Recorder(ResourceKey<Level> dimension) {
            this.dimension = dimension;
        }

        private void capture(ServerPlayer player, ServerLevel level, long now) {
            AABB box = player.getBoundingBox().inflate(radius());
            Map<UUID, Snap> snaps = new HashMap<>();
            for (Entity entity : level.getEntities((Entity) null, box, RealityBend::tracked)) {
                float health = entity instanceof LivingEntity living ? living.getHealth() : -1F;
                snaps.put(entity.getUUID(), new Snap(entity.getX(), entity.getY(), entity.getZ(),
                        entity.getYRot(), entity.getXRot(), health));
            }
            frames.addLast(new Frame(now, snaps));
        }

        private void prune(long oldest) {
            while (frames.size() > 2 && frames.peekFirst().time < oldest) {
                frames.removeFirst();
            }
            while (!changes.isEmpty() && changes.peekFirst().time < oldest) {
                changes.removeFirst();
            }
            while (!fell.isEmpty() && fell.peekFirst().time < oldest) {
                fell.removeFirst();
            }
        }
    }

    private record Playback(UUID user, ResourceKey<Level> dimension, List<Frame> frames,
                            List<Change> newestFirst, List<Fell> fell, int[] progress) {

        private boolean step(MinecraftServer server) {
            ServerLevel level = server.getLevel(dimension);
            if (level == null) {
                return true;
            }
            int tick = progress[0]++;
            int last = frames.size() - 1;
            int index = last - Math.round(tick * last / (float) (PLAYBACK_TICKS - 1));
            Frame frame = frames.get(Math.max(0, Math.min(last, index)));

            replaying = true;
            try {
                revertTo(level, frame.time);
                for (Map.Entry<UUID, Snap> entry : frame.snaps.entrySet()) {
                    Entity entity = level.getEntity(entry.getKey());
                    if (entity != null) {
                        place(level, entity, entry.getValue(), tick % 4 == 0);
                    }
                }
                if (tick >= PLAYBACK_TICKS - 1) {
                    finish(level, server);
                    return true;
                }
            } finally {
                replaying = false;
            }
            return false;
        }

        private void revertTo(ServerLevel level, long time) {
            while (progress[1] < newestFirst.size() && newestFirst.get(progress[1]).time >= time) {
                Change change = newestFirst.get(progress[1]++);
                level.setBlock(change.pos, change.old, Block.UPDATE_ALL);
                if (change.blockEntity != null) {
                    BlockEntity restored = level.getBlockEntity(change.pos);
                    if (restored != null) {
                        restored.loadWithComponents(change.blockEntity, level.registryAccess());
                        restored.setChanged();
                    }
                }
            }
        }

        private static void place(ServerLevel level, Entity entity, Snap snap, boolean trail) {
            if (entity instanceof ServerPlayer player) {
                player.teleportTo(level, snap.x, snap.y, snap.z, snap.yRot, snap.xRot);
            } else {
                entity.teleportTo(snap.x, snap.y, snap.z);
                entity.setYRot(snap.yRot);
                entity.setXRot(snap.xRot);
            }
            entity.setDeltaMovement(Vec3.ZERO);
            entity.fallDistance = 0F;
            if (trail && entity instanceof LivingEntity) {
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, snap.x, snap.y + entity.getBbHeight() * 0.5, snap.z,
                        6, 0.25, 0.4, 0.25, 0.02);
                level.sendParticles(new DustParticleOptions(REWIND_GOLD, 1.4F), snap.x,
                        snap.y + entity.getBbHeight() * 0.5, snap.z, 8, 0.2, entity.getBbHeight() * 0.4, 0.2, 0.0);
            }
        }

        private void finish(ServerLevel level, MinecraftServer server) {
            revertTo(level, Long.MIN_VALUE);
            Frame oldest = frames.get(0);

            Set<UUID> appeared = new HashSet<>();
            for (Frame frame : frames) {
                appeared.addAll(frame.snaps.keySet());
            }
            appeared.removeAll(oldest.snaps.keySet());
            for (UUID id : appeared) {
                Entity entity = level.getEntity(id);
                if (entity instanceof ItemEntity || entity instanceof ExperienceOrb
                        || (entity instanceof Mob && !(entity instanceof StandEntity))) {
                    entity.discard();
                }
            }

            for (Fell fallen : fell) {
                Snap snap = oldest.snaps.get(fallen.id);
                if (snap == null || level.getEntity(fallen.id) != null) {
                    continue;
                }
                Entity risen = FallenMobs.restore(level, fallen.tag, new Vec3(snap.x, snap.y, snap.z));
                if (risen instanceof LivingEntity living && snap.health > 0F) {
                    living.setHealth(Math.min(snap.health, living.getMaxHealth()));
                }
            }

            for (Map.Entry<UUID, Snap> entry : oldest.snaps.entrySet()) {
                if (entry.getKey().equals(user) || entry.getValue().health <= 0F) {
                    continue;
                }
                if (level.getEntity(entry.getKey()) instanceof LivingEntity living && living.isAlive()) {
                    living.setHealth(Math.min(entry.getValue().health, living.getMaxHealth()));
                }
            }

            ServerPlayer player = server.getPlayerList().getPlayer(user);
            if (player != null && player.isAlive()) {
                List<Holder<MobEffect>> harmful = new ArrayList<>();
                for (MobEffectInstance effect : player.getActiveEffects()) {
                    if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                        harmful.add(effect.getEffect());
                    }
                }
                harmful.forEach(player::removeEffect);
                player.clearFire();
                player.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, HEALTH_BOOST_TICKS,
                        HEALTH_BOOST_LEVEL, false, true, true));
                player.setHealth(player.getMaxHealth());
                level.playSound(null, player.blockPosition(), ModSounds.CLOCK_SINGLE.get(), SoundSource.PLAYERS,
                        1.2F, 0.7F);
                level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0, player.getZ(),
                        30, 0.5, 0.8, 0.5, 0.08);
                HeavenShow.on(player, HeavenFxPacket.RISE, 0F, 0);
                level.playSound(null, player.blockPosition(), SoundEvents.TOTEM_USE,
                        SoundSource.PLAYERS, 0.9F, 1.3F);
            }
        }
    }
}
