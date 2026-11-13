package org.gumel.jojoha.stand.skill;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.data.JojohaPlayerData;

public final class Heading {
    public static final double MOVING_THRESHOLD = 0.04;

    private Heading() {
    }

    public static Vec3 of(ServerPlayer player, JojohaPlayerData data) {
        Vec3 look = flat(player.getLookAngle());

        Vec3 held = MoveInput.heading(player);
        if (held != null) {
            if (held.lengthSqr() > 1.0E-8) {
                return held;
            }
            return look.lengthSqr() < 1.0E-4 ? Vec3.ZERO : look.normalize();
        }

        if (player.isSprinting() && look.lengthSqr() > 1.0E-4) {
            return look.normalize();
        }

        if (data.recentMoveTicks > 0) {
            Vec3 travelled = new Vec3(data.recentMoveX, 0, data.recentMoveZ);
            if (travelled.lengthSqr() > 1.0E-8) {
                return travelled.normalize();
            }
        }

        return look.lengthSqr() < 1.0E-4 ? Vec3.ZERO : look.normalize();
    }

    public static Vec3 away(ServerPlayer player, JojohaPlayerData data) {
        Vec3 forward = of(player, data);
        return forward.lengthSqr() < 1.0E-8 ? Vec3.ZERO : forward.scale(-1);
    }

    private static Vec3 flat(Vec3 vector) {
        return new Vec3(vector.x, 0, vector.z);
    }
}
