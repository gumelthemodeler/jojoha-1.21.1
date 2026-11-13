package org.gumel.jojoha.data;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.gumel.jojoha.stand.BondRank;
import org.gumel.jojoha.stand.TrustTier;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class StandGrowth {
    private static final float POWER_PER_DAMAGE = 0.6F;

    private static final int SPEED_PER_USE = 4;
    private static final float POTENTIAL_PER_ENERGY = 0.5F;

    private static final int ENDURANCE_PER_SECOND = 1;

    private static final int PROTECTION_PER_BLOCK = 6;

    static final int[] TRUST_AVERAGE = {0, 10, 17, 25};

    public static int trustAverage(int tier) {
        return TRUST_AVERAGE[Math.max(0, Math.min(tier, TRUST_AVERAGE.length - 1))];
    }

    private StandGrowth() {
    }

    public static void dealtDamage(ServerPlayer player, float amount) {
        if (amount > 0F) {
            StatGrowth.awardStand(player, StatPoints.POWER,
                    Math.max(1, Math.round(amount * POWER_PER_DAMAGE * StatGrowth.dangerScale(player))));
        }
    }

    public static void usedSkill(ServerPlayer player, ResourceLocation skill, float energySpent,
                                 int cooldownTicks) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (data.deeds.add("skill/" + skill)) {
            PlayerDataAccess.set(player, data);
        }

        if (cooldownTicks <= 0 && energySpent <= 0F) {
            return;
        }

        long now = player.level().getGameTime();
        Map<ResourceLocation, Long> practised =
                PRACTISED.computeIfAbsent(player.getUUID(), id -> new HashMap<>());
        Long lastOfThis = practised.get(skill);
        Long lastOfAny = LAST_PRACTICE.get(player.getUUID());
        if ((lastOfThis != null && now - lastOfThis < SAME_MOVE_SPACING_TICKS)
                || (lastOfAny != null && now - lastOfAny < ANY_MOVE_SPACING_TICKS)) {
            return;
        }
        practised.put(skill, now);
        LAST_PRACTICE.put(player.getUUID(), now);

        StatGrowth.awardStand(player, StatPoints.SPEED,
                StatGrowth.dailyLimited(player, "moves", SPEED_PER_USE));
        StatGrowth.awardStand(player, StatPoints.POTENTIAL,
                StatGrowth.dailyLimited(player, "move_energy",
                        Math.max(1, Math.round(energySpent * POTENTIAL_PER_ENERGY))));
    }

    private static final int SAME_MOVE_SPACING_TICKS = 100;

    private static final int ANY_MOVE_SPACING_TICKS = 40;

    private static final Map<UUID, Map<ResourceLocation, Long>> PRACTISED =
            new HashMap<>();
    private static final Map<UUID, Long> LAST_PRACTICE = new HashMap<>();

    public static void heldForm(ServerPlayer player) {
        int[] seconds = HELD_SECONDS.computeIfAbsent(player.getUUID(), id -> new int[1]);
        if (++seconds[0] < HELD_SECONDS_PER_POINT) {
            return;
        }
        seconds[0] = 0;
        StatGrowth.awardStand(player, StatPoints.STAND_ENDURANCE,
                StatGrowth.dailyLimited(player, "manifest", ENDURANCE_PER_SECOND));
    }

    private static final int HELD_SECONDS_PER_POINT = 4;
    private static final Map<UUID, int[]> HELD_SECONDS = new HashMap<>();

    public static void forget(UUID player) {
        HELD_SECONDS.remove(player);
        PRACTISED.remove(player);
        LAST_PRACTICE.remove(player);
    }

    public static void blockedHit(ServerPlayer player) {
        StatGrowth.awardStand(player, StatPoints.PROTECTION, PROTECTION_PER_BLOCK);
    }

    public static void considerTrust(ServerPlayer player) {
        considerTrust(player, false);
    }

    static void considerTrust(ServerPlayer player, boolean quiet) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (!data.stand.isPresent()) {
            return;
        }

        TrustTier current = data.stand.trust();
        int average = BondRank.averageStat(data.stand);

        int earned = current.level();
        for (int tier = current.level() + 1; tier < TRUST_AVERAGE.length; tier++) {
            if (average >= TRUST_AVERAGE[tier]) {
                earned = tier;
            } else {
                break;
            }
        }

        if (earned <= current.level()) {
            return;
        }

        TrustTier risen = TrustTier.fromLevel(earned);
        data.stand = data.stand.withTrust(risen);
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);

        if (quiet) {
            return;
        }
        player.displayClientMessage(Component.translatable("message.jojoha.trust.rose",
                Component.translatable("trust_tier.jojoha." + risen.key())), false);
    }

    public static int nextTrustAverage(StandData stand) {
        if (!stand.isPresent()) {
            return -1;
        }
        int tier = stand.trust().level();
        if (tier + 1 < TRUST_AVERAGE.length) {
            return TRUST_AVERAGE[tier + 1];
        }
        BondRank next = BondRank.of(stand).next();
        return next == null ? -1 : next.average();
    }
}
