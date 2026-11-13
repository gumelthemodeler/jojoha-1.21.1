package org.gumel.jojoha.stand.hierophant;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.skill.StandBeat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PossessionMotion {
    private static final double GLIDE_SPEED = 6.0;

    private static final Map<UUID, Long> DIVING = new HashMap<>();

    private PossessionMotion() {
    }

    public static boolean diving(ServerPlayer user) {
        Long until = DIVING.get(user.getUUID());
        return until != null && user.level().getGameTime() < until;
    }

    public static void enter(ServerPlayer user, LivingEntity body, StandEntity stand) {
        DIVING.put(user.getUUID(), user.level().getGameTime() + HierophantClips.POSSESS_TICKS + 1);
        boolean alt = stand.getRandom().nextBoolean();
        String clip = alt ? HierophantClips.POSSESS_ALT : HierophantClips.POSSESS;
        double drop = alt ? HierophantClips.POSSESS_ALT_DROP : HierophantClips.POSSESS_DROP;
        Vec3 from = stand.position();
        stand.triggerStrikeAt(body, clip);

        StandBeat.during(1, HierophantClips.POSSESS_TICKS - 1, user, tick -> {
            StandEntity diving = StandEntity.findFor(user);
            if (diving == null || !body.isAlive() || body.isRemoved()) {
                return;
            }
            double t = ease(tick / (double) HierophantClips.POSSESS_TICKS);
            diving.sendToWork(from.lerp(meeting(diving, body, drop), t), GLIDE_SPEED);
            face(diving, body.position().subtract(from));
        });
        StandBeat.at(HierophantClips.POSSESS_TICKS, user, () -> {
            DIVING.remove(user.getUUID());
            StandEntity diving = StandEntity.findFor(user);
            if (diving == null) {
                return;
            }
            diving.clearWork();
            if (!body.isAlive() || body.isRemoved() || Possession.possessing(user)
                    || Possession.canTake(user, body) != null) {
                diving.stopTriggeredAnim(StandEntity.CONTROLLER, clip);
                return;
            }
            diving.moveTo(meeting(diving, body, drop));
            Possession.begin(user, body, diving);
        });
    }

    public static void exit(ServerPlayer user, Entity body, StandEntity stand) {
        Vec3 inside = meeting(stand, body, 0.0);
        stand.moveTo(inside.x, inside.y, inside.z, stand.getYRot(), stand.getXRot());
        stand.sendToWork(inside, GLIDE_SPEED);

        StandBeat.during(1, HierophantClips.EXIT_HIDDEN_TICKS - 1, user, tick -> {
            StandEntity leaving = StandEntity.findFor(user);
            if (leaving != null) {
                leaving.sendToWork(inside, GLIDE_SPEED);
            }
        });
        StandBeat.at(HierophantClips.EXIT_HIDDEN_TICKS, user, () -> {
            StandEntity leaving = StandEntity.findFor(user);
            if (leaving == null) {
                return;
            }
            leaving.setInvisible(false);
            face(leaving, user.position().subtract(inside));
            leaving.triggerStrikeAt(user, HierophantClips.POSSESS_EXIT);
        });
        StandBeat.at(HierophantClips.EXIT_HIDDEN_TICKS + HierophantClips.EXIT_EMERGE_TICKS, user, () -> {
            StandEntity leaving = StandEntity.findFor(user);
            if (leaving != null) {
                leaving.clearWork();
            }
        });
    }

    private static Vec3 meeting(StandEntity stand, Entity body, double drop) {
        Vec3 middle = body.getBoundingBox().getCenter();
        return middle.subtract(0.0, stand.getBbHeight() * 0.5 - drop, 0.0);
    }

    private static void face(StandEntity stand, Vec3 toward) {
        if (toward.horizontalDistanceSqr() < 1.0E-4) {
            return;
        }
        float yaw = (float) (Mth.atan2(toward.z, toward.x) * Mth.RAD_TO_DEG) - 90.0F;
        stand.setYRot(yaw);
        stand.setYBodyRot(yaw);
        stand.setYHeadRot(yaw);
    }

    private static double ease(double t) {
        double clamped = Mth.clamp(t, 0.0, 1.0);
        return clamped * clamped * (3.0 - 2.0 * clamped);
    }
}
