package org.gumel.jojoha.worldevent;

import org.gumel.jojoha.entity.npc.Quests;
import org.gumel.jojoha.town.Settlements;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.entity.VampireEntity;
import org.gumel.jojoha.entity.npc.HamonStudent;
import org.gumel.jojoha.entity.npc.NpcStanding;
import org.gumel.jojoha.registry.ModItems;
import org.gumel.jojoha.registry.ModRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

final class VampireRaid implements WorldEvents.Handler {
    private static final int FIND_RANGE = 96;
    private static final double BAR_RANGE = 128.0;
    private static final int WAVES = 3;
    private static final int MORE_PER_WAVE = 1;
    private static final int MOST_EXTRA_PLAYERS = 3;
    private static final int SPAWN_NEAREST = 28;
    private static final int SPAWN_FARTHEST = 40;
    private static final int CHECK_EVERY = 20;
    private static final int MARCH_EVERY = 100;
    private static final double PREY_RANGE = 48.0;
    private static final int HELPER_WAVE = 2;
    private static final int HELPERS = 2;
    private static final float HELP_CHANCE = 0.35F;
    private static final float HELP_PER_STANDING = 0.02F;
    private static final float MOST_HELP = 0.9F;
    private static final int HERO_TICKS = 48000;
    private static final int FEWEST_COINS = 6;
    private static final int EXTRA_COINS = 5;
    private static final float CRYSTAL_CHANCE = 0.25F;
    private static final int GIVE_UP_TICKS = 2400;

    private final ServerBossEvent bar = new ServerBossEvent(Component.translatable("event.jojoha.vampire_raid.bar"),
            BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);
    private final List<UUID> wave = new ArrayList<>();
    private int waveNumber;
    private int waveSize;
    private int sinceProgress;
    private int residentsAtStart;
    private int unwatched;
    private static final double TOWN_RANGE = 64.0;
    private static final int ABANDONED_TICKS = 1200;

    @Override
    public boolean canRoll(ServerLevel level) {
        return level.players().stream().anyMatch(player -> bell(level, player.blockPosition()).isPresent());
    }

    private static Optional<BlockPos> bell(ServerLevel level, BlockPos near) {
        return level.getPoiManager().findClosest(holder -> holder.is(PoiTypes.MEETING), near, FIND_RANGE,
                PoiManager.Occupancy.ANY);
    }

    @Override
    public void start(ServerLevel level, WorldEvents state) {
        reset();
        List<ServerPlayer> players = new ArrayList<>(level.players());
        Collections.shuffle(players);
        for (ServerPlayer player : players) {
            Optional<BlockPos> bell = bell(level, player.blockPosition());
            if (bell.isPresent()) {
                state.focus(bell.get());
                nextWave(level, bell.get());
                return;
            }
        }
        if (state.forced() && !players.isEmpty()) {
            BlockPos at = players.get(0).blockPosition();
            state.focus(at);
            nextWave(level, at);
            return;
        }
        MinecraftServer server = level.getServer();
        state.cancel(server);
    }

    private void reset() {
        residentsAtStart = 0;
        unwatched = 0;
        wave.clear();
        waveNumber = 0;
        waveSize = 0;
        sinceProgress = 0;
        bar.removeAllPlayers();
        bar.setProgress(1F);
    }

    private void nextWave(ServerLevel level, BlockPos bell) {
        waveNumber++;
        wave.clear();
        sinceProgress = 0;
        int extraPlayers = Math.min(MOST_EXTRA_PLAYERS, Math.max(0, near(level, bell).size() - 1));
        int count = VampireNight.cap(level, bell) + (waveNumber - 1) * MORE_PER_WAVE + extraPlayers;
        for (int i = 0; i < count; i++) {
            VampireEntity vampire = VampireNight.spawnAround(level, ModRegistries.VAMPIRE.get(), bell, SPAWN_NEAREST,
                    SPAWN_FARTHEST, false);
            if (vampire != null) {
                vampire.setPersistenceRequired();
                wave.add(vampire.getUUID());
            }
        }
        waveSize = Math.max(1, wave.size());
        bar.setName(Component.translatable("event.jojoha.vampire_raid.wave", waveNumber, WAVES));
        bar.setProgress(1F);
        level.playSound(null, bell, SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 4F, 0.6F);
        if (waveNumber == HELPER_WAVE) {
            helpers(level, bell);
        }
    }

    private void helpers(ServerLevel level, BlockPos bell) {
        int best = 0;
        for (ServerPlayer player : near(level, bell)) {
            best = Math.max(best, NpcStanding.hamon(player));
        }
        float chance = Math.min(MOST_HELP, HELP_CHANCE + Math.max(0, best) * HELP_PER_STANDING);
        if (level.random.nextFloat() >= chance) {
            return;
        }
        HamonStudent first = null;
        for (int i = 0; i < HELPERS; i++) {
            HamonStudent student = VampireNight.spawnAround(level, ModRegistries.HAMON_STUDENT.get(), bell, 4, 10, false);
            if (student != null && first == null) {
                first = student;
            }
        }
        if (first != null) {
            first.cry();
        }
    }

    private static List<ServerPlayer> near(ServerLevel level, BlockPos bell) {
        return level.getEntitiesOfClass(ServerPlayer.class, new AABB(bell).inflate(FIND_RANGE),
                player -> !player.isSpectator());
    }

    @Override
    public void tick(ServerLevel level, WorldEvents state) {
        BlockPos bell = state.focus();
        if (bell == null || level.getGameTime() % CHECK_EVERY != 0) {
            return;
        }
        if (waveNumber == 0) {
            nextWave(level, bell);
            return;
        }
        int residents = residents(level, bell);
        if (residentsAtStart == 0) {
            residentsAtStart = residents;
        }
        unwatched = near(level, bell).isEmpty() ? unwatched + CHECK_EVERY : 0;
        boolean overrun = residentsAtStart >= 2 && residents * 2 < residentsAtStart;
        if (overrun || unwatched >= ABANDONED_TICKS) {
            lost(level, bell);
            state.stop(level.getServer());
            return;
        }
        for (ServerPlayer player : level.players()) {
            boolean close = player.distanceToSqr(Vec3.atCenterOf(bell)) <= BAR_RANGE * BAR_RANGE;
            if (close) {
                bar.addPlayer(player);
            } else {
                bar.removePlayer(player);
            }
        }
        int before = wave.size();
        wave.removeIf(id -> !(level.getEntity(id) instanceof VampireEntity vampire) || !vampire.isAlive());
        sinceProgress = wave.size() < before ? 0 : sinceProgress + CHECK_EVERY;
        bar.setProgress(wave.size() / (float) waveSize);
        if (level.getGameTime() % MARCH_EVERY == 0) {
            march(level, bell);
        }
        if (!wave.isEmpty() && sinceProgress < GIVE_UP_TICKS) {
            return;
        }
        if (waveNumber < WAVES) {
            nextWave(level, bell);
        } else {
            won(level, bell);
            state.stop(level.getServer());
        }
    }

    private void march(ServerLevel level, BlockPos bell) {
        for (UUID id : wave) {
            if (!(level.getEntity(id) instanceof VampireEntity vampire) || vampire.getTarget() != null) {
                continue;
            }
            LivingEntity prey = prey(level, vampire, bell);
            if (prey != null) {
                vampire.setTarget(prey);
            } else {
                vampire.getNavigation().moveTo(bell.getX() + 0.5, bell.getY(), bell.getZ() + 0.5, 1.0);
            }
        }
    }

    @Nullable
    private static LivingEntity prey(ServerLevel level, VampireEntity vampire, BlockPos bell) {
        LivingEntity best = null;
        double nearest = Double.MAX_VALUE;
        for (LivingEntity body : level.getEntitiesOfClass(LivingEntity.class, new AABB(bell).inflate(PREY_RANGE),
                body -> body.isAlive() && (body instanceof AbstractVillager || body instanceof HamonStudent
                        || body instanceof ServerPlayer player && !player.isCreative() && !player.isSpectator()
                        && !vampire.spared(player)))) {
            double distance = body.distanceToSqr(vampire);
            if (distance < nearest) {
                nearest = distance;
                best = body;
            }
        }
        return best;
    }

    private static int residents(ServerLevel level, BlockPos bell) {
        return level.getEntitiesOfClass(AbstractVillager.class, new AABB(bell).inflate(TOWN_RANGE), LivingEntity::isAlive).size();
    }

    private void lost(ServerLevel level, BlockPos bell) {
        Settlements.raidLost(level, bell, residentsAtStart);
        Quests.raidEnded(level, bell, true);
        for (UUID id : wave) {
            if (level.getEntity(id) instanceof VampireEntity vampire) {
                vampire.setPersistenceRequired();
            }
        }
    }

    private void won(ServerLevel level, BlockPos bell) {
        Settlements.raidWon(level, bell, near(level, bell));
        Quests.raidEnded(level, bell, false);
        for (ServerPlayer player : near(level, bell)) {
            player.addEffect(new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE, HERO_TICKS, 0, false, false, true));
            give(player, new ItemStack(ModItems.JOJO_COIN.get(), FEWEST_COINS + level.random.nextInt(EXTRA_COINS)));
            if (level.random.nextFloat() < CRYSTAL_CHANCE) {
                give(player, new ItemStack(ModItems.STAT_CRYSTAL.get()));
            }
            player.sendSystemMessage(Component.translatable("event.jojoha.vampire_raid.won").withStyle(ChatFormatting.GOLD));
        }
        level.playSound(null, bell, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1F, 1F);
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    @Override
    public void stop(ServerLevel level, WorldEvents state) {
        reset();
    }
}
