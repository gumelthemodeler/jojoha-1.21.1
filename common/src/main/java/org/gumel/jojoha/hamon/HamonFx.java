package org.gumel.jojoha.hamon;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.data.Fate;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.registry.ModRegistries;
import org.gumel.jojoha.registry.ModSounds;
import org.gumel.jojoha.stand.StandAuraEffect;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public final class HamonFx {
    public static void sound(ServerLevel level, Entity from, float volume, float pitch) {
        float wobble = (level.getRandom().nextFloat() - 0.5F) * 0.08F;
        level.playSound(null, from, variant(level, from).get(),
                SoundSource.PLAYERS, volume, pitch + wobble);
    }

    private static final List<Supplier<SoundEvent>> VARIANTS =
            List.of(ModSounds.HAMON_DISCHARGE,
                    ModSounds.HAMON_STRIKE);

    public static final int CHARGE_LOOP_TICKS = 40;

    private static final Map<UUID, Long> LAST_CHARGE = new HashMap<>();

    public static void chargeSound(ServerLevel level, Entity from,
                                   float volume, float pitch) {
        float wobble = (level.getRandom().nextFloat() - 0.5F) * 0.06F;
        level.playSound(null, from, ModSounds.HAMON.get(),
                SoundSource.PLAYERS, volume, pitch + wobble);
        LAST_CHARGE.put(from.getUUID(), level.getGameTime());
    }

    public static void chargeLoop(ServerLevel level, Entity from,
                                  float volume, float pitch) {
        Long last = LAST_CHARGE.get(from.getUUID());
        long now = level.getGameTime();
        if (last == null || now - last >= CHARGE_LOOP_TICKS || now < last) {
            chargeSound(level, from, volume, pitch);
        }
    }

    private static final Set<UUID> BREATHING = new HashSet<>();
    private static final Map<UUID, Long> LAST_PASSIVE = new HashMap<>();

    public static final int PASSIVE_BREATH_QUIET_TICKS = 600;

    public static void passiveBreath(ServerLevel level, Entity from,
                                     float volume, float pitch) {
        if (!BREATHING.add(from.getUUID())) {
            return;
        }
        Long last = LAST_PASSIVE.get(from.getUUID());
        long now = level.getGameTime();
        if (last != null && now >= last && now - last < PASSIVE_BREATH_QUIET_TICKS) {
            return;
        }
        LAST_PASSIVE.put(from.getUUID(), now);
        chargeSound(level, from, volume, pitch);
    }

    public static void passiveBreathEnded(Entity from) {
        BREATHING.remove(from.getUUID());
    }

    private static final Map<UUID, Integer> LAST_VARIANT = new HashMap<>();

    private static Supplier<SoundEvent> variant(
            ServerLevel level, Entity from) {
        Integer last = LAST_VARIANT.get(from.getUUID());
        int pick = level.getRandom().nextInt(last == null ? VARIANTS.size() : VARIANTS.size() - 1);
        if (last != null && pick >= last) {
            pick++;
        }
        LAST_VARIANT.put(from.getUUID(), pick);
        return VARIANTS.get(pick);
    }

    public static final float RED = 1.0F;
    public static final float GREEN = 0.87F;
    public static final float BLUE = 0.20F;

    private static final double DEFAULT_SPREAD = 0.25;

    private static final int AURA_MOTES_PER_TICK = 3;

    private HamonFx() {
    }

    private static final double RAINBOW_REACH = 16.0;

    public static boolean rainbow(Player player) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        return Fate.ultimate(data) && Fate.breathesHamon(data);
    }

    private static boolean rainbowNear(ServerLevel level, double x, double y, double z) {
        ServerPlayer nearest = null;
        double best = RAINBOW_REACH * RAINBOW_REACH;
        for (ServerPlayer player : level.players()) {
            double distance = player.distanceToSqr(x, y, z);
            if (distance < best && Fate.breathesHamon(PlayerDataAccess.get(player))) {
                best = distance;
                nearest = player;
            }
        }
        return nearest != null && rainbow(nearest);
    }

    public static void ripple(ServerLevel level, double x, double y, double z, int count) {
        level.sendParticles(rainbowNear(level, x, y, z) ? ModRegistries.HAMON_RIPPLE_RAINBOW.get() : ModRegistries.HAMON_RIPPLE.get(), x, y, z,
                count, DEFAULT_SPREAD, DEFAULT_SPREAD, DEFAULT_SPREAD, 0.0);
    }

    public static void spark(ServerLevel level, double x, double y, double z, int count,
                             double spread, double speed) {
        level.sendParticles(rainbowNear(level, x, y, z) ? ModRegistries.HAMON_SPARK_RAINBOW.get() : ModRegistries.HAMON_SPARK.get(), x, y, z,
                count, spread, spread, spread, speed);
    }

    public static void mote(ServerLevel level, double x, double y, double z, int count,
                            double spread) {
        level.sendParticles(rainbowNear(level, x, y, z) ? ModRegistries.HAMON_MOTE_RAINBOW.get() : ModRegistries.HAMON_MOTE.get(), x, y, z,
                count, spread, spread, spread, 0.01);
    }

    public static void fineSpark(ServerLevel level, double x, double y, double z, int count,
                                 double spread, double speed) {
        level.sendParticles(rainbowNear(level, x, y, z) ? ModRegistries.HAMON_SPARK_FINE_RAINBOW.get() : ModRegistries.HAMON_SPARK_FINE.get(), x, y, z,
                count, spread, spread, spread, speed);
    }

    public static void fineMote(ServerLevel level, double x, double y, double z, int count,
                                double spread) {
        level.sendParticles(rainbowNear(level, x, y, z) ? ModRegistries.HAMON_MOTE_FINE_RAINBOW.get() : ModRegistries.HAMON_MOTE_FINE.get(), x, y, z,
                count, spread, spread, spread, 0.01);
    }

    public static void ring(ServerLevel level, double x, double y, double z,
                            double scale, int band) {
        level.sendParticles(rainbowNear(level, x, y, z) ? ModRegistries.HAMON_RING_RAINBOW.get() : ModRegistries.HAMON_RING.get(), x, y, z, 0, scale, band, 0.0, 1.0);
    }

    public static Vec3 mainHand(ServerPlayer player) {
        return player.getRopeHoldPosition(1.0F).add(0.0, HELD_MIDDLE, 0.0);
    }

    private static final double HELD_MIDDLE = 0.2;

    public static void aura(ServerPlayer player) {
        aura(player.serverLevel(), player, rainbow(player));
    }

    public static void aura(ServerLevel level, Entity player, boolean rainbow) {
        RandomSource random = level.getRandom();

        for (int i = 0; i < AURA_MOTES_PER_TICK; i++) {
            Vec3 offset = StandAuraEffect.auraOffset(player.tickCount * AURA_MOTES_PER_TICK + i,
                    random);
            level.sendParticles(rainbow ? ModRegistries.HAMON_AURA_RAINBOW.get() : ModRegistries.HAMON_AURA.get(),
                    player.getX() + offset.x, player.getY() + offset.y, player.getZ() + offset.z,
                    0, 0.0, 0.0, 0.0, 0.0);
        }
    }
}
