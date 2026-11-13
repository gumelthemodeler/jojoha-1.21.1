package org.gumel.jojoha.ultimate;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import org.gumel.jojoha.combat.fx.Palette;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.data.VampireStage;
import org.gumel.jojoha.network.NetworkHandler;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public final class AdaptationLibrary {
    public static final int MASTERY_MAX = 3;
    public static final int MAX_SLOTS = 3;

    private static final int[] RANK_AT = {0, 3, 7, 12, 18, 26, 36};
    public static final int TOP_RANK = RANK_AT.length - 1;

    private static final int[] ACTIVE_SLOT_RANKS = {0, 2, 5};
    private static final int[] PASSIVE_SLOT_RANKS = {0, 1, 4};

    private static final float COOLDOWN_CUT_PER_MASTERY = 0.15F;

    private static final Map<UUID, Map<Adaptation, UUID>> LAST_STUDIED = new HashMap<>();

    private AdaptationLibrary() {
    }

    public enum Learned { NEW, DEEPER, SAME_CREATURE, MASTERED }

    public static Consumer<ResourceLocation> migrate(JojohaPlayerData data) {
        return id -> {
            if (!data.adaptMastery.isEmpty()) {
                return;
            }
            Adaptation adaptation = BuiltInRegistries.ENTITY_TYPE.getOptional(id).map(Adaptation::of).orElse(null);
            if (adaptation != null) {
                data.adaptMastery.put(adaptation.key(), 1);
                data.adaptActives.add(adaptation.key());
                data.adaptPassives.add(adaptation.key());
            }
        };
    }

    public static int mastery(JojohaPlayerData data, Adaptation adaptation) {
        return Math.min(MASTERY_MAX, Math.max(0, data.adaptMastery.getOrDefault(adaptation.key(), 0)));
    }

    public static EntityType<?> source(JojohaPlayerData data, Adaptation adaptation) {
        ResourceLocation id = ResourceLocation.tryParse(data.adaptSources.getOrDefault(adaptation.key(), ""));
        EntityType<?> studied = id == null ? null : BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
        return studied != null && Adaptation.of(studied) == adaptation ? studied : adaptation.face();
    }

    public static boolean knows(JojohaPlayerData data, Adaptation adaptation) {
        return mastery(data, adaptation) > 0;
    }

    public static int score(JojohaPlayerData data) {
        int score = 0;
        for (Adaptation adaptation : Adaptation.values()) {
            score += mastery(data, adaptation);
        }
        return score;
    }

    public static int rank(JojohaPlayerData data) {
        int score = score(data);
        int rank = 0;
        for (int i = 0; i < RANK_AT.length; i++) {
            if (score >= RANK_AT[i]) {
                rank = i;
            }
        }
        return rank;
    }

    public static int rankAt(int rank) {
        return RANK_AT[Math.max(0, Math.min(TOP_RANK, rank))];
    }

    public static int activeSlots(JojohaPlayerData data) {
        return slots(ACTIVE_SLOT_RANKS, rank(data));
    }

    public static int passiveSlots(JojohaPlayerData data) {
        return slots(PASSIVE_SLOT_RANKS, rank(data));
    }

    public static int activeSlotRank(int slot) {
        return ACTIVE_SLOT_RANKS[slot];
    }

    public static int passiveSlotRank(int slot) {
        return PASSIVE_SLOT_RANKS[slot];
    }

    private static int slots(int[] ranks, int rank) {
        int open = 0;
        for (int needed : ranks) {
            if (rank >= needed) {
                open++;
            }
        }
        return open;
    }

    public static Adaptation active(JojohaPlayerData data, int slot) {
        if (slot < 0 || slot >= data.adaptActives.size() || slot >= activeSlots(data)) {
            return null;
        }
        Adaptation adaptation = Adaptation.named(data.adaptActives.get(slot));
        return adaptation != null && knows(data, adaptation) ? adaptation : null;
    }

    public static Map<Adaptation, Integer> passives(JojohaPlayerData data) {
        Map<Adaptation, Integer> out = new EnumMap<>(Adaptation.class);
        int open = passiveSlots(data);
        for (int i = 0; i < data.adaptPassives.size() && i < open; i++) {
            Adaptation adaptation = Adaptation.named(data.adaptPassives.get(i));
            if (adaptation != null && knows(data, adaptation)) {
                out.put(adaptation, mastery(data, adaptation));
            }
        }
        return out;
    }

    public static boolean equippedActive(JojohaPlayerData data, Adaptation adaptation) {
        int at = data.adaptActives.indexOf(adaptation.key());
        return at >= 0 && at < activeSlots(data);
    }

    public static boolean equippedPassive(JojohaPlayerData data, Adaptation adaptation) {
        int at = data.adaptPassives.indexOf(adaptation.key());
        return at >= 0 && at < passiveSlots(data);
    }

    public static int cooldownTicks(JojohaPlayerData data, Adaptation adaptation) {
        int steps = Math.max(0, mastery(data, adaptation) - 1);
        return Math.round(adaptation.cooldownTicks() * (1F - COOLDOWN_CUT_PER_MASTERY * steps));
    }

    public static boolean ultimate(JojohaPlayerData data) {
        return data.vampireStage == VampireStage.ULTIMATE_LIFEFORM;
    }

    public static Learned learn(ServerPlayer player, JojohaPlayerData data, LivingEntity target,
                                Adaptation adaptation) {
        int had = mastery(data, adaptation);
        Map<Adaptation, UUID> studied = LAST_STUDIED.computeIfAbsent(player.getUUID(), id -> new EnumMap<>(Adaptation.class));
        if (had >= MASTERY_MAX) {
            return Learned.MASTERED;
        }
        if (had > 0 && target.getUUID().equals(studied.get(adaptation))) {
            return Learned.SAME_CREATURE;
        }
        studied.put(adaptation, target.getUUID());
        int rankBefore = rank(data);
        data.adaptMastery.put(adaptation.key(), had + 1);
        if (had == 0) {
            data.adaptSources.put(adaptation.key(), BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString());
            fill(data.adaptActives, activeSlots(data), adaptation.key());
            fill(data.adaptPassives, passiveSlots(data), adaptation.key());
        }
        int rankAfter = rank(data);
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
        if (rankAfter > rankBefore) {
            evolved(player, rankAfter);
        }
        return had == 0 ? Learned.NEW : Learned.DEEPER;
    }

    private static void fill(List<String> equipped, int open, String key) {
        if (equipped.contains(key)) {
            return;
        }
        if (equipped.size() < open) {
            equipped.add(key);
        }
    }

    private static final int EVOLVED_PALETTE_TICKS = 40;

    private static void evolved(ServerPlayer player, int rank) {
        NetworkHandler.sendPalette(player, Palette.AWAKEN, EVOLVED_PALETTE_TICKS);
        ServerLevel level = player.serverLevel();
        player.displayClientMessage(Component.translatable("message.jojoha.adapt.evolved", rank), false);
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 0.7F);
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.8F, 1.4F);
        level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0, player.getZ(),
                24, 0.5, 0.8, 0.5, 0.08);
    }

    public static void equip(ServerPlayer player, String key, boolean active, int slot) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (!ultimate(data)) {
            return;
        }
        List<String> equipped = active ? data.adaptActives : data.adaptPassives;
        int open = active ? activeSlots(data) : passiveSlots(data);
        Adaptation adaptation = Adaptation.named(key);
        if (slot < 0) {
            if (adaptation != null) {
                equipped.remove(adaptation.key());
            }
        } else {
            if (slot >= open || adaptation == null || !knows(data, adaptation)) {
                return;
            }
            List<String> next = new ArrayList<>(equipped.subList(0, Math.min(equipped.size(), open)));
            int was = next.indexOf(adaptation.key());
            while (next.size() <= slot) {
                next.add("");
            }
            String displaced = next.get(slot);
            next.set(slot, adaptation.key());
            if (was >= 0 && was != slot) {
                next.set(was, displaced);
            }
            next.removeIf(String::isEmpty);
            equipped.clear();
            equipped.addAll(next);
        }
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_LEATHER.value(),
                SoundSource.PLAYERS, 0.6F, 1.3F);
    }

    public static void forget(UUID playerId) {
        LAST_STUDIED.remove(playerId);
    }
}
