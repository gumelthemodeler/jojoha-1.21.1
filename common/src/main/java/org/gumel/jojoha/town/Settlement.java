package org.gumel.jojoha.town;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Settlement {
    public static final int NAMES = 24;
    public static final int REP_LOWEST = -50;
    public static final int REP_HIGHEST = 50;
    public static final int REP_DISLIKED = -10;
    public static final int REP_LIKED = 10;
    public static final int REP_LOVED = 30;

    final int id;
    BlockPos centre;
    final int name;
    int peak;
    int population;
    boolean ravaged;
    long lastArrivalDay;
    int turnedPending;
    long raidedDay = -1L;
    final Map<UUID, Integer> reputation = new HashMap<>();

    Settlement(int id, BlockPos centre, int name) {
        this.id = id;
        this.centre = centre;
        this.name = name;
    }

    public int id() {
        return id;
    }

    public BlockPos centre() {
        return centre;
    }

    public Component name() {
        return Component.translatable("town.jojoha.name." + name);
    }

    public int peak() {
        return peak;
    }

    public int population() {
        return population;
    }

    public boolean ravaged() {
        return ravaged;
    }

    public boolean holds(Vec3 at, double radius) {
        return at.distanceToSqr(Vec3.atCenterOf(centre)) <= radius * radius;
    }

    public int reputation(Player player) {
        return reputation.getOrDefault(player.getUUID(), 0);
    }

    int addReputation(Player player, int amount) {
        int after = Mth.clamp(reputation(player) + amount, REP_LOWEST, REP_HIGHEST);
        reputation.put(player.getUUID(), after);
        return after;
    }

    CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Id", id);
        tag.putLong("Centre", centre.asLong());
        tag.putInt("Name", name);
        tag.putInt("Peak", peak);
        tag.putInt("Population", population);
        tag.putBoolean("Ravaged", ravaged);
        tag.putLong("Arrival", lastArrivalDay);
        tag.putInt("Turned", turnedPending);
        tag.putLong("Raided", raidedDay);
        CompoundTag rep = new CompoundTag();
        reputation.forEach((player, value) -> rep.putInt(player.toString(), value));
        tag.put("Reputation", rep);
        return tag;
    }

    static Settlement load(CompoundTag tag) {
        Settlement settlement = new Settlement(tag.getInt("Id"), BlockPos.of(tag.getLong("Centre")), tag.getInt("Name"));
        settlement.peak = tag.getInt("Peak");
        settlement.population = tag.getInt("Population");
        settlement.ravaged = tag.getBoolean("Ravaged");
        settlement.lastArrivalDay = tag.getLong("Arrival");
        settlement.turnedPending = tag.getInt("Turned");
        settlement.raidedDay = tag.contains("Raided") ? tag.getLong("Raided") : -1L;
        CompoundTag rep = tag.getCompound("Reputation");
        for (String player : rep.getAllKeys()) {
            settlement.reputation.put(UUID.fromString(player), rep.getInt(player));
        }
        return settlement;
    }
}
