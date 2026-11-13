package org.gumel.jojoha.entity;

import dev.architectury.registry.level.biome.BiomeModifications;
import dev.architectury.registry.level.entity.SpawnPlacementsRegistry;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.Heightmap;
import org.gumel.jojoha.config.JojohaServerConfig;
import org.gumel.jojoha.registry.ModRegistries;

public final class VampireSpawns {
    public static final int WEIGHT = 5;
    private static final int MIN_GROUP = 1;
    private static final int MAX_GROUP = 1;

    private VampireSpawns() {
    }

    public static void init() {
        SpawnPlacementsRegistry.register(ModRegistries.VAMPIRE, SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules);

        BiomeModifications.addProperties(
                context -> context.hasTag(BiomeTags.IS_OVERWORLD)
                        && !context.hasTag(BiomeTags.IS_OCEAN)
                        && !context.hasTag(BiomeTags.IS_DEEP_OCEAN)
                        && !context.hasTag(BiomeTags.IS_RIVER)
                        && !context.getKey().map(key -> key.equals(Biomes.MUSHROOM_FIELDS.location()))
                                .orElse(false)
                        && JojohaServerConfig.VAMPIRE_WEIGHT.whole() > 0,
                (context, properties) -> properties.getSpawnProperties().addSpawn(MobCategory.MONSTER,
                        new MobSpawnSettings.SpawnerData(ModRegistries.VAMPIRE.get(), JojohaServerConfig.VAMPIRE_WEIGHT.whole(),
                                MIN_GROUP, MAX_GROUP)));
    }
}
