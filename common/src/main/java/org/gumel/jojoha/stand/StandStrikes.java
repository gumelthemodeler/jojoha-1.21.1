package org.gumel.jojoha.stand;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.combat.ComboLink;
import org.gumel.jojoha.combat.DamageScale;
import org.gumel.jojoha.combat.StandPierce;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.data.StandTuning;
import org.gumel.jojoha.stand.heaven.Heaven;
import org.gumel.jojoha.stand.passive.StandPassives;
import org.gumel.jojoha.stand.skill.StandBeat;
import org.gumel.jojoha.stand.skill.moves.KnifeBarrageSkill;

final class StandStrikes {
    private final StandEntity stand;

    StandStrikes(StandEntity stand) {
        this.stand = stand;
    }

    private static final double STRIKE_ARRIVAL_DISTANCE = 0.55;

    private static final float PURSUIT_DAMAGE = 4.0F;

    private static final double PURSUIT_KNOCKBACK = 0.6;

    private static final double DEFENSE_SCAN_RANGE = 8.0;

    private static final int PUNCH_INTERVAL_TICKS = 11;

    private static final int STRIKE_RECOVERY_TICKS = 4;

    private static final int JAB_CONTACT_TICKS = (int) Math.round(StandEntity.AUTHORED_CONTACT_TICKS / StandEntity.JAB_ANIM_SPEED);

    private static final double CHARIOT_AUTHORED_CONTACT_TICKS = 5.0;

    private static final double MAGICIAN_AUTHORED_CONTACT_TICKS = 5.83;

    private static final int JAB_WAIT_TICKS = 3;

    private static final int STRIKING_TICKS = 14;

    private static final int COMBO_LAUNCH_TICKS = StandEntity.STRIKE_CONTACT_TICKS + STRIKE_RECOVERY_TICKS;

    private static final int COMBO_CHASE_TICKS = 16;

    private static final int COMBO_HOLD_TICKS = StandEntity.STRIKE_CONTACT_TICKS + 6;

    private static final int COMBO_SLAM_TICKS = StandEntity.STRIKE_CONTACT_TICKS + STRIKE_RECOVERY_TICKS;

    private static final int COMBO_SIGNATURE_TICKS = 26;

    private static final float COMBO_LAUNCH_DAMAGE = 5.0F;

    private static final float COMBO_SLAM_DAMAGE = 6.0F;

    private static final double COMBO_LAUNCH_LIFT = 0.95;

    private static final double COMBO_SLAM_FORCE = 1.5;

    private static final double COMBO_HOLD_DISTANCE = 1.1;

    private static final double COMBO_CHASE_SPEED = 0.85;

    private static final float COMBO_ENERGY_PER_STAGE = 3.5F;

    private static final int ATTACK_RECOVERY_TICKS = 8;

    private static final int BARRAGE_EVERY_N_ATTACKS = 3;

    private static final String[] CHARIOT_HITS = {"hit", "hit_followup", "hit_followup2"};

    private static final int CHARIOT_HIT_CHAIN_TICKS = 20;

    private int chariotHitStep = 0;

    private int lastChariotHitTick = -CHARIOT_HIT_CHAIN_TICKS - 1;

    private boolean comboUseGrab2 = false;

    private int pendingStrikeTicks = 0;

    private Runnable strikeContact;

    private int strikeContactTicks;

    void tickCommandedStrikes(Player owner, double distance) {
        if (distance <= StandEntity.ENGAGE_RANGE + 0.5 && stand.attackCooldown <= 0 && stand.commandedStrikes > 0) {
            stand.commandedStrikes--;
            stand.triggerAnim(StandEntity.CONTROLLER, strikeAnim(stand.pursuitUsePunch2));
            stand.pursuitUsePunch2 = !stand.pursuitUsePunch2;
            dealHitOnContact(strikeContactTicks(), owner, StandTuning.damage("pursuit", PURSUIT_DAMAGE), null);
            stand.attackCooldown = PUNCH_INTERVAL_TICKS;
            commandedLingerTicks = COMMANDED_LINGER_TICKS;
            return;
        }
        if (stand.commandedStrikes <= 0 && stand.attackCooldown <= 0 && stand.barrageHitsRemaining <= 0 && !stand.isHeldBarrageRunning()
                && --commandedLingerTicks <= 0) {
            stand.endEngagement();
        }
    }

    private static final int MOST_COMMANDED_STRIKES = 2;

    private int commandedLingerTicks;

    private static final int COMMANDED_LINGER_TICKS = 30;

    boolean tickCombo(Player owner) {
        if (!stand.huntingSelfDirected || stand.pursuitTarget == null) {
            stand.comboStage = StandEntity.ComboStage.NONE;
            return false;
        }

        if (stand.comboStage == StandEntity.ComboStage.NONE) {
            beginComboStage(owner, StandEntity.ComboStage.LAUNCH);
            return stand.comboStage != StandEntity.ComboStage.NONE;
        }

        switch (stand.comboStage) {
            case CHASE -> {
                Vec3 below = stand.pursuitTarget.position().add(0, stand.pursuitTarget.getBbHeight() * 0.3, 0);
                stand.moveTowardsAtSpeed(below, COMBO_CHASE_SPEED);
            }
            case HOLD -> holdTarget();
            case BARRAGE -> {
                Vec3 center = stand.pursuitTarget.position().add(0, stand.pursuitTarget.getBbHeight() * 0.5, 0);
                if (stand.position().distanceTo(center) > StandEntity.ENGAGE_RANGE) {
                    stand.moveTowardsAtSpeed(center, COMBO_CHASE_SPEED);
                }
            }
            default -> {
            }
        }

        stand.faceTarget();

        if (--stand.comboStageTicks > 0) {
            return true;
        }

        beginComboStage(owner, nextStage(stand.comboStage));
        return stand.comboStage != StandEntity.ComboStage.NONE;
    }

    private StandEntity.ComboStage nextStage(StandEntity.ComboStage stage) {
        return switch (stage) {
            case LAUNCH -> StandEntity.ComboStage.CHASE;
            case CHASE -> StandEntity.ComboStage.HOLD;
            case HOLD -> StandEntity.ComboStage.SLAM;
            case SLAM -> stand.rapierless() ? (signature() != null ? StandEntity.ComboStage.SIGNATURE : StandEntity.ComboStage.LAUNCH)
                    : StandEntity.ComboStage.BARRAGE;
            case BARRAGE -> signature() != null ? StandEntity.ComboStage.SIGNATURE : StandEntity.ComboStage.LAUNCH;
            default -> StandEntity.ComboStage.LAUNCH;
        };
    }

    private ComboSignature signature() {
        return StandTypes.THE_WORLD_ID.equals(stand.getStandType().id()) ? ComboSignature.KNIVES : null;
    }

    private enum ComboSignature {
        KNIVES
    }

    private void beginComboStage(Player owner, StandEntity.ComboStage stage) {
        stand.comboStage = stage;

        if (!spendComboEnergy(owner)) {
            stand.comboStage = StandEntity.ComboStage.NONE;
            stand.huntingSelfDirected = false;
            return;
        }

        switch (stage) {
            case LAUNCH -> {
                stand.comboStageTicks = COMBO_LAUNCH_TICKS;
                triggerStrikeAt(stand.pursuitTarget, "uppercut");
                dealHitOnContact(owner, StandTuning.damage("combo_launch", COMBO_LAUNCH_DAMAGE),
                        this::launchTarget);
            }
            case CHASE -> stand.comboStageTicks = COMBO_CHASE_TICKS;
            case HOLD -> {
                stand.comboStageTicks = COMBO_HOLD_TICKS;
                triggerStrikeAt(stand.pursuitTarget, comboUseGrab2 ? "grab2" : "grab");
                stand.comboHoldPose = comboUseGrab2 ? StandEntity.HOLD_GRAB2 : StandEntity.HOLD_GRAB;
                comboUseGrab2 = !comboUseGrab2;
            }
            case SLAM -> {
                stand.comboStageTicks = COMBO_SLAM_TICKS;
                triggerStrikeAt(stand.pursuitTarget, "kick");
                dealHitOnContact(owner, StandTuning.damage("combo_slam", COMBO_SLAM_DAMAGE),
                        this::slamTarget);
            }
            case BARRAGE -> {
                stand.comboStageTicks = StandEntity.BARRAGE_HITS * StandEntity.BARRAGE_HIT_INTERVAL + ATTACK_RECOVERY_TICKS;
                stand.barrageDealt.clear();
                stand.barrageStyle = stand.isChariot() ? StandEntity.BarrageStyle.SWORD : StandEntity.BarrageStyle.PUNCHES;
                stand.barrageHitsRemaining = stand.barrageStyle.hits;
                stand.barrageHitInterval = StandEntity.BARRAGE_HIT_INTERVAL;
                stand.playFlurryAnimation();
            }
            case SIGNATURE -> {
                stand.comboStageTicks = COMBO_SIGNATURE_TICKS;
                throwSignature(owner);
            }
            default -> stand.comboStageTicks = 0;
        }
    }

    private void throwSignature(Player owner) {
        ComboSignature move = signature();
        if (move == null || stand.pursuitTarget == null || !(owner instanceof ServerPlayer serverOwner)) {
            stand.comboStageTicks = 0;
            return;
        }

        switch (move) {
            case KNIVES -> {
                stand.faceTarget();
                KnifeBarrageSkill.throwAt(
                        serverOwner, stand, stand.pursuitTarget);
            }
        }
    }

    private void launchTarget() {
        Vec3 motion = stand.pursuitTarget.getDeltaMovement();
        stand.pursuitTarget.setDeltaMovement(motion.x * 0.2, COMBO_LAUNCH_LIFT, motion.z * 0.2);
        stand.pursuitTarget.hurtMarked = true;
    }

    private void holdTarget() {
        Vec3 held = stand.position()
                .add(stand.getLookAngle().scale(COMBO_HOLD_DISTANCE))
                .add(0, stand.getBbHeight() * 0.4, 0);

        stand.pursuitTarget.setPos(held.x, held.y, held.z);
        stand.pursuitTarget.setDeltaMovement(Vec3.ZERO);
        stand.pursuitTarget.fallDistance = 0F;
        stand.pursuitTarget.hurtMarked = true;
    }

    private void slamTarget() {
        Vec3 away = stand.getLookAngle().scale(COMBO_SLAM_FORCE);
        stand.pursuitTarget.setDeltaMovement(away.x, -COMBO_SLAM_FORCE, away.z);
        stand.pursuitTarget.hurtMarked = true;
    }

    private boolean spendComboEnergy(Player owner) {
        if (!(owner instanceof ServerPlayer serverOwner)) {
            return false;
        }

        JojohaPlayerData data = PlayerDataAccess.get(serverOwner);
        float stage = COMBO_ENERGY_PER_STAGE * Heaven.costScale(data);
        if (data.standEnergy < stage) {
            return false;
        }

        data.standEnergy -= stage;
        PlayerDataAccess.set(serverOwner, data);
        PlayerDataAccess.sync(serverOwner);
        return true;
    }

    void performAttack(Player owner) {
        boolean barrage = ++stand.attacksThisEngagement % BARRAGE_EVERY_N_ATTACKS == 0 && !stand.rapierless();

        if (barrage) {
            stand.barrageDealt.clear();
            stand.barrageStyle = stand.isChariot() ? StandEntity.BarrageStyle.SWORD : StandEntity.BarrageStyle.PUNCHES;
            stand.barrageHitsRemaining = stand.barrageStyle.hits;
            stand.playFlurryAnimation();
            stand.attackCooldown = StandEntity.BARRAGE_HITS * StandEntity.BARRAGE_HIT_INTERVAL + ATTACK_RECOVERY_TICKS;
        } else {
            stand.triggerAnim(StandEntity.CONTROLLER, strikeAnim(stand.pursuitUsePunch2));
            stand.pursuitUsePunch2 = !stand.pursuitUsePunch2;
            dealHitOnContact(strikeContactTicks(), owner, StandTuning.damage("pursuit", PURSUIT_DAMAGE), null);
            stand.attackCooldown = PUNCH_INTERVAL_TICKS;
        }
    }

    private void dealHitOnContact(Player owner, float damage, Runnable alsoDo) {
        dealHitOnContact(StandEntity.STRIKE_CONTACT_TICKS, owner, damage, alsoDo);
    }

    private int strikeContactTicks() {
        return stand.isMagician() || stand.isChariot() ? jabContactTicks() : StandEntity.STRIKE_CONTACT_TICKS;
    }

    private void dealHitOnContact(int ticks, Player owner, float damage, Runnable alsoDo) {
        LivingEntity struck = stand.pursuitTarget;
        if (struck == null || !(owner instanceof ServerPlayer serverOwner)) {
            return;
        }

        StandBeat.after(ticks, serverOwner, struck,
                (attacker, hit) -> {
                    if (stand.pursuitTarget != hit) {
                        return;
                    }
                    dealHit(attacker, damage);
                    if (alsoDo != null) {
                        alsoDo.run();
                    }
                });
    }

    void dealHit(Player owner, float damage) {
        dealHit(owner, damage, false);
    }

    void dealHit(Player owner, float damage, boolean scaled) {
        LivingEntity target = stand.pursuitTarget;
        if (target == null || !(stand.level() instanceof ServerLevel serverLevel)
                || !(owner instanceof ServerPlayer serverOwner)) {
            return;
        }

        JojohaPlayerData data = PlayerDataAccess.get(serverOwner);
        float dealt = scaled ? damage * data.stand.powerScale()
                : StandPassives.scaleOutgoing(serverOwner, data, target,
                        damage * data.stand.powerScale());

        boolean landed = StandPierce.hurt(target, serverLevel.damageSources().playerAttack(owner), DamageScale.stand(owner, target, dealt));
        if (landed && !scaled) {
            ComboLink.landed(serverOwner);
        }
        stand.impactRings(serverLevel, target, dealt, stand.barrageHitsRemaining > 0 ? StandEntity.BARRAGE_RING_CLUSTER : 1);
        Vec3 push = target.position().subtract(stand.position()).normalize().scale(PURSUIT_KNOCKBACK);
        target.setDeltaMovement(target.getDeltaMovement().add(push.x, 0.1, push.z));
        target.hurtMarked = true;

        data.combatTicks = StandCombatHandler.COMBAT_TIMER_TICKS;
        PlayerDataAccess.set(serverOwner, data);
    }

    LivingEntity findNearbyHostile(Player owner) {
        AABB search = owner.getBoundingBox().inflate(DEFENSE_SCAN_RANGE);
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;

        for (LivingEntity candidate : stand.level().getEntitiesOfClass(LivingEntity.class, search,
                entity -> entity instanceof Enemy && entity.isAlive() && !(entity instanceof StandEntity))) {
            double distance = candidate.distanceToSqr(owner);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }

        return best;
    }

    public void triggerSwing(boolean usePunch2) {
        Player owner = stand.getOwner();
        if (owner != null) {
            float yaw = owner.getYRot();
            stand.setYRot(yaw);
            stand.setYBodyRot(yaw);
            stand.setYHeadRot(yaw);
        }
        stand.triggerAnim(StandEntity.CONTROLLER, strikeAnim(usePunch2));
        takeAttackStance();
    }

    public void triggerPunchAt(LivingEntity target, boolean usePunch2) {
        stand.faceTowards(target);
        stand.triggerAnim(StandEntity.CONTROLLER, strikeAnim(usePunch2));
        takeAttackStance();
    }

    public void triggerStrikeAt(LivingEntity target, String animation) {
        stand.faceTowards(target);
        stand.triggerAnim(StandEntity.CONTROLLER, animation);
        takeAttackStance();
    }

    public void triggerStrikeForward(String animation) {
        Player owner = stand.getOwner();
        if (owner != null) {
            float yaw = owner.getYRot();
            stand.setYRot(yaw);
            stand.setYBodyRot(yaw);
            stand.setYHeadRot(yaw);
        }
        stand.triggerAnim(StandEntity.CONTROLLER, animation);
        takeAttackStance();
    }

    public void strikeFrom(Vec3 at, LivingEntity target, String animation) {
        stand.moveTo(at.x, at.y, at.z, stand.getYRot(), stand.getXRot());
        stand.sendToWork(at, StandEntity.UTILITY_STEP_SPEED);
        stand.faceTowards(target);
        stand.triggerAnim(StandEntity.CONTROLLER, animation);
        takeAttackStance();
    }

    public void pursueAndPunch(LivingEntity target, boolean usePunch2) {
        if (!stand.getTrustTier().canActAtRange()) {
            punch(usePunch2);
            return;
        }

        if (stand.pursuitTarget != target) {
            stand.attacksThisEngagement = 0;
            stand.orbitAngle = stand.getRandom().nextDouble() * Math.PI * 2;
        }

        stand.pursuitTarget = target;
        stand.pursuitUsePunch2 = usePunch2;
        stand.returningFromPursuit = false;
        stand.huntingSelfDirected = false;
        stand.commandedStrikes = Math.min(MOST_COMMANDED_STRIKES, stand.commandedStrikes + 1);
        commandedLingerTicks = COMMANDED_LINGER_TICKS;
    }

    void takeAttackStance() {
        stand.punchStanceTicksRemaining = StandEntity.PUNCH_STANCE_TICKS;
        stand.combatHoldTicks = StandEntity.COMBAT_HOLD_TICKS;
    }

    public void keepStance() {
        takeAttackStance();
    }

    String strikeAnim(boolean usePunch2) {
        if (stand.isMagician()) {
            return usePunch2 ? "hit_followup" : "hit";
        }
        if (!stand.isChariot()) {
            return usePunch2 ? "punch2" : "punch";
        }
        if (stand.tickCount - lastChariotHitTick > CHARIOT_HIT_CHAIN_TICKS) {
            chariotHitStep = 0;
        }
        lastChariotHitTick = stand.tickCount;
        String clip = CHARIOT_HITS[chariotHitStep];
        chariotHitStep = (chariotHitStep + 1) % CHARIOT_HITS.length;
        return clip;
    }

    public void punch(boolean usePunch2) {
        punch(usePunch2, null);
    }

    public void punch(boolean usePunch2, Runnable onContact) {
        takeAttackStance();
        queueStrike(usePunch2 ? StandEntity.PendingStrike.PUNCH2 : StandEntity.PendingStrike.PUNCH);
        pendingStrikeTicks = JAB_WAIT_TICKS;
        stand.strikingTicks = STRIKING_TICKS;
        strikeContact = onContact;
        strikeContactTicks = 0;
    }

    void queueStrike(StandEntity.PendingStrike strike) {
        takeAttackStance();
        landStrikeNow();
        stand.pendingStrike = strike;
        pendingStrikeTicks = StandEntity.STRIKE_WAIT_TIMEOUT_TICKS;
    }

    private String jabAnim(boolean usePunch2) {
        if (stand.isMagician() || stand.isChariot()) {
            return strikeAnim(usePunch2);
        }
        return usePunch2 ? "jab2" : "jab";
    }

    private int jabContactTicks() {
        if (stand.isMagician()) {
            return (int) Math.round(MAGICIAN_AUTHORED_CONTACT_TICKS / StandEntity.CHARIOT_HIT_ANIM_SPEED);
        }
        if (stand.isChariot()) {
            return (int) Math.round(CHARIOT_AUTHORED_CONTACT_TICKS / StandEntity.CHARIOT_HIT_ANIM_SPEED);
        }
        return JAB_CONTACT_TICKS;
    }

    private void landStrikeNow() {
        Runnable contact = strikeContact;
        strikeContact = null;
        strikeContactTicks = 0;
        if (contact != null) {
            contact.run();
        }
    }

    void tickStrikeContact() {
        if (stand.strikingTicks > 0) {
            stand.strikingTicks--;
        }
        if (strikeContact != null && strikeContactTicks > 0 && --strikeContactTicks == 0) {
            landStrikeNow();
        }
    }

    void tickPendingStrike(Player owner) {
        if (stand.pendingStrike == null) {
            return;
        }

        boolean arrived = stand.isPiloted()
                || stand.position().distanceTo(stand.followAnchor(owner)) <= STRIKE_ARRIVAL_DISTANCE;
        if (!arrived && --pendingStrikeTicks > 0) {
            return;
        }

        switch (stand.pendingStrike) {
            case PUNCH, PUNCH2 -> {
                stand.triggerAnim(StandEntity.CONTROLLER, jabAnim(stand.pendingStrike == StandEntity.PendingStrike.PUNCH2));
                strikeContactTicks = jabContactTicks();
            }
            case BARRAGE -> stand.releaseCommandedBarrage();
        }

        stand.pendingStrike = null;
    }
}
