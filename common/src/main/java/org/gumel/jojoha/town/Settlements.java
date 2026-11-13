package org.gumel.jojoha.town;

import dev.architectury.event.events.common.TickEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.data.Fate;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.entity.VampireEntity;
import org.gumel.jojoha.entity.npc.HamonStudent;
import org.gumel.jojoha.entity.npc.StandUserNpc;
import org.gumel.jojoha.registry.ModRegistries;
import org.gumel.jojoha.worldevent.VampireNight;
import org.gumel.jojoha.worldevent.WorldEvent;
import org.gumel.jojoha.worldevent.WorldEvents;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class Settlements extends SavedData {
    public static final double RADIUS = 64.0;
    public static final String NEWS_NONE = "town.jojoha.news.none";
    private static final String NAME = "jojoha_settlements";
    private static final Factory<Settlements> FACTORY = new Factory<>(Settlements::new, Settlements::load, null);

    private static final int FIND_EVERY = 200;
    private static final int BELL_SEARCH = 64;
    private static final int MERGE_RANGE = 80;
    private static final int LOOSE_VILLAGERS = 4;
    private static final double LOADED_RANGE = 160.0;
    private static final int ARRIVAL_DAYS = 2;
    private static final int RAVAGED_ARRIVAL_DAYS = 3;
    private static final int MARKET_DAYS = 7;
    private static final float PILGRIM_CHANCE = 0.3F;
    private static final float ARROW_CHANCE = 0.02F;
    private static final float ARROW_AFTER_METEORS = 0.1F;
    private static final int RING_EVERY = 600;
    private static final int CHATTER_EVERY = 900;
    private static final float CHATTER_CHANCE = 0.5F;
    private static final double CHATTER_PAIR = 6.0;
    private static final double CHATTER_HEAR = 24.0;
    private static final int CHATTER_LINES = 12;
    private static final int ARGUE_LINES = 6;
    private static final int ARRIVE_LINES = 4;
    private static final int PANIC_EVERY = 20;
    private static final double PANIC_RANGE = 10.0;
    private static final int PANIC_TICKS = 60;
    private static final int MOST_NEWS = 4;
    private static final int TURNED_SHARE = 3;
    private static final int RAID_REP = 10;
    private static final int SAVED_REP = 1;
    private static final double ANNOUNCE_RANGE = 128.0;

    private final List<Settlement> settlements = new ArrayList<>();
    private final List<String> news = new ArrayList<>();
    private int nextId;
    private long lastDay = -1L;
    private long lastMarketDay = -1L;
    private long lastDuskDay = -1L;
    private long meteorDay = -100L;

    private Settlements() {
    }

    public static void init() {
        TickEvent.SERVER_POST.register(Settlements::tick);
    }

    public static Settlements get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, NAME);
    }

    @Nullable
    public static Settlement at(ServerLevel level, Vec3 at) {
        if (level != level.getServer().overworld()) {
            return null;
        }
        Settlement best = null;
        double nearest = RADIUS * RADIUS;
        for (Settlement settlement : get(level.getServer()).settlements) {
            double distance = at.distanceToSqr(Vec3.atCenterOf(settlement.centre));
            if (distance <= nearest) {
                nearest = distance;
                best = settlement;
            }
        }
        return best;
    }

    @Nullable
    public static Settlement byId(MinecraftServer server, int id) {
        for (Settlement settlement : get(server).settlements) {
            if (settlement.id == id) {
                return settlement;
            }
        }
        return null;
    }

    @Nullable
    public static Settlement nearestOther(MinecraftServer server, BlockPos from, int excluded, double fewest) {
        return get(server).settlements.stream()
                .filter(settlement -> settlement.id != excluded)
                .filter(settlement -> settlement.centre.distSqr(from) >= fewest * fewest)
                .min(Comparator.comparingDouble(settlement -> settlement.centre.distSqr(from)))
                .orElse(null);
    }

    public static List<String> news(MinecraftServer server) {
        return List.copyOf(get(server).news);
    }

    public static void report(MinecraftServer server, String key) {
        Settlements state = get(server);
        state.news.remove(key);
        state.news.add(0, key);
        while (state.news.size() > MOST_NEWS) {
            state.news.remove(state.news.size() - 1);
        }
        state.setDirty();
    }

    public static void meteorsFell(MinecraftServer server) {
        Settlements state = get(server);
        state.meteorDay = server.overworld().getDayTime() / WorldEvents.DAY;
        state.setDirty();
        report(server, "town.jojoha.news.meteor");
    }

    public static int reputation(ServerLevel level, Vec3 at, ServerPlayer player) {
        Settlement settlement = at(level, at);
        return settlement == null ? 0 : settlement.reputation(player);
    }

    public static void addReputation(ServerPlayer player, @Nullable Settlement settlement, int amount) {
        if (settlement == null || amount == 0 || player.getServer() == null) {
            return;
        }
        int before = settlement.reputation(player);
        int after = settlement.addReputation(player, amount);
        get(player.getServer()).setDirty();
        if (band(before) != band(after)) {
            player.displayClientMessage(Component.translatable("town.jojoha.rep." + band(after), settlement.name())
                    .withStyle(after > before ? ChatFormatting.GREEN : ChatFormatting.RED), false);
        }
    }

    public static String band(int reputation) {
        if (reputation <= Settlement.REP_DISLIKED) {
            return "disliked";
        }
        if (reputation >= Settlement.REP_LOVED) {
            return "loved";
        }
        return reputation >= Settlement.REP_LIKED ? "liked" : "neutral";
    }

    private static void tick(MinecraftServer server) {
        ServerLevel level = server.overworld();
        int now = server.getTickCount();
        Settlements state = get(server);
        if (now % FIND_EVERY == 0) {
            state.discover(level);
        }
        if (now % PANIC_EVERY == 0) {
            panic(level);
        }
        if (now % RING_EVERY == 0 && WorldEvents.active(server, WorldEvent.VAMPIRE_NIGHT)) {
            state.ringBells(level);
        }
        if (now % CHATTER_EVERY == 0) {
            state.chatter(level);
        }
        long day = level.getDayTime() / WorldEvents.DAY;
        if (day != state.lastDay) {
            boolean first = state.lastDay < 0;
            state.lastDay = day;
            state.setDirty();
            if (!first) {
                state.newDay(level, day);
            }
        }
        if (WorldEvents.night(level) && state.lastDuskDay != day) {
            state.lastDuskDay = day;
            state.setDirty();
            state.dusk(level);
        }
    }

    private void discover(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            BlockPos here = player.blockPosition();
            Optional<BlockPos> bell = level.getPoiManager().findClosest(holder -> holder.is(PoiTypes.MEETING), here,
                    BELL_SEARCH, PoiManager.Occupancy.ANY);
            BlockPos centre = bell.orElse(null);
            if (centre == null) {
                List<Villager> loose = level.getEntitiesOfClass(Villager.class, player.getBoundingBox().inflate(48.0),
                        LivingEntity::isAlive);
                if (loose.size() < LOOSE_VILLAGERS) {
                    continue;
                }
                double x = loose.stream().mapToDouble(Villager::getX).average().orElse(player.getX());
                double z = loose.stream().mapToDouble(Villager::getZ).average().orElse(player.getZ());
                centre = BlockPos.containing(x, player.getY(), z);
            }
            Settlement settlement = claim(level, centre);
            int residents = residents(level, settlement).size();
            if (residents != settlement.population) {
                settlement.population = residents;
                setDirty();
            }
            if (!settlement.ravaged && residents > settlement.peak) {
                settlement.peak = residents;
                setDirty();
            }
            if (settlement.ravaged && residents >= settlement.peak) {
                settlement.ravaged = false;
                setDirty();
                report(level.getServer(), "town.jojoha.news.recovered");
            }
        }
    }

    private Settlement claim(ServerLevel level, BlockPos centre) {
        for (Settlement settlement : settlements) {
            if (settlement.centre.distSqr(centre) <= (double) MERGE_RANGE * MERGE_RANGE) {
                return settlement;
            }
        }
        Settlement made = new Settlement(nextId++, centre.immutable(), level.random.nextInt(Settlement.NAMES));
        settlements.add(made);
        setDirty();
        return made;
    }

    public static List<Villager> residents(ServerLevel level, Settlement settlement) {
        return level.getEntitiesOfClass(Villager.class, new AABB(settlement.centre).inflate(RADIUS),
                villager -> villager.isAlive() && !villager.isBaby());
    }

    private List<Settlement> loaded(ServerLevel level) {
        List<Settlement> near = new ArrayList<>();
        for (Settlement settlement : settlements) {
            Vec3 centre = Vec3.atCenterOf(settlement.centre);
            if (level.players().stream().anyMatch(player -> player.position().distanceToSqr(centre) <= LOADED_RANGE * LOADED_RANGE)
                    && level.isLoaded(settlement.centre)) {
                near.add(settlement);
            }
        }
        return near;
    }

    private void newDay(ServerLevel level, long day) {
        List<Settlement> near = loaded(level);
        for (Settlement settlement : near) {
            int every = settlement.ravaged ? RAVAGED_ARRIVAL_DAYS : ARRIVAL_DAYS;
            if (settlement.population < settlement.peak && day - settlement.lastArrivalDay >= every) {
                arrive(level, settlement);
                settlement.lastArrivalDay = day;
                setDirty();
            }
            if (level.random.nextFloat() < PILGRIM_CHANCE) {
                HamonStudent pilgrim = VampireNight.spawnAround(level, ModRegistries.HAMON_STUDENT.get(), settlement.centre,
                        24, 40, false);
                if (pilgrim != null) {
                    pilgrim.makePilgrim();
                }
            }
            float arrow = ARROW_CHANCE + (day - meteorDay <= 1 ? ARROW_AFTER_METEORS : 0F);
            if (level.random.nextFloat() < arrow) {
                strayArrow(level, settlement);
            }
        }
        if (day - lastMarketDay >= MARKET_DAYS && !near.isEmpty()) {
            Settlement busiest = near.stream().max(Comparator.comparingInt(Settlement::population)).orElse(null);
            WanderingTrader trader = VampireNight.spawnAround(level, EntityType.WANDERING_TRADER, busiest.centre, 4, 12, false);
            if (trader != null) {
                trader.setDespawnDelay((int) WorldEvents.DAY);
                lastMarketDay = day;
                setDirty();
                announce(level, busiest, Component.translatable("town.jojoha.market", busiest.name())
                        .withStyle(ChatFormatting.GOLD));
            }
        }
    }

    public static boolean arrive(ServerLevel level, Settlement settlement) {
        Villager newcomer = VampireNight.spawnAround(level, EntityType.VILLAGER, settlement.centre, 20, 32, false);
        if (newcomer == null) {
            return false;
        }
        newcomer.setVillagerData(newcomer.getVillagerData().setProfession(VillagerProfession.NONE));
        newcomer.getBrain().setMemory(MemoryModuleType.MEETING_POINT,
                GlobalPos.of(level.dimension(), settlement.centre));
        Residents.speak(newcomer, "town.jojoha.arrive." + level.random.nextInt(ARRIVE_LINES));
        return true;
    }

    private void strayArrow(ServerLevel level, Settlement settlement) {
        List<Villager> residents = residents(level, settlement);
        if (residents.isEmpty()) {
            return;
        }
        Villager struck = residents.get(level.random.nextInt(residents.size()));
        StandUserNpc awakened = ModRegistries.STAND_USER.get().create(level);
        if (awakened == null) {
            return;
        }
        awakened.moveTo(struck.getX(), struck.getY(), struck.getZ(), struck.getYRot(), 0F);
        awakened.finalizeSpawn(level, level.getCurrentDifficultyAt(struck.blockPosition()), MobSpawnType.EVENT, null);
        int best = Integer.MIN_VALUE;
        for (ServerPlayer player : level.players()) {
            if (settlement.holds(player.position(), LOADED_RANGE)) {
                best = Math.max(best, settlement.reputation(player));
            }
        }
        boolean friendly = best == Integer.MIN_VALUE ? level.random.nextBoolean() : best >= Settlement.REP_LIKED
                || best > Settlement.REP_DISLIKED && level.random.nextBoolean();
        awakened.awaken(friendly, settlement.centre);
        level.addFreshEntity(awakened);
        struck.discard();
        report(level.getServer(), "town.jojoha.news.arrow");
        announce(level, settlement, Component.translatable(friendly ? "town.jojoha.arrow.friendly" : "town.jojoha.arrow.rogue",
                settlement.name()).withStyle(friendly ? ChatFormatting.AQUA : ChatFormatting.DARK_RED));
    }

    private void dusk(ServerLevel level) {
        for (Settlement settlement : loaded(level)) {
            if (settlement.turnedPending <= 0) {
                continue;
            }
            for (int i = 0; i < settlement.turnedPending; i++) {
                VampireEntity turned = VampireNight.spawnAround(level, ModRegistries.VAMPIRE.get(), settlement.centre, 6, 24, false);
                if (turned != null) {
                    turned.setCustomName(Component.translatable("town.jojoha.turned"));
                }
            }
            announce(level, settlement, Component.translatable("town.jojoha.turned.back", settlement.name())
                    .withStyle(ChatFormatting.DARK_RED));
            settlement.turnedPending = 0;
            setDirty();
        }
    }

    private void ringBells(ServerLevel level) {
        BellBlock bell = (BellBlock) Blocks.BELL;
        for (Settlement settlement : loaded(level)) {
            if (level.getBlockState(settlement.centre).is(Blocks.BELL)) {
                bell.attemptToRing(level, settlement.centre, null);
            }
        }
    }

    private void chatter(ServerLevel level) {
        if (WorldEvents.night(level)) {
            return;
        }
        for (Settlement settlement : loaded(level)) {
            if (level.random.nextFloat() > CHATTER_CHANCE) {
                continue;
            }
            List<Villager> residents = residents(level, settlement);
            if (residents.size() < 2) {
                continue;
            }
            Villager first = residents.get(level.random.nextInt(residents.size()));
            Villager second = residents.stream()
                    .filter(other -> other != first && other.distanceTo(first) <= CHATTER_PAIR && !other.isSleeping())
                    .findFirst().orElse(null);
            if (second == null || first.isSleeping() || level.players().stream()
                    .noneMatch(player -> player.distanceTo(first) <= CHATTER_HEAR)) {
                continue;
            }
            boolean rivals = Residents.rivals(first, second);
            first.getLookControl().setLookAt(second);
            second.getLookControl().setLookAt(first);
            if (rivals) {
                Residents.speak(first, "town.jojoha.argue." + level.random.nextInt(ARGUE_LINES));
            } else {
                Residents.speak(first, "town.jojoha.chatter." + level.random.nextInt(CHATTER_LINES));
            }
        }
    }

    private static void panic(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || player.isCreative() || !Fate.pillar(PlayerDataAccess.get(player))) {
                continue;
            }
            for (Villager villager : level.getEntitiesOfClass(Villager.class, player.getBoundingBox().inflate(PANIC_RANGE),
                    LivingEntity::isAlive)) {
                villager.getBrain().setMemoryWithExpiry(MemoryModuleType.HURT_BY, player.damageSources().playerAttack(player), PANIC_TICKS);
                villager.getBrain().setMemoryWithExpiry(MemoryModuleType.HURT_BY_ENTITY, player, PANIC_TICKS);
            }
        }
    }

    public static void raidWon(ServerLevel level, BlockPos bell, List<ServerPlayer> defenders) {
        Settlement settlement = at(level, Vec3.atCenterOf(bell));
        report(level.getServer(), "town.jojoha.news.raid_won");
        if (settlement == null) {
            return;
        }
        Settlements state = get(level.getServer());
        settlement.peak++;
        settlement.lastArrivalDay = -100L;
        state.setDirty();
        for (ServerPlayer player : defenders) {
            addReputation(player, settlement, RAID_REP);
        }
    }

    public static void raidLost(ServerLevel level, BlockPos bell, int before) {
        Settlements state = get(level.getServer());
        Settlement settlement = at(level, Vec3.atCenterOf(bell));
        if (settlement == null) {
            settlement = state.claim(level, bell);
        }
        List<Villager> left = residents(level, settlement);
        int keep = Math.max(0, Math.max(before, left.size()) / 2);
        int lost = Math.max(0, Math.max(before, settlement.peak) - keep);
        int toTake = Math.max(0, left.size() - keep);
        for (int i = 0; i < toTake && i < left.size(); i++) {
            left.get(i).kill();
        }
        settlement.peak = Math.max(settlement.peak, before);
        settlement.ravaged = true;
        settlement.turnedPending = Math.max(1, lost / TURNED_SHARE);
        settlement.raidedDay = level.getDayTime() / WorldEvents.DAY;
        settlement.lastArrivalDay = settlement.raidedDay;
        state.setDirty();
        report(level.getServer(), "town.jojoha.news.raid_lost");
        state.announce(level, settlement, Component.translatable("town.jojoha.ravaged", settlement.name())
                .withStyle(ChatFormatting.DARK_RED));
        level.playSound(null, bell, SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 4F, 0.4F);
    }

    public static void vampireSlain(ServerPlayer player, LivingEntity dead) {
        if (!(dead.level() instanceof ServerLevel level)) {
            return;
        }
        Settlement settlement = at(level, dead.position());
        if (settlement != null && !residents(level, settlement).isEmpty()) {
            addReputation(player, settlement, SAVED_REP);
        }
    }

    public static void rebuilt(ServerLevel level, @Nullable Settlement settlement) {
        if (settlement != null && settlement.population < settlement.peak) {
            arrive(level, settlement);
        }
    }

    private void announce(ServerLevel level, Settlement settlement, Component line) {
        for (ServerPlayer player : level.players()) {
            if (settlement.holds(player.position(), ANNOUNCE_RANGE)) {
                player.sendSystemMessage(line);
            }
        }
    }

    private static Settlements load(CompoundTag tag, HolderLookup.Provider registries) {
        Settlements state = new Settlements();
        ListTag list = tag.getList("Settlements", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            state.settlements.add(Settlement.load(list.getCompound(i)));
        }
        ListTag news = tag.getList("News", Tag.TAG_STRING);
        for (int i = 0; i < news.size(); i++) {
            state.news.add(news.getString(i));
        }
        state.nextId = tag.getInt("NextId");
        state.lastDay = tag.contains("LastDay") ? tag.getLong("LastDay") : -1L;
        state.lastMarketDay = tag.contains("Market") ? tag.getLong("Market") : -1L;
        state.lastDuskDay = tag.contains("Dusk") ? tag.getLong("Dusk") : -1L;
        state.meteorDay = tag.contains("Meteors") ? tag.getLong("Meteors") : -100L;
        return state;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        settlements.forEach(settlement -> list.add(settlement.save()));
        tag.put("Settlements", list);
        ListTag newsTag = new ListTag();
        news.forEach(key -> newsTag.add(StringTag.valueOf(key)));
        tag.put("News", newsTag);
        tag.putInt("NextId", nextId);
        tag.putLong("LastDay", lastDay);
        tag.putLong("Market", lastMarketDay);
        tag.putLong("Dusk", lastDuskDay);
        tag.putLong("Meteors", meteorDay);
        return tag;
    }
}
