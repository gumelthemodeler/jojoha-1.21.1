package org.gumel.jojoha.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.BlockAttachedEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockAttachedEntity.class)
public abstract class PlacedHangingMixin {
    private static final double SAVED_REACH = 16.0;

    @Shadow
    protected BlockPos pos;

    @Inject(method = "readAdditionalSaveData", at = @At("HEAD"), cancellable = true)
    private void jojoha$hangWherePlaced(CompoundTag tag, CallbackInfo ci) {
        BlockPos placed = ((Entity) (Object) this).blockPosition();
        BlockPos saved = new BlockPos(tag.getInt("TileX"), tag.getInt("TileY"), tag.getInt("TileZ"));
        if (!saved.closerThan(placed, SAVED_REACH)) {
            pos = placed;
            ci.cancel();
        }
    }
}
