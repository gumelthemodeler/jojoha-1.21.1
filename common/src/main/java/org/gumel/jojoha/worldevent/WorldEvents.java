package org.gumel.jojoha.worldevent;

import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.networking.NetworkManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import org.gumel.jojoha.config.JojohaServerConfig;
import org.gumel.jojoha.entity.npc.Quests;
import org.gumel.jojoha.network.packet.WorldEventPacket;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class WorldEvents extends SavedData {
    public static final int DAY = 24000;
    public static final int DUSK = 12500;
    public static final int DAWN = 23500;
    public static final int NIGHT_LENGTH = DAWN - DUSK;

    private static final String NAME = "jojoha_world_events";
    private static final Factory<WorldEvents> FACTORY = new Factory<>(WorldEvents::new, WorldEvents::load, null);

    public interface Handler {
        boolean canRoll(ServerLevel level);

        void start(ServerLevel level, WorldEvents state);

        void tick(ServerLevel level, WorldEvents state);

        void stop(ServerLevel level, WorldEvents state);
    }

    private static final Map<WorldEvent, Handler> HANDLERS = new EnumMap<>(WorldEvent.class);

    @Nullable
    private WorldEvent active;
    private long endsAt;
    private long rolledDay = -1L;
    private boolean forced;
    @Nullable
    private BlockPos focus;
    private final Map<WorldEvent, Long> lastDay = new EnumMap<>(WorldEvent.class);

    private WorldEvents() {
    }

    public static void init() {
        HANDLERS.put(WorldEvent.VAMPIRE_NIGHT, new VampireNight());
        HANDLERS.put(WorldEvent.METEOR_SHOWER, new MeteorShower());
        HANDLERS.put(WorldEvent.VAMPIRE_RAID, new VampireRaid());
        TickEvent.SERVER_POST.register(WorldEvents::tick);
        PlayerEvent.PLAYER_JOIN.register(player -> {
            MinecraftServer server = player.getServer();
            if (server != null) {
                NetworkManager.sendToPlayer(player, packet(get(server)));
            }
        });
    }

    public static void handle(WorldEvent event, Handler handler) {
        HANDLERS.put(event, handler);
    }

    public static WorldEvents get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, NAME);
    }

    public static boolean active(MinecraftServer server, WorldEvent event) {
        return get(server).active == event;
    }

    @Nullable
    public WorldEvent active() {
        return active;
    }

    @Nullable
    public BlockPos focus() {
        return focus;
    }

    public void focus(@Nullable BlockPos at) {
        focus = at;
        setDirty();
    }

    public boolean forced() {
        return forced;
    }

    public long endsAt() {
        return endsAt;
    }

    public static boolean night(ServerLevel level) {
        long time = level.getDayTime() % DAY;
        return time >= DUSK && time < DAWN;
    }

    private static int odds(WorldEvent event) {
        return switch (event) {
            case VAMPIRE_NIGHT -> JojohaServerConfig.VAMPIRE_NIGHT.on() ? JojohaServerConfig.VAMPIRE_NIGHT_ODDS.whole() : 0;
            case METEOR_SHOWER -> JojohaServerConfig.METEOR_SHOWER.on() ? JojohaServerConfig.METEOR_SHOWER_ODDS.whole() : 0;
            case VAMPIRE_RAID -> JojohaServerConfig.VAMPIRE_RAID.on() ? JojohaServerConfig.VAMPIRE_RAID_ODDS.whole() : 0;
        };
    }

    private static void tick(MinecraftServer server) {
        ServerLevel level = server.overworld();
        WorldEvents state = get(server);
        if (state.active != null) {
            Handler handler = HANDLERS.get(state.active);
            if (handler == null || level.getGameTime() >= state.endsAt) {
                state.stop(server);
            } else {
                handler.tick(level, state);
            }
        }
        long day = level.getDayTime() / DAY;
        if (night(level) && state.rolledDay != day) {
            state.rolledDay = day;
            state.setDirty();
            state.roll(server, day);
        }
    }

    private void roll(MinecraftServer server, long day) {
        if (active != null || day < JojohaServerConfig.FIRST_EVENT_DAY.whole()) {
            return;
        }
        ServerLevel level = server.overworld();
        List<WorldEvent> order = new ArrayList<>(HANDLERS.keySet());
        Collections.shuffle(order);
        for (WorldEvent event : order) {
            int odds = odds(event);
            if (odds <= 0 || lastDay.getOrDefault(event, Long.MIN_VALUE / 2) >= day - 1) {
                continue;
            }
            if (!HANDLERS.get(event).canRoll(level)) {
                continue;
            }
            if (level.random.nextInt(odds) == 0) {
                start(server, event, false);
                return;
            }
        }
    }

    public boolean start(MinecraftServer server, WorldEvent event) {
        return start(server, event, false);
    }

    public boolean start(MinecraftServer server, WorldEvent event, boolean forced) {
        Handler handler = HANDLERS.get(event);
        if (handler == null) {
            return false;
        }
        if (active != null) {
            stop(server);
        }
        ServerLevel level = server.overworld();
        if (forced && !night(level)) {
            long today = level.getDayTime() - level.getDayTime() % DAY;
            level.setDayTime(today + DUSK);
        }
        long time = level.getDayTime() % DAY;
        this.forced = forced;
        active = event;
        focus = null;
        endsAt = level.getGameTime() + (night(level) ? DAWN - time : NIGHT_LENGTH);
        lastDay.put(event, level.getDayTime() / DAY);
        setDirty();
        handler.start(level, this);
        if (active != event) {
            return false;
        }
        server.getPlayerList().broadcastSystemMessage(Component.translatable("event.jojoha." + event.id() + ".start")
                .withStyle(ChatFormatting.DARK_RED), false);
        sync(server);
        return true;
    }

    public void stop(MinecraftServer server) {
        WorldEvent ending = active;
        if (ending == null) {
            return;
        }
        active = null;
        setDirty();
        Handler handler = HANDLERS.get(ending);
        if (handler != null) {
            handler.stop(server.overworld(), this);
        }
        Quests.eventEnded(server, ending);
        focus = null;
        server.getPlayerList().broadcastSystemMessage(Component.translatable("event.jojoha." + ending.id() + ".end")
                .withStyle(ChatFormatting.GRAY), false);
        sync(server);
    }

    public void cancel(MinecraftServer server) {
        WorldEvent ending = active;
        active = null;
        focus = null;
        setDirty();
        if (ending != null && HANDLERS.get(ending) != null) {
            HANDLERS.get(ending).stop(server.overworld(), this);
        }
        sync(server);
    }

    private static WorldEventPacket packet(WorldEvents state) {
        return new WorldEventPacket(state.active == null ? "" : state.active.id());
    }

    private void sync(MinecraftServer server) {
        WorldEventPacket packet = packet(this);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            NetworkManager.sendToPlayer(player, packet);
        }
    }

    private static WorldEvents load(CompoundTag tag, HolderLookup.Provider registries) {
        WorldEvents state = new WorldEvents();
        state.active = WorldEvent.byId(tag.getString("Active"));
        state.endsAt = tag.getLong("EndsAt");
        state.forced = tag.getBoolean("Forced");
        state.rolledDay = tag.contains("RolledDay") ? tag.getLong("RolledDay") : -1L;
        state.focus = NbtUtils.readBlockPos(tag, "Focus").orElse(null);
        CompoundTag last = tag.getCompound("LastDay");
        for (WorldEvent event : WorldEvent.values()) {
            if (last.contains(event.id())) {
                state.lastDay.put(event, last.getLong(event.id()));
            }
        }
        return state;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putString("Active", active == null ? "" : active.id());
        tag.putLong("EndsAt", endsAt);
        tag.putBoolean("Forced", forced);
        tag.putLong("RolledDay", rolledDay);
        if (focus != null) {
            tag.put("Focus", NbtUtils.writeBlockPos(focus));
        }
        CompoundTag last = new CompoundTag();
        lastDay.forEach((event, day) -> last.putLong(event.id(), day));
        tag.put("LastDay", last);
        return tag;
    }
}
