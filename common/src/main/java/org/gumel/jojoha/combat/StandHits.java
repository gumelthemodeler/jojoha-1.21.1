package org.gumel.jojoha.combat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.fortune.Kindred;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.mob.MobStands;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class StandHits {
    private record Struck(UUID attacker, long tick) {
    }

    private static final Map<UUID, Struck> THROUGH_STAND = new HashMap<>();

    private static final Map<UUID, Struck> DIRECT = new HashMap<>();

    private static final int KEPT = 256;

    private static boolean forwarding;

    private static Entity swinging;

    private StandHits() {
    }

    public static boolean hittable(Entity attacker, Entity candidate) {
        if (!(candidate instanceof StandEntity stand)) {
            return true;
        }
        LivingEntity user = stand.getUser();
        return user != null && user.isAlive() && user != attacker && stand.isAlive() && !stand.isRemoved()
                && !stand.isDismissing() && !stand.isInvisible() && !stand.isPreviewMode()
                && !(attacker instanceof StandEntity other && other.getUser() == user);
    }

    public static LivingEntity bodyOf(LivingEntity victim) {
        if (victim instanceof StandEntity stand) {
            LivingEntity user = stand.getUser();
            return user != null ? user : victim;
        }
        return victim;
    }

    public static boolean hasStandOut(Entity entity) {
        if (entity instanceof StandEntity) {
            return true;
        }
        if (entity instanceof ServerPlayer player) {
            return PlayerDataAccess.get(player).standSummoned;
        }
        return entity instanceof Mob mob && MobStands.out(mob);
    }

    public static boolean isStandAttack(DamageSource source, StandEntity victim) {
        Entity attacker = source.getEntity();
        if (!(attacker instanceof LivingEntity) || attacker == victim.getUser() || attacker == victim) {
            return false;
        }
        if (source.is(DamageTypeTags.IS_EXPLOSION) || source.is(DamageTypeTags.IS_FIRE)
                || source.getDirectEntity() instanceof AbstractArrow) {
            return false;
        }
        if (swinging != null && swinging == attacker) {
            return false;
        }
        return hasStandOut(attacker) || Kindred.standPet(attacker);
    }

    public static boolean forward(StandEntity stand, DamageSource source, float amount) {
        LivingEntity user = stand.getUser();
        Entity attacker = source.getEntity();
        if (user == null || !user.isAlive() || attacker == null) {
            return false;
        }
        long now = stand.level().getGameTime();
        if (struckThisTick(DIRECT.get(user.getUUID()), attacker, now)
                || struckThisTick(THROUGH_STAND.get(user.getUUID()), attacker, now)) {
            return true;
        }
        remember(THROUGH_STAND, user.getUUID(), new Struck(attacker.getUUID(), now), now);
        if (BarrageBlow.is(source)) {
            user.invulnerableTime = 0;
        }
        boolean previous = forwarding;
        forwarding = true;
        try {
            return user.hurt(source, StandPierce.asThrownAt(user, source, amount));
        } finally {
            forwarding = previous;
        }
    }

    public static boolean refuseDirect(LivingEntity victim, DamageSource source) {
        if (forwarding || THROUGH_STAND.isEmpty()) {
            return false;
        }
        Entity attacker = source.getEntity();
        return attacker != null
                && struckThisTick(THROUGH_STAND.get(victim.getUUID()), attacker, victim.level().getGameTime());
    }

    public static void noteDirect(LivingEntity victim, DamageSource source) {
        if (forwarding || victim.level().isClientSide()) {
            return;
        }
        Entity attacker = source.getEntity();
        if (!(attacker instanceof LivingEntity) || !(victim instanceof Player || victim instanceof Mob)) {
            return;
        }
        long now = victim.level().getGameTime();
        remember(DIRECT, victim.getUUID(), new Struck(attacker.getUUID(), now), now);
    }

    private static boolean struckThisTick(Struck struck, Entity attacker, long now) {
        return struck != null && struck.tick() == now && struck.attacker().equals(attacker.getUUID());
    }

    private static void remember(Map<UUID, Struck> into, UUID key, Struck struck, long now) {
        if (into.size() >= KEPT) {
            into.values().removeIf(old -> old.tick() < now - 1);
        }
        into.put(key, struck);
    }

    public static void beginSwing(Entity attacker) {
        swinging = attacker;
    }

    public static void endSwing() {
        swinging = null;
    }
}
