package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.data.ClientPlayerDataCache;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.StandShadowForm;
import org.gumel.jojoha.stand.StandTypes;
import org.gumel.jojoha.supporter.SupporterAuras;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class StandGlowTracker {
    private static final double GLOW_RANGE = 48.0;

    private static final Map<UUID, Integer> GLOWING = new HashMap<>();

    private static boolean ownStandOut;

    private StandGlowTracker() {
    }

    public static boolean isGlowing(UUID playerId) {
        return GLOWING.containsKey(playerId);
    }

    public static int glowColor(UUID playerId) {
        return GLOWING.getOrDefault(playerId, -1);
    }

    public static boolean ownStandOut() {
        return ownStandOut;
    }

    public static void clear() {
        GLOWING.clear();
        ownStandOut = false;
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        GLOWING.clear();
        ownStandOut = false;

        if (minecraft.level == null) {
            return;
        }

        Vec3 eye = minecraft.gameRenderer.getMainCamera().getPosition();
        UUID selfId = minecraft.player == null ? null : minecraft.player.getUUID();

        boolean selfManifested = false;

        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (!(entity instanceof StandEntity stand) || stand.isDismissing()) {
                continue;
            }

            Player owner = stand.getOwner();
            if (owner == null) {
                continue;
            }

            boolean isSelf = owner.getUUID().equals(selfId);
            if (isSelf && !ClientPlayerDataCache.data.standSummoned) {
                continue;
            }
            if (isSelf) {
                selfManifested = true;
            }

            if (!near(owner, eye)) {
                continue;
            }

            StandShadowForm shadow = StandShadowForm.of(stand);
            GLOWING.put(owner.getUUID(), shadow != null
                    ? shadow.auraColour()
                    : SupporterAuras.tint(owner.getUUID(),
                            StandTypes.byIdOrDefault(stand.getStandType().id()).auraColorFor(stand.getSkin())));
        }

        ownStandOut = selfManifested;

        Player self = minecraft.player;
        if (self == null || !ClientPlayerDataCache.data.standSummoned
                || !ClientPlayerDataCache.data.stand.isPresent()) {
            return;
        }

        if (!selfManifested && ClientPlayerDataCache.data.stand.trust().manifestsEntity()) {
            return;
        }

        StandShadowForm shadow = StandShadowForm.of(
                ClientPlayerDataCache.data.stand.standId(), ClientPlayerDataCache.data.stand.skin(),
                ClientPlayerDataCache.data.stand.trust());
        GLOWING.put(self.getUUID(), shadow != null
                ? shadow.auraColour()
                : SupporterAuras.ownTint(StandTypes.byIdOrDefault(
                        ClientPlayerDataCache.data.stand.standId()).auraColorFor(ClientPlayerDataCache.data.stand.skin())));
    }

    private static boolean near(Player player, Vec3 eye) {
        return player.position().distanceToSqr(eye) <= GLOW_RANGE * GLOW_RANGE;
    }
}
