package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.gumel.jojoha.item.StandArrowProjectile;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StandArrowRenderer extends EntityRenderer<StandArrowProjectile> {
    private static final float SIZE = 0.75F;
    private static final int VERTEX_STRIDE = 8;

    private final ItemRenderer items;
    private final Map<Item, Vector3f> centres = new HashMap<>();

    public StandArrowRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.items = context.getItemRenderer();
    }

    @Override
    public void render(StandArrowProjectile entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int light) {
        ItemStack stack = entity.shownItem();
        if (stack.isEmpty()) {
            return;
        }
        if (entity.tickCount < 2 && entityRenderDispatcher.camera.getEntity().distanceToSqr(entity) < 12.25D) {
            return;
        }
        BakedModel model = items.getModel(stack, entity.level(), null, entity.getId());
        Vector3f centre = centres.computeIfAbsent(stack.getItem(), item -> centreOf(model));
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTick, entity.yRotO, entity.getYRot()) - 90F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTick, entity.xRotO, entity.getXRot())));
        float shake = entity.shakeTime - partialTick;
        if (shake > 0F) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(-Mth.sin(shake * 3F) * shake));
        }
        poseStack.scale(SIZE, SIZE, SIZE);
        for (int plane = 0; plane < 2; plane++) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.XP.rotationDegrees(plane * 90F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(-45F));
            poseStack.translate(0.5F - centre.x(), 0.5F - centre.y(), 0.5F - centre.z());
            items.render(stack, ItemDisplayContext.NONE, false, poseStack, buffers, light, OverlayTexture.NO_OVERLAY,
                    model);
            poseStack.popPose();
        }
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffers, light);
    }

    private static Vector3f centreOf(BakedModel model) {
        Vector3f min = new Vector3f(Float.MAX_VALUE);
        Vector3f max = new Vector3f(-Float.MAX_VALUE);
        RandomSource random = RandomSource.create(42L);
        boolean any = false;
        for (Direction side : new Direction[]{null, Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH,
                Direction.WEST, Direction.EAST}) {
            List<BakedQuad> quads = model.getQuads(null, side, random);
            for (BakedQuad quad : quads) {
                int[] vertices = quad.getVertices();
                for (int at = 0; at + 2 < vertices.length; at += VERTEX_STRIDE) {
                    Vector3f point = new Vector3f(Float.intBitsToFloat(vertices[at]),
                            Float.intBitsToFloat(vertices[at + 1]), Float.intBitsToFloat(vertices[at + 2]));
                    min.min(point);
                    max.max(point);
                    any = true;
                }
            }
        }
        return any ? min.add(max).mul(0.5F) : new Vector3f(0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(StandArrowProjectile entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
