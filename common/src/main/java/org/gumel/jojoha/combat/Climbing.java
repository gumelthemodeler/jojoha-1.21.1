package org.gumel.jojoha.combat;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.gumel.jojoha.hamon.HamonState;
import org.gumel.jojoha.pillar.PillarMen;
import org.gumel.jojoha.vampire.VampireState;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class Climbing {
    public enum Grip {
        WALL,
        RIPPLE,
        CONTORT;

        private static final Grip[] VALUES = values();

        public static Grip byOrdinal(int ordinal) {
            return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : WALL;
        }
    }

    private static final Set<UUID> MIRROR_WALL = new HashSet<>();
    private static final Set<UUID> MIRROR_RIPPLE = new HashSet<>();
    private static final Set<UUID> MIRROR_CONTORT = new HashSet<>();

    private Climbing() {
    }

    public static void setMirror(UUID id, Grip grip, boolean climbing) {
        Set<UUID> set = switch (grip) {
            case RIPPLE -> MIRROR_RIPPLE;
            case CONTORT -> MIRROR_CONTORT;
            default -> MIRROR_WALL;
        };
        if (climbing) {
            set.add(id);
        } else {
            set.remove(id);
        }
    }

    public static void clearMirror() {
        MIRROR_WALL.clear();
        MIRROR_RIPPLE.clear();
        MIRROR_CONTORT.clear();
    }

    public static boolean contorting(Entity entity) {
        if (!(entity instanceof Player player)) {
            return false;
        }
        if (player.level().isClientSide()) {
            return MIRROR_CONTORT.contains(player.getUUID());
        }
        return PillarMen.isContorting(player);
    }

    public static boolean walksOnWater(LivingEntity entity) {
        return entity instanceof Player player
                && player.level().isClientSide()
                && MIRROR_RIPPLE.contains(player.getUUID());
    }

    public static boolean wallCrawling(Player player) {
        if (player.level().isClientSide()) {
            return MIRROR_WALL.contains(player.getUUID());
        }
        return VampireState.isCrawling(player);
    }

    public static boolean canClimb(LivingEntity entity) {
        if (!(entity instanceof Player player) || !player.horizontalCollision) {
            return false;
        }

        if (player.level().isClientSide()) {
            return MIRROR_RIPPLE.contains(player.getUUID());
        }
        return HamonState.isWalking(player);
    }
}
