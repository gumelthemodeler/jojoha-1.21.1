package org.gumel.jojoha.combat;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

public final class Bystander {
    private static final int IN_THE_FIGHT_TICKS = 100;

    private Bystander() {
    }

    public static boolean innocent(Player attacker, LivingEntity body) {
        body = StandHits.bodyOf(body);
        if (body instanceof Enemy || body instanceof Player) {
            return false;
        }
        if (body instanceof Mob mob && mob.getTarget() == attacker) {
            return false;
        }
        if (body instanceof NeutralMob neutral && neutral.isAngryAt(attacker)) {
            return false;
        }
        return !(body.getLastHurtByMob() == attacker
                && body.tickCount - body.getLastHurtByMobTimestamp() < IN_THE_FIGHT_TICKS);
    }
}
