package org.gumel.jojoha.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.gumel.jojoha.stand.anubis.Anubis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemNameMixin {
    @Inject(method = "getHoverName", at = @At("HEAD"), cancellable = true)
    private void jojoha$calledAnubis(CallbackInfoReturnable<Component> cir) {
        if (Anubis.named((ItemStack) (Object) this)) {
            cir.setReturnValue(Component.translatable("item.jojoha.anubis"));
        }
    }
}
