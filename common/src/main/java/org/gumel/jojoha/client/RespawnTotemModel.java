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

public final class RespawnTotemModel {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "respawn_totem"), "main");

    private RespawnTotemModel() {
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition bone = root.addOrReplaceChild("bone", CubeListBuilder.create()
                .texOffs(0, 21).addBox(-14.0F, -16.0F, 2.0F, 12.0F, 16.0F, 12.0F, new CubeDeformation(-0.01F))
                .texOffs(0, 0).addBox(-16.0F, -5.0F, 0.0F, 16.0F, 5.0F, 16.0F, new CubeDeformation(0.0F))
                .texOffs(48, 21).addBox(-12.0F, -32.0F, 4.0F, 8.0F, 16.0F, 8.0F, new CubeDeformation(0.0F))
                .texOffs(48, 45).addBox(-13.0F, -34.0F, 3.0F, 10.0F, 3.0F, 10.0F, new CubeDeformation(0.0F)),
                PartPose.offset(8.0F, 24.0F, -8.0F));

        PartDefinition faces = bone.addOrReplaceChild("bone2", CubeListBuilder.create()
                .texOffs(0, 49).addBox(-3.0F, -3.0F, -1.5F, 6.0F, 6.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(18, 49).addBox(-3.0F, -3.0F, 7.5F, 6.0F, 6.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-8.0F, -27.0F, 3.5F));

        faces.addOrReplaceChild("bone3", CubeListBuilder.create()
                .texOffs(0, 58).addBox(-3.0F, -1.0F, -6.0F, 6.0F, 5.0F, 3.0F, new CubeDeformation(-0.01F))
                .texOffs(0, 67).addBox(-3.0F, -1.5F, -6.0F, 6.0F, 6.0F, 3.0F, new CubeDeformation(0.15F))
                .texOffs(0, 67).addBox(-3.0F, -1.5F, 3.0F, 6.0F, 6.0F, 3.0F, new CubeDeformation(0.15F))
                .texOffs(0, 58).addBox(-3.0F, -1.0F, 3.0F, 6.0F, 5.0F, 3.0F, new CubeDeformation(-0.01F)),
                PartPose.offset(0.0F, -1.0F, 4.5F));

        return LayerDefinition.create(mesh, 128, 128);
    }
}
