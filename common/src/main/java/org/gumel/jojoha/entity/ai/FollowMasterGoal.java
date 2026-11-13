package org.gumel.jojoha.entity.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import org.gumel.jojoha.entity.RisenZombie;

import java.util.EnumSet;

public class FollowMasterGoal extends Goal {
    private static final double STOP = 4.0;

    private static final double TELEPORT = 22.0;

    private static final int REPATH_EVERY = 10;

    private final RisenZombie body;
    private final double speed;
    private final PathNavigation navigation;

    private LivingEntity master;
    private int repathIn;

    public FollowMasterGoal(RisenZombie body, double speed) {
        this.body = body;
        this.speed = speed;
        this.navigation = body.getNavigation();
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity raiser = body.masterEntity();
        if (raiser == null || raiser.isSpectator()) {
            return false;
        }
        if (body.getTarget() != null && body.getTarget().isAlive()) {
            return false;
        }
        if (body.distanceToSqr(raiser) < STOP * STOP) {
            return false;
        }
        this.master = raiser;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return master != null && master.isAlive() && !navigation.isDone()
                && body.distanceToSqr(master) > STOP * STOP
                && (body.getTarget() == null || !body.getTarget().isAlive());
    }

    @Override
    public void start() {
        repathIn = 0;
    }

    @Override
    public void stop() {
        master = null;
        navigation.stop();
    }

    @Override
    public void tick() {
        if (master == null) {
            return;
        }

        body.getLookControl().setLookAt(master, 10F, body.getMaxHeadXRot());
        if (--repathIn > 0) {
            return;
        }
        repathIn = REPATH_EVERY;

        if (body.isLeashed() || body.isPassenger()) {
            return;
        }

        if (body.distanceToSqr(master) >= TELEPORT * TELEPORT) {
            placeBeside(master);
            return;
        }
        navigation.moveTo(master, speed);
    }

    private void placeBeside(LivingEntity raiser) {
        BlockPos at = raiser.blockPosition();
        for (int attempt = 0; attempt < 10; attempt++) {
            int dx = body.getRandom().nextInt(7) - 3;
            int dz = body.getRandom().nextInt(7) - 3;
            BlockPos to = at.offset(dx, 0, dz);

            if (Math.abs(dx) < 2 && Math.abs(dz) < 2) {
                continue;
            }
            if (!body.level().noCollision(body, body.getBoundingBox().move(
                    to.getX() + 0.5 - body.getX(), to.getY() - body.getY(),
                    to.getZ() + 0.5 - body.getZ()))) {
                continue;
            }
            if (WalkNodeEvaluator.getPathTypeStatic(body, to)
                    != PathType.WALKABLE) {
                continue;
            }

            body.moveTo(to.getX() + 0.5, to.getY(), to.getZ() + 0.5, body.getYRot(), body.getXRot());
            navigation.stop();
            return;
        }
    }
}
