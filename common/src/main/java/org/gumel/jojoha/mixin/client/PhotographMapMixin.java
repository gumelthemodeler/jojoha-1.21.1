package org.gumel.jojoha.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.MapRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.stand.skill.moves.CameraCrushSkill;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Swaps the paper behind a Spirit Photograph when it is held up.
 *
 * <p>The inventory icon was the easy half. What you see with a map actually raised is not that icon
 * at all - it is a quad textured with {@code map_background}, with the map's own pixels drawn over
 * the middle of it. Changing the icon and stopping there gives an item that looks like a photograph
 * in the inventory and unmistakably like a map the moment you look at it.
 *
 * <h2>Why a mixin and not a texture</h2>
 *
 * <p>Because the alternative is replacing {@code textures/map/map_background.png}, and that is every
 * map in the world. Turning a player's whole cartography collection into photographs to dress one
 * Stand ability is not a trade worth making, and it is the kind of change that is invisible until
 * somebody opens a chest full of maps.
 *
 * <p>{@code renderMap} is handed the stack, which is the only reason this is a two-line change: the
 * background is chosen by a single {@code getBuffer} call, so swapping that one argument is enough.
 * Everything else about the render - the quad, the map pixels, the decorations - is untouched.
 *
 * <h2>How narrow the change is</h2>
 *
 * <p>Only the stack carrying the photograph's model value is affected. Anything else, including a
 * perfectly ordinary map held in the same hand a moment later, gets vanilla's parchment back.
 */
@Mixin(ItemInHandRenderer.class)
public abstract class PhotographMapMixin {
    /**
     * Photo paper.
     *
     * <p>Built the same way vanilla builds its own - {@code RenderType.text} - so it sorts, blends
     * and lights identically. The only difference between the two is which file is sampled.
     */
    private static final RenderType JOJOHA$PHOTO_PAPER = RenderType.text(
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/map/photo_background.png"));

    /**
     * Where the picture sits inside the print, and how big it is drawn.
     *
     * <p>These three numbers exist twice - here, and in the script that draws the dark well into
     * photo_background.png - and they have to agree or the picture sits off its own mount. Said
     * plainly in both places rather than left to be rediscovered.
     *
     * <p>The map contents are drawn nought to 128 while the paper spans minus seven to 135, so
     * shrinking to 78 percent and nudging in leaves a proper white border all round and a deep
     * margin at the foot, which is what makes it a photograph rather than a map with a frame.
     */
    private static final float JOJOHA$INSET = 0.78F;
    private static final float JOJOHA$OFF_X = 14F;
    private static final float JOJOHA$OFF_Y = 6F;

    /**
     * Shrinks the picture into the well.
     *
     * <p>Wrapped rather than injected around, because the contents have to be drawn inside a pose
     * that is pushed and popped - and an inject at head and return would leave the stack unbalanced
     * on any frame where vanilla took an early exit between them.
     *
     * <p>The decorations shrink with it, which is correct: the X and the player marker are part of
     * the picture, and a marker that stayed full size while the terrain shrank would drift off the
     * spot it is marking.
     */
    @WrapOperation(method = "renderMap(Lcom/mojang/blaze3d/vertex/PoseStack;"
            + "Lnet/minecraft/client/renderer/MultiBufferSource;I"
            + "Lnet/minecraft/world/item/ItemStack;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/MapRenderer;render"
                            + "(Lcom/mojang/blaze3d/vertex/PoseStack;"
                            + "Lnet/minecraft/client/renderer/MultiBufferSource;"
                            + "Lnet/minecraft/world/level/saveddata/maps/MapId;"
                            + "Lnet/minecraft/world/level/saveddata/maps/MapItemSavedData;ZI)V"))
    private void jojoha$insetPicture(MapRenderer renderer, PoseStack poseStack,
                                     MultiBufferSource buffers, MapId mapId,
                                     MapItemSavedData data, boolean active, int light,
                                     Operation<Void> original,
                                     @Local(argsOnly = true) ItemStack stack) {
        if (!jojoha$isPhotograph(stack)) {
            original.call(renderer, poseStack, buffers, mapId, data, active, light);
            return;
        }

        poseStack.pushPose();
        poseStack.translate(JOJOHA$OFF_X, JOJOHA$OFF_Y, 0F);
        poseStack.scale(JOJOHA$INSET, JOJOHA$INSET, 1F);
        original.call(renderer, poseStack, buffers, mapId, data, active, light);
        poseStack.popPose();
    }

    @ModifyArg(method = "renderMap(Lcom/mojang/blaze3d/vertex/PoseStack;"
            + "Lnet/minecraft/client/renderer/MultiBufferSource;I"
            + "Lnet/minecraft/world/item/ItemStack;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/MultiBufferSource;getBuffer"
                            + "(Lnet/minecraft/client/renderer/RenderType;)"
                            + "Lcom/mojang/blaze3d/vertex/VertexConsumer;"))
    private RenderType jojoha$photoPaper(RenderType original,
                                         @Local(argsOnly = true) ItemStack stack) {
        return jojoha$isPhotograph(stack) ? JOJOHA$PHOTO_PAPER : original;
    }

    /** Whether this stack is one of Hermit Purple's prints rather than an ordinary map. */
    private static boolean jojoha$isPhotograph(ItemStack stack) {
        CustomModelData model = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        return model != null && model.value() == CameraCrushSkill.PHOTOGRAPH_MODEL;
    }
}
