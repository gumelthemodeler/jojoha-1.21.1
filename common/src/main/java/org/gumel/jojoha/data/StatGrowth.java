package org.gumel.jojoha.data;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.gumel.jojoha.family.FamilyLine;
import org.gumel.jojoha.item.Provisions;
import org.gumel.jojoha.stand.BondRank;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class StatGrowth {
    private static final int BASE_COST = 20;
    private static final float SPREAD = 40F;

    private static final int DANGER_MEMORY_TICKS = 100;

    private static final float DANGER_FLOOR = 0.15F;
    private static final float DANGER_CEILING = 1.0F;

    private static final int DAILY_ALLOWANCE = 400;
    private static final float BEYOND_ALLOWANCE_RATE = 0.1F;

    private record Danger(float fraction, long atTick) {
    }

    private static final Map<UUID, Danger> DANGER = new HashMap<>();

    private static final Map<String, int[]> DAILY = new HashMap<>();
    private static long dailyDay = -1;

    private StatGrowth() {
    }

    public static int cost(int level) {
        return Math.round(BASE_COST * (1 + (level - StatEffects.BASE_STAT) / SPREAD));
    }

    public static float towardsNext(JojohaPlayerData data, boolean stand, int stat) {
        if (stand && stat == StatPoints.STAND_ENDURANCE) {
            stand = false;
            stat = StatPoints.ENDURANCE;
        }
        int level = stand ? standLevel(data, stat) : StatPoints.playerStat(data, stat);
        if (level >= StatPoints.MAX_STAT) {
            return 1F;
        }
        int[] progress = stand ? data.standStatProgress : data.playerStatProgress;
        return Math.min(1F, progress[stat] / (float) cost(level));
    }

    private static int standLevel(JojohaPlayerData data, int stat) {
        return data.stand.isPresent() ? data.stand.stats()[stat] : 0;
    }

    public static void award(ServerPlayer player, int stat, int amount) {
        award(player, stat, Provisions.trained(player, amount), false);
    }

    public static int equalize(ServerPlayer player, int levels) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        int high = 0;
        int low = 0;
        for (int stat = 1; stat < StatPoints.COUNT; stat++) {
            if (StatPoints.playerStat(data, stat) > StatPoints.playerStat(data, high)) {
                high = stat;
            }
            if (StatPoints.playerStat(data, stat) < StatPoints.playerStat(data, low)) {
                low = stat;
            }
        }
        int top = StatPoints.playerStat(data, high);
        int moved = Math.min(levels, (top - StatPoints.playerStat(data, low)) / 2);
        if (moved <= 0) {
            return 0;
        }
        setPlayerStat(data, high, top - moved);
        data.playerStatProgress[high] = 0;
        PlayerDataAccess.set(player, data);
        if (high == StatPoints.ENDURANCE && data.stand.isPresent()) {
            StandGrowth.considerTrust(player, true);
        }
        raise(player, false, low, moved, true);
        player.displayClientMessage(Component.translatable("message.jojoha.stat_crystal.moved", moved,
                Component.translatable("stat.jojoha." + StatPoints.PLAYER_NAMES[high]),
                Component.translatable("stat.jojoha." + StatPoints.PLAYER_NAMES[low])), false);
        PlayerDataAccess.sync(player);
        return moved;
    }

    private static void award(ServerPlayer player, int stat, int amount, boolean quiet) {
        if (amount <= 0 || stat < 0 || stat >= StatPoints.COUNT) {
            return;
        }
        JojohaPlayerData data = PlayerDataAccess.get(player);
        int level = StatPoints.playerStat(data, stat);
        if (level >= StatPoints.MAX_STAT) {
            return;
        }
        int before = level;

        data.playerStatProgress[stat] += amount;
        boolean rose = false;
        while (level < StatPoints.MAX_STAT && data.playerStatProgress[stat] >= cost(level)) {
            data.playerStatProgress[stat] -= cost(level);
            level++;
            rose = true;
        }

        if (rose) {
            setPlayerStat(data, stat, level);
            if (!quiet) {
                announce(player, false, stat, before, level);
            }
            StatMilestones.reached(player, data, stat, level);
        }

        PlayerDataAccess.set(player, data);
        if (rose) {
            PlayerDataAccess.sync(player);
        } else {
            PlayerDataAccess.syncSoon(player);
        }

        if (rose && stat == StatPoints.ENDURANCE && data.stand.isPresent()) {
            StandGrowth.considerTrust(player, quiet);
        }
    }

    public static void awardStand(ServerPlayer player, int stat, int amount) {
        awardStand(player, stat, amount, false);
    }

    private static void awardStand(ServerPlayer player, int stat, int amount, boolean quiet) {
        if (amount <= 0 || stat < 0 || stat >= StatPoints.COUNT) {
            return;
        }
        if (stat == StatPoints.STAND_ENDURANCE) {
            award(player, StatPoints.ENDURANCE, amount, quiet);
            return;
        }
        amount = FamilyLine.standGain(player, amount);
        if (amount <= 0) {
            return;
        }
        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (!data.stand.isPresent()) {
            return;
        }

        int level = data.stand.stats()[stat];
        if (level >= StatPoints.MAX_STAT) {
            return;
        }
        int before = level;
        BondRank rankBefore = BondRank.of(data.stand);

        data.standStatProgress[stat] += amount;
        boolean rose = false;
        while (level < StatPoints.MAX_STAT && data.standStatProgress[stat] >= cost(level)) {
            data.standStatProgress[stat] -= cost(level);
            data.stand = data.stand.plusStat(stat);
            level++;
            rose = true;
        }

        if (rose && !quiet) {
            announce(player, true, stat, before, level);
            BondRank rankAfter = BondRank.of(data.stand);
            if (rankAfter.ordinal() > rankBefore.ordinal()) {
                player.displayClientMessage(Component.translatable("message.jojoha.bond_rank.rose",
                        Component.translatable(rankAfter.translationKey())
                                .withStyle(ChatFormatting.LIGHT_PURPLE)), false);
            }
        }

        PlayerDataAccess.set(player, data);
        if (rose) {
            PlayerDataAccess.sync(player);
        } else {
            PlayerDataAccess.syncSoon(player);
        }

        if (rose) {
            StandGrowth.considerTrust(player, quiet);
        }
    }

    public static int raise(ServerPlayer player, boolean stand, int stat, int levels, boolean quiet) {
        if (levels <= 0 || stat < 0 || stat >= StatPoints.COUNT) {
            return 0;
        }
        if (stand && stat == StatPoints.STAND_ENDURANCE) {
            stand = false;
            stat = StatPoints.ENDURANCE;
        }
        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (stand && !data.stand.isPresent()) {
            return 0;
        }
        int level = stand ? standLevel(data, stat) : StatPoints.playerStat(data, stat);
        int[] progress = stand ? data.standStatProgress : data.playerStatProgress;
        int rising = Math.min(levels, StatPoints.MAX_STAT - level);
        if (rising <= 0) {
            return 0;
        }

        int needed = Math.max(0, cost(level) - progress[stat]);
        for (int step = 1; step < rising; step++) {
            needed += cost(level + step);
        }
        if (stand) {
            awardStand(player, stat, needed, quiet);
        } else {
            award(player, stat, needed, quiet);
        }
        return rising;
    }

    public static int feed(ServerPlayer player, boolean stand, int stat, float fraction, boolean quiet) {
        if (fraction <= 0F || stat < 0 || stat >= StatPoints.COUNT) {
            return 0;
        }
        if (stand && stat == StatPoints.STAND_ENDURANCE) {
            stand = false;
            stat = StatPoints.ENDURANCE;
        }
        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (stand && !data.stand.isPresent()) {
            return 0;
        }
        int level = stand ? standLevel(data, stat) : StatPoints.playerStat(data, stat);
        if (level >= StatPoints.MAX_STAT) {
            return 0;
        }
        int amount = Math.max(1, Math.round(cost(level) * fraction));
        if (stand) {
            awardStand(player, stat, amount, quiet);
        } else {
            award(player, stat, amount, quiet);
        }
        JojohaPlayerData after = PlayerDataAccess.get(player);
        return (stand ? standLevel(after, stat) : StatPoints.playerStat(after, stat)) - level;
    }

    private static void setPlayerStat(JojohaPlayerData data, int stat, int value) {
        switch (stat) {
            case StatPoints.STRENGTH -> data.strength = value;
            case StatPoints.VITALITY -> data.vitality = value;
            case StatPoints.AGILITY -> data.agility = value;
            case StatPoints.ENDURANCE -> data.endurance = value;
            case StatPoints.WORTHINESS -> data.worthiness = value;
            default -> {
            }
        }
    }

    private static void announce(ServerPlayer player, boolean stand, int stat, int before, int after) {
        String name = stand
                ? StatPoints.STAND_ABBREVIATIONS[stat]
                : StatPoints.PLAYER_ABBREVIATIONS[stat];

        player.displayClientMessage(Component.translatable("message.jojoha.stat.rose",
                Component.literal(name).withStyle(stand
                        ? ChatFormatting.LIGHT_PURPLE
                        : ChatFormatting.YELLOW),
                before, after), true);

        List<StatPerk> crossed = new ArrayList<>(StatPerk.crossed(stand, stat, before, after));
        if (!stand && stat == StatPoints.ENDURANCE) {
            crossed.addAll(StatPerk.crossed(true, StatPoints.STAND_ENDURANCE, before, after));
        }
        for (StatPerk perk : crossed) {
            player.displayClientMessage(Component.translatable("message.jojoha.perk.reached",
                    Component.literal(name).withStyle(stand
                            ? ChatFormatting.LIGHT_PURPLE
                            : ChatFormatting.YELLOW),
                    perk.threshold(),
                    Component.translatable(perk.nameKey()).withStyle(ChatFormatting.GOLD),
                    Component.translatable(perk.descriptionKey())), false);
        }
        if (!crossed.isEmpty()) {
            player.serverLevel().playSound(null, player.blockPosition(),
                    SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS,
                    0.6F, 1.3F);
        }
    }

    public static void rememberDanger(ServerPlayer player, float damage) {
        float max = Math.max(1F, player.getMaxHealth());
        float fraction = Math.min(1F, damage / max);
        Danger existing = DANGER.get(player.getUUID());
        long now = player.level().getGameTime();
        float carried = existing != null && now - existing.atTick() < DANGER_MEMORY_TICKS
                ? existing.fraction() : 0F;
        DANGER.put(player.getUUID(), new Danger(Math.min(1F, carried + fraction), now));
    }

    public static float dangerScale(ServerPlayer player) {
        Danger danger = DANGER.get(player.getUUID());
        if (danger == null || player.level().getGameTime() - danger.atTick() > DANGER_MEMORY_TICKS) {
            return DANGER_FLOOR;
        }
        return DANGER_FLOOR + (DANGER_CEILING - DANGER_FLOOR) * danger.fraction();
    }

    public static int dailyLimited(ServerPlayer player, String source, int amount) {
        long day = player.level().getGameTime() / 24000L;
        if (day != dailyDay) {
            dailyDay = day;
            DAILY.clear();
        }

        int[] used = DAILY.computeIfAbsent(player.getUUID() + "/" + source, key -> new int[1]);
        int free = Math.max(0, DAILY_ALLOWANCE - used[0]);
        used[0] += amount;

        int atFullRate = Math.min(amount, free);
        int beyond = amount - atFullRate;
        return atFullRate + Math.round(beyond * BEYOND_ALLOWANCE_RATE);
    }

    public static void forget(UUID player) {
        DANGER.remove(player);
        DAILY.keySet().removeIf(key -> key.startsWith(player + "/"));
    }
}
