package org.gumel.jojoha.supporter;

import org.gumel.jojoha.Jojoha;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;

import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public final class SkinCatalog {
    public static final int BASE = 1 << 20;
    public static final String TEXTURE = "texture";
    public static final String MODEL = "model";
    public static final String ANIMATIONS = "animations";
    private static final long STALE_MS = 10L * 60L * 1000L;
    private static final long RETRY_MS = 60L * 1000L;
    private static final int MAX_FILE_BYTES = 4 * 1024 * 1024;

    public static final int NO_AURA = -1;

    public record Entry(int id, String slug, ResourceLocation stand, String name, int version,
                        int aura, int aura2, Map<String, String> files) {
        public int skin() {
            return BASE + id;
        }

        public boolean has(String kind) {
            return files.containsKey(kind);
        }
    }

    private static volatile Map<Integer, Entry> entries = Map.of();
    private static volatile boolean loaded;
    private static volatile long fetchedAt;
    private static volatile long failedAt;
    private static final AtomicBoolean FETCHING = new AtomicBoolean();

    private SkinCatalog() {
    }

    public static boolean remote(int skin) {
        return skin >= BASE;
    }

    public static boolean loaded() {
        return loaded;
    }

    public static Entry get(int skin) {
        return remote(skin) ? entries.get(skin - BASE) : null;
    }

    public static List<Entry> forStand(ResourceLocation stand) {
        List<Entry> out = new ArrayList<>();
        for (Entry entry : entries.values()) {
            if (entry.stand().equals(stand)) {
                out.add(entry);
            }
        }
        out.sort((a, b) -> Integer.compare(a.id(), b.id()));
        return out;
    }

    public static Entry find(ResourceLocation stand, String slug) {
        for (Entry entry : entries.values()) {
            if (entry.stand().equals(stand) && entry.slug().equalsIgnoreCase(slug)) {
                return entry;
            }
        }
        return null;
    }

    public static void refreshIfStale() {
        long now = System.currentTimeMillis();
        if (loaded && now - fetchedAt < STALE_MS) {
            return;
        }
        if (now - failedAt < RETRY_MS) {
            return;
        }
        refresh();
    }

    public static CompletableFuture<Void> refresh() {
        if (!SupporterLink.apiSet() || !FETCHING.compareAndSet(false, true)) {
            return CompletableFuture.completedFuture(null);
        }
        return SupporterLink.HTTP.sendAsync(SupporterLink.request("/v1/skins").GET().build(),
                        HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    Map<Integer, Entry> parsed = response.statusCode() == 200 ? parse(response.body()) : null;
                    if (parsed == null) {
                        failedAt = System.currentTimeMillis();
                        return;
                    }
                    entries = parsed;
                    fetchedAt = System.currentTimeMillis();
                    loaded = true;
                })
                .exceptionally(error -> {
                    failedAt = System.currentTimeMillis();
                    Jojoha.LOGGER.debug("Skin catalog fetch failed", error);
                    return null;
                })
                .whenComplete((ignored, error) -> FETCHING.set(false));
    }

    static Map<Integer, Entry> parse(String body) {
        try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            Map<Integer, Entry> out = new LinkedHashMap<>();
            for (JsonElement element : json.getAsJsonArray("skins")) {
                JsonObject skin = element.getAsJsonObject();
                int id = skin.get("id").getAsInt();
                ResourceLocation stand = ResourceLocation.tryParse(skin.get("stand").getAsString());
                if (id <= 0 || stand == null) {
                    continue;
                }
                Map<String, String> files = new LinkedHashMap<>();
                JsonObject listed = skin.getAsJsonObject("files");
                for (String kind : List.of(TEXTURE, MODEL, ANIMATIONS)) {
                    if (listed.has(kind)) {
                        files.put(kind, listed.get(kind).getAsString());
                    }
                }
                if (!files.containsKey(TEXTURE)) {
                    continue;
                }
                out.put(id, new Entry(id, skin.get("slug").getAsString(), stand, skin.get("name").getAsString(),
                        skin.get("version").getAsInt(), colour(skin, "aura"), colour(skin, "aura2"),
                        Map.copyOf(files)));
            }
            return Map.copyOf(out);
        } catch (RuntimeException error) {
            Jojoha.LOGGER.debug("Malformed skin catalog", error);
            return null;
        }
    }

    private static int colour(JsonObject skin, String key) {
        if (!skin.has(key) || !skin.get(key).isJsonPrimitive()) {
            return NO_AURA;
        }
        int colour = skin.get(key).getAsInt();
        return colour >= 0 && colour <= 0xFFFFFF ? colour : NO_AURA;
    }

    public static CompletableFuture<byte[]> download(Entry entry, String kind) {
        String expected = entry.files().get(kind);
        if (expected == null) {
            return CompletableFuture.completedFuture(null);
        }
        return SupporterLink.HTTP.sendAsync(SupporterLink.request("/v1/skins/" + entry.id() + "/" + kind
                                + "?v=" + entry.version())
                        .setHeader("Accept", "*/*").GET().build(),
                HttpResponse.BodyHandlers.ofByteArray())
                .thenApply(response -> {
                    byte[] body = response.body();
                    if (response.statusCode() != 200 || body == null || body.length > MAX_FILE_BYTES) {
                        throw new IllegalStateException("Skin file " + entry.id() + "/" + kind
                                + " returned " + response.statusCode());
                    }
                    if (!expected.equalsIgnoreCase(sha256(body))) {
                        throw new IllegalStateException("Skin file " + entry.id() + "/" + kind
                                + " does not match its fingerprint");
                    }
                    return body;
                });
    }

    static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException(error);
        }
    }
}
