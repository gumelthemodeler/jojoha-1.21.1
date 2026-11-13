package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import org.gumel.jojoha.body.Body;
import org.gumel.jojoha.combat.Climbing;
import org.gumel.jojoha.combat.WallWalk;

public final class CombatBarOverlay {
    private static boolean showingPage2 = false;

    private static boolean visible = false;

    private CombatBarOverlay() {
    }

    public static void togglePage() {
        showingPage2 = !showingPage2;
    }

    public static void tickClientState() {
        if (Minecraft.getInstance().level == null) {
            visible = false;
            SunScorch.clear();
            Climbing.clearMirror();
            Body.clearMirror();
            NoiseSense.clear();
            WallWalk.clear();
        }
    }

    public static void toggleVisibility() {
        visible = !visible;
    }

    public static boolean isVisible() {
        return visible;
    }

    public static int currentSkillPage() {
        return showingPage2 ? 1 : 0;
    }
}
