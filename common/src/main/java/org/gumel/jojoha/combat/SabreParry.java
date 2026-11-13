package org.gumel.jojoha.combat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import org.gumel.jojoha.item.WeaponClass;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class SabreParry {
    private static final Set<UUID> PRIMED = new HashSet<>();

    private SabreParry() {
    }

    public static boolean holdsSabre(ServerPlayer player) {
        WeaponClass weapon = WeaponClasses.held(player);
        return weapon != null && weapon.chainsPerfects();
    }

    public static boolean perfect(ServerPlayer player, boolean onTime) {
        if (!holdsSabre(player)) {
            return onTime;
        }
        boolean primed = PRIMED.remove(player.getUUID());
        if (onTime) {
            PRIMED.add(player.getUUID());
        }
        return onTime || primed;
    }

    public static boolean primed(ServerPlayer player) {
        return PRIMED.contains(player.getUUID());
    }

    public static void forget(Player player) {
        PRIMED.remove(player.getUUID());
    }

    public static void onGuardRaised(ServerPlayer player) {
        if (!holdsSabre(player)) {
            return;
        }
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 0.45F, primed(player) ? 2.0F : 1.9F);
    }
}
