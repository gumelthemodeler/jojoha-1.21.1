package org.gumel.jojoha.hamon;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * One of the 8 Hamon paths from the design doc. Only {@link HamonPaths#HERMIT} has a
 * functional moveset this pass — the rest are registered as data stubs (empty movesets)
 * so the shape is ready for future content passes.
 */
public record HamonPath(ResourceLocation id, String displayName, @Nullable String teacher, List<ResourceLocation> moveIds) {
}
