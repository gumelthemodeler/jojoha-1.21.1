package org.gumel.jojoha.stand;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.registry.ModRegistries;
import org.gumel.jojoha.registry.ModSounds;

final class StandGuard {
    private final StandEntity stand;

    StandGuard(StandEntity stand) {
        this.stand = stand;
    }

    private static final double GUARD_ARC = 0.2;

    private static final double GUARD_PUSH_RANGE = 1.35;

    private static final double GUARD_PUSH_STRENGTH = 0.16;

    private static final double GUARD_DEFLECT_KNOCKBACK = 0.62;

    private static final double GUARD_DEFLECT_LIFT = 0.22;

    private static final double GUARD_PUSH_LIFT = 0.04;

    private static final double GUARD_PUSH_ARC = -0.1;

    private static final int GUARD_BROKEN_FLASH_TICKS = 14;

    private static final int GUARD_BREAK_SHARDS = 18;

    private static final double GUARD_BREAK_SHARD_SPEED = 0.18;

    private static final double GUARD_BREAK_SHARD_VARIANCE = 0.22;

    private static final double GUARD_BREAK_SCATTER = 0.3;

    private static final double GUARD_BREAK_HEIGHT = 0.55;

    private static final int COUNTER_COOLDOWN_TICKS = 70;

    public void absorbStrain(float amount) {
    }

    void pushBackFromGuard(Player owner) {
        if (!(stand.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        Vec3 look = owner.getLookAngle();
        Vec3 forward = new Vec3(look.x, 0, look.z).normalize();
        Vec3 facing = guardDirection(owner, forward);

        AABB area = owner.getBoundingBox().inflate(GUARD_PUSH_RANGE);

        for (LivingEntity threat : serverLevel.getEntitiesOfClass(LivingEntity.class, area,
                candidate -> candidate != owner && candidate.isAlive()
                        && !(candidate instanceof StandEntity))) {

            if (!isWorthPushing(threat, owner)) {
                continue;
            }

            Vec3 away = threat.position().subtract(owner.position());
            Vec3 flat = new Vec3(away.x, 0, away.z);
            double distance = flat.length();
            if (distance > GUARD_PUSH_RANGE || distance < 1.0E-4) {
                continue;
            }

            Vec3 outward = flat.scale(1.0 / distance);
            if (outward.dot(facing) < GUARD_PUSH_ARC) {
                continue;
            }

            double strength = GUARD_PUSH_STRENGTH * (1.0 - distance / GUARD_PUSH_RANGE);

            threat.setDeltaMovement(threat.getDeltaMovement()
                    .add(outward.x * strength, GUARD_PUSH_LIFT, outward.z * strength));
            threat.hurtMarked = true;
            threat.hasImpulse = true;
        }
    }

    void deflect(LivingEntity attacker, Player owner) {
        Vec3 away = attacker.position().subtract(owner.position());
        Vec3 flat = new Vec3(away.x, 0, away.z);
        if (flat.lengthSqr() < 1.0E-4) {
            return;
        }

        Vec3 outward = flat.normalize().scale(GUARD_DEFLECT_KNOCKBACK);
        attacker.setDeltaMovement(attacker.getDeltaMovement().x * 0.4 + outward.x,
                GUARD_DEFLECT_LIFT,
                attacker.getDeltaMovement().z * 0.4 + outward.z);
        attacker.hurtMarked = true;
        attacker.hasImpulse = true;
    }

    private static boolean isWorthPushing(LivingEntity candidate, Player owner) {
        if (candidate instanceof Enemy || candidate instanceof Player) {
            return true;
        }
        return candidate instanceof Mob mob && mob.getTarget() == owner;
    }

    void faceGuard(Player owner) {
        Vec3 look = owner.getLookAngle();
        Vec3 forward = new Vec3(look.x, 0, look.z).normalize();
        Vec3 facing = guardDirection(owner, forward);

        float yaw = (float) (Mth.atan2(facing.z, facing.x) * (180F / Math.PI)) - 90F;
        float eased = Mth.approachDegrees(stand.getYRot(), yaw, stand.movement().combatRotationSpeed());

        stand.setYRot(eased);
        stand.setYBodyRot(eased);
        stand.setYHeadRot(eased);
    }

    Vec3 guardDirection(Player owner, Vec3 forward) {
        Vec3 toThreat = guardThreatDirection(owner);
        if (toThreat == null) {
            return forward;
        }

        double alignment = toThreat.dot(forward);
        if (alignment >= GUARD_ARC) {
            return toThreat;
        }

        Vec3 right = new Vec3(-forward.z, 0, forward.x);
        double side = Math.signum(toThreat.dot(right));
        if (side == 0) {
            side = 1;
        }

        double lateral = Math.sqrt(Math.max(0, 1 - GUARD_ARC * GUARD_ARC));
        return forward.scale(GUARD_ARC).add(right.scale(lateral * side)).normalize();
    }

    public Entity getGuardThreat() {
        int id = stand.synced(StandEntity.DATA_GUARD_THREAT);
        if (id < 0) {
            return null;
        }

        Entity threat = stand.level().getEntity(id);
        return threat != null && threat.isAlive() ? threat : null;
    }

    private Vec3 guardThreatDirection(Player owner) {
        Entity threat = getGuardThreat();
        if (threat == null) {
            return null;
        }

        Vec3 away = threat.position().subtract(owner.position());
        Vec3 flat = new Vec3(away.x, 0, away.z);
        return flat.lengthSqr() < 1.0E-4 ? null : flat.normalize();
    }

    public void onGuardHit(LivingEntity attacker, Player owner) {
        if (attacker == null || !attacker.isAlive()) {
            return;
        }

        stand.guardThreat = attacker;
        stand.guardThreatTicks = StandEntity.GUARD_THREAT_MEMORY;

        deflect(attacker, owner);

        Vec3 look = owner.getLookAngle();
        Vec3 forward = new Vec3(look.x, 0, look.z).normalize();
        Vec3 away = attacker.position().subtract(owner.position());
        Vec3 flat = new Vec3(away.x, 0, away.z);
        Vec3 toAttacker = flat.lengthSqr() < 1.0E-4 ? null : flat.normalize();

        if (toAttacker == null || toAttacker.dot(forward) >= 0 || stand.counterCooldownTicks > 0
                || !stand.getMode().isAutonomous()) {
            return;
        }

        stand.counterCooldownTicks = COUNTER_COOLDOWN_TICKS;

        stand.faceTowards(attacker);
        stand.pursueAndPunch(attacker, false);

        if (stand.level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, stand.blockPosition(), ModSounds.STAND_JUMP.get(),
                    SoundSource.PLAYERS, 0.7F, 1.25F);
        }
    }

    public void startGuarding() {
        stand.guarding = true;
        stand.triggerAnim(StandEntity.CONTROLLER, stand.isChariot() ? "chariot_guard" : "block");
    }

    public void setGuardStrain(float strain) {
        stand.sync(StandEntity.DATA_GUARD_STRAIN, Mth.clamp(strain, 0F, 1F));
    }

    public float guardStrain() {
        return stand.synced(StandEntity.DATA_GUARD_STRAIN);
    }

    public int guardBrokenTicks() {
        return stand.synced(StandEntity.DATA_GUARD_BROKEN);
    }

    public static int guardBrokenFlashTicks() {
        return GUARD_BROKEN_FLASH_TICKS;
    }

    public void stopGuarding() {
        stand.guarding = false;
        stand.guardThreat = null;
        stand.guardThreatTicks = 0;
        stand.sync(StandEntity.DATA_GUARD_STRAIN, 0F);
        stand.stopTriggeredAnim(StandEntity.CONTROLLER, stand.isChariot() ? "chariot_guard" : "block");
    }

    public void triggerGuardBroken() {
        stand.guarding = false;
        stand.sync(StandEntity.DATA_GUARD_STRAIN, 0F);
        stand.sync(StandEntity.DATA_GUARD_BROKEN, GUARD_BROKEN_FLASH_TICKS);
        stand.triggerAnim(StandEntity.CONTROLLER, stand.isChariot() ? "chariot_guard_break" : "guardbroken");
        playGuardBreak();
    }

    private void playGuardBreak() {
        if (!(stand.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        Vec3 at = stand.position().add(0, stand.getBbHeight() * GUARD_BREAK_HEIGHT, 0);

        serverLevel.sendParticles(ModRegistries.GUARD_BREAK.get(), at.x, at.y, at.z, 0,
                0.0, 0.0, 0.0, 0.0);

        for (int i = 0; i < GUARD_BREAK_SHARDS; i++) {
            double yaw = stand.getRandom().nextDouble() * Math.PI * 2;
            double lift = 0.15 + stand.getRandom().nextDouble() * 0.85;
            double flat = Math.sqrt(Math.max(0, 1 - lift * lift));
            double speed = GUARD_BREAK_SHARD_SPEED + stand.getRandom().nextDouble() * GUARD_BREAK_SHARD_VARIANCE;

            double vx = Math.cos(yaw) * flat * speed;
            double vy = lift * speed;
            double vz = Math.sin(yaw) * flat * speed;

            double ox = (stand.getRandom().nextDouble() - 0.5) * GUARD_BREAK_SCATTER;
            double oy = (stand.getRandom().nextDouble() - 0.5) * GUARD_BREAK_SCATTER;
            double oz = (stand.getRandom().nextDouble() - 0.5) * GUARD_BREAK_SCATTER;

            serverLevel.sendParticles(ModRegistries.GUARD_BREAK.get(),
                    at.x + ox, at.y + oy, at.z + oz, 0, vx, vy, vz, 1.0);
        }

        serverLevel.playSound(null, stand.blockPosition(), ModSounds.GUARD_BREAK.get(),
                SoundSource.PLAYERS, 1.1F, 1.0F);
        serverLevel.playSound(null, stand.blockPosition(), SoundEvents.SHIELD_BREAK,
                SoundSource.PLAYERS, 0.9F, 0.7F);
    }

    public void recoverFromGuardBreak() {
        stand.stopTriggeredAnim(StandEntity.CONTROLLER, stand.isChariot() ? "chariot_guard_break" : "guardbroken");
    }
}
