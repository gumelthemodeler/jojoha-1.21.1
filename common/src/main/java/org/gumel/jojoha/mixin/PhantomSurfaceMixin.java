package org.gumel.jojoha.mixin;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.gumel.jojoha.level.ModBiomes;
import org.gumel.jojoha.registry.ModBlocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(NoiseGeneratorSettings.class)
public abstract class PhantomSurfaceMixin {
    private static final double JOJOHA$ANDESITE_ABOVE = 0.25;

    private static final double JOJOHA$BOG_MUD_ABOVE = 0.0;

    private static final double JOJOHA$BONE_SAND_COARSE = 0.254;

    private static final double JOJOHA$BONE_ASH_COARSE = 0.255;

    private static final double JOJOHA$BONE_SAND_FINE = 0.300;

    private static final double JOJOHA$BONE_ASH_FINE = 0.418;

    private static final double JOJOHA$BONE_HILL_ROCK_ABOVE = 0.161;

    private static final double JOJOHA$BONE_SHORE_ABOVE = -0.095;

    private static final int JOJOHA$BONE_ROCK_LOW = 72;

    private static final int JOJOHA$BONE_ROCK_HIGH = 92;

    private static final int JOJOHA$BONE_SHORE_DEPTH = 5;

    private static final int JOJOHA$BONE_SHORE_RISE = 2;

    private static final double JOJOHA$WASTES_ROCK_ABOVE = 0.3;

    private static final double JOJOHA$FLATS_SAND_ABOVE = 0.35;

    private static final double JOJOHA$PEAKS_ASH_ABOVE = 0.25;

    @SuppressWarnings("unchecked")
    private static SurfaceRules.ConditionSource jojoha$moors() {
        return SurfaceRules.isBiome(ModBiomes.MOORS.toArray(ResourceKey[]::new));
    }

    @SuppressWarnings("unchecked")
    private static SurfaceRules.ConditionSource jojoha$bonelands() {
        return SurfaceRules.isBiome(ModBiomes.BONELANDS.toArray(ResourceKey[]::new));
    }

    @SuppressWarnings("unchecked")
    private static SurfaceRules.ConditionSource jojoha$sands() {
        return SurfaceRules.isBiome(ModBiomes.SANDS.toArray(ResourceKey[]::new));
    }

    @Inject(method = "surfaceRule", at = @At("RETURN"), cancellable = true)
    private void jojoha$phantomStoneColumn(CallbackInfoReturnable<SurfaceRules.RuleSource> cir) {
        SurfaceRules.RuleSource vanilla = cir.getReturnValue();
        if (vanilla == null) {
            return;
        }

        SurfaceRules.RuleSource stone =
                SurfaceRules.state(ModBlocks.PHANTOM_STONE.get().defaultBlockState());

        SurfaceRules.RuleSource rock = SurfaceRules.sequence(
                SurfaceRules.ifTrue(
                        SurfaceRules.noiseCondition(Noises.SURFACE, JOJOHA$ANDESITE_ABOVE),
                        SurfaceRules.state(ModBlocks.PHANTOM_ANDESITE.get().defaultBlockState())),
                stone);

        SurfaceRules.RuleSource surface = SurfaceRules.ifTrue(
                SurfaceRules.abovePreliminarySurface(),
                SurfaceRules.ifTrue(
                        jojoha$moors(),
                        SurfaceRules.sequence(
                                SurfaceRules.ifTrue(SurfaceRules.steep(), rock),
                                SurfaceRules.ifTrue(
                                        SurfaceRules.ON_FLOOR,
                                        SurfaceRules.ifTrue(
                                                SurfaceRules.isBiome(ModBiomes.PHANTOM_BOG),
                                                SurfaceRules.ifTrue(
                                                        SurfaceRules.noiseCondition(Noises.SWAMP,
                                                                JOJOHA$BOG_MUD_ABOVE),
                                                        SurfaceRules.state(Blocks.MUD.defaultBlockState())))),
                                SurfaceRules.ifTrue(
                                        SurfaceRules.ON_FLOOR,
                                        SurfaceRules.ifTrue(
                                                SurfaceRules.waterBlockCheck(0, 0),
                                                SurfaceRules.state(
                                                        ModBlocks.PHANTOM_GRASS_BLOCK.get()
                                                                .defaultBlockState()))),
                                SurfaceRules.ifTrue(
                                        SurfaceRules.UNDER_FLOOR,
                                        SurfaceRules.state(ModBlocks.PHANTOM_DIRT.get()
                                                .defaultBlockState())))));

        SurfaceRules.RuleSource deep = SurfaceRules.ifTrue(
                jojoha$moors(), rock);

        SurfaceRules.RuleSource ash =
                SurfaceRules.state(ModBlocks.ASHY_ROCKLAND_STONE.get().defaultBlockState());
        SurfaceRules.RuleSource boneSand =
                SurfaceRules.state(ModBlocks.SKELETON_SAND.get().defaultBlockState());
        SurfaceRules.RuleSource boneRock =
                SurfaceRules.state(ModBlocks.SKELETON_ROCK.get().defaultBlockState());

        SurfaceRules.ConditionSource highGround = SurfaceRules.not(SurfaceRules.verticalGradient(
                "jojoha:bone_rock",
                VerticalAnchor.absolute(JOJOHA$BONE_ROCK_LOW),
                VerticalAnchor.absolute(JOJOHA$BONE_ROCK_HIGH)));

        SurfaceRules.RuleSource groundMix = SurfaceRules.sequence(
                SurfaceRules.ifTrue(
                        SurfaceRules.noiseCondition(Noises.NETHERRACK, JOJOHA$BONE_SAND_COARSE),
                        boneSand),
                SurfaceRules.ifTrue(
                        SurfaceRules.noiseCondition(Noises.NETHER_WART, JOJOHA$BONE_ASH_COARSE),
                        ash),
                SurfaceRules.ifTrue(
                        SurfaceRules.noiseCondition(Noises.SWAMP, JOJOHA$BONE_SAND_FINE),
                        boneSand),
                SurfaceRules.ifTrue(
                        SurfaceRules.noiseCondition(Noises.BADLANDS_PILLAR, JOJOHA$BONE_ASH_FINE),
                        ash),
                boneRock);

        SurfaceRules.RuleSource cappedRock = SurfaceRules.sequence(
                SurfaceRules.ifTrue(
                        SurfaceRules.ON_FLOOR,
                        SurfaceRules.sequence(
                                SurfaceRules.ifTrue(
                                        SurfaceRules.noiseCondition(
                                                Noises.ICE, JOJOHA$BONE_HILL_ROCK_ABOVE),
                                        boneRock),
                                groundMix)),
                boneRock);

        SurfaceRules.RuleSource hills = SurfaceRules.ifTrue(highGround, cappedRock);

        SurfaceRules.RuleSource shoreRock = SurfaceRules.ifTrue(
                SurfaceRules.not(SurfaceRules.waterBlockCheck(JOJOHA$BONE_SHORE_RISE, 0)),
                SurfaceRules.ifTrue(
                        SurfaceRules.waterBlockCheck(-JOJOHA$BONE_SHORE_DEPTH, 0),
                        SurfaceRules.ifTrue(
                                SurfaceRules.noiseCondition(
                                        Noises.NETHER_STATE_SELECTOR, JOJOHA$BONE_SHORE_ABOVE),
                                boneRock)));

        SurfaceRules.RuleSource wastes = SurfaceRules.ifTrue(
                SurfaceRules.isBiome(ModBiomes.ASH_WASTES),
                SurfaceRules.sequence(
                        SurfaceRules.ifTrue(SurfaceRules.steep(), boneRock),
                        SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.sequence(
                                SurfaceRules.ifTrue(
                                        SurfaceRules.noiseCondition(Noises.NETHERRACK, JOJOHA$WASTES_ROCK_ABOVE),
                                        boneRock),
                                ash)),
                        SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, ash)));

        SurfaceRules.RuleSource flats = SurfaceRules.ifTrue(
                SurfaceRules.isBiome(ModBiomes.OSSUARY_FLATS),
                SurfaceRules.sequence(
                        SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.sequence(
                                SurfaceRules.ifTrue(
                                        SurfaceRules.noiseCondition(Noises.SWAMP, JOJOHA$FLATS_SAND_ABOVE),
                                        boneSand),
                                boneRock)),
                        SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, boneRock)));

        SurfaceRules.RuleSource peaks = SurfaceRules.ifTrue(
                SurfaceRules.isBiome(ModBiomes.BONE_PEAKS),
                SurfaceRules.sequence(
                        SurfaceRules.ifTrue(SurfaceRules.steep(), boneRock),
                        SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.sequence(
                                SurfaceRules.ifTrue(
                                        SurfaceRules.noiseCondition(Noises.ICE, JOJOHA$PEAKS_ASH_ABOVE),
                                        ash),
                                boneRock)),
                        SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, boneRock)));

        SurfaceRules.RuleSource bonelands = SurfaceRules.ifTrue(
                SurfaceRules.abovePreliminarySurface(),
                SurfaceRules.ifTrue(
                        jojoha$bonelands(),
                        SurfaceRules.sequence(
                                shoreRock,
                                wastes,
                                flats,
                                peaks,
                                SurfaceRules.ifTrue(SurfaceRules.steep(), boneRock),
                                hills,
                                SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, groundMix),
                                SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, groundMix))));

        SurfaceRules.RuleSource boneDeep = SurfaceRules.ifTrue(
                jojoha$bonelands(),
                jojoha$ashBeds(ash, boneRock));

        SurfaceRules.RuleSource sandstone = SurfaceRules.state(Blocks.SANDSTONE.defaultBlockState());
        SurfaceRules.RuleSource crusaderSand = SurfaceRules.sequence(
                SurfaceRules.ifTrue(SurfaceRules.ON_CEILING, sandstone),
                SurfaceRules.state(ModBlocks.CRUSADER_SAND.get().defaultBlockState()));
        SurfaceRules.RuleSource oasis = SurfaceRules.ifTrue(
                SurfaceRules.isBiome(ModBiomes.OASIS),
                SurfaceRules.sequence(
                        SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR,
                                SurfaceRules.ifTrue(SurfaceRules.waterBlockCheck(-1, 0),
                                        SurfaceRules.state(Blocks.GRASS_BLOCK.defaultBlockState()))),
                        SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR,
                                SurfaceRules.ifTrue(SurfaceRules.waterBlockCheck(-1, 0),
                                        SurfaceRules.state(Blocks.DIRT.defaultBlockState())))));
        SurfaceRules.RuleSource stardust = SurfaceRules.ifTrue(
                SurfaceRules.abovePreliminarySurface(),
                SurfaceRules.ifTrue(
                        jojoha$sands(),
                        SurfaceRules.sequence(
                                oasis,
                                SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR,
                                        SurfaceRules.ifTrue(SurfaceRules.waterBlockCheck(-1, 0), crusaderSand)),
                                SurfaceRules.ifTrue(SurfaceRules.waterStartCheck(-6, -1),
                                        SurfaceRules.sequence(
                                                SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, crusaderSand),
                                                SurfaceRules.ifTrue(SurfaceRules.VERY_DEEP_UNDER_FLOOR, sandstone))))));

        cir.setReturnValue(SurfaceRules.sequence(surface, bonelands, stardust, vanilla, deep, boneDeep));
    }

    private static SurfaceRules.RuleSource jojoha$ashBeds(SurfaceRules.RuleSource ash, SurfaceRules.RuleSource rock) {
        List<ResourceKey<NormalNoise.NoiseParameters>> fields =
                List.of(Noises.NETHERRACK, Noises.AQUIFER_BARRIER, Noises.ICE, Noises.NETHER_WART,
                        Noises.ICEBERG_PILLAR_ROOF);
        double[] above = {0.145, 0.170, 0.161, 0.146, 0.170};
        int[] thickness = {4, 3, 5, 4, 6, 3, 5};
        int from = -64;
        int to = 320;
        int perGroup = 8;

        List<SurfaceRules.RuleSource> groups = new ArrayList<>();
        List<SurfaceRules.RuleSource> beds = new ArrayList<>();
        int bottom = from;
        for (int bed = 0; bottom < to; bed++) {
            int top = bottom + thickness[bed % thickness.length];
            int field = (bed * 2) % fields.size();
            beds.add(SurfaceRules.ifTrue(
                    SurfaceRules.not(SurfaceRules.yBlockCheck(VerticalAnchor.absolute(top), 0)),
                    SurfaceRules.sequence(
                            SurfaceRules.ifTrue(SurfaceRules.noiseCondition(fields.get(field), above[field]), ash),
                            rock)));
            bottom = top;
            if (beds.size() == perGroup || bottom >= to) {
                groups.add(SurfaceRules.ifTrue(
                        SurfaceRules.not(SurfaceRules.yBlockCheck(VerticalAnchor.absolute(bottom), 0)),
                        SurfaceRules.sequence(beds.toArray(SurfaceRules.RuleSource[]::new))));
                beds = new ArrayList<>();
            }
        }
        groups.add(rock);
        return SurfaceRules.sequence(groups.toArray(SurfaceRules.RuleSource[]::new));
    }
}
