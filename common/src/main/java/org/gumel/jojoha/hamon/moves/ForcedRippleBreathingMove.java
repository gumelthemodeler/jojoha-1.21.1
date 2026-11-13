package org.gumel.jojoha.hamon.moves;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.combat.fx.MovementFx;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.hamon.HamonCombat;
import org.gumel.jojoha.hamon.HamonFx;
import org.gumel.jojoha.hamon.HamonMove;
import org.gumel.jojoha.hamon.HamonState;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.registry.ModSounds;

public final class ForcedRippleBreathingMove implements HamonMove {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "forced_ripple_breathing");
    public static final ForcedRippleBreathingMove INSTANCE = new ForcedRippleBreathingMove();

    public static final float HEAL_PER_BEAT = 1.5F;
    public static final int BEAT_TICKS = 20;

    public static final float DRAIN_PER_TICK = 0.5F;

    public static final double REACH = 4.0;

    private static final float ENERGY_COST = 15F;

    private ForcedRippleBreathingMove() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public int cooldownTicks() {
        return 0;
    }

    @Override
    public float energyCost() {
        return ENERGY_COST;
    }

    @Override
    public boolean isSustained() {
        return true;
    }

    @Override
    public boolean isSustainActive(ServerPlayer player) {
        return HamonState.isBreathing(player);
    }

    @Override
    public void release(ServerPlayer player, JojohaPlayerData data) {
        stop(player);
    }

    public static void stop(ServerPlayer player) {
        HamonState.setBreathing(player, false);
        NetworkHandler.clearFirstPersonPose(player);
        MovementFx.drop(player);
    }

    @Override
    public boolean activate(ServerPlayer player, JojohaPlayerData data) {
        ServerLevel level = player.serverLevel();
        HamonState.setBreathing(player, true);
        NetworkHandler.sendFirstPersonPose(player, "heal");
        MovementFx.hold(player, MovementFx.HEAL_HOLD);

        HamonFx.ring(level, player.getX(), player.getY() + 1.0, player.getZ(), 1.5, 1);
        level.playSound(null, player.blockPosition(), ModSounds.HAMON_BREATHE.get(),
                SoundSource.PLAYERS, 0.6F, 1.2F);
        HamonFx.chargeSound(level, player, 0.5F, 1.2F);
        return true;
    }

    public static void beat(ServerPlayer player) {
        ServerLevel level = player.serverLevel();

        LivingEntity mending = player;
        double nearest = REACH * REACH;
        AABB probe = player.getBoundingBox().inflate(REACH);
        for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, probe,
                entity -> entity != player && entity.isAlive()
                        && !HamonCombat.isUndead(entity))) {
            double away = candidate.distanceToSqr(player);
            if (away < nearest && candidate.getHealth() < candidate.getMaxHealth()) {
                nearest = away;
                mending = candidate;
            }
        }

        mending.heal(HEAL_PER_BEAT);

        mending.removeEffect(MobEffects.POISON);
        mending.removeEffect(MobEffects.WITHER);

        HamonFx.ring(level, mending.getX(), mending.getY() + mending.getBbHeight() * 0.5,
                mending.getZ(), 1.1, 1);
        HamonFx.fineMote(level, mending.getX(), mending.getY() + mending.getBbHeight() * 0.6,
                mending.getZ(), 6, 0.3);
    }
}
