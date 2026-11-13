package org.gumel.jojoha.hamon.moves;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.hamon.HamonFx;
import org.gumel.jojoha.hamon.HamonMove;
import org.gumel.jojoha.hamon.HamonState;
import org.gumel.jojoha.network.NetworkHandler;

public final class ChannelingMove implements HamonMove {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "channeling");
    public static final ChannelingMove INSTANCE = new ChannelingMove();

    private static final int CHANNEL_TICKS = 200;

    private static final int COOLDOWN_TICKS = 400;
    private static final float ENERGY_COST = 25F;

    private ChannelingMove() {
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
        if (player.getMainHandItem().isEmpty()) {
            player.displayClientMessage(Component.translatable(
                    "message.jojoha.hamon.nothing_to_channel")
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }

        HamonState.channel(player, CHANNEL_TICKS);

        NetworkHandler.sendFirstPersonPose(player, "channel");

        ServerLevel level = player.serverLevel();

        Vec3 look = player.getLookAngle();
        for (int i = 0; i < 18; i++) {
            double reach = 0.4 + i * 0.06;
            HamonFx.spark(level,
                    player.getX() + look.x * reach,
                    player.getY() + 1.1 + look.y * reach,
                    player.getZ() + look.z * reach,
                    1, 0.08, 0.01);
        }

        double gripX = player.getX() + look.x * 1.4;
        double gripY = player.getY() + 1.1 + look.y * 1.4;
        double gripZ = player.getZ() + look.z * 1.4;
        HamonFx.ring(level, gripX, gripY, gripZ, 1.0, 0);
        HamonFx.ripple(level, gripX, gripY, gripZ, 2);

        level.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_RETURN,
                SoundSource.PLAYERS, 0.6F, 1.7F);
        HamonFx.chargeSound(level, player, 0.55F, 1.15F);
        return true;
    }
}
