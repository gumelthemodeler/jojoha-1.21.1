package org.gumel.jojoha.level.feature;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.gumel.jojoha.Jojoha;

public final class Wild {
    public static final TagKey<Block> TAG =
            TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "wild"));

    private Wild() {
    }

    public static boolean is(BlockState state) {
        return state.isAir() || !state.getFluidState().isEmpty() || state.is(TAG);
    }
}
