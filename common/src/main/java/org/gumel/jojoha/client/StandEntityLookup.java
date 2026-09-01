package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.gumel.jojoha.stand.StandEntity;

import java.util.Optional;

/**
 * Finds the local player's own Stand among the entities the client is tracking.
 *
 * <p>The client is never told which entity id belongs to its Stand - the server keeps that mapping
 * for its own bookkeeping and has no reason to publish it. Every Stand does carry its owner's UUID
 * in synced data, though, so the client can simply ask the ones it can already see.
 *
 * <h2>Asked far too often to keep scanning</h2>
 *
 * <p>The scan walks every entity the client is rendering, and the callers turned out to be some of
 * the hottest code there is: the pilot loop asks every tick, and the first-person arms ask twice per
 * <em>frame</em>. In a busy chunk that is tens of thousands of comparisons a second to answer a
 * question whose answer almost never changes.
 *
 * <p>So the id is remembered and re-checked instead. Looking an entity up by id is a map hit rather
 * than a walk, and the remembered one is validated on every call - still loaded, still alive, still
 * ours - so a dismissed Stand or a swapped one falls back to the scan on the next ask rather than
 * being answered wrongly. The cache is a shortcut to the same answer, never a different one.
 */
final class StandEntityLookup {
    /** The last Stand found, by entity id. Negative means nothing remembered. */
    private static int remembered = -1;

    /** How far from its owner a bound Stand can be and still be theirs, in blocks. */
    private static final double BOUND_SEARCH = 3.0;

    /** Player entity id to their bound Stand's entity id, for the arms layer. */
    private static final java.util.Map<Integer, Integer> BOUND_BY_PLAYER =
            new java.util.HashMap<>();

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

    /**
     * The bound Stand belonging to a given player, or null.
     *
     * <p>Unlike the lookup above this is asked about <em>every</em> player on screen, once per
     * frame each, from the layer that draws their vines. So it is cached per player rather than
     * globally: one remembered id each, checked by id before anything is scanned.
     *
     * <p>Bound only. A free-standing Stand is a figure in the world with its own entity render, and
     * has no business being drawn onto anybody's arms.
     */
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

        // A bounded search rather than a walk of everything on screen: a bound Stand is pinned to
        // its owner, so if it is not within a couple of blocks of them it is not theirs to draw.
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

    /** Whether this is a live Stand belonging to the player at the keyboard. */
    private static boolean owned(Entity entity, Minecraft minecraft) {
        return entity instanceof StandEntity stand
                && stand.isAlive()
                && stand.getOwnerUuid().filter(minecraft.player.getUUID()::equals).isPresent();
    }
}
