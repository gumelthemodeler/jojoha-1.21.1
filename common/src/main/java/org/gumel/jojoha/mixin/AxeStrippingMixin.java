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

/**
 * Lets an axe strip a phantom log.
 *
 * <h2>Why a mixin and not a registry call</h2>
 *
 * <p>Vanilla keeps the answer in {@code AxeItem.STRIPPABLES}, which is an {@code ImmutableMap} built
 * in a static initialiser - there is no adding to it. Fabric offers a registry for this and NeoForge
 * does it another way again, and using either would put a loader-specific call into common code,
 * which is the one thing this project's structure is meant to avoid.
 *
 * <p>So the lookup itself is extended instead. {@code getStripped} is private, but a mixin does not
 * care about that, and its signature is identical in the plain and the NeoForge-patched jars - both
 * checked, because getting this wrong once already cost a crash on Fabric from a method that only
 * existed on NeoForge. Everything else about stripping - the durability cost, the sound, the
 * particles, the advancement, the shield-use check - is vanilla's and stays vanilla's.
 */
@Mixin(AxeItem.class)
public abstract class AxeStrippingMixin {

    @Inject(method = "getStripped", at = @At("HEAD"), cancellable = true)
    private void jojoha$stripPhantomLog(BlockState state,
                                        CallbackInfoReturnable<Optional<BlockState>> cir) {
        if (!state.is(ModBlocks.PHANTOM_LOG.get())) return;

        // The axis carries over, or a stripped log would always come out standing upright no matter
        // which way the trunk it came from was lying.
        cir.setReturnValue(Optional.of(ModBlocks.STRIPPED_PHANTOM_LOG.get().defaultBlockState()
                .setValue(RotatedPillarBlock.AXIS, state.getValue(RotatedPillarBlock.AXIS))));
    }
}
