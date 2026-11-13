package org.gumel.jojoha.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.phys.AABB;
import org.gumel.jojoha.stand.heaven.WorldPatch;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({HangingEntity.class, ItemFrame.class})
public abstract class ErasedWallMixin {
    @Inject(method = "survives", at = @At("HEAD"), cancellable = true)
    private void jojoha$wallComesBack(CallbackInfoReturnable<Boolean> cir) {
        HangingEntity hanging = (HangingEntity) (Object) this;
        if (hanging.level().isClientSide()) {
            return;
        }
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        AABB box = hanging.getBoundingBox().inflate(1.0);
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY, box.minZ),
                BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {
            if (WorldPatch.held(hanging.level(), at.set(pos))) {
                cir.setReturnValue(true);
                return;
            }
        }
    }
}
