package org.gumel.jojoha.stand;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * What a Stand is being aimed at.
 *
 * <p>One implementation on purpose. The crosshair indicator and the move that actually swings used
 * to run separate copies of this, which is a recipe for the two disagreeing: the marker would say a
 * mob was targeted and the move would report nothing in range, or the reverse. Since it lives in
 * common code, both sides now ask exactly the same question and get exactly the same answer.
 *
 * <p>Aiming happens in three passes, in descending order of confidence:
 * <ol>
 *   <li>the ray is cut short at the first solid block, so nothing behind cover can be picked;</li>
 *   <li>a strict ray, which is what the player is literally pointing at;</li>
 *   <li>failing that, the entity nearest the line of sight within a small cone.</li>
 * </ol>
 *
 * <p>That third pass is the part that makes this feel reliable rather than fussy. A strict ray on a
 * hitbox demands more precision than a fast fight allows, and missing by a pixel produced a move
 * that silently did nothing - so a near miss is treated as a hit, provided the target is genuinely
 * in front and actually visible.
 */
public final class StandAim {
    /** Default reach - the Stand's own strike range. */
    public static final double DEFAULT_REACH = 7.0;

    /**
     * How far off the crosshair the fallback will still accept, as a dot product against the look
     * vector. About 0.995 is a couple of degrees: forgiving enough to rescue a near miss, tight
     * enough that it never picks something the player clearly was not aiming at.
     */
    private static final double AIM_CONE = 0.995;

    private StandAim() {
    }

    public static LivingEntity lookTarget(Player player) {
        return lookTarget(player, DEFAULT_REACH);
    }

    public static LivingEntity lookTarget(Player player, double reach) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);

        double clearReach = reachBeforeCover(player, eye, look, reach);
        Vec3 end = eye.add(look.scale(clearReach));

        // Grown from the ray itself rather than from the player's own box, which is not where the
        // ray starts - aiming steeply up or down used to leave the search volume behind the shot.
        AABB search = new AABB(eye, end).inflate(1.0);

        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, end, search,
                candidate -> isValidTarget(player, candidate), clearReach * clearReach);
        if (hit != null && hit.getEntity() instanceof LivingEntity living) {
            return living;
        }

        return nearestInCone(player, eye, look, clearReach);
    }

    /**
     * The distance to the first solid block, or the full reach if nothing is in the way.
     *
     * <p>Without this a Stand would happily strike through a wall, since an entity ray does not
     * consult terrain at all.
     */
    private static double reachBeforeCover(Player player, Vec3 eye, Vec3 look, double reach) {
        HitResult block = player.level().clip(new ClipContext(eye, eye.add(look.scale(reach)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));

        return block.getType() == HitResult.Type.MISS ? reach : eye.distanceTo(block.getLocation());
    }

    /** The valid target closest to the line of sight, provided it is in front and visible. */
    private static LivingEntity nearestInCone(Player player, Vec3 eye, Vec3 look, double reach) {
        AABB search = player.getBoundingBox().inflate(reach);
        LivingEntity best = null;
        double bestAlignment = AIM_CONE;

        for (LivingEntity candidate : player.level().getEntitiesOfClass(LivingEntity.class, search,
                entity -> isValidTarget(player, entity))) {

            Vec3 toTarget = candidate.getBoundingBox().getCenter().subtract(eye);
            double distance = toTarget.length();
            if (distance > reach || distance < 1.0E-4) {
                continue;
            }

            double alignment = look.dot(toTarget.scale(1.0 / distance));
            if (alignment <= bestAlignment) {
                continue;
            }

            // Checked last because it is the expensive one, and only for a candidate that has
            // already earned it by being the best aligned so far.
            if (!player.hasLineOfSight(candidate)) {
                continue;
            }

            bestAlignment = alignment;
            best = candidate;
        }

        return best;
    }

    /**
     * Stands are excluded explicitly.
     *
     * <p>They are deliberately unpickable to vanilla's own attack ray, but this runs its own filter
     * and would otherwise lock straight onto the user's Stand hovering at their shoulder.
     */
    private static boolean isValidTarget(Player player, Entity candidate) {
        return candidate instanceof LivingEntity living
                && living.isAlive()
                && candidate != player
                && !(candidate instanceof StandEntity)
                && !candidate.isSpectator();
    }
}
