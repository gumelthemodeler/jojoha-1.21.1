package org.gumel.jojoha.stand.hierophant;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.stand.StandAim;
import org.gumel.jojoha.stand.StandEntity;

public final class HierophantAim {
    private HierophantAim() {
    }

    public static Vec3 point(ServerPlayer player, double reach) {
        LivingEntity target = StandAim.lookTarget(player, reach);
        if (target != null) {
            return target.getBoundingBox().getCenter();
        }
        BlockHitResult block = block(player, reach);
        if (block.getType() != HitResult.Type.MISS) {
            return block.getLocation();
        }
        return StandAim.eye(player).add(player.getLookAngle().scale(reach));
    }

    public static Vec3 crosshair(ServerPlayer player, double reach) {
        Vec3 eye = StandAim.eye(player);
        Vec3 look = player.getLookAngle();
        BlockHitResult block = block(player, reach);
        Vec3 end = block.getType() == HitResult.Type.MISS ? eye.add(look.scale(reach)) : block.getLocation();
        EntityHitResult body = ProjectileUtil
                .getEntityHitResult(player.level(), player, eye, end, new AABB(eye, end).inflate(1.0),
                        e -> e != player && e instanceof LivingEntity living && living.isAlive() && !living.isSpectator()
                                && !(e instanceof StandEntity), 0.3F);
        return body == null ? end : body.getEntity().getBoundingBox().inflate(0.3).clip(eye, end)
                .orElse(body.getEntity().getBoundingBox().getCenter());
    }

    public static BlockHitResult block(ServerPlayer player, double reach) {
        Vec3 eye = StandAim.eye(player);
        return player.level().clip(new ClipContext(eye, eye.add(player.getLookAngle().scale(reach)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
    }

    public static Vec3 hands(ServerPlayer player, StandEntity stand) {
        Vec3 look = player.getLookAngle();
        if (stand == null) {
            return player.getEyePosition().add(look.scale(0.6)).add(0.0, -0.3, 0.0);
        }
        return stand.position().add(0.0, stand.getBbHeight() * 0.7, 0.0).add(look.scale(0.5));
    }

    public static Vec3 waist(ServerPlayer player, StandEntity stand) {
        return stand == null ? player.position().add(0.0, 0.9, 0.0)
                : stand.position().add(0.0, stand.getBbHeight() * 0.45, 0.0);
    }

    public static Vec3 legs(ServerPlayer player, StandEntity stand) {
        return stand == null ? player.position().add(0.0, 0.5, 0.0)
                : stand.position().add(0.0, stand.getBbHeight() * 0.3, 0.0);
    }

    public static Vec3 planted(ServerPlayer player, StandEntity stand, double ahead) {
        if (stand == null) {
            return StandAim.feet(player);
        }
        Vec3 facing = Vec3.directionFromRotation(0.0F, stand.getYRot());
        return stand.position().add(facing.scale(ahead));
    }

    public static Vec3 turn(Vec3 v, Vec3 axis, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return v.scale(cos).add(axis.cross(v).scale(sin)).add(axis.scale(axis.dot(v) * (1.0 - cos)));
    }
}
