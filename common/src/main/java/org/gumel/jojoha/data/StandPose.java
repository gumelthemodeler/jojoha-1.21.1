package org.gumel.jojoha.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record StandPose(List<Bone> bones) {
    public static final int SLOTS = 3;
    public static final float MOVE_LIMIT = 8F;
    public static final float TURN_LIMIT = 180F;
    public static final int CHANNELS = 6;

    public static final List<String> BONES = List.of("root", "Body", "upperTorso", "Head", "RightArm", "bone2",
            "LeftArm", "bone3", "RightLeg", "bone", "LeftLeg", "bone4");

    public static final StandPose EMPTY = new StandPose(List.of());

    public record Bone(String name, float x, float y, float z, float pitch, float yaw, float roll) {
        public static final Codec<Bone> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("bone").forGetter(Bone::name),
                Codec.FLOAT.optionalFieldOf("x", 0F).forGetter(Bone::x),
                Codec.FLOAT.optionalFieldOf("y", 0F).forGetter(Bone::y),
                Codec.FLOAT.optionalFieldOf("z", 0F).forGetter(Bone::z),
                Codec.FLOAT.optionalFieldOf("pitch", 0F).forGetter(Bone::pitch),
                Codec.FLOAT.optionalFieldOf("yaw", 0F).forGetter(Bone::yaw),
                Codec.FLOAT.optionalFieldOf("roll", 0F).forGetter(Bone::roll)
        ).apply(instance, Bone::new));

        public float[] values() {
            return new float[] {x, y, z, pitch, yaw, roll};
        }

        public static Bone of(String name, float[] values) {
            return new Bone(name, values[0], values[1], values[2], values[3], values[4], values[5]);
        }

        private boolean still() {
            return x == 0F && y == 0F && z == 0F && pitch == 0F && yaw == 0F && roll == 0F;
        }
    }

    public static final Codec<StandPose> CODEC = Bone.CODEC.listOf().xmap(StandPose::new, StandPose::bones);

    public boolean saved() {
        return !bones.isEmpty();
    }

    public float[] values(String name) {
        for (Bone bone : bones) {
            if (bone.name().equals(name)) {
                return bone.values();
            }
        }
        return new float[CHANNELS];
    }

    private static final Map<String, String> OPPOSITE = Map.of("RightArm", "LeftArm", "LeftArm", "RightArm",
            "bone2", "bone3", "bone3", "bone2", "RightLeg", "LeftLeg", "LeftLeg", "RightLeg", "bone", "bone4",
            "bone4", "bone");

    public static int opposite(int bone) {
        String other = OPPOSITE.get(BONES.get(bone));
        return other == null ? -1 : BONES.indexOf(other);
    }

    public static float[] mirrored(float[] values) {
        return new float[] {-values[0], values[1], values[2], values[3], -values[4], -values[5]};
    }

    public static void flip(float[][] pose) {
        float[][] before = new float[pose.length][];
        for (int i = 0; i < pose.length; i++) {
            before[i] = pose[i].clone();
        }
        for (int i = 0; i < pose.length; i++) {
            int other = opposite(i);
            pose[i] = mirrored(before[other < 0 ? i : other]);
        }
    }

    public static float limit(int channel) {
        return channel < 3 ? MOVE_LIMIT : TURN_LIMIT;
    }

    public StandPose tidy() {
        List<Bone> kept = new ArrayList<>();
        for (String name : BONES) {
            float[] values = values(name);
            for (int channel = 0; channel < CHANNELS; channel++) {
                float limit = limit(channel);
                values[channel] = Float.isFinite(values[channel]) ? Mth.clamp(values[channel], -limit, limit) : 0F;
            }
            Bone bone = Bone.of(name, values);
            if (!bone.still()) {
                kept.add(bone);
            }
        }
        return new StandPose(List.copyOf(kept));
    }

    public String clip() {
        JsonObject bonesJson = new JsonObject();
        for (String name : BONES) {
            float[] values = values(name);
            JsonObject entry = new JsonObject();
            entry.add("rotation", vector(values[3], values[4], values[5]));
            entry.add("position", vector(values[0], values[1], values[2]));
            bonesJson.add(name, entry);
        }
        JsonObject idle = new JsonObject();
        idle.addProperty("loop", true);
        idle.addProperty("animation_length", 1F);
        idle.add("bones", bonesJson);
        JsonObject animations = new JsonObject();
        animations.add("idle", idle);
        JsonObject root = new JsonObject();
        root.addProperty("format_version", "1.8.0");
        root.add("animations", animations);
        return root.toString();
    }

    private static JsonArray vector(float x, float y, float z) {
        JsonArray array = new JsonArray();
        array.add(x);
        array.add(y);
        array.add(z);
        return array;
    }
}
