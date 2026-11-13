package org.gumel.jojoha.mixin.client;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.gumel.jojoha.client.TimeStopView;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.world.level.chunk.LevelChunk$BoundTickingBlockEntity")
public abstract class ClientBlockEntityFreezeMixin {

    @Shadow
    @Final
    private BlockEntity blockEntity;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void jojoha$freezeInStoppedTime(CallbackInfo ci) {
        if (!TimeStopView.active()) {
            return;
        }

        Level level = blockEntity.getLevel();
        if (level == null || !level.isClientSide()) {
            return;
        }

        if (TimeStopView.holdsPosition(blockEntity.getBlockPos().getX() + 0.5,
                blockEntity.getBlockPos().getY() + 0.5,
                blockEntity.getBlockPos().getZ() + 0.5)) {
            ci.cancel();
        }
    }
}
