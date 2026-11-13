package org.gumel.jojoha.stand.heaven;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.network.packet.HeavenFxPacket;
import org.gumel.jojoha.stand.passive.HeavenTierPassive;

public final class Ascension {
    public static final float MAX = 100F;

    public static final float ASCENDED = 50F;
    public static final float TRANSCENDENT = 75F;
    public static final float HEAVEN = 100F;

    public static final float PER_DAMAGE = 1.2F;
    public static final float PER_BLOCK = 3F;
    public static final float PER_MOVE = 2.5F;
    public static final float PER_TIME_STOP = 12F;

    public static final float DRAIN_PER_SECOND = 2F;

    private Ascension() {
    }

    static void init() {
    }

    public static float rate() {
        return 1F;
    }

    public static boolean reached(JojohaPlayerData data, float threshold) {
        return Heaven.is(data) && data.ascension >= threshold - 0.001F;
    }

    public static float fraction(JojohaPlayerData data) {
        return Math.max(0F, Math.min(1F, data.ascension / MAX));
    }

    public static void add(ServerPlayer player, float amount) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (!Heaven.is(data) || amount <= 0F) {
            return;
        }
        float before = data.ascension;
        data.ascension = Math.min(MAX, before + amount * rate());
        PlayerDataAccess.set(player, data);
        announce(player, before, data.ascension);
    }

    public static void spend(ServerPlayer player) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        data.ascension = 0F;
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
    }

    public static boolean shows(JojohaPlayerData data, ResourceLocation passiveId) {
        HeavenTierPassive tier = HeavenTierPassive.byId(passiveId);
        if (tier == null) {
            return true;
        }
        HeavenTierPassive highest = HeavenTierPassive.highest(data.ascension);
        return highest == tier;
    }

    static void tick(ServerPlayer player, JojohaPlayerData data) {
        if (data.combatTicks > 0) {
            data.heavenFightTicks++;
            return;
        }
        data.heavenFightTicks = 0;
        if (data.ascension > 0F && !Overwrite.running(player)) {
            data.ascension = Math.max(0F, data.ascension - DRAIN_PER_SECOND / 20F);
        }
    }

    private static void announce(ServerPlayer player, float before, float after) {
        HeavenTierPassive was = HeavenTierPassive.highest(before);
        HeavenTierPassive now = HeavenTierPassive.highest(after);
        if (now == null || now == was) {
            return;
        }
        player.displayClientMessage(Component.translatable("message.jojoha.heaven.tier",
                Component.translatable(now.translationKey())).withStyle(ChatFormatting.GOLD), true);
        player.serverLevel().playSound(null, player.blockPosition(),
                now.threshold() >= HEAVEN ? SoundEvents.BEACON_POWER_SELECT : SoundEvents.AMETHYST_BLOCK_RESONATE,
                SoundSource.PLAYERS, 1.0F, now.threshold() >= HEAVEN ? 0.8F : 1.2F);
        HeavenShow.on(player, HeavenFxPacket.TIER,
                now.threshold() >= HEAVEN ? 3F : now.threshold() >= TRANSCENDENT ? 2F : 1F, 0);
        PlayerDataAccess.sync(player);
    }
}
