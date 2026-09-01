package org.gumel.jojoha.mixin;

import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.SurfaceRules;
import org.gumel.jojoha.level.ModBiomes;
import org.gumel.jojoha.registry.ModBlocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * What the Phantom Highlands are made of, from the turf down to the bedrock.
 *
 * <h2>Two rules, on either side of vanilla's</h2>
 *
 * <p>A surface rule sequence takes the first answer it gets, so where a rule sits is what decides
 * which blocks it may claim. This needs both ends of vanilla's, for opposite reasons.
 *
 * <p><b>Before</b>, for the surface: vanilla would otherwise lay its own grass and dirt here, and
 * whatever runs first wins. This is where the turf goes and where a steep face is turned to stone.
 *
 * <p><b>After</b>, for the rock: vanilla answers for everything it cares about and returns nothing
 * elsewhere, leaving the default block standing - and the default block is stone. So "last" and
 * "only where vanilla left plain stone" are the same rule. It also means bedrock and deepslate look
 * after themselves, because vanilla answers for both before this is reached.
 *
 * <h2>Why steep faces are stone</h2>
 *
 * <p>Because a hillside made of dirt is what a hillside made of grass blocks looks like from the
 * side. Every step of the terrain puts a block's flank in view, and vanilla's surface is one block
 * of grass over dirt - so the steeper the ground, the more of it reads as bare earth. Vanilla has
 * the same problem and the same answer: five of its own rules test for steepness, and the windswept
 * biomes use it to put stone on their slopes.
 */
@Mixin(NoiseGeneratorSettings.class)
public abstract class PhantomSurfaceMixin {
    /**
     * How much of the rock is andesite.
     *
     * <p>The noise runs about -1 to 1, so a threshold above zero makes andesite the minority: higher
     * is rarer. Raise it for purer stone, lower it toward zero for an even mix.
     */
    private static final double JOJOHA$ANDESITE_ABOVE = 0.25;

    @Inject(method = "surfaceRule", at = @At("RETURN"), cancellable = true)
    private void jojoha$phantomStoneColumn(CallbackInfoReturnable<SurfaceRules.RuleSource> cir) {
        SurfaceRules.RuleSource vanilla = cir.getReturnValue();
        if (vanilla == null) {
            return;
        }

        SurfaceRules.RuleSource stone =
                SurfaceRules.state(ModBlocks.PHANTOM_STONE.get().defaultBlockState());

        // Veins first, plain stone as the fallback - the same first-answer-wins rule one level down.
        SurfaceRules.RuleSource rock = SurfaceRules.sequence(
                SurfaceRules.ifTrue(
                        SurfaceRules.noiseCondition(Noises.SURFACE, JOJOHA$ANDESITE_ABOVE),
                        SurfaceRules.state(ModBlocks.PHANTOM_ANDESITE.get().defaultBlockState())),
                stone);

        // Both of the outer conditions have to hold before any of this is allowed to claim a block.
        //
        // abovePreliminarySurface() is the one that keeps it out of the caves. ON_FLOOR just means
        // "the top of a run of stone", and a cave floor is exactly that - so without this guard the
        // turf rule fires as happily two hundred blocks down as it does on a hillside, and the caves
        // come out carpeted. Vanilla wraps its own surface rules in the same call for the same
        // reason. The dirt band is inside the guard too, so it stops where the turf stops; below
        // that the deep rule still answers and cave walls are phantom stone.
        SurfaceRules.RuleSource surface = SurfaceRules.ifTrue(
                SurfaceRules.abovePreliminarySurface(),
                SurfaceRules.ifTrue(
                        SurfaceRules.isBiome(ModBiomes.PHANTOM_HIGHLANDS),
                        SurfaceRules.sequence(
                                // Steep before turf, so a cliff is rock rather than grass clinging
                                // to it.
                                SurfaceRules.ifTrue(SurfaceRules.steep(), rock),
                                SurfaceRules.ifTrue(
                                        SurfaceRules.ON_FLOOR,
                                        // Vanilla's own gate for laying grass: the top block, and
                                        // not under water. Without it lake and river beds turn to
                                        // turf.
                                        SurfaceRules.ifTrue(
                                                SurfaceRules.waterBlockCheck(0, 0),
                                                SurfaceRules.state(
                                                        ModBlocks.PHANTOM_GRASS_BLOCK.get()
                                                                .defaultBlockState()))),
                                // Everything between the turf and the stone. UNDER_FLOOR is the same
                                // condition vanilla lays its own dirt with - depth zero from the
                                // floor plus the surface-depth noise - so this band is as thick as
                                // it would have been, and it has to come after the turf rule or it
                                // would answer first and there would be no grass at all.
                                SurfaceRules.ifTrue(
                                        SurfaceRules.UNDER_FLOOR,
                                        SurfaceRules.state(ModBlocks.PHANTOM_DIRT.get()
                                                .defaultBlockState())))));

        SurfaceRules.RuleSource deep = SurfaceRules.ifTrue(
                SurfaceRules.isBiome(ModBiomes.PHANTOM_HIGHLANDS), rock);

        cir.setReturnValue(SurfaceRules.sequence(surface, vanilla, deep));
    }
}
