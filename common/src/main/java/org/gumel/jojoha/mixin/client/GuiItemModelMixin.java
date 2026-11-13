package org.gumel.jojoha.mixin.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.gumel.jojoha.client.InventoryIconContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphics.class)
public abstract class GuiItemModelMixin {
    private boolean jojoha$outerContext;

    @Inject(
            method = "renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;"
                    + "Lnet/minecraft/world/item/ItemStack;IIII)V",
            at = @At("HEAD"))
    private void jojoha$markSlot(LivingEntity entity, Level level, ItemStack stack,
                                 int x, int y, int seed, int guiOffset, CallbackInfo ci) {
        jojoha$outerContext = InventoryIconContext.push(true);
    }

    @Inject(
            method = "renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;"
                    + "Lnet/minecraft/world/item/ItemStack;IIII)V",
            at = @At("RETURN"))
    private void jojoha$clearSlot(LivingEntity entity, Level level, ItemStack stack,
                                  int x, int y, int seed, int guiOffset, CallbackInfo ci) {
        InventoryIconContext.restore(jojoha$outerContext);
    }
}
