package org.gumel.jojoha.mixin;

import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.OverworldBiomeBuilder;
import org.gumel.jojoha.level.ModBiomes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

/**
 * Puts the Phantom Highlands into overworld generation, without a worldgen library.
 *
 * <h2>Why here and not in a datapack</h2>
 *
 * <p>There is no data-driven way to <em>add</em> an overworld biome. The list of what generates
 * where is one registry entry - {@code multi_noise_biome_source_parameter_list/overworld} - built in
 * code, and a datapack can only replace the whole of it. Replacing it means restating every vanilla
 * biome and its climate niche, which works perfectly on one machine and silently loses to the next
 * mod that does the same. Neither loader offers an add-a-biome API either; that gap is deliberate.
 *
 * <p>{@code addBiomes} is the one method vanilla enumerates the whole overworld through, and it
 * takes a consumer. Appending to it is additive by construction: nothing vanilla declared is
 * removed, and two mods doing this both land rather than one overwriting the other. It is the same
 * seam worldgen libraries hook - they wrap it in region management, which is worth having when a
 * pack carries a dozen biome mods and is a great deal of machinery for one biome.
 *
 * <h2>What the numbers mean</h2>
 *
 * <p>Six axes of noise, each from -1 to 1, and a biome claims a box in that space. Whichever box is
 * nearest a given point wins it, so these are not a filter - they are a bid, and the biome appears
 * wherever it outbids vanilla. That is also why the ranges below are narrow: a wide box wins a great
 * deal of the world - and wins it in one piece, because a box that is twice as wide does not
 * produce twice as many, it produces the same ones twice the size.
 */
@Mixin(OverworldBiomeBuilder.class)
public abstract class OverworldBiomesMixin {
    /**
     * Cool but not cold, and fairly dry.
     *
     * <p>These two and weirdness are the axes that set the biome's size, and there are now two
     * measurements to work from rather than a guess. At 0.65 and 0.75 wide the patches came out too
     * small; at 1.10 and 1.15 they came out far too large.
     *
     * <p>The three axes multiply, so the wide box was a bit over four times the volume of the narrow
     * one - which means the midpoint in size is nowhere near the midpoint in each number. These sit
     * at about twice the narrow box and half the wide one.
     */
    private static final Climate.Parameter JOJOHA$TEMPERATURE = Climate.Parameter.span(-0.58F, 0.40F);
    private static final Climate.Parameter JOJOHA$HUMIDITY = Climate.Parameter.span(-0.58F, 0.48F);

    /**
     * Inland, from the near band outward.
     *
     * <p>Opened up from far-inland-only so the biome is not confined to the deep interior of a
     * continent. Left alone while the size was being brought back down: this one is a statement
     * about where the biome belongs rather than how much of the map it takes, and the three axes
     * above are the honest place to spend that.
     */
    private static final Climate.Parameter JOJOHA$CONTINENTALNESS = Climate.Parameter.span(0.03F, 1.0F);

    /**
     * Hills rather than mountains.
     *
     * <p>Erosion decides how much relief the land has and runs backwards from its name: the lowest
     * band is the most dramatic. Vanilla's bands sit roughly at -1.0 for peaks, -0.78 for mountains,
     * -0.375 for hills, -0.2225 for rolling ground and 0.05 upward for plains.
     *
     * <p>This started in the mountain bands and came out as cliffs. It now spans rolling into plains,
     * which is open ground that undulates without ever going vertical - flat and hilly at once, which
     * sounds contradictory and is exactly what a moor looks like.
     */
    private static final Climate.Parameter JOJOHA$EROSION = Climate.Parameter.span(-0.25F, 0.45F);

    /** Surface only. Depth 1 is the underground copy of the map, and a sky biome has no business there. */
    private static final Climate.Parameter JOJOHA$DEPTH = Climate.Parameter.point(0.0F);

    /**
     * A wide slice, which is what makes each one big.
     *
     * <p>The size of a biome and how often it turns up are the same number here, and that is worth
     * being clear about: the space is carved by nearest-match, so widening a box does not scatter
     * more of them - it makes the ones that exist cover more ground before another biome outbids it.
     * Which also means there is no way to ask for many small ones. Fewer and larger, or more and
     * smaller: this number picks a point on that line and cannot get off it.
     */
    private static final Climate.Parameter JOJOHA$WEIRDNESS = Climate.Parameter.span(-0.2F, 0.35F);

    /**
     * How much it is willing to lose ties by.
     *
     * <p>Offset is added to the distance when biomes compete for the same point, so zero bids as
     * hard as vanilla does and a larger number yields to it. Raise this to make the biome rarer
     * without touching the ranges above.
     */
    private static final float JOJOHA$OFFSET = 0.0F;

    @Inject(method = "addBiomes", at = @At("TAIL"))
    private void jojoha$addPhantomHighlands(
            Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> consumer, CallbackInfo ci) {
        consumer.accept(Pair.of(
                Climate.parameters(JOJOHA$TEMPERATURE, JOJOHA$HUMIDITY, JOJOHA$CONTINENTALNESS,
                        JOJOHA$EROSION, JOJOHA$DEPTH, JOJOHA$WEIRDNESS, JOJOHA$OFFSET),
                ModBiomes.PHANTOM_HIGHLANDS));
    }
}
