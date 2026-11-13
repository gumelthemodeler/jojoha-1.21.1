package org.gumel.jojoha.fortune;

import dev.architectury.event.EventResult;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.data.Fate;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.registry.ModItems;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class Devout {
    public enum Vow {
        MERCY,
        HONEST_COMBAT,
        ABSTINENCE,
        SABBATH,
        PURE_BLOOD;

        public int bit() {
            return 1 << ordinal();
        }

        public String key() {
            return "fortune.jojoha.devout.vow." + name().toLowerCase(Locale.ROOT);
        }
    }

    public static final float PRICE_SCALE = 0.75F;

    public static final float GUILT_STEP = 0.10F;

    public static final int GUILT_MOST = 5;

    public static final int GUILT_FADE_TICKS = 72000;

    public static final float DREAM_ODDS = 1.5F;

    public static final float CALM_BELOW = 0.30F;

    public static final int CALM_STILL_TICKS = 20;

    public static final int CALM_PRIME_TICKS = 6;

    public static final int CALM_AGAIN_TICKS = 6000;

    public static final int FAITH_MOST = 5;

    public static final float FAITH_PRICE_STEP = 0.05F;

    public static final int FAITH_CALM_STEP = 1200;

    public static final int WEEK = 7;

    public static final int SABBATH_DAY = 6;

    public static final int[] FAST_DAYS = {2, 4};

    public static final int SELF_DEFENCE_TICKS = 600;

    private static final double BEHIND = -0.3;

    private static final int[] PRIMES = {2, 3, 5, 7, 11, 13};

    private static final int GUILT_EVERY = 200;

    private static final int DAY_TICKS = 24000;

    private static final double STILL_DRIFT_SQR = 0.0025;

    private static final Map<UUID, Integer> COUNTING = new HashMap<>();

    private static final Map<UUID, Vec3> LAST_SPOT = new HashMap<>();

    private static final Map<UUID, Long> CALMED = new HashMap<>();

    private Devout() {
    }

    public static int faith(JojohaPlayerData data) {
        return (int) (data.fortuneUntil & 0xFF);
    }

    private static int broken(JojohaPlayerData data) {
        return (int) (data.fortuneUntil >>> 8 & 0xFF);
    }

    private static long dayKept(JojohaPlayerData data) {
        return data.fortuneUntil >>> 16;
    }

    private static void store(JojohaPlayerData data, long day, int broken, int faith) {
        data.fortuneUntil = day << 16 | (long) (broken & 0xFF) << 8 | faith & 0xFF;
    }

    public static long today(Level level) {
        return level.getServer() == null ? level.getDayTime() / DAY_TICKS
                : level.getServer().overworld().getDayTime() / DAY_TICKS;
    }

    public static boolean sabbath(long day) {
        return day % WEEK == SABBATH_DAY;
    }

    public static boolean fastDay(long day) {
        for (int fast : FAST_DAYS) {
            if (day % WEEK == fast) {
                return true;
            }
        }
        return false;
    }

    static void tick(ServerPlayer player, JojohaPlayerData data, boolean devout) {
        if (!devout) {
            COUNTING.remove(player.getUUID());
            LAST_SPOT.remove(player.getUUID());
            return;
        }
        calmMind(player, data);
        if (player.tickCount % GUILT_EVERY == 0) {
            fade(player, data);
            dawn(player, data);
            if (impure(player, data)) {
                breakVow(player, data, Vow.PURE_BLOOD);
            }
        }
    }

    private static void dawn(ServerPlayer player, JojohaPlayerData data) {
        long today = today(player.level());
        long kept = dayKept(data);
        if (kept == today + 1) {
            return;
        }
        int faith = faith(data);
        if (kept != 0 && broken(data) == 0 && !impure(player, data)) {
            faith = Math.min(FAITH_MOST, faith + 1);
            Fortunes.whisper(player, Component.translatable("message.jojoha.fortune.devout.kept", faith));
        }
        store(data, today + 1, 0, faith);
        PlayerDataAccess.set(player, data);
        if (sabbath(today)) {
            Fortunes.whisper(player, Component.translatable("message.jojoha.fortune.devout.sabbath"));
        } else if (fastDay(today)) {
            Fortunes.whisper(player, Component.translatable("message.jojoha.fortune.devout.fast"));
        }
    }

    private static boolean impure(ServerPlayer player, JojohaPlayerData data) {
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        return Fate.undead(data) || head.is(ModItems.STONE_MASK.get()) || head.is(ModItems.RED_STONE_MASK.get());
    }

    public static void breakVow(ServerPlayer player, JojohaPlayerData data, Vow vow) {
        dawn(player, data);
        int broken = broken(data);
        if ((broken & vow.bit()) != 0) {
            return;
        }
        store(data, dayKept(data), broken | vow.bit(), 0);
        data.fortuneTally = Math.min(GUILT_MOST, data.fortuneTally + 1);
        data.lastConflict = player.level().getGameTime();
        PlayerDataAccess.set(player, data);
        player.playNotifySound(SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.5F, 0.6F);
        Fortunes.whisper(player, Component.translatable("message.jojoha.fortune.devout.broke",
                Component.translatable(vow.key()), data.fortuneTally));
    }

    public static float priceScale(JojohaPlayerData data) {
        if (!Fortunes.on() || !Fortune.has(data, Fortune.DEVOUT)) {
            return 1F;
        }
        return PRICE_SCALE * (1F - FAITH_PRICE_STEP * faith(data))
                * (1F + GUILT_STEP * Math.min(GUILT_MOST, data.fortuneTally));
    }

    public static float dreamOdds(JojohaPlayerData data) {
        return Fortunes.on() && Fortune.has(data, Fortune.DEVOUT) ? DREAM_ODDS : 1F;
    }

    static void struck(ServerPlayer attacker, JojohaPlayerData data, LivingEntity victim) {
        if (sabbath(today(attacker.level()))) {
            breakVow(attacker, data, Vow.SABBATH);
        }
        if (attacker.isInvisible() || attacker.isCrouching() || ambushed(attacker, victim)) {
            breakVow(attacker, data, Vow.HONEST_COMBAT);
        }
    }

    private static boolean ambushed(ServerPlayer attacker, LivingEntity victim) {
        if (threatened(attacker, victim)) {
            return false;
        }
        Vec3 toAttacker = attacker.position().subtract(victim.position());
        Vec3 flat = new Vec3(toAttacker.x, 0, toAttacker.z);
        if (flat.lengthSqr() < 1.0E-6) {
            return false;
        }
        Vec3 facing = victim.getViewVector(1F);
        return new Vec3(facing.x, 0, facing.z).normalize().dot(flat.normalize()) < BEHIND;
    }

    private static boolean threatened(ServerPlayer player, LivingEntity other) {
        if (other instanceof Mob mob && mob.getTarget() == player) {
            return true;
        }
        if (other instanceof NeutralMob angry && angry.isAngryAt(player)) {
            return true;
        }
        return player.getLastHurtByMob() == other
                && player.tickCount - player.getLastHurtByMobTimestamp() < SELF_DEFENCE_TICKS;
    }

    static void killed(ServerPlayer killer, JojohaPlayerData data, LivingEntity victim) {
        if (victim instanceof Enemy || threatened(killer, victim)) {
            return;
        }
        breakVow(killer, data, Vow.MERCY);
    }

    public static void ate(Player player, ItemStack food) {
        if (!(player instanceof ServerPlayer server) || !Fortunes.on()
                || !(food.is(ItemTags.MEAT) || food.is(ItemTags.FISHES))) {
            return;
        }
        JojohaPlayerData data = PlayerDataAccess.get(server);
        if (Fortune.has(data, Fortune.DEVOUT) && fastDay(today(server.level()))) {
            breakVow(server, data, Vow.ABSTINENCE);
        }
    }

    public static EventResult miracle(ServerPlayer player) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (!Fortunes.on() || !Fortune.has(data, Fortune.DEVOUT) || faith(data) < FAITH_MOST) {
            return EventResult.pass();
        }
        store(data, dayKept(data), broken(data), 0);
        PlayerDataAccess.set(player, data);
        player.setHealth(1F);
        player.invulnerableTime = 40;
        player.playNotifySound(SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.6F, 1.4F);
        Fortunes.whisper(player, Component.translatable("message.jojoha.fortune.devout.miracle"));
        return EventResult.interruptFalse();
    }

    private static void calmMind(ServerPlayer player, JojohaPlayerData data) {
        UUID id = player.getUUID();
        Vec3 last = LAST_SPOT.put(id, player.position());
        long now = player.level().getGameTime();
        Long calmed = CALMED.get(id);
        int again = CALM_AGAIN_TICKS - FAITH_CALM_STEP * faith(data);
        boolean still = last != null && last.distanceToSqr(player.position()) < STILL_DRIFT_SQR
                && player.isShiftKeyDown() && player.getHealth() < player.getMaxHealth() * CALM_BELOW
                && troubled(player) && (calmed == null || now - calmed >= again);
        int held = still ? COUNTING.getOrDefault(id, 0) + 1 : 0;
        COUNTING.put(id, held);
        if (held < CALM_STILL_TICKS) {
            return;
        }
        int counted = held - CALM_STILL_TICKS;
        if (counted % CALM_PRIME_TICKS != 0) {
            return;
        }
        int reached = counted / CALM_PRIME_TICKS;
        if (reached < PRIMES.length) {
            StringBuilder said = new StringBuilder();
            for (int i = 0; i <= reached; i++) {
                said.append(i == 0 ? "" : ", ").append(PRIMES[i]);
            }
            player.displayClientMessage(Component.translatable("message.jojoha.fortune.devout.count",
                    said.toString()), true);
            return;
        }
        player.removeEffect(MobEffects.WEAKNESS);
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        player.removeEffect(MobEffects.CONFUSION);
        CALMED.put(id, now);
        COUNTING.put(id, 0);
        player.playNotifySound(SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 1.2F);
        Fortunes.whisper(player, Component.translatable("message.jojoha.fortune.devout.calm"));
    }

    private static boolean troubled(ServerPlayer player) {
        return player.hasEffect(MobEffects.WEAKNESS) || player.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)
                || player.hasEffect(MobEffects.CONFUSION);
    }

    private static void fade(ServerPlayer player, JojohaPlayerData data) {
        long now = player.level().getGameTime();
        if (data.fortuneTally <= 0 || data.lastConflict > now) {
            return;
        }
        if (now - data.lastConflict < GUILT_FADE_TICKS) {
            return;
        }
        data.fortuneTally--;
        data.lastConflict = now;
        PlayerDataAccess.set(player, data);
        Fortunes.whisper(player, Component.translatable("message.jojoha.fortune.devout.forgiven", data.fortuneTally));
    }

    static void forget(Player player) {
        COUNTING.remove(player.getUUID());
        LAST_SPOT.remove(player.getUUID());
        CALMED.remove(player.getUUID());
    }
}
