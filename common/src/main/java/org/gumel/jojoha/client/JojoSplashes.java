package org.gumel.jojoha.client;

import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.RandomSource;
import org.gumel.jojoha.Jojoha;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;

public final class JojoSplashes {
    private static final ResourceLocation LOCATION =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "texts/splashes.txt");

    private static final float CHANCE = 1F / 3F;

    private static final RandomSource RANDOM = RandomSource.create();

    private static List<String> lines = List.of();

    private JojoSplashes() {
    }

    public static void load(ResourceManager resources) {
        try (BufferedReader reader = resources.openAsReader(LOCATION)) {
            lines = reader.lines().map(String::trim).filter(line -> !line.isEmpty()).toList();
        } catch (IOException e) {
            Jojoha.LOGGER.warn("Could not read {}", LOCATION, e);
            lines = List.of();
        }
    }

    public static SplashRenderer pick(SplashRenderer vanilla) {
        if (vanilla == null || vanilla == SplashRenderer.CHRISTMAS || vanilla == SplashRenderer.NEW_YEAR
                || vanilla == SplashRenderer.HALLOWEEN || lines.isEmpty() || RANDOM.nextFloat() >= CHANCE) {
            return vanilla;
        }
        return new SplashRenderer(lines.get(RANDOM.nextInt(lines.size())));
    }
}
