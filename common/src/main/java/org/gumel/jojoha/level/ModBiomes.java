package org.gumel.jojoha.level;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import org.gumel.jojoha.Jojoha;

import java.util.List;

public final class ModBiomes {
    public static final ResourceKey<Biome> PHANTOM_MOORS = ResourceKey.create(
            Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "phantom_moors"));

    public static final ResourceKey<Biome> MOOR_CLEARING = ResourceKey.create(
            Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "moor_clearing"));

    public static final ResourceKey<Biome> PHANTOM_BOG = ResourceKey.create(
            Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "phantom_bog"));

    public static final ResourceKey<Biome> MISTY_HOLLOW = ResourceKey.create(
            Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "misty_hollow"));

    public static final List<ResourceKey<Biome>> MOORS = List.of(PHANTOM_MOORS, MOOR_CLEARING, PHANTOM_BOG,
            MISTY_HOLLOW);

    public static final TagKey<Biome> IS_MOORS = TagKey.create(Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "is_moors"));

    public static final ResourceKey<Biome> SKELETON_HEEL_BONELANDS = ResourceKey.create(
            Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "skeleton_heel_bonelands"));

    public static final ResourceKey<Biome> STARDUST_SANDS = ResourceKey.create(
            Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "stardust_sands"));

    public static final ResourceKey<Biome> OSSUARY_FLATS = ResourceKey.create(
            Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "ossuary_flats"));

    public static final ResourceKey<Biome> ASH_WASTES = ResourceKey.create(
            Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "ash_wastes"));

    public static final ResourceKey<Biome> RIBCAGE_CANYON = ResourceKey.create(
            Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "ribcage_canyon"));

    public static final ResourceKey<Biome> BONE_PEAKS = ResourceKey.create(
            Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "bone_peaks"));

    public static final List<ResourceKey<Biome>> BONELANDS = List.of(SKELETON_HEEL_BONELANDS, OSSUARY_FLATS, ASH_WASTES,
            RIBCAGE_CANYON, BONE_PEAKS);

    public static final TagKey<Biome> IS_BONELANDS = TagKey.create(Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "is_bonelands"));

    public static final ResourceKey<Biome> DUNE_SEA = ResourceKey.create(
            Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "dune_sea"));

    public static final ResourceKey<Biome> OASIS = ResourceKey.create(
            Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "oasis"));

    public static final ResourceKey<Biome> METEOR_FIELD = ResourceKey.create(
            Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "meteor_field"));

    public static final List<ResourceKey<Biome>> SANDS = List.of(STARDUST_SANDS, DUNE_SEA, OASIS, METEOR_FIELD);

    public static final TagKey<Biome> IS_SANDS = TagKey.create(Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "is_sands"));

    public static int buildingVanilla;

    private ModBiomes() {
    }
}
