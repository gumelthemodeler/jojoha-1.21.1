package org.gumel.jojoha.entity.ai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import org.gumel.jojoha.entity.RisenZombie;

import java.util.EnumSet;

public class MasterHurtByTargetGoal extends TargetGoal {

    private final RisenZombie body;

    private int lastSeen;

    public MasterHurtByTargetGoal(RisenZombie body) {
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

        LivingEntity attacker = raiser.getLastHurtByMob();
        int when = raiser.getLastHurtByMobTimestamp();
        if (attacker == null || when == lastSeen) {
            return false;
        }

        if (attacker == raiser || body.sharesMasterWith(attacker)) {
            return false;
        }
        return canAttack(attacker, TargetingConditions.DEFAULT);
    }

    @Override
    public void start() {
        LivingEntity raiser = body.masterEntity();
        if (raiser == null) {
            return;
        }
        mob.setTarget(raiser.getLastHurtByMob());
        lastSeen = raiser.getLastHurtByMobTimestamp();
        super.start();
    }
}
