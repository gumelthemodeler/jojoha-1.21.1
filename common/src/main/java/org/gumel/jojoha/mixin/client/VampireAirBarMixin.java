package org.gumel.jojoha.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.world.entity.player.Player;
import org.gumel.jojoha.combat.VampireBlood;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Gui.class)
public abstract class VampireAirBarMixin {
    @ModifyExpressionValue(method = {"renderPlayerHealth", "renderAirLevel"}, require = 0,
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;isEyeInFluid(Lnet/minecraft/tags/TagKey;)Z"))
    private boolean jojoha$noBubblesForTheDead(boolean underwater) {
        Player player = Minecraft.getInstance().player;
        return underwater && !(player != null && VampireBlood.breathless(player));
    }
}
