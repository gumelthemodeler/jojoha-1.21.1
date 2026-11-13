package org.gumel.jojoha.stand;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.stand.mob.MobStandBrain;
import org.joml.Vector3f;

final class StandMovement {
    private final StandEntity stand;

    StandMovement(StandEntity stand) {
        this.stand = stand;
    }

    private static final double UTILITY_SIDE_OFFSET = 1.15;

    private static final double UTILITY_ARRIVAL = 1.2;

    private static final double GUARD_OFFSET = 0.85;

    private static final double GUARD_HEIGHT = 0.0;

    private static final double PARTIAL_HEIGHT_OFFSET = 0.0;

    private static final double PURSUIT_SPEED = 1.0;

    private static final double RETURN_SPEED = 0.9;

    private static final int RETURN_GIVE_UP_TICKS = 80;

    private static final double HUNT_SPEED = 0.42;

    private static final int RETARGET_DELAY_TICKS = 28;

    private static final double ENGAGE_STRAFE_SPEED = 0.35;

    private static final double ORBIT_SPEED = 0.10;

    private static final double STRIKE_SIDE = 0.35;

    private static final double STRIKE_STIFFNESS = 0.45;

    private static final double STRIKE_DAMPING = 0.62;

    private static final double RETURN_ARRIVAL_DISTANCE = 0.35;

    private Vec3 followVelocity = Vec3.ZERO;

    private Vec3 lastOwnerAt;

    private Vec3 barrageSway = Vec3.ZERO;

    private static final double COMBAT_REACH_FRACTION = 0.66;

    private double utilityStepSpeed = StandEntity.UTILITY_STEP_SPEED;

    private boolean workIsUtilityJob;

    private int returnTicks;

    private static final float GLIDE_TURN = 50F;

    private static final int GLIDE_TICKS = 7;

    private static final int TURN_TICKS = 6;

    private Vec3 glideFrom = Vec3.ZERO;

    private int glideAt = Integer.MIN_VALUE / 2;

    private float turnFrom;

    private int turnAt = Integer.MIN_VALUE / 2;

    void glide(Vec3 before, float yawBefore) {
        if (stand.isPiloted() || stand.getTrustTier().isPartialManifestation() || stand.getStandType().form().isBound()) {
            glideAt = Integer.MIN_VALUE / 2;
            turnAt = Integer.MIN_VALUE / 2;
            return;
        }
        Vec3 jumped = before.subtract(stand.position());
        Player owner = stand.getOwner();
        Vec3 carried = owner == null ? Vec3.ZERO
                : new Vec3(owner.getX() - owner.xo, owner.getY() - owner.yo, owner.getZ() - owner.zo);
        if (jumped.add(carried).lengthSqr() > StandEntity.GLIDE_JUMP * StandEntity.GLIDE_JUMP) {
            glideFrom = glideOffset(0F).add(jumped);
            glideAt = stand.tickCount;
            stand.xo = stand.getX();
            stand.yo = stand.getY();
            stand.zo = stand.getZ();
            stand.xOld = stand.getX();
            stand.yOld = stand.getY();
            stand.zOld = stand.getZ();
        }
        float turned = Mth.wrapDegrees(stand.yBodyRot - yawBefore);
        if (Math.abs(turned) > GLIDE_TURN) {
            turnFrom = Mth.wrapDegrees(glideYaw(0F) - turned);
            turnAt = stand.tickCount;
            stand.yBodyRotO = stand.yBodyRot;
        }
    }

    private static float easeLeft(float share) {
        if (share >= 1F) {
            return 0F;
        }
        float left = 1F - Math.max(0F, share);
        return left * left * left;
    }

    public Vec3 glideOffset(float partialTick) {
        float left = easeLeft((stand.tickCount - glideAt + partialTick) / GLIDE_TICKS);
        return left <= 0F ? Vec3.ZERO : glideFrom.scale(left);
    }

    public float glideYaw(float partialTick) {
        return turnFrom * easeLeft((stand.tickCount - turnAt + partialTick) / TURN_TICKS);
    }

    void tickMobStand() {
        if (stand.level().isClientSide()) {
            return;
        }
        Mob mob = stand.getMobOwner();
        if (mob == null || !mob.isAlive() || mob.isRemoved()) {
            stand.discard();
            return;
        }
        if (stand.isDismissing() && stand.dismissProgress() >= 1F) {
            stand.discard();
            return;
        }
        if (!stand.spawnAnimationTriggered) {
            stand.spawnAnimationTriggered = true;
            stand.setInvisible(false);
            stand.triggerAnim(StandEntity.CONTROLLER, StandTypes.spawnAnimation(stand.getStandType()));
        }
        stand.setDeltaMovement(Vec3.ZERO);
        if (!stand.isDismissing()) {
            MobStandBrain.tick(stand, mob);
        }
    }

    boolean isClientPredictingFollow() {
        return stand.level().isClientSide()
                && !stand.isMobStand()
                && !stand.isEngaged()
                && !stand.isInhaling()
                && !stand.isPiloted()
                && !stand.getTrustTier().isPartialManifestation();
    }

    void tickFollow(Player owner) {
        StandMovementProfile profile = stand.movement();

        stand.setPos(stand.position().subtract(barrageSway));
        Vec3 anchor = followAnchor(owner);

        if (stand.position().distanceToSqr(anchor) > FOLLOW_LEASH * FOLLOW_LEASH) {
            snapToOwner(anchor);
        }

        stand.setPos(stand.position().add(carried(owner)));
        if (stand.synced(StandEntity.DATA_STRIKING)) {
            springTo(anchor, Math.max(STRIKE_STIFFNESS, profile.springStiffness()),
                    Math.max(STRIKE_DAMPING, profile.springDamping()));
        } else {
            springTo(anchor, profile.springStiffness(), profile.springDamping());
        }
        barrageSway = stand.nextBarrageSway(owner, profile);
        stand.setPos(stand.position().add(barrageSway));

        if (stand.guarding) {
            stand.faceGuard(owner);
            return;
        }

        faceOwner(owner);
    }

    Vec3 carried(Player owner) {
        Vec3 now = owner.position();
        Vec3 was = lastOwnerAt;
        lastOwnerAt = now;
        if (was == null) {
            return Vec3.ZERO;
        }
        Vec3 moved = now.subtract(was);
        double speed = moved.length();
        if (speed <= CARRY_FROM || speed > FOLLOW_LEASH) {
            return Vec3.ZERO;
        }
        return moved.scale((speed - CARRY_FROM) / speed);
    }

    public void sendToWork(Vec3 anchor) {
        sendToWork(anchor, StandEntity.UTILITY_STEP_SPEED);
    }

    public void sendToWork(Vec3 anchor, double blocksPerTick) {
        sendToWork(anchor, blocksPerTick, false);
    }

    void sendToWork(Vec3 anchor, double blocksPerTick, boolean forUtilityJob) {
        if (stand.pursuitTarget != null || stand.returningFromPursuit) {
            endEngagement();
            stand.returningFromPursuit = false;
        }
        stand.utilityAnchor = anchor;
        utilityStepSpeed = blocksPerTick;
        workIsUtilityJob = forUtilityJob;
    }

    public void sendToUtilityWork(Vec3 anchor) {
        sendToWork(anchor, StandEntity.UTILITY_STEP_SPEED, true);
    }

    public void clearWork() {
        boolean wasWorking = stand.utilityAnchor != null;
        stand.utilityAnchor = null;
        utilityStepSpeed = StandEntity.UTILITY_STEP_SPEED;
        workIsUtilityJob = false;

        if (!wasWorking) {
            return;
        }
        Player owner = stand.getOwner();
        if (owner != null && stand.position().distanceTo(followAnchor(owner)) > RETURN_ARRIVAL_DISTANCE) {
            beginReturn();
        }
    }

    private void beginReturn() {
        stand.returningFromPursuit = true;
        returnTicks = RETURN_GIVE_UP_TICKS;
    }

    public void clearUtilityWork() {
        if (workIsUtilityJob) {
            clearWork();
        }
    }

    public boolean isWorking() {
        return stand.utilityAnchor != null;
    }

    public boolean isUtilityWorking() {
        return stand.utilityAnchor != null && workIsUtilityJob;
    }

    public boolean hasArrivedAtWork() {
        return stand.utilityAnchor != null
                && stand.position().distanceToSqr(stand.utilityAnchor) <= UTILITY_ARRIVAL * UTILITY_ARRIVAL;
    }

    void tickUtilityWork() {
        Vec3 toAnchor = stand.utilityAnchor.subtract(stand.position());
        double distance = toAnchor.length();

        if (distance <= 1.0E-3) {
            stand.setPos(stand.utilityAnchor.x, stand.utilityAnchor.y, stand.utilityAnchor.z);
        } else {
            Vec3 step = toAnchor.scale(Math.min(utilityStepSpeed, distance) / distance);
            stand.setPos(stand.getX() + step.x, stand.getY() + step.y, stand.getZ() + step.z);
        }

        stand.setDeltaMovement(Vec3.ZERO);
        clearFollowMotion();
    }

    private static final float GUARDED_HEALTH = 0.3F;

    private static final double GUARDED_CLOSE = 0.65;

    Vec3 followAnchor(Player owner) {
        if (stand.getStandType().form().isBound()) {
            return owner.position();
        }

        Vec3 look = owner.getLookAngle();
        Vec3 forward = new Vec3(look.x, 0, look.z).normalize();
        Vec3 right = new Vec3(-forward.z, 0, forward.x);

        StandMovementProfile profile = stand.movement();
        boolean front = stand.synced(StandEntity.DATA_FRONT_STANCE);
        double height = stand.synced(StandEntity.DATA_GUARDING) ? GUARD_HEIGHT : profile.verticalOffset();

        if (owner.isCrouching()) {
            height -= profile.crouchDrop();
        }

        double behind = profile.followDistance();
        if (owner.isSprinting()) {
            behind *= 1.0 - profile.sprintLean();
        }
        if (!owner.onGround()) {
            behind *= profile.fallCatchUp();
        }

        if (stand.synced(StandEntity.DATA_GUARDING)) {
            return owner.position().add(stand.guardDirection(owner, forward).scale(GUARD_OFFSET))
                    .add(0, height, 0);
        }

        if (front) {
            double ahead = profile.attackForward();
            if (stand.synced(StandEntity.DATA_BARRAGING)) {
                ahead = Math.max(0.0, ahead - StandEntity.BARRAGE_PULL_IN);
            }

            double side = stand.synced(StandEntity.DATA_STRIKING) ? STRIKE_SIDE : profile.attackSide();
            return owner.position()
                    .add(forward.scale(ahead))
                    .add(right.scale(side))
                    .add(0, height, 0);
        }

        if (stand.synced(StandEntity.DATA_COMBAT_STANCE)) {
            return owner.position()
                    .add(forward.scale(profile.attackForward() * COMBAT_REACH_FRACTION))
                    .add(right.scale(profile.attackSide()))
                    .add(0, height, 0);
        }

        if (StandMode.fromOrdinal(stand.synced(StandEntity.DATA_MODE)).handlesItems()) {
            return owner.position().add(right.scale(UTILITY_SIDE_OFFSET)).add(0, height, 0);
        }

        Vector3f placed = stand.placement();
        double close = owner.getHealth() < owner.getMaxHealth() * GUARDED_HEALTH ? GUARDED_CLOSE : 1.0;
        return owner.position()
                .add(right.scale((profile.sideOffset() + placed.x()) * close))
                .add(forward.scale((placed.z() - behind) * close))
                .add(0, height + placed.y(), 0);
    }

    private void faceOwner(Player owner) {
        float turn = stand.movement().rotationSpeed();
        float placed = stand.restingBehindOwner() && !stand.getStandType().form().isBound() ? stand.placementTurn() : 0F;
        float yaw = Mth.approachDegrees(stand.getYRot(), owner.getYRot() + placed, turn);
        float headYaw = Mth.approachDegrees(stand.getYHeadRot(), owner.getYHeadRot() + placed, turn);

        stand.setYRot(yaw);
        stand.setYBodyRot(yaw);
        stand.setYHeadRot(headYaw);

        stand.setXRot(Mth.approachDegrees(stand.getXRot(), owner.getXRot(), turn));
    }

    void tickReturn(Player owner) {
        Vec3 anchor = followAnchor(owner);
        moveTowardsAtSpeed(anchor, RETURN_SPEED);
        faceOwner(owner);

        if (stand.position().distanceTo(anchor) <= RETURN_ARRIVAL_DISTANCE) {
            stand.returningFromPursuit = false;
            returnTicks = 0;
            return;
        }

        if (returnTicks > 0 && --returnTicks == 0) {
            snapToOwner(anchor);
            stand.returningFromPursuit = false;
        }
    }

    void lockToOwner(Player owner) {
        stand.setPos(owner.getX(), owner.getY() + PARTIAL_HEIGHT_OFFSET, owner.getZ());
        stand.xo = owner.xo;
        stand.yo = owner.yo + PARTIAL_HEIGHT_OFFSET;
        stand.zo = owner.zo;
        clearFollowMotion();

        stand.setYRot(owner.getYRot());
        stand.setYBodyRot(owner.yBodyRot);
        stand.setYHeadRot(owner.getYHeadRot());
        stand.yRotO = owner.yRotO;
        stand.yBodyRotO = owner.yBodyRotO;
        stand.yHeadRotO = owner.yHeadRotO;
    }

    void tickPursuit(Player owner) {
        Vec3 center = stand.pursuitTarget.position().add(0, stand.pursuitTarget.getBbHeight() * 0.5, 0);
        double distance = stand.position().distanceTo(center);

        if (stand.huntingSelfDirected && (stand.comboStage != StandEntity.ComboStage.NONE || distance <= StandEntity.ENGAGE_RANGE + 0.5)
                && stand.tickCombo(owner)) {
            return;
        }

        if (distance > StandEntity.ENGAGE_RANGE) {
            moveTowardsAtSpeed(center, stand.huntingSelfDirected ? HUNT_SPEED : PURSUIT_SPEED);
        } else {
            stand.orbitAngle += ORBIT_SPEED;
            Vec3 station = center.add(Math.cos(stand.orbitAngle) * StandEntity.ENGAGE_RANGE, 0, Math.sin(stand.orbitAngle) * StandEntity.ENGAGE_RANGE);
            moveTowardsAtSpeed(station, ENGAGE_STRAFE_SPEED);
        }

        faceTarget();

        if (!stand.huntingSelfDirected) {
            stand.tickCommandedStrikes(owner, distance);
            return;
        }

        if (distance <= StandEntity.ENGAGE_RANGE + 0.5 && stand.attackCooldown <= 0) {
            stand.performAttack(owner);
        }
    }

    public void reachTowards(Vec3 point) {
        Vec3 face = point.subtract(stand.position());
        if (face.horizontalDistanceSqr() > 1.0E-4) {
            float yaw = (float) (Mth.atan2(face.z, face.x) * (180.0 / Math.PI)) - 90.0F;
            stand.setYRot(yaw);
            stand.setYBodyRot(yaw);
            stand.setYHeadRot(yaw);
        }

        stand.triggerAnim(StandEntity.CONTROLLER, "grab");
        stand.punchStanceTicksRemaining = StandEntity.PUNCH_STANCE_TICKS;
    }

    void faceTowards(LivingEntity target) {
        Vec3 face = target.position().subtract(stand.position());
        float yaw = (float) (Mth.atan2(face.z, face.x) * (180.0 / Math.PI)) - 90.0F;
        stand.setYRot(yaw);
        stand.setYBodyRot(yaw);
        stand.setYHeadRot(yaw);
    }

    void faceOwnerlessIdle() {
    }

    void faceTarget() {
        Vec3 face = stand.pursuitTarget.position().subtract(stand.position());
        float yaw = (float) (Mth.atan2(face.z, face.x) * (180.0 / Math.PI)) - 90.0F;
        stand.setYRot(yaw);
        stand.setYBodyRot(yaw);
        stand.setYHeadRot(yaw);
    }

    public void interceptAt(Vec3 point, boolean usePunch2) {
        Vec3 approach = point.subtract(stand.position());
        double distance = approach.length();
        Vec3 stopAt = distance > INTERCEPT_STANDOFF
                ? point.subtract(approach.scale(INTERCEPT_STANDOFF / distance))
                : stand.position();

        stand.setPos(stopAt.x, stopAt.y, stopAt.z);
        clearFollowMotion();

        float yaw = (float) (Mth.atan2(approach.z, approach.x) * (180.0 / Math.PI)) - 90.0F;
        stand.setYRot(yaw);
        stand.setYBodyRot(yaw);
        stand.setYHeadRot(yaw);

        stand.triggerAnim(StandEntity.CONTROLLER, stand.strikeAnim(usePunch2));
        stand.takeAttackStance();
    }

    private static final double INTERCEPT_STANDOFF = 0.9;

    void endEngagement() {
        stand.comboStage = StandEntity.ComboStage.NONE;
        stand.comboStageTicks = 0;
        stand.pursuitTarget = null;
        beginReturn();
        stand.huntingSelfDirected = false;
        stand.attacksThisEngagement = 0;
        stand.attackCooldown = 0;
        stand.commandedStrikes = 0;
        stand.retargetDelay = RETARGET_DELAY_TICKS;

        if (stand.barrageHitsRemaining > 0) {
            stand.barrageHitsRemaining = 0;
            stand.stopFlurryAnimation();
        }
    }

    void moveTowardsAtSpeed(Vec3 target, double speed) {
        clearFollowMotion();

        Vec3 delta = target.subtract(stand.position());
        double distance = delta.length();
        if (distance <= speed) {
            stand.setPos(target);
            return;
        }

        stand.setPos(stand.position().add(delta.scale(speed / distance)));
    }

    public StandMovementState movementState() {
        if (stand.isDismissing() || !stand.hasOutlivedEmergingWindow()) return StandMovementState.SUMMON;
        if (stand.isHolding()) return StandMovementState.HOLDING;
        if (stand.isPiloted()) return StandMovementState.PILOTED;
        if (stand.isInhaling()) return StandMovementState.WORKING;
        if (stand.isEngaged()) return StandMovementState.PURSUING;
        if (stand.isGuarding()) return StandMovementState.GUARDING;
        if (stand.isBarraging()) return StandMovementState.BARRAGE;
        if (stand.synced(StandEntity.DATA_FRONT_STANCE)) return StandMovementState.ATTACK;
        if (stand.synced(StandEntity.DATA_COMBAT_STANCE)) return StandMovementState.COMBAT;

        Player owner = stand.getOwner();
        boolean moving = owner != null && owner.getDeltaMovement().horizontalDistanceSqr() > 1.0E-4;
        return moving ? StandMovementState.FOLLOW : StandMovementState.IDLE;
    }

    void clearFollowMotion() {
        followVelocity = Vec3.ZERO;
        barrageSway = Vec3.ZERO;
        stand.swayStrength = 0F;
        stand.combatHoldTicks = 0;
        stand.setHeldBarrage(false);
    }

    private static final double FOLLOW_LEASH = 8.0;

    private static final double CARRY_FROM = 0.3;

    private void snapToOwner(Vec3 anchor) {
        followVelocity = Vec3.ZERO;
        barrageSway = Vec3.ZERO;
        stand.moveTo(anchor.x, anchor.y, anchor.z, stand.getYRot(), stand.getXRot());
        stand.setOldPosAndRot();
    }

    private void springTo(Vec3 target, double stiffness, double damping) {
        Vec3 toTarget = target.subtract(stand.position());
        Vec3 acceleration = toTarget.scale(stiffness).subtract(followVelocity.scale(damping));
        followVelocity = followVelocity.add(acceleration);
        stand.setPos(stand.position().add(followVelocity));
    }
}
