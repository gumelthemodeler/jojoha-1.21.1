package org.gumel.jojoha.hamon;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.data.PlayerSpec;
import org.gumel.jojoha.hamon.moves.RipplePulseMove;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Registry of Hamon moves, plus server-side validation and dispatch of move-use requests. */
public final class HamonMoves {
    private static final Map<ResourceLocation, HamonMove> MOVES = new LinkedHashMap<>();

    static {
        register(RipplePulseMove.INSTANCE);
    }

    private HamonMoves() {
    }

    private static void register(HamonMove move) {
        MOVES.put(move.id(), move);
    }

    /** Every registered move, in registration order. For anything listing what exists. */
    public static java.util.Collection<HamonMove> all() {
        return MOVES.values();
    }

    public static HamonMove byId(ResourceLocation id) {
        return MOVES.get(id);
    }

    /**
     * Server-side handling of a client's move-use request. Re-validates spec, path
     * ownership and cooldown here rather than trusting the client.
     */
    public static void handleUseRequest(ServerPlayer player, ResourceLocation moveId) {
        HamonMove move = byId(moveId);
        if (move == null) {
            return;
        }

        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (data.spec != PlayerSpec.HAMON) {
            return;
        }

        boolean known = data.getUnlockedHamonPaths().stream()
                .map(HamonPaths::byId)
                .filter(Objects::nonNull)
                .anyMatch(path -> path.moveIds().contains(moveId));
        if (!known) {
            return;
        }

        long now = player.level().getGameTime();
        if (data.isMoveOnCooldown(moveId, now)) {
            return;
        }
        if (data.specEnergy < move.energyCost()) {
            return;
        }

        data.setMoveCooldown(moveId, now, move.cooldownTicks());
        data.specEnergy -= move.energyCost();
        move.activate(player, data);

        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
    }
}
