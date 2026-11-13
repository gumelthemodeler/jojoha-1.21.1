package org.gumel.jojoha.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import org.gumel.jojoha.combat.VampireBlood;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Player.class)
public abstract class VampireFoodMixin {

    @ModifyVariable(method = "eat", at = @At("HEAD"), argsOnly = true)
    private FoodProperties jojoha$bloodComesFromBlood(FoodProperties food) {
        if (!VampireBlood.isVampire((Player) (Object) this)) {
            return food;
        }
        return new FoodProperties(0, 0F, food.canAlwaysEat(), food.eatSeconds(),
                food.usingConvertsTo(), food.effects());
    }
}
