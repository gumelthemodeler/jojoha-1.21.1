package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.gumel.jojoha.Jojoha;

public final class TangledVinesLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
    private static final int STAGES = 4;
    private static final ResourceLocation[] VINES = new ResourceLocation[STAGES];

    static {
        for (int i = 0; i < STAGES; i++) {
            VINES[i] = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID,
                    "textures/entity/effect/tangled_vines_" + i + ".png");
        }
    }

    public TangledVinesLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, T entity,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || entity.isInvisible()) {
            return;
        }
        float growth = TangledVines.growth(entity.getId(), minecraft.level.getGameTime());
        if (growth <= 0F) {
            return;
        }
        int stage = Mth.clamp(Mth.ceil(growth * STAGES) - 1, 0, STAGES - 1);
        getParentModel().renderToBuffer(poseStack,
                bufferSource.getBuffer(RenderType.entityCutoutNoCull(VINES[stage])), packedLight,
                LivingEntityRenderer.getOverlayCoords(entity, 0F), 0xFFFFFFFF);
    }
}
