package org.gumel.jojoha.hamon;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record HamonPath(ResourceLocation id, String displayName, @Nullable String teacher, List<ResourceLocation> moveIds) {
}
