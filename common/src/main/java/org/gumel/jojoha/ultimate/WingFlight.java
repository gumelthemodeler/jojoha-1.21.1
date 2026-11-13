package org.gumel.jojoha.ultimate;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import org.gumel.jojoha.data.Fate;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;

import java.util.function.Predicate;

public final class WingFlight {
    public static Predicate<Player> clientWings = player -> false;

    private WingFlight() {
    }

    public static boolean out(Player player) {
        if (player instanceof ServerPlayer server) {
            JojohaPlayerData data = PlayerDataAccess.get(server);
            return Fate.ultimate(data) && data.ultimateWings;
        }
        return player.level().isClientSide() && clientWings.test(player);
    }

    public static boolean canGlide(Player player) {
        return !player.onGround() && !player.isPassenger() && !player.isInWater() && !player.isInLava()
                && !player.getAbilities().flying && !player.hasEffect(MobEffects.LEVITATION);
    }
}
