package org.gumel.jojoha.client.fp;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import org.gumel.jojoha.client.anim.BedrockAnimation;
import org.gumel.jojoha.client.anim.ClipContact;
import org.gumel.jojoha.combat.fx.MovementFx;
import org.gumel.jojoha.network.packet.FirstPersonPosePacket;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class FirstPersonPoses {
    private static final Map<String, FirstPersonPose> POSES = new LinkedHashMap<>();

    private static final Map<String, Optional<BedrockAnimation>> LOADED = new HashMap<>();

    static {
        register(FirstPersonPose.once("gather"));

        register(FirstPersonPose.withItem("channel"));

        register(FirstPersonPose.once("palm"));

        register(FirstPersonPose.movesWithItem("hand_parry_fp"));
        register(FirstPersonPose.movesWithItem("hand_parry2_fp"));
        register(FirstPersonPose.movesWithItem("sword_parry_fp"));
        register(FirstPersonPose.movesWithItem("sword_parry2_fp"));

        register(FirstPersonPose.looping("guard"));

        register(FirstPersonPose.looping("barrage"));

        register(FirstPersonPose.once("bubble_launcher"));

        register(FirstPersonPose.once("bubble_cutter"));

        register(FirstPersonPose.looping("heal"));

        register(FirstPersonPose.once("zoom_punch"));

        register(FirstPersonPose.withItem("clacker_hit"));

        register(FirstPersonPose.withItem("clacker_spin"));
    }

    private FirstPersonPoses() {
    }

    private static void register(FirstPersonPose pose) {
        POSES.put(pose.id(), pose);
    }

    public static FirstPersonPose byId(String id) {
        return id == null ? null : POSES.get(id);
    }

    public static Collection<FirstPersonPose> all() {
        return POSES.values();
    }

    private static FirstPersonPose current;

    private static float startedAt;

    private static float frozenUntil = Float.NEGATIVE_INFINITY;

    private static final float ECHO_TICKS = 8F;

    private static int lengthTicks;

    public static void play(String id) {
        play(id, 0);
    }

    public static void play(String id, int ticks) {
        FirstPersonPose pose = byId(id);
        if (pose == null) {
            stop();
            return;
        }
        if (current == pose && id.contains("parry") && renderTicks() - startedAt < ECHO_TICKS) {
            return;
        }

        current = pose;
        startedAt = renderTicks();
        frozenUntil = Float.NEGATIVE_INFINITY;
        lengthTicks = Math.max(0, ticks);
        released = false;
    }

    private static boolean released;

    public static void catchAt(int ticks) {
        BedrockAnimation animation = animationOf(current);
        if (animation == null || ticks <= 0) {
            return;
        }
        float held = Math.max(renderTicks(), frozenUntil);
        float clash = ClipContact.clamp(animation, held((held - startedAt) / 20F));
        startedAt = held - clash * 20F + ticks;
        frozenUntil = held + ticks;
    }

    public static void freeze(int ticks) {
        if (current == null || ticks <= 0) {
            return;
        }
        startedAt += ticks;
        frozenUntil = Math.max(renderTicks(), frozenUntil) + ticks;
    }

    public static void heard(String id, int ticks) {
        if (ticks == FirstPersonPosePacket.RELEASE) {
            release(id);
        } else {
            play(id, ticks);
        }
    }

    public static void release(String id) {
        Float holdAt = MovementFx.HOLD_AT.get(id);
        if (current == null || !current.id().equals(id) || holdAt == null || released) {
            return;
        }
        released = true;
        startedAt = Math.max(startedAt, renderTicks() - holdAt * 20F);
    }

    private static float held(float seconds) {
        Float holdAt = current == null || released ? null
                : MovementFx.HOLD_AT.get(current.id());
        return holdAt == null ? seconds : Math.min(seconds, holdAt);
    }

    public static void stop() {
        current = null;
        lengthTicks = 0;
    }

    public static boolean active() {
        return animationOf(current) != null;
    }

    public static FirstPersonPose playing() {
        return current;
    }

    private static float elapsed(BedrockAnimation animation) {
        float ticks = Math.max(renderTicks(), frozenUntil) - startedAt;
        float seconds = held(ticks / 20F);

        if (lengthTicks > 0 && ticks >= lengthTicks) {
            stop();
            return 0F;
        }

        if (current != null && current.loop()) {
            return seconds;
        }

        if (seconds < animation.lengthSeconds()) {
            return seconds;
        }

        if (current != null && current.hold()) {
            return animation.lengthSeconds();
        }

        if (seconds < animation.lengthSeconds() + EASE_OUT_SECONDS && endsAway(animation)) {
            return seconds;
        }

        stop();
        return 0F;
    }

    private static final float EASE_OUT_SECONDS = 4F / 20F;

    private static final float REST_TOLERANCE = (float) Math.toRadians(2.0);

    private static boolean endsAway(BedrockAnimation animation) {
        for (String bone : new String[] {FirstPersonPose.RIGHT_ARM, FirstPersonPose.LEFT_ARM}) {
            for (BedrockAnimation.Channel channel : new BedrockAnimation.Channel[] {
                    BedrockAnimation.Channel.ROTATION, BedrockAnimation.Channel.POSITION}) {
                Vector3f v = animation.drives(bone) ? animation.sample(bone, channel, animation.lengthSeconds()) : null;
                if (v != null && Math.max(Math.abs(v.x), Math.max(Math.abs(v.y), Math.abs(v.z))) > REST_TOLERANCE) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Vector3f easeOut(Vector3f value, BedrockAnimation.Channel channel, float pastEnd) {
        float weight = Mth.clamp(1F - pastEnd / EASE_OUT_SECONDS, 0F, 1F);
        weight = weight * weight * (3F - 2F * weight);
        if (channel == BedrockAnimation.Channel.SCALE) {
            return value.sub(1F, 1F, 1F).mul(weight).add(1F, 1F, 1F);
        }
        if (channel == BedrockAnimation.Channel.ROTATION) {
            value.set(Mth.wrapDegrees(value.x * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD,
                    Mth.wrapDegrees(value.y * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD,
                    Mth.wrapDegrees(value.z * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD);
        }
        return value.mul(weight);
    }

    public static Vector3f sample(String bone, BedrockAnimation.Channel channel) {
        BedrockAnimation animation = animationOf(current);
        if (animation == null) {
            return null;
        }
        float at = elapsed(animation);

        if (current != null && current.loop()) {
            return animation.sampleLooping(bone, channel, at);
        }
        Vector3f value = animation.sample(bone, channel, at);
        float pastEnd = at - animation.lengthSeconds();
        return value != null && pastEnd > 0F && current != null && !current.hold()
                ? easeOut(value, channel, pastEnd) : value;
    }

    public static boolean drives(String bone) {
        BedrockAnimation animation = animationOf(current);
        return animation != null && animation.drives(bone);
    }

    private static BedrockAnimation animationOf(FirstPersonPose pose) {
        if (pose == null) {
            return null;
        }
        return LOADED
                .computeIfAbsent(pose.id(), key -> BedrockAnimation.load(pose.file(), pose.name()))
                .orElse(null);
    }

    public static void forgetLoaded() {
        LOADED.clear();
    }

    private static float renderTicks() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return 0F;
        }
        return minecraft.level.getGameTime() + minecraft.getTimer().getGameTimeDeltaPartialTick(false);
    }
}
