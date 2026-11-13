package org.gumel.jojoha.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.gumel.jojoha.client.AnubisLook;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ItemRenderer.class)
public abstract class AnubisItemModelMixin {
    @WrapMethod(method = "getModel")
    private BakedModel jojoha$drawnAsAnubis(ItemStack stack, @Nullable Level level, @Nullable LivingEntity entity,
                                             int seed, Operation<BakedModel> original) {
        return original.call(AnubisLook.drawnAs(stack, entity), level, entity, seed);
    }
}
