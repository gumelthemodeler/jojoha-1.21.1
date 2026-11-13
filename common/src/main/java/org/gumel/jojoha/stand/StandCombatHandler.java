package org.gumel.jojoha.stand;

import dev.architectury.event.EventResult;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.accessory.Relics;
import org.gumel.jojoha.combat.ComboDecay;
import org.gumel.jojoha.combat.ComboLink;
import org.gumel.jojoha.combat.BarragePressure;
import org.gumel.jojoha.combat.Bystander;
import org.gumel.jojoha.combat.DamageScale;
import org.gumel.jojoha.combat.Defence;
import org.gumel.jojoha.combat.EnergySystem;
import org.gumel.jojoha.combat.Exposed;
import org.gumel.jojoha.combat.GuardBreak;
import org.gumel.jojoha.combat.Hitbox;
import org.gumel.jojoha.combat.M1String;
import org.gumel.jojoha.combat.OwnPets;
import org.gumel.jojoha.combat.Parry;
import org.gumel.jojoha.combat.StandHits;
import org.gumel.jojoha.combat.StandPierce;
import org.gumel.jojoha.combat.StatPerkEffects;
import org.gumel.jojoha.combat.fx.ImpactSpec;
import org.gumel.jojoha.combat.fx.Impacts;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.stand.skill.StandBeat;
import org.gumel.jojoha.data.StandGrowth;
import org.gumel.jojoha.data.StandTuning;
import org.gumel.jojoha.data.StatEffects;
import org.gumel.jojoha.data.StatPerk;
import org.gumel.jojoha.registry.ModSounds;
import org.gumel.jojoha.stand.heaven.Heaven;
import org.gumel.jojoha.stand.passive.StandPassives;
import org.gumel.jojoha.stand.skill.TimeStopCast;
import org.gumel.jojoha.stand.skill.TimeStopSystem;

import java.util.List;

public final class StandCombatHandler {
    public static final double PUNCH_RANGE = 3.5;
    private static final float PUNCH_DAMAGE = 4.0F;

    public static final double PUNCH_LANE_HALF_WIDTH = 0.4;

    private static final double ASSIST_FACING = Math.cos(Math.toRadians(20.0));

    private static final double AIMED_LAG_SLACK = 1.0;
    private static final double PUNCH_KNOCKBACK = 0.6;
    private static final int PUNCH_COOLDOWN_TICKS = 12;
    private static final float PUNCH_FX_FULL_AT = 8F;
    private static final float PUNCH_FX_LEAST = 0.35F;
    private static final double PURSUIT_LOOK_RANGE = 7.0;

    private static final int MODE_LABEL_COLOR = 0xC8C8C8;
    private static final int MODE_ATTACK_COLOR = 0xFF5555;
    private static final int MODE_DEFENSE_COLOR = 0x5599FF;
    private static final int MODE_UTILITY_COLOR = 0x77DD67;

    public static final int COMBAT_TIMER_TICKS = 100;
    static final int GUARD_BREAK_LOCKOUT_TICKS = 100;
    private static final float BLOCK_ENERGY_RESTORE = 5F;
    private static final float NON_STAND_HIT_ENERGY_RESTORE = 1.5F;

    private StandCombatHandler() {
    }

    private static final double GRAB_PULL_SPEED = 1.15;
    private static final double GRAB_PULL_LIFT = 0.35;
    private static final float GRAB_DAMAGE = 2.0F;

    private static final double THROW_SPEED = 1.4;
    private static final double THROW_LIFT = 0.25;

    private static void grabAndHold(ServerPlayer player, JojohaPlayerData data,
                                    StandEntity stand, LivingEntity target) {
        data.standLeapGrabTicks = 0;
        stand.triggerGrabAt(target);
        StandBeat.after(StandEntity.STRIKE_CONTACT_TICKS, player, target, StandCombatHandler::snatch);
    }

    private static void snatch(ServerPlayer player, LivingEntity target) {
        StandEntity stand = StandEntity.findFor(player);
        if (stand == null) {
            return;
        }
        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (!stand.seize(target)) {
            Vec3 toPlayer = player.position().subtract(target.position());
            double distance = toPlayer.length();
            if (distance > 1.0E-4) {
                Vec3 pull = toPlayer.scale(1.0 / distance).scale(GRAB_PULL_SPEED);
                target.setDeltaMovement(pull.x, GRAB_PULL_LIFT, pull.z);
                target.hurtMarked = true;
                target.fallDistance = 0F;
            }
        }

        data.lastDamageWasStandAttack = true;
        StandPierce.hurt(target, player.serverLevel().damageSources().playerAttack(player),
                DamageScale.stand(player, target, StandPassives.scaleOutgoing(player, data, target,
                        StandTuning.damage("grab", GRAB_DAMAGE) * data.stand.powerScale())));
        data.lastDamageWasStandAttack = false;

        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG,
                SoundSource.PLAYERS, 0.9F, 0.8F);
        data.combatTicks = COMBAT_TIMER_TICKS;
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
    }

    private static void throwHeld(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        LivingEntity thrown = stand.heldEntity();
        Vec3 velocity = player.getLookAngle().scale(THROW_SPEED).add(0, THROW_LIFT, 0);
        stand.releaseHeld(velocity);
        if (thrown != null) {
            ThrownBodies.launch(thrown, velocity);
        }

        if (thrown != null) {
            data.lastDamageWasStandAttack = true;
            StandPierce.hurt(thrown, player.serverLevel().damageSources().playerAttack(player),
                    DamageScale.stand(player, thrown, StandPassives.scaleOutgoing(player, data, thrown,
                            StandTuning.damage("grab", GRAB_DAMAGE) * data.stand.powerScale())));
            data.lastDamageWasStandAttack = false;
        }

        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG,
                SoundSource.PLAYERS, 1.0F, 0.7F);
        data.combatTicks = COMBAT_TIMER_TICKS;
    }

    private static LivingEntity aimedTarget(ServerPlayer player, ServerLevel level, int aimedId) {
        if (aimedId < 0 || !(level.getEntity(aimedId) instanceof LivingEntity body) || body == player
                || !body.isAlive() || body instanceof StandEntity
                || !StandHits.hittable(player, body)
                || OwnPets.tamedBy(player, body)) {
            return null;
        }
        double reach = PUNCH_RANGE + AIMED_LAG_SLACK;
        if (body.getBoundingBox().distanceToSqr(player.getEyePosition()) > reach * reach
                || !player.hasLineOfSight(body)) {
            return null;
        }
        return body;
    }

    private static boolean hasReachableTarget(ServerPlayer player, ServerLevel level,
                                              LivingEntity lookTarget) {
        return lookTarget != null || punchTarget(player, level) != null;
    }

    private static LivingEntity punchTarget(ServerPlayer player, ServerLevel level) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        double wall = wallAt(player, level, eye, look, PUNCH_RANGE);
        LivingEntity nearest = null;
        double nearestAt = Double.MAX_VALUE;
        for (LivingEntity body : withinPunchRange(player, level)) {
            double lane = Bystander.innocent(player, body) ? 0.0 : PUNCH_LANE_HALF_WIDTH;
            double at = Hitbox.distanceAlong(eye, look, PUNCH_RANGE, lane, body);
            if (at < 0 || at > wall || at >= nearestAt) {
                continue;
            }
            nearest = body;
            nearestAt = at;
        }
        return nearest != null ? nearest : assisted(player, eye, look, wall, level);
    }

    private static LivingEntity assisted(ServerPlayer player, Vec3 eye, Vec3 look, double wall, ServerLevel level) {
        LivingEntity best = null;
        double bestFacing = ASSIST_FACING;
        for (LivingEntity body : withinPunchRange(player, level)) {
            if (!(body instanceof Enemy) || !player.hasLineOfSight(body)) {
                continue;
            }
            Vec3 toward = body.getBoundingBox().getCenter().subtract(eye);
            double distance = toward.length();
            if (distance < 1.0E-4 || distance > wall + body.getBbWidth()) {
                continue;
            }
            double facing = toward.scale(1.0 / distance).dot(look);
            if (facing > bestFacing) {
                best = body;
                bestFacing = facing;
            }
        }
        return best;
    }

    private static double wallAt(ServerPlayer player, ServerLevel level, Vec3 eye, Vec3 look, double reach) {
        BlockHitResult hit = level.clip(new ClipContext(eye,
                eye.add(look.scale(reach)), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.MISS
                ? reach + 1.0 : hit.getLocation().distanceTo(eye);
    }

    private static List<LivingEntity> withinPunchRange(ServerPlayer player, ServerLevel level) {
        AABB area = player.getBoundingBox().inflate(PUNCH_RANGE);
        return level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != player && StandHits.hittable(player, entity) && entity.isAlive()
                        && !OwnPets.tamedBy(player, entity)
                        && player.distanceTo(entity) <= PUNCH_RANGE);
    }

    public static void handlePunchRequest(ServerPlayer player, int aimedId) {
        if (BarrageStruggle.mash(player)) {
            return;
        }
        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (!data.standSummoned || !data.stand.isPresent() || !data.manifestTier().canStandPunch()) {
            return;
        }
        if (!StandTypes.hasBody(data)) {
            return;
        }

        if (data.standMode.handlesItems()) {
            StandEntity carrier = findStand(player, data);
            if (carrier != null && carrier.isCarrying()) {
                LivingEntity carried = carrier.heldEntity();
                Vec3 toss = player.getLookAngle().scale(THROW_SPEED).add(0, THROW_LIFT, 0);
                carrier.releaseHeld(toss);
                if (carried != null) {
                    ThrownBodies.launch(carried, toss);
                }
                player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                        SoundSource.PLAYERS, 0.8F, 0.8F);
            }
            return;
        }

        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        StandEntity stand = findStand(player, data);

        if (now < data.punchCooldownExpiry) {
            return;
        }

        if (stand != null && stand.isHolding()) {
            data.punchCooldownExpiry = now + DividedAttention.punchTicks(data, Heaven.punchTicks(data, PUNCH_COOLDOWN_TICKS));
            if (player.isShiftKeyDown()) {
                stand.releaseHeld(Vec3.ZERO);
                PlayerDataAccess.set(player, data);
                PlayerDataAccess.sync(player);
                return;
            }
            throwHeld(player, data, stand);
            PlayerDataAccess.set(player, data);
            PlayerDataAccess.sync(player);
            return;
        }

        if (stand != null && data.standPiloting && stand.isPiloted()) {
            pilotPunch(player, data, stand, now);
            return;
        }

        LivingEntity aimed = aimedTarget(player, level, aimedId);
        LivingEntity lookTarget = aimed != null ? aimed : findLookTarget(player, PURSUIT_LOOK_RANGE);

        if (!hasReachableTarget(player, level, lookTarget)) {
            if (stand != null) {
                data.punchCooldownExpiry = now + punchGap(data, M1String.swing(player));
                boolean second = data.nextPunchIsPunch2;
                data.nextPunchIsPunch2 = !second;
                stand.punch(second);
                PlayerDataAccess.set(player, data);
                PlayerDataAccess.sync(player);
            }
            return;
        }

        int at = M1String.swing(player);
        data.punchCooldownExpiry = now + punchGap(data, at);

        boolean usePunch2 = data.nextPunchIsPunch2;
        data.nextPunchIsPunch2 = !usePunch2;

        if (stand != null && aimed == null && lookTarget != null && data.standLeapGrabTicks > 0
                && player.distanceTo(lookTarget) > PUNCH_RANGE) {
            grabAndHold(player, data, stand, lookTarget);
            PlayerDataAccess.set(player, data);
            PlayerDataAccess.sync(player);
            return;
        }

        LivingEntity target = aimed != null ? aimed : punchTarget(player, level);
        boolean ender = M1String.ender(at);
        float punch = StandTuning.damage("punch", PUNCH_DAMAGE) * data.stand.powerScale()
                * (ender ? M1String.ENDER_DAMAGE : 1F);
        Runnable contact = target == null ? null : () -> strikeOnContact(player, stand, target, punch, PUNCH_RANGE, ender);
        if (stand != null) {
            stand.punch(usePunch2, contact);
        } else if (contact != null) {
            contact.run();
        }

        data.combatTicks = COMBAT_TIMER_TICKS;
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
    }

    private static final double PILOT_PUNCH_REACH = 4.5;

    private static final double CONTACT_SLACK = 1.0;

    private static int punchGap(JojohaPlayerData data, int at) {
        int ticks = DividedAttention.punchTicks(data, Heaven.punchTicks(data, PUNCH_COOLDOWN_TICKS));
        return M1String.gap(at, ticks, PUNCH_COOLDOWN_TICKS);
    }

    private static void pilotPunch(ServerPlayer player, JojohaPlayerData data, StandEntity stand, long now) {
        int at = M1String.swing(player);
        boolean ender = M1String.ender(at);
        data.punchCooldownExpiry = now + punchGap(data, at);
        boolean usePunch2 = data.nextPunchIsPunch2;
        data.nextPunchIsPunch2 = !usePunch2;

        LivingEntity target = StandAim.lookTarget(player, PILOT_PUNCH_REACH, true);
        if (target != null && target.distanceTo(stand) > PILOT_PUNCH_REACH + target.getBbWidth()) {
            target = null;
        }

        float punch = StandTuning.damage("punch", PUNCH_DAMAGE) * data.stand.powerScale()
                * (ender ? M1String.ENDER_DAMAGE : 1F);
        LivingEntity struck = target;
        stand.punch(usePunch2, struck == null ? null
                : () -> strikeOnContact(player, stand, struck, punch, PILOT_PUNCH_REACH, ender));
        data.combatTicks = COMBAT_TIMER_TICKS;
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
    }

    private static void strikeOnContact(ServerPlayer player, StandEntity stand, LivingEntity target, float punch,
                                        double reach, boolean ender) {
        if (!player.isAlive() || !target.isAlive() || target.isRemoved()) {
            return;
        }
        Vec3 from = stand != null && !stand.isRemoved() ? stand.position() : player.position();
        double slack = reach + CONTACT_SLACK + target.getBbWidth();
        if (from.distanceTo(target.position()) > slack && player.distanceTo(target) > slack) {
            return;
        }
        if (StandClash.contested(StandHits.bodyOf(target))) {
            StandClash.swing(player, StandHits.bodyOf(target), () -> landPunch(player, target, punch, from, ender));
        } else {
            landPunch(player, target, punch, from, ender);
        }
    }

    private static void landPunch(ServerPlayer player, LivingEntity target, float punch) {
        landPunch(player, target, punch, player.position(), false);
    }

    private static void landPunch(ServerPlayer player, LivingEntity target, float punch, Vec3 from, boolean ender) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        data.lastDamageWasStandAttack = true;
        float dealt = DamageScale.stand(player, target, StandPassives.scaleOutgoing(player, data, StandHits.bodyOf(target), punch));
        boolean landed = StandPierce.hurt(target, player.serverLevel().damageSources().playerAttack(player), dealt);
        data.lastDamageWasStandAttack = false;
        Relics.standPunchLanded(player, target);
        if (landed) {
            Impacts.landed(player.serverLevel(), target, target.position().subtract(from),
                    (ender ? ImpactSpec.HEAVY : ImpactSpec.MEDIUM).scaledBy(
                            Mth.clamp(dealt / PUNCH_FX_FULL_AT, PUNCH_FX_LEAST, 1F)),
                    Impacts.colourOf(player));
            ComboLink.landed(player);
            if (ender) {
                ComboDecay.reset(StandHits.bodyOf(target));
            }
        }
        Vec3 push = target.position().subtract(from).normalize().scale(ender ? M1String.ENDER_KNOCKBACK : PUNCH_KNOCKBACK);
        target.setDeltaMovement(target.getDeltaMovement().add(push.x, ender ? M1String.ENDER_LIFT : 0.2, push.z));
        target.hurtMarked = true;
    }

    private static LivingEntity findLookTarget(ServerPlayer player, double maxDistance) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        double reach = Math.min(maxDistance, wallAt(player, player.serverLevel(), eyePos, look, maxDistance));
        Vec3 endVec = eyePos.add(look.scale(reach));
        AABB searchBox = player.getBoundingBox().expandTowards(look.scale(reach)).inflate(1.0);

        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player.level(), player, eyePos, endVec, searchBox,
                entity -> entity instanceof LivingEntity livingEntity && livingEntity.isAlive()
                        && entity != player && !(entity instanceof StandEntity)
                        && !OwnPets.tamedBy(player, entity)
                        && !Bystander.innocent(player, livingEntity));

        return hit != null ? (LivingEntity) hit.getEntity() : null;
    }

    public static void handleEngageTarget(ServerPlayer player, int targetId) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (!data.standSummoned || !data.stand.isPresent() || !data.stand.trust().canActAtRange()) {
            return;
        }

        StandEntity stand = findStand(player, data);
        if (stand == null) {
            return;
        }

        Entity target = player.serverLevel().getEntity(targetId);
        if (!(target instanceof LivingEntity living) || !living.isAlive() || living == player
                || living instanceof StandEntity || player.distanceTo(living) > PURSUIT_LOOK_RANGE) {
            return;
        }

        stand.pursueAndPunch(living, data.nextPunchIsPunch2);
        data.nextPunchIsPunch2 = !data.nextPunchIsPunch2;
        data.combatTicks = COMBAT_TIMER_TICKS;

        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
    }

    public static void handleCycleMode(ServerPlayer player) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (!StandTypes.hasBody(data)) {
            return;
        }

        data.standMode = data.standMode.next();

        StandEntity stand = findStand(player, data);
        if (stand != null) {
            stand.setMode(data.standMode);
        }

        StandMode mode = data.standMode;
        player.displayClientMessage(Component.translatable("hud.jojoha.stand_mode")
                .withStyle(style -> style.withColor(MODE_LABEL_COLOR))
                .append(Component.translatable(mode.translationKey())
                        .withStyle(style -> style.withColor(modeColor(mode)))),
                true);

        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
    }

    private static int modeColor(StandMode mode) {
        return switch (mode) {
            case DEFENSE -> MODE_DEFENSE_COLOR;
            case UTILITY -> MODE_UTILITY_COLOR;
            default -> MODE_ATTACK_COLOR;
        };
    }

    public static void handleSetGuard(ServerPlayer player, boolean wantsGuarding) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        boolean allowed = wantsGuarding && data.standSummoned && data.stand.isPresent()
                && data.stand.trust().canGuard() && data.guardBreakCooldownTicks <= 0
                && !GuardBreak.isBroken(player)
                && !Exposed.isOpen(player)
                && data.standEnergy >= BLOCK_ENERGY_LEAST
                && (player.getMainHandItem().isEmpty()
                        || BarragePressure.on(player))
                && !data.standMode.handlesItems()
                && StandTypes.hasBody(data);

        if (data.standGuarding == allowed) {
            return;
        }
        data.standGuarding = allowed;

        StandEntity stand = findStand(player, data);
        if (stand != null) {
            if (allowed) {
                stand.startGuarding();
            } else {
                stand.stopGuarding();
            }
        }

        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
        if (allowed) {
            Parry.duel(player);
        }
    }

    public static EventResult handleLivingHurt(LivingEntity hurt, DamageSource source, float amount) {
        if (hurt.level().isClientSide()) {
            return EventResult.pass();
        }

        if (TimeStopSystem.holdDamage(hurt, source, amount)) {
            return EventResult.interruptFalse();
        }

        EventResult result = EventResult.pass();

        if (hurt instanceof ServerPlayer hurtPlayer) {
            JojohaPlayerData data = PlayerDataAccess.get(hurtPlayer);
            data.combatTicks = COMBAT_TIMER_TICKS;

            TimeStopCast.interrupt(hurtPlayer);

            if (data.inhaleIFrameTicks > 0 && source.getDirectEntity() != null) {
                PlayerDataAccess.set(hurtPlayer, data);
                PlayerDataAccess.sync(hurtPlayer);
                return EventResult.interruptFalse();
            }

            if (data.standGuarding && Defence.guardable(source)) {
                StandEntity guard = findStand(hurtPlayer, data);
                if (guard != null && source.getEntity() instanceof LivingEntity attacker) {
                    guard.onGuardHit(attacker, hurtPlayer);
                }

                Defence.Facing facing =
                        Defence.facing(hurtPlayer, source.getEntity());
                if (facing != Defence.Facing.BACK) {
                    if (!payForBlock(data, amount)) {
                        breakGuard(hurtPlayer, data, guard);
                    } else if (amount >= standBreakAt(data)) {
                        breakGuard(hurtPlayer, data, guard);
                        if (standBlow(source)) {
                            result = EventResult.interruptFalse();
                        }
                    } else {
                        onBlockedHit(hurtPlayer, data,
                                amount >= Defence.HEAVY_BREAK_AT ? HEAVY_BLOW_HITS : 1);
                        if (data.halfSummon) {
                            EnergySystem.restoreStand(data,
                                    data.maxStandEnergy() * HALF_BLOCK_REFUND_SHARE);
                        }
                        Relics.guardBlow(hurtPlayer);
                        if (facing == Defence.Facing.FRONT || standBlow(source)) {
                            result = EventResult.interruptFalse();
                        }
                    }
                }
            }

            PlayerDataAccess.set(hurtPlayer, data);
            PlayerDataAccess.sync(hurtPlayer);
        }

        if (source.getEntity() instanceof ServerPlayer attacker) {
            JojohaPlayerData data = PlayerDataAccess.get(attacker);
            data.combatTicks = COMBAT_TIMER_TICKS;
            if (data.standSummoned && !data.lastDamageWasStandAttack) {
                EnergySystem.restoreStand(data, NON_STAND_HIT_ENERGY_RESTORE);
            }
            PlayerDataAccess.set(attacker, data);
            PlayerDataAccess.syncSoon(attacker);
        }

        return result;
    }

    public static final float BLOCK_ENERGY_PER_DAMAGE = 1.5F;
    public static final float HALF_BLOCK_REFUND_SHARE = 0.03F;
    public static final float BLOCK_ENERGY_LEAST = 0.5F;

    private static boolean payForBlock(JojohaPlayerData data, float amount) {
        float cost = Math.max(BLOCK_ENERGY_LEAST, amount * BLOCK_ENERGY_PER_DAMAGE)
                * (StatPerk.BRACED.has(data)
                        ? StatPerkEffects.BRACED_DRAIN_SCALE : 1F);
        if (data.standEnergy < cost) {
            data.standEnergy = 0F;
            return false;
        }
        data.standEnergy -= cost;
        return true;
    }

    public static boolean standBlow(DamageSource source) {
        if (!(source.getEntity() instanceof ServerPlayer attacker) || source.getDirectEntity() != attacker) {
            return false;
        }
        JojohaPlayerData data = PlayerDataAccess.get(attacker);
        return data.standSummoned && data.stand.isPresent()
                && (attacker.getMainHandItem().isEmpty() || data.lastDamageWasStandAttack);
    }

    public static void restoreFromParry(JojohaPlayerData data) {
        EnergySystem.restoreStand(data, BLOCK_ENERGY_RESTORE
                * (StatPerk.REBUKE.has(data)
                        ? StatPerkEffects.REBUKE_RESTORE_SCALE : 1F));
    }

    private static float standBreakAt(JojohaPlayerData data) {
        return StatPerk.IMMOVABLE.has(data)
                ? STAND_BREAK_AT * StatPerkEffects.IMMOVABLE_BREAK_SCALE
                : STAND_BREAK_AT;
    }

    private static final float STAND_BREAK_AT = 14.0F;
    private static final int HEAVY_BLOW_HITS = 3;

    private static final int GUARD_RECOVER_DELAY_TICKS = 40;
    private static final int GUARD_RECOVER_STEP_TICKS = 15;

    public static void tickGuardRecovery(ServerPlayer player, JojohaPlayerData data) {
        if (data.blockedHitsSinceBreak <= 0 || --data.guardRecoverTicks > 0) {
            return;
        }
        data.blockedHitsSinceBreak--;
        data.guardRecoverTicks = GUARD_RECOVER_STEP_TICKS;
        StandEntity guard = findStand(player, data);
        if (guard != null) {
            guard.setGuardStrain(data.blockedHitsSinceBreak
                    / (float) StatEffects.guardHits(data.stand.protection()));
        }
    }

    private static void onBlockedHit(ServerPlayer player, JojohaPlayerData data) {
        onBlockedHit(player, data, 1);
    }

    private static void onBlockedHit(ServerPlayer player, JojohaPlayerData data, int hits) {
        data.blockedHitsSinceBreak += hits;
        data.guardRecoverTicks = GUARD_RECOVER_DELAY_TICKS;
        StandGrowth.blockedHit(player);

        StandEntity guard = findStand(player, data);
        if (guard != null) {
            guard.setGuardStrain(data.blockedHitsSinceBreak
                    / (float) StatEffects.guardHits(data.stand.protection()));
        }

        if (data.blockedHitsSinceBreak >= StatEffects.guardHits(
                data.stand.protection())) {
            breakGuard(player, data, guard);
        }
    }

    public static void breakStandGuard(ServerPlayer player) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (!data.standGuarding) {
            return;
        }
        breakGuard(player, data, findStand(player, data));
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
    }

    private static void breakGuard(ServerPlayer player, JojohaPlayerData data, StandEntity guard) {
        data.blockedHitsSinceBreak = 0;
        data.guardBreakCooldownTicks = StatPerk.BULWARK.has(data)
                ? Math.round(GUARD_BREAK_LOCKOUT_TICKS * StatPerkEffects.BULWARK_LOCKOUT_SCALE)
                : GUARD_BREAK_LOCKOUT_TICKS;
        data.standGuarding = false;

        if (guard != null) {
            guard.triggerGuardBroken();
        } else {
            player.serverLevel().playSound(null, player.blockPosition(), ModSounds.GUARD_BREAK.get(),
                    SoundSource.PLAYERS, 1.0F, 1.0F);
        }

        Impacts.landed(player.serverLevel(), player, new Vec3(0, 1, 0),
                ImpactSpec.GUARD_BREAK.silent(),
                StandShadowForm.auraColourFor(data.stand, player.getUUID()));
    }

    private static StandEntity findStand(ServerPlayer player, JojohaPlayerData data) {
        if (data.summonedStandEntityUuid == null) {
            return null;
        }
        Entity entity = player.serverLevel().getEntity(data.summonedStandEntityUuid);
        return entity instanceof StandEntity stand ? stand : null;
    }
}
