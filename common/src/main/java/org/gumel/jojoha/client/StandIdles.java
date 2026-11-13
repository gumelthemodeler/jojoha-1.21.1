package org.gumel.jojoha.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.GsonHelper;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.client.anim.BedrockAnimation;
import org.gumel.jojoha.data.ClientPlayerDataCache;
import org.gumel.jojoha.data.StandPose;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.network.packet.StandIdlePacket;
import org.gumel.jojoha.stand.StandCustomization;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class StandIdles {
    public record Entry(String file, String animation) {
        public String key() {
            return file + "#" + animation;
        }

        public int pose() {
            return POSE_FILE.equals(file) ? Integer.parseInt(animation) : -1;
        }

        public boolean builtIn() {
            return file.startsWith(StandCustomization.BUILT_IN_PREFIX);
        }

        public String label() {
            if (pose() >= 0) {
                return Component.translatable("menu.jojoha.custom.idle.pose", pose() + 1).getString();
            }
            if (builtIn()) {
                String base = file.substring(file.lastIndexOf('/') + 1).replace(".animation.json", "");
                return Component.translatable("menu.jojoha.custom.idle.builtin." + base).getString();
            }
            String name = animation;
            int dot = name.lastIndexOf('.');
            if (dot >= 0 && dot < name.length() - 1) {
                name = name.substring(dot + 1);
            }
            String base = file.endsWith(".json") ? file.substring(0, file.length() - 5) : file;
            return base + " - " + name;
        }
    }

    public enum Status {
        NONE,
        SENT,
        TOO_LARGE,
        UNREADABLE,
        MISSING
    }

    private static final String CLIP_NAME = "idle";

    public static final List<Entry> BUILT_IN = List.of(
            builtIn("universal", "idle"),
            builtIn("theworld", "idle"),
            builtIn("magicians_red", "idle_pose"),
            builtIn("silverchariot", "idle_pose"));

    public static final String POSE_FILE = StandCustomization.BUILT_IN_PREFIX + "pose";

    public static final List<Entry> POSES = poses();

    private static List<Entry> found = List.of();
    private static String sentKey;
    private static Status status = Status.NONE;
    private static Object sentFor;
    private static BedrockAnimation own;
    private static BedrockAnimation editing;

    private static final Map<Integer, Optional<BedrockAnimation>> CLIPS = new HashMap<>();
    private static final Set<Integer> ASKED = new HashSet<>();

    private StandIdles() {
    }

    public static Path folder() {
        Path dir = Minecraft.getInstance().gameDirectory.toPath().resolve("jojoha").resolve("idles");
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            Jojoha.LOGGER.warn("Could not create idle folder {}", dir, e);
        }
        return dir;
    }

    private static List<Entry> poses() {
        List<Entry> poses = new ArrayList<>();
        for (int slot = 0; slot < StandPose.SLOTS; slot++) {
            poses.add(new Entry(POSE_FILE, Integer.toString(slot)));
        }
        return List.copyOf(poses);
    }

    public static int chosenPose() {
        String key = chosen();
        String prefix = POSE_FILE + "#";
        if (!key.startsWith(prefix)) {
            return -1;
        }
        try {
            int slot = Integer.parseInt(key.substring(prefix.length()));
            return slot >= 0 && slot < StandPose.SLOTS ? slot : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static void resend() {
        send();
    }

    public static void editing(StandPose pose) {
        editing = pose == null ? null : parse(pose.clip()).orElse(null);
    }

    private static Entry builtIn(String file, String animation) {
        return new Entry(StandCustomization.BUILT_IN_PREFIX + "animations/" + file + ".animation.json", animation);
    }

    public static List<Entry> found() {
        return found;
    }

    public static Status status() {
        return status;
    }

    public static List<Entry> scan() {
        List<Entry> entries = new ArrayList<>();
        try (DirectoryStream<Path> files = Files.newDirectoryStream(folder(), "*.json")) {
            List<Path> sorted = new ArrayList<>();
            files.forEach(sorted::add);
            sorted.sort((a, b) -> a.getFileName().toString().compareToIgnoreCase(b.getFileName().toString()));
            for (Path path : sorted) {
                JsonObject animations = animations(path);
                if (animations == null) {
                    continue;
                }
                for (String name : animations.keySet()) {
                    entries.add(new Entry(path.getFileName().toString(), name));
                }
            }
        } catch (IOException e) {
            Jojoha.LOGGER.warn("Could not list idle animations", e);
        }
        found = List.copyOf(entries);
        return found;
    }

    private static JsonObject animations(Path path) {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonObject root = GsonHelper.parse(reader);
            return GsonHelper.getAsJsonObject(root, "animations", null);
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    public static String chosen() {
        return ClientPlayerDataCache.data.standIdle;
    }

    public static void choose(Entry entry) {
        choose(entry == null ? "" : entry.key());
    }

    public static void choose(String key) {
        ClientPlayerDataCache.data.standIdle = key;
        send();
    }

    public static BedrockAnimation own() {
        return editing != null ? editing : own;
    }

    private static void send() {
        own = null;
        String key = chosen();
        sentKey = key;
        if (key.isEmpty()) {
            status = Status.NONE;
            NetworkHandler.sendStandIdle("", "");
            return;
        }
        int split = key.lastIndexOf('#');
        String file = split <= 0 ? key : key.substring(0, split);
        String clip = split <= 0 ? null : clip(file, key.substring(split + 1));
        if (clip == null) {
            status = !file.startsWith(StandCustomization.BUILT_IN_PREFIX) && Files.exists(folder().resolve(file))
                    ? Status.UNREADABLE : Status.MISSING;
            NetworkHandler.sendStandIdle(key, "");
            return;
        }
        if (clip.length() > StandIdlePacket.MAX_CHARS) {
            status = Status.TOO_LARGE;
            NetworkHandler.sendStandIdle(key, "");
            return;
        }
        status = Status.SENT;
        own = parse(clip).orElse(null);
        NetworkHandler.sendStandIdle(key, clip);
    }

    private static String clip(String file, String animation) {
        JsonObject animations;
        if (POSE_FILE.equals(file)) {
            int slot = chosenPose();
            return slot < 0 ? null : ClientPlayerDataCache.data.poses[slot].clip();
        } else if (file.startsWith(StandCustomization.BUILT_IN_PREFIX)) {
            animations = packed(file);
        } else if (file.contains("/") || file.contains("\\") || !file.toLowerCase(Locale.ROOT).endsWith(".json")) {
            return null;
        } else {
            animations = animations(folder().resolve(file));
        }
        if (animations == null || !animations.has(animation) || !animations.get(animation).isJsonObject()) {
            return null;
        }
        JsonObject wrapped = new JsonObject();
        wrapped.addProperty("format_version", "1.8.0");
        JsonObject only = new JsonObject();
        only.add(CLIP_NAME, animations.get(animation));
        wrapped.add("animations", only);
        return wrapped.toString();
    }

    private static JsonObject packed(String file) {
        ResourceLocation id = ResourceLocation.tryParse(file);
        Optional<Resource> resource = id == null ? Optional.empty()
                : Minecraft.getInstance().getResourceManager().getResource(id);
        if (resource.isEmpty()) {
            return null;
        }
        try (Reader reader = resource.get().openAsReader()) {
            return GsonHelper.getAsJsonObject(GsonHelper.parse(reader), "animations", null);
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        Object connection = minecraft.getConnection();
        if (connection == null || minecraft.player == null) {
            if (connection == null) {
                sentFor = null;
                sentKey = null;
                CLIPS.clear();
                ASKED.clear();
            }
            return;
        }
        if (connection != sentFor) {
            sentFor = connection;
            sentKey = null;
            CLIPS.clear();
            ASKED.clear();
        }
        if (sentKey == null && ClientPlayerDataCache.synced()) {
            if (chosen().isEmpty()) {
                sentKey = "";
            } else {
                send();
            }
        }
    }

    public static BedrockAnimation clip(int stamp) {
        if (stamp == 0) {
            return null;
        }
        Optional<BedrockAnimation> known = CLIPS.get(stamp);
        if (known != null) {
            return known.orElse(null);
        }
        if (ASKED.add(stamp)) {
            NetworkHandler.sendRequestStandIdle(stamp);
        }
        return null;
    }

    public static void heard(int stamp, String clip) {
        CLIPS.put(stamp, parse(clip));
    }

    private static Optional<BedrockAnimation> parse(String clip) {
        try {
            JsonObject root = GsonHelper.parse(clip);
            JsonObject animations = GsonHelper.getAsJsonObject(root, "animations");
            String name = null;
            for (Map.Entry<String, JsonElement> entry : animations.entrySet()) {
                name = entry.getKey();
            }
            return name == null ? Optional.empty() : BedrockAnimation.parse(root, name);
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }
}
