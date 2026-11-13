package org.gumel.jojoha.client;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.stand.StandEntity;

public final class StandAim {

    private StandAim() {
    }

    public static float[] angles(StandEntity stand, Vec3 aimAt,
                                 float partialTick) {
        if (aimAt == null) {
            return null;
        }

        double dx = aimAt.x - stand.getX();
        double dy = aimAt.y - stand.getEyeY();
        double dz = aimAt.z - stand.getZ();
        double flat = Math.sqrt(dx * dx + dz * dz);
        if (flat < 1.0E-4) {
            return null;
        }

        float wanted = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90F;
        float body = Mth.rotLerp(partialTick, stand.yBodyRotO, stand.yBodyRot);

        return new float[]{
                -Mth.wrapDegrees(wanted - body),
                (float) (Mth.atan2(dy, flat) * Mth.RAD_TO_DEG),
        };
    }

    public static float[] angles(StandEntity stand, Entity target, float partialTick) {
        return target == null ? null
                : angles(stand, target.position().add(0, target.getBbHeight() * 0.5, 0), partialTick);
    }
}
