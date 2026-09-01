package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.data.ClientPlayerDataCache;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.StandTypes;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks which players currently have a Stand out, and in what colour, so the outline mixins can
 * answer instantly instead of searching the world on every render call.
 *
 * <p>Two sources, because neither covers everyone. The local player's own state comes from their
 * synced data, which is the only way to know about a DORMANT cast - it raises an aura but never
 * manifests an entity. Everyone else is inferred from the Stand entities actually in the world,
 * since other players' data isn't synced to this client at all.
 */
public final class StandGlowTracker {
    /**
     * How far away a Stand user can still be seen glowing, in blocks.
     *
     * <p>The outline is an aura coming off a body, and an aura that reads across a whole landscape
     * stops being an aura and becomes a map marker - it picked players out through terrain at any
     * distance the chunk was loaded at, which is a tracker, not a lighting effect.
     *
     * <p>Forty-eight blocks: three chunks, comfortably past any range a fight is fought at, and well
     * short of the horizon. Measured from the camera rather than from the player, because this is
     * decided for drawing and it is the camera that does the looking.
     */
    private static final double GLOW_RANGE = 48.0;

    /** Player UUID -> packed RGB of the Stand they have out. */
    private static final Map<UUID, Integer> GLOWING = new HashMap<>();

    private StandGlowTracker() {
    }

    public static boolean isGlowing(UUID playerId) {
        return GLOWING.containsKey(playerId);
    }

    /** Packed RGB for a glowing player, or -1 if they aren't one. */
    public static int glowColor(UUID playerId) {
        return GLOWING.getOrDefault(playerId, -1);
    }

    public static void clear() {
        GLOWING.clear();
    }

    /** Rebuilt once per client tick - cheap, and it keeps the render path free of world scans. */
    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        GLOWING.clear();

        if (minecraft.level == null) {
            return;
        }

        Vec3 eye = minecraft.gameRenderer.getMainCamera().getPosition();
        UUID selfId = minecraft.player == null ? null : minecraft.player.getUUID();

        // Whether a Stand of the local player's is actually standing in the world. Tracked while
        // scanning rather than searched for again afterwards, and deliberately set before the range
        // test: a Stand that exists but is too far off to draw has still manifested.
        boolean selfManifested = false;

        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (!(entity instanceof StandEntity stand) || stand.isDismissing()) {
                continue;
            }

            Player owner = stand.getOwner();
            if (owner == null) {
                continue;
            }

            // For yourself, your own synced flag has the last word. Inferring the outline purely
            // from Stand entities in the world leaves it lit whenever one of them outlives the
            // dismissal that was supposed to take it away - an entity removal that arrives a moment
            // late, or a Stand that is dropped without ever being marked as dismissing - and the
            // result is a player still glowing with no Stand anywhere near them.
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

            GLOWING.put(owner.getUUID(), StandTypes.byIdOrDefault(stand.getStandType().id())
                    .auraColorFor(stand.getSkin()));
        }

        Player self = minecraft.player;
        if (self == null || !ClientPlayerDataCache.data.standSummoned
                || !ClientPlayerDataCache.data.stand.isPresent()) {
            return;
        }

        // The flag says a Stand is out. If one that should have taken form has not, the flag is
        // wrong rather than the world - a summon that failed, an entity killed out from under it, a
        // dismissal that cleared the Stand and not the flag - and lighting the player up for it
        // means glowing with nothing to glow from. DORMANT is the one case where that is correct:
        // it raises the aura and never manifests anything, by design.
        if (!selfManifested && ClientPlayerDataCache.data.stand.trust().manifestsEntity()) {
            return;
        }

        GLOWING.put(self.getUUID(),
                StandTypes.byIdOrDefault(ClientPlayerDataCache.data.stand.standId())
                        .auraColorFor(ClientPlayerDataCache.data.stand.skin()));
    }

    private static boolean near(Player player, Vec3 eye) {
        return player.position().distanceToSqr(eye) <= GLOW_RANGE * GLOW_RANGE;
    }
}
