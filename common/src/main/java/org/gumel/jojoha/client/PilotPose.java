package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.gumel.jojoha.stand.StandEntity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class PilotPose {
    private static final Map<Integer, float[]> HELD = new HashMap<>();

    private PilotPose() {
    }

    public static void tick(Minecraft minecraft) {
        if (minecraft.level == null) {
            HELD.clear();
            return;
        }

        Set<Integer> flying = new HashSet<>();

        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (!(entity instanceof StandEntity stand) || !stand.isPiloted()) {
                continue;
            }

            Player owner = stand.getOwner();
            if (owner == null) {
                continue;
            }

            flying.add(owner.getId());

            HELD.computeIfAbsent(owner.getId(), id -> new float[]{
                    owner.yBodyRot, owner.getYHeadRot(), owner.getXRot()});
        }

        if (minecraft.player != null && PossessionClient.possessing() && !PossessionClient.merged()) {
            Player self = minecraft.player;
            flying.add(self.getId());
            HELD.computeIfAbsent(self.getId(), id -> new float[]{self.yBodyRot, self.getYHeadRot(), self.getXRot()});
        }

        HELD.keySet().retainAll(flying);
    }

    public static float[] of(Player player) {
        return HELD.get(player.getId());
    }

    public static void forget() {
        HELD.clear();
    }
}
