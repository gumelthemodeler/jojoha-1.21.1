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

/**
 * Marks the window in which an item is being drawn into a slot.
 *
 * <p>It has to be here, and the reason is a detail of the order vanilla does things in. Drawing an
 * item into a slot is two steps:
 *
 * <pre>
 *   BakedModel model = itemRenderer.getModel(stack, level, entity, seed);
 *   itemRenderer.render(stack, ItemDisplayContext.GUI, ..., model);
 * </pre>
 *
 * <p>The model is chosen in the first line and the display context does not appear until the second.
 * So anything watching for {@code GUI} on the renderer - which is where this logic started - is
 * looking a step too late: by then the override predicate has already been asked and answered, and
 * the model is settled. Nothing about a slot is knowable from inside {@code getModel} either, since
 * it is handed no context at all.
 *
 * <p>Wrapping the method that performs both steps is what closes that gap. This is the private sink
 * every public {@code renderItem} overload funnels into, so one injection covers inventories,
 * containers, the hotbar, tooltips and anything else drawing a stack into a GUI.
 */
@Mixin(GuiGraphics.class)
public abstract class GuiItemModelMixin {
    /** What was in force before this draw, so nested item draws restore rather than clear. */
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
