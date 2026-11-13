package org.gumel.jojoha.stand;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.player.Player;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.config.JojohaServerConfig;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.data.StandPlacement;
import org.gumel.jojoha.network.packet.StandIdlePacket;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class StandCustomization {
    private static final Map<UUID, Integer> IDLE_OF = new HashMap<>();

    private static final Map<Integer, String> CLIPS = new HashMap<>();

    private static final Set<UUID> BUILT_IN = new HashSet<>();

    public static final String BUILT_IN_PREFIX = Jojoha.MOD_ID + ":";

    private StandCustomization() {
    }

    public static void place(ServerPlayer player, StandPlacement asked) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        StandPlacement placement = asked.clamped(JojohaServerConfig.STAND_PLACEMENT_RANGE.value());
        if (placement.equals(data.standPlacement)) {
            return;
        }
        data.standPlacement = placement;
        StandEntity stand = data.standSummoned ? StandEntity.findFor(player) : null;
        if (stand != null) {
            stand.setPlacement(placement);
        }
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
    }

    public static void idle(ServerPlayer player, String key, String clip) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        String chosen = key == null ? "" : key;
        if (!chosen.equals(data.standIdle)) {
            data.standIdle = chosen;
            PlayerDataAccess.set(player, data);
            PlayerDataAccess.sync(player);
        }
        boolean builtIn = chosen.startsWith(BUILT_IN_PREFIX);
        if (builtIn) {
            BUILT_IN.add(player.getUUID());
        } else {
            BUILT_IN.remove(player.getUUID());
        }
        int stamp = 0;
        if ((builtIn || JojohaServerConfig.CUSTOM_IDLES.on()) && clip != null && !clip.isEmpty()
                && clip.length() <= StandIdlePacket.MAX_CHARS) {
            String tidy = tidy(clip);
            if (tidy != null) {
                stamp = tidy.hashCode() == 0 ? 1 : tidy.hashCode();
                CLIPS.put(stamp, tidy);
            }
        }
        Integer old = stamp == 0 ? IDLE_OF.remove(player.getUUID()) : IDLE_OF.put(player.getUUID(), stamp);
        if (old != null && old != stamp && !IDLE_OF.containsValue(old)) {
            CLIPS.remove(old);
        }
        StandEntity stand = StandEntity.findFor(player);
        if (stand != null) {
            stand.setIdleClip(stamp);
        }
    }

    public static int idleOf(Player player) {
        if (!JojohaServerConfig.CUSTOM_IDLES.on() && !BUILT_IN.contains(player.getUUID())) {
            return 0;
        }
        Integer stamp = IDLE_OF.get(player.getUUID());
        return stamp == null ? 0 : stamp;
    }

    public static String clip(int stamp) {
        return CLIPS.get(stamp);
    }

    private static String tidy(String clip) {
        try {
            JsonObject root = GsonHelper.parse(clip);
            JsonObject animations = GsonHelper.getAsJsonObject(root, "animations");
            if (animations.size() != 1) {
                return null;
            }
            for (Map.Entry<String, JsonElement> entry : animations.entrySet()) {
                if (!entry.getValue().isJsonObject()
                        || !entry.getValue().getAsJsonObject().has("bones")) {
                    return null;
                }
            }
            return root.toString();
        } catch (RuntimeException e) {
            return null;
        }
    }
}
