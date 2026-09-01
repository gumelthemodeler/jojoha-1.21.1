package org.gumel.jojoha.mixin.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.level.ModBiomes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The Phantom Highlands night, which is a different colour from its day rather than a darker one.
 *
 * <h2>Why this cannot be a biome field</h2>
 *
 * <p>A biome carries one {@code sky_color}, and vanilla does not treat it as "the colour of the
 * sky" - it treats it as the colour of the sky at noon. Every other hour is that value multiplied
 * down toward black by how far the sun has gone, so a biome asking for turquoise gets turquoise at
 * midday and a dark, desaturated turquoise at midnight. There is no second field for the other end
 * of the day, and no amount of choosing a cleverer single colour produces two different hues.
 *
 * <p>So the day colour stays in the JSON where it belongs, and this handles the half of the brief
 * the format has no room for: as the light goes, the sky is carried toward a deep blue rather than
 * toward black.
 *
 * <h2>Blended, not switched</h2>
 *
 * <p>Against the same curve vanilla dims by, so the two agree by construction. At noon the blend is
 * zero and the sky is exactly what the biome asked for; at midnight it is one and the sky is the
 * night colour outright; dusk is the crossfade between them, which is when a sky is worth looking at
 * and the worst possible moment for a hard switch.
 */
@Mixin(ClientLevel.class)
public abstract class PhantomSkyMixin {
    /**
     * What the sky becomes once the sun is down.
     *
     * <p>Deep and blue rather than black, and deliberately still lit: a night sky that reaches true
     * black takes the horizon with it, and the point of a themed biome is that you can tell you are
     * standing in it after dark.
     */
    private static final Vec3 JOJOHA$NIGHT = new Vec3(0.043, 0.055, 0.180);

    /** Where the biome starts to answer for the sky, in blocks from the camera. */
    private static final double JOJOHA$SAMPLE_DROP = 2.0;

    @Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true)
    private void jojoha$phantomNight(Vec3 pos, float partialTick,
                                     CallbackInfoReturnable<Vec3> cir) {
        ClientLevel level = (ClientLevel) (Object) this;

        // The same position vanilla sampled the biome colour from, so the sky changes over at the
        // same step the biome's own colour does rather than a block earlier or later.
        BlockPos at = BlockPos.containing(pos.x, pos.y - JOJOHA$SAMPLE_DROP, pos.z);
        if (!level.getBiome(at).is(ModBiomes.PHANTOM_HIGHLANDS)) {
            return;
        }

        // Vanilla's own day curve, not an approximation of it: cos of the time of day, doubled and
        // lifted, clamped to nought and one. Recomputed rather than read because it is a local in
        // the method being returned from, and matching it exactly is what keeps this from drifting
        // out of step with the dimming it is riding on.
        float day = Mth.clamp(Mth.cos(level.getTimeOfDay(partialTick) * Mth.TWO_PI) * 2F + 0.5F,
                0F, 1F);
        float night = 1F - day;
        if (night <= 0.001F) {
            return;
        }

        cir.setReturnValue(cir.getReturnValue().lerp(JOJOHA$NIGHT, night));
    }
}
