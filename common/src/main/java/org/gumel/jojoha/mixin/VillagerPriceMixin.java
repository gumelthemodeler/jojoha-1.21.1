package org.gumel.jojoha.mixin;

import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import org.gumel.jojoha.fortune.Bizarre;
import org.gumel.jojoha.item.Provisions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Villager.class)
public abstract class VillagerPriceMixin {
    @Inject(method = "updateSpecialPrices", at = @At("TAIL"))
    private void jojoha$charismaticDiscount(Player player, CallbackInfo ci) {
        Bizarre.discount((Villager) (Object) this, player);
        Provisions.crowned((Villager) (Object) this, player);
    }
}
