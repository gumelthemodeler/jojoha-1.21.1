package org.gumel.jojoha.client;

import dev.architectury.registry.client.rendering.ColorHandlerRegistry;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.world.item.Item;
import org.gumel.jojoha.registry.ModItems;

import java.util.List;
import java.util.Map;

public final class SoulCardColors {
    private static final int UNTINTED = 0xFFFFFFFF;

    private static final Map<RegistrySupplier<Item>, Integer> CARDS = Map.ofEntries(
            Map.entry(ModItems.SOUL_CARD_JOTARO, 0xFFB35CFF),
            Map.entry(ModItems.SOUL_CARD_JOSEPH, 0xFF7A3FA8),
            Map.entry(ModItems.SOUL_CARD_KAKYOIN, 0xFF3FBF6A),
            Map.entry(ModItems.SOUL_CARD_POLNAREFF, 0xFFD8DCE8),
            Map.entry(ModItems.SOUL_CARD_AVDOL, 0xFFE0492A),
            Map.entry(ModItems.SOUL_CARD_DIO, 0xFFE8C144),
            Map.entry(ModItems.SOUL_CARD_ZEPPELI, 0xFFE0A33A),
            Map.entry(ModItems.SOUL_CARD_JONATHAN, 0xFF3E6FB0),
            Map.entry(ModItems.SOUL_CARD_DIO_P1, 0xFF9C2B3A),
            Map.entry(ModItems.SOUL_CARD_JOSEPH_P2, 0xFF3FA7A0),
            Map.entry(ModItems.SOUL_CARD_CAESAR, 0xFFE87FB0));

    private SoulCardColors() {
    }

    public static void register() {
        for (Map.Entry<RegistrySupplier<Item>, Integer> card : CARDS.entrySet()) {
            int colour = card.getValue();
            ColorHandlerRegistry.registerItemColors(
                    (stack, tint) -> tint == 1 ? colour : UNTINTED,
                    card.getKey().get());
        }
    }

    public static List<Integer> palette() {
        return List.copyOf(CARDS.values());
    }
}
