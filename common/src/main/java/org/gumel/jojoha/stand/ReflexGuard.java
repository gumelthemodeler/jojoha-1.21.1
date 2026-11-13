package org.gumel.jojoha.stand;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.combat.BarrageBlow;
import org.gumel.jojoha.combat.Defence;
import org.gumel.jojoha.combat.EnergySystem;
import org.gumel.jojoha.combat.GuardBreak;
import org.gumel.jojoha.combat.fx.ImpactSpec;
import org.gumel.jojoha.combat.fx.Impacts;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ReflexGuard {
    public static final int MELEE_EVERY_TICKS = 100;
    public static final int SHOT_EVERY_TICKS = 40;
    public static final float COST_SHARE = 0.04F;
    private static final double CATCH_REACH = 3.5;

    private static final Map<UUID, Long> MELEE_AT = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> SHOT_AT = new ConcurrentHashMap<>();

    private ReflexGuard() {
    }

    public static boolean catches(ServerPlayer player, DamageSource source) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (!data.standSummoned || !data.stand.isPresent() || data.standGuarding
                || !data.manifestTier().canGuard() || GuardBreak.isBroken(player) || data.guardBreakCooldownTicks > 0
                || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || source.is(DamageTypeTags.IS_EXPLOSION)
                || BarrageBlow.is(source) || !Defence.guardable(source)) {
            return false;
        }

        Entity direct = source.getDirectEntity();
        boolean shot = direct instanceof Projectile;
        if (!shot && !data.halfSummon) {
            return false;
        }
        if (!shot && !(source.getEntity() instanceof LivingEntity && direct == source.getEntity())) {
            return false;
        }
        Vec3 from = shot ? direct.position() : source.getEntity().position();
        if (Defence.facing(player, from) == Defence.Facing.BACK) {
            return false;
        }

        StandEntity stand = StandSummonHandler.findStand(player, data);
        if (stand == null || shot && !data.halfSummon && stand.distanceToSqr(player) > CATCH_REACH * CATCH_REACH) {
            return false;
        }

        Map<UUID, Long> last = shot ? SHOT_AT : MELEE_AT;
        long now = player.level().getGameTime();
        Long caught = last.get(player.getUUID());
        if (caught != null && now >= caught && now - caught < (shot ? SHOT_EVERY_TICKS : MELEE_EVERY_TICKS)) {
            return false;
        }

        float cost = data.maxStandEnergy() * COST_SHARE;
        if (data.standEnergy - cost <= EnergySystem.spentAt(data)) {
            return false;
        }
        data.standEnergy -= cost;
        last.put(player.getUUID(), now);
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);

        ServerLevel level = player.serverLevel();
        if (shot) {
            stand.catchProjectile((Projectile) direct);
            return true;
        }
        if (source.getEntity() instanceof LivingEntity attacker) {
            stand.onGuardHit(attacker, player);
        }
        Vec3 toward = from.subtract(player.position());
        Impacts.landed(level, player, player.getEyePosition().add(toward.normalize().scale(0.6)), toward,
                ImpactSpec.FIST_PARRY, StandShadowForm.auraColourFor(data.stand, player.getUUID()));
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS,
                0.5F, 1.4F);
        return true;
    }

    public static void forget(UUID player) {
        MELEE_AT.remove(player);
        SHOT_AT.remove(player);
    }
}
