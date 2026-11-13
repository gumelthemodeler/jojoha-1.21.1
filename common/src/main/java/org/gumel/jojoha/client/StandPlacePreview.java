package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.gumel.jojoha.data.ClientPlayerDataCache;
import org.gumel.jojoha.stand.BuildMode;
import org.gumel.jojoha.stand.PlacementRun;
import org.gumel.jojoha.stand.PlacementTurn;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.StandHands;
import org.gumel.jojoha.stand.StandRange;
import org.gumel.jojoha.stand.StandShadowForm;

import java.util.List;

public final class StandPlacePreview {
    private static final double INSET = 0.002;

    private static final float FILL_ALPHA = 0.22F;
    private static final float FILL_PULSE = 0.07F;
    private static final float EDGE_ALPHA = 0.85F;

    private static final float GHOST_ALPHA = 0.46F;
    private static final float GHOST_PULSE = 0.1F;

    private static final float PULSE_SPEED = 0.12F;

    private static final float TRAIL_DIM = 0.55F;

    private static final float[] BLOCKED = {0.92F, 0.22F, 0.18F};

    private StandPlacePreview() {
    }

    public static void render(PoseStack poseStack, MultiBufferSource buffers, Camera camera,
                              float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }

        if (!StandHandsInput.shouldDelegate()) {
            return;
        }

        InteractionHand hand = StandHandsInput.delegatedHand(minecraft.player);
        if (hand == null) {
            return;
        }

        ItemStack stack = minecraft.player.getItemInHand(hand);

        Entity stand = StandEntityLookup.localStand(minecraft).orElse(null);
        if (!(stand instanceof StandEntity standEntity)) {
            return;
        }

        BlockHitResult hit = StandHands.aimFrom(minecraft.player,
                minecraft.player.getEyePosition(partialTick),
                minecraft.player.getViewVector(partialTick),
                reachFor(standEntity));
        boolean fills = StandHands.fills(stack);

        if (hit.getType() != HitResult.Type.BLOCK) {
            BlockPos anchor = fills && !mode().fillsAVolume()
                    ? StandStretch.anchor().orElse(null)
                    : null;
            if (anchor != null) {
                drawRun(poseStack, buffers, camera, minecraft, standEntity, partialTick, true,
                        PlacementRun.between(anchor, PlacementRun.farEnd(anchor,
                                minecraft.player.getEyePosition(partialTick),
                                minecraft.player.getViewVector(partialTick),
                                reachFor(standEntity), mode()), mode()),
                        null, hand, stack, Direction.UP, PlacementRun.CENTRE);
            }
            return;
        }

        BlockPlaceContext context = new BlockPlaceContext(minecraft.player, hand, stack, hit);
        BlockPos far = fills ? context.getClickedPos() : hit.getBlockPos();

        Vec3 grip = PlacementRun.grip(hit, far);

        List<BlockPos> cells;
        if (!StandHands.shapes(stack)) {
            cells = List.of(far);
        } else if (mode().fillsAVolume()) {
            BlockPos corner = StandStretch.corner().orElse(null);
            cells = corner == null ? List.of(far) : PlacementRun.box(corner, far);
        } else {
            cells = StandStretch.anchor().map(anchor -> PlacementRun.between(anchor, far, mode()))
                    .orElseGet(() -> List.of(far));
        }

        drawRun(poseStack, buffers, camera, minecraft, standEntity, partialTick, fills, cells, far,
                hand, stack, hit.getDirection(), grip);
    }

    private static void drawRun(PoseStack poseStack, MultiBufferSource buffers, Camera camera,
                                Minecraft minecraft, StandEntity stand, float partialTick,
                                boolean fills, List<BlockPos> cells, BlockPos lead,
                                InteractionHand hand, ItemStack stack, Direction face, Vec3 grip) {
        float breath = Mth.sin((minecraft.level.getGameTime() + partialTick) * PULSE_SPEED);
        float pulse = FILL_ALPHA + FILL_PULSE * breath;
        float solidity = GHOST_ALPHA + GHOST_PULSE * breath;
        Vec3 cameraPos = camera.getPosition();

        for (BlockPos cell : cells) {
            float strength = lead == null || cell.equals(lead) ? 1F : TRAIL_DIM;

            BlockState state = fills ? landing(minecraft, hand, stack, cell, face, grip) : null;
            boolean takes = state != null;
            float[] tint = takes ? auraTint(stand) : BLOCKED;

            boolean ghosted = takes && !state.isAir();
            if (ghosted) {
                ghost(poseStack, buffers, state, cell, cameraPos, solidity * strength);
            }

            for (AABB piece : pieces(minecraft, state, cell)) {
                AABB box = piece.inflate(-INSET)
                        .move(-cameraPos.x, -cameraPos.y, -cameraPos.z);

                if (takes && !ghosted) {
                    GlowBox.fill(poseStack, buffers.getBuffer(RenderType.lightning()), box, tint,
                            pulse * strength);
                }
                LevelRenderer.renderLineBox(poseStack, buffers.getBuffer(RenderType.lines()), box,
                        tint[0], tint[1], tint[2], EDGE_ALPHA * strength);
            }
        }
    }

    private static BlockState landing(Minecraft minecraft, InteractionHand hand, ItemStack stack,
                                      BlockPos cell, Direction face, Vec3 grip) {
        BlockPlaceContext context = new BlockPlaceContext(minecraft.player, hand, stack,
                PlacementRun.hitAt(cell, face, grip));

        if (!context.canPlace() || !context.getClickedPos().equals(cell)) {
            return null;
        }

        if (!(stack.getItem() instanceof BlockItem item)) {
            return Blocks.AIR.defaultBlockState();
        }

        BlockState state = item.getBlock().getStateForPlacement(context);
        if (state == null) {
            return null;
        }

        state = PlacementTurn.settled(minecraft.level, cell, state, StandTurn.turns());
        return state.canSurvive(minecraft.level, cell) ? state : null;
    }

    private static List<AABB> pieces(Minecraft minecraft, BlockState state, BlockPos cell) {
        if (state == null || state.isAir()) {
            return List.of(new AABB(cell));
        }

        VoxelShape shape = state.getShape(minecraft.level, cell);
        if (shape.isEmpty()) {
            return List.of(new AABB(cell));
        }

        return shape.toAabbs().stream()
                .map(box -> box.move(cell.getX(), cell.getY(), cell.getZ()))
                .toList();
    }

    private static void ghost(PoseStack poseStack, MultiBufferSource buffers, BlockState state,
                              BlockPos cell, Vec3 cameraPos, float alpha) {
        MultiBufferSource faded = type ->
                new Faded(buffers.getBuffer(Sheets.translucentCullBlockSheet()),
                        Mth.clamp(alpha, 0F, 1F));

        poseStack.pushPose();
        poseStack.translate(cell.getX() - cameraPos.x, cell.getY() - cameraPos.y,
                cell.getZ() - cameraPos.z);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state, poseStack, faded,
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }

    private record Faded(VertexConsumer delegate, float alpha) implements VertexConsumer {
        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            delegate.addVertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int a) {
            delegate.setColor(red, green, blue, Math.round(a * alpha));
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            delegate.setUv(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            delegate.setUv1(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            delegate.setUv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            delegate.setNormal(x, y, z);
            return this;
        }
    }

    private static BuildMode mode() {
        return ClientPlayerDataCache.data.buildMode;
    }

    private static double reachFor(StandEntity stand) {
        return stand.getStandType().range() == StandRange.LONG
                ? StandHands.LONG_REACH
                : StandHands.CLOSE_REACH;
    }

    private static float[] auraTint(StandEntity stand) {
        int colour = StandShadowForm.auraColourFor(stand);
        return new float[]{
                ((colour >> 16) & 0xFF) / 255F,
                ((colour >> 8) & 0xFF) / 255F,
                (colour & 0xFF) / 255F};
    }

}
