package org.gumel.jojoha.combat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.gumel.jojoha.accessory.Relics;
import org.gumel.jojoha.config.JojohaServerConfig;
import org.gumel.jojoha.config.NumberSetting;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.hamon.HamonCombat;
import org.gumel.jojoha.stand.heaven.Heaven;
import org.gumel.jojoha.stand.mob.MobStands;

import java.util.Map;
import java.util.WeakHashMap;

public final class DamageScale {
    public enum Kind {
        STAND(true),
        HAMON(false),
        VAMPIRE(false),
        COMBAT(false),
        MOB_STAND(true),
        VAMPIRE_MOB(false);

        private final boolean perStand;

        Kind(boolean perStand) {
            this.perStand = perStand;
        }

        public NumberSetting setting() {
            return switch (this) {
                case STAND -> JojohaServerConfig.STAND_DAMAGE;
                case HAMON -> JojohaServerConfig.HAMON_DAMAGE;
                case VAMPIRE -> JojohaServerConfig.VAMPIRE_DAMAGE;
                case COMBAT -> JojohaServerConfig.COMBAT_DAMAGE;
                case MOB_STAND -> JojohaServerConfig.MOB_STAND_DAMAGE;
                case VAMPIRE_MOB -> JojohaServerConfig.VAMPIRE_MOB_DAMAGE;
            };
        }
    }

    public static final float HIT_CEILING = 10F;

    private static final Map<Entity, Long> MOD_HITS = new WeakHashMap<>();

    private DamageScale() {
    }

    public static float stand(Entity attacker, Entity target, float amount) {
        return scale(Kind.STAND, attacker, target, amount);
    }

    public static float hamon(Entity attacker, Entity target, float amount) {
        return scale(Kind.HAMON, attacker, target, amount);
    }

    public static float vampire(Entity attacker, Entity target, float amount) {
        return scale(Kind.VAMPIRE, attacker, target, amount);
    }

    public static float combat(Entity attacker, Entity target, float amount) {
        return scale(Kind.COMBAT, attacker, target, amount);
    }

    public static float mobStand(Entity attacker, Entity target, float amount) {
        return scale(Kind.MOB_STAND, attacker, target, amount);
    }

    public static float vampireMob(Entity attacker, Entity target, float amount) {
        return scale(Kind.VAMPIRE_MOB, attacker, target, amount);
    }

    public static float scale(Kind kind, Entity attacker, Entity target, float raw, float ceiling) {
        mark(target);
        boolean beyond = beyondCeiling(kind, attacker, target, raw);
        float amount = beyond ? raw : ComboDecay.apply(attacker, target, Math.min(raw, ceiling));
        float scaled = amount * kind.setting().value();
        if (kind.perStand) {
            scaled *= JojohaServerConfig.standDamage(standOf(attacker));
        }
        if (kind == Kind.HAMON) {
            scaled *= Relics.hamonScale(attacker);
        }
        if (target instanceof Player) {
            scaled *= JojohaServerConfig.PLAYER_DAMAGE.value();
        }
        float cap = JojohaServerConfig.MAX_HIT.value();
        return cap > 0F ? Math.min(scaled, cap) : scaled;
    }

    public static float scale(Kind kind, Entity attacker, Entity target, float amount) {
        return scale(kind, attacker, target, amount, HIT_CEILING);
    }

    public static boolean modHit(LivingEntity victim) {
        Long at = MOD_HITS.get(victim);
        return at != null && at == victim.level().getGameTime();
    }

    private static void mark(Entity target) {
        if (target == null || target.level().isClientSide()) {
            return;
        }
        long now = target.level().getGameTime();
        MOD_HITS.put(target, now);
        if (target instanceof LivingEntity living) {
            MOD_HITS.put(StandHits.bodyOf(living), now);
        }
    }

    public static float withinCeiling(float before, float after) {
        return Math.min(after, Math.max(before, HIT_CEILING));
    }

    private static boolean beyondCeiling(Kind kind, Entity attacker, Entity target, float raw) {
        return raw == Float.MAX_VALUE
                || Heaven.is(attacker)
                || kind == Kind.HAMON && target instanceof LivingEntity living
                        && HamonCombat.isUndead(living);
    }

    private static ResourceLocation standOf(Entity attacker) {
        if (attacker instanceof Player player) {
            return PlayerDataAccess.get(player).stand.standId();
        }
        if (attacker instanceof Mob mob && MobStands.has(mob)) {
            return MobStands.standId(mob);
        }
        return null;
    }
}
