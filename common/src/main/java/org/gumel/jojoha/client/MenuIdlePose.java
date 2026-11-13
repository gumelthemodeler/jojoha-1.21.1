package org.gumel.jojoha.client;

import net.minecraft.Util;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import org.gumel.jojoha.client.anim.BedrockAnimation;
import org.joml.Vector3f;

import java.util.Optional;

public final class MenuIdlePose {
    private static final String ANIMATION = "menu_idle";
    private static final String ROOT = "body1";

    private static Optional<BedrockAnimation> loaded;

    private static boolean drawing;

    private MenuIdlePose() {
    }

    public static void drawing(boolean on) {
        drawing = on;
    }

    public static boolean drawing() {
        return drawing;
    }

    public static boolean apply(HumanoidModel<?> model) {
        if (!drawing) {
            return false;
        }
        if (loaded == null) {
            loaded = BedrockAnimation.load(PlayerMoveAnimation.FILE, ANIMATION);
        }
        BedrockAnimation anim = loaded.orElse(null);
        if (anim == null) {
            return false;
        }

        long length = Math.max(1L, Math.round(anim.lengthSeconds() * 1000F));
        float seconds = (Util.getMillis() % length) / 1000F;
        PlayerStabAnimation.pose(model, anim, seconds);

        Vector3f root = anim.sample(ROOT, BedrockAnimation.Channel.POSITION, seconds);
        if (root != null) {
            for (ModelPart part : new ModelPart[] {model.body, model.head, model.rightArm, model.leftArm,
                    model.rightLeg, model.leftLeg}) {
                part.offsetPos(root);
            }
            PlayerStabAnimation.copyOverlays(model);
        }
        return true;
    }
}
