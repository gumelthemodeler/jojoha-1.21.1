package org.gumel.jojoha.town;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;
import org.gumel.jojoha.entity.npc.JojoNpc;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class NpcMemory extends SavedData {
    private static final String NAME = "jojoha_npc_memory";
    private static final Factory<NpcMemory> FACTORY = new Factory<>(NpcMemory::new, NpcMemory::load, null);
    private static final long GIFT_GAP = 24000L;

    private final Map<UUID, Map<UUID, Integer>> trust = new HashMap<>();
    private final Map<UUID, Map<UUID, Long>> gifted = new HashMap<>();
    private final Map<UUID, UUID> companions = new HashMap<>();
    private final Map<UUID, Set<UUID>> grudges = new HashMap<>();
    private final Map<String, Map<JojoNpc.Small, Integer>> talkedToday = new HashMap<>();
    private long talkDay = -1L;

    private NpcMemory() {
    }

    public static NpcMemory get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, NAME);
    }

    @Nullable
    private static NpcMemory of(Entity entity) {
        MinecraftServer server = entity.getServer();
        return server == null ? null : get(server);
    }

    public static int trust(Entity npc, Player player) {
        NpcMemory memory = of(npc);
        return memory == null ? 0 : memory.trust.getOrDefault(npc.getUUID(), Map.of()).getOrDefault(player.getUUID(), 0);
    }

    public static int addTrust(Entity npc, Player player, int amount) {
        NpcMemory memory = of(npc);
        if (memory == null) {
            return 0;
        }
        Map<UUID, Integer> held = memory.trust.computeIfAbsent(npc.getUUID(), id -> new HashMap<>());
        int after = Mth.clamp(held.getOrDefault(player.getUUID(), 0) + amount, JojoNpc.TRUST_MIN, JojoNpc.TRUST_MAX);
        held.put(player.getUUID(), after);
        memory.setDirty();
        return after;
    }

    public static void seedTrust(Entity npc, UUID player, int value) {
        NpcMemory memory = of(npc);
        if (memory == null) {
            return;
        }
        memory.trust.computeIfAbsent(npc.getUUID(), id -> new HashMap<>()).putIfAbsent(player, value);
        memory.setDirty();
    }

    public static boolean canGift(Entity npc, Player player) {
        NpcMemory memory = of(npc);
        if (memory == null) {
            return false;
        }
        Long at = memory.gifted.getOrDefault(npc.getUUID(), Map.of()).get(player.getUUID());
        return at == null || npc.level().getGameTime() - at >= GIFT_GAP;
    }

    public static void gifted(Entity npc, Player player) {
        NpcMemory memory = of(npc);
        if (memory == null) {
            return;
        }
        memory.gifted.computeIfAbsent(npc.getUUID(), id -> new HashMap<>()).put(player.getUUID(), npc.level().getGameTime());
        memory.setDirty();
    }

    public static int talkedToday(Entity npc, Player player, JojoNpc.Small small) {
        NpcMemory memory = of(npc);
        if (memory == null) {
            return 0;
        }
        long today = npc.level().getDayTime() / 24000L;
        if (today != memory.talkDay) {
            memory.talkDay = today;
            memory.talkedToday.clear();
        }
        Map<JojoNpc.Small, Integer> counts = memory.talkedToday.computeIfAbsent(npc.getUUID() + "/" + player.getUUID(),
                key -> new EnumMap<>(JojoNpc.Small.class));
        int times = counts.getOrDefault(small, 0);
        counts.put(small, times + 1);
        return times;
    }

    @Nullable
    public static UUID companion(MinecraftServer server, Player player) {
        return get(server).companions.get(player.getUUID());
    }

    public static void setCompanion(MinecraftServer server, Player player, @Nullable UUID npc) {
        NpcMemory memory = get(server);
        if (npc == null) {
            memory.companions.remove(player.getUUID());
        } else {
            memory.companions.put(player.getUUID(), npc);
        }
        memory.setDirty();
    }

    public static boolean remembers(Entity npc, Player player) {
        NpcMemory memory = of(npc);
        return memory != null && memory.grudges.getOrDefault(npc.getUUID(), Set.of()).contains(player.getUUID());
    }

    public static void remember(Entity npc, Player player) {
        NpcMemory memory = of(npc);
        if (memory == null) {
            return;
        }
        memory.grudges.computeIfAbsent(npc.getUUID(), id -> new HashSet<>()).add(player.getUUID());
        memory.setDirty();
    }

    public static void forget(Entity npc) {
        NpcMemory memory = of(npc);
        if (memory == null) {
            return;
        }
        memory.trust.remove(npc.getUUID());
        memory.gifted.remove(npc.getUUID());
        memory.grudges.remove(npc.getUUID());
        memory.companions.values().removeIf(npc.getUUID()::equals);
        memory.setDirty();
    }

    private static NpcMemory load(CompoundTag tag, HolderLookup.Provider registries) {
        NpcMemory memory = new NpcMemory();
        CompoundTag trust = tag.getCompound("Trust");
        for (String npc : trust.getAllKeys()) {
            CompoundTag players = trust.getCompound(npc);
            Map<UUID, Integer> held = new HashMap<>();
            for (String player : players.getAllKeys()) {
                held.put(UUID.fromString(player), players.getInt(player));
            }
            memory.trust.put(UUID.fromString(npc), held);
        }
        CompoundTag gifted = tag.getCompound("Gifted");
        for (String npc : gifted.getAllKeys()) {
            CompoundTag players = gifted.getCompound(npc);
            Map<UUID, Long> held = new HashMap<>();
            for (String player : players.getAllKeys()) {
                held.put(UUID.fromString(player), players.getLong(player));
            }
            memory.gifted.put(UUID.fromString(npc), held);
        }
        CompoundTag companions = tag.getCompound("Companions");
        for (String player : companions.getAllKeys()) {
            memory.companions.put(UUID.fromString(player), companions.getUUID(player));
        }
        CompoundTag grudges = tag.getCompound("Grudges");
        for (String npc : grudges.getAllKeys()) {
            CompoundTag players = grudges.getCompound(npc);
            Set<UUID> held = new HashSet<>();
            for (String player : players.getAllKeys()) {
                held.add(UUID.fromString(player));
            }
            memory.grudges.put(UUID.fromString(npc), held);
        }
        return memory;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag trustTag = new CompoundTag();
        trust.forEach((npc, players) -> {
            CompoundTag inner = new CompoundTag();
            players.forEach((player, value) -> inner.putInt(player.toString(), value));
            trustTag.put(npc.toString(), inner);
        });
        tag.put("Trust", trustTag);
        CompoundTag giftedTag = new CompoundTag();
        gifted.forEach((npc, players) -> {
            CompoundTag inner = new CompoundTag();
            players.forEach((player, at) -> inner.putLong(player.toString(), at));
            giftedTag.put(npc.toString(), inner);
        });
        tag.put("Gifted", giftedTag);
        CompoundTag companionTag = new CompoundTag();
        companions.forEach((player, npc) -> companionTag.putUUID(player.toString(), npc));
        tag.put("Companions", companionTag);
        CompoundTag grudgeTag = new CompoundTag();
        grudges.forEach((npc, players) -> {
            CompoundTag inner = new CompoundTag();
            players.forEach(player -> inner.putBoolean(player.toString(), true));
            grudgeTag.put(npc.toString(), inner);
        });
        tag.put("Grudges", grudgeTag);
        return tag;
    }
}
