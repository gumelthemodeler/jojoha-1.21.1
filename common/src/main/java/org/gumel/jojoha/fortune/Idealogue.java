package org.gumel.jojoha.fortune;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.stand.mob.MobStands;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class Idealogue {
    public static final int DUEL_WINDOW_TICKS = 200;

    private static final double CROWD_RANGE = 16.0;

    public static final float OUTMATCHED = 1.15F;

    public static final float RIVAL = 1.1F;

    public static final float CORNERED = 0.85F;

    public static final float CORNERED_AT = 0.4F;

    public static final float VICTORY_HEAL = 0.6F;

    private static final int VICTORY_REGEN_TICKS = 60;

    private static final int DAY_TICKS = 24000;

    public static final int GRACE_DAYS = 2;

    public static final int WORST_DAYS = 3;

    private static final double LUCK_PER_DAY = -1.0;

    private static final double ATTACK_SPEED_PER_DAY = -0.05;

    public static final int LEVELS_PER_TIER = 10;

    public static final int MAX_TIER = 3;

    private static final double SPEED_PER_TIER = 0.03;

    private static final double MINING_PER_TIER = 0.05;

    private static final double STAND_USER_RANGE = 64.0;

    private static final ResourceLocation WITHDRAWN_LUCK = Fortunes.id("idealogue_withdrawn_luck");
    private static final ResourceLocation WITHDRAWN_SPEED = Fortunes.id("idealogue_withdrawn_attack");
    private static final ResourceLocation SEASONED_SPEED = Fortunes.id("idealogue_seasoned_speed");
    private static final ResourceLocation SEASONED_MINING = Fortunes.id("idealogue_seasoned_mining");

    private static final Map<UUID, Map<UUID, Long>> EXCHANGES = new HashMap<>();

    private static final Map<UUID, Integer> WARNED = new HashMap<>();

    private Idealogue() {
    }

    private static boolean is(JojohaPlayerData data) {
        return Fortunes.on() && Fortune.has(data, Fortune.IDEALOGUE);
    }

    private static void exchanged(ServerPlayer player, LivingEntity foe) {
        long now = player.level().getGameTime();
        EXCHANGES.computeIfAbsent(player.getUUID(), id -> new HashMap<>()).put(foe.getUUID(), now);
        JojohaPlayerData data = PlayerDataAccess.get(player);
        data.lastConflict = now;
        PlayerDataAccess.set(player, data);
    }

    public static LivingEntity duelFoe(ServerPlayer player) {
        Map<UUID, Long> foes = EXCHANGES.get(player.getUUID());
        if (foes == null || foes.isEmpty()) {
            return null;
        }
        long now = player.level().getGameTime();
        LivingEntity only = null;
        Iterator<Map.Entry<UUID, Long>> each = foes.entrySet().iterator();
        while (each.hasNext()) {
            Map.Entry<UUID, Long> entry = each.next();
            Entity entity = player.serverLevel().getEntity(entry.getKey());
            if (now - entry.getValue() > DUEL_WINDOW_TICKS || !(entity instanceof LivingEntity living)
                    || !living.isAlive()) {
                each.remove();
                continue;
            }
            if (only != null) {
                return null;
            }
            only = living;
        }
        if (only == null) {
            return null;
        }
        LivingEntity foe = only;
        boolean crowded = !player.serverLevel().getEntitiesOfClass(Mob.class,
                new AABB(player.blockPosition()).inflate(CROWD_RANGE),
                mob -> mob != foe && mob.isAlive() && mob.getTarget() == player).isEmpty();
        return crowded ? null : foe;
    }

    private static boolean rival(LivingEntity foe) {
        if (foe instanceof Player player) {
            return PlayerDataAccess.get(player).stand.isPresent();
        }
        return foe instanceof Mob mob && MobStands.has(mob);
    }

    public static float dealt(ServerPlayer attacker, JojohaPlayerData data, LivingEntity victim, float amount) {
        if (!is(data) || victim == attacker) {
            return amount;
        }
        exchanged(attacker, victim);
        if (duelFoe(attacker) != victim) {
            return amount;
        }
        float scaled = amount;
        if (victim.getMaxHealth() > attacker.getMaxHealth()) {
            scaled *= OUTMATCHED;
        }
        if (rival(victim)) {
            scaled *= RIVAL;
        }
        return scaled;
    }

    public static float taken(ServerPlayer victim, JojohaPlayerData data, LivingEntity attacker, float amount) {
        if (!is(data) || attacker == victim) {
            return amount;
        }
        exchanged(victim, attacker);
        if (duelFoe(victim) != attacker || victim.getHealth() > victim.getMaxHealth() * CORNERED_AT) {
            return amount;
        }
        return amount * CORNERED;
    }

    public static void killed(ServerPlayer killer, LivingEntity dead) {
        JojohaPlayerData data = PlayerDataAccess.get(killer);
        if (!is(data)) {
            return;
        }
        Map<UUID, Long> foes = EXCHANGES.get(killer.getUUID());
        if (foes == null || !foes.containsKey(dead.getUUID()) || foes.size() > 1) {
            return;
        }
        foes.remove(dead.getUUID());
        killer.giveExperiencePoints(Math.max(5, Math.round(dead.getMaxHealth())));
        killer.heal(killer.getMaxHealth() * VICTORY_HEAL);
        killer.addEffect(new MobEffectInstance(MobEffects.REGENERATION, VICTORY_REGEN_TICKS, 1, false, false, true));
        killer.serverLevel().playSound(null, killer.blockPosition(), SoundEvents.PLAYER_LEVELUP,
                SoundSource.PLAYERS, 0.6F, 1.4F);
        data.lastConflict = killer.level().getGameTime();
        PlayerDataAccess.set(killer, data);
        killer.displayClientMessage(Component.translatable("message.jojoha.idealogue.victory"), true);
    }

    public static void marked(Mob mob) {
        if (!(mob.level() instanceof ServerLevel level) || !Fortunes.on()) {
            return;
        }
        Player chosen = level.getNearestPlayer(mob.getX(), mob.getY(), mob.getZ(), STAND_USER_RANGE,
                entity -> entity instanceof ServerPlayer player && !player.isCreative() && !player.isSpectator()
                        && is(PlayerDataAccess.get(player)));
        if (chosen != null) {
            mob.setTarget(chosen);
        }
    }

    public static int hungryDays(ServerPlayer player, JojohaPlayerData data) {
        long since = player.level().getGameTime() - data.lastConflict;
        return (int) Math.max(0, Math.min(WORST_DAYS, since / DAY_TICKS - GRACE_DAYS));
    }

    public static void tick(ServerPlayer player, JojohaPlayerData data, boolean active) {
        if (!active) {
            clear(player);
            return;
        }
        if (data.lastConflict <= 0L || data.lastConflict > player.level().getGameTime()) {
            data.lastConflict = player.level().getGameTime();
            PlayerDataAccess.set(player, data);
        }
        if (player.tickCount % 20 != 0) {
            return;
        }
        int hungry = hungryDays(player, data);
        Fortunes.modifier(player, Attributes.LUCK, WITHDRAWN_LUCK, LUCK_PER_DAY * hungry,
                AttributeModifier.Operation.ADD_VALUE);
        Fortunes.modifier(player, Attributes.ATTACK_SPEED, WITHDRAWN_SPEED, ATTACK_SPEED_PER_DAY * hungry,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        if (hungry > WARNED.getOrDefault(player.getUUID(), 0)) {
            Fortunes.whisper(player, Component.translatable("message.jojoha.idealogue.unsatiated"));
        }
        WARNED.put(player.getUUID(), hungry);

        boolean calm = data.combatTicks <= 0 && duelFoe(player) == null;
        int tier = calm ? Math.min(MAX_TIER, player.experienceLevel / LEVELS_PER_TIER) : 0;
        Fortunes.modifier(player, Attributes.MOVEMENT_SPEED, SEASONED_SPEED, SPEED_PER_TIER * tier,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        Fortunes.modifier(player, Attributes.BLOCK_BREAK_SPEED, SEASONED_MINING, MINING_PER_TIER * tier,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    private static void clear(ServerPlayer player) {
        if (player.tickCount % 20 != 0) {
            return;
        }
        Fortunes.modifier(player, Attributes.LUCK, WITHDRAWN_LUCK, 0.0, AttributeModifier.Operation.ADD_VALUE);
        Fortunes.modifier(player, Attributes.ATTACK_SPEED, WITHDRAWN_SPEED, 0.0,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        Fortunes.modifier(player, Attributes.MOVEMENT_SPEED, SEASONED_SPEED, 0.0,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        Fortunes.modifier(player, Attributes.BLOCK_BREAK_SPEED, SEASONED_MINING, 0.0,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    public static void forget(Player player) {
        EXCHANGES.remove(player.getUUID());
        WARNED.remove(player.getUUID());
    }
}
