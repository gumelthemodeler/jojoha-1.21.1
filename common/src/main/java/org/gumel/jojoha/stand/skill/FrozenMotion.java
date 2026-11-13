package org.gumel.jojoha.stand.skill;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class FrozenMotion {
    private FrozenMotion() {
    }

    public static void settle(Entity entity) {
        double x = entity.lerpTargetX();
        double y = entity.lerpTargetY();
        double z = entity.lerpTargetZ();
        float yRot = entity.lerpTargetYRot();
        float xRot = entity.lerpTargetXRot();
        if (x == entity.getX() && y == entity.getY() && z == entity.getZ()
                && yRot == entity.getYRot() && xRot == entity.getXRot()) {
            return;
        }

        entity.setPos(x, y, z);
        entity.setYRot(yRot);
        entity.setXRot(xRot);
        entity.lerpTo(x, y, z, yRot, xRot, 0);
    }

    public static void hold(Entity entity) {
        entity.xOld = entity.getX();
        entity.yOld = entity.getY();
        entity.zOld = entity.getZ();

        entity.xo = entity.getX();
        entity.yo = entity.getY();
        entity.zo = entity.getZ();

        entity.xRotO = entity.getXRot();
        entity.yRotO = entity.getYRot();
    }

    public static void carry(Entity vehicle, Entity passenger) {
        passenger.setOldPosAndRot();
        vehicle.positionRider(passenger);
        for (Entity rider : passenger.getPassengers()) {
            if (rider.getVehicle() == passenger && !(rider instanceof Player)) {
                carry(passenger, rider);
            }
        }
    }
}
