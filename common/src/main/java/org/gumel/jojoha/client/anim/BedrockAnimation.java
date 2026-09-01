package org.gumel.jojoha.client.anim;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.GsonHelper;
import org.joml.Vector3f;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A Blockbench/Bedrock-format animation, loaded straight from a resource and evaluated against
 * plain vanilla {@link net.minecraft.client.model.geom.ModelPart ModelParts}.
 *
 * <p>This exists because the Stand uses GeckoLib but the <em>player</em> doesn't - the player is
 * rendered by vanilla's own {@code PlayerModel}, which GeckoLib has no hold over. Rather than
 * pull in a whole player-animation library for one animation, this reads the same JSON Blockbench
 * exports and drives the vanilla model parts directly.
 *
 * <p>Values follow vanilla's own Blockbench conventions, mirrored from
 * {@code KeyframeAnimations.degreeVec}/{@code posVec}: rotations are degrees converted straight to
 * radians with no axis flips, and positions are used as-is except for Y, which is negated.
 */
public final class BedrockAnimation {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

    /** Which transform a channel drives on a bone. */
    public enum Channel { ROTATION, POSITION, SCALE }

    private final float lengthSeconds;
    private final Map<String, Map<Channel, List<Keyframe>>> bones;

    private BedrockAnimation(float lengthSeconds, Map<String, Map<Channel, List<Keyframe>>> bones) {
        this.lengthSeconds = lengthSeconds;
        this.bones = bones;
    }

    public float lengthSeconds() {
        return lengthSeconds;
    }

    public float lengthTicks() {
        return lengthSeconds * 20F;
    }

    /**
     * Samples a bone's channel at the given time, or null if this animation doesn't drive it.
     * Time is clamped to the animation's bounds rather than looping - the stab is a one-shot.
     */
    /**
     * Samples a looping animation, wrapping the time rather than clamping it.
     *
     * <p>{@link #sample} deliberately clamps, because it was written for one-shots. A cycling
     * animation needs the opposite: past the end it should be back at the start.
     */
    public Vector3f sampleLooping(String boneName, Channel channel, float seconds) {
        float wrapped = lengthSeconds <= 1.0E-5F ? 0F : seconds % lengthSeconds;
        return sample(boneName, channel, wrapped < 0F ? wrapped + lengthSeconds : wrapped);
    }

    public Vector3f sample(String boneName, Channel channel, float seconds) {
        Map<Channel, List<Keyframe>> channels = bones.get(boneName);
        if (channels == null) {
            return null;
        }
        List<Keyframe> frames = channels.get(channel);
        if (frames == null || frames.isEmpty()) {
            return null;
        }
        return interpolate(frames, Math.max(0F, Math.min(seconds, lengthSeconds)), channel);
    }

    public boolean drives(String boneName) {
        return bones.containsKey(boneName);
    }

    // --- Evaluation ---------------------------------------------------------------------

    private static Vector3f interpolate(List<Keyframe> frames, float time, Channel channel) {
        if (frames.size() == 1 || time <= frames.get(0).time) {
            return convert(frames.get(0).value, channel);
        }

        int next = -1;
        for (int i = 1; i < frames.size(); i++) {
            if (time <= frames.get(i).time) {
                next = i;
                break;
            }
        }
        if (next < 0) {
            return convert(frames.get(frames.size() - 1).value, channel);
        }

        Keyframe from = frames.get(next - 1);
        Keyframe to = frames.get(next);
        float span = to.time - from.time;
        float t = span <= 1.0E-5F ? 0F : (time - from.time) / span;

        // Every keyframe Blockbench wrote here is catmullrom, so honouring it matters - linear
        // would visibly flatten the wind-up and the recoil. The neighbours on either side are the
        // spline's control points, clamped at the ends where there is no further neighbour.
        Vector3f p0 = frames.get(Math.max(0, next - 2)).value;
        Vector3f p3 = frames.get(Math.min(frames.size() - 1, next + 1)).value;
        Vector3f raw = to.catmullrom
                ? catmullRom(p0, from.value, to.value, p3, t)
                : lerp(from.value, to.value, t);

        return convert(raw, channel);
    }

    private static Vector3f lerp(Vector3f a, Vector3f b, float t) {
        return new Vector3f(
                a.x + (b.x - a.x) * t,
                a.y + (b.y - a.y) * t,
                a.z + (b.z - a.z) * t);
    }

    private static Vector3f catmullRom(Vector3f p0, Vector3f p1, Vector3f p2, Vector3f p3, float t) {
        float t2 = t * t;
        float t3 = t2 * t;
        return new Vector3f(
                catmullRom(p0.x, p1.x, p2.x, p3.x, t, t2, t3),
                catmullRom(p0.y, p1.y, p2.y, p3.y, t, t2, t3),
                catmullRom(p0.z, p1.z, p2.z, p3.z, t, t2, t3));
    }

    private static float catmullRom(float p0, float p1, float p2, float p3, float t, float t2, float t3) {
        return 0.5F * ((2F * p1)
                + (-p0 + p2) * t
                + (2F * p0 - 5F * p1 + 4F * p2 - p3) * t2
                + (-p0 + 3F * p1 - 3F * p2 + p3) * t3);
    }

    /** Applies vanilla's Blockbench axis conventions - see the class javadoc. */
    private static Vector3f convert(Vector3f raw, Channel channel) {
        return switch (channel) {
            case ROTATION -> new Vector3f(raw.x * DEG_TO_RAD, raw.y * DEG_TO_RAD, raw.z * DEG_TO_RAD);
            // Scale is a plain multiplier on every axis - no unit conversion, and crucially no Y
            // flip, which would turn a bone that is meant to vanish into one that is inside out.
            case SCALE -> new Vector3f(raw.x, raw.y, raw.z);
            case POSITION -> new Vector3f(raw.x, -raw.y, raw.z);
        };
    }

    // --- Loading ------------------------------------------------------------------------

    /** Reads a single named animation out of a Blockbench animation file. Empty if anything is off. */
    public static Optional<BedrockAnimation> load(ResourceLocation file, String animationName) {
        try {
            Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(file);
            if (resource.isEmpty()) {
                LOGGER.error("[jojoha] animation file not found: {}", file);
                return Optional.empty();
            }

            JsonObject root;
            try (BufferedReader reader = resource.get().openAsReader()) {
                root = GsonHelper.parse(reader);
            }

            JsonObject animations = GsonHelper.getAsJsonObject(root, "animations");
            if (!animations.has(animationName)) {
                LOGGER.error("[jojoha] animation '{}' missing from {}", animationName, file);
                return Optional.empty();
            }

            JsonObject animation = GsonHelper.getAsJsonObject(animations, animationName);
            float length = GsonHelper.getAsFloat(animation, "animation_length", 1F);
            Map<String, Map<Channel, List<Keyframe>>> bones = new HashMap<>();

            JsonObject bonesObj = GsonHelper.getAsJsonObject(animation, "bones", new JsonObject());
            for (Map.Entry<String, JsonElement> boneEntry : bonesObj.entrySet()) {
                JsonObject boneObj = boneEntry.getValue().getAsJsonObject();
                Map<Channel, List<Keyframe>> channels = new HashMap<>();
                readChannel(boneObj, "rotation").ifPresent(f -> channels.put(Channel.ROTATION, f));
                readChannel(boneObj, "position").ifPresent(f -> channels.put(Channel.POSITION, f));
                readChannel(boneObj, "scale").ifPresent(f -> channels.put(Channel.SCALE, f));
                if (!channels.isEmpty()) {
                    bones.put(boneEntry.getKey(), channels);
                }
            }

            return Optional.of(new BedrockAnimation(length, bones));
        } catch (Exception e) {
            LOGGER.error("[jojoha] failed to load animation '{}' from {}", animationName, file, e);
            return Optional.empty();
        }
    }

    private static Optional<List<Keyframe>> readChannel(JsonObject boneObj, String channelName) {
        if (!boneObj.has(channelName)) {
            return Optional.empty();
        }

        JsonElement channel = boneObj.get(channelName);
        List<Keyframe> frames = new ArrayList<>();

        // A channel with no timestamps at all is a single static pose for the whole animation.
        if (channel.isJsonArray()) {
            frames.add(new Keyframe(0F, readVector(channel), false));
            return Optional.of(frames);
        }

        JsonObject channelObj = channel.getAsJsonObject();
        if (channelObj.has("vector")) {
            frames.add(new Keyframe(0F, readVector(channelObj.get("vector")), false));
            return Optional.of(frames);
        }

        for (Map.Entry<String, JsonElement> frame : channelObj.entrySet()) {
            float time;
            try {
                time = Float.parseFloat(frame.getKey());
            } catch (NumberFormatException e) {
                continue;
            }

            JsonElement value = frame.getValue();
            boolean catmullrom = false;
            if (value.isJsonObject()) {
                JsonObject valueObj = value.getAsJsonObject();
                catmullrom = "catmullrom".equals(GsonHelper.getAsString(valueObj, "lerp_mode", ""));
                // Blockbench splits a keyframe into "pre"/"post" when the two sides differ; only
                // "post" matters for playback moving forwards through the timeline.
                if (valueObj.has("post")) {
                    value = valueObj.get("post");
                } else if (valueObj.has("vector")) {
                    value = valueObj.get("vector");
                }
                if (value.isJsonObject() && value.getAsJsonObject().has("vector")) {
                    value = value.getAsJsonObject().get("vector");
                }
            }

            frames.add(new Keyframe(time, readVector(value), catmullrom));
        }

        frames.sort((a, b) -> Float.compare(a.time, b.time));
        return frames.isEmpty() ? Optional.empty() : Optional.of(frames);
    }

    private static Vector3f readVector(JsonElement element) {
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            return new Vector3f(
                    array.get(0).getAsFloat(),
                    array.get(1).getAsFloat(),
                    array.get(2).getAsFloat());
        }
        return new Vector3f();
    }

    private record Keyframe(float time, Vector3f value, boolean catmullrom) {
    }
}
