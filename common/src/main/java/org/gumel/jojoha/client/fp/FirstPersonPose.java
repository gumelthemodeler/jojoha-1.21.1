package org.gumel.jojoha.client.fp;

import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

public record FirstPersonPose(String id, ResourceLocation file, String name,
                              boolean loop, boolean hold, boolean item) {
    public static final String RIGHT_ARM = "right_arm";
    public static final String LEFT_ARM = "left_arm";

    public static final ResourceLocation POSES =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "animations/hamonplayeranimations.json");

    public static FirstPersonPose once(String name) {
        return new FirstPersonPose(name, POSES, name, false, false, false);
    }

    public static FirstPersonPose withItem(String name) {
        return new FirstPersonPose(name, POSES, name, false, false, true);
    }

    public static FirstPersonPose held(String name) {
        return new FirstPersonPose(name, POSES, name, false, true, false);
    }

    public static FirstPersonPose looping(String name) {
        return new FirstPersonPose(name, POSES, name, true, true, false);
    }

    public static final ResourceLocation MOVES =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "animations/playeranimations.animation.json");

    public static FirstPersonPose movesWithItem(String name) {
        return new FirstPersonPose(name, MOVES, name, false, false, true);
    }
}
