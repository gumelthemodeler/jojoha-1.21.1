package org.gumel.jojoha.client.compat;

import com.github.exopandora.shouldersurfing.api.client.IShoulderSurfing;
import com.github.exopandora.shouldersurfing.api.client.IShoulderSurfingCamera;
import dev.architectury.platform.Platform;
import net.minecraft.util.Mth;

public final class ShoulderSurfingCamera {
    private static final String MOD_ID = "shouldersurfing";

    private static Boolean present;

    private ShoulderSurfingCamera() {
    }

    public static boolean installed() {
        return present();
    }

    private static boolean present() {
        if (present == null) {
            present = Platform.isModLoaded(MOD_ID);
        }
        return present;
    }

    public static boolean surfing() {
        if (!present()) {
            return false;
        }
        try {
            return IShoulderSurfing.getInstance().isShoulderSurfing();
        } catch (LinkageError | RuntimeException broken) {
            present = false;
            return false;
        }
    }

    public static void follow(float yRot, float xRot, float share) {
        if (!surfing()) {
            return;
        }
        try {
            turn(yRot, xRot, share);
        } catch (LinkageError | RuntimeException broken) {
            present = false;
        }
    }

    private static void turn(float yRot, float xRot, float share) {
        IShoulderSurfingCamera camera = IShoulderSurfing.getInstance().getCamera();
        float y = camera.getYRot();
        float x = camera.getXRot();
        camera.setYRot(y + Mth.wrapDegrees(yRot - y) * share);
        camera.setXRot(Mth.clamp(x + (xRot - x) * share, -90F, 90F));
    }
}
