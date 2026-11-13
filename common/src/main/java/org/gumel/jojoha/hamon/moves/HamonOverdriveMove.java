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
import org.gumel.jojoha.stand.skill.StandBeat;

public final class HamonOverdriveMove implements HamonMove {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "hamon_overdrive");
    public static final HamonOverdriveMove INSTANCE = new HamonOverdriveMove();

    private static final int CHARGE_TICKS = 200;

    private static final int COOLDOWN_TICKS = 120;
    private static final float ENERGY_COST = 25F;

    private HamonOverdriveMove() {
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
    public boolean activate(ServerPlayer player, JojohaPlayerData data) {
        MovementFx.pose(player, MovementFx.OVERDRIVE);
        NetworkHandler.sendFirstPersonPose(player, "gather");
        StandBeat.at(MovementFx.OVERDRIVE_RELEASE_TICKS, player,
                () -> release(player));
        return true;
    }

    private static void release(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        HamonState.chargeOverdrive(player, CHARGE_TICKS);

        HamonFx.spark(level, player.getX(), player.getY() + 1.0, player.getZ(), 22, 0.4, 0.05);
        HamonFx.ripple(level, player.getX(), player.getY() + 1.1, player.getZ(), 3);
        HamonFx.ring(level, player.getX(), player.getY() + 0.1, player.getZ(), 1.4, 2);
        HamonFx.mote(level, player.getX(), player.getY() + 1.0, player.getZ(), 8, 0.5);
        level.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_RETURN,
                SoundSource.PLAYERS, 0.7F, 1.7F);
        HamonFx.chargeSound(level, player, 0.7F, 1.0F);
    }
}
