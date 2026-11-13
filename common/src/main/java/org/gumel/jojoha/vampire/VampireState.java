package org.gumel.jojoha.vampire;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.gumel.jojoha.combat.Climbing;
import org.gumel.jojoha.network.NetworkHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class VampireState {

    private static final Map<UUID, Long> DRIVE_UNTIL = new HashMap<>();
    private static final Map<UUID, Long> SUBZERO_UNTIL = new HashMap<>();

    private static final Set<UUID> CRAWLING = new HashSet<>();

    private static final Map<UUID, Long> REFORM_UNTIL = new HashMap<>();

    private static final Map<UUID, Long> REFORMED_UNTIL = new HashMap<>();

    private static final Map<UUID, Bud> FLESH_BUDS = new HashMap<>();

    private static final Map<UUID, Set<UUID>> BUDS_BY_CASTER = new HashMap<>();

    private record Bud(UUID caster, int victimId, long until) {
    }

    private VampireState() {
    }

    public static void chargeDrive(ServerPlayer player, int ticks) {
        DRIVE_UNTIL.put(player.getUUID(), player.level().getGameTime() + ticks);
    }

    public static boolean hasDrive(Player player) {
        return until(DRIVE_UNTIL, player) > player.level().getGameTime();
    }

    public static void consumeDrive(Player player) {
        DRIVE_UNTIL.remove(player.getUUID());
    }

    public static boolean isCrawling(Player player) {
        return CRAWLING.contains(player.getUUID());
    }

    public static void setCrawling(ServerPlayer player, boolean crawling) {
        if (crawling) {
            CRAWLING.add(player.getUUID());
        } else {
            CRAWLING.remove(player.getUUID());
        }
        NetworkHandler.sendClimbing(player,
                Climbing.Grip.WALL, crawling);
    }

    public static boolean isPrimedToReform(Player player) {
        return until(REFORM_UNTIL, player) > player.level().getGameTime();
    }

    public static void primeReform(ServerPlayer player, int ticks) {
        REFORM_UNTIL.put(player.getUUID(), player.level().getGameTime() + ticks);
    }

    public static void spendReform(Player player) {
        REFORM_UNTIL.remove(player.getUUID());
    }

    public static void graceAfterReform(ServerPlayer player, int ticks) {
        REFORMED_UNTIL.put(player.getUUID(), player.level().getGameTime() + ticks);
    }

    public static boolean isReforming(Player player) {
        return until(REFORMED_UNTIL, player) > player.level().getGameTime();
    }

    public static void freeze(ServerPlayer player, int ticks) {
        SUBZERO_UNTIL.put(player.getUUID(), player.level().getGameTime() + ticks);
    }

    public static void thaw(Player player) {
        SUBZERO_UNTIL.remove(player.getUUID());
    }

    public static boolean runOut(Player player) {
        Long until = SUBZERO_UNTIL.get(player.getUUID());
        if (until == null || until > player.level().getGameTime()) {
            return false;
        }
        SUBZERO_UNTIL.remove(player.getUUID());
        return true;
    }

    public static boolean isFrozen(Player player) {
        return until(SUBZERO_UNTIL, player) > player.level().getGameTime();
    }

    public static void plantBud(LivingEntity victim, ServerPlayer caster, int ticks) {
        FLESH_BUDS.put(victim.getUUID(),
                new Bud(caster.getUUID(), victim.getId(), caster.level().getGameTime() + ticks));
        BUDS_BY_CASTER.computeIfAbsent(caster.getUUID(), id -> new HashSet<>())
                .add(victim.getUUID());
    }

    public static List<Integer> budsOf(ServerPlayer caster, long now) {
        Set<UUID> mine = BUDS_BY_CASTER.get(caster.getUUID());
        if (mine == null || mine.isEmpty()) {
            return List.of();
        }

        List<Integer> ids = new ArrayList<>(mine.size());
        for (UUID victim : Set.copyOf(mine)) {
            Bud bud = FLESH_BUDS.get(victim);
            if (bud == null || now >= bud.until()) {
                expire(victim);
                continue;
            }
            ids.add(bud.victimId());
        }
        return ids;
    }

    public static void dropBud(UUID victim) {
        expire(victim);
    }

    private static void expire(UUID victim) {
        Bud bud = FLESH_BUDS.remove(victim);
        if (bud == null) {
            return;
        }
        Set<UUID> mine = BUDS_BY_CASTER.get(bud.caster());
        if (mine != null) {
            mine.remove(victim);
            if (mine.isEmpty()) {
                BUDS_BY_CASTER.remove(bud.caster());
            }
        }
    }

    public static boolean hasBud(LivingEntity victim) {
        Bud bud = FLESH_BUDS.get(victim.getUUID());
        if (bud == null) {
            return false;
        }
        if (victim.level().getGameTime() >= bud.until()) {
            expire(victim.getUUID());
            return false;
        }
        return true;
    }

    public static boolean plantedBy(LivingEntity victim, Player caster) {
        Bud bud = FLESH_BUDS.get(victim.getUUID());
        return bud != null && caster.getUUID().equals(bud.caster()) && hasBud(victim);
    }

    public static void removeBud(LivingEntity victim) {
        expire(victim.getUUID());
    }

    public static void clear(LivingEntity entity) {
        expire(entity.getUUID());
        BUDS_BY_CASTER.remove(entity.getUUID());
        DRIVE_UNTIL.remove(entity.getUUID());
        SUBZERO_UNTIL.remove(entity.getUUID());
        FLESH_BUDS.remove(entity.getUUID());
    }

    private static long until(Map<UUID, Long> from, Player player) {
        Long value = from.get(player.getUUID());
        return value == null ? Long.MIN_VALUE : value;
    }
}
