package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.gumel.jojoha.stand.StandEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

final class StandEntityLookup {
    private static int remembered = -1;

    private static final double BOUND_SEARCH = 12.0;

    private static final Map<Integer, Integer> BOUND_BY_PLAYER =
            new HashMap<>();

    private StandEntityLookup() {
    }

    static Optional<Entity> localStand(Minecraft minecraft) {
        if (minecraft.level == null || minecraft.player == null) {
            remembered = -1;
            return Optional.empty();
        }

        if (remembered >= 0) {
            Entity known = minecraft.level.getEntity(remembered);
            if (owned(known, minecraft)) {
                return Optional.of(known);
            }
            remembered = -1;
        }

        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (owned(entity, minecraft)) {
                remembered = entity.getId();
                return Optional.of(entity);
            }
        }

        return Optional.empty();
    }

    static StandEntity boundStandOf(Player player) {
        if (player.level() == null) {
            return null;
        }

        Integer known = BOUND_BY_PLAYER.get(player.getId());
        if (known != null) {
            Entity remembered = player.level().getEntity(known);
            if (belongsTo(remembered, player)) {
                return (StandEntity) remembered;
            }
            BOUND_BY_PLAYER.remove(player.getId());
        }

        for (StandEntity stand : player.level().getEntitiesOfClass(StandEntity.class,
                player.getBoundingBox().inflate(BOUND_SEARCH), s -> belongsTo(s, player))) {
            BOUND_BY_PLAYER.put(player.getId(), stand.getId());
            return stand;
        }
        return null;
    }

    private static boolean belongsTo(Entity entity, Player player) {
        return entity instanceof StandEntity stand
                && stand.isAlive()
                && stand.getStandType().form().isBound()
                && stand.getOwnerUuid().filter(player.getUUID()::equals).isPresent();
    }

    private static boolean owned(Entity entity, Minecraft minecraft) {
        return entity instanceof StandEntity stand
                && stand.isAlive()
                && stand.getOwnerUuid().filter(minecraft.player.getUUID()::equals).isPresent();
    }
}
