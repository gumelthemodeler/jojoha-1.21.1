package org.gumel.jojoha.stand.passive;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;

public final class DeadeyePassive implements StandPassive {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "deadeye");

    public static final DeadeyePassive INSTANCE = new DeadeyePassive();

    private static final double NEAR = 8.0;
    private static final double FAR = 24.0;

    private static final float MOST = 0.25F;

    private DeadeyePassive() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "passive.jojoha.deadeye";
    }

    public static float bonus(double distance) {
        return MOST * (float) Mth.clamp((distance - NEAR) / (FAR - NEAR), 0.0, 1.0);
    }

    @Override
    public float onRangedHit(ServerPlayer player, JojohaPlayerData data, LivingEntity target, Vec3 from,
                             float amount) {
        float bonus = bonus(from.distanceTo(target.getBoundingBox().getCenter()));
        if (bonus <= 0F) {
            return amount;
        }
        player.serverLevel().sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() * 0.6,
                target.getZ(), 2 + Math.round(bonus * 24F), 0.2, 0.25, 0.2, 0.25);
        return amount * (1F + bonus);
    }
}
