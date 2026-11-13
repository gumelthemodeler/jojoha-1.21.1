package org.gumel.jojoha;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class JojohaAdvancements {
    private static final String CRITERION = "granted";

    private JojohaAdvancements() {
    }

    public static void award(ServerPlayer player, String path) {
        AdvancementHolder held = player.server.getAdvancements()
                .get(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, path));

        if (held != null) {
            player.getAdvancements().award(held, CRITERION);
        }
    }
}
