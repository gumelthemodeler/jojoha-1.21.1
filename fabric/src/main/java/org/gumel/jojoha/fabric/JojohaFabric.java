package org.gumel.jojoha.fabric;

import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.fabric.PlayerDataAccessImpl;
import net.fabricmc.api.ModInitializer;

public final class JojohaFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        PlayerDataAccessImpl.bootstrap();

        Jojoha.init();
    }
}
