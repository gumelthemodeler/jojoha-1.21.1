package org.gumel.jojoha.stand;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.registry.ModItems;
import org.joml.Vector3f;
import java.util.List;

final class StandPoses {
    private final StandEntity stand;

    StandPoses(StandEntity stand) {
        this.stand = stand;
    }

    private static final double INHALE_STAND_OFFSET = 2.6;

    private static final double INHALE_STEP_SPEED = 0.55;

    private static final double INHALE_ARRIVAL = 0.12;

    private float inhaleYaw;

    void tickInhaleHold() {
        if (stand.inhaleAnchor == null) {
            stand.inhaleAnchor = stand.position();
        }

        Vec3 toAnchor = stand.inhaleAnchor.subtract(stand.position());
        double distance = toAnchor.length();

        if (distance <= INHALE_ARRIVAL) {
            stand.setPos(stand.inhaleAnchor.x, stand.inhaleAnchor.y, stand.inhaleAnchor.z);
        } else {
            Vec3 step = toAnchor.scale(Math.min(INHALE_STEP_SPEED, distance) / distance);
            stand.setPos(stand.getX() + step.x, stand.getY() + step.y, stand.getZ() + step.z);
        }

        stand.setDeltaMovement(Vec3.ZERO);

        float eased = Mth.approachDegrees(stand.getYRot(), inhaleYaw, stand.movement().rotationSpeed());
        stand.setYRot(eased);
        stand.setYBodyRot(eased);
        stand.setYHeadRot(eased);
    }

    public void triggerLeapPose() {
        stand.triggerAnim(StandEntity.CONTROLLER, "stand_jump");
    }

    private static final int CATCH_HOLD_TICKS = 7;
    private static final double THROW_BACK_RANGE = 48.0;
    private static final double THROW_BACK_ARC = 0.15;
    private static final float THROW_BACK_SPEED = 2.2F;
    private static final int TOSS_FADE_AGE = 6000 - 40;
    private static final double TOSS_SIDE = 0.32;
    private static final double TOSS_LIFT = 0.28;
    private static final double TOSS_FORWARD = 0.08;
    private static final double HAND_SIDE = 0.35;
    private static final double HAND_HEIGHT = 1.3;
    private static final double HAND_FORWARD = 0.45;

    private CompoundTag tossing;
    private Entity tossAt;
    private int tossIn;

    public void catchProjectile(Projectile projectile) {
        CompoundTag caught = projectile.saveWithoutId(new CompoundTag());
        caught.putString("id", EntityType.getKey(projectile.getType()).toString());
        if (projectile instanceof AbstractArrow arrow) {
            arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        }
        projectile.discard();
        stand.triggerAnim(StandEntity.CONTROLLER, "catch");
        stand.takeAttackStance();
        stand.sync(StandEntity.DATA_CAUGHT, caught);
        tossing = caught;
        tossAt = projectile.getOwner();
        tossIn = CATCH_HOLD_TICKS;
    }

    void tickCatch() {
        if (tossIn <= 0 || --tossIn > 0 || !(stand.level() instanceof ServerLevel level)) {
            return;
        }
        stand.sync(StandEntity.DATA_CAUGHT, new CompoundTag());
        LivingEntity target = tossAt instanceof LivingEntity shooter && shooter.isAlive() && shooter.level() == level
                && shooter.distanceToSqr(stand) < THROW_BACK_RANGE * THROW_BACK_RANGE ? shooter : null;
        tossAt = null;
        if (target != null) {
            stand.faceTowards(target);
        }
        float yaw = stand.getYRot() * Mth.DEG_TO_RAD;
        Vec3 forward = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
        Vec3 right = new Vec3(-forward.z, 0, forward.x);
        Vec3 hand = stand.position().add(right.scale(HAND_SIDE)).add(forward.scale(HAND_FORWARD)).add(0, HAND_HEIGHT, 0);
        Vec3 fling = right.scale(TOSS_SIDE).add(forward.scale(TOSS_FORWARD)).add(0, TOSS_LIFT, 0);
        Entity thrown = EntityType.create(tossing, level).orElse(null);
        tossing = null;
        if (thrown instanceof AbstractArrow arrow) {
            arrow.moveTo(hand.x, hand.y, hand.z, arrow.getYRot(), arrow.getXRot());
            if (target != null) {
                arrow.setOwner(stand.getOwner());
                Vec3 aim = target.getEyePosition().subtract(hand);
                double across = Math.sqrt(aim.x * aim.x + aim.z * aim.z);
                arrow.shoot(aim.x, aim.y + across * THROW_BACK_ARC, aim.z, THROW_BACK_SPEED, 0F);
            } else {
                arrow.setBaseDamage(0.0);
                arrow.setDeltaMovement(fling);
            }
            level.addFreshEntity(arrow);
        } else if (thrown instanceof ItemSupplier supplier && !supplier.getItem().isEmpty()) {
            ItemEntity tossed = new ItemEntity(level, hand.x, hand.y, hand.z, supplier.getItem().copy(),
                    fling.x, fling.y, fling.z);
            CompoundTag tag = tossed.saveWithoutId(new CompoundTag());
            tag.putShort("Age", (short) TOSS_FADE_AGE);
            tag.putShort("PickupDelay", Short.MAX_VALUE);
            tossed.load(tag);
            level.addFreshEntity(tossed);
        }
        level.playSound(null, hand.x, hand.y, hand.z, SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.6F, 0.7F);
    }

    public void triggerStarFingerPose(LivingEntity target) {
        stand.faceTowards(target);
        stand.sync(StandEntity.DATA_FINGER_TARGET, target.getId());
        Vec3 chest = target.position().add(0, target.getBbHeight() * FINGER_AIM_HEIGHT, 0);
        stand.sync(StandEntity.DATA_FINGER_POINT, new Vector3f((float) chest.x, (float) chest.y, (float) chest.z));
        stand.triggerAnim(StandEntity.CONTROLLER, "star_finger");
        stand.takeAttackStance();
    }

    public void triggerStarFingerAt(Vec3 point) {
        stand.sync(StandEntity.DATA_FINGER_TARGET, -1);
        stand.sync(StandEntity.DATA_FINGER_POINT, new Vector3f((float) point.x, (float) point.y, (float) point.z));
        stand.triggerStrikeForward("star_finger");
    }

    public static final float FINGER_AIM_HEIGHT = 0.6F;

    public Vec3 fingerGoal(float partialTick) {
        int id = stand.synced(StandEntity.DATA_FINGER_TARGET);
        if (id >= 0 && stand.level().getEntity(id) instanceof LivingEntity target && target.isAlive()) {
            return target.getPosition(partialTick).add(0, target.getBbHeight() * FINGER_AIM_HEIGHT, 0);
        }
        Vector3f point = stand.synced(StandEntity.DATA_FINGER_POINT);
        return point.lengthSquared() == 0F ? null : new Vec3(point.x, point.y, point.z);
    }

    public void beginInhale(Player owner, int ticks) {
        holdInhale(owner, ticks);
        triggerInhalePose();
    }

    public void holdInhale(Player owner, int ticks) {
        if (stand.pursuitTarget != null || stand.returningFromPursuit) {
            stand.endEngagement();
            stand.returningFromPursuit = false;
        }

        Vec3 look = owner.getLookAngle();
        Vec3 forward = new Vec3(look.x, 0, look.z);
        forward = forward.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : forward.normalize();

        stand.inhaleAnchor = owner.position().add(forward.scale(INHALE_STAND_OFFSET))
                .add(0, stand.movement().verticalOffset(), 0);
        inhaleYaw = (float) (Mth.atan2(forward.z, forward.x) * (180F / Math.PI)) - 90F;
        stand.inhaleHoldTicks = Math.max(stand.inhaleHoldTicks, ticks);
    }

    public void releaseInhale(int overrunTicks) {
        if (stand.inhaleHoldTicks > 0) {
            stand.inhaleHoldTicks = Math.min(stand.inhaleHoldTicks, Math.max(1, overrunTicks));
        }
    }

    public Vec3 inhaleAnchor() {
        return stand.inhaleAnchor;
    }

    private static final double LEFT_HAND_X = 6.0;

    private static final double LEFT_HAND_Y = 17.0;

    private static final double LEFT_HAND_Z = 0.66667;

    private static final double UNIT = 1.0 / 16.0;

    public Vec3 leftHandAnchor() {
        Vec3 look = stand.getLookAngle();
        Vec3 forward = new Vec3(look.x, 0.0, look.z);
        forward = forward.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : forward.normalize();

        Vec3 left = new Vec3(forward.z, 0.0, -forward.x);
        double scale = stand.getScale();

        return stand.position()
                .add(left.scale(LEFT_HAND_X * UNIT * scale))
                .add(0.0, LEFT_HAND_Y * UNIT * scale, 0.0)
                .subtract(forward.scale(LEFT_HAND_Z * UNIT * scale));
    }

    public void triggerGroundBreakerPose() {
        stand.triggerAnim(StandEntity.CONTROLLER, "ground_breaker");
    }

    public void triggerBeatdownPose() {
        stand.triggerAnim(StandEntity.CONTROLLER, "beatdown");
    }

    public void triggerThrowPose() {
        stand.triggerAnim(StandEntity.CONTROLLER, "throw");
    }

    public void triggerKnifeThrowPose() {
        stand.triggerAnim(StandEntity.CONTROLLER, "knife_throw");
    }

    public void triggerKnifeVolleyPose(int knives) {
        int count = Math.max(1, Math.min(knives, StandEntity.KNIFE_VOLLEYS.length));
        stand.triggerAnim(StandEntity.CONTROLLER, "knife_volley_" + count);
    }

    public int knivesHeld() {
        return stand.synced(StandEntity.DATA_KNIVES);
    }

    public void setKnivesHeld(int count) {
        stand.sync(StandEntity.DATA_KNIVES, Math.max(0, count));
        if (count <= 0) {
            stand.sync(StandEntity.DATA_KNIFE_KINDS, "");
        }
    }

    public void setKnivesHeld(List<ItemStack> knives) {
        StringBuilder kinds = new StringBuilder();
        for (ItemStack knife : knives) {
            if (kinds.length() > 0) {
                kinds.append(',');
            }
            kinds.append(BuiltInRegistries.ITEM.getKey(knife.getItem()));
        }
        stand.sync(StandEntity.DATA_KNIFE_KINDS, kinds.toString());
        stand.sync(StandEntity.DATA_KNIVES, knives.size());
    }

    public ItemStack knifeHeld(int index) {
        String[] kinds = stand.synced(StandEntity.DATA_KNIFE_KINDS).split(",");
        if (index >= 0 && index < kinds.length && !kinds[index].isEmpty()) {
            ResourceLocation id = ResourceLocation.tryParse(kinds[index]);
            if (id != null && BuiltInRegistries.ITEM.containsKey(id)) {
                return new ItemStack(BuiltInRegistries.ITEM.get(id));
            }
        }
        return new ItemStack(ModItems.DAGGER_IRON.get());
    }

    public boolean isInhaling() {
        return stand.synced(StandEntity.DATA_INHALING);
    }

    public void triggerInhalePose() {
        stand.triggerAnim(StandEntity.CONTROLLER, "inhale");
        stand.punchStanceTicksRemaining = StandEntity.PUNCH_STANCE_TICKS;
    }

    private record TimeStopPose(String animation, int keyTicks, int lengthTicks) {
    }

    private static final List<TimeStopPose> TIMESTOP_POSES = List.of(
            new TimeStopPose("timestop_1", 10, 15),
            new TimeStopPose("timestop_2", 8, 16));

    private static final int OWN_TIMESTOP_KEY_TICKS = 10;
    private static final int OWN_TIMESTOP_TICKS = 15;

    private String timeStopPose;

    public boolean isPosing() {
        return stand.synced(StandEntity.DATA_POSING);
    }

    public boolean isCharging() {
        return stand.synced(StandEntity.DATA_CHARGING);
    }

    private static final int CHARGE_STANCE_TIMEOUT_TICKS = 100;

    private int chargingTicksLeft;

    public void setCharging(boolean charging) {
        chargingTicksLeft = charging ? CHARGE_STANCE_TIMEOUT_TICKS : 0;
        stand.sync(StandEntity.DATA_CHARGING, charging);
    }

    void tickChargingStance() {
        if (chargingTicksLeft > 0 && --chargingTicksLeft == 0) {
            stand.sync(StandEntity.DATA_CHARGING, false);
        }
    }

    private static final int FREEZE_HOLD_TICKS = 20;

    private TimeStopPose chosenTimeStopPose;

    private TimeStopPose pickTimeStopPose() {
        String own = StandTypes.timeStopAnimation(stand.getStandType());
        return own != null ? new TimeStopPose(own, OWN_TIMESTOP_KEY_TICKS, OWN_TIMESTOP_TICKS)
                : TIMESTOP_POSES.get(stand.getRandom().nextInt(TIMESTOP_POSES.size()));
    }

    public int chooseTimeStopPose() {
        chosenTimeStopPose = pickTimeStopPose();
        return chosenTimeStopPose.keyTicks();
    }

    public int triggerTimeStopPose() {
        TimeStopPose pose = chosenTimeStopPose != null ? chosenTimeStopPose : pickTimeStopPose();
        chosenTimeStopPose = null;
        chargeLoop = null;
        timeStopPose = pose.animation();
        stand.triggerAnim(StandEntity.CONTROLLER, timeStopPose);
        stand.timeStopPoseTicks = Math.max(pose.lengthTicks(), pose.keyTicks() + FREEZE_HOLD_TICKS);
        return pose.keyTicks();
    }

    void tickTimeStopPose() {
        if (stand.timeStopPoseTicks > 0 && --stand.timeStopPoseTicks == 0) {
            releaseTimeStopPose();
        }
    }

    public void stopTimeStopPose() {
        stand.timeStopPoseTicks = 0;
        releaseTimeStopPose();
    }

    private String chargeLoop;

    public boolean startChargeLoop() {
        chargeLoop = StandTypes.chargeAnimation(stand.getStandType());
        if (chargeLoop == null) {
            return false;
        }
        stand.triggerAnim(StandEntity.CONTROLLER, chargeLoop);
        return true;
    }

    public void stopChargeLoop() {
        if (chargeLoop != null) {
            stand.stopTriggeredAnim(StandEntity.CONTROLLER, chargeLoop);
            chargeLoop = null;
        }
    }

    private void releaseTimeStopPose() {
        if (timeStopPose != null) {
            stand.stopTriggeredAnim(StandEntity.CONTROLLER, timeStopPose);
            timeStopPose = null;
        }
    }
}
