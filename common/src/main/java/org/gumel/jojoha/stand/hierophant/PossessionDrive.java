package org.gumel.jojoha.stand.hierophant;

import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class PossessionDrive {
    private static final double PLAYER_WALK = 0.2;
    private static final double MOB_SLOWEST = 0.12;
    private static final double MOB_FASTEST = 0.3;
    private static final double SPRINT = 1.3;
    private static final double SNEAK = 0.3;

    private static final double GRIP = 0.45;
    private static final double AIR_GRIP = 0.08;

    private static final double JUMP = 0.42;
    private static final double GRAVITY = 0.08;
    private static final double DRAG = 0.98;

    private PossessionDrive() {
    }

    public static Vec3 step(LivingEntity body, Vec3 velocity, float forward, float strafe, boolean jump,
                            boolean sneak, boolean sprint, float yaw) {
        double speed = speedOf(body) * (sneak ? SNEAK : sprint && forward > 0F ? SPRINT : 1.0);
        float yawRad = yaw * Mth.DEG_TO_RAD;
        double sin = Mth.sin(yawRad);
        double cos = Mth.cos(yawRad);
        double length = Math.sqrt(forward * forward + strafe * strafe);
        double f = length > 1.0 ? forward / length : forward;
        double s = length > 1.0 ? strafe / length : strafe;
        double wantX = (s * cos - f * sin) * speed;
        double wantZ = (f * cos + s * sin) * speed;

        double grip = body.onGround() ? GRIP : AIR_GRIP;
        double x = velocity.x + (wantX - velocity.x) * grip;
        double z = velocity.z + (wantZ - velocity.z) * grip;

        double y;
        if (body.isInWater() || body.isInLava()) {
            y = jump ? Math.min(velocity.y + 0.04, 0.12) : Math.max(-0.1, velocity.y - 0.02);
            x *= 0.8;
            z *= 0.8;
        } else if (body.onGround()) {
            y = jump ? JUMP + jumpBoost(body) : -GRAVITY;
        } else {
            y = (velocity.y - GRAVITY) * DRAG;
        }

        Vec3 wanted = new Vec3(x, y, z);
        Vec3 before = body.position();
        body.move(MoverType.SELF, wanted);
        Vec3 moved = body.position().subtract(before);

        return new Vec3(
                Math.abs(moved.x - wanted.x) > 1.0E-4 ? moved.x : x,
                body.verticalCollision ? 0.0 : y,
                Math.abs(moved.z - wanted.z) > 1.0E-4 ? moved.z : z);
    }

    private static double speedOf(LivingEntity body) {
        if (body instanceof Player) {
            return PLAYER_WALK;
        }
        double own = body.getAttributes().hasAttribute(Attributes.MOVEMENT_SPEED)
                ? body.getAttributeValue(Attributes.MOVEMENT_SPEED) : PLAYER_WALK;
        return Mth.clamp(own, MOB_SLOWEST, MOB_FASTEST);
    }

    private static double jumpBoost(LivingEntity body) {
        return body.hasEffect(MobEffects.JUMP)
                ? 0.1 * (body.getEffect(MobEffects.JUMP).getAmplifier() + 1) : 0.0;
    }
}
