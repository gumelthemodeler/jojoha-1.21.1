package org.gumel.jojoha.stand.heaven;

import dev.architectury.event.events.common.TickEvent;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.registry.ModRegistries;
import org.gumel.jojoha.stand.skill.TimeStopSystem;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class HeavenTimeStop {
    public static final int HALF_LIFE_TICKS = 30;
    public static final double MOST_SLOWED = 0.97;
    private static final int MOTE_EVERY = 4;
    private static final double MOTES_AT_MOST = 14.0;

    private static final ResourceLocation SLOW =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "heaven_time_stop");
    private static final List<Holder<Attribute>> SLOWED = List.of(Attributes.MOVEMENT_SPEED,
            Attributes.JUMP_STRENGTH);

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    private HeavenTimeStop() {
    }

    static void init() {
        TickEvent.SERVER_POST.register(HeavenTimeStop::tick);
    }

    public static void begin(ServerPlayer user, int castTicks, int stopTicks) {
        long now = user.serverLevel().getGameTime();
        release(user.getUUID());
        SESSIONS.put(user.getUUID(), new Session(now + castTicks, now + castTicks + stopTicks, new HashMap<>()));
    }

    public static boolean holding(UUID userId) {
        return SESSIONS.containsKey(userId);
    }

    public static void release(UUID userId) {
        Session session = SESSIONS.remove(userId);
        if (session == null) {
            return;
        }
        MinecraftServer server = Heaven.server();
        if (server == null) {
            return;
        }
        for (UUID caught : session.caught.keySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(caught);
            if (player != null) {
                unslow(player);
            }
        }
    }

    private static void tick(MinecraftServer server) {
        if (SESSIONS.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, Session>> iterator = SESSIONS.entrySet().iterator();
        Set<UUID> ended = new HashSet<>();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Session> entry = iterator.next();
            ServerPlayer user = server.getPlayerList().getPlayer(entry.getKey());
            Session session = entry.getValue();
            long now = user == null ? 0L : user.serverLevel().getGameTime();
            if (user == null || !user.isAlive() || now >= session.endsAt
                    || (now > session.stopsAt + 2 && !TimeStopSystem.isStopped(user.getUUID()))) {
                ended.add(entry.getKey());
                continue;
            }
            if (now < session.stopsAt) {
                continue;
            }
            slowMovers(user, session, now);
        }
        for (UUID id : ended) {
            release(id);
        }
    }

    private static void slowMovers(ServerPlayer user, Session session, long now) {
        Set<UUID> inside = new HashSet<>();
        for (ServerPlayer other : user.serverLevel().players()) {
            if (other == user || !other.isAlive() || other.isSpectator()
                    || PlayerDataAccess.get(other).isTimeStopFrozen()) {
                continue;
            }
            inside.add(other.getUUID());
            long since = session.caught.computeIfAbsent(other.getUUID(), id -> now);
            double slowed = Math.min(MOST_SLOWED, 1.0 - Math.pow(0.5, (now - since) / (double) HALF_LIFE_TICKS));
            slow(other, slowed);
            if ((now - since) % MOTE_EVERY == 0) {
                user.serverLevel().sendParticles(ModRegistries.TIMESTOP_MOTE.get(),
                        other.getX(), other.getY() + other.getBbHeight() * 0.5, other.getZ(),
                        2 + (int) Math.round(slowed * MOTES_AT_MOST), 0.5, other.getBbHeight() * 0.4, 0.5, 0.02);
            }
        }
        Iterator<UUID> caught = session.caught.keySet().iterator();
        while (caught.hasNext()) {
            UUID id = caught.next();
            if (!inside.contains(id)) {
                ServerPlayer gone = user.getServer().getPlayerList().getPlayer(id);
                if (gone != null) {
                    unslow(gone);
                }
                caught.remove();
            }
        }
    }

    private static void slow(ServerPlayer player, double amount) {
        for (Holder<Attribute> attribute : SLOWED) {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null) {
                continue;
            }
            instance.removeModifier(SLOW);
            instance.addTransientModifier(new AttributeModifier(SLOW, -amount,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    private static void unslow(ServerPlayer player) {
        for (Holder<Attribute> attribute : SLOWED) {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance != null) {
                instance.removeModifier(SLOW);
            }
        }
    }

    private record Session(long stopsAt, long endsAt, Map<UUID, Long> caught) {
    }
}
