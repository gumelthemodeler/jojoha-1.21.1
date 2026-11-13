package org.gumel.jojoha.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.gumel.jojoha.stand.heaven.RealityBend;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class HeavenRecordMixin {
    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
            at = @At("HEAD"))
    private void jojoha$recordForRealityBend(BlockPos pos, BlockState state, int flags, int recursionLeft,
                                             CallbackInfoReturnable<Boolean> cir) {
        Level self = (Level) (Object) this;
        if (RealityBend.watching(self, pos)) {
            RealityBend.noteBlock(self, pos, state);
        }
    }
}
