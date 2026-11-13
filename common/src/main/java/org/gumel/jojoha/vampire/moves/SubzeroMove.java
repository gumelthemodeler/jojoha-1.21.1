package org.gumel.jojoha.vampire.moves;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.combat.fx.MovementFx;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.stand.skill.StandBeat;
import org.gumel.jojoha.vampire.VampireFx;
import org.gumel.jojoha.vampire.VampireMove;
import org.gumel.jojoha.vampire.VampirePoses;
import org.gumel.jojoha.vampire.VampireState;

public final class SubzeroMove implements VampireMove {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "subzero");
    public static final SubzeroMove INSTANCE = new SubzeroMove();

    private static final int FROZEN_TICKS = 140;

    public static final int STANCE_TICKS = 21;
    private static final int COOLDOWN_TICKS = 420;
    private static final float ENERGY_COST = 35F;

    private SubzeroMove() {
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
    public boolean isSustained() {
        return true;
    }

    @Override
    public boolean isSustainActive(ServerPlayer player) {
        return VampireState.isFrozen(player);
    }

    @Override
    public void release(ServerPlayer player, JojohaPlayerData data) {
        if (!VampireState.isFrozen(player)) {
            return;
        }
        VampireState.thaw(player);
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        MovementFx.drop(player);
        NetworkHandler.sendSubzero(player, 0);

        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.SNOWFLAKE,
                player.getX(), player.getY() + 1.0, player.getZ(), 20, 0.4, 0.8, 0.4, 0.03);
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_HURT_FREEZE,
                SoundSource.PLAYERS, 0.5F, 1.6F);
    }

    @Override
    public void activate(ServerPlayer player, JojohaPlayerData data) {
        VampireState.freeze(player, FROZEN_TICKS);

        MovementFx.hold(player, VampirePoses.SUBZERO);

        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, FROZEN_TICKS, 1,
                false, true, true));

        NetworkHandler.sendSubzero(player, FROZEN_TICKS);

        StandBeat.at(STANCE_TICKS, player, () -> {
            if (VampireState.isFrozen(player)) {
                frostOver(player.serverLevel(), player);
            }
        });
    }

    private static void frostOver(ServerLevel level, ServerPlayer player) {
        for (int i = 0; i < 4; i++) {
            VampireFx.ring(level, player.getX(), player.getY() + 0.3 + i * 0.45, player.getZ(),
                    1.1, 1);
        }
        VampireFx.blood(level, player.getX(), player.getY() + 1.0, player.getZ(), 14, 0.3, 0.02);

        level.sendParticles(ParticleTypes.SNOWFLAKE,
                player.getX(), player.getY() + 1.0, player.getZ(), 80, 0.45, 0.9, 0.45, 0.02);
        level.playSound(null, player.blockPosition(), SoundEvents.GLASS_BREAK,
                SoundSource.PLAYERS, 0.6F, 0.6F);
    }
}
