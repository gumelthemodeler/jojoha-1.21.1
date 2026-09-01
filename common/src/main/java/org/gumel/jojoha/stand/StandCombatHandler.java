package org.gumel.jojoha.stand;

import dev.architectury.event.EventResult;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.data.StandTuning;
import org.gumel.jojoha.registry.ModSounds;
import org.gumel.jojoha.stand.skill.TimeStopCast;

import java.util.List;

/**
 * Server-side handling of the M1 punch chain and the hold-to-guard stance. Placeholder
 * numbers, not balanced content - see the design doc's Stand Balance section for the intent
 * (energy drain/restoration is handled by {@link org.gumel.jojoha.combat.EnergySystem} and the
 * {@code EntityEvent.LIVING_HURT} hook registered in {@code Jojoha.init()}).
 */
public final class StandCombatHandler {
    private static final double PUNCH_RANGE = 3.0;
    /**
     * The M1, matched to {@code StandEntity.PURSUIT_DAMAGE}.
     *
     * <p>The two are the same act - the Stand striking something its user pointed at - and only
     * differ in whether it has to travel first, so they had no business being separate numbers.
     * They were both three, which is under a stone sword at this cadence; see the note on the
     * other one for where four comes from.
     */
    private static final float PUNCH_DAMAGE = 4.0F;

    /**
     * How far off dead ahead the swing still connects, as a dot product against the look vector.
     *
     * <p>Zero is the whole front half, which is generous - a Stand's arms are long and the point is
     * not to make the player aim precisely. What it rules out is the previous behaviour, where the
     * swing was a plain sphere and landed on everything within three blocks including whatever was
     * directly behind the player. That is the same defect the barrage sweep had and it is fixed the
     * same way, so the two now agree about what "in front of you" means.
     */
    private static final double PUNCH_ARC = 0.0;
    private static final double PUNCH_KNOCKBACK = 0.6;
    private static final int PUNCH_COOLDOWN_TICKS = 10;
    // Star Platinum's reach: the furthest it will fly out to strike something instead of swinging
    // in place. Beyond this the Stand simply doesn't answer - it's a close-range powerhouse, not
    // a long-distance one. StandEntity's PURSUIT_ABANDON_RANGE sits just past this so a target
    // stepping back mid-flight doesn't immediately cancel the strike.
    private static final double PURSUIT_LOOK_RANGE = 7.0;

    /** Action-bar colours for the stance announcement - red for attack, blue for defense. */
    private static final int MODE_LABEL_COLOR = 0xC8C8C8;
    private static final int MODE_ATTACK_COLOR = 0xFF5555;
    private static final int MODE_DEFENSE_COLOR = 0x5599FF;
    /** Green, and the same green as the Utility eyes - see CentralBarOverlay. */
    private static final int MODE_UTILITY_COLOR = 0x77DD67;

    static final int COMBAT_TIMER_TICKS = 100;
    static final int GUARD_BREAK_LOCKOUT_TICKS = 100;
    private static final float BLOCK_ENERGY_RESTORE = 8F;
    private static final float NON_STAND_HIT_ENERGY_RESTORE = 5F;

    private StandCombatHandler() {
    }

    /** How hard a grabbed target is hauled in, and the lift that keeps it off the ground on the way. */
    private static final double GRAB_PULL_SPEED = 1.15;
    private static final double GRAB_PULL_LIFT = 0.35;
    // A grab is a reposition, not a blow - the value of it is that the target is now somewhere you
    // can hit it. Lowered so the pull is not also competitive as damage in its own right.
    private static final float GRAB_DAMAGE = 2.0F;

    /** How hard a held thing leaves the hand, and the lift that keeps a level throw off the floor. */
    private static final double THROW_SPEED = 1.4;
    private static final double THROW_LIFT = 0.25;

    /**
     * Reaches out and drags a distant target to the user.
     *
     * <p>Aimed at a point just short of the player rather than at the player themselves, so the
     * target arrives in front of them at swinging distance instead of inside them - landing a mob
     * on top of the user would shove the two apart again and undo the pull.
     *
     * <p>The velocity is set outright rather than added, because whatever the target was doing
     * before is precisely what the grab is meant to override.
     */
    /**
     * Takes hold of something, or hauls it in if it cannot be held.
     *
     * <p>Holding is the better outcome and is tried first. Some things cannot be held - a player, or
     * something already riding - and rather than the grab doing nothing at all in those cases it
     * falls back to what it used to do, which is yank the target over to its user. The move still
     * lands either way; only the ending differs.
     */
    private static void grabAndHold(ServerPlayer player, JojohaPlayerData data,
                                    StandEntity stand, LivingEntity target) {
        // Spent, so one leap buys one grab - otherwise a single leap would let the whole window be
        // spent reeling in one target after another.
        data.standLeapGrabTicks = 0;

        if (!stand.grab(target)) {
            stand.triggerGrabAt(target);

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
        target.hurt(player.serverLevel().damageSources().playerAttack(player),
                org.gumel.jojoha.stand.passive.StandPassives.scaleOutgoing(player, data, target,
                        StandTuning.damage("grab", GRAB_DAMAGE) * data.stand.powerScale()));
        data.lastDamageWasStandAttack = false;

        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG,
                SoundSource.PLAYERS, 0.9F, 0.8F);
        data.combatTicks = COMBAT_TIMER_TICKS;
    }

    /**
     * Puts what the Stand is holding through the air, the way its user is looking.
     *
     * <p>Thrown along the look vector rather than flat, so aiming up lobs and aiming down slams. The
     * small extra lift is there because a throw straight along a level gaze skims the ground and
     * stops almost at once, which reads as dropping something rather than throwing it.
     */
    private static void throwHeld(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        LivingEntity thrown = stand.heldEntity();
        Vec3 velocity = player.getLookAngle().scale(THROW_SPEED).add(0, THROW_LIFT, 0);
        stand.releaseHeld(velocity);

        if (thrown != null) {
            data.lastDamageWasStandAttack = true;
            thrown.hurt(player.serverLevel().damageSources().playerAttack(player),
                    org.gumel.jojoha.stand.passive.StandPassives.scaleOutgoing(player, data, thrown,
                            StandTuning.damage("grab", GRAB_DAMAGE) * data.stand.powerScale()));
            data.lastDamageWasStandAttack = false;
        }

        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG,
                SoundSource.PLAYERS, 1.0F, 0.7F);
        data.combatTicks = COMBAT_TIMER_TICKS;
    }

    /**
     * Whether there is anything the Stand could actually hit.
     *
     * <p>Either something standing close enough to swing at, or something the user is looking at
     * that the Stand can be sent out to. Those are exactly the two branches below, so this asks the
     * same questions they answer rather than inventing a third notion of range.
     */
    private static boolean hasReachableTarget(ServerPlayer player, ServerLevel level,
                                              LivingEntity lookTarget) {
        return lookTarget != null || !withinPunchRange(player, level).isEmpty();
    }

    /** Everything close enough for the Stand to hit without going anywhere. */
    private static List<LivingEntity> withinPunchRange(ServerPlayer player, ServerLevel level) {
        AABB area = player.getBoundingBox().inflate(PUNCH_RANGE);
        return level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != player && !(entity instanceof StandEntity) && entity.isAlive()
                        && player.distanceTo(entity) <= PUNCH_RANGE);
    }

    public static void handlePunchRequest(ServerPlayer player) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        // Nothing is manifested at DORMANT, and PARTIAL's arms are reinforcement that guards
        // rather than strikes - at both tiers the M1 stays the player's own ordinary punch,
        // which vanilla already handles since the client never consumes the attack key.
        if (!data.standSummoned || !data.stand.isPresent() || !data.stand.trust().canStandPunch()) {
            return;
        }

        // Utility is not a fighting stance. The Stand is out there laying blocks and its user's
        // right hand has already been given away to it - handing it a left hook as well would mean
        // a stance with no way to simply hit something yourself, which is the one thing a player
        // reaches for when a stance turns out to be the wrong one.
        if (data.standMode.handlesItems()) {
            return;
        }

        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        StandEntity stand = findStand(player, data);

        if (now < data.punchCooldownExpiry) {
            return;
        }

        // With something in its hand, the attack button throws rather than punches. No new binding:
        // a Stand holding a cow above its head has exactly one thing its user obviously wants to do
        // next, and asking them to learn a key for it would be ceremony.
        //
        // Ahead of the reach test, because what is being thrown is already in the hand - whether
        // there is anything else within range has nothing to do with it.
        if (stand != null && stand.isHolding()) {
            data.punchCooldownExpiry = now + PUNCH_COOLDOWN_TICKS;
            throwHeld(player, data, stand);
            PlayerDataAccess.set(player, data);
            PlayerDataAccess.sync(player);
            return;
        }

        LivingEntity lookTarget = findLookTarget(player, PURSUIT_LOOK_RANGE);

        // Swinging at nothing does nothing. A Stand throwing punches into empty air on every click
        // reads as a twitch rather than an attack, and it costs the player their stance and their
        // Stand's position for a blow that was never going to land.
        //
        // Reachable means either of the two things the Stand can actually do about a target: swing
        // where it stands, or be sent out to something the user is looking at. Nothing in range of
        // either and the request is dropped whole - before the cooldown is spent and before the
        // punch alternation advances, so a miss costs nothing and the next real swing is not the
        // weaker half of a pair.
        //
        // The player's own attack is untouched: the client sends this alongside the ordinary swing
        // rather than instead of it, so a punch at the air still mines the block in front of you.
        if (!hasReachableTarget(player, level, lookTarget)) {
            return;
        }

        data.punchCooldownExpiry = now + PUNCH_COOLDOWN_TICKS;

        boolean usePunch2 = data.nextPunchIsPunch2;
        data.nextPunchIsPunch2 = !usePunch2;

        // Mid-leap, a strike at something out of arm's reach becomes a grab: the Stand reaches out
        // and hauls it back rather than being sent over to it. Checked before the ordinary pursuit
        // branch, which would otherwise claim exactly the same case.
        if (stand != null && lookTarget != null && data.standLeapGrabTicks > 0
                && player.distanceTo(lookTarget) > PUNCH_RANGE) {
            grabAndHold(player, data, stand, lookTarget);
            PlayerDataAccess.set(player, data);
            PlayerDataAccess.sync(player);
            return;
        }

        if (stand != null && lookTarget != null && player.distanceTo(lookTarget) > PUNCH_RANGE) {
            // Out of melee range - send the Stand over to chase it down instead of swinging in
            // place. StandEntity deals the actual damage itself once it arrives (tickPursuit()).
            stand.pursueAndPunch(lookTarget, usePunch2);
            data.combatTicks = COMBAT_TIMER_TICKS;
            PlayerDataAccess.set(player, data);
            PlayerDataAccess.sync(player);
            return;
        }

        if (stand != null) {
            stand.punch(usePunch2);
        }

        List<LivingEntity> targets = withinPunchRange(player, level);

        Vec3 look = player.getLookAngle();
        float punch = StandTuning.damage("punch", PUNCH_DAMAGE) * data.stand.powerScale();

        data.lastDamageWasStandAttack = true;
        for (LivingEntity target : targets) {
            Vec3 toTarget = target.position().subtract(player.position());
            if (toTarget.lengthSqr() > 1.0E-4 && look.dot(toTarget.normalize()) < PUNCH_ARC) {
                continue;
            }

            // Scaled inside the loop, not outside it: the swing may catch several bodies at
            // different distances, and a passive that cares about distance owes each its own answer.
            target.hurt(level.damageSources().playerAttack(player),
                    org.gumel.jojoha.stand.passive.StandPassives.scaleOutgoing(player, data, target, punch));
            Vec3 push = target.position().subtract(player.position()).normalize().scale(PUNCH_KNOCKBACK);
            target.setDeltaMovement(target.getDeltaMovement().add(push.x, 0.2, push.z));
            target.hurtMarked = true;
        }
        data.lastDamageWasStandAttack = false;

        data.combatTicks = COMBAT_TIMER_TICKS;
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
    }

    /** What the player's crosshair is on, within range - used to decide whether to pursue instead of swinging in place. */
    private static LivingEntity findLookTarget(ServerPlayer player, double maxDistance) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        Vec3 endVec = eyePos.add(look.scale(maxDistance));
        AABB searchBox = player.getBoundingBox().expandTowards(look.scale(maxDistance)).inflate(1.0);

        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player.level(), player, eyePos, endVec, searchBox,
                entity -> entity instanceof LivingEntity livingEntity && livingEntity.isAlive()
                        && entity != player && !(entity instanceof StandEntity));

        return hit != null ? (LivingEntity) hit.getEntity() : null;
    }

    /**
     * Sends the Stand at a specific entity the player picked out.
     *
     * <p>Everything the client asserted is re-checked here - that the Stand is out, that it can
     * act at range for its tier, and that the entity is a live, valid target still within reach -
     * because the id arrived over the wire and nothing about it can be taken on trust.
     */
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

    /**
     * Flips the Stand's stance. Harmless with no Stand out - the mode is remembered either way, so
     * a player who set themselves to DEFENSE before summoning gets that stance immediately.
     */
    public static void handleCycleMode(ServerPlayer player) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        data.standMode = data.standMode.next();

        StandEntity stand = findStand(player, data);
        if (stand != null) {
            stand.setMode(data.standMode);
        }

        // Announced on the action bar rather than drawn persistently: the stance only matters at
        // the moment it changes, and vanilla already fades this out on its own.
        //
        // The name and the colour both come off the mode itself now. With two stances a boolean
        // and a pair of ternaries was the same thing written shorter; with three it would be the
        // start of a chain that has to be extended in two places every time a stance is added, and
        // which silently mislabels the new one until both are.
        StandMode mode = data.standMode;
        player.displayClientMessage(Component.translatable("hud.jojoha.stand_mode")
                .withStyle(style -> style.withColor(MODE_LABEL_COLOR))
                .append(Component.translatable(mode.translationKey())
                        .withStyle(style -> style.withColor(modeColor(mode)))),
                true);

        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
    }

    /** Matches the eye colour the HUD puts on the figure for the same stance. */
    private static int modeColor(StandMode mode) {
        return switch (mode) {
            case DEFENSE -> MODE_DEFENSE_COLOR;
            case UTILITY -> MODE_UTILITY_COLOR;
            default -> MODE_ATTACK_COLOR;
        };
    }

    public static void handleSetGuard(ServerPlayer player, boolean wantsGuarding) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        // Guarding needs arms but not a whole Stand, so PARTIAL qualifies where punching doesn't.
        // Only DORMANT, with nothing manifested at all, has nothing to block with.
        boolean allowed = wantsGuarding && data.standSummoned && data.stand.isPresent()
                && data.stand.trust().canGuard() && data.guardBreakCooldownTicks <= 0
                // Nothing to guard with: in Utility the Stand is somewhere else entirely, and a
                // guard it cannot be present for would be a shield that is not there.
                && !data.standMode.handlesItems();

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
    }

    /**
     * Registered against {@code EntityEvent.LIVING_HURT} in {@code Jojoha.init()}. Handles two
     * independent cases per the design doc's Stand Balance section: the victim absorbing a hit
     * via Stand blocking (restores energy, tracks toward a guard break, fully negates the hit),
     * and the attacker restoring energy for dealing damage "in a non-stand form".
     */
    public static EventResult handleLivingHurt(LivingEntity hurt, DamageSource source, float amount) {
        if (hurt.level().isClientSide()) {
            return EventResult.pass();
        }

        // Held time takes the blow before anything else looks at it. Checked first because a hit
        // that is not happening yet should not restore energy, count toward a guard break, or start
        // a combat timer - all of that belongs to the moment it actually lands, which is later.
        if (org.gumel.jojoha.stand.skill.TimeStopSystem.holdDamage(hurt, source, amount)) {
            return EventResult.interruptFalse();
        }

        EventResult result = EventResult.pass();

        if (hurt instanceof ServerPlayer hurtPlayer) {
            JojohaPlayerData data = PlayerDataAccess.get(hurtPlayer);
            data.combatTicks = COMBAT_TIMER_TICKS;

            // Taking a hit breaks a time stop that is still charging. Deliberately any damage, not
            // only attacks: being set on fire mid-cast should cost it just as much as being punched.
            TimeStopCast.interrupt(hurtPlayer);

            // The breath's window comes before the guard, for the same reason held time comes
            // before both: a hit that never lands should not spend a block or count toward a
            // guard break. Same "something threw this" test the guard uses, so the environment
            // still gets through.
            if (data.inhaleIFrameTicks > 0 && source.getDirectEntity() != null) {
                PlayerDataAccess.set(hurtPlayer, data);
                PlayerDataAccess.sync(hurtPlayer);
                return EventResult.interruptFalse();
            }

            // Only direct hits (melee/projectiles) are blockable - not fall damage, fire, etc.
            if (data.standGuarding && source.getDirectEntity() != null) {
                // The Stand is told who it was before the guard-break check, which can end the
                // stance - a counter is owed for the blow that broke it just as much as for one it
                // held against.
                StandEntity guard = findStand(hurtPlayer, data);
                if (guard != null && source.getEntity() instanceof LivingEntity attacker) {
                    guard.onGuardHit(attacker, hurtPlayer);
                }

                onBlockedHit(hurtPlayer, data);
                data.standEnergy = Math.min(data.maxStandEnergy(), data.standEnergy + BLOCK_ENERGY_RESTORE);
                result = EventResult.interruptFalse();
            }

            PlayerDataAccess.set(hurtPlayer, data);
            PlayerDataAccess.sync(hurtPlayer);
        }

        if (source.getEntity() instanceof ServerPlayer attacker) {
            JojohaPlayerData data = PlayerDataAccess.get(attacker);
            data.combatTicks = COMBAT_TIMER_TICKS;
            if (data.standSummoned && !data.lastDamageWasStandAttack) {
                data.standEnergy = Math.min(data.maxStandEnergy(), data.standEnergy + NON_STAND_HIT_ENERGY_RESTORE);
            }
            PlayerDataAccess.set(attacker, data);
            PlayerDataAccess.sync(attacker);
        }

        return result;
    }

    /** Called when a guarding player absorbs a hit - tracks toward a guard break. */
    private static void onBlockedHit(ServerPlayer player, JojohaPlayerData data) {
        data.blockedHitsSinceBreak++;

        // Told how close it is to giving way, so the crack overlay grows off the same count the
        // break fires on rather than off a second one kept alongside it.
        StandEntity guard = findStand(player, data);
        if (guard != null) {
            guard.setGuardStrain(data.blockedHitsSinceBreak
                    / (float) org.gumel.jojoha.data.StatEffects.guardHits(data.stand.protection()));
        }

        if (data.blockedHitsSinceBreak >= org.gumel.jojoha.data.StatEffects.guardHits(
                data.stand.protection())) {
            data.blockedHitsSinceBreak = 0;
            data.guardBreakCooldownTicks = GUARD_BREAK_LOCKOUT_TICKS;
            data.standGuarding = false;

            // The break belongs to the Stand: it knows where the guard was standing, which is
            // where the sound and the wreckage have to come from. Only the case where there is no
            // Stand to ask is handled here, and that should not be reachable while guarding.
            if (guard != null) {
                guard.triggerGuardBroken();
            } else {
                player.serverLevel().playSound(null, player.blockPosition(), ModSounds.GUARD_BREAK.get(),
                        SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        }
    }

    private static StandEntity findStand(ServerPlayer player, JojohaPlayerData data) {
        if (data.summonedStandEntityUuid == null) {
            return null;
        }
        Entity entity = player.serverLevel().getEntity(data.summonedStandEntityUuid);
        return entity instanceof StandEntity stand ? stand : null;
    }
}
