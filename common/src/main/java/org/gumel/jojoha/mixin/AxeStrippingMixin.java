package org.gumel.jojoha.mixin;

import net.minecraft.world.item.AxeItem;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.gumel.jojoha.registry.ModBlocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(AxeItem.class)
public abstract class AxeStrippingMixin {

    @Inject(method = "getStripped", at = @At("HEAD"), cancellable = true)
    private void jojoha$stripPhantomLog(BlockState state,
                                        CallbackInfoReturnable<Optional<BlockState>> cir) {
        if (!state.is(ModBlocks.PHANTOM_LOG.get())) return;

        cir.setReturnValue(Optional.of(ModBlocks.STRIPPED_PHANTOM_LOG.get().defaultBlockState()
                .setValue(RotatedPillarBlock.AXIS, state.getValue(RotatedPillarBlock.AXIS))));
    }
}
