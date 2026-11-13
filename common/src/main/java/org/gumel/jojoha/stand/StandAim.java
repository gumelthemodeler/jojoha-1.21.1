package org.gumel.jojoha.stand;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.combat.OwnPets;
import org.gumel.jojoha.combat.StandHits;
import org.gumel.jojoha.stand.skill.PilotSystem;

public final class StandAim {
    public static final double DEFAULT_REACH = 7.0;

    private static final double AIM_CONE = 0.995;

    private StandAim() {
    }

    public static LivingEntity lookTarget(Player player) {
        return lookTarget(player, DEFAULT_REACH);
    }

    public static Vec3 eye(Player player) {
        StandEntity flown = flown(player);
        return flown != null ? flown.getEyePosition() : player.getEyePosition();
    }

    public static Vec3 feet(Player player) {
        StandEntity flown = flown(player);
        return flown != null ? flown.position() : player.position();
    }

    public static StandEntity flown(Player player) {
        return player instanceof ServerPlayer served
                ? PilotSystem.flownBy(served) : null;
    }

    public static LivingEntity lookTarget(Player player, double reach) {
        return lookTarget(player, reach, false);
    }

    public static LivingEntity lookTarget(Player player, double reach, boolean standsToo) {
        Vec3 eye = eye(player);
        Vec3 look = player.getViewVector(1.0F);

        double clearReach = reachBeforeCover(player, eye, look, reach);
        Vec3 end = eye.add(look.scale(clearReach));

        AABB search = new AABB(eye, end).inflate(1.0);

        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, end, search,
                candidate -> isValidTarget(player, candidate, standsToo), clearReach * clearReach);
        if (hit != null && hit.getEntity() instanceof LivingEntity living) {
            return living;
        }

        return nearestInCone(player, eye, look, reach, standsToo);
    }

    private static double reachBeforeCover(Player player, Vec3 eye, Vec3 look, double reach) {
        HitResult block = player.level().clip(new ClipContext(eye, eye.add(look.scale(reach)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));

        return block.getType() == HitResult.Type.MISS ? reach : eye.distanceTo(block.getLocation());
    }

    private static LivingEntity nearestInCone(Player player, Vec3 eye, Vec3 look, double reach, boolean standsToo) {
        AABB search = new AABB(eye, eye).inflate(reach);
        LivingEntity best = null;
        double bestAlignment = AIM_CONE;

        for (LivingEntity candidate : player.level().getEntitiesOfClass(LivingEntity.class, search,
                entity -> isValidTarget(player, entity, standsToo))) {

            Vec3 toTarget = candidate.getBoundingBox().getCenter().subtract(eye);
            double distance = toTarget.length();
            if (distance > reach || distance < 1.0E-4) {
                continue;
            }

            Vec3 toHead = candidate.getEyePosition().subtract(eye);
            double headDistance = toHead.length();
            double alignment = look.dot(toTarget.scale(1.0 / distance));
            if (headDistance > 1.0E-4) {
                alignment = Math.max(alignment, look.dot(toHead.scale(1.0 / headDistance)));
            }
            if (alignment <= bestAlignment) {
                continue;
            }

            if (!seen(player, eye, candidate)) {
                continue;
            }

            bestAlignment = alignment;
            best = candidate;
        }

        return best;
    }

    private static boolean seen(Player player, Vec3 eye, Entity candidate) {
        if (eye.equals(player.getEyePosition())) {
            return player.hasLineOfSight(candidate);
        }
        for (Vec3 to : new Vec3[]{candidate.getEyePosition(), candidate.getBoundingBox().getCenter()}) {
            if (player.level().clip(new ClipContext(eye, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                    player)).getType() == HitResult.Type.MISS) {
                return true;
            }
        }
        return false;
    }

    private static boolean isValidTarget(Player player, Entity candidate, boolean standsToo) {
        return candidate instanceof LivingEntity living
                && living.isAlive()
                && candidate != player
                && (standsToo ? StandHits.hittable(player, candidate)
                        : !(candidate instanceof StandEntity))
                && !OwnPets.tamedBy(player, candidate)
                && !candidate.isSpectator();
    }
}
