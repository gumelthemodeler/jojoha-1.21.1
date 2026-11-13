package org.gumel.jojoha.supporter;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.platform.Platform;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.gumel.jojoha.Jojoha;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class SupporterGate {
    public static final String BUILD_REWARD = "jojoha:supporter_build";
    public static final String DISCORD = "https://discord.gg/29wFh8TAK2";
    public static final boolean ACTIVE = SupporterGate.class.getResource("/jojoha_supporter_build") != null;
    private static final long GRACE_MS = Duration.ofDays(7).toMillis();
    private static final Duration TIMEOUT = Duration.ofSeconds(8);
    private static final Object FILE_LOCK = new Object();

    public enum Verdict {
        ALLOWED, DENIED, UNREACHABLE
    }

    private SupporterGate() {
    }

    public static void init() {
        if (ACTIVE) {
            Jojoha.LOGGER.info("Supporter build: players must be whitelisted in the Discord");
            PlayerEvent.PLAYER_JOIN.register(SupporterGate::joined);
        }
    }

    public static CompletableFuture<Verdict> check(UUID uuid) {
        if (!SupporterLink.apiSet()) {
            return CompletableFuture.completedFuture(settle(uuid, Verdict.UNREACHABLE));
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create(SupporterLink.api() + "/v1/players/" + uuid))
                .timeout(TIMEOUT)
                .GET()
                .build();
        return SupporterLink.HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(SupporterGate::read)
                .exceptionally(error -> Verdict.UNREACHABLE)
                .thenApply(verdict -> settle(uuid, verdict));
    }

    private static Verdict read(HttpResponse<String> response) {
        if (response.statusCode() != 200) {
            return Verdict.UNREACHABLE;
        }
        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        JsonArray rewards = json.has("rewards") ? json.getAsJsonArray("rewards") : new JsonArray();
        for (JsonElement reward : rewards) {
            if (BUILD_REWARD.equals(reward.getAsString())) {
                return Verdict.ALLOWED;
            }
        }
        return Verdict.DENIED;
    }

    private static Verdict settle(UUID uuid, Verdict verdict) {
        synchronized (FILE_LOCK) {
            JsonObject seen = loadSeen();
            String key = uuid.toString();
            if (verdict == Verdict.ALLOWED) {
                seen.addProperty(key, System.currentTimeMillis());
                saveSeen(seen);
                return verdict;
            }
            if (verdict == Verdict.DENIED) {
                if (seen.remove(key) != null) {
                    saveSeen(seen);
                }
                return verdict;
            }
            long last = seen.has(key) ? seen.get(key).getAsLong() : 0L;
            long age = System.currentTimeMillis() - last;
            return age >= 0 && age < GRACE_MS ? Verdict.ALLOWED : verdict;
        }
    }

    private static void joined(ServerPlayer player) {
        UUID uuid = player.getUUID();
        check(uuid).thenAccept(verdict -> {
            if (verdict == Verdict.ALLOWED) {
                return;
            }
            player.server.execute(() -> {
                ServerPlayer online = player.server.getPlayerList().getPlayer(uuid);
                if (online != null) {
                    online.connection.disconnect(Component.translatable(verdict == Verdict.DENIED
                            ? "message.jojoha.supporter_build.kicked"
                            : "message.jojoha.supporter_build.kicked_offline", DISCORD));
                }
            });
        });
    }

    private static Path file() {
        return Platform.getConfigFolder().resolve("jojoha-supporter-build.json");
    }

    private static JsonObject loadSeen() {
        try {
            if (Files.exists(file())) {
                return JsonParser.parseString(Files.readString(file(), StandardCharsets.UTF_8)).getAsJsonObject();
            }
        } catch (IOException | RuntimeException error) {
            Jojoha.LOGGER.warn("Could not read the supporter build record", error);
        }
        return new JsonObject();
    }

    private static void saveSeen(JsonObject seen) {
        try {
            Files.createDirectories(file().getParent());
            Files.writeString(file(), seen.toString(), StandardCharsets.UTF_8);
        } catch (IOException error) {
            Jojoha.LOGGER.warn("Could not save the supporter build record", error);
        }
    }
}
