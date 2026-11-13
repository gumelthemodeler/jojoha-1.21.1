package org.gumel.jojoha.level;

import dev.architectury.registry.level.biome.BiomeModifications;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.gumel.jojoha.Jojoha;

public final class MeteorWorldgen {

    private static final ResourceKey<PlacedFeature> METEOR_CRASH = ResourceKey.create(
            Registries.PLACED_FEATURE,
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "meteor_crash_placed"));

    private static final ResourceKey<PlacedFeature> PILLAR_SHRINE = ResourceKey.create(
            Registries.PLACED_FEATURE,
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "pillar_shrine_placed"));

    private MeteorWorldgen() {
    }

    public static void init() {
        BiomeModifications.addProperties(
                context -> is(context, ModBiomes.STARDUST_SANDS, ModBiomes.DUNE_SEA),
                (context, properties) -> properties.getGenerationProperties()
                        .addFeature(GenerationStep.Decoration.SURFACE_STRUCTURES, METEOR_CRASH));
        BiomeModifications.addProperties(
                context -> is(context, ModBiomes.SKELETON_HEEL_BONELANDS, ModBiomes.OSSUARY_FLATS,
                        ModBiomes.ASH_WASTES),
                (context, properties) -> properties.getGenerationProperties()
                        .addFeature(GenerationStep.Decoration.SURFACE_STRUCTURES, PILLAR_SHRINE));
    }

    @SafeVarargs
    private static boolean is(BiomeModifications.BiomeContext context, ResourceKey<Biome>... biomes) {
        return context.getKey().map(key -> {
            for (ResourceKey<Biome> biome : biomes) {
                if (biome.location().equals(key)) {
                    return true;
                }
            }
            return false;
        }).orElse(false);
    }
}
