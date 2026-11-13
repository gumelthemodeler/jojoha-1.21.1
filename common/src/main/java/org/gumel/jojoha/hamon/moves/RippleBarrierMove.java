package org.gumel.jojoha.hamon.moves;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.combat.fx.MovementFx;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.hamon.HamonFx;
import org.gumel.jojoha.hamon.HamonMove;
import org.gumel.jojoha.hamon.HamonState;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.registry.ModSounds;

public final class RippleBarrierMove implements HamonMove {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "ripple_barrier");
    public static final RippleBarrierMove INSTANCE = new RippleBarrierMove();

    private static final int BARRIER_TICKS = 160;

    private static final int COOLDOWN_TICKS = 300;
    private static final float ENERGY_COST = 30F;

    public static final float DRAIN_PER_TICK =
            (JojohaPlayerData.MAX_SPEC_ENERGY - ENERGY_COST) / BARRIER_TICKS;

    private static final int BARRIER_POINTS = 40;
    private static final double BARRIER_RADIUS = 1.2;

    private RippleBarrierMove() {
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
        return HamonState.hasBarrier(player);
    }

    @Override
    public void release(ServerPlayer player, JojohaPlayerData data) {
        drop(player);
    }

    public static void drop(ServerPlayer player) {
        if (!HamonState.hasBarrier(player)) {
            return;
        }
        HamonState.dropBarrier(player);
        NetworkHandler.clearFirstPersonPose(player);
        MovementFx.drop(player);

        ServerLevel level = player.serverLevel();
        HamonFx.mote(level, player.getX(), player.getY() + 1.0, player.getZ(), 8, 0.5);
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_DEACTIVATE,
                SoundSource.PLAYERS, 0.4F, 1.8F);
    }

    public static void ranOut(ServerPlayer player) {
        NetworkHandler.clearFirstPersonPose(player);
        MovementFx.drop(player);
    }

    @Override
    public boolean activate(ServerPlayer player, JojohaPlayerData data) {
        ServerLevel level = player.serverLevel();
        HamonState.raiseBarrier(player, BARRIER_TICKS);

        NetworkHandler.sendFirstPersonPose(player, "guard");
        MovementFx.pose(player, MovementFx.RIPPLE_BARRIER);

        for (int i = 0; i < BARRIER_POINTS; i++) {
            double angle = (Math.PI * 2 * i) / BARRIER_POINTS;
            double x = player.getX() + Math.cos(angle) * BARRIER_RADIUS;
            double z = player.getZ() + Math.sin(angle) * BARRIER_RADIUS;

            HamonFx.spark(level, x, player.getY() + 1.0, z, 1, 0.0, 0.0);
            if (i % 5 == 0) {
                HamonFx.ripple(level, x, player.getY() + 1.0, z, 1);
            }
        }

        HamonFx.ring(level, player.getX(), player.getY() + 0.1, player.getZ(), 2.4, 2);
        level.playSound(null, player.blockPosition(), ModSounds.HAMON_RING.get(),
                SoundSource.PLAYERS, 0.5F, 1.8F);
        HamonFx.sound(level, player, 0.6F, 0.95F);

        return true;
    }
}
