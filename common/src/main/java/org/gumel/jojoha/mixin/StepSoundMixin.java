package org.gumel.jojoha.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.gumel.jojoha.trait.TraitEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class StepSoundMixin {

    @Inject(method = "playStepSound", at = @At("HEAD"), cancellable = true)
    private void jojoha$silentFootsteps(BlockPos pos, BlockState state, CallbackInfo ci) {
        if ((Object) this instanceof Player player
                && TraitEffects.silentFootsteps(player)) {
            ci.cancel();
        }
    }
}
