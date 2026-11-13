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
import org.gumel.jojoha.combat.Defence;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.StandSummonHandler;
import org.gumel.jojoha.stand.skill.TimeStopCast;

public final class EnhancedReflexesPassive implements StandPassive {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "enhanced_reflexes");
    public static final EnhancedReflexesPassive INSTANCE = new EnhancedReflexesPassive();

    private static final double RADIUS = 4.0;
    private static final double CLOSING_THRESHOLD = 0.3;

    private static final int BASE_COOLDOWN_TICKS = 70;
    private static final int MIN_COOLDOWN_TICKS = 30;
    private static final float COOLDOWN_PER_PROTECTION = 1.6F;

    private static final double RETURN_SPEED = 1.2;

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
        if (TimeStopCast.isCasting(player)) {
            return;
        }

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

        StandEntity stand = StandSummonHandler.findStand(player, data);
        if (stand != null) {
            stand.interceptAt(incoming.position(), deflectUsesPunch2);
            deflectUsesPunch2 = !deflectUsesPunch2;
        }
    }

    private static Projectile firstIncoming(ServerPlayer player) {
        AABB area = player.getBoundingBox().inflate(RADIUS);
        Vec3 chest = player.position().add(0, player.getBbHeight() * 0.5, 0);

        Projectile best = null;
        double bestDistance = Double.MAX_VALUE;

        for (Projectile projectile : player.serverLevel().getEntitiesOfClass(Projectile.class, area,
                candidate -> candidate.isAlive()
                        && Defence.lastStand(player, candidate.getOwner()))) {

            Vec3 velocity = projectile.getDeltaMovement();
            if (velocity.lengthSqr() < 1.0E-4) {
                continue;
            }

            Vec3 toPlayer = chest.subtract(projectile.position());
            double distance = toPlayer.length();
            if (distance < 1.0E-4) {
                continue;
            }

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
        projectile.setYRot((float) (Math.atan2(returned.x, returned.z) * (180.0 / Math.PI)));
        projectile.setXRot((float) (Math.atan2(returned.y, returned.horizontalDistance()) * (180.0 / Math.PI)));
        projectile.hasImpulse = true;

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
