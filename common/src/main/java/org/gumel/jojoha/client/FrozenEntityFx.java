package org.gumel.jojoha.client;

import net.minecraft.Util;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class FrozenEntityFx {
    private static final float SHAKE = 0.022F;

    private static final float FADE_PER_SECOND = 5F;

    private static float amount;

    private FrozenEntityFx() {
    }

    public static void tick(boolean frozen) {
        amount = Mth.approach(amount, frozen ? 1F : 0F, FADE_PER_SECOND / 20F);
    }

    public static void clear() {
        amount = 0F;
    }

    public static boolean active() {
        return amount > 0.001F;
    }

    public static float[] shake(Entity entity) {
        if (amount <= 0.001F || !(entity instanceof Player)) {
            return null;
        }

        float seconds = (Util.getMillis() % 1000000L) / 1000F;
        float seed = entity.getId() * 1.618F;
        float scale = SHAKE * amount;

        return new float[] {
                Mth.sin(seconds * 41.3F + seed) * scale,
                Mth.sin(seconds * 57.7F + seed * 2.1F) * scale * 0.6F,
                Mth.sin(seconds * 47.1F + seed * 3.7F) * scale,
        };
    }
}
