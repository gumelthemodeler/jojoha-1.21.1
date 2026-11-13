package org.gumel.jojoha.hamon.moves;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.hamon.HamonFx;
import org.gumel.jojoha.hamon.HamonMove;
import org.gumel.jojoha.hamon.HamonState;
import org.gumel.jojoha.network.NetworkHandler;

public final class RippleWalkMove implements HamonMove {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "ripple_walk");
    public static final RippleWalkMove INSTANCE = new RippleWalkMove();

    public static final double CLIMB_SPEED = 0.2;

    public static final float DRAIN_PER_TICK = 0.45F;

    private static final float ENERGY_COST = 10F;

    private RippleWalkMove() {
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
        return HamonState.isWalking(player);
    }

    @Override
    public void release(ServerPlayer player, JojohaPlayerData data) {
        HamonState.setWalking(player, false);
    }

    @Override
    public boolean activate(ServerPlayer player, JojohaPlayerData data) {
        ServerLevel level = player.serverLevel();
        HamonState.setWalking(player, true);

        NetworkHandler.sendFirstPersonPose(player, "gather");

        HamonFx.ring(level, player.getX(), player.getY() + 0.1, player.getZ(), 1.3, 1);
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_SWIM,
                SoundSource.PLAYERS, 0.5F, 1.6F);
        HamonFx.chargeSound(level, player, 0.4F, 1.3F);
        return true;
    }
}
