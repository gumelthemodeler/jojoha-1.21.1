package org.gumel.jojoha.level;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import org.gumel.jojoha.Jojoha;

/**
 * Keys for the mod's own biomes.
 *
 * <p>Keys only, and no registration: a biome is data. The JSON under {@code worldgen/biome} is what
 * brings it into existence, and this is only the name code uses to ask whether it is standing in
 * one - see PhantomSkyMixin.
 */
public final class ModBiomes {
    /**
     * The Part 1 biome: violet ground under a turquoise sky.
     *
     * <p>Everything about how it looks lives in the JSON except the night, which no biome field can
     * describe - see PhantomSkyMixin.
     */
    public static final ResourceKey<Biome> PHANTOM_HIGHLANDS = ResourceKey.create(
            Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "phantom_highlands"));

    private ModBiomes() {
    }
}
