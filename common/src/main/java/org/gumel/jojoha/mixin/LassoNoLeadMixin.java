package org.gumel.jojoha.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.gumel.jojoha.stand.skill.moves.LassoOfThornsSkill;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class LassoNoLeadMixin {
    @Inject(method = "spawnAtLocation(Lnet/minecraft/world/item/ItemStack;F)Lnet/minecraft/world/entity/item/ItemEntity;",
            at = @At("HEAD"), cancellable = true)
    private void jojoha$noLead(ItemStack stack, float offset, CallbackInfoReturnable<ItemEntity> cir) {
        Entity self = (Entity) (Object) this;
        if (!stack.is(Items.LEAD) || !self.getTags().contains(LassoOfThornsSkill.LASSO_TAG)) {
            return;
        }
        self.removeTag(LassoOfThornsSkill.LASSO_TAG);
        cir.setReturnValue(null);
    }
}
