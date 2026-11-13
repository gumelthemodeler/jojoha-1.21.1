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

public final class HermitGrappleModels {
    public static final ModelLayerLocation ROPE = layer("hermit_grapple");
    public static final ModelLayerLocation HOOK = layer("hermit_grapple_hook");

    public static final float SEGMENT_HEIGHT = 10F;

    private HermitGrappleModels() {
    }

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(
                ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, name), "main");
    }

    public static LayerDefinition createRopeLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition main = root.addOrReplaceChild("bb_main", CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 10.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 12)
                        .addBox(-3.0F, 0.0F, 0.0F, 6.0F, 10.0F, 0.0F, new CubeDeformation(0.0F))
                        .texOffs(8, 0)
                        .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 10.0F, 2.0F, new CubeDeformation(0.25F)),
                PartPose.ZERO);

        main.addOrReplaceChild("cross", CubeListBuilder.create()
                        .texOffs(0, 12)
                        .addBox(-3.0F, -5.0F, 0.0F, 6.0F, 10.0F, 0.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 5.0F, 0.0F, 0.0F, 1.5708F, 0.0F));

        return LayerDefinition.create(mesh, 32, 32);
    }

    public static LayerDefinition createHookLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("bb_main", CubeListBuilder.create()
                        .texOffs(0, 14)
                        .addBox(-3.0F, -3.0F, -3.0F, 6.0F, 3.0F, 6.0F, new CubeDeformation(0.3F))
                        .texOffs(0, 23)
                        .addBox(-3.0F, -3.0F, -3.0F, 6.0F, 3.0F, 6.0F, new CubeDeformation(0.0F))
                        .texOffs(1, -8)
                        .addBox(0.0F, -5.0F, -5.0F, 0.0F, 5.0F, 10.0F, new CubeDeformation(0.0F)),
                PartPose.ZERO);

        return LayerDefinition.create(mesh, 32, 32);
    }
}
