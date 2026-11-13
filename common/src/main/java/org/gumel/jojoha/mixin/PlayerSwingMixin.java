package org.gumel.jojoha.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.gumel.jojoha.combat.StandHits;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerSwingMixin {
    @Inject(method = "attack(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"))
    private void jojoha$swingBegins(Entity target, CallbackInfo ci) {
        StandHits.beginSwing((Player) (Object) this);
    }

    @Inject(method = "attack(Lnet/minecraft/world/entity/Entity;)V", at = @At("RETURN"))
    private void jojoha$swingEnds(Entity target, CallbackInfo ci) {
        StandHits.endSwing();
    }
}
