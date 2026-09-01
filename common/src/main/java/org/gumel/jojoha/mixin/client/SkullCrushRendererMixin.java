package org.gumel.jojoha.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.gumel.jojoha.client.SkullFlashFx;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Makes a victim of Skull Crusher see-through, and puts a solid skull inside them.
 *
 * <h2>Why this is done to the whole body</h2>
 *
 * <p>The ask was for the head alone, and for a player or a zombie that would be one model part away.
 * But this has to work on every mob in the game and whatever a modpack adds, and there is no shared
 * notion of "the head part" - a slime has no head, a spider's is not called one, and a modded boss
 * could be anything. There is exactly one hook every living renderer shares, and it turns the whole
 * body translucent.
 *
 * <p>In practice the head is what anyone is looking at during the second it lasts, and a body that
 * has gone glassy around it reads as part of the same effect rather than as a mistake.
 *
 * <h2>How it hangs together</h2>
 *
 * <p>Three touches on one render, and the order between them is the whole trick:
 *
 * <ol>
 *   <li>the skull is drawn first, solid, before the body is drawn at all - so it writes depth and is
 *       genuinely inside;</li>
 *   <li>the render type is swapped for the translucent one vanilla already uses for entities you can
 *       see through;</li>
 *   <li>the colour the model is drawn with gets an alpha, because the type alone does not fade
 *       anything - it only permits fading.</li>
 * </ol>
 *
 * <p>Drawn first rather than last because a translucent surface still writes depth: a solid skull
 * drawn after the head it sits inside would be rejected by the depth test and never appear. That is
 * what the previous attempt got wrong, and no amount of adjusting the render type would have fixed
 * it.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class SkullCrushRendererMixin {

    /**
     * The alpha for the body being drawn right now, or -1 when it is nobody's business.
     *
     * <p>A field rather than an argument because the two injections that need it cannot see each
     * other's locals. Rendering is single threaded and this is set and cleared within one call, so
     * there is never a second body between the two.
     */
    private static float jojoha$alpha = -1F;

    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FF"
            + "Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"))
    private void jojoha$skullBeforeBody(LivingEntity entity, float entityYaw, float partialTick,
                                        PoseStack poseStack, MultiBufferSource buffers, int light,
                                        CallbackInfo ci) {
        jojoha$alpha = SkullFlashFx.bodyAlpha(entity);
        if (jojoha$alpha < 0F) {
            return;
        }

        SkullFlashFx.renderInside(entity, poseStack, buffers, light, partialTick);
    }

    /**
     * The skeleton, drawn in the one place in this method where it can be.
     *
     * <p>Three things have to be true at once, and there is exactly one window where they all are.
     * The bytecode of {@code render} settles it:
     *
     * <pre>
     *   356: PoseStack.translate     - the last of the entity transforms
     *   451: EntityModel.setupAnim   - the model now holds this entity's pose
     *   520: MultiBufferSource.getBuffer - the body claims the shared builder
     *   563: EntityModel.renderToBuffer
     * </pre>
     *
     * <p>Before 356 the pose stack is not in model space, so the skeleton would be drawn in the
     * wrong place. Before 451 the model still holds the pose of whichever entity was drawn before
     * this one, since the instance is shared across every mob of the type - which is what made the
     * first attempt copy stale limbs. And after 520 the body already owns the shared builder.
     *
     * <p>That last one is not a preference, it is a crash. {@code MultiBufferSource.BufferSource}
     * keeps one builder for everything outside its small fixed set, and asking it for a second
     * render type ends the batch already in progress. The body had captured its {@code
     * VertexConsumer} at 520 and handed it to 563 regardless, so drawing the skeleton in between
     * closed that builder under vanilla's feet and it wrote into a dead one - IllegalStateException,
     * Not building.
     *
     * <p>Claiming the buffer here instead inverts it harmlessly: the skeleton takes the shared
     * builder first, and the body's own request at 520 ends the skeleton's batch and starts its
     * own. The skeleton is therefore drawn first for real, which is also exactly the order it needs
     * - a translucent body writes depth, so anything solid meant to be seen through it has to be
     * down before it.
     */
    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FF"
            + "Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/model/EntityModel;setupAnim"
                            + "(Lnet/minecraft/world/entity/Entity;FFFFF)V",
                    shift = At.Shift.AFTER))
    private void jojoha$skeletonInPose(LivingEntity entity, float entityYaw, float partialTick,
                                       PoseStack poseStack, MultiBufferSource buffers, int light,
                                       CallbackInfo ci) {
        if (jojoha$alpha < 0F) {
            return;
        }

        // Whether the mob is shaped like a person is answered by the model itself rather than by a
        // list of entity types, which goes stale the moment anything adds a mob.
        @SuppressWarnings({"unchecked", "rawtypes"})
        EntityModel<?> model = ((LivingEntityRenderer) (Object) this).getModel();
        if (model instanceof HumanoidModel<?> humanoid) {
            SkullFlashFx.renderSkeleton(humanoid, poseStack, buffers, light,
                    OverlayTexture.NO_OVERLAY);
        }
    }

    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FF"
            + "Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("RETURN"))
    private void jojoha$done(LivingEntity entity, float entityYaw, float partialTick,
                             PoseStack poseStack, MultiBufferSource buffers, int light,
                             CallbackInfo ci) {
        jojoha$alpha = -1F;
    }

    /**
     * Swaps in the type vanilla already reaches for when an entity is meant to be seen through.
     *
     * <p>Borrowed rather than invented: {@code getRenderType} has a translucent branch of its own for
     * invisible-but-visible entities, so this is the same path the game already uses and needs no
     * new render type registering.
     */
    @ModifyReturnValue(method = "getRenderType", at = @At("RETURN"))
    private RenderType jojoha$seeThrough(RenderType original, LivingEntity entity) {
        if (jojoha$alpha < 0F) {
            return original;
        }

        // Raw rather than wildcard: the method is declared on the generic parameter, and a
        // wildcard receiver will not accept the very entity the game just handed us.
        @SuppressWarnings({"unchecked", "rawtypes"})
        ResourceLocation texture = ((LivingEntityRenderer) (Object) this).getTextureLocation(entity);
        return RenderType.itemEntityTranslucentCull(texture);
    }

    /**
     * Puts the alpha into the colour the model is drawn with.
     *
     * <p>Wrapped rather than modified in place because the argument is the last of five ints and an
     * index is a poor way to say which - wrapping names it.
     */
    @WrapOperation(method = "render(Lnet/minecraft/world/entity/LivingEntity;FF"
            + "Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer"
                            + "(Lcom/mojang/blaze3d/vertex/PoseStack;"
                            + "Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"))
    private void jojoha$fadeBody(EntityModel<?> model, PoseStack poseStack, VertexConsumer consumer,
                                 int light, int overlay, int colour, Operation<Void> original) {
        if (jojoha$alpha >= 0F) {
            int alpha = Math.max(0, Math.min(255, Math.round(jojoha$alpha * 255F)));
            colour = (alpha << 24) | (colour & 0xFFFFFF);
        }
        original.call(model, poseStack, consumer, light, overlay, colour);
    }
}
