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

public final class ClackerModels {
    public static final ModelLayerLocation HEAD = layer("clacker_head");
    public static final ModelLayerLocation STRING = layer("clacker_string");

    public static final ResourceLocation STRING_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/entity/hamon/clacker/string.png");

    public static final float LINK_LENGTH = 5F / 16F;

    public static final String SIDEWAYS = "sideways";
    public static final String FACING = "facing";

    private ClackerModels() {
    }

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, name), "main");
    }

    public static ResourceLocation headTexture(String colour) {
        return ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/entity/hamon/clacker/" + colour + ".png");
    }

    public static LayerDefinition createHeadLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("bb_main", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-1.5F, -3.0F, -1.5F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 1.5F, 0.0F));
        return LayerDefinition.create(mesh, 16, 16);
    }

    public static LayerDefinition createStringLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild(SIDEWAYS, CubeListBuilder.create()
                        .texOffs(1, 1).addBox(0.0F, -5.0F, -0.5F, 0.0F, 5.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 2.5F, 0.0F));
        root.addOrReplaceChild(FACING, CubeListBuilder.create()
                        .texOffs(5, 0).addBox(-0.5F, -5.0F, 0.0F, 1.0F, 5.0F, 0.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 2.5F, 0.0F));
        return LayerDefinition.create(mesh, 16, 16);
    }
}
