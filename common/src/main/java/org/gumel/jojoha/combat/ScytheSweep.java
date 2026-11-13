package org.gumel.jojoha.combat;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.combat.fx.MovementFx;
import org.gumel.jojoha.stand.skill.StandBeat;
import org.gumel.jojoha.config.JojohaServerConfig;
import org.gumel.jojoha.stand.StandEntity;

public final class ScytheSweep {
    public static final int COOLDOWN_TICKS = 50;

    public static final float DAMAGE = 6F;

    public static final float RADIUS = 3F;

    private static final double KNOCKBACK = 0.6;

    private static final int WEAR = 2;

    private static final int RING_POINTS = 12;

    private ScytheSweep() {
    }

    public static int cooldownTicks() {
        return JojohaServerConfig.SCYTHE_COOLDOWN.whole();
    }

    public static void perform(ServerPlayer player, ItemStack scythe) {
        ServerLevel level = player.serverLevel();
        double radius = JojohaServerConfig.SCYTHE_RADIUS.value();
        float damage = JojohaServerConfig.SCYTHE_DAMAGE.value();

        MovementFx.pose(player, MovementFx.SCYTHE_SWEEP);
        player.swing(InteractionHand.MAIN_HAND, true);
        scythe.hurtAndBreak(WEAR, player, EquipmentSlot.MAINHAND);
        ring(level, player, radius);
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 1.0F, 0.8F);

        StandBeat.at(MovementFx.SCYTHE_SWEEP_CONTACT_TICKS, player, () -> reap(player, radius, damage));
    }

    private static void reap(ServerPlayer player, double radius, float damage) {
        ServerLevel level = player.serverLevel();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(radius, 0.5, radius),
                candidate -> reaped(player, candidate, radius))) {
            target.hurt(level.damageSources().playerAttack(player), DamageScale.combat(player, target, damage));
            Vec3 away = target.position().subtract(player.position()).multiply(1.0, 0.0, 1.0);
            if (away.lengthSqr() > 1.0E-4) {
                away = away.normalize();
                target.knockback(KNOCKBACK, -away.x, -away.z);
            }
        }
    }

    private static boolean reaped(ServerPlayer player, LivingEntity candidate, double radius) {
        return candidate != player && candidate.isAlive() && !(candidate instanceof StandEntity)
                && !player.isAlliedTo(candidate)
                && !(candidate instanceof ArmorStand stand && stand.isMarker())
                && !(candidate instanceof OwnableEntity owned && owned.getOwner() == player)
                && candidate.distanceToSqr(player) <= radius * radius;
    }

    private static void ring(ServerLevel level, ServerPlayer player, double radius) {
        double y = player.getY() + player.getBbHeight() * 0.5;
        double reach = radius * 0.7;
        for (int i = 0; i < RING_POINTS; i++) {
            double angle = Math.PI * 2.0 * i / RING_POINTS;
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, player.getX() + Math.cos(angle) * reach, y,
                    player.getZ() + Math.sin(angle) * reach, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }
}
