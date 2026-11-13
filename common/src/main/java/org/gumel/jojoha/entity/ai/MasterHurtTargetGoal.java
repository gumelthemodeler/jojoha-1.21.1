package org.gumel.jojoha.entity.ai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import org.gumel.jojoha.entity.RisenZombie;

import java.util.EnumSet;

public class MasterHurtTargetGoal extends TargetGoal {

    private final RisenZombie body;

    private int lastSeen;

    public MasterHurtTargetGoal(RisenZombie body) {
        super(body, false);
        this.body = body;
        setFlags(EnumSet.of(Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        LivingEntity raiser = body.masterEntity();
        if (raiser == null) {
            return false;
        }

        LivingEntity struck = raiser.getLastHurtMob();
        int when = raiser.getLastHurtMobTimestamp();
        if (struck == null || when == lastSeen) {
            return false;
        }

        if (struck == raiser || body.sharesMasterWith(struck)) {
            return false;
        }
        return canAttack(struck, TargetingConditions.DEFAULT);
    }

    @Override
    public void start() {
        LivingEntity raiser = body.masterEntity();
        if (raiser == null) {
            return;
        }
        mob.setTarget(raiser.getLastHurtMob());
        lastSeen = raiser.getLastHurtMobTimestamp();
        super.start();
    }
}
