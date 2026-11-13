package org.gumel.jojoha.entity.npc;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.combat.DamageScale;
import org.gumel.jojoha.combat.fx.ImpactSpec;
import org.gumel.jojoha.combat.fx.Impacts;
import org.gumel.jojoha.combat.fx.MovementFx;
import org.gumel.jojoha.hamon.HamonCombat;
import org.gumel.jojoha.hamon.HamonFx;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.registry.ModSounds;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;

final class HamonTechniques extends Goal {
    private static final float UNDEAD_BONUS = 3F;
    private static final int BREATHER = 10;
    private static final int PRACTICE_ODDS = 900;
    private static final double ZOOM_MIN = 3.0;
    private static final double ZOOM_MAX = 9.0;
    private static final double ZOOM_REACH = 10.0;
    private static final double ZOOM_STEP = 0.5;
    private static final double ZOOM_HIT_RADIUS = 0.6;
    private static final float ZOOM_DAMAGE = 4F;
    private static final double ZOOM_PULL = 0.45;
    private static final int ZOOM_ARM_TICKS = 8;
    private static final double PALM_RANGE = 2.6;
    private static final double PALM_REACH = 3.2;
    private static final float PALM_DAMAGE = 5F;
    private static final double PALM_PUSH = 0.8;
    private static final double PULSE_RANGE = 3.5;
    private static final float PULSE_DAMAGE = 3F;
    private static final double PULSE_PUSH = 1.0;
    private static final double HEAL_SAFE = 6.0;
    private static final float HEAL_BELOW = 0.45F;
    private static final float HEAL_IDLE_BELOW = 0.75F;
    private static final float HEAL_STEP = 2F;
    private static final int HEAL_EVERY = 10;
    private static final double RETREAT_DISTANCE = 9.0;
    private static final double RETREAT_SPEED = 1.35;
    private static final double HOP_SPEED = 0.55;
    private static final double HOP_LIFT = 0.28;
    private static final int SIDESTEP_ODDS = 3;

    private enum Technique {
        ZOOM_PUNCH(MovementFx.ZOOM_PUNCH, MovementFx.ZOOM_PUNCH_CONTACT_TICKS, 27, 100),
        PALM(MovementFx.PALM, MovementFx.PALM_CONTACT_TICKS, 18, 50),
        RIPPLE_PULSE(MovementFx.RIPPLE_PULSE, MovementFx.RIPPLE_PULSE_CONTACT_TICKS, 30, 200),
        HEAL(MovementFx.HEAL, -1, 40, 400),
        CHANNEL(MovementFx.CHANNEL, -1, 33, 0),
        GATHER(MovementFx.GATHER, -1, 25, 0),
        BACKSTEP(null, -1, 8, 60),
        SIDESTEP(null, -1, 6, 50),
        RETREAT(null, -1, 40, 120);

        @Nullable
        private final String clip;
        private final int contact;
        private final int length;
        private final int cooldown;

        Technique(@Nullable String clip, int contact, int length, int cooldown) {
            this.clip = clip;
            this.contact = contact;
            this.length = length;
            this.cooldown = cooldown;
        }
    }

    private final JojoNpc npc;
    private final long[] readyAt = new long[Technique.values().length];
    private long breatherUntil;
    @Nullable
    private Technique current;
    @Nullable
    private Technique next;
    @Nullable
    private LivingEntity target;
    private int ticks;

    HamonTechniques(JojoNpc npc) {
        this.npc = npc;
        setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (npc.talking()) {
            next = null;
            return false;
        }
        long now = npc.level().getGameTime();
        target = npc.getTarget() != null && npc.getTarget().isAlive() ? npc.getTarget() : null;
        if (next != null) {
            current = next;
            next = null;
            if (fits(current, now)) {
                return true;
            }
        }
        if (now < breatherUntil) {
            return false;
        }
        current = pick(now);
        return current != null;
    }

    private boolean fits(Technique technique, long now) {
        if (!ready(technique, now)) {
            return false;
        }
        if (technique == Technique.HEAL) {
            return target == null || npc.distanceTo(target) > HEAL_SAFE - 1.5;
        }
        if (technique == Technique.ZOOM_PUNCH) {
            return target != null && npc.distanceTo(target) <= ZOOM_MAX && npc.hasLineOfSight(target);
        }
        return target != null || technique.clip != null;
    }

    @Nullable
    private Technique pick(long now) {
        boolean healReady = ready(Technique.HEAL, now);
        if (target == null) {
            if (healReady && npc.getHealth() < npc.getMaxHealth() * HEAL_IDLE_BELOW) {
                return Technique.HEAL;
            }
            if (npc.getRandom().nextInt(PRACTICE_ODDS) == 0 && npc.getNavigation().isDone()) {
                return npc.getRandom().nextBoolean() ? Technique.CHANNEL : Technique.GATHER;
            }
            return null;
        }
        double distance = npc.distanceTo(target);
        if (healReady && npc.getHealth() < npc.getMaxHealth() * HEAL_BELOW) {
            if (distance > HEAL_SAFE) {
                return Technique.HEAL;
            }
            if (ready(Technique.RETREAT, now)) {
                return Technique.RETREAT;
            }
        }
        boolean pulseReady = ready(Technique.RIPPLE_PULSE, now);
        if (pulseReady && foes(PULSE_RANGE).size() >= 2) {
            return Technique.RIPPLE_PULSE;
        }
        if (distance <= PALM_RANGE) {
            if (ready(Technique.PALM, now)) {
                return Technique.PALM;
            }
            if (pulseReady && npc.hurtTime > 0) {
                return Technique.RIPPLE_PULSE;
            }
            if (ready(Technique.BACKSTEP, now) && ready(Technique.ZOOM_PUNCH, now)) {
                next = Technique.ZOOM_PUNCH;
                return Technique.BACKSTEP;
            }
        }
        if (npc.hurtTime > 0 && ready(Technique.SIDESTEP, now) && npc.getRandom().nextInt(SIDESTEP_ODDS) == 0) {
            return Technique.SIDESTEP;
        }
        if (ready(Technique.ZOOM_PUNCH, now) && distance >= ZOOM_MIN && distance <= ZOOM_MAX
                && npc.hasLineOfSight(target)) {
            return Technique.ZOOM_PUNCH;
        }
        return null;
    }

    private boolean ready(Technique technique, long now) {
        return now >= readyAt[technique.ordinal()];
    }

    @Override
    public void start() {
        ticks = 0;
        if (current == null) {
            return;
        }
        npc.getNavigation().stop();
        face();
        if (current.clip != null) {
            npc.playClip(current.clip);
        }
        if (!(npc.level() instanceof ServerLevel level)) {
            return;
        }
        switch (current) {
            case HEAL -> {
                HamonFx.ring(level, npc.getX(), npc.getY() + 1.0, npc.getZ(), 1.5, 1);
                level.playSound(null, npc.blockPosition(), ModSounds.HAMON_BREATHE.get(), SoundSource.NEUTRAL, 0.6F, 1.2F);
                HamonFx.chargeSound(level, npc, 0.5F, 1.2F);
            }
            case CHANNEL, GATHER -> HamonFx.chargeSound(level, npc, 0.4F, 1.1F);
            case ZOOM_PUNCH, PALM, RIPPLE_PULSE -> HamonFx.chargeLoop(level, npc, 0.5F, 1.15F);
            case BACKSTEP -> hop(away(), 1.0);
            case SIDESTEP -> hop(side(), 0.8);
            case RETREAT -> flee();
        }
    }

    @Override
    public boolean canContinueToUse() {
        if (current == null || !npc.isAlive() || ticks >= current.length) {
            return false;
        }
        if (current == Technique.RETREAT) {
            return target != null && target.isAlive() && npc.distanceTo(target) < HEAL_SAFE + 1.0;
        }
        return true;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (current == null) {
            return;
        }
        ticks++;
        if (current == Technique.RETREAT) {
            if (npc.getNavigation().isDone()) {
                flee();
            }
            return;
        }
        npc.getNavigation().stop();
        if (current.contact < 0 || ticks <= current.contact) {
            face();
        }
        if (!(npc.level() instanceof ServerLevel level)) {
            return;
        }
        if (current.clip != null) {
            HamonFx.aura(level, npc, false);
        }
        if (current == Technique.HEAL && ticks % HEAL_EVERY == 0) {
            npc.heal(HEAL_STEP);
            HamonFx.ring(level, npc.getX(), npc.getY() + npc.getBbHeight() * 0.5, npc.getZ(), 1.1, 1);
            HamonFx.fineMote(level, npc.getX(), npc.getY() + npc.getBbHeight() * 0.6, npc.getZ(), 6, 0.3);
        }
        if ((current == Technique.CHANNEL || current == Technique.GATHER) && ticks % HEAL_EVERY == 0) {
            HamonFx.fineMote(level, npc.getX(), npc.getY() + npc.getBbHeight() * 0.6, npc.getZ(), 4, 0.35);
        }
        if (ticks == current.contact) {
            land(level, current);
        }
    }

    @Override
    public void stop() {
        if (current != null) {
            long now = npc.level().getGameTime();
            readyAt[current.ordinal()] = now + current.cooldown;
            if (current.clip != null && current.contact >= 0) {
                breatherUntil = now + BREATHER;
            }
            if (current == Technique.RETREAT) {
                next = Technique.HEAL;
            } else if (current == Technique.PALM && next == null && ready(Technique.BACKSTEP, now)
                    && ready(Technique.ZOOM_PUNCH, now) && npc.getRandom().nextBoolean()) {
                next = Technique.BACKSTEP;
            } else if (current != Technique.BACKSTEP) {
                next = null;
            }
            if (current == Technique.BACKSTEP && next == null && ready(Technique.ZOOM_PUNCH, now)) {
                next = Technique.ZOOM_PUNCH;
            }
        }
        npc.getNavigation().stop();
        current = null;
        target = null;
    }

    private void face() {
        if (target == null || !target.isAlive()) {
            return;
        }
        npc.getLookControl().setLookAt(target, 60F, 60F);
        double dx = target.getX() - npc.getX();
        double dz = target.getZ() - npc.getZ();
        float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90F;
        npc.setYRot(yaw);
        npc.yBodyRot = yaw;
        npc.yHeadRot = yaw;
    }

    private Vec3 away() {
        if (target == null) {
            return Vec3.ZERO;
        }
        Vec3 away = npc.position().subtract(target.position());
        return new Vec3(away.x, 0, away.z).normalize();
    }

    private Vec3 side() {
        Vec3 away = away();
        int sign = npc.getRandom().nextBoolean() ? 1 : -1;
        return new Vec3(-away.z * sign, 0, away.x * sign);
    }

    private void hop(Vec3 direction, double scale) {
        if (!npc.onGround() || direction.lengthSqr() < 1.0E-4) {
            return;
        }
        Vec3 push = direction.scale(HOP_SPEED * scale);
        npc.setDeltaMovement(push.x, HOP_LIFT, push.z);
        npc.hurtMarked = true;
        if (npc.level() instanceof ServerLevel level) {
            HamonFx.fineMote(level, npc.getX(), npc.getY() + 0.1, npc.getZ(), 6, 0.3);
            HamonFx.ripple(level, npc.getX(), npc.getY() + 0.1, npc.getZ(), 1);
            level.playSound(null, npc.blockPosition(), ModSounds.HAMON_SWOOSH.get(), SoundSource.NEUTRAL, 0.4F, 1.4F);
        }
    }

    private void flee() {
        Vec3 away = away();
        if (away.lengthSqr() < 1.0E-4) {
            return;
        }
        Vec3 goal = npc.position().add(away.scale(RETREAT_DISTANCE));
        npc.getNavigation().moveTo(goal.x, goal.y, goal.z, RETREAT_SPEED);
    }

    private void land(ServerLevel level, Technique technique) {
        switch (technique) {
            case RIPPLE_PULSE -> pulse(level);
            case PALM -> palm(level);
            case ZOOM_PUNCH -> zoom(level);
            default -> {
            }
        }
    }

    private void pulse(ServerLevel level) {
        for (LivingEntity each : foes(PULSE_RANGE)) {
            if (strike(each, PULSE_DAMAGE)) {
                Vec3 push = each.position().subtract(npc.position()).normalize().scale(PULSE_PUSH);
                each.setDeltaMovement(each.getDeltaMovement().add(push.x, 0.35, push.z));
                each.hurtMarked = true;
                Impacts.landed(level, each, push, ImpactSpec.MEDIUM, Impacts.HAMON);
            }
        }
        Vec3 look = npc.getLookAngle();
        double x = npc.getX() + look.x * 1.5;
        double y = npc.getY() + npc.getEyeHeight() * 0.5;
        double z = npc.getZ() + look.z * 1.5;
        HamonFx.ring(level, x, y, z, 2.0, 2);
        HamonFx.ripple(level, x, y, z, 5);
        HamonFx.spark(level, x, y, z, 10, 0.45, 0.12);
        level.playSound(null, npc.blockPosition(), ModSounds.HAMON_ECHO.get(), SoundSource.NEUTRAL, 0.9F, 1.0F);
        HamonFx.sound(level, npc, 0.9F, 1.0F);
    }

    private void palm(ServerLevel level) {
        Vec3 look = npc.getLookAngle();
        Vec3 hand = npc.getEyePosition().add(look.scale(1.0)).add(0, -0.4, 0);
        HamonFx.ring(level, hand.x, hand.y, hand.z, 1.0, 1);
        HamonFx.spark(level, hand.x, hand.y, hand.z, 6, 0.15, 0.08);
        if (target == null || !target.isAlive() || npc.distanceTo(target) > PALM_REACH) {
            level.playSound(null, npc.blockPosition(), ModSounds.HAMON_SWOOSH.get(), SoundSource.NEUTRAL, 0.5F, 1.5F);
            return;
        }
        if (strike(target, PALM_DAMAGE)) {
            Vec3 push = target.position().subtract(npc.position()).normalize().scale(PALM_PUSH);
            target.setDeltaMovement(target.getDeltaMovement().add(push.x, 0.25, push.z));
            target.hurtMarked = true;
            Impacts.landed(level, target, push, ImpactSpec.MEDIUM, Impacts.HAMON);
            HamonFx.sound(level, npc, 0.8F, 1.1F);
        }
    }

    private void zoom(ServerLevel level) {
        Vec3 eye = npc.getEyePosition();
        Vec3 aim = target != null && target.isAlive()
                ? target.getBoundingBox().getCenter().subtract(eye).normalize()
                : npc.getLookAngle();
        LivingEntity struck = null;
        Vec3 at = eye;
        for (double travelled = 1.0; travelled <= ZOOM_REACH && struck == null; travelled += ZOOM_STEP) {
            at = eye.add(aim.scale(travelled));
            BlockPos cell = BlockPos.containing(at);
            if (!level.getBlockState(cell).getCollisionShape(level, cell).isEmpty()) {
                break;
            }
            AABB probe = new AABB(at, at).inflate(ZOOM_HIT_RADIUS);
            for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, probe,
                    each -> each != npc && each.isAlive() && !JojoNpc.allies(npc, each))) {
                struck = candidate;
                break;
            }
        }
        for (double travelled = 1.0; travelled <= eye.distanceTo(at); travelled += ZOOM_STEP) {
            Vec3 point = eye.add(aim.scale(travelled));
            HamonFx.spark(level, point.x, point.y, point.z, 1, 0.04, 0.0);
        }
        HamonFx.ring(level, at.x, at.y, at.z, 1.5, struck == null ? 0 : 2);
        HamonFx.ripple(level, at.x, at.y, at.z, struck == null ? 1 : 4);
        NetworkHandler.sendZoomPunch(level, npc, at, ZOOM_ARM_TICKS);
        if (struck == null) {
            level.playSound(null, npc.blockPosition(), ModSounds.HAMON_SWOOSH.get(), SoundSource.NEUTRAL, 0.5F, 1.5F);
            return;
        }
        if (strike(struck, ZOOM_DAMAGE)) {
            Vec3 pull = npc.position().subtract(struck.position()).normalize().scale(ZOOM_PULL);
            struck.setDeltaMovement(struck.getDeltaMovement().add(pull.x, 0.1, pull.z));
            struck.hurtMarked = true;
            Impacts.landed(level, struck, aim, ImpactSpec.HEAVY, Impacts.HAMON);
        }
        HamonFx.sound(level, npc, 0.8F, 1.15F);
    }

    private List<LivingEntity> foes(double range) {
        return npc.level().getEntitiesOfClass(LivingEntity.class, npc.getBoundingBox().inflate(range),
                each -> each != npc && each.isAlive() && !JojoNpc.allies(npc, each) && npc.distanceTo(each) <= range
                        && (each == npc.getTarget() || each instanceof Mob mob && mob.getTarget() == npc
                        || each instanceof Enemy && !(each instanceof Creeper) && !(each instanceof Player)));
    }

    private boolean strike(LivingEntity victim, float base) {
        float amount = base + (HamonCombat.isUndead(victim) ? UNDEAD_BONUS : 0F);
        boolean landed = victim.hurt(npc.damageSources().mobAttack(npc),
                DamageScale.scale(DamageScale.Kind.HAMON, npc, victim, amount));
        if (landed) {
            npc.setLastHurtMob(victim);
        }
        return landed;
    }

    static void meleeFx(ServerLevel level, JojoNpc npc, LivingEntity victim, boolean undead) {
        Vec3 at = victim.getBoundingBox().getCenter();
        Vec3 direction = victim.position().subtract(npc.position()).normalize();
        HamonFx.spark(level, at.x, at.y, at.z, undead ? 10 : 4, 0.25, 0.12);
        if (undead) {
            HamonFx.ripple(level, at.x, at.y, at.z, 2);
            Impacts.landed(level, victim, direction, ImpactSpec.LIGHT, Impacts.HAMON);
            HamonFx.sound(level, npc, 0.7F, 1.1F);
        }
    }
}
