package org.gumel.jojoha.stand.skill;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.PlayerEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.StandSummonHandler;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class PilotSystem {
    public static final double LEASH = 26.0;

    private static final double SPEED = 0.55;

    private static final double ACCELERATION = 0.18;
    private static final double DRAG = 0.91;

    private static final double REST = 1.0E-3;
    private static final double RECALL_SPEED = 0.9;

    private static final Set<UUID> FLYING = new HashSet<>();

    private PilotSystem() {
    }

    public static boolean anyoneFlying() {
        return !FLYING.isEmpty();
    }

    public static StandEntity flownBy(ServerPlayer player) {
        if (!FLYING.contains(player.getUUID())) {
            return null;
        }
        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (!data.standPiloting) {
            FLYING.remove(player.getUUID());
            return null;
        }
        return StandSummonHandler.findStand(player, data);
    }

    public static void begin(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        data.standPiloting = true;
        if (data.halfSummon) {
            data.halfSummon = false;
            stand.setTrustTier(data.manifestTier());
        }
        FLYING.add(player.getUUID());
        PlayerDataAccess.set(player, data);
        stand.beginPiloting();
    }

    public static void init() {
        EntityEvent.LIVING_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer player) {
                JojohaPlayerData data = PlayerDataAccess.get(player);
                if (data.standPiloting) {
                    stop(player, data);
                }
            }
            return EventResult.pass();
        });
        PlayerEvent.PLAYER_QUIT.register(
                player -> FLYING.remove(player.getUUID()));
    }

    public static void applyClientPose(ServerPlayer player, double x, double y, double z,
                                       float yRot, float xRot) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (!data.standPiloting) {
            return;
        }

        StandEntity stand = StandSummonHandler.findStand(player, data);
        if (stand == null) {
            stop(player, data);
            return;
        }

        player.setYRot(yRot);
        player.setXRot(Mth.clamp(xRot, -90F, 90F));
        player.setYHeadRot(yRot);

        Vec3 fromUser = new Vec3(x, y, z).subtract(player.position());
        if (fromUser.lengthSqr() > LEASH * LEASH) {
            Vec3 clamped = player.position().add(fromUser.normalize().scale(LEASH));
            x = clamped.x;
            y = clamped.y;
            z = clamped.z;
        }

        if (!clear(stand, stand.getBoundingBox().move(new Vec3(x, y, z).subtract(stand.position())))
                && clear(stand, stand.getBoundingBox())) {
            return;
        }

        stand.setPos(x, y, z);
        stand.setYRot(yRot);
        stand.setXRot(xRot);
        stand.setYBodyRot(yRot);
        stand.setYHeadRot(yRot);

        stand.setDeltaMovement(Vec3.ZERO);
    }

    public static void advance(Player player, StandEntity stand, float forward, float strafe,
                               boolean up, boolean down) {
        stand.setYRot(player.getYRot());
        stand.setXRot(player.getXRot());
        stand.setYBodyRot(player.getYRot());
        stand.setYHeadRot(player.getYRot());

        Vec3 heading = heading(player, forward, strafe, up, down);
        Vec3 wanted = heading.lengthSqr() < 1.0E-6 ? Vec3.ZERO
                : heading.normalize().scale(SPEED);

        Vec3 previous = stand.pilotVelocity();
        Vec3 velocity = previous.scale(DRAG).add(wanted.subtract(previous).scale(ACCELERATION));

        if (velocity.lengthSqr() < REST * REST) {
            velocity = Vec3.ZERO;
        }

        velocity = leash(player, stand, velocity);
        velocity = solid(stand, velocity);
        stand.setPilotVelocity(velocity);

        stand.setDeltaMovement(Vec3.ZERO);

        stand.move(MoverType.SELF, velocity);
    }

    private static Vec3 solid(StandEntity stand, Vec3 velocity) {
        if (velocity.lengthSqr() < REST * REST) {
            return velocity;
        }

        AABB box = stand.getBoundingBox();
        if (!clear(stand, box) || clear(stand, box.move(velocity))) {
            return velocity;
        }

        double x = clear(stand, box.move(velocity.x, 0.0, 0.0)) ? velocity.x : 0.0;
        double y = clear(stand, box.move(x, velocity.y, 0.0)) ? velocity.y : 0.0;
        double z = clear(stand, box.move(x, y, velocity.z)) ? velocity.z : 0.0;
        return new Vec3(x, y, z);
    }

    private static boolean clear(StandEntity stand, AABB box) {
        for (VoxelShape shape
                : stand.level().getBlockCollisions(stand, box)) {
            if (!shape.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static Vec3 leash(Player player, StandEntity stand, Vec3 velocity) {
        Vec3 fromUser = stand.position().subtract(player.position());
        if (fromUser.lengthSqr() < LEASH * LEASH) {
            return velocity;
        }

        Vec3 outward = fromUser.normalize();
        double leaving = velocity.dot(outward);
        return leaving <= 0 ? velocity : velocity.subtract(outward.scale(leaving));
    }

    private static Vec3 heading(Player player, float forward, float strafe, boolean up, boolean down) {
        Vec3 ahead = player.getLookAngle();

        float yaw = (float) Math.toRadians(player.getYRot());
        Vec3 left = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));

        double lift = (up ? 1 : 0) - (down ? 1 : 0);
        return ahead.scale(forward).add(left.scale(strafe)).add(0, lift, 0);
    }

    public static void stop(ServerPlayer player, JojohaPlayerData data) {
        if (!data.standPiloting) {
            return;
        }

        data.standPiloting = false;
        FLYING.remove(player.getUUID());
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);

        StandEntity stand = StandSummonHandler.findStand(player, data);
        if (stand != null) {
            stand.endPiloting();
        }
    }

    public static double recallSpeed() {
        return RECALL_SPEED;
    }
}
