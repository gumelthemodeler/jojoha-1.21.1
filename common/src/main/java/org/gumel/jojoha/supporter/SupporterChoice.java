package org.gumel.jojoha.supporter;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.network.packet.SupporterChoicePacket;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SupporterChoice {
    private static int saved = SupporterAuras.NONE;
    private static boolean loaded;

    private SupporterChoice() {
    }

    public static void init() {
        ClientPlayerEvent.CLIENT_PLAYER_JOIN.register(player -> {
            if (NetworkManager.canServerReceive(SupporterChoicePacket.TYPE)) {
                NetworkManager.sendToServer(new SupporterChoicePacket(saved()));
            }
        });
    }

    private static Path file() {
        return Platform.getConfigFolder().resolve("jojoha-supporter.json");
    }

    private static int saved() {
        if (!loaded) {
            loaded = true;
            try {
                if (Files.exists(file())) {
                    JsonObject json = JsonParser.parseString(Files.readString(file(), StandardCharsets.UTF_8))
                            .getAsJsonObject();
                    int aura = json.has("aura") ? json.get("aura").getAsInt() : SupporterAuras.NONE;
                    saved = SupporterAuras.valid(aura) ? aura : SupporterAuras.NONE;
                }
            } catch (IOException | RuntimeException error) {
                saved = SupporterAuras.NONE;
            }
        }
        return saved;
    }

    public static void save(int aura) {
        if (!SupporterAuras.valid(aura)) {
            return;
        }
        saved = aura;
        loaded = true;
        JsonObject json = new JsonObject();
        json.addProperty("aura", aura);
        try {
            Files.createDirectories(file().getParent());
            Files.writeString(file(), json.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            Jojoha.LOGGER.warn("Could not save the supporter aura choice", e);
        }
    }
}
