package org.gumel.jojoha.stand.passive;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.StandSummonHandler;

/**
 * The Stand swats down something thrown at its user - once, and then it needs a moment.
 *
 * <p>The cooldown is the entire balance of this passive. Deflecting everything continuously makes
 * its user immune to ranged damage outright, which is both stronger than any active move in the kit
 * and quietly removes skeletons, pillagers and blazes from the game. Batting one shot aside and then
 * being briefly open turns it into a reprieve instead of an immunity: a single archer is neutralised,
 * a firing line is not.
 *
 * <p>Only projectiles actually closing on the user are eligible. A dot-product test against their
 * direction of travel is what makes that distinction - something merely passing nearby, or already
 * moving away, is left alone, and it also stops the reflex being wasted on a shot that was never
 * going to connect. Projectiles the user fired themselves are skipped as well, or a bow would be
 * unusable while a Stand is out.
 *
 * <p>Deflected rather than deleted: the shot is sent back the way it came and reassigned to the
 * user, so whoever fired it has to deal with it. Making it simply vanish reads as the projectile
 * failing rather than as the Stand doing something.
 */
public final class EnhancedReflexesPassive implements StandPassive {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "enhanced_reflexes");
    public static final EnhancedReflexesPassive INSTANCE = new EnhancedReflexesPassive();

    /** Deliberately short - this is a reflex, not a bubble. */
    private static final double RADIUS = 4.0;
    /** How head-on the approach must be. ~0.3 is a generous cone without catching passers-by. */
    private static final double CLOSING_THRESHOLD = 0.3;

    /**
     * How long the Stand is caught out after a deflection.
     *
     * <p>Shortened by the Stand's Protection, which the design doc defines as scaling how long it
     * can block for its user - a defensive stat lowering a defensive cooldown. Floored so no amount
     * of investment turns the reflex back into a permanent shield.
     */
    private static final int BASE_COOLDOWN_TICKS = 70;
    private static final int MIN_COOLDOWN_TICKS = 30;
    private static final float COOLDOWN_PER_PROTECTION = 1.6F;

    /** How hard the shot is sent back. Faster than it arrived, so it clearly leaves. */
    private static final double RETURN_SPEED = 1.2;

    /** Alternated so consecutive deflections do not replay the identical swing. */
    private boolean deflectUsesPunch2;

    private EnhancedReflexesPassive() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "passive.jojoha.enhanced_reflexes";
    }

    @Override
    public void tick(ServerPlayer player, JojohaPlayerData data) {
        long now = player.level().getGameTime();
        if (data.isMoveOnCooldown(ID, now)) {
            return;
        }

        Projectile incoming = firstIncoming(player);
        if (incoming == null) {
            return;
        }

        deflect(player, incoming);
        data.setMoveCooldown(ID, now, cooldownTicks(data));

        // The Stand goes and hits it. Without this the shot simply turns round in mid-air with
        // nothing visibly causing it, which reads as the projectile glitching rather than as the
        // Stand having done something.
        StandEntity stand = StandSummonHandler.findStand(player, data);
        if (stand != null) {
            stand.interceptAt(incoming.position(), deflectUsesPunch2);
            deflectUsesPunch2 = !deflectUsesPunch2;
        }
    }

    /**
     * The nearest projectile genuinely on its way in, or null.
     *
     * <p>Nearest rather than first found, because only one is deflected per reflex and it should be
     * the one about to land - picking arbitrarily out of a volley would swat the shot with the most
     * time left on it and let the imminent one through.
     */
    private static Projectile firstIncoming(ServerPlayer player) {
        AABB area = player.getBoundingBox().inflate(RADIUS);
        Vec3 chest = player.position().add(0, player.getBbHeight() * 0.5, 0);

        Projectile best = null;
        double bestDistance = Double.MAX_VALUE;

        for (Projectile projectile : player.serverLevel().getEntitiesOfClass(Projectile.class, area,
                candidate -> candidate.isAlive() && candidate.getOwner() != player)) {

            Vec3 velocity = projectile.getDeltaMovement();
            if (velocity.lengthSqr() < 1.0E-4) {
                continue;
            }

            Vec3 toPlayer = chest.subtract(projectile.position());
            double distance = toPlayer.length();
            if (distance < 1.0E-4) {
                continue;
            }

            // Positive only when it is travelling toward the user rather than past them.
            if (velocity.normalize().dot(toPlayer.scale(1.0 / distance)) < CLOSING_THRESHOLD) {
                continue;
            }

            if (distance < bestDistance) {
                bestDistance = distance;
                best = projectile;
            }
        }

        return best;
    }

    private static void deflect(ServerPlayer player, Projectile projectile) {
        ServerLevel level = player.serverLevel();
        Vec3 returned = projectile.getDeltaMovement().normalize().scale(-RETURN_SPEED);

        projectile.setDeltaMovement(returned);
        // Rotation is not derived from velocity on its own, so without this the shot flies backwards
        // while still pointing the way it was going.
        projectile.setYRot((float) (Math.atan2(returned.x, returned.z) * (180.0 / Math.PI)));
        projectile.setXRot((float) (Math.atan2(returned.y, returned.horizontalDistance()) * (180.0 / Math.PI)));
        projectile.hasImpulse = true;

        // Reassigned so the return trip can hurt whoever fired it - a projectile cannot hit its own
        // owner, and while it still belonged to the shooter it would pass straight through them.
        projectile.setOwner(player);

        Vec3 at = projectile.position();
        level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 8, 0.12, 0.12, 0.12, 0.06);
        level.playSound(null, projectile.blockPosition(), SoundEvents.SHIELD_BLOCK,
                SoundSource.PLAYERS, 0.6F, 1.7F);
    }

    private static int cooldownTicks(JojohaPlayerData data) {
        int reduction = Math.round(data.stand.protection() * COOLDOWN_PER_PROTECTION);
        return Math.max(MIN_COOLDOWN_TICKS, BASE_COOLDOWN_TICKS - reduction);
    }
}
