package org.gumel.jojoha.fabric;

import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.fabric.PlayerDataAccessImpl;
import net.fabricmc.api.ModInitializer;

public final class JojohaFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        // Force the attachment type to register up front rather than on first use.
        PlayerDataAccessImpl.bootstrap();

        // Run our common setup.
        Jojoha.init();
    }
}
