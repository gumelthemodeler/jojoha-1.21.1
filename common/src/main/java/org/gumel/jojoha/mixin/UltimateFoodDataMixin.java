package org.gumel.jojoha.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.gumel.jojoha.ultimate.UltimateDiet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FoodData.class)
public abstract class UltimateFoodDataMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void jojoha$noteEater(Player player, CallbackInfo ci) {
        UltimateDiet.eating(player);
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void jojoha$forgetEater(Player player, CallbackInfo ci) {
        UltimateDiet.doneEating();
    }

    @ModifyArg(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/food/FoodData;addExhaustion(F)V"))
    private float jojoha$slowUltimateHunger(float exhaustion) {
        return UltimateDiet.exhaustion(exhaustion);
    }
}
