package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import org.gumel.jojoha.data.ClientPlayerDataCache;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.stand.StandEntity;

public final class StandViewAlpha {
    private static final double FADE_START_DISTANCE = 4.5;
    private static final double FADE_FULL_DISTANCE = 1.2;
    private static final float MIN_ALPHA = 0.14F;

    private static final float FIGHTING_MIN_ALPHA = 0.6F;

    private StandViewAlpha() {
    }

    public static float of(StandEntity stand) {
        float base = stand.getRenderAlpha() * SummonFlair.bodyAlpha(stand);

        if (stand.getStandType().form().isBound()) {
            return 0F;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (!seen(minecraft, stand)) {
            return 0F;
        }

        if (PilotView.drawing(stand) || stand.isManifesting()) {
            return base;
        }

        if (minecraft.player == null || !minecraft.options.getCameraType().isFirstPerson()) {
            return base;
        }
        if (stand.getOwnerUuid().filter(minecraft.player.getUUID()::equals).isEmpty()) {
            return base;
        }

        double distance = minecraft.gameRenderer.getMainCamera().getPosition().distanceTo(stand.position());
        if (distance >= FADE_START_DISTANCE) {
            return base;
        }

        float closeness = (float) Mth.clamp(
                (FADE_START_DISTANCE - distance) / (FADE_START_DISTANCE - FADE_FULL_DISTANCE), 0.0, 1.0);
        return base * Mth.lerp(closeness, 1F, fighting(stand) ? FIGHTING_MIN_ALPHA : MIN_ALPHA);
    }

    public static boolean seesStands() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player.isSpectator()) {
            return true;
        }
        JojohaPlayerData data = ClientPlayerDataCache.data;
        return data != null && (data.stand.isPresent() || data.anubis);
    }

    private static boolean seen(Minecraft minecraft, StandEntity stand) {
        if (minecraft.player != null && stand.getOwnerUuid().filter(minecraft.player.getUUID()::equals).isPresent()) {
            return true;
        }
        return seesStands();
    }

    private static boolean fighting(StandEntity stand) {
        return switch (stand.movementState()) {
            case GUARDING, BARRAGE, ATTACK, COMBAT, HOLDING, PURSUING -> true;
            default -> false;
        };
    }
}
