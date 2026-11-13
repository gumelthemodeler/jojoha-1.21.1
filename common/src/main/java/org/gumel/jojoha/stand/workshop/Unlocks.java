package org.gumel.jojoha.stand.workshop;

import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.stand.StandTypes;

public final class Unlocks {
    public static final String STAND = "stand:";
    public static final String ALL = "debug:all";

    private Unlocks() {
    }

    public static String standKey(ResourceLocation id) {
        return STAND + id;
    }

    public static boolean kept(String key) {
        return key.startsWith(STAND) || key.equals(ALL);
    }

    public static boolean all(JojohaPlayerData data) {
        return data.workshopUnlocks.contains(ALL);
    }

    public static boolean style(JojohaPlayerData data, SummonStyle style) {
        return style.free() || all(data);
    }

    public static boolean shout(JojohaPlayerData data, BarrageShout shout) {
        if (all(data)) {
            return true;
        }
        if (shout == BarrageShout.OVER_HEAVEN) {
            int skin = StandTypes.byIdOrDefault(StandTypes.THE_WORLD_ID).skinIndex(Workshop.OVER_HEAVEN);
            return skin >= 0 && data.canWearSkin(StandTypes.THE_WORLD_ID, skin);
        }
        for (ResourceLocation voice : shout.voices()) {
            if (!data.workshopUnlocks.contains(standKey(voice))) {
                return false;
            }
        }
        return true;
    }
}
