package org.gumel.jojoha.stand.heaven;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.network.packet.HeavenFxPacket;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.StandSummonHandler;

public final class GreatestHigh {
    public static final int UNLOCK_SECONDS = 30;
    public static final float START = 1.2F;
    public static final float GROWTH_PER_SECOND = 1.04F;
    public static final float CAP = 2.5F;

    private static final int EMBER_EVERY = 5;
    private static final float EMBERS_BASE = 2F;
    private static final float EMBERS_PER_STEP = 0.6F;

    private GreatestHigh() {
    }

    public static int unlockTicks() {
        return UNLOCK_SECONDS * 20;
    }

    public static boolean available(JojohaPlayerData data) {
        return data.greatestHighTicks > 0 || data.heavenFightTicks >= unlockTicks();
    }

    public static boolean active(JojohaPlayerData data) {
        return Heaven.is(data) && data.greatestHighTicks > 0;
    }

    public static float multiplier(JojohaPlayerData data) {
        if (!active(data)) {
            return 1F;
        }
        float seconds = data.greatestHighTicks / 20F;
        float ramp = START * (float) Math.pow(GROWTH_PER_SECOND, seconds);
        return Math.min(CAP, ramp);
    }

    public static void begin(ServerPlayer player, JojohaPlayerData data) {
        data.greatestHighTicks = 1;
        ServerLevel level = player.serverLevel();
        level.playSound(null, player.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.6F, 1.4F);
        level.playSound(null, player.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 0.9F, 1.2F);
        HeavenShow.on(player, HeavenFxPacket.ROAR, 0F, 0);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY() + 1.0, player.getZ(),
                40, 0.8, 1.0, 0.8, 0.08);
        Heaven.announce(player, "announce.jojoha.heaven_greatest_high");
        player.displayClientMessage(Component.translatable("message.jojoha.heaven.greatest_high")
                .withStyle(ChatFormatting.DARK_PURPLE), true);
        StandEntity stand = StandSummonHandler.findStand(player, data);
        if (stand != null) {
            stand.triggerTimeStopPose();
        }
    }

    public static void end(ServerPlayer player, JojohaPlayerData data) {
        if (data.greatestHighTicks <= 0) {
            return;
        }
        data.greatestHighTicks = 0;
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.BEACON_DEACTIVATE,
                SoundSource.PLAYERS, 0.8F, 0.8F);
        player.displayClientMessage(Component.translatable("message.jojoha.heaven.greatest_high_end")
                .withStyle(ChatFormatting.GRAY), true);
    }

    static void tick(ServerPlayer player, JojohaPlayerData data) {
        if (data.greatestHighTicks <= 0) {
            return;
        }
        if (data.combatTicks <= 0 || !data.standSummoned) {
            end(player, data);
            return;
        }
        data.greatestHighTicks++;
        if (data.greatestHighTicks % EMBER_EVERY == 0) {
            StandEntity stand = StandSummonHandler.findStand(player, data);
            if (stand != null) {
                int embers = Math.round(EMBERS_BASE + EMBERS_PER_STEP * (multiplier(data) - START) / 0.1F);
                player.serverLevel().sendParticles(ParticleTypes.SOUL_FIRE_FLAME, stand.getX(),
                        stand.getY() + stand.getBbHeight() * 0.6, stand.getZ(), embers, 0.4, 0.6, 0.4, 0.02);
            }
        }
    }
}
