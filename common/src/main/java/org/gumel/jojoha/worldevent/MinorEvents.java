package org.gumel.jojoha.worldevent;

import dev.architectury.event.events.common.TickEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.gumel.jojoha.config.JojohaServerConfig;
import org.gumel.jojoha.entity.VampireEntity;
import org.gumel.jojoha.entity.npc.HamonStudent;
import org.gumel.jojoha.entity.npc.StandUserNpc;
import org.gumel.jojoha.registry.ModRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class MinorEvents {
    public static final String SKIRMISH = "skirmish";
    public static final String STAND_USER = "stand_user";

    private static final int CHECK_EVERY = 600;
    private static final long SKIRMISH_GAP = 72000L;
    private static final int SKIRMISH_ONE_IN = 15;
    private static final long APPROACH_GAP = 48000L;
    private static final int APPROACH_ONE_IN = 12;

    private static final int SITE_NEAREST = 26;
    private static final int SITE_FARTHEST = 38;
    private static final int SIDE_SPREAD = 4;
    private static final int SIDE_GAP = 8;
    private static final int FEWEST_FIGHTERS = 2;
    private static final int MORE_FIGHTERS = 2;
    private static final int APPROACH_NEAREST = 24;
    private static final int APPROACH_FARTHEST = 32;

    private static final Map<UUID, Long> SKIRMISH_AT = new HashMap<>();
    private static final Map<UUID, Long> APPROACH_AT = new HashMap<>();

    private MinorEvents() {
    }

    public static void init() {
        TickEvent.SERVER_POST.register(MinorEvents::tick);
    }

    private static void tick(MinecraftServer server) {
        ServerLevel level = server.overworld();
        long now = level.getGameTime();
        if (now % CHECK_EVERY != 0 || level.getDayTime() / WorldEvents.DAY < JojohaServerConfig.FIRST_EVENT_DAY.whole()) {
            return;
        }
        boolean night = WorldEvents.night(level);
        for (ServerPlayer player : List.copyOf(level.players())) {
            if (player.isCreative() || player.isSpectator()) {
                continue;
            }
            if (night && JojohaServerConfig.SKIRMISHES.on() && ready(SKIRMISH_AT, player, now)
                    && level.random.nextInt(SKIRMISH_ONE_IN) == 0 && skirmish(level, player)) {
                SKIRMISH_AT.put(player.getUUID(), now + SKIRMISH_GAP);
            } else if (!night && JojohaServerConfig.STAND_USER_APPROACH.on() && ready(APPROACH_AT, player, now)
                    && level.random.nextInt(APPROACH_ONE_IN) == 0 && approach(level, player)) {
                APPROACH_AT.put(player.getUUID(), now + APPROACH_GAP);
            }
        }
    }

    private static boolean ready(Map<UUID, Long> gaps, ServerPlayer player, long now) {
        return gaps.getOrDefault(player.getUUID(), 0L) <= now;
    }

    public static boolean force(String id, ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }
        return switch (id) {
            case SKIRMISH -> skirmish(level, player);
            case STAND_USER -> approach(level, player);
            default -> false;
        };
    }

    public static boolean skirmish(ServerLevel level, ServerPlayer player) {
        BlockPos site = VampireNight.ground(level, player.blockPosition(), SITE_NEAREST, SITE_FARTHEST);
        if (site == null) {
            return false;
        }
        BlockPos dark = VampireNight.ground(level, site, SIDE_GAP, SIDE_GAP + 2);
        if (dark == null) {
            return false;
        }
        List<HamonStudent> students = new ArrayList<>();
        List<VampireEntity> vampires = new ArrayList<>();
        int fighters = FEWEST_FIGHTERS + level.random.nextInt(MORE_FIGHTERS);
        for (int i = 0; i < fighters; i++) {
            HamonStudent student = VampireNight.spawnAround(level, ModRegistries.HAMON_STUDENT.get(), site, 0, SIDE_SPREAD, false);
            if (student != null) {
                students.add(student);
            }
            VampireEntity vampire = VampireNight.spawnAround(level, ModRegistries.VAMPIRE.get(), dark, 0, SIDE_SPREAD, false);
            if (vampire != null) {
                vampires.add(vampire);
            }
        }
        if (students.isEmpty() || vampires.isEmpty()) {
            students.forEach(HamonStudent::discard);
            vampires.forEach(VampireEntity::discard);
            return false;
        }
        for (int i = 0; i < Math.max(students.size(), vampires.size()); i++) {
            HamonStudent student = students.get(i % students.size());
            VampireEntity vampire = vampires.get(i % vampires.size());
            student.setTarget(vampire);
            vampire.setTarget(student);
        }
        students.get(0).cry();
        return true;
    }

    public static boolean approach(ServerLevel level, ServerPlayer player) {
        StandUserNpc npc = VampireNight.spawnAround(level, ModRegistries.STAND_USER.get(), player.blockPosition(),
                APPROACH_NEAREST, APPROACH_FARTHEST, false);
        if (npc == null) {
            return false;
        }
        npc.challenge(player);
        return true;
    }
}
