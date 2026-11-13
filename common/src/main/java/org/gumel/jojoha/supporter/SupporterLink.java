package org.gumel.jojoha.supporter;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.networking.NetworkManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.config.JojohaServerConfig;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.network.packet.SupporterPacket;
import org.gumel.jojoha.network.packet.SupporterSavePacket;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.StandSkinChoice;
import org.gumel.jojoha.stand.StandTypes;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SupporterLink {
    public static final String SKINS = "jojoha:supporter_skins";
    public static final String AURA = "jojoha:supporter_aura";
    private static final String DEFAULT_API = "https://jojoha-supporter-bot-production.up.railway.app";
    private static final String API = System.getProperty("jojoha.supporterApi", DEFAULT_API).trim()
            .replaceAll("/+$", "");
    private static final int REFRESH_TICKS = 12000;
    private static final Duration TIMEOUT = Duration.ofSeconds(8);
    static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    private static final Map<UUID, Perks> PERKS = new ConcurrentHashMap<>();
    private static final Set<UUID> LINKING = ConcurrentHashMap.newKeySet();

    private static final Map<UUID, Integer> CHOICES = new ConcurrentHashMap<>();
    private static final Map<UUID, Shown> SHOWN = new ConcurrentHashMap<>();

    public record Perks(boolean skins, boolean auraReward, int aura) {
        static final Perks NONE = new Perks(false, false, SupporterAuras.NONE);
    }

    private record Shown(int aura, boolean skins) {
        static final Shown NONE = new Shown(SupporterAuras.NONE, false);
    }

    private SupporterLink() {
    }

    public static void init() {
        PlayerEvent.PLAYER_JOIN.register(SupporterLink::joined);
        PlayerEvent.PLAYER_QUIT.register(SupporterLink::left);
        TickEvent.SERVER_POST.register(SupporterLink::tick);
        LifecycleEvent.SERVER_STOPPED.register(server -> {
            PERKS.clear();
            LINKING.clear();
            CHOICES.clear();
            SHOWN.clear();
        });
        CommandRegistrationEvent.EVENT.register((dispatcher, registry, selection) -> commands(dispatcher));
    }

    static String api() {
        return API;
    }

    static boolean apiSet() {
        return !API.isEmpty();
    }

    public static boolean enabled() {
        return !API.isEmpty() && JojohaServerConfig.SUPPORTER_PERKS.on();
    }

    public static void chose(ServerPlayer player, int aura) {
        if (!SupporterAuras.valid(aura)) {
            return;
        }
        if (aura == SupporterAuras.NONE) {
            CHOICES.remove(player.getUUID());
        } else {
            CHOICES.put(player.getUUID(), aura);
        }
        publish(player.server, player.getUUID());
    }

    public static boolean hasSkins(ServerPlayer player) {
        return PERKS.getOrDefault(player.getUUID(), Perks.NONE).skins();
    }

    private static void commands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("supporter")
                .executes(ctx -> {
                    status(ctx.getSource().getPlayerOrException());
                    return 1;
                })
                .then(Commands.literal("link")
                        .then(Commands.argument("code", StringArgumentType.word())
                                .executes(ctx -> {
                                    link(ctx.getSource().getPlayerOrException(),
                                            StringArgumentType.getString(ctx, "code"));
                                    return 1;
                                })))
                .then(Commands.literal("skin")
                        .executes(ctx -> {
                            listSkins(ctx.getSource().getPlayerOrException());
                            return 1;
                        })
                        .then(Commands.literal("reset")
                                .executes(ctx -> {
                                    resetSkin(ctx.getSource().getPlayerOrException());
                                    return 1;
                                }))
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                        skinIds(ctx.getSource()), builder))
                                .executes(ctx -> {
                                    wearSkin(ctx.getSource().getPlayerOrException(),
                                            StringArgumentType.getString(ctx, "id"));
                                    return 1;
                                })))
                .then(Commands.literal("aura")
                        .executes(ctx -> {
                            showAura(ctx.getSource().getPlayerOrException());
                            return 1;
                        })
                        .then(Commands.literal("reset")
                                .executes(ctx -> {
                                    resetAura(ctx.getSource().getPlayerOrException());
                                    return 1;
                                }))
                        .then(Commands.argument("color", StringArgumentType.greedyString())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                        SupporterAuras.PRESETS.keySet().stream().sorted(), builder))
                                .executes(ctx -> {
                                    setAura(ctx.getSource().getPlayerOrException(),
                                            StringArgumentType.getString(ctx, "color"));
                                    return 1;
                                }))));
    }

    private static ResourceLocation standOf(ServerPlayer player) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        return data.stand.isPresent() ? data.stand.standId() : null;
    }

    private static List<String> skinIds(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        ResourceLocation stand = player == null ? null : standOf(player);
        return stand == null ? List.of()
                : SkinCatalog.forStand(stand).stream().map(SkinCatalog.Entry::slug).toList();
    }

    private static ResourceLocation skinStand(ServerPlayer player) {
        if (!enabled()) {
            say(player, Component.translatable("message.jojoha.supporter.off"));
            return null;
        }
        if (!hasSkins(player)) {
            say(player, Component.translatable("message.jojoha.supporter.skin_locked"));
            say(player, Component.translatable("message.jojoha.supporter.how"));
            return null;
        }
        ResourceLocation stand = standOf(player);
        if (stand == null) {
            say(player, Component.translatable("message.jojoha.supporter.skin_no_stand"));
            return null;
        }
        if (!SkinCatalog.loaded()) {
            SkinCatalog.refreshIfStale();
            say(player, Component.translatable("message.jojoha.supporter.skin_loading"));
            return null;
        }
        return stand;
    }

    private static void listSkins(ServerPlayer player) {
        ResourceLocation stand = skinStand(player);
        if (stand == null) {
            return;
        }
        List<SkinCatalog.Entry> skins = SkinCatalog.forStand(stand);
        if (skins.isEmpty()) {
            say(player, Component.translatable("message.jojoha.supporter.skin_none"));
            return;
        }
        say(player, Component.translatable("message.jojoha.supporter.skin_list", String.join(", ",
                skins.stream().map(skin -> skin.name() + " (" + skin.slug() + ")").toList())));
        say(player, Component.translatable("message.jojoha.supporter.skin_help"));
    }

    private static void wearSkin(ServerPlayer player, String slug) {
        ResourceLocation stand = skinStand(player);
        if (stand == null) {
            return;
        }
        SkinCatalog.Entry entry = SkinCatalog.find(stand, slug);
        if (entry == null) {
            say(player, Component.translatable("message.jojoha.supporter.skin_unknown", slug));
            return;
        }
        StandSkinChoice.equip(player, entry.skin());
        say(player, PlayerDataAccess.get(player).stand.skin() == entry.skin()
                ? Component.translatable("message.jojoha.supporter.skin_set", entry.name())
                : Component.translatable("message.jojoha.supporter.skin_busy"));
    }

    private static void resetSkin(ServerPlayer player) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (!data.stand.isPresent() || !SkinCatalog.remote(data.stand.skin())) {
            say(player, Component.translatable("message.jojoha.supporter.skin_not_worn"));
            return;
        }
        StandSkinChoice.equip(player, 0);
        say(player, SkinCatalog.remote(PlayerDataAccess.get(player).stand.skin())
                ? Component.translatable("message.jojoha.supporter.skin_busy")
                : Component.translatable("message.jojoha.supporter.skin_reset"));
    }

    private static boolean auraAllowed(ServerPlayer player) {
        if (!enabled()) {
            say(player, Component.translatable("message.jojoha.supporter.off"));
            return false;
        }
        if (!PERKS.getOrDefault(player.getUUID(), Perks.NONE).auraReward()) {
            say(player, Component.translatable("message.jojoha.supporter.aura_locked"));
            say(player, Component.translatable("message.jojoha.supporter.how"));
            return false;
        }
        return true;
    }

    private static void setAura(ServerPlayer player, String raw) {
        if (!auraAllowed(player)) {
            return;
        }
        Integer aura = SupporterAuras.parse(raw);
        if (aura == null) {
            say(player, Component.translatable("message.jojoha.supporter.aura_bad",
                    String.join(", ", SupporterAuras.PRESETS.keySet().stream().sorted().toList())));
            return;
        }
        chose(player, aura);
        NetworkManager.sendToPlayer(player, new SupporterSavePacket(aura));
        say(player, Component.translatable("message.jojoha.supporter.aura_set", SupporterAuras.describe(aura)));
    }

    private static void resetAura(ServerPlayer player) {
        if (!auraAllowed(player)) {
            return;
        }
        chose(player, SupporterAuras.NONE);
        NetworkManager.sendToPlayer(player, new SupporterSavePacket(SupporterAuras.NONE));
        int discord = PERKS.getOrDefault(player.getUUID(), Perks.NONE).aura();
        say(player, discord == SupporterAuras.NONE
                ? Component.translatable("message.jojoha.supporter.aura_reset")
                : Component.translatable("message.jojoha.supporter.aura_reset_discord",
                        SupporterAuras.describe(discord)));
    }

    private static void showAura(ServerPlayer player) {
        if (!auraAllowed(player)) {
            return;
        }
        int aura = shownAura(player.getUUID());
        say(player, aura == SupporterAuras.NONE
                ? Component.translatable("message.jojoha.supporter.aura_own")
                : Component.translatable("message.jojoha.supporter.aura_is", SupporterAuras.describe(aura)));
        say(player, Component.translatable("message.jojoha.supporter.aura_help"));
    }

    public static int auraOf(UUID id) {
        Shown shown = SHOWN.get(id);
        return shown == null ? SupporterAuras.NONE : shown.aura();
    }

    private static int shownAura(UUID id) {
        Perks perks = PERKS.getOrDefault(id, Perks.NONE);
        if (!perks.auraReward()) {
            return SupporterAuras.NONE;
        }
        Integer choice = CHOICES.get(id);
        return choice != null ? choice : perks.aura();
    }

    private static void joined(ServerPlayer player) {
        MinecraftServer server = player.server;
        SHOWN.forEach((id, shown) -> {
            if (!id.equals(player.getUUID()) && server.getPlayerList().getPlayer(id) != null) {
                NetworkManager.sendToPlayer(player, new SupporterPacket(id, shown.aura(), shown.skins()));
            }
        });
        lookup(player);
    }

    private static void left(ServerPlayer player) {
        PERKS.remove(player.getUUID());
        CHOICES.remove(player.getUUID());
        if (SHOWN.remove(player.getUUID()) != null) {
            broadcast(player.server, player.getUUID(), Shown.NONE);
        }
        LINKING.remove(player.getUUID());
    }

    private static void tick(MinecraftServer server) {
        if (!enabled() || server.getTickCount() % REFRESH_TICKS != 0) {
            return;
        }
        SkinCatalog.refresh();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            lookup(player);
        }
    }

    static HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(URI.create(API + path))
                .timeout(TIMEOUT)
                .header("User-Agent", Jojoha.MOD_ID)
                .header("Accept", "application/json");
    }

    private static void lookup(ServerPlayer player) {
        if (!enabled()) {
            return;
        }
        SkinCatalog.refreshIfStale();
        MinecraftServer server = player.server;
        UUID id = player.getUUID();
        HTTP.sendAsync(request("/v1/players/" + id).GET().build(), HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() != 200) {
                        return;
                    }
                    Perks perks = parse(response.body());
                    if (perks != null) {
                        server.execute(() -> apply(server, id, perks));
                    }
                })
                .exceptionally(error -> {
                    Jojoha.LOGGER.debug("Supporter lookup failed for {}", id, error);
                    return null;
                });
    }

    private static Perks parse(String body) {
        try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            boolean skins = false;
            boolean aura = false;
            if (json.has("rewards") && json.get("rewards").isJsonArray()) {
                for (JsonElement reward : json.getAsJsonArray("rewards")) {
                    skins |= SKINS.equals(reward.getAsString());
                    aura |= AURA.equals(reward.getAsString());
                }
            }
            JsonElement colour = json.get("aura");
            int tint = aura && colour != null && colour.isJsonPrimitive() ? colour.getAsInt() : SupporterAuras.NONE;
            return new Perks(skins, aura, SupporterAuras.valid(tint) ? tint : SupporterAuras.NONE);
        } catch (RuntimeException error) {
            Jojoha.LOGGER.debug("Malformed supporter response", error);
            return null;
        }
    }

    private static void apply(MinecraftServer server, UUID id, Perks perks) {
        ServerPlayer player = server.getPlayerList().getPlayer(id);
        if (player == null) {
            return;
        }
        PERKS.put(id, perks);
        publish(server, id);
        strip(player, !perks.skins());
    }

    private static void publish(MinecraftServer server, UUID id) {
        Perks perks = PERKS.get(id);
        if (perks == null || server.getPlayerList().getPlayer(id) == null) {
            return;
        }
        Shown shown = new Shown(shownAura(id), perks.skins());
        if (!shown.equals(SHOWN.put(id, shown))) {
            broadcast(server, id, shown);
        }
    }

    private static void broadcast(MinecraftServer server, UUID id, Shown shown) {
        SupporterPacket packet = new SupporterPacket(id, shown.aura(), shown.skins());
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            NetworkManager.sendToPlayer(player, packet);
        }
    }

    private static void strip(ServerPlayer player, boolean lostPerk) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (!data.stand.isPresent()) {
            return;
        }
        int skin = data.stand.skin();
        boolean supporter = StandTypes.byIdOrDefault(data.stand.standId()).supporterSkin(skin);
        SkinCatalog.Entry entry = SkinCatalog.get(skin);
        boolean gone = SkinCatalog.remote(skin) && SkinCatalog.loaded()
                && (entry == null || !entry.stand().equals(data.stand.standId()));
        if (!(supporter && lostPerk) && !gone) {
            return;
        }
        data.stand = data.stand.withSkin(0);
        StandEntity stand = data.standSummoned ? StandEntity.findFor(player) : null;
        if (stand != null) {
            stand.setSkin(0);
        }
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
        player.sendSystemMessage(Component.translatable(gone
                        ? "message.jojoha.supporter.skin_gone" : "message.jojoha.supporter.skin_removed")
                .withStyle(ChatFormatting.GRAY));
    }

    private static void link(ServerPlayer player, String code) {
        if (!enabled()) {
            say(player, Component.translatable("message.jojoha.supporter.off"));
            return;
        }
        UUID id = player.getUUID();
        if (!LINKING.add(id)) {
            return;
        }
        say(player, Component.translatable("message.jojoha.supporter.linking"));
        JsonObject body = new JsonObject();
        body.addProperty("code", code);
        body.addProperty("uuid", id.toString());
        body.addProperty("name", player.getGameProfile().getName());
        MinecraftServer server = player.server;
        HttpRequest request = request("/v1/link")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();
        HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> server.execute(() -> linked(server, id, response.statusCode(), response.body())))
                .exceptionally(error -> {
                    server.execute(() -> linked(server, id, 0, ""));
                    return null;
                });
    }

    private static void linked(MinecraftServer server, UUID id, int status, String body) {
        LINKING.remove(id);
        ServerPlayer player = server.getPlayerList().getPlayer(id);
        if (player == null) {
            return;
        }
        if (status == 200) {
            Perks perks = parse(body);
            String discord = discordName(body);
            say(player, discord == null
                    ? Component.translatable("message.jojoha.supporter.linked_plain")
                    : Component.translatable("message.jojoha.supporter.linked", discord));
            if (perks != null) {
                apply(server, id, perks);
                say(player, describe(perks));
            }
            return;
        }
        say(player, Component.translatable(switch (status) {
            case 400, 404 -> "message.jojoha.supporter.bad_code";
            case 429 -> "message.jojoha.supporter.slow_down";
            default -> "message.jojoha.supporter.unreachable";
        }));
    }

    private static String discordName(String body) {
        try {
            JsonElement name = JsonParser.parseString(body).getAsJsonObject().get("discord");
            return name != null && name.isJsonPrimitive() ? name.getAsString() : null;
        } catch (RuntimeException error) {
            return null;
        }
    }

    private static void status(ServerPlayer player) {
        if (!enabled()) {
            say(player, Component.translatable("message.jojoha.supporter.off"));
            return;
        }
        Perks perks = PERKS.get(player.getUUID());
        if (perks == null || (!perks.skins() && !perks.auraReward())) {
            say(player, Component.translatable("message.jojoha.supporter.none"));
            say(player, Component.translatable("message.jojoha.supporter.how"));
            return;
        }
        say(player, describe(perks));
        if (perks.auraReward()) {
            say(player, Component.translatable("message.jojoha.supporter.aura_help"));
        }
    }

    private static Component describe(Perks perks) {
        if (!perks.skins() && !perks.auraReward()) {
            return Component.translatable("message.jojoha.supporter.none");
        }
        MutableComponent line = Component.translatable("message.jojoha.supporter.perks");
        if (perks.skins()) {
            line.append(Component.literal(" "))
                    .append(Component.translatable("message.jojoha.supporter.perk_skins"));
        }
        if (perks.auraReward()) {
            line.append(Component.literal(" "))
                    .append(Component.translatable("message.jojoha.supporter.perk_aura"));
        }
        return line;
    }

    private static void say(ServerPlayer player, Component message) {
        player.sendSystemMessage(message.copy().withStyle(ChatFormatting.GRAY));
    }
}
