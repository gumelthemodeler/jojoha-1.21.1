package org.gumel.jojoha.stand.grapple;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class GrappleSwing {
    private static final double TAKE_UP = 0.62;

    private static final double REFUSE = 0.94;

    private static final double AIR_CONTROL = 0.058;

    private static final double PULL = 0.092;
    private static final double PULL_EASE = 4.0;

    private static final double PULL_STOP = 2.2;

    private static final double REEL_IN = 0.32;
    private static final double REEL_OUT = 0.28;

    private static final double MIN_LENGTH = 1.6;

    private static final double SWING_DRAG = 0.9985;

    private GrappleSwing() {
    }

    public static double tick(Player player, Vec3 anchor, double length,
                              boolean reelIn, boolean reelOut) {
        Vec3 from = player.getEyePosition();
        Vec3 toAnchor = anchor.subtract(from);
        double distance = toAnchor.length();

        if (distance < 1.0E-4) {
            return length;
        }

        Vec3 along = toAnchor.scale(1.0 / distance);
        Vec3 velocity = player.getDeltaMovement();

        if (reelIn) {
            length = Math.max(MIN_LENGTH, Math.min(length, distance) - REEL_IN);
        } else if (!reelOut && distance < length) {
            length = Math.max(MIN_LENGTH, distance);
        } else if (reelOut) {
            length = Math.min(HermitGrappleHook.MAX_RANGE, length + REEL_OUT);
        }

        if (distance > PULL_STOP) {
            double ease = Math.min(1.0, (distance - PULL_STOP) / PULL_EASE);
            velocity = velocity.add(along.scale(PULL * ease));
        }

        if (distance > length) {
            velocity = velocity.add(along.scale((distance - length) * TAKE_UP));

            double outward = -velocity.dot(along);
            if (outward > 0) {
                velocity = velocity.add(along.scale(outward * REFUSE));
            }

            velocity = velocity.scale(SWING_DRAG);
        }

        velocity = velocity.add(steer(player, along));

        player.setDeltaMovement(velocity);

        player.resetFallDistance();
        return length;
    }

    private static Vec3 steer(Player player, Vec3 along) {
        float forward = player.zza;
        float strafe = player.xxa;
        if (forward == 0F && strafe == 0F) {
            return Vec3.ZERO;
        }

        float yaw = player.getYRot() * ((float) Math.PI / 180F);
        Vec3 ahead = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
        Vec3 side = new Vec3(ahead.z, 0, -ahead.x);

        Vec3 push = ahead.scale(forward).add(side.scale(strafe));
        if (push.lengthSqr() < 1.0E-6) {
            return Vec3.ZERO;
        }

        push = push.normalize().scale(AIR_CONTROL);

        return push.subtract(along.scale(push.dot(along)));
    }
}
