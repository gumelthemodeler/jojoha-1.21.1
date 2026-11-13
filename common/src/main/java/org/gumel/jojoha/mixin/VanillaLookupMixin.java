package org.gumel.jojoha.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.registries.VanillaRegistries;
import org.gumel.jojoha.level.ModBiomes;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(VanillaRegistries.class)
public abstract class VanillaLookupMixin {
    @WrapMethod(method = "createLookup")
    private static HolderLookup.Provider jojoha$withoutOurBiomes(Operation<HolderLookup.Provider> original) {
        ModBiomes.buildingVanilla++;
        try {
            return original.call();
        } finally {
            ModBiomes.buildingVanilla--;
        }
    }
}
