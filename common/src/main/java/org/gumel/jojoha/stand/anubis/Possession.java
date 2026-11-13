package org.gumel.jojoha.stand.anubis;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.registry.ModSounds;

public final class Possession {
    public static final int SECOND = 20;

    public static final int GRACE_TICKS = 120 * SECOND;

    public static final float RAMP_PER_SECOND = 1F / (60F * 480F);

    public static final float MOST_PER_SECOND = 1F / 30F;

    public static final float SHARE = 0.20F;

    public static final int SHORTEST_TICKS = 10 * SECOND;

    public static final int LONGEST_TICKS = 90 * SECOND;

    public static final int METER_FULL_TICKS = 600 * SECOND;

    private static final int SYNC_EVERY = 5 * SECOND;

    private Possession() {
    }

    public static boolean possessed(JojohaPlayerData data) {
        return data.anubisPossessed > 0;
    }

    public static float chancePerSecond(int useTicks) {
        if (useTicks <= GRACE_TICKS) {
            return 0F;
        }
        return Math.min(MOST_PER_SECOND, (useTicks - GRACE_TICKS) / (float) SECOND * RAMP_PER_SECOND);
    }

    public static int lengthFor(int useTicks) {
        return Mth.clamp(Math.round(useTicks * SHARE), SHORTEST_TICKS, LONGEST_TICKS);
    }

    static void tick(ServerPlayer player, JojohaPlayerData data, boolean out) {
        if (!player.isAlive() || player.isSpectator() || player.isCreative()) {
            if (possessed(data)) {
                end(player, data, false);
            }
            return;
        }
        if (possessed(data)) {
            data.anubisPossessed--;
            if (data.anubisPossessed <= 0) {
                end(player, data, true);
            } else if (data.anubisPossessed % SECOND == 0) {
                player.displayClientMessage(Component.translatable("message.jojoha.anubis.possessed_left",
                        Mth.positiveCeilDiv(data.anubisPossessed, SECOND)).withStyle(ChatFormatting.DARK_PURPLE), true);
            }
            return;
        }
        if (!out) {
            return;
        }
        data.anubisUse++;
        if (data.anubisUse % SYNC_EVERY == 0) {
            PlayerDataAccess.syncSoon(player);
        }
        if (data.anubisUse % SECOND == 0 && player.getRandom().nextFloat() < chancePerSecond(data.anubisUse)) {
            begin(player, data, lengthFor(data.anubisUse));
        }
    }

    public static void begin(ServerPlayer player, JojohaPlayerData data, int ticks) {
        data.anubisPossessed = Math.max(1, ticks);
        data.anubisUse = 0;
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
        player.sendSystemMessage(Component.translatable("message.jojoha.anubis.possessed")
                .withStyle(ChatFormatting.DARK_PURPLE));
        player.serverLevel().playSound(null, player.blockPosition(),
                ModSounds.ANUBIS_UNSHEATHE.get(), SoundSource.PLAYERS, 1.0F, 0.6F);
    }

    static void end(ServerPlayer player, JojohaPlayerData data, boolean announce) {
        data.anubisPossessed = 0;
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
        if (announce) {
            player.sendSystemMessage(Component.translatable("message.jojoha.anubis.released")
                    .withStyle(ChatFormatting.GRAY));
            player.serverLevel().playSound(null, player.blockPosition(),
                    ModSounds.ANUBIS_SHEATHE.get(), SoundSource.PLAYERS, 1.0F, 0.8F);
        }
    }
}
