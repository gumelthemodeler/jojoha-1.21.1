package org.gumel.jojoha.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.client.ModEntityRenderers;
import org.gumel.jojoha.data.neoforge.PlayerDataAccessImpl;
import org.gumel.jojoha.neoforge.client.JojohaNeoForgeConfigScreen;

@Mod(Jojoha.MOD_ID)
public final class JojohaNeoForge {
    public JojohaNeoForge(IEventBus modEventBus, ModContainer container) {
        PlayerDataAccessImpl.register(modEventBus);

        if (FMLEnvironment.dist.isClient()) {
            JojohaNeoForgeConfigScreen.register(container);
        }

        Jojoha.init();

        if (FMLEnvironment.dist.isClient()) {
            ModEntityRenderers.registerModels();
        }
    }
}
