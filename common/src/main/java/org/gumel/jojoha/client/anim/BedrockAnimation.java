package org.gumel.jojoha.client.anim;

import org.gumel.jojoha.Jojoha;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.GsonHelper;
import org.joml.Vector3f;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.DoubleUnaryOperator;

public final class BedrockAnimation {
    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

    public enum Channel { ROTATION, POSITION, SCALE }

    private final float lengthSeconds;
    private final Map<String, Map<Channel, List<Keyframe>>> bones;
    private final float marker;

    private BedrockAnimation(float lengthSeconds, Map<String, Map<Channel, List<Keyframe>>> bones, float marker) {
        this.lengthSeconds = lengthSeconds;
        this.bones = bones;
        this.marker = marker;
    }

    public float lengthSeconds() {
        return lengthSeconds;
    }

    public float lengthTicks() {
        return lengthSeconds * 20F;
    }

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

    public float marker() {
        return marker;
    }

    public List<float[]> keys(String boneName, Channel channel) {
        Map<Channel, List<Keyframe>> channels = bones.get(boneName);
        List<Keyframe> frames = channels == null ? null : channels.get(channel);
        List<float[]> out = new ArrayList<>();
        if (frames != null) {
            for (Keyframe frame : frames) {
                out.add(new float[] {frame.time, frame.value.x, frame.value.y, frame.value.z});
            }
        }
        return out;
    }

    public boolean still() {
        for (Map<Channel, List<Keyframe>> channels : bones.values()) {
            for (List<Keyframe> frames : channels.values()) {
                if (frames.size() > 1) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean drives(String boneName) {
        return bones.containsKey(boneName);
    }

    public Set<String> boneNames() {
        return Collections.unmodifiableSet(bones.keySet());
    }

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

        Vector3f p0 = frames.get(Math.max(0, next - 2)).value;
        Vector3f p3 = frames.get(Math.min(frames.size() - 1, next + 1)).value;
        Vector3f raw = to.catmullrom
                ? catmullRom(p0, from.value, to.value, p3, t)
                : lerp(from.value, to.value, ease(to.easing, t));

        return convert(raw, channel);
    }

    private static float ease(String easing, float t) {
        if (easing == null || easing.equals("linear")) {
            return t;
        }
        if (easing.equals("step")) {
            return t < 1F ? 0F : 1F;
        }
        boolean inOut = easing.startsWith("easeinout");
        boolean in = !inOut && easing.startsWith("easein");
        boolean out = easing.startsWith("easeout");
        if (!inOut && !in && !out) {
            return t;
        }
        DoubleUnaryOperator shape = switch (easing.substring(inOut ? 9 : in ? 6 : 7)) {
            case "sine" -> n -> 1 - Math.cos(n * Math.PI / 2);
            case "quad" -> n -> n * n;
            case "cubic" -> n -> n * n * n;
            case "quart" -> n -> n * n * n * n;
            case "quint" -> n -> n * n * n * n * n;
            case "expo" -> n -> n <= 0 ? 0 : Math.pow(2, 10 * n - 10);
            case "circ" -> n -> 1 - Math.sqrt(Math.max(0, 1 - n * n));
            case "back" -> n -> n * n * ((1.70158 + 1) * n - 1.70158);
            case "elastic" -> n -> n <= 0 ? 0 : n >= 1 ? 1
                    : -Math.pow(2, 10 * n - 10) * Math.sin((n * 10 - 10.75) * (2 * Math.PI / 3));
            case "bounce" -> n -> 1 - bounce(1 - n);
            default -> null;
        };
        if (shape == null) {
            return t;
        }
        double x = t;
        if (inOut) {
            return (float) (x < 0.5 ? shape.applyAsDouble(x * 2) / 2 : 1 - shape.applyAsDouble((1 - x) * 2) / 2);
        }
        return (float) (in ? shape.applyAsDouble(x) : 1 - shape.applyAsDouble(1 - x));
    }

    private static double bounce(double n) {
        double n1 = 7.5625;
        double d1 = 2.75;
        if (n < 1 / d1) {
            return n1 * n * n;
        }
        if (n < 2 / d1) {
            n -= 1.5 / d1;
            return n1 * n * n + 0.75;
        }
        if (n < 2.5 / d1) {
            n -= 2.25 / d1;
            return n1 * n * n + 0.9375;
        }
        n -= 2.625 / d1;
        return n1 * n * n + 0.984375;
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

    private static Vector3f convert(Vector3f raw, Channel channel) {
        return switch (channel) {
            case ROTATION -> new Vector3f(raw.x * DEG_TO_RAD, raw.y * DEG_TO_RAD, raw.z * DEG_TO_RAD);
            case SCALE -> new Vector3f(raw.x, raw.y, raw.z);
            case POSITION -> new Vector3f(raw.x, -raw.y, raw.z);
        };
    }

    public static Optional<BedrockAnimation> load(ResourceLocation file, String animationName) {
        return load(file, animationName, false);
    }

    public static Optional<BedrockAnimation> loadGeckoLib(ResourceLocation file, String animationName) {
        return load(file, animationName, true);
    }

    public static Optional<BedrockAnimation> loadIfPresent(ResourceLocation file, String animationName) {
        return load(file, animationName, false, true);
    }

    private static Optional<BedrockAnimation> load(ResourceLocation file, String animationName, boolean geckoLib) {
        return load(file, animationName, geckoLib, geckoLib);
    }

    private static Optional<BedrockAnimation> load(ResourceLocation file, String animationName, boolean geckoLib,
                                                   boolean quiet) {
        try {
            Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(file);
            if (resource.isEmpty()) {
                if (!quiet) {
                    Jojoha.LOGGER.error("animation file not found: {}", file);
                }
                return Optional.empty();
            }

            JsonObject root;
            try (BufferedReader reader = resource.get().openAsReader()) {
                root = GsonHelper.parse(reader);
            }

            JsonObject animations = GsonHelper.getAsJsonObject(root, "animations");
            if (!animations.has(animationName)) {
                if (!quiet) {
                    Jojoha.LOGGER.error("animation '{}' missing from {}", animationName, file);
                }
                return Optional.empty();
            }

            return Optional.of(read(GsonHelper.getAsJsonObject(animations, animationName), geckoLib));
        } catch (Exception e) {
            Jojoha.LOGGER.error("failed to load animation '{}' from {}", animationName, file, e);
            return Optional.empty();
        }
    }

    public static Optional<BedrockAnimation> parse(JsonObject root, String animationName) {
        try {
            JsonObject animations = GsonHelper.getAsJsonObject(root, "animations");
            if (!animations.has(animationName)) {
                return Optional.empty();
            }
            return Optional.of(read(GsonHelper.getAsJsonObject(animations, animationName), false));
        } catch (Exception e) {
            Jojoha.LOGGER.warn("could not read animation '{}': {}", animationName, e.getMessage());
            return Optional.empty();
        }
    }

    private static BedrockAnimation read(JsonObject animation, boolean geckoLib) {
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

        return new BedrockAnimation(length, bones, marker(animation));
    }

    private static float marker(JsonObject animation) {
        float first = -1F;
        for (String track : new String[] {"timeline", "sound_effects", "particle_effects"}) {
            if (!animation.has(track) || !animation.get(track).isJsonObject()) {
                continue;
            }
            for (String key : animation.getAsJsonObject(track).keySet()) {
                try {
                    float time = Float.parseFloat(key);
                    if (first < 0F || time < first) {
                        first = time;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return first;
    }

    private static Optional<List<Keyframe>> readChannel(JsonObject boneObj, String channelName) {
        if (!boneObj.has(channelName)) {
            return Optional.empty();
        }

        JsonElement channel = boneObj.get(channelName);
        List<Keyframe> frames = new ArrayList<>();

        if (channel.isJsonArray()) {
            if (!isVector(channel)) {
                Jojoha.LOGGER.warn("skipping a {} channel that is not three numbers: {}", channelName, channel);
                return Optional.empty();
            }
            frames.add(new Keyframe(0F, readVector(channel), false, null));
            return Optional.of(frames);
        }

        JsonObject channelObj = channel.getAsJsonObject();
        if (channelObj.has("vector")) {
            frames.add(new Keyframe(0F, readVector(channelObj.get("vector")), false, null));
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
            String easing = null;
            if (value.isJsonObject()) {
                JsonObject valueObj = value.getAsJsonObject();
                catmullrom = "catmullrom".equals(GsonHelper.getAsString(valueObj, "lerp_mode", ""));
                if (valueObj.has("easing") && valueObj.get("easing").isJsonPrimitive()) {
                    easing = valueObj.get("easing").getAsString().toLowerCase(Locale.ROOT);
                }
                if (valueObj.has("post")) {
                    value = valueObj.get("post");
                } else if (valueObj.has("vector")) {
                    value = valueObj.get("vector");
                }
                if (value.isJsonObject() && value.getAsJsonObject().has("vector")) {
                    value = value.getAsJsonObject().get("vector");
                }
            }

            if (!isVector(value)) {
                continue;
            }
            frames.add(new Keyframe(time, readVector(value), catmullrom, easing));
        }

        frames.sort((a, b) -> Float.compare(a.time, b.time));
        return frames.isEmpty() ? Optional.empty() : Optional.of(frames);
    }

    private static boolean isVector(JsonElement element) {
        if (!element.isJsonArray() || element.getAsJsonArray().size() < 3) {
            return false;
        }
        for (int i = 0; i < 3; i++) {
            JsonElement part = element.getAsJsonArray().get(i);
            if (!part.isJsonPrimitive() || !part.getAsJsonPrimitive().isNumber()) {
                return false;
            }
        }
        return true;
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

    private record Keyframe(float time, Vector3f value, boolean catmullrom, String easing) {
    }
}
