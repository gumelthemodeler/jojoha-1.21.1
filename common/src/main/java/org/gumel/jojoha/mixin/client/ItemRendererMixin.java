package org.gumel.jojoha.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.gumel.jojoha.client.StandArrowGlow;
import org.gumel.jojoha.client.TintedVertexConsumer;
import org.gumel.jojoha.registry.ModItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Burns a Stand Arrow through the colour wheel while its owner is driving it into themselves.
 *
 * <p>Hooked on the entity-aware {@code renderStatic} because that is the one place that knows
 * <em>whose</em> hand the item is in - without it there would be no way to tell a mid-ritual arrow
 * from one sitting in someone's inventory. Both the first-person hand and the third-person model
 * route through here, so a single hook covers the holder's own view and everyone watching them.
 *
 * <p>Parked: this is deliberately absent from the client list in {@code jojoha.mixins.json}, so
 * none of it is applied. See {@code StandArrowColors} for how to switch the whole effect back on.
 */
@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {

    /**
     * The glow samples a flat white texture rather than the arrow's own.
     *
     * <p>This is the difference between a rainbow and a barely-there wash. The eyes shader computes
     * {@code texture * vertexColor}, so drawing the glow against the block atlas multiplies the hue
     * by whatever the arrow's art happens to be - dark pixels come out near black and the colour
     * all but disappears. Sampling a texture that is white at every texel leaves the hue intact, so
     * what lands on the arrow is the colour asked for rather than the colour filtered through it.
     */
    private static final ResourceLocation GLOW_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/misc/white.png");

    /**
     * How hard the glow is laid over the arrow.
     *
     * <p>Additive against a white source, so this is close to the amount of pure hue added to every
     * channel. Held well under full: at full strength the arrow becomes a flat coloured silhouette
     * and stops reading as an arrow at all.
     */
    private static final int GLOW_ALPHA = 145;

    @Inject(
            method = "renderStatic(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/level/Level;III)V",
            at = @At("TAIL"))
    private void jojoha$glowStandArrow(LivingEntity entity, ItemStack stack, ItemDisplayContext displayContext,
                                       boolean leftHand, PoseStack poseStack, MultiBufferSource bufferSource,
                                       Level level, int packedLight, int packedOverlay, int seed,
                                       CallbackInfo ci) {
        if (!(entity instanceof Player player) || !stack.is(ModItems.STAND_ARROW.get())) {
            return;
        }
        if (!StandArrowGlow.isActive(player.getUUID())) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        float clientTime = (float) minecraft.level.getGameTime() + minecraft.getTimer().getGameTimeDeltaPartialTick(false);
        int rgb = StandArrowGlow.rainbow(clientTime);

        ItemRenderer self = (ItemRenderer) (Object) this;
        BakedModel model = self.getModel(stack, level, entity, seed);

        // Vanilla's own render is re-run rather than the model walked by hand, so the item's
        // display transform, its quads and any custom model all come along for free. Everything it
        // draws is redirected into one additive buffer wearing the rainbow, which is what turns a
        // second pass over the same geometry into light on top of the arrow instead of a repaint.
        self.render(stack, displayContext, leftHand, poseStack,
                glowBuffer(bufferSource, rgb), packedLight, packedOverlay, model);
    }

    /**
     * A buffer source that answers every request with the same additive, rainbow-tinted consumer.
     *
     * <p>{@code RenderType.eyes} is the additive, colour-only pass vanilla uses for glowing eyes:
     * it doesn't write depth and it adds rather than covers, so the arrow keeps the shape the solid
     * pass underneath already gave it and the colour reads as light coming off it.
     */
    private static MultiBufferSource glowBuffer(MultiBufferSource delegate, int rgb) {
        VertexConsumer glow = new TintedVertexConsumer(
                delegate.getBuffer(RenderType.eyes(GLOW_TEXTURE)), rgb, GLOW_ALPHA);
        return type -> glow;
    }
}
