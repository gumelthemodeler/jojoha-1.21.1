package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

public final class FleshBudModel {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "flesh_bud"), "main");

    public static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/entity/vampire/flesh_bud.png");

    private final ModelPart root;

    public FleshBudModel(ModelPart root) {
        this.root = root.getChild("bb_main");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition parts = mesh.getRoot();

        PartDefinition main = parts.addOrReplaceChild("bb_main", CubeListBuilder.create()
                        .texOffs(0, 7).addBox(-1.0F, -2.0F, -2.0F, 2.0F, 2.0F, 3.0F,
                                new CubeDeformation(0.0F))
                        .texOffs(10, 7).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 2.0F, 3.0F,
                                new CubeDeformation(0.2F)),
                PartPose.ZERO);

        main.addOrReplaceChild("barb_left", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-2.0F, -2.0F, -1.5F, 4.0F, 4.0F, 3.0F,
                                new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -1.0F, 2.5F, 0.0F, 0.0F, 0.7854F));

        main.addOrReplaceChild("barb_right", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-2.0F, -2.0F, -1.5F, 4.0F, 4.0F, 3.0F,
                                new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -1.0F, 1.5F, 0.0F, 0.0F, -0.7854F));

        return LayerDefinition.create(mesh, 32, 32);
    }

    public void render(PoseStack poseStack, VertexConsumer consumer, int light, int overlay,
                       int colour) {
        root.render(poseStack, consumer, light, overlay, colour);
    }
}
