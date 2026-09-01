package org.gumel.jojoha.hamon.moves;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.hamon.HamonMove;

import java.util.List;

/**
 * Hermit Path's starter move: a short-range Hamon burst that damages and knocks back
 * everything in front of the player. Placeholder numbers, not balanced content.
 */
public final class RipplePulseMove implements HamonMove {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "ripple_pulse");
    public static final RipplePulseMove INSTANCE = new RipplePulseMove();

    private static final double RANGE = 3.5;
    private static final float DAMAGE = 4.0F;
    private static final double KNOCKBACK = 1.2;
    private static final int COOLDOWN_TICKS = 40;
    private static final float ENERGY_COST = 20F;

    private RipplePulseMove() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public int cooldownTicks() {
        return COOLDOWN_TICKS;
    }

    @Override
    public float energyCost() {
        return ENERGY_COST;
    }

    @Override
    public void activate(ServerPlayer player, JojohaPlayerData data) {
        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle();
        AABB area = player.getBoundingBox().inflate(RANGE);

        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != player && entity.isAlive() && player.distanceTo(entity) <= RANGE);

        for (LivingEntity target : targets) {
            target.hurt(level.damageSources().playerAttack(player), DAMAGE);
            Vec3 push = target.position().subtract(player.position()).normalize().scale(KNOCKBACK);
            target.setDeltaMovement(target.getDeltaMovement().add(push.x, 0.35, push.z));
            target.hurtMarked = true;
        }

        level.sendParticles(ParticleTypes.SWEEP_ATTACK,
                player.getX() + look.x * 1.5, player.getY() + player.getEyeHeight() * 0.5, player.getZ() + look.z * 1.5,
                6, 0.4, 0.2, 0.4, 0.0);
        level.playSound(null, player.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.6F, 1.6F);
    }
}
