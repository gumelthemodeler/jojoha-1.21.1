package org.gumel.jojoha.fortune;

import dev.architectury.event.EventResult;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public final class Gambler {
    public static final int WIN_LEVELS = 2;

    public static final float LOSE_SHARE = 1F / 3F;

    public static final double BAD_LUCK = -1.0;

    public static final int BAD_LUCK_TICKS = 24000;

    public static final int OFFER_TICKS = 600;

    public static final int DUEL_TICKS = 6000;

    public static final int MOST_WORDS = 120;

    private static final int CHECK_EVERY = 20;

    private static final ResourceLocation LUCK_ID = Fortunes.id("fortune_gambler_luck");

    private record Prompt(UUID target, long until) {
    }

    private record Offer(UUID from, String words, long until) {
    }

    private record Duel(UUID gambler, UUID bettor, ItemStack stake, String words, long until) {
        UUID other(UUID id) {
            return id.equals(gambler) ? bettor : gambler;
        }
    }

    private static final Map<UUID, Prompt> PROMPTS = new HashMap<>();

    private static final Map<UUID, Offer> OFFERS = new HashMap<>();

    private static final Map<UUID, Duel> DUELS = new HashMap<>();

    private static final Set<UUID> SECOND_ROLL = new HashSet<>();

    private Gambler() {
    }

    static void tick(ServerPlayer player, JojohaPlayerData data, boolean gambler) {
        long now = player.level().getGameTime();
        Fortunes.modifier(player, Attributes.LUCK, LUCK_ID,
                gambler && now < data.fortuneUntil ? BAD_LUCK : 0.0, AttributeModifier.Operation.ADD_VALUE);
        if (player.tickCount % CHECK_EVERY != 0) {
            return;
        }
        Prompt prompt = PROMPTS.get(player.getUUID());
        if (prompt != null && now > prompt.until()) {
            PROMPTS.remove(player.getUUID());
        }
        Offer offer = OFFERS.get(player.getUUID());
        if (offer != null && now > offer.until()) {
            OFFERS.remove(player.getUUID());
            if (gambler) {
                refused(player, data);
            }
        }
        Duel duel = DUELS.get(player.getUUID());
        if (duel != null && now > duel.until()) {
            settle(player.getServer(), duel, null);
        }
    }

    public static boolean calls(Player player) {
        return player instanceof ServerPlayer server && player.isShiftKeyDown() && Fortunes.on()
                && Fortune.has(PlayerDataAccess.get(server), Fortune.GAMBLER);
    }

    public static boolean flip(ServerPlayer player) {
        boolean won = player.getRandom().nextBoolean();
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.CHAIN_PLACE,
                SoundSource.PLAYERS, 0.7F, won ? 1.8F : 0.7F);
        if (!won) {
            player.serverLevel().sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY(1.0),
                    player.getZ(), 8, 0.3, 0.3, 0.3, 0.01);
        }
        Fortunes.whisper(player, Component.translatable(won
                ? "message.jojoha.fortune.gambler.won" : "message.jojoha.fortune.gambler.lost"));
        return won;
    }

    public static void doubleRoll(ServerPlayer player) {
        SECOND_ROLL.add(player.getUUID());
    }

    public static boolean secondRoll(ServerPlayer player) {
        return SECOND_ROLL.remove(player.getUUID());
    }

    static EventResult chest(Player player, InteractionHand hand, BlockPos pos) {
        if (hand != InteractionHand.MAIN_HAND || !(player instanceof ServerPlayer server) || !calls(server)
                || !server.getMainHandItem().isEmpty() || !server.getOffhandItem().isEmpty()) {
            return EventResult.pass();
        }
        BlockEntity block = server.level().getBlockEntity(pos);
        if (!(block instanceof RandomizableContainerBlockEntity container) || container.getLootTable() == null) {
            return EventResult.pass();
        }
        ResourceKey<LootTable> table = container.getLootTable();
        boolean won = flip(server);
        container.unpackLootTable(server);
        if (won) {
            refill(server, container, table, pos);
        } else {
            plunder(server, container);
        }
        return EventResult.pass();
    }

    private static void refill(ServerPlayer player, RandomizableContainerBlockEntity container,
                               ResourceKey<LootTable> key, BlockPos pos) {
        ServerLevel level = player.serverLevel();
        LootTable table = level.getServer().reloadableRegistries().getLootTable(key);
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .withParameter(LootContextParams.THIS_ENTITY, player)
                .withLuck(player.getLuck())
                .create(LootContextParamSets.CHEST);
        table.fill(container, params, level.getRandom().nextLong());
        container.setChanged();
    }

    private static void plunder(ServerPlayer player, RandomizableContainerBlockEntity container) {
        List<Integer> full = new ArrayList<>();
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (!container.getItem(slot).isEmpty()) {
                full.add(slot);
            }
        }
        Collections.shuffle(full, new Random(player.getRandom().nextLong()));
        int lost = (int) Math.ceil(full.size() * LOSE_SHARE);
        for (int i = 0; i < lost; i++) {
            container.setItem(full.get(i), ItemStack.EMPTY);
        }
        container.setChanged();
    }

    static EventResult wager(Player player, Entity target, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || !(player instanceof ServerPlayer server)
                || !(target instanceof ServerPlayer other) || !server.isShiftKeyDown() || !Fortunes.on()) {
            return EventResult.pass();
        }
        long now = server.level().getGameTime();
        Offer offered = OFFERS.get(server.getUUID());
        if (offered != null && offered.from().equals(other.getUUID()) && now <= offered.until()
                && !server.getMainHandItem().isEmpty()) {
            return accept(server, other, offered, now);
        }
        if (!calls(server) || DUELS.containsKey(server.getUUID()) || DUELS.containsKey(other.getUUID())) {
            return EventResult.pass();
        }
        PROMPTS.put(server.getUUID(), new Prompt(other.getUUID(), now + OFFER_TICKS));
        Fortunes.whisper(server, Component.translatable("message.jojoha.fortune.gambler.name_stake",
                other.getDisplayName()));
        return EventResult.interruptTrue();
    }

    static EventResult spoke(ServerPlayer player, Component message) {
        if (player != null) {
            String words = message.getString().strip();
            player.getServer().execute(() -> named(player, words));
        }
        return EventResult.pass();
    }

    private static void named(ServerPlayer gambler, String words) {
        Prompt prompt = PROMPTS.get(gambler.getUUID());
        if (prompt == null || words.isEmpty()) {
            return;
        }
        PROMPTS.remove(gambler.getUUID());
        long now = gambler.level().getGameTime();
        ServerPlayer other = gambler.getServer().getPlayerList().getPlayer(prompt.target());
        if (now > prompt.until() || other == null || DUELS.containsKey(gambler.getUUID())
                || DUELS.containsKey(other.getUUID())) {
            return;
        }
        String stake = words.length() > MOST_WORDS ? words.substring(0, MOST_WORDS) : words;
        OFFERS.put(other.getUUID(), new Offer(gambler.getUUID(), stake, now + OFFER_TICKS));
        Fortunes.whisper(other, Component.translatable("message.jojoha.fortune.gambler.offered",
                gambler.getDisplayName(), stake));
        Fortunes.whisper(gambler, Component.translatable("message.jojoha.fortune.gambler.offering",
                other.getDisplayName(), stake));
    }

    private static EventResult accept(ServerPlayer bettor, ServerPlayer gambler, Offer offer, long now) {
        OFFERS.remove(bettor.getUUID());
        if (DUELS.containsKey(bettor.getUUID()) || DUELS.containsKey(gambler.getUUID())) {
            return EventResult.pass();
        }
        ItemStack stake = bettor.getMainHandItem().copy();
        bettor.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        Duel duel = new Duel(gambler.getUUID(), bettor.getUUID(), stake, offer.words(), now + DUEL_TICKS);
        DUELS.put(gambler.getUUID(), duel);
        DUELS.put(bettor.getUUID(), duel);
        for (ServerPlayer side : List.of(gambler, bettor)) {
            side.playNotifySound(SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 0.6F, 0.8F);
            Fortunes.whisper(side, Component.translatable("message.jojoha.fortune.gambler.set",
                    stake.getDisplayName(), offer.words()));
        }
        return EventResult.interruptTrue();
    }

    static void fell(ServerPlayer player) {
        Duel duel = DUELS.get(player.getUUID());
        if (duel != null) {
            settle(player.getServer(), duel, duel.other(player.getUUID()));
        }
    }

    private static void settle(MinecraftServer server, Duel duel, UUID winner) {
        DUELS.remove(duel.gambler());
        DUELS.remove(duel.bettor());
        ServerPlayer gambler = server.getPlayerList().getPlayer(duel.gambler());
        ServerPlayer bettor = server.getPlayerList().getPlayer(duel.bettor());
        Component item = duel.stake().getDisplayName();
        if (duel.gambler().equals(winner)) {
            if (gambler != null) {
                gambler.getInventory().placeItemBackInInventory(duel.stake().copy());
                Fortunes.whisper(gambler, Component.translatable("message.jojoha.fortune.gambler.collected", item)
                        .withStyle(ChatFormatting.GOLD));
            }
            if (bettor != null) {
                Fortunes.whisper(bettor, Component.translatable("message.jojoha.fortune.gambler.forfeit", item));
            }
            return;
        }
        if (bettor != null) {
            bettor.getInventory().placeItemBackInInventory(duel.stake().copy());
        }
        if (winner == null) {
            for (ServerPlayer side : new ServerPlayer[]{gambler, bettor}) {
                if (side != null) {
                    Fortunes.whisper(side, Component.translatable("message.jojoha.fortune.gambler.drawn", item));
                }
            }
            return;
        }
        if (bettor != null) {
            Fortunes.whisper(bettor, Component.translatable("message.jojoha.fortune.gambler.paid", item, duel.words())
                    .withStyle(ChatFormatting.GOLD));
        }
        if (gambler != null) {
            JojohaPlayerData data = PlayerDataAccess.get(gambler);
            if (Fortune.has(data, Fortune.GAMBLER)) {
                badLuck(gambler, data);
            }
            Fortunes.whisper(gambler, Component.translatable("message.jojoha.fortune.gambler.owed", duel.words()));
        }
    }

    private static void refused(ServerPlayer player, JojohaPlayerData data) {
        badLuck(player, data);
        Fortunes.whisper(player, Component.translatable("message.jojoha.fortune.gambler.refused"));
    }

    private static void badLuck(ServerPlayer player, JojohaPlayerData data) {
        data.fortuneUntil = player.level().getGameTime() + BAD_LUCK_TICKS;
        PlayerDataAccess.set(player, data);
    }

    static void quit(ServerPlayer player) {
        PROMPTS.remove(player.getUUID());
        OFFERS.remove(player.getUUID());
        SECOND_ROLL.remove(player.getUUID());
        Duel duel = DUELS.get(player.getUUID());
        if (duel == null) {
            return;
        }
        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (Fortune.has(data, Fortune.GAMBLER)) {
            badLuck(player, data);
        }
        settle(player.getServer(), duel, duel.other(player.getUUID()));
    }

    static void stopping(MinecraftServer server) {
        for (Duel duel : new HashSet<>(DUELS.values())) {
            settle(server, duel, null);
        }
        PROMPTS.clear();
        OFFERS.clear();
        SECOND_ROLL.clear();
    }
}
