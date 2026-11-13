package org.gumel.jojoha.stand.mob;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.combat.BarrageBlow;
import org.gumel.jojoha.combat.DamageScale;
import org.gumel.jojoha.combat.StandPierce;
import org.gumel.jojoha.combat.Stun;
import org.gumel.jojoha.combat.fx.ImpactSpec;
import org.gumel.jojoha.combat.fx.Impacts;
import org.gumel.jojoha.config.JojohaServerConfig;
import org.gumel.jojoha.registry.ModSounds;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.StandRange;
import org.gumel.jojoha.stand.StandShadowForm;
import org.gumel.jojoha.stand.hierophant.EmeraldSplash;

import java.util.Map;
import java.util.WeakHashMap;

public final class MobStandBrain {
    private static final double ENGAGE = 6.0;
    private static final double STAND_OFF = 0.9;
    private static final double LEASH = 4.0;

    private static final double MELEE_REACH = 1.6;

    private static final double GLIDE = 0.35;

    public static final float PUNCH_DAMAGE = 3F;
    private static final int PUNCH_EVERY = 24;
    private static final int PUNCH_LANDS_AFTER = 4;

    public static final int BARRAGE_TICKS = 40;
    private static final int BARRAGE_EVERY = 4;
    public static final float BARRAGE_DAMAGE = 1F;
    private static final int BARRAGE_REST = 200;

    private static final double SPLASH_NEAREST = 5.0;
    private static final double SPLASH_FARTHEST = 18.0;
    private static final int SPLASH_SHARDS = 3;
    public static final float SPLASH_DAMAGE = 3F;
    private static final int SPLASH_EVERY = 50;

    private static final int TELL_TICKS = 10;
    private static final int TELL_SPARK_EVERY = 2;
    private static final float TELL_VOLUME = 1.0F;
    private static final float TELL_PITCH = 0.7F;

    private enum Tell {
        NONE, BARRAGE, SPLASH
    }

    private static final Map<StandEntity, State> STATES = new WeakHashMap<>();

    private MobStandBrain() {
    }

    private static final class State {
        int punchCooldown = PUNCH_EVERY / 2;
        int punchLands = -1;
        boolean secondPunch;
        int barrageLeft;
        int barrageCooldown = BARRAGE_REST / 2;
        int splashCooldown = SPLASH_EVERY / 2;
        Tell tell = Tell.NONE;
        int tellLeft;
    }

    public static Vec3 shoulder(Mob mob) {
        float yaw = mob.yBodyRot * Mth.DEG_TO_RAD;
        Vec3 forward = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
        Vec3 right = new Vec3(-forward.z, 0, forward.x);
        return mob.position().add(forward.scale(-0.6)).add(right.scale(0.6)).add(0, mob.getBbHeight() * 0.15, 0);
    }

    public static void tick(StandEntity stand, Mob mob) {
        if (!(stand.level() instanceof ServerLevel level)) {
            return;
        }
        State state = STATES.computeIfAbsent(stand, s -> new State());
        LivingEntity foe = MobStands.foe(mob);
        boolean longRange = stand.getStandType().range() == StandRange.LONG;

        Vec3 anchor = shoulder(mob);
        float facing = mob.yBodyRot;
        if (foe != null) {
            double gap = mob.distanceTo(foe);
            if (!longRange && gap <= ENGAGE) {
                anchor = besideFoe(mob, foe);
            }
            Vec3 toFoe = foe.position().subtract(stand.position());
            facing = (float) (Mth.atan2(toFoe.z, toFoe.x) * Mth.RAD_TO_DEG) - 90F;
        } else {
            state.tell = Tell.NONE;
            if (state.barrageLeft > 0) {
                endBarrage(stand, state);
            }
        }

        if (state.tell == Tell.NONE) {
            Vec3 next = stand.position().lerp(anchor, GLIDE);
            stand.setPos(next.x, next.y, next.z);
        }
        stand.setYRot(facing);
        stand.setYBodyRot(facing);
        stand.setYHeadRot(facing);

        if (foe != null) {
            fight(level, stand, mob, foe, state, longRange);
        }
    }

    private static Vec3 besideFoe(Mob mob, LivingEntity foe) {
        Vec3 back = mob.position().subtract(foe.position());
        Vec3 flat = new Vec3(back.x, 0, back.z);
        Vec3 away = flat.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : flat.normalize();
        Vec3 spot = foe.position().add(away.scale(foe.getBbWidth() * 0.5 + STAND_OFF)).add(0, 0.2, 0);
        Vec3 fromMob = spot.subtract(mob.position());
        return fromMob.length() > LEASH ? mob.position().add(fromMob.normalize().scale(LEASH)) : spot;
    }

    private static void fight(ServerLevel level, StandEntity stand, Mob mob, LivingEntity foe, State state,
                              boolean longRange) {
        state.punchCooldown--;
        state.barrageCooldown--;
        state.splashCooldown--;
        boolean inReach = inReach(stand, foe);

        if (state.tell != Tell.NONE) {
            telling(level, stand, mob, foe, state);
            return;
        }

        if (state.barrageLeft > 0) {
            state.barrageLeft--;
            if (inReach && state.barrageLeft % BARRAGE_EVERY == 0) {
                foe.invulnerableTime = 0;
                BarrageBlow.hurt(foe, level.damageSources().mobAttack(mob), DamageScale.mobStand(mob, foe, BARRAGE_DAMAGE));
                foe.invulnerableTime = 0;
                Impacts.landed(level, foe, foe.position().subtract(stand.position()), ImpactSpec.FLURRY,
                        StandShadowForm.auraColourFor(stand));
            }
            if (state.barrageLeft == 0) {
                if (inReach && JojohaServerConfig.MOB_STATUSES.on()) {
                    Stun.stagger(foe);
                }
                endBarrage(stand, state);
            }
            return;
        }

        if (state.punchLands >= 0 && --state.punchLands < 0) {
            if (inReach) {
                StandPierce.hurt(foe, level.damageSources().mobAttack(mob), DamageScale.mobStand(mob, foe, PUNCH_DAMAGE));
                Impacts.landed(level, foe, foe.position().subtract(stand.position()), ImpactSpec.LIGHT,
                        StandShadowForm.auraColourFor(stand));
            }
        }

        if (longRange && !inReach) {
            double gap = mob.distanceTo(foe);
            if (state.splashCooldown <= 0 && gap >= SPLASH_NEAREST && gap <= SPLASH_FARTHEST
                    && mob.hasLineOfSight(foe)) {
                tell(level, stand, state, Tell.SPLASH);
                state.splashCooldown = SPLASH_EVERY;
            }
            return;
        }

        if (!inReach) {
            return;
        }
        if (!longRange && state.barrageCooldown <= 0) {
            state.barrageCooldown = BARRAGE_REST;
            tell(level, stand, state, Tell.BARRAGE);
            return;
        }
        if (state.punchCooldown <= 0 && state.punchLands < 0) {
            stand.triggerAnim(StandEntity.CONTROLLER, state.secondPunch ? "punch2" : "punch");
            state.secondPunch = !state.secondPunch;
            state.punchLands = PUNCH_LANDS_AFTER;
            state.punchCooldown = PUNCH_EVERY;
        }
    }

    private static void tell(ServerLevel level, StandEntity stand, State state, Tell tell) {
        state.tell = tell;
        state.tellLeft = TELL_TICKS;
        Vec3 fist = fist(stand);
        level.sendParticles(ParticleTypes.FLASH, fist.x, fist.y, fist.z, 1, 0.0, 0.0, 0.0, 0.0);
        level.playSound(null, fist.x, fist.y, fist.z, ModSounds.MOVE_CAST.get(), SoundSource.HOSTILE,
                TELL_VOLUME, TELL_PITCH);
    }

    private static void telling(ServerLevel level, StandEntity stand, Mob mob, LivingEntity foe, State state) {
        if (state.tellLeft % TELL_SPARK_EVERY == 0) {
            Vec3 fist = fist(stand);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, fist.x, fist.y, fist.z, 3, 0.1, 0.1, 0.1, 0.05);
        }
        if (--state.tellLeft > 0) {
            return;
        }
        Tell tell = state.tell;
        state.tell = Tell.NONE;
        if (tell == Tell.BARRAGE) {
            state.barrageLeft = BARRAGE_TICKS;
            stand.setMobBarraging(true);
            stand.triggerAnim(StandEntity.CONTROLLER, "barrage");
        } else if (tell == Tell.SPLASH && mob.hasLineOfSight(foe)) {
            splash(level, stand, mob, foe);
        }
    }

    private static Vec3 fist(StandEntity stand) {
        float yaw = stand.getYRot() * Mth.DEG_TO_RAD;
        Vec3 forward = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
        return stand.position().add(0, stand.getBbHeight() * 0.6, 0).add(forward.scale(0.6));
    }

    private static void endBarrage(StandEntity stand, State state) {
        state.barrageLeft = 0;
        stand.setMobBarraging(false);
        stand.stopTriggeredAnim(StandEntity.CONTROLLER, "barrage");
    }

    private static boolean inReach(StandEntity stand, LivingEntity foe) {
        Vec3 chest = stand.position().add(0, stand.getBbHeight() * 0.5, 0);
        AABB reach = foe.getBoundingBox().inflate(MELEE_REACH);
        return reach.contains(chest);
    }

    private static void splash(ServerLevel level, StandEntity stand, Mob mob, LivingEntity foe) {
        Vec3 from = stand.position().add(0, stand.getBbHeight() * 0.6, 0);
        Vec3 to = foe.getBoundingBox().getCenter();
        Vec3 heading = to.subtract(from).normalize();
        stand.triggerAnim(StandEntity.CONTROLLER, "punch");
        for (int i = 0; i < SPLASH_SHARDS; i++) {
            EmeraldSplash.fire(level, mob, from, heading, 2.4F, 3.0F,
                    SPLASH_DAMAGE, false);
        }
        level.playSound(null, stand.blockPosition(), SoundEvents.SLIME_BLOCK_PLACE, SoundSource.HOSTILE, 0.8F, 1.6F);
    }
}
