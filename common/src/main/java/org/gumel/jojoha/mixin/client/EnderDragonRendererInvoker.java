package org.gumel.jojoha.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.entity.EnderDragonRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Exposes vanilla's {@code EnderDragonRenderer.renderRays}, which is private static.
 *
 * <p>Calling the real thing rather than reimplementing it means the Stand awakening rays are the
 * genuine dragon-death effect - identical geometry, identical seeded randomness, and no drift if
 * Mojang ever changes it - for the cost of one accessor.
 */
@Mixin(EnderDragonRenderer.class)
public interface EnderDragonRendererInvoker {
    @Invoker("renderRays")
    static void jojoha$renderRays(PoseStack poseStack, float deathProgress, VertexConsumer consumer) {
        throw new AssertionError();
    }
}
