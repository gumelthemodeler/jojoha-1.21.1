package org.gumel.jojoha.mixin;

import com.mojang.serialization.Lifecycle;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;
import org.gumel.jojoha.Jojoha;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.stream.Stream;

@Mixin(WorldDimensions.class)
public abstract class DreamDimensionStabilityMixin {
    @Inject(method = "checkStability", at = @At("HEAD"), cancellable = true, require = 0)
    private static void jojoha$ourDimensionsAreStable(ResourceKey<LevelStem> key, LevelStem stem,
                                                      CallbackInfoReturnable<Lifecycle> info) {
        if (jojoha$ours(key)) {
            info.setReturnValue(Lifecycle.stable());
        }
    }

    @Redirect(method = "bake", require = 0,
            at = @At(value = "INVOKE",
                    target = "Lcom/mojang/serialization/Lifecycle;experimental()"
                            + "Lcom/mojang/serialization/Lifecycle;"))
    private Lifecycle jojoha$vouchForOurOwnDimensions(Registry<LevelStem> registry) {
        WorldDimensions self = (WorldDimensions) (Object) this;
        Stream<ResourceKey<LevelStem>> every = Stream.concat(
                registry.registryKeySet().stream(), self.dimensions().keySet().stream());

        return every.distinct().allMatch(key -> jojoha$vanilla(key) || jojoha$ours(key))
                ? Lifecycle.stable()
                : Lifecycle.experimental();
    }

    private static boolean jojoha$ours(ResourceKey<LevelStem> key) {
        return key.location().getNamespace().equals(Jojoha.MOD_ID);
    }

    private static boolean jojoha$vanilla(ResourceKey<LevelStem> key) {
        return key == LevelStem.OVERWORLD || key == LevelStem.NETHER || key == LevelStem.END;
    }
}
