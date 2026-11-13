package org.gumel.jojoha.combat;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;

public final class OwnPets {

    private OwnPets() {
    }

    public static boolean tamedBy(Player player, Entity candidate) {
        return player != null && candidate instanceof OwnableEntity owned
                && player.getUUID().equals(owned.getOwnerUUID());
    }
}
