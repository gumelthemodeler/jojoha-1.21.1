package org.gumel.jojoha.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.gumel.jojoha.stand.skill.TimeStopSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockTickFreezeMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void jojoha$freezeScheduledTick(ServerLevel level, BlockPos pos, RandomSource random,
                                            CallbackInfo ci) {
        if (frozen(level, pos)) {
            ci.cancel();
        }
    }

    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void jojoha$freezeRandomTick(ServerLevel level, BlockPos pos, RandomSource random,
                                         CallbackInfo ci) {
        if (frozen(level, pos)) {
            ci.cancel();
        }
    }

    private static boolean frozen(ServerLevel level, BlockPos pos) {
        return TimeStopSystem.freezesPosition(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }
}
