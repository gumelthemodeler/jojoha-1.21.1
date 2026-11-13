package org.gumel.jojoha.stand.anubis;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.combat.BarrageBlow;
import org.gumel.jojoha.combat.DamageScale;
import org.gumel.jojoha.combat.Defence;
import org.gumel.jojoha.combat.fx.ImpactSpec;
import org.gumel.jojoha.combat.fx.Impacts;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.stand.StandShadowForm;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class Adaptation {
    static final int[] THRESHOLDS = {2, 4, 7, 11};

    private static final int MOST = THRESHOLDS[THRESHOLDS.length - 1];

    private static final float[] TAKEN_OFF = {0.05F, 0.10F, 0.15F, 0.20F};

    private static final float DAMAGE_PER_LEVEL = 0.04F;

    private static final int PREDICTION_TICKS = 4;

    private static final double PREDICTION_RANGE = 8.0;

    private static final float COUNTER_SHARE = 0.5F;
    private static final float COUNTER_LEAST = 2.0F;
    private static final float COUNTER_MOST = 6.0F;
    private static final int COUNTER_EVERY = 60;
    private static final double COUNTER_REACH = 4.0;

    private static final int MASTERY_EVERY = 100;

    private static final double SIGHT_RANGE = 32.0;
    private static final int FADES_AFTER = 60 * 20;
    private static final int FADES_EVERY = 10 * 20;

    private static final class Foe {
        final Map<String, Integer> attacks = new HashMap<>();
        long lastSeen;
        long lastFaded;
    }

    private static final Map<UUID, Map<UUID, Foe>> LEDGER = new HashMap<>();
    private static final Map<UUID, Long> COUNTERED_AT = new HashMap<>();
    private static final Map<UUID, Long> MASTERED_AT = new HashMap<>();

    private Adaptation() {
    }

    static int levelOf(int experiences) {
        int level = 0;
        for (int threshold : THRESHOLDS) {
            if (experiences >= threshold) {
                level++;
            }
        }
        return level;
    }

    static String kindOf(DamageSource source) {
        String type = source.typeHolder().unwrapKey().map(key -> key.location().toString()).orElse("unknown");
        Entity direct = source.getDirectEntity();
        Entity attacker = source.getEntity();
        if (direct != null && direct != attacker) {
            return type + "/" + BuiltInRegistries.ENTITY_TYPE.getKey(direct.getType());
        }
        if (attacker instanceof LivingEntity living) {
            return type + "/" + (living.getMainHandItem().isEmpty() ? "hand"
                    : BuiltInRegistries.ITEM.getKey(living.getMainHandItem().getItem()).toString());
        }
        return type;
    }

    static void experience(ServerPlayer user, LivingEntity foe, String kind) {
        Foe known = LEDGER.computeIfAbsent(user.getUUID(), key -> new HashMap<>())
                .computeIfAbsent(foe.getUUID(), key -> new Foe());
        known.lastSeen = user.level().getGameTime();
        int before = known.attacks.getOrDefault(kind, 0);
        if (before >= MOST) {
            return;
        }
        known.attacks.put(kind, before + 1);
        int level = levelOf(before + 1);
        if (level > levelOf(before)) {
            learned(user, foe, level);
        }
    }

    private static void learned(ServerPlayer user, LivingEntity foe, int level) {
        user.displayClientMessage(Component.translatable("message.jojoha.anubis.learned",
                foe.getDisplayName(), Component.translatable("adaptation.jojoha.level." + level)), true);
        user.playNotifySound(SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 0.9F + level * 0.15F);
    }

    static int level(ServerPlayer user, LivingEntity foe, String kind) {
        Foe known = known(user, foe);
        return known == null ? 0 : levelOf(known.attacks.getOrDefault(kind, 0));
    }

    public static int foeLevel(ServerPlayer user, LivingEntity foe) {
        Foe known = known(user, foe);
        if (known == null) {
            return 0;
        }
        int best = 0;
        for (int experiences : known.attacks.values()) {
            best = Math.max(best, levelOf(experiences));
        }
        return best;
    }

    private static Foe known(ServerPlayer user, LivingEntity foe) {
        Map<UUID, Foe> foes = LEDGER.get(user.getUUID());
        return foes == null ? null : foes.get(foe.getUUID());
    }

    static float damageBonus(ServerPlayer user, LivingEntity foe) {
        return foeLevel(user, foe) * DAMAGE_PER_LEVEL;
    }

    static float struck(ServerPlayer user, LivingEntity foe, DamageSource source, float amount) {
        String kind = kindOf(source);
        int level = level(user, foe, kind);
        float taken = level > 0 ? amount * (1F - TAKEN_OFF[level - 1]) : amount;
        experience(user, foe, kind);
        if (level >= 3) {
            counter(user, foe, taken);
        }
        return taken;
    }

    private static void counter(ServerPlayer user, LivingEntity foe, float taken) {
        long now = user.level().getGameTime();
        Long last = COUNTERED_AT.get(user.getUUID());
        if (last != null && now - last < COUNTER_EVERY && now >= last) {
            return;
        }
        if (!foe.isAlive() || foe.distanceTo(user) > COUNTER_REACH) {
            return;
        }
        COUNTERED_AT.put(user.getUUID(), now);
        float cut = Mth.clamp(taken * COUNTER_SHARE, COUNTER_LEAST, COUNTER_MOST);
        foe.hurt(user.damageSources().playerAttack(user), DamageScale.stand(user, foe, cut));
        user.swing(InteractionHand.MAIN_HAND, true);
        ServerLevel level = user.serverLevel();
        Vec3 toward = foe.position().subtract(user.position());
        Impacts.landed(level, foe, toward,
                ImpactSpec.MEDIUM.silent(), Impacts.STEEL);
        level.playSound(null, foe.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8F, 1.7F);
    }

    static boolean foresees(ServerPlayer user, LivingEntity foe, DamageSource source) {
        if (level(user, foe, kindOf(source)) < 4 || !Defence.guardable(source)
                || BarrageBlow.is(source)
                || Defence.facing(user, foe) == Defence.Facing.BACK) {
            return false;
        }
        long now = user.level().getGameTime();
        Long last = MASTERED_AT.get(user.getUUID());
        if (last != null && now - last < MASTERY_EVERY && now >= last) {
            return false;
        }
        if (!Defence.openParry(user, 1)) {
            return false;
        }
        int colour = StandShadowForm.auraColourFor(
                PlayerDataAccess.get(user).stand);
        boolean turned = Defence.tryParry(user, source, colour);
        if (turned) {
            MASTERED_AT.put(user.getUUID(), now);
        }
        return turned;
    }

    static int windowBonus(ServerPlayer user) {
        Map<UUID, Foe> foes = LEDGER.get(user.getUUID());
        if (foes == null) {
            return 0;
        }
        ServerLevel level = user.serverLevel();
        for (UUID id : foes.keySet()) {
            if (level.getEntity(id) instanceof LivingEntity foe && foe.distanceTo(user) <= PREDICTION_RANGE
                    && foeLevel(user, foe) >= 2) {
                return PREDICTION_TICKS;
            }
        }
        return 0;
    }

    static void tick(ServerPlayer user) {
        if (user.tickCount % 20 != 0) {
            return;
        }
        Map<UUID, Foe> foes = LEDGER.get(user.getUUID());
        if (foes == null) {
            return;
        }
        long now = user.level().getGameTime();
        Iterator<Map.Entry<UUID, Foe>> it = foes.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Foe> entry = it.next();
            Foe known = entry.getValue();
            if (known.lastSeen > now) {
                known.lastSeen = now;
            }
            if (inSight(user, entry.getKey())) {
                known.lastSeen = now;
                continue;
            }
            if (now - known.lastSeen < FADES_AFTER || now - known.lastFaded < FADES_EVERY) {
                continue;
            }
            known.lastFaded = now;
            known.attacks.replaceAll((kind, experiences) -> experiences - 1);
            known.attacks.values().removeIf(experiences -> experiences <= 0);
            if (known.attacks.isEmpty()) {
                it.remove();
            }
        }
        if (foes.isEmpty()) {
            LEDGER.remove(user.getUUID());
        }
    }

    private static boolean inSight(ServerPlayer user, UUID id) {
        Entity found = user.serverLevel().getEntity(id);
        return found instanceof LivingEntity foe && foe.isAlive() && foe.distanceTo(user) <= SIGHT_RANGE
                && user.hasLineOfSight(foe);
    }

    static void forget(Player player) {
        LEDGER.remove(player.getUUID());
        COUNTERED_AT.remove(player.getUUID());
        MASTERED_AT.remove(player.getUUID());
    }
}
