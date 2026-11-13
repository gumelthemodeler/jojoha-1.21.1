package org.gumel.jojoha.stand;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

final class StandHolding {
    private final StandEntity stand;

    StandHolding(StandEntity stand) {
        this.stand = stand;
    }

    private static final int GRAB_HOLD_TICKS = 120;

    private int gripTicksRemaining = 0;

    public void showHeldItem(ItemStack stack, int ticks) {
        stand.sync(StandEntity.DATA_HELD_ITEM, stack.copyWithCount(1));
        stand.heldItemTicks = Math.max(stand.heldItemTicks, ticks);
    }

    public void triggerGrabAt(LivingEntity target) {
        stand.faceTowards(target);
        stand.triggerAnim(StandEntity.CONTROLLER, "grab");
        stand.takeAttackStance();
    }

    public boolean grab(LivingEntity target) {
        if (!seize(target)) {
            return false;
        }
        triggerGrabAt(target);
        return true;
    }

    public boolean seize(LivingEntity target) {
        if (stand.grip != null) {
            return false;
        }
        stand.grip = StandGrip.take(stand, target);
        if (stand.grip == null) {
            return false;
        }
        gripTicksRemaining = GRAB_HOLD_TICKS;
        stand.faceTowards(target);
        return true;
    }

    public boolean carry(LivingEntity target) {
        if (stand.grip != null) {
            return false;
        }
        stand.grip = StandGrip.take(stand, target);
        if (stand.grip == null) {
            return false;
        }
        stand.gripCarried = true;
        stand.faceTowards(target);
        return true;
    }

    public boolean isCarrying() {
        return stand.grip != null && stand.gripCarried;
    }

    public boolean isCradling() {
        return stand.synced(StandEntity.DATA_HOLD_POSE) == StandEntity.HOLD_FETCH;
    }

    public boolean isHolding() {
        return stand.grip != null;
    }

    public LivingEntity heldEntity() {
        return stand.grip == null ? null : stand.grip.held();
    }

    public void releaseHeld(Vec3 velocity) {
        if (stand.grip == null) {
            return;
        }
        stand.grip.release(velocity);
        stand.grip = null;
        if (stand.gripCarried) {
            stand.gripCarried = false;
            return;
        }
        stand.takeAttackStance();
    }

    void tickGrip() {
        if (stand.grip == null) {
            return;
        }
        if (!stand.grip.stillHeld(stand)) {
            stand.grip = null;
            stand.gripCarried = false;
            return;
        }
        if (stand.grip.outlivedTheStop()) {
            releaseHeld(Vec3.ZERO);
            return;
        }
        if (!stand.gripCarried && !stand.grip.frozenInStoppedTime() && --gripTicksRemaining <= 0) {
            releaseHeld(Vec3.ZERO);
            return;
        }
        stand.grip.tick(stand);
        if (!stand.gripCarried) {
            stand.combatHoldTicks = StandEntity.COMBAT_HOLD_TICKS;
        }
    }
}
