package org.gumel.jojoha.mixin;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.level.Continents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;

@Mixin(MultiNoiseBiomeSource.class)
public abstract class ContinentBiomesMixin {
    @Unique
    private volatile Map<ResourceKey<Biome>, Holder<Biome>> jojoha$continents;

    @Shadow
    private Climate.ParameterList<Holder<Biome>> parameters() {
        throw new AssertionError();
    }

    @Inject(method = "getNoiseBiome(IIILnet/minecraft/world/level/biome/Climate$Sampler;)Lnet/minecraft/core/Holder;",
            at = @At("RETURN"), cancellable = true)
    private void jojoha$continent(int x, int y, int z, Climate.Sampler sampler,
                                  CallbackInfoReturnable<Holder<Biome>> cir) {
        Map<ResourceKey<Biome>, Holder<Biome>> lands = jojoha$lands();
        if (lands.isEmpty() || !Continents.remaps(cir.getReturnValue())) {
            return;
        }
        ResourceKey<Biome> key = Continents.biomeAt(x << 2, z << 2, Continents.salt(sampler),
                cir.getReturnValue().unwrapKey().orElse(null));
        Holder<Biome> biome = key == null ? null : lands.get(key);
        if (biome != null) {
            cir.setReturnValue(biome);
        }
    }

    @Unique
    private Map<ResourceKey<Biome>, Holder<Biome>> jojoha$lands() {
        Map<ResourceKey<Biome>, Holder<Biome>> lands = jojoha$continents;
        if (lands != null) {
            return lands;
        }
        Map<ResourceKey<Biome>, Holder<Biome>> found = new HashMap<>();
        for (Pair<Climate.ParameterPoint, Holder<Biome>> entry : parameters().values()) {
            Holder<Biome> biome = entry.getSecond();
            biome.unwrapKey().filter(key -> Jojoha.MOD_ID.equals(key.location().getNamespace()))
                    .ifPresent(key -> found.put(key, biome));
        }
        jojoha$continents = found;
        return found;
    }
}
