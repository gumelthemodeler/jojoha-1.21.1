package org.gumel.jojoha.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.neoforge.PlayerDataAccessImpl;

@Mod(Jojoha.MOD_ID)
public final class JojohaNeoForge {
    public JojohaNeoForge(IEventBus modEventBus) {
        // Attachment types are NeoForge-native, so they need direct access to the mod event bus.
        PlayerDataAccessImpl.register(modEventBus);

        // Run our common setup.
        Jojoha.init();
    }
}
