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

import java.util.List;
import java.util.function.Consumer;

@Mixin(OverworldBiomeBuilder.class)
public abstract class OverworldBiomesMixin {
    private static final Climate.Parameter JOJOHA$ANY = Climate.Parameter.span(-1.0F, 1.0F);

    private static final Climate.Parameter JOJOHA$DEPTH = Climate.Parameter.point(0.0F);

    private static final float JOJOHA$UNREACHABLE = 1.0F;

    @Inject(method = "addBiomes", at = @At("TAIL"))
    private void jojoha$keepContinentBiomes(
            Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> consumer, CallbackInfo ci) {
        if (ModBiomes.buildingVanilla > 0) {
            return;
        }
        for (ResourceKey<Biome> biome : List.of(ModBiomes.PHANTOM_MOORS, ModBiomes.MOOR_CLEARING, ModBiomes.PHANTOM_BOG, ModBiomes.MISTY_HOLLOW,
                ModBiomes.OSSUARY_FLATS, ModBiomes.ASH_WASTES, ModBiomes.RIBCAGE_CANYON, ModBiomes.BONE_PEAKS,
                ModBiomes.DUNE_SEA, ModBiomes.OASIS, ModBiomes.METEOR_FIELD,
                ModBiomes.SKELETON_HEEL_BONELANDS,
                ModBiomes.STARDUST_SANDS)) {
            consumer.accept(Pair.of(
                    Climate.parameters(JOJOHA$ANY, JOJOHA$ANY, JOJOHA$ANY, JOJOHA$ANY, JOJOHA$DEPTH, JOJOHA$ANY,
                            JOJOHA$UNREACHABLE),
                    biome));
        }
    }
}
