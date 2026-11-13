package org.gumel.jojoha.client;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

public final class ScarfModels {
    public static final ModelLayerLocation RING = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "lisa_scarf"), "main");

    public static final ResourceLocation RING_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/entity/hamon/lisa_scarf.png");
    public static final ResourceLocation SEGMENT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/entity/hamon/lisa_scarf_segment.png");

    public static final int SHEET = 64;
    public static final int SEGMENT_U = 9;
    public static final int SEGMENT_V = 9;
    public static final int SEGMENT_WIDTH = 6;
    public static final int SEGMENT_LENGTH = 12;
    public static final int SEGMENT_CAP = 1;

    public static final float TAIL_X = 0F;
    public static final float TAIL_Y = 1.0F / 16F;
    public static final float TAIL_Z = 4.0F / 16F;
    public static final float FORWARD = 2.5F / 16F;
    public static final float DROP = 1.0F / 16F;

    private ScarfModels() {
    }

    public static LayerDefinition createRingLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("Head", CubeListBuilder.create(),
                PartPose.offset(-0.25F, 1.0F, 0.0F));
        head.addOrReplaceChild("Head_r1", CubeListBuilder.create()
                        .texOffs(0, 12).addBox(-4.5F, -1.5F, -4.5F, 9.0F, 3.0F, 9.0F, new CubeDeformation(0.3F))
                        .texOffs(0, 0).addBox(-4.5F, -1.5F, -4.5F, 9.0F, 3.0F, 9.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.25F, -1.5F, 2.0F, 0.1309F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, SHEET, SHEET);
    }
}
