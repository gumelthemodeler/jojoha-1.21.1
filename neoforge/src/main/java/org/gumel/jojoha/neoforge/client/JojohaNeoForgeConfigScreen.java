package org.gumel.jojoha.neoforge.client;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.gumel.jojoha.client.JojohaConfigScreen;

public final class JojohaNeoForgeConfigScreen {
    private JojohaNeoForgeConfigScreen() {
    }

    public static void register(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (modContainer, parent) -> new JojohaConfigScreen(parent));
    }
}
