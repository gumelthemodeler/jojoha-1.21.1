package org.gumel.jojoha.vampire;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class VampireAim {
    private static final double STEP = 0.5;

    private VampireAim() {
    }

    public static LivingEntity firstAlong(ServerPlayer player, double reach, double radius) {
        ServerLevel level = player.serverLevel();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();

        for (double travelled = 0.5; travelled <= reach; travelled += STEP) {
            Vec3 at = eye.add(look.scale(travelled));
            AABB probe = new AABB(at.subtract(radius, radius, radius), at.add(radius, radius, radius));

            for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, probe,
                    entity -> entity != player && entity.isAlive())) {
                return candidate;
            }
        }
        return null;
    }

    public static List<LivingEntity> allAlong(ServerPlayer player, double reach, double radius) {
        ServerLevel level = player.serverLevel();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();

        List<LivingEntity> found = new ArrayList<>();
        for (double travelled = 0.5; travelled <= reach; travelled += STEP) {
            Vec3 at = eye.add(look.scale(travelled));
            AABB probe = new AABB(at.subtract(radius, radius, radius), at.add(radius, radius, radius));

            for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, probe,
                    entity -> entity != player && entity.isAlive())) {
                if (!found.contains(candidate)) {
                    found.add(candidate);
                }
            }
        }
        return found;
    }
}
