package org.gumel.jojoha.entity.npc;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.networking.NetworkManager;
import java.util.Locale;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;
import org.gumel.jojoha.entity.VampireEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.gumel.jojoha.network.packet.FactionStandingPacket;

public final class NpcStanding extends SavedData {
    public static final String HAMON = "hamon";
    public static final String VAMPIRE = "vampire";
    public static final int HATED = -30;
    public static final int HOSTILE = -10;
    public static final int FRIENDLY = 10;
    public static final int HONOURED = 30;
    private static final int LOUD_CHANGE = 5;
    public static final int LOWEST = -50;
    public static final int HIGHEST = 50;
    private static final int VAMPIRE_SLAIN = 1;
    private static final int STUDENT_SLAIN = -5;
    private static final double WITNESS_RANGE = 24.0;

    private static final String NAME = "jojoha_npc_standing";
    private static final Factory<NpcStanding> FACTORY = new Factory<>(NpcStanding::new, NpcStanding::load, null);

    private final Map<UUID, Integer> hamon = new HashMap<>();
    private final Map<UUID, Integer> vampire = new HashMap<>();

    public enum Level {
        HATED,
        HOSTILE,
        NEUTRAL,
        FRIENDLY,
        HONOURED;

        public static Level of(int standing) {
            if (standing <= NpcStanding.HATED) {
                return HATED;
            }
            if (standing <= NpcStanding.HOSTILE) {
                return HOSTILE;
            }
            if (standing >= NpcStanding.HONOURED) {
                return HONOURED;
            }
            return standing >= NpcStanding.FRIENDLY ? FRIENDLY : NEUTRAL;
        }

        public String key() {
            return "faction.jojoha.level." + name().toLowerCase(Locale.ROOT);
        }
    }

    private NpcStanding() {
    }

    public static void init() {
        EntityEvent.LIVING_DEATH.register((entity, source) -> {
            if (source.getEntity() instanceof ServerPlayer player) {
                slain(player, entity);
            }
            return EventResult.pass();
        });
    }

    private static void slain(ServerPlayer player, LivingEntity dead) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        if (dead instanceof HamonStudent) {
            addHamon(server, player, STUDENT_SLAIN, false);
            return;
        }
        if (!(dead instanceof VampireEntity)) {
            return;
        }
        List<HamonStudent> witnesses = dead.level().getEntitiesOfClass(HamonStudent.class,
                dead.getBoundingBox().inflate(WITNESS_RANGE), LivingEntity::isAlive);
        if (witnesses.isEmpty()) {
            return;
        }
        addHamon(server, player, VAMPIRE_SLAIN, false);
        witnesses.get(0).thank(player, dead);
    }

    public static NpcStanding get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, NAME);
    }

    public static int hamon(Player player) {
        MinecraftServer server = player.getServer();
        return server == null ? 0 : get(server).hamon.getOrDefault(player.getUUID(), 0);
    }

    public static void addHamon(MinecraftServer server, Player player, int amount, boolean quest) {
        NpcStanding standing = get(server);
        int before = standing.hamon.getOrDefault(player.getUUID(), 0);
        int after = Mth.clamp(before + amount, LOWEST, HIGHEST);
        standing.hamon.put(player.getUUID(), after);
        standing.setDirty();
        boolean loud = quest || Math.abs(amount) >= LOUD_CHANGE || Level.of(before) != Level.of(after);
        if (loud && (after != before || quest) && player instanceof ServerPlayer target) {
            NetworkManager.sendToPlayer(target, new FactionStandingPacket(HAMON, after, after - before, quest));
        }
    }

    public static int vampire(Player player) {
        MinecraftServer server = player.getServer();
        return server == null ? 0 : get(server).vampire.getOrDefault(player.getUUID(), 0);
    }

    public static void addVampire(MinecraftServer server, Player player, int amount, boolean quest) {
        NpcStanding standing = get(server);
        int before = standing.vampire.getOrDefault(player.getUUID(), 0);
        int after = Mth.clamp(before + amount, LOWEST, HIGHEST);
        standing.vampire.put(player.getUUID(), after);
        standing.setDirty();
        boolean loud = quest || Math.abs(amount) >= LOUD_CHANGE || Level.of(before) != Level.of(after);
        if (loud && (after != before || quest) && player instanceof ServerPlayer target) {
            NetworkManager.sendToPlayer(target, new FactionStandingPacket(VAMPIRE, after, after - before, quest));
        }
    }

    public static void announceVampire(ServerPlayer player) {
        NetworkManager.sendToPlayer(player, new FactionStandingPacket(VAMPIRE, vampire(player), 0, false));
    }

    public static void announce(ServerPlayer player) {
        NetworkManager.sendToPlayer(player, new FactionStandingPacket(HAMON, hamon(player), 0, false));
    }

    private static NpcStanding load(CompoundTag tag, HolderLookup.Provider registries) {
        NpcStanding standing = new NpcStanding();
        CompoundTag held = tag.getCompound("Hamon");
        for (String id : held.getAllKeys()) {
            standing.hamon.put(UUID.fromString(id), held.getInt(id));
        }
        CompoundTag coven = tag.getCompound("Vampire");
        for (String id : coven.getAllKeys()) {
            standing.vampire.put(UUID.fromString(id), coven.getInt(id));
        }
        return standing;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag held = new CompoundTag();
        hamon.forEach((id, value) -> held.putInt(id.toString(), value));
        tag.put("Hamon", held);
        CompoundTag coven = new CompoundTag();
        vampire.forEach((id, value) -> coven.putInt(id.toString(), value));
        tag.put("Vampire", coven);
        return tag;
    }
}
