package org.gumel.jojoha.client;

import net.minecraft.client.Camera;
import org.joml.Matrix4f;

public final class LevelPostPasses {
    private static final Matrix4f MODEL_VIEW = new Matrix4f();
    private static final Matrix4f PROJECTION = new Matrix4f();

    private static Camera camera;
    private static float partialTick;

    private LevelPostPasses() {
    }

    public static void capture(Matrix4f modelView, Matrix4f projection, Camera view, float tick) {
        MODEL_VIEW.set(modelView);
        PROJECTION.set(projection);
        camera = view;
        partialTick = tick;
    }

    public static void run() {
        Camera view = camera;
        camera = null;
        if (view == null) {
            return;
        }

        BlindSense.post(MODEL_VIEW, PROJECTION, view, partialTick);
        DreamPost.render(MODEL_VIEW, PROJECTION, view, partialTick);
        TimeStopPost.render(MODEL_VIEW, PROJECTION, view, partialTick);
        AwakeningLight.render(MODEL_VIEW, PROJECTION, partialTick);
        PaletteSwap.render();
        ImpactFramePost.render();
        ErasurePost.render(MODEL_VIEW, PROJECTION, view);
    }
}
