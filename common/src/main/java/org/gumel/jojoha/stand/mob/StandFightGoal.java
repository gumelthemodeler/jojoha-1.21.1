package org.gumel.jojoha.stand.mob;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.stand.StandRange;
import org.gumel.jojoha.stand.StandType;
import org.gumel.jojoha.stand.StandTypes;
import org.gumel.jojoha.stand.anubis.Anubis;

import java.util.EnumSet;

public final class StandFightGoal extends Goal {
    private static final double KEEP_NEAR = 8.0;
    private static final double KEEP_FAR = 14.0;
    private static final double CLOSE_IN = 4.0;
    private static final double BACK_OFF_SPEED = 1.2;
    private static final double APPROACH_SPEED = 1.0;
    private static final int REPATH_EVERY = 10;
    private static final int STRAFE_EVERY = 40;
    private static final float STRAFE_SPEED = 0.4F;

    private final PathfinderMob mob;
    private int repath;
    private int strafe;
    private float side = 1F;
    private boolean holding;

    public StandFightGoal(PathfinderMob mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private boolean longRange() {
        StandType type = StandTypes.byId(MobStands.standId(mob));
        return type != null && type.range() == StandRange.LONG;
    }

    private boolean meleeOnly() {
        return mob.getAttribute(Attributes.ATTACK_DAMAGE) != null && !(mob instanceof RangedAttackMob);
    }

    private boolean leads() {
        if (!MobStands.has(mob) || Anubis.ID.equals(MobStands.standId(mob)) || MobStands.foe(mob) == null
                || StandTypes.byId(MobStands.standId(mob)) == null) {
            return false;
        }
        return longRange() ? !(mob instanceof RangedAttackMob) : !meleeOnly();
    }

    @Override
    public boolean canUse() {
        return leads();
    }

    @Override
    public boolean canContinueToUse() {
        return leads();
    }

    @Override
    public void start() {
        repath = 0;
        strafe = 0;
        holding = false;
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
        mob.getMoveControl().strafe(0F, 0F);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity foe = MobStands.foe(mob);
        if (foe == null) {
            return;
        }
        mob.getLookControl().setLookAt(foe, 30F, 30F);
        double gap = mob.distanceTo(foe);
        boolean sees = mob.getSensing().hasLineOfSight(foe);
        if (holding) {
            if (--strafe <= 0) {
                strafe = STRAFE_EVERY;
                side = mob.getRandom().nextBoolean() ? 1F : -1F;
            }
            mob.getMoveControl().strafe(0F, side * STRAFE_SPEED);
        }
        if (--repath > 0) {
            return;
        }
        repath = REPATH_EVERY;
        holding = false;
        if (!longRange()) {
            if (gap > CLOSE_IN || !sees) {
                mob.getNavigation().moveTo(foe, APPROACH_SPEED);
            } else {
                mob.getNavigation().stop();
            }
            return;
        }
        if (gap < KEEP_NEAR) {
            Vec3 away = DefaultRandomPos.getPosAway(mob, (int) KEEP_FAR, 4, foe.position());
            if (away != null) {
                mob.getNavigation().moveTo(away.x, away.y, away.z, BACK_OFF_SPEED);
                return;
            }
        }
        if (gap > KEEP_FAR || !sees) {
            mob.getNavigation().moveTo(foe, APPROACH_SPEED);
            return;
        }
        mob.getNavigation().stop();
        holding = true;
    }
}
